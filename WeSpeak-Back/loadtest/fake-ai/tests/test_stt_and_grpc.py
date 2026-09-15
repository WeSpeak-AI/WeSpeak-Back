import asyncio
import time

from app.grpc.chat_servicer import ChatServicer
from app.grpc.generated import wespeak_ai_pb2
from app.services import stt_service
from fake import state
from fake.config import SETTINGS

MEDIUM_AUDIO = b"\x00" * 112_936  # 측정에 쓴 9.1초 m4a와 같은 크기


def _expected_stt_ms(audio: bytes) -> float:
    return SETTINGS.stt_base_ms + SETTINGS.stt_per_audio_sec_ms * len(audio) / SETTINGS.stt_bytes_per_sec


async def _timed_transcribe(scale):
    start = time.perf_counter()
    text = await stt_service.transcribe(MEDIUM_AUDIO)
    return (time.perf_counter() - start) / scale * 1000, text


async def test_serial_stt_reproduces_queue(scale):
    SETTINGS.stt_serial = True
    results = await asyncio.gather(*[_timed_transcribe(scale) for _ in range(3)])
    finish = sorted(ms for ms, _ in results)
    one = _expected_stt_ms(MEDIUM_AUDIO)  # 약 627ms
    for index, value in enumerate(finish, start=1):
        assert abs(value - one * index) / (one * index) < 0.15
    assert state.stats.stt_calls == 3 and state.stats.stt_max_waiting == 3
    assert all(text == SETTINGS.stt_text for _, text in results)


async def test_bypass_stt_runs_in_parallel(scale):
    SETTINGS.stt_serial = False
    results = await asyncio.gather(*[_timed_transcribe(scale) for _ in range(3)])
    one = _expected_stt_ms(MEDIUM_AUDIO)
    assert all(abs(ms - one) / one < 0.15 for ms, _ in results)


async def test_empty_audio_returns_empty_text_without_delay():
    assert await stt_service.transcribe(b"") == ""
    assert state.stats.stt_calls == 0


async def _upload():
    yield wespeak_ai_pb2.ChatUploadChunk(metadata=wespeak_ai_pb2.ChatMetadata(history_json="[]"))
    yield wespeak_ai_pb2.ChatUploadChunk(audio_chunk=MEDIUM_AUDIO)


async def test_real_grpc_chat_servicer_streams_through_fake_stt_and_llm():
    chunks = [chunk async for chunk in ChatServicer().Chat(_upload(), context=None)]
    kinds = [c.WhichOneof("payload") for c in chunks]
    assert kinds[0] == "user_text_final"
    assert chunks[0].user_text_final == SETTINGS.stt_text
    assert kinds[1:] == ["ai_text_delta"] * SETTINGS.ollama.stream_chunks
