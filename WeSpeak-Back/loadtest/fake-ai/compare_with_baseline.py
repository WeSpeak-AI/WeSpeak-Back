"""
실측 결과와 가짜 모드 결과의 p50을 시나리오별로 비교한다 (표준 라이브러리만 사용).

    # Ollama 경로 (measure_ai_baseline.py 결과)
    python compare_with_baseline.py --real ../ai-baseline/results.json --fake ../ai-baseline/validation-results-fake.json
    # Claude 경로 (measure_claude_baseline.py 결과)
    python compare_with_baseline.py --kind claude --real ../ai-baseline/results-claude.json --fake ../ai-baseline/validation-results-claude-fake.json
"""

import argparse
import json
import statistics

METRICS = {
    "ollama": {"chat": ["stt_ms", "ttft_ms", "llm_total_ms", "e2e_ms"], "feedback": ["stt_ms", "ttft_ms", "llm_total_ms", "e2e_ms"],
               "correct": ["e2e_ms"]},
    "claude": {"chat": ["ttft_ms", "total_ms"], "feedback": ["ttft_ms", "total_ms"], "correct": ["total_ms"]},
}
# 응답 청크 수를 고정한 단순화 때문에 전체 시간이 실제 출력 길이와 달라지는 지표 (판정에서 제외하고 참고로만 표시)
LENGTH_DEPENDENT = {"llm_total_ms", "total_ms", "e2e_ms"}


def load(path):
    return {s["name"]: s["results"] for s in json.load(open(path, encoding="utf-8"))["scenarios"]}


def p50(rows, metric):
    values = [r[metric] for r in rows if r.get("ok") and r.get(metric) is not None]
    return statistics.median(values) if values else None


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--real", required=True)
    parser.add_argument("--fake", required=True)
    parser.add_argument("--kind", choices=["ollama", "claude"], default="ollama")
    parser.add_argument("--tolerance", type=float, default=0.15)
    args = parser.parse_args()

    real, fake = load(args.real), load(args.fake)
    failures = 0
    print(f"{'scenario':<22} {'metric':<13} {'real p50':>10} {'fake p50':>10} {'diff':>7}  판정")
    for name in real:
        if name.startswith("warmup") or name not in fake:
            continue
        kind = name.split("-")[0]
        length_fixed_scenario = kind in ("chat", "feedback") and not name.endswith("medium")
        for metric in METRICS[args.kind][kind]:
            r, f = p50(real[name], metric), p50(fake[name], metric)
            if r is None or f is None:
                continue
            diff = (f - r) / r
            if metric in LENGTH_DEPENDENT and kind in ("chat", "feedback") and (length_fixed_scenario or kind == "feedback"):
                verdict = "참고(청크 수 고정)"
            elif abs(diff) <= args.tolerance:
                verdict = "OK"
            else:
                verdict = "FAIL"
                failures += 1
            print(f"{name:<22} {metric:<13} {r:>10,.0f} {f:>10,.0f} {diff:>+7.0%}  {verdict}")
    print(f"\n허용 오차 ±{args.tolerance:.0%}, FAIL {failures}건")
    raise SystemExit(1 if failures else 0)


if __name__ == "__main__":
    main()
