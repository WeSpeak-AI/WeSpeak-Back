"""
실제 AI 서버 코드로 Claude 경로의 LLM 지연을 측정한다 (유료 API 호출).

AI 서버 컨테이너 안에서 **별도 프로세스**로 실행한다. 이 프로세스 안에서만 settings.provider를 "claude"로 바꾸고
서비스 함수(chat_service · feedback_service · correction_service)를 직접 호출하므로, 실행 중인 서버 프로세스와
운영 트래픽에는 영향이 없다. STT는 호출하지 않는다 — 입력은 Ollama 측정 때 STT가 실제로 인식한 텍스트를 그대로 쓴다.

    docker cp ai-baseline wespeak-ai:/tmp/ai-baseline
    docker exec wespeak-ai python /tmp/ai-baseline/measure_claude_baseline.py          # 계획만 출력
    docker exec wespeak-ai python /tmp/ai-baseline/measure_claude_baseline.py --yes    # 실제 실행
"""

import argparse
import asyncio
import contextlib
import json
import logging
import sys
import time
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from measure_ai_baseline import BOOK_CONTENT, ESSAYS, percentile  # Ollama 측정과 같은 입력 사용

# Ollama 측정(2026-09-13)에서 STT가 인식한 텍스트
TRANSCRIPTS = {
    "short": "I went to the park with my friend yesterday.",
    "medium": "last weekend i visited my grandparents in the countryside we cooked dinner together and after that "
              "we took a long walk near the river while talking about my school life",
    "long": "I have been studying English for about three years, but speaking is still the hardest part for me. "
            "When I talk with foreigners, I often forget simple words because I get nervous. Recently I started "
            "practicing every morning by describing my plans for the day out loud, and I think my confidence is "
            "slowly getting better. My goal is to have a comfortable conversation during my trip to Canada next summer.",
}


class ProviderCounter:
    """routed_*_llm()이 실제로 내준 LLM 객체의 클래스(ChatAnthropic / ChatOllama)를 센다.

    2026-09-13 1차 측정에서는 httpx 요청 로그로 세려 했으나 이 프로세스에서는 해당 로그가 잡히지 않아
    claude_calls=0으로 기록됐다(측정 자체는 Claude로 수행됨 — 지연 특성으로 확인). 그래서 라우팅 함수를 감싸 직접 센다.
    """

    def __init__(self):
        self.claude = 0
        self.other = 0

    def wrap(self, routed):
        @contextlib.asynccontextmanager
        async def counting_routed():
            async with routed() as llm:
                # 실제 서버: ChatAnthropic / 가짜 모드(loadtest/fake-ai): FakeChatModel(provider="claude")
                if type(llm).__name__ == "ChatAnthropic" or getattr(llm, "provider", None) == "claude":
                    self.claude += 1
                else:
                    self.other += 1
                yield llm
        return counting_routed


class FallbackCounter(logging.Handler):
    def __init__(self):
        super().__init__(level=logging.WARNING)
        self.count = 0

    def emit(self, record):
        if "structured output failed" in record.getMessage():
            self.count += 1


@dataclass
class Result:
    scenario: str
    label: str
    concurrency: int
    ok: bool = False
    error: str | None = None
    ttft_ms: float | None = None    # 호출 시작 → 첫 텍스트 청크 (STT 없음)
    total_ms: float | None = None   # 호출 시작 → 마지막 청크 / 첨삭 응답
    chunk_count: int = 0
    output_chars: int = 0


def text_of(chunk) -> str:
    if isinstance(chunk, str):
        return chunk
    if isinstance(chunk, list):  # 콘텐츠 블록 리스트로 오는 경우 대비
        return "".join(part.get("text", "") if isinstance(part, dict) else str(part) for part in chunk)
    return str(chunk)


async def stream_call(stream_factory, scenario, label, concurrency) -> Result:
    result = Result(scenario=scenario, label=label, concurrency=concurrency)
    start = time.perf_counter()
    try:
        async for chunk in stream_factory():
            text = text_of(chunk)
            if not text:
                continue
            if result.ttft_ms is None:
                result.ttft_ms = (time.perf_counter() - start) * 1000
            result.chunk_count += 1
            result.output_chars += len(text)
        result.total_ms = (time.perf_counter() - start) * 1000
        result.ok = result.chunk_count > 0
        if not result.ok:
            result.error = "no text chunk"
    except Exception as e:
        result.total_ms = (time.perf_counter() - start) * 1000
        result.error = f"{type(e).__name__}: {e}"
    return result


async def correct_call(correction_service, scenario, label, concurrency, essay) -> Result:
    result = Result(scenario=scenario, label=label, concurrency=concurrency)
    start = time.perf_counter()
    try:
        output = await correction_service.correct_essay(essay)
        result.total_ms = (time.perf_counter() - start) * 1000
        result.output_chars = len(output)
        result.chunk_count = 1
        result.ok = True
    except Exception as e:
        result.total_ms = (time.perf_counter() - start) * 1000
        result.error = f"{type(e).__name__}: {e}"
    return result


def build_plan(args):
    """(시나리오 이름, 라운드 수, 동시 수, 종류, 라벨) 목록"""
    plan = [("warmup-chat", 1, 1, "chat", "short")]
    plan += [(f"chat-c1-{label}", args.runs, 1, "chat", label) for label in ("short", "medium", "long")]
    plan += [(f"chat-c{args.concurrency}-medium", args.rounds, args.concurrency, "chat", "medium")]
    plan += [("feedback-c1-medium", args.runs, 1, "feedback", "medium")]
    plan += [(f"correct-c1-{label}", args.runs, 1, "correct", label) for label in ("short", "medium", "long")]
    plan += [(f"correct-c{args.concurrency}-medium", args.rounds, args.concurrency, "correct", "medium")]
    return plan


def print_summary(scenarios):
    print("\n==================== SUMMARY (Claude) ====================")
    for scenario in scenarios:
        if scenario["name"].startswith("warmup"):
            continue
        rows = scenario["results"]
        ok_rows = [r for r in rows if r.ok]
        print(f"\n[{scenario['name']}] n={len(rows)} ok={len(ok_rows)} fail={len(rows) - len(ok_rows)}"
              f"  claude_calls={scenario['claude_calls']} other_calls={scenario['other_calls']}"
              f"  correction_fallback={scenario['fallbacks']}")
        for r in rows:
            if not r.ok:
                print(f"  FAIL: {r.error}")
        if not ok_rows:
            continue
        for metric in ("ttft_ms", "total_ms"):
            values = [getattr(r, metric) for r in ok_rows if getattr(r, metric) is not None]
            if values and not (metric == "ttft_ms" and scenario["name"].startswith("correct")):
                print(f"  {metric:<9} p50={percentile(values, 0.5):>9,.0f}ms  p95={percentile(values, 0.95):>9,.0f}ms"
                      f"  min={min(values):>9,.0f}ms  max={max(values):>9,.0f}ms")
        chars_per_sec = [r.output_chars / (r.total_ms / 1000) for r in ok_rows if r.total_ms]
        print(f"  chars p50={percentile([r.output_chars for r in ok_rows], 0.5):,.0f}"
              f"  chars/sec p50={percentile(chars_per_sec, 0.5):,.1f}"
              f"  chunks p50={percentile([r.chunk_count for r in ok_rows], 0.5):,.0f}")


async def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--app-dir", default="/app")
    parser.add_argument("--runs", type=int, default=3, help="동시 1개 시나리오 반복 횟수")
    parser.add_argument("--rounds", type=int, default=2, help="동시 N개 시나리오 라운드 수")
    parser.add_argument("--concurrency", type=int, default=3, help="동시 시나리오의 동시 요청 수")
    parser.add_argument("--out", default=str(Path(__file__).parent / "results-claude.json"))
    parser.add_argument("--yes", action="store_true", help="유료 API 호출에 동의하고 실제로 실행")
    args = parser.parse_args()

    plan = build_plan(args)
    total_calls = sum(rounds * concurrency for _, rounds, concurrency, _, _ in plan)
    print("측정 계획 (Claude API 호출):")
    for name, rounds, concurrency, kind, label in plan:
        print(f"  {name:<22} {kind:<8} rounds={rounds} concurrency={concurrency} → {rounds * concurrency}건")
    print(f"  합계 {total_calls}건 (첨삭 실패 시 fallback으로 호출이 늘어날 수 있음)")
    if not args.yes:
        print("\n실제로 실행하려면 --yes 를 붙이세요.")
        return

    sys.path.insert(0, args.app_dir)
    from app.config import settings
    if not settings.anthropic_api_key:
        print("ANTHROPIC_API_KEY가 설정돼 있지 않아 중단합니다.")
        sys.exit(1)

    # 이 프로세스에서만 Claude로 라우팅 (llm.py의 routed_*_llm은 호출 시점에 settings.provider를 읽음)
    settings.provider = "claude"
    from app.services import chat_service, correction_service, feedback_service

    logging.basicConfig(level=logging.WARNING)
    call_counter = ProviderCounter()
    # 서비스 모듈이 import해 둔 라우팅 함수 참조를 감싼다 (llm 모듈 쪽을 바꾸면 이미 import된 이름에는 반영되지 않음)
    chat_service.routed_chat_llm = call_counter.wrap(chat_service.routed_chat_llm)
    feedback_service.routed_chat_llm = call_counter.wrap(feedback_service.routed_chat_llm)
    correction_service.routed_structured_llm = call_counter.wrap(correction_service.routed_structured_llm)
    fallback_counter = FallbackCounter()
    logging.getLogger("wespeak.correction").addHandler(fallback_counter)

    def make_call(kind, scenario, input_label, display_label, concurrency):
        if kind == "chat":
            history = [{"role": "user", "content": TRANSCRIPTS[input_label]}]
            return stream_call(lambda: chat_service.chat_stream(history), scenario, display_label, concurrency)
        if kind == "feedback":
            messages = [
                {"role": "user", "content": BOOK_CONTENT},
                {"role": "assistant", "content": "Give me a summary."},
                {"role": "user", "content": TRANSCRIPTS[input_label]},
            ]
            return stream_call(lambda: feedback_service.get_feedback_stream(messages), scenario, display_label, concurrency)
        return correct_call(correction_service, scenario, display_label, concurrency, ESSAYS[input_label])

    started_at = datetime.now(timezone.utc).isoformat()
    scenarios = []
    for name, rounds, concurrency, kind, label in plan:
        print(f"\n▶ {name} (rounds={rounds}, concurrency={concurrency})", flush=True)
        claude_before, other_before, fallback_before = call_counter.claude, call_counter.other, fallback_counter.count
        results = []
        for round_index in range(rounds):
            batch = await asyncio.gather(*[
                make_call(kind, name, label, label if concurrency == 1 else f"{label}#{i}", concurrency)
                for i in range(concurrency)
            ])
            results.extend(batch)
            for r in batch:
                status = "ok" if r.ok else f"FAIL {r.error}"
                print(f"  round {round_index + 1} [{r.label}] ttft={r.ttft_ms and f'{r.ttft_ms:,.0f}ms'} "
                      f"total={r.total_ms and f'{r.total_ms:,.0f}ms'} {status}", flush=True)
            await asyncio.sleep(1)
        scenarios.append({
            "name": name,
            "results": results,
            "claude_calls": call_counter.claude - claude_before,
            "other_calls": call_counter.other - other_before,
            "fallbacks": fallback_counter.count - fallback_before,
        })

    print_summary(scenarios)
    output = {
        "measured_at": started_at,
        "claude_model": settings.claude_model,
        "scenarios": [{**s, "results": [asdict(r) for r in s["results"]]} for s in scenarios],
    }
    Path(args.out).write_text(json.dumps(output, ensure_ascii=False, indent=2))
    print(f"\n결과 저장: {args.out}")
    if call_counter.other or not call_counter.claude:
        print(f"⚠️  Claude가 아닌 LLM 사용 {call_counter.other}건 / Claude {call_counter.claude}건 — 라우팅 확인 필요")


if __name__ == "__main__":
    asyncio.run(main())
