"""
가짜 모드 파라미터. 기본값은 2026-09-13 실제 AI 서버 측정값
(specs/005-portfolio-tech-writeup/measurement-plan.md 5장)이며, 전부 FAKE_* 환경변수로 덮어쓸 수 있다.
"""

import os
from dataclasses import dataclass, field, fields


def _env(name: str, default, cast):
    raw = os.getenv(name)
    if raw is None or raw == "":
        return default
    if cast is bool:
        return raw.strip().lower() in ("1", "true", "yes", "on")
    return cast(raw)


@dataclass
class LlmProfile:
    ttft_ms: float                # 첫 청크까지 대기 (기준 길이 프롬프트)
    ttft_concurrent_ms: float     # GPU를 다른 요청(LLM 생성 · STT)과 나눠 쓰는 중일 때의 TTFT
    chunk_ms: float               # 청크 간격 (동시 1)
    chunk_slowdown: float         # 동시 수 1 증가당 청크 간격 증가 비율
    stream_chunks: int            # 스트리밍 응답 청크 수 (고정)
    chunk_text: str               # 청크 1개의 텍스트
    correct_short_ms: float       # 짧은 에세이 첨삭 소요
    correct_ms: float             # 그 외 에세이 첨삭 소요
    correct_slowdown: float       # 동시 수 1 증가당 첨삭 소요 증가 비율
    prefill_ms_per_1k_chars: float = 0.0  # 프롬프트가 기준 길이보다 1,000자 길어질 때마다 TTFT 증가량
    rate_limit_rate: float = 0.0  # 호출 시 429 오류를 낼 확률 (Claude용)


@dataclass
class FakeSettings:
    time_scale: float = 1.0
    seed: int = 42

    stt_serial: bool = True                 # True: 실제 stt_service._executor(max_workers=1)에서 대기 → 대기열 재현
    stt_base_ms: float = 290.0
    stt_per_audio_sec_ms: float = 37.0
    stt_bytes_per_sec: float = 12_400.0     # m4a(AAC 128kbps) 기준 음성 1초당 바이트
    stt_gpu_contention: float = 0.25        # Ollama가 생성 중일 때 STT 지연 증가 비율
    stt_text: str = ("last weekend i visited my grandparents in the countryside we cooked dinner together "
                     "and after that we took a long walk near the river while talking about my school life")

    tts_ms: float = 0.0
    correct_short_threshold_chars: int = 250  # 첨삭 프롬프트의 사용자 메시지 길이가 이 값 미만이면 짧은 에세이
    ttft_reference_prompt_chars: int = 600    # ttft_ms를 측정한 회화 프롬프트 길이 (이보다 길면 prefill 지연 추가)

    ollama: LlmProfile = field(default_factory=lambda: LlmProfile(
        ttft_ms=190, ttft_concurrent_ms=275, chunk_ms=57, chunk_slowdown=0.09, stream_chunks=35, chunk_text="word ",
        correct_short_ms=16_200, correct_ms=33_000, correct_slowdown=0.265,
        prefill_ms_per_1k_chars=6.8,  # 리딩 피드백(17,464자) TTFT 307ms vs 회화(598자) 193ms
    ))
    claude: LlmProfile = field(default_factory=lambda: LlmProfile(
        ttft_ms=550, ttft_concurrent_ms=550, chunk_ms=25, chunk_slowdown=0.0, stream_chunks=20, chunk_text="words here ",
        correct_short_ms=3_900, correct_ms=5_500, correct_slowdown=0.035, rate_limit_rate=0.0,
    ))

    def profile(self, provider: str) -> LlmProfile:
        return self.ollama if provider == "ollama" else self.claude


def load_settings() -> FakeSettings:
    settings = FakeSettings()
    for f in fields(FakeSettings):
        if f.name in ("ollama", "claude"):
            continue
        setattr(settings, f.name, _env(f"FAKE_{f.name.upper()}", getattr(settings, f.name), type(getattr(settings, f.name))))
    for provider in ("ollama", "claude"):
        profile = settings.profile(provider)
        for f in fields(LlmProfile):
            current = getattr(profile, f.name)
            setattr(profile, f.name, _env(f"FAKE_{provider.upper()}_{f.name.upper()}", current, type(current)))
    return settings


SETTINGS = load_settings()


def scaled(ms: float) -> float:
    """밀리초를 time_scale이 적용된 초로 변환"""
    return ms / 1000.0 * SETTINGS.time_scale
