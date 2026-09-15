import asyncio
import time

from app.services import chat_service, correction_service, feedback_service, stt_service
from app.services.correction_service import CorrectionResult
from fake import state
from fake.config import SETTINGS

SHORT_ESSAY = "I goed to the store yesterday and buyed some apple."
MEDIUM_ESSAY = ("Last summer I travel to Jeju island with my family. We stayed in a small hotel near the beach and "
                "the view was very beautiful. Every morning we eat breakfast outside and watched the sea. My brother "
                "and me tried surfing for the first time, but it was more harder than we expected.")


async def _consume_chat():
    start = time.perf_counter()
    first = None
    chunks = 0
    async for _ in chat_service.chat_stream([{"role": "user", "content": "hello"}]):
        first = first or time.perf_counter()
        chunks += 1
    return first - start, time.perf_counter() - start, chunks


def _ms(seconds, scale):
    return seconds / scale * 1000  # 배율을 되돌려 실측 단위(ms)로 비교


def _close(actual_ms, expected_ms, scale, rel=0.15, overhead_s=0.15):
    """비율 오차 + 고정 오버헤드(LangChain 호출 · 첫 호출 준비 등, 실제 시간 기준) 허용.
    시간 배율을 줄이면 고정 오버헤드가 비율상 커지므로 실제 초 단위로 따로 허용한다."""
    return abs(actual_ms - expected_ms) <= expected_ms * rel + overhead_s / scale * 1000


async def test_single_chat_stream_follows_ollama_profile(scale):
    ttft, total, chunks = await _consume_chat()
    profile = SETTINGS.ollama
    assert chunks == profile.stream_chunks
    assert _close(_ms(ttft, scale), profile.ttft_ms, scale)
    expected_total = profile.ttft_ms + profile.chunk_ms * (profile.stream_chunks - 1)
    assert _close(_ms(total, scale), expected_total, scale)
    assert state.stats.llm["ollama"].calls == 1 and state.stats.llm["claude"].calls == 0


async def test_real_routing_sends_fourth_concurrent_request_to_fake_claude():
    results = await asyncio.gather(*[_consume_chat() for _ in range(4)])
    chunk_counts = sorted(chunks for _, _, chunks in results)
    assert chunk_counts == sorted([SETTINGS.ollama.stream_chunks] * 3 + [SETTINGS.claude.stream_chunks])
    assert state.stats.llm["ollama"].max_inflight == 3
    assert state.stats.llm["claude"].calls == 1


async def test_correction_service_returns_valid_json_through_structured_output(scale):
    start = time.perf_counter()
    output = await correction_service.correct_essay(SHORT_ESSAY)
    elapsed = _ms(time.perf_counter() - start, scale)
    CorrectionResult.model_validate_json(output)
    assert _close(elapsed, SETTINGS.ollama.correct_short_ms, scale, rel=0.1)
    assert state.stats.llm["ollama"].structured_calls == 1


async def test_concurrent_corrections_slow_down_like_measurement(scale):
    async def timed():
        start = time.perf_counter()
        await correction_service.correct_essay(MEDIUM_ESSAY)
        return _ms(time.perf_counter() - start, scale)

    single = await timed()
    triple = await asyncio.gather(*[timed() for _ in range(3)])
    assert _close(single, SETTINGS.ollama.correct_ms, scale, rel=0.1)
    # 실측: 동시 3에서 3건 모두 ×1.53 (마지막 요청만 느려지면 안 됨)
    assert all(1.4 < t / single < 1.65 for t in triple), triple


async def test_claude_rate_limit_injection_raises_and_counts():
    original = SETTINGS.claude.rate_limit_rate
    SETTINGS.claude.rate_limit_rate = 1.0
    try:
        from app.services import llm
        model = llm._claude_chat
        raised = False
        try:
            async for _ in model.astream("hi"):
                pass
        except Exception as e:  # FakeRateLimitError
            raised = "429" in str(e)
        assert raised
        assert state.stats.llm["claude"].rate_limited == 1
        assert state.stats.llm["claude"].inflight == 0
    finally:
        SETTINGS.claude.rate_limit_rate = original


async def _first_chunk_ms(stream, scale):
    start = time.perf_counter()
    async for _ in stream:
        return _ms(time.perf_counter() - start, scale)


async def test_long_feedback_prompt_adds_prefill_to_ttft(scale):
    messages = [
        {"role": "user", "content": "The Amazon rainforest produces about 20% of the world's oxygen."},
        {"role": "assistant", "content": "Give me a summary."},
        {"role": "user", "content": "The Amazon makes oxygen and people cut trees."},
    ]
    ttft = await _first_chunk_ms(feedback_service.get_feedback_stream(messages), scale)
    chat_ttft = await _first_chunk_ms(chat_service.chat_stream([{"role": "user", "content": "hello"}]), scale)
    # 실측: 리딩 피드백 307ms vs 회화 193ms (few-shot이 많은 긴 프롬프트의 prefill)
    assert _close(ttft, 307, scale, rel=0.15)
    assert ttft - chat_ttft > 60


async def test_ollama_ttft_is_slower_while_stt_uses_gpu(scale):
    SETTINGS.stt_serial = False
    stt_task = asyncio.create_task(stt_service.transcribe(b"\x00" * 300_000))  # 약 1.2초짜리 STT
    await asyncio.sleep(0.01)
    ttft = await _first_chunk_ms(chat_service.chat_stream([{"role": "user", "content": "hello"}]), scale)
    await stt_task
    assert _close(ttft, SETTINGS.ollama.ttft_concurrent_ms, scale)
    assert ttft > SETTINGS.ollama.ttft_ms
