"""
measure_ai_baseline.py 실행 구간의 AI 서버 로그(docker logs -t)를 시나리오별로 집계한다.
표준 라이브러리만 사용하므로 로컬에서 실행해도 된다.

    python parse_ai_logs.py --results results.json --logs ai-logs.txt

서버 로그 기준 지표
- stt_elapsed_ms   : stt_service의 'transcribe done ... elapsed' (대기열 제외 순수 STT 시간)
- chat/feedback_ms : 'chat completed (stream)' / 'feedback completed (stream)' (LLM 스트리밍 전체)
- correction_ms    : 'correction completed' / 'correction fallback completed'
- LLM 호출 대상    : httpx 'HTTP Request: POST ...' 로그의 호스트로 Ollama / Claude 호출 수를 센다
"""

import argparse
import json
import re
from datetime import datetime

PATTERNS = {
    "stt_elapsed_ms": re.compile(r"transcribe done - lang=\S+ duration=(?P<audio_sec>[\d.]+)s elapsed=(?P<ms>[\d.]+)ms"),
    "chat_llm_ms": re.compile(r"chat completed \(stream\) - (?P<ms>[\d.]+)ms"),
    "feedback_llm_ms": re.compile(r"feedback completed \(stream\) - (?P<ms>[\d.]+)ms"),
    "correction_ms": re.compile(r"correction (?:fallback )?completed - (?P<ms>[\d.]+)ms"),
}
CORRECTION_FALLBACK = re.compile(r"correction structured output failed")
HTTP_REQUEST = re.compile(r"HTTP Request: POST (?P<url>\S+)")
DOCKER_TS = re.compile(r"^(?P<ts>\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})(?:\.(?P<frac>\d+))?Z\s")


def parse_ts(value: str) -> datetime:
    return datetime.fromisoformat(value.replace("Z", "+00:00"))


def parse_docker_line(line: str):
    match = DOCKER_TS.match(line)
    if not match:
        return None, line
    frac = (match.group("frac") or "0")[:6].ljust(6, "0")
    ts = datetime.fromisoformat(f"{match.group('ts')}.{frac}+00:00")
    return ts, line[match.end():]


def percentile(values, p):
    ordered = sorted(values)
    k = (len(ordered) - 1) * p
    lower, upper = int(k), min(int(k) + 1, len(ordered) - 1)
    return ordered[lower] + (ordered[upper] - ordered[lower]) * (k - lower)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--results", default="results.json")
    parser.add_argument("--logs", default="ai-logs.txt")
    args = parser.parse_args()

    results = json.loads(open(args.results, encoding="utf-8").read())
    windows = [(s["name"], parse_ts(s["started_at"]), parse_ts(s["ended_at"])) for s in results["scenarios"]]

    stats = {name: {"metrics": {}, "ollama_calls": 0, "claude_calls": 0, "other_calls": 0, "correction_fallback": 0}
             for name, _, _ in windows}
    unmatched = 0

    with open(args.logs, encoding="utf-8", errors="replace") as f:
        for raw in f:
            ts, line = parse_docker_line(raw.rstrip("\n"))
            if ts is None:
                continue
            # 시나리오 종료 시각은 마지막 라운드 후 1초 대기까지 포함하므로, 완료 로그는 구간 안에 들어온다
            scenario = next((name for name, start, end in windows if start <= ts <= end), None)
            if scenario is None:
                unmatched += 1
                continue
            bucket = stats[scenario]

            for metric, pattern in PATTERNS.items():
                m = pattern.search(line)
                if m:
                    bucket["metrics"].setdefault(metric, []).append(float(m.group("ms")))
                    if metric == "stt_elapsed_ms":
                        bucket["metrics"].setdefault("audio_sec", []).append(float(m.group("audio_sec")))
            if CORRECTION_FALLBACK.search(line):
                bucket["correction_fallback"] += 1
            m = HTTP_REQUEST.search(line)
            if m:
                url = m.group("url")
                if "anthropic.com" in url:
                    bucket["claude_calls"] += 1
                elif ":11434" in url or "/api/chat" in url or "/api/generate" in url:
                    bucket["ollama_calls"] += 1
                else:
                    bucket["other_calls"] += 1

    print("==================== SUMMARY (server logs) ====================")
    total_claude = 0
    for name, _, _ in windows:
        bucket = stats[name]
        total_claude += bucket["claude_calls"]
        print(f"\n[{name}] LLM calls: ollama={bucket['ollama_calls']} claude={bucket['claude_calls']} other={bucket['other_calls']}"
              f"  correction_fallback={bucket['correction_fallback']}")
        for metric, values in bucket["metrics"].items():
            unit = "s " if metric == "audio_sec" else "ms"
            print(f"  {metric:<16} n={len(values):<3} p50={percentile(values, 0.5):>9,.1f}{unit}"
                  f"  p95={percentile(values, 0.95):>9,.1f}{unit}  min={min(values):>9,.1f}{unit}  max={max(values):>9,.1f}{unit}")

    print(f"\n측정 구간 밖 로그 줄 수: {unmatched}")
    if total_claude:
        print(f"⚠️  Claude 호출 {total_claude}건 감지 — Ollama 슬롯 초과(다른 트래픽 포함)로 일부 요청이 Claude로 처리됨. 해당 시나리오 수치는 Ollama 기준값으로 쓰지 말 것.")
    elif not any(stats[name]["ollama_calls"] for name, _, _ in windows):
        print("ℹ️  httpx 요청 로그가 없어 LLM 호출 대상을 판별하지 못함 — 로그 레벨 확인 필요.")


if __name__ == "__main__":
    main()
