"""가짜 모드의 런타임 상태: LLM 동시 처리 수, 호출 집계, 오류 주입 규칙, 요청 태그 집계."""

import random
import threading
import time
from collections import defaultdict
from dataclasses import dataclass, field

from fake.config import SETTINGS

_lock = threading.Lock()
rng = random.Random(SETTINGS.seed)


@dataclass
class ProviderStats:
    calls: int = 0
    stream_calls: int = 0
    structured_calls: int = 0
    rate_limited: int = 0
    inflight: int = 0
    max_inflight: int = 0


@dataclass
class Stats:
    llm: dict = field(default_factory=lambda: {"ollama": ProviderStats(), "claude": ProviderStats()})
    stt_calls: int = 0
    stt_waiting: int = 0
    stt_running: int = 0
    stt_max_waiting: int = 0
    http: dict = field(default_factory=lambda: defaultdict(lambda: {"requests": 0, "injected_faults": 0, "blocked": 0}))
    tags: dict = field(default_factory=lambda: defaultdict(lambda: {"received": 0, "succeeded": 0, "failed": 0}))


stats = Stats()


@dataclass
class FaultRule:
    path_prefix: str
    error_rate: float
    status: int = 503


@dataclass
class Faults:
    rules: list = field(default_factory=list)
    outages: dict = field(default_factory=dict)  # path_prefix -> monotonic 종료 시각


faults = Faults()


def reset_stats():
    global stats
    with _lock:
        inflight = {p: s.inflight for p, s in stats.llm.items()}
        stats = Stats()
        for provider, count in inflight.items():  # 진행 중인 요청은 유지
            stats.llm[provider].inflight = count


class Inflight:
    """같은 제공자(= 같은 GPU 또는 같은 API)에서 동시에 생성 중인 요청 수를 센다."""

    def __init__(self, provider: str, kind: str):
        self.provider = provider
        self.kind = kind

    def __enter__(self) -> int:
        with _lock:
            s = stats.llm[self.provider]
            s.calls += 1
            if self.kind == "stream":
                s.stream_calls += 1
            else:
                s.structured_calls += 1
            s.inflight += 1
            s.max_inflight = max(s.max_inflight, s.inflight)
            return s.inflight

    def __exit__(self, *exc):
        with _lock:
            stats.llm[self.provider].inflight -= 1
        return False


def inflight(provider: str) -> int:
    return stats.llm[provider].inflight


def active_fault(path: str):
    """(status) 또는 None. 장애 모드가 우선, 그다음 오류 비율."""
    now = time.monotonic()
    for prefix, until in list(faults.outages.items()):
        if until <= now:
            faults.outages.pop(prefix, None)
        elif path.startswith(prefix):
            return 503
    for rule in faults.rules:
        if path.startswith(rule.path_prefix) and rng.random() < rule.error_rate:
            return rule.status
    return None


def with_lock(fn):
    with _lock:
        return fn()
