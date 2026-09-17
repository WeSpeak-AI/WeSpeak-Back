"""stt_service.transcribe / tts_service.text_to_speech 대체."""

import asyncio
import logging
import time

from fake import state
from fake.config import SETTINGS, scaled

logger = logging.getLogger("AI.stt")  # 실제 stt_service와 같은 로거 · 로그 형식 (parse_ai_logs.py 호환)


def _stt_delay_ms(audio_bytes: bytes) -> tuple[float, float]:
    audio_sec = len(audio_bytes) / SETTINGS.stt_bytes_per_sec
    delay_ms = SETTINGS.stt_base_ms + SETTINGS.stt_per_audio_sec_ms * audio_sec
    if state.inflight("ollama") > 0:
        delay_ms *= 1 + SETTINGS.stt_gpu_contention
    return audio_sec, delay_ms


def _set_running(delta: int, waiting_delta: int = 0):
    def update():
        state.stats.stt_running += delta
        state.stats.stt_waiting += waiting_delta
    state.with_lock(update)


def _transcribe_blocking(audio_bytes: bytes) -> str:
    _set_running(+1, waiting_delta=-1)
    start = time.perf_counter()
    try:
        audio_sec, delay_ms = _stt_delay_ms(audio_bytes)  # 실행 시작 시점 기준 (대기열 제외)
        time.sleep(scaled(delay_ms))
    finally:
        _set_running(-1)
    logger.info("transcribe done - lang=en duration=%.1fs elapsed=%.1fms chars=%d",
                audio_sec, (time.perf_counter() - start) * 1000, len(SETTINGS.stt_text))
    return SETTINGS.stt_text


def make_transcribe(stt_service_module):
    executor = stt_service_module._executor  # 실제 서비스의 ThreadPoolExecutor(max_workers=1)

    async def transcribe(audio_bytes: bytes) -> str:
        if not audio_bytes:
            return ""

        def count():
            state.stats.stt_calls += 1
            state.stats.stt_waiting += 1
            state.stats.stt_max_waiting = max(state.stats.stt_max_waiting, state.stats.stt_waiting)
        state.with_lock(count)

        if SETTINGS.stt_serial:
            return await asyncio.get_running_loop().run_in_executor(executor, _transcribe_blocking, audio_bytes)

        _set_running(+1, waiting_delta=-1)
        start = time.perf_counter()
        try:
            audio_sec, delay_ms = _stt_delay_ms(audio_bytes)
            await asyncio.sleep(scaled(delay_ms))
        finally:
            _set_running(-1)
        logger.info("transcribe done - lang=en duration=%.1fs elapsed=%.1fms chars=%d",
                    audio_sec, (time.perf_counter() - start) * 1000, len(SETTINGS.stt_text))
        return SETTINGS.stt_text

    return transcribe


async def text_to_speech(text: str) -> bytes:
    await asyncio.sleep(scaled(SETTINGS.tts_ms))
    return b""


async def text_to_speech_stream(text: str):
    await asyncio.sleep(scaled(SETTINGS.tts_ms))
    return
    yield  # noqa: async generator
