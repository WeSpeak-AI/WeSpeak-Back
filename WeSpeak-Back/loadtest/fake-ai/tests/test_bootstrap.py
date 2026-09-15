import pytest

from app.services import llm, stt_service, tts_service
from fake.models import FakeChatModel


def test_llm_objects_are_replaced_but_routing_is_real():
    for name in ("_ollama_chat", "_ollama_structured", "_claude_chat", "_claude_structured"):
        assert isinstance(getattr(llm, name), FakeChatModel)
    assert llm.routed_chat_llm.__module__ == "app.services.llm"
    assert llm.routed_structured_llm.__module__ == "app.services.llm"
    assert llm._ollama_chat.provider == "ollama" and llm._claude_structured.provider == "claude"


def test_paid_or_gpu_entry_points_are_replaced():
    import app.main as main_module
    from app.services import voca_service

    assert main_module.get_model() is None
    assert stt_service.transcribe.__module__ == "fake.stt"
    assert tts_service.text_to_speech.__module__ == "fake.stt"
    with pytest.raises(RuntimeError, match="blocked"):
        voca_service.get_image_client()


def test_claude_structured_llm_used_by_voca_is_fake():
    assert isinstance(llm.get_claude_structured_llm(), FakeChatModel)
