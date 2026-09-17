"""
실제 ChatOllama / ChatAnthropic 대신 llm.py의 전역 LLM 객체 자리에 들어가는 가짜 채팅 모델.

LangChain BaseChatModel을 상속하므로 서비스 코드의 `PROMPT | llm`, `astream`, `ainvoke`,
`with_structured_output(...)`이 실제와 같은 경로로 동작한다. 지연은 fake.config의 측정 기반 프로필을 따른다.
"""

import asyncio
from typing import Any, AsyncIterator, Iterator, Optional

from langchain_core.language_models.chat_models import BaseChatModel
from langchain_core.messages import AIMessage, AIMessageChunk, BaseMessage, HumanMessage
from langchain_core.outputs import ChatGeneration, ChatGenerationChunk, ChatResult
from langchain_core.runnables import RunnableLambda

from fake import state
from fake.config import SETTINGS, scaled


class FakeRateLimitError(Exception):
    """Claude 429를 흉내 내는 오류 (rate_limit_rate > 0일 때만 발생)"""


class FakeChatModel(BaseChatModel):
    provider: str  # "ollama" | "claude"
    role: str      # "chat" | "structured" — 원래 대체한 객체 이름(표시용)

    @property
    def _llm_type(self) -> str:
        return f"fake-{self.provider}"

    # ── 스트리밍 / 일반 호출 ─────────────────────────────────────────────

    def _maybe_rate_limit(self):
        profile = SETTINGS.profile(self.provider)
        if profile.rate_limit_rate > 0 and state.rng.random() < profile.rate_limit_rate:
            def count():
                state.stats.llm[self.provider].rate_limited += 1
            state.with_lock(count)
            raise FakeRateLimitError(f"fake {self.provider} 429 rate_limit_error")

    async def _astream(self, messages: list[BaseMessage], stop: Optional[list[str]] = None,
                       run_manager: Any = None, **kwargs: Any) -> AsyncIterator[ChatGenerationChunk]:
        profile = SETTINGS.profile(self.provider)
        prompt_chars = sum(len(str(m.content)) for m in messages)
        with state.Inflight(self.provider, "stream") as concurrent:
            self._maybe_rate_limit()
            # Ollama는 STT와 같은 GPU를 쓰므로 STT 실행 중에도 첫 토큰이 늦어진다 (실측: 동시 2의 첫 요청 TTFT 증가)
            sharing_gpu = concurrent > 1 or (self.provider == "ollama" and state.stats.stt_running > 0)
            ttft = profile.ttft_concurrent_ms if sharing_gpu else profile.ttft_ms
            extra_prompt = max(0, prompt_chars - SETTINGS.ttft_reference_prompt_chars)
            ttft += profile.prefill_ms_per_1k_chars * extra_prompt / 1000
            await asyncio.sleep(scaled(ttft))
            for index in range(profile.stream_chunks):
                if index > 0:
                    # 매 청크마다 현재 동시 수를 다시 읽어, 도중에 요청이 늘거나 줄어도 반영한다
                    current = state.inflight(self.provider)
                    await asyncio.sleep(scaled(profile.chunk_ms * (1 + profile.chunk_slowdown * (current - 1))))
                chunk = ChatGenerationChunk(message=AIMessageChunk(content=profile.chunk_text))
                if run_manager is not None:
                    await run_manager.on_llm_new_token(profile.chunk_text, chunk=chunk)
                yield chunk

    async def _agenerate(self, messages: list[BaseMessage], stop: Optional[list[str]] = None,
                         run_manager: Any = None, **kwargs: Any) -> ChatResult:
        parts = [chunk.message.content async for chunk in self._astream(messages, stop, run_manager, **kwargs)]
        return ChatResult(generations=[ChatGeneration(message=AIMessage(content="".join(parts)))])

    def _generate(self, messages: list[BaseMessage], stop: Optional[list[str]] = None,
                  run_manager: Any = None, **kwargs: Any) -> ChatResult:
        raise NotImplementedError("fake mode supports async calls only (AI 서버 서비스 코드는 전부 async)")

    def _stream(self, *args, **kwargs) -> Iterator[ChatGenerationChunk]:
        raise NotImplementedError("fake mode supports async calls only")

    # ── 구조화 출력 (첨삭) ───────────────────────────────────────────────

    def with_structured_output(self, schema: Any, **kwargs: Any):
        builder = STRUCTURED_BUILDERS.get(getattr(schema, "__name__", ""))
        if builder is None:
            raise NotImplementedError(f"fake mode does not support structured output schema: {schema}")

        async def invoke(prompt_value: Any) -> Any:
            profile = SETTINGS.profile(self.provider)
            user_chars = _last_human_chars(prompt_value)
            base = profile.correct_short_ms if user_chars < SETTINGS.correct_short_threshold_chars else profile.correct_ms
            with state.Inflight(self.provider, "structured"):
                self._maybe_rate_limit()
                await _work(scaled(base), lambda: 1 + profile.correct_slowdown * (state.inflight(self.provider) - 1))
            return builder(schema)

        return RunnableLambda(invoke)


async def _work(amount_seconds: float, slowdown_factor) -> None:
    """동시 1 기준 amount_seconds만큼의 작업을, 진행 중 매 구간의 동시 수에 따른 속도로 처리한다.

    시작 시점의 동시 수만 쓰면 동시에 들어온 요청 중 마지막 것만 느려진다. 실측(동시 3에서 3건 모두 ×1.53)과
    맞추려면 처리 도중 늘거나 줄어드는 동시 수를 계속 반영해야 한다.
    """
    remaining = amount_seconds
    step = max(amount_seconds / 50, 0.005)
    while remaining > 1e-9:
        factor = slowdown_factor()
        wall = min(step, remaining * factor)
        await asyncio.sleep(wall)
        remaining -= wall / factor


def _last_human_chars(prompt_value: Any) -> int:
    messages = prompt_value.to_messages() if hasattr(prompt_value, "to_messages") else []
    humans = [m for m in messages if isinstance(m, HumanMessage)]
    if humans:
        return len(str(humans[-1].content))
    return len(str(prompt_value))


def _correction_result(schema: Any) -> Any:
    """실측 출력(약 1,600자 JSON)과 비슷한 크기의 고정 첨삭 결과"""
    corrections = [
        {"error": f"sample error {i}", "correct": f"sample correction {i}",
         "explanation": "This is a fixed explanation generated by the fake AI mode for load testing purposes."}
        for i in range(1, 9)
    ]
    return schema(
        overallScore=70,
        corrections=corrections,
        suggestions=[
            "Try to use a variety of sentence structures to make your writing more engaging.",
            "Double-check irregular verb forms before submitting.",
            "Adding transition words can improve the flow between sentences.",
        ],
    )


STRUCTURED_BUILDERS = {"CorrectionResult": _correction_result}


def fake_models() -> dict[str, FakeChatModel]:
    return {
        "_ollama_chat": FakeChatModel(provider="ollama", role="chat"),
        "_ollama_structured": FakeChatModel(provider="ollama", role="structured"),
        "_claude_chat": FakeChatModel(provider="claude", role="chat"),
        "_claude_structured": FakeChatModel(provider="claude", role="structured"),
    }

