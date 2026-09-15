"""
실제 AI 서버(WeSpeak-AI)의 STT · Ollama 지연 기준값 측정 스크립트.

AI 서버 컨테이너 안에서 실행한다(grpcio · httpx · gRPC 생성 코드가 이미 설치돼 있음).
    docker cp loadtest/ai-baseline wespeak-ai:/tmp/ai-baseline
    docker exec wespeak-ai python /tmp/ai-baseline/measure_ai_baseline.py

측정 원칙
- 동시 요청은 최대 3개로 제한한다. llm.py의 Ollama 슬롯(OLLAMA_MAX_CONCURRENT=3)을 넘으면 Claude로 넘어가므로,
  이 스크립트는 Ollama 경로만 측정한다. 측정 중 다른 트래픽이 있으면 Claude로 넘어갈 수 있으니 서버가 한가할 때 실행한다.
- 시나리오는 순차 실행하며, 각 시나리오의 시작·종료 시각(UTC)을 기록해 docker logs와 대조할 수 있게 한다.
"""

import argparse
import asyncio
import json
import statistics
import sys
import time
from dataclasses import asdict, dataclass, field
from datetime import datetime, timezone
from pathlib import Path

AUDIO_CHUNK_SIZE = 64 * 1024  # Spring AiClient/FeedbackClient와 동일
MAX_CONCURRENCY = 3           # Ollama 슬롯 수. 이 값을 넘기면 Claude로 라우팅됨

BOOK_CONTENT = (
    "The Amazon rainforest, often called the 'lungs of the Earth', produces about 20% of the world's oxygen. "
    "It covers over 5.5 million square kilometers across nine countries, with Brazil containing the largest portion. "
    "Deforestation due to agriculture and logging has destroyed nearly 20% of the original forest over the past 50 years, "
    "threatening thousands of species."
)

ESSAYS = {
    "short": "I goed to the store yesterday and buyed some apple. It was very cheaper than I think.",
    "medium": (
        "Last summer I travel to Jeju island with my family. We stayed in a small hotel near the beach and "
        "the view was very beautiful. Every morning we eat breakfast outside and watched the sea. "
        "My brother and me tried surfing for the first time, but it was more harder than we expected. "
        "Although we was tired, it was one of the best trip in my life and I want to go there again."
    ),
    "long": (
        "Nowadays, many people spends a lot of time on their smartphones, and I think this habit have both "
        "advantages and disadvantages. First, smartphones makes our life more convenient. We can search information, "
        "send message to friends and even study foreign languages anywhere. For example, I use an English app every day "
        "on the subway, and my vocabulary are improving. However, using smartphones too much can be harmful. "
        "Many students cannot focus on their homework because they checks social media every few minutes. "
        "Also, looking at the screen for a long time make our eyes tired and we don't sleep well at night. "
        "In my opinion, the most important thing is to control how much time we use it. If we set a daily limit "
        "and use smartphones for useful purposes, they can be a great tool instead of a distraction."
    ),
}


def utc_now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


@dataclass
class StreamResult:
    scenario: str
    label: str
    concurrency: int
    ok: bool = False
    error: str | None = None
    stt_ms: float | None = None          # 요청 시작 → user_text_final (업로드 + STT 대기열 + STT)
    ttft_ms: float | None = None         # user_text_final → 첫 텍스트 청크 (LLM 첫 토큰)
    llm_total_ms: float | None = None    # user_text_final → 마지막 텍스트 청크
    e2e_ms: float | None = None          # 요청 시작 → 스트림 종료
    delta_count: int = 0
    output_chars: int = 0
    user_text: str = ""


@dataclass
class HttpResult:
    scenario: str
    label: str
    concurrency: int
    ok: bool = False
    error: str | None = None
    e2e_ms: float | None = None
    status: int | None = None
    output_chars: int = 0


@dataclass
class Scenario:
    name: str
    started_at: str
    ended_at: str = ""
    results: list = field(default_factory=list)


def upload_chunks(pb2, kind: str, audio: bytes, meta_value: str):
    if kind == "chat":
        yield pb2.ChatUploadChunk(metadata=pb2.ChatMetadata(history_json=meta_value))
        make = lambda b: pb2.ChatUploadChunk(audio_chunk=b)
    else:
        yield pb2.FeedbackUploadChunk(metadata=pb2.FeedbackMetadata(book_content=meta_value))
        make = lambda b: pb2.FeedbackUploadChunk(audio_chunk=b)
    for offset in range(0, len(audio), AUDIO_CHUNK_SIZE):
        yield make(audio[offset:offset + AUDIO_CHUNK_SIZE])


async def stream_call(stubs, pb2, kind: str, scenario: str, label: str, concurrency: int, audio: bytes) -> StreamResult:
    result = StreamResult(scenario=scenario, label=label, concurrency=concurrency)
    start = time.perf_counter()
    t_user = t_first = t_last = None
    try:
        if kind == "chat":
            call = stubs["chat"].Chat(upload_chunks(pb2, "chat", audio, "[]"), timeout=120)
            delta_field = "ai_text_delta"
        else:
            call = stubs["feedback"].Feedback(upload_chunks(pb2, "feedback", audio, BOOK_CONTENT), timeout=120)
            delta_field = "feedback_text_delta"

        async for chunk in call:
            now = time.perf_counter()
            which = chunk.WhichOneof("payload")
            if which == "user_text_final":
                t_user = now
                result.user_text = chunk.user_text_final
            elif which == delta_field:
                t_first = t_first or now
                t_last = now
                result.delta_count += 1
                result.output_chars += len(getattr(chunk, delta_field))
            elif which == "stream_error":
                raise RuntimeError(f"stream_error: {chunk.stream_error.message}")

        end = time.perf_counter()
        result.e2e_ms = (end - start) * 1000
        if t_user is not None:
            result.stt_ms = (t_user - start) * 1000
            if t_first is not None:
                result.ttft_ms = (t_first - t_user) * 1000
                result.llm_total_ms = (t_last - t_user) * 1000
        result.ok = t_user is not None and t_first is not None
        if not result.ok:
            result.error = "missing user_text_final or text delta"
    except Exception as e:  # 측정 스크립트이므로 실패도 결과로 기록하고 계속 진행
        result.e2e_ms = (time.perf_counter() - start) * 1000
        result.error = f"{type(e).__name__}: {e}"
    return result


async def correct_call(client, base_url: str, scenario: str, label: str, concurrency: int, essay: str) -> HttpResult:
    result = HttpResult(scenario=scenario, label=label, concurrency=concurrency)
    start = time.perf_counter()
    try:
        response = await client.post(f"{base_url}/correct", json={"content": essay}, timeout=300)
        result.e2e_ms = (time.perf_counter() - start) * 1000
        result.status = response.status_code
        result.ok = response.status_code == 200
        if result.ok:
            result.output_chars = len(response.json().get("result", ""))
        else:
            result.error = response.text[:300]
    except Exception as e:
        result.e2e_ms = (time.perf_counter() - start) * 1000
        result.error = f"{type(e).__name__}: {e}"
    return result


async def run_scenario(scenarios: list, name: str, rounds: int, concurrency: int, make_calls):
    """rounds번 반복하며, 매 라운드마다 concurrency개의 요청을 동시에 보내고 전부 끝날 때까지 기다린다."""
    assert concurrency <= MAX_CONCURRENCY, "Ollama 슬롯을 넘는 동시 요청은 Claude로 라우팅되므로 금지"
    scenario = Scenario(name=name, started_at=utc_now_iso())
    print(f"\n▶ {name} (rounds={rounds}, concurrency={concurrency}) started {scenario.started_at}", flush=True)
    for round_index in range(rounds):
        results = await asyncio.gather(*make_calls())
        scenario.results.extend(results)
        for r in results:
            status = "ok" if r.ok else f"FAIL {r.error}"
            print(f"  round {round_index + 1} [{r.label}] e2e={fmt(r.e2e_ms)} {status}", flush=True)
        await asyncio.sleep(1)  # 라운드 사이 여유 (앞 라운드의 슬롯 반환 대기)
    scenario.ended_at = utc_now_iso()
    scenarios.append(scenario)


def fmt(value):
    return "-" if value is None else f"{value:,.0f}ms"


def percentile(values: list[float], p: float) -> float:
    ordered = sorted(values)
    k = (len(ordered) - 1) * p
    lower, upper = int(k), min(int(k) + 1, len(ordered) - 1)
    return ordered[lower] + (ordered[upper] - ordered[lower]) * (k - lower)


def print_summary(scenarios: list):
    print("\n==================== SUMMARY (client-side) ====================")
    metrics_by_type = {
        StreamResult: ["stt_ms", "ttft_ms", "llm_total_ms", "e2e_ms"],
        HttpResult: ["e2e_ms"],
    }
    for scenario in scenarios:
        if scenario.name.startswith("warmup"):
            continue
        groups: dict[str, list] = {}
        for r in scenario.results:
            groups.setdefault(r.label, []).append(r)
        for label, rows in groups.items():
            ok_rows = [r for r in rows if r.ok]
            print(f"\n[{scenario.name} / {label}] n={len(rows)} ok={len(ok_rows)} fail={len(rows) - len(ok_rows)}")
            if not ok_rows:
                continue
            for metric in metrics_by_type[type(ok_rows[0])]:
                values = [getattr(r, metric) for r in ok_rows if getattr(r, metric) is not None]
                if values:
                    print(f"  {metric:<13} p50={percentile(values, 0.5):>9,.0f}ms  p95={percentile(values, 0.95):>9,.0f}ms"
                          f"  min={min(values):>9,.0f}ms  max={max(values):>9,.0f}ms")
            if isinstance(ok_rows[0], StreamResult):
                rates = [r.delta_count / (r.llm_total_ms / 1000) for r in ok_rows if r.llm_total_ms and r.llm_total_ms > 0]
                if rates:
                    print(f"  chunks/sec    p50={statistics.median(rates):>9,.1f}      (chunks={statistics.median([r.delta_count for r in ok_rows]):.0f}, chars={statistics.median([r.output_chars for r in ok_rows]):.0f})")


async def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--app-dir", default="/app", help="WeSpeak-AI 코드 경로 (gRPC 생성 코드 import용)")
    parser.add_argument("--grpc", default="localhost:50051")
    parser.add_argument("--http", default="http://localhost:8000")
    parser.add_argument("--audio-dir", default=str(Path(__file__).parent / "audio"))
    parser.add_argument("--runs", type=int, default=5, help="동시 1개 시나리오의 반복 횟수")
    parser.add_argument("--rounds", type=int, default=3, help="동시 2·3개 시나리오의 라운드 수")
    parser.add_argument("--out", default=str(Path(__file__).parent / "results.json"))
    parser.add_argument("--only", default="", help="쉼표로 구분한 시나리오 그룹만 실행: chat,feedback,correct")
    args = parser.parse_args()

    sys.path.insert(0, args.app_dir)
    import grpc
    import httpx
    from app.grpc.generated import wespeak_ai_pb2 as pb2
    from app.grpc.generated import wespeak_ai_pb2_grpc as pb2_grpc

    only = {s.strip() for s in args.only.split(",") if s.strip()}
    enabled = lambda group: not only or group in only

    audio = {name: (Path(args.audio_dir) / f"{name}.m4a").read_bytes() for name in ("short", "medium", "long")}
    scenarios: list[Scenario] = []
    measurement_started = utc_now_iso()

    async with grpc.aio.insecure_channel(args.grpc) as channel, httpx.AsyncClient() as client:
        stubs = {"chat": pb2_grpc.ChatServiceStub(channel), "feedback": pb2_grpc.FeedbackServiceStub(channel)}

        # 워밍업: 첫 호출은 모델 로딩 등으로 느리므로 집계에서 제외
        await run_scenario(scenarios, "warmup-chat", 1, 1,
                           lambda: [stream_call(stubs, pb2, "chat", "warmup-chat", "short", 1, audio["short"])])
        if enabled("correct"):
            await run_scenario(scenarios, "warmup-correct", 1, 1,
                               lambda: [correct_call(client, args.http, "warmup-correct", "short", 1, ESSAYS["short"])])

        if enabled("chat"):
            for label in ("short", "medium", "long"):
                await run_scenario(scenarios, f"chat-c1-{label}", args.runs, 1,
                                   lambda label=label: [stream_call(stubs, pb2, "chat", f"chat-c1-{label}", label, 1, audio[label])])
            for c in (2, 3):
                await run_scenario(scenarios, f"chat-c{c}-medium", args.rounds, c,
                                   lambda c=c: [stream_call(stubs, pb2, "chat", f"chat-c{c}-medium", f"medium#{i}", c, audio["medium"])
                                                for i in range(c)])

        if enabled("feedback"):
            await run_scenario(scenarios, "feedback-c1-medium", args.runs, 1,
                               lambda: [stream_call(stubs, pb2, "feedback", "feedback-c1-medium", "medium", 1, audio["medium"])])

        if enabled("correct"):
            for label in ("short", "medium", "long"):
                await run_scenario(scenarios, f"correct-c1-{label}", max(3, args.runs - 2), 1,
                                   lambda label=label: [correct_call(client, args.http, f"correct-c1-{label}", label, 1, ESSAYS[label])])
            for c in (2, 3):
                await run_scenario(scenarios, f"correct-c{c}-medium", args.rounds, c,
                                   lambda c=c: [correct_call(client, args.http, f"correct-c{c}-medium", f"medium#{i}", c, ESSAYS["medium"])
                                                for i in range(c)])

    await asyncio.sleep(3)  # 마지막 요청의 서버 로그가 기록될 여유
    measurement_ended = utc_now_iso()
    print_summary(scenarios)

    output = {
        "measurement_started_at": measurement_started,
        "measurement_ended_at": measurement_ended,
        "audio_bytes": {k: len(v) for k, v in audio.items()},
        "scenarios": [
            {"name": s.name, "started_at": s.started_at, "ended_at": s.ended_at, "results": [asdict(r) for r in s.results]}
            for s in scenarios
        ],
    }
    Path(args.out).write_text(json.dumps(output, ensure_ascii=False, indent=2))
    print(f"\n결과 저장: {args.out}")
    print("\n다음 명령으로 측정 구간의 서버 로그를 저장하세요 (호스트에서 실행):")
    print(f"  docker logs -t --since {measurement_started} --until {measurement_ended} wespeak-ai > ai-logs.txt 2>&1")


if __name__ == "__main__":
    asyncio.run(main())
