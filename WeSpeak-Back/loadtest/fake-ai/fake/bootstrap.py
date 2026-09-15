"""
실제 WeSpeak-AI 코드를 수정하지 않고 LLM · STT · TTS · 이미지 클라이언트만 가짜로 교체한다.

순서가 중요하다:
1. app.services.llm / stt_service / tts_service를 먼저 import해 교체한다.
2. 그 뒤 app.main을 import한다 — main.py는 `from app.services.stt_service import get_model`로 이름을 직접
   가져가고, voca_service는 `get_image_client`를 직접 가져가므로 교체 후에 import돼야 가짜가 바인딩된다.
"""

import importlib
import logging
import os
import sys
from pathlib import Path

logger = logging.getLogger("fake.bootstrap")

REQUIRED = {
    "app.services.llm": ["_ollama_chat", "_ollama_structured", "_claude_chat", "_claude_structured",
                         "routed_chat_llm", "routed_structured_llm", "get_image_client"],
    "app.services.stt_service": ["transcribe", "get_model", "_executor"],
    "app.services.tts_service": ["text_to_speech", "text_to_speech_stream"],
}


class BootstrapError(RuntimeError):
    pass


def read_ai_commit(ai_dir: str) -> str:
    git = Path(ai_dir) / ".git"
    try:
        head = (git / "HEAD").read_text().strip()
        if not head.startswith("ref:"):
            return head
        ref = head.split(" ", 1)[1]
        ref_file = git / ref
        branch = ref.removeprefix("refs/heads/")
        if ref_file.exists():
            return f"{branch}@{ref_file.read_text().strip()[:12]}"
        packed = git / "packed-refs"
        if packed.exists():
            for line in packed.read_text().splitlines():
                if line.endswith(ref):
                    return f"{branch}@{line.split()[0][:12]}"
        return f"{ref} (commit unknown)"
    except OSError:
        return "unknown (.git not mounted)"


def _blocked_image_client():
    raise RuntimeError("fake AI mode: image generation (OpenAI) is blocked")


_installed = False


def install():
    """교체를 적용하고 app.main의 FastAPI app을 돌려준다. 여러 번 호출해도 한 번만 적용된다."""
    global _installed

    modules = {}
    for module_name, attributes in REQUIRED.items():
        module = importlib.import_module(module_name)
        missing = [a for a in attributes if not hasattr(module, a)]
        if missing:
            raise BootstrapError(f"{module_name}에 교체 대상 {missing}이 없습니다. "
                                 f"WeSpeak-AI 코드 구조가 바뀌었으니 fake-ai를 갱신하세요.")
        modules[module_name] = module

    if "app.main" in sys.modules and not _installed:
        raise BootstrapError("app.main이 교체 전에 import됐습니다. install()을 app.main보다 먼저 호출하세요.")

    if not _installed:
        from fake.models import fake_models
        from fake.stt import make_transcribe, text_to_speech, text_to_speech_stream

        llm = modules["app.services.llm"]
        for name, model in fake_models().items():
            setattr(llm, name, model)
        llm.get_image_client = _blocked_image_client

        stt = modules["app.services.stt_service"]
        stt.transcribe = make_transcribe(stt)
        stt.get_model = lambda: None

        tts = modules["app.services.tts_service"]
        tts.text_to_speech = text_to_speech
        tts.text_to_speech_stream = text_to_speech_stream
        _installed = True

    from app.main import app  # noqa: E402 — 교체 후 import (위 docstring 참고)
    _verify(modules)
    return app


def _verify(modules):
    llm = modules["app.services.llm"]
    for name in ("_ollama_chat", "_ollama_structured", "_claude_chat", "_claude_structured"):
        if type(getattr(llm, name)).__name__ != "FakeChatModel":
            raise BootstrapError(f"llm.{name}이 가짜 모델로 교체되지 않았습니다")
    if llm.routed_chat_llm.__module__ != "app.services.llm":
        raise BootstrapError("routed_chat_llm이 실제 코드가 아닙니다")
    import app.main as main_module
    if main_module.get_model is not modules["app.services.stt_service"].get_model:
        raise BootstrapError("app.main의 get_model이 가짜로 바인딩되지 않았습니다 (whisper 로드 위험)")
    if os.getenv("ANTHROPIC_API_KEY", "").startswith("sk-ant-"):
        raise BootstrapError("가짜 모드 컨테이너에 실제 Anthropic 키로 보이는 값이 있습니다. 제거하세요.")
