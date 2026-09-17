import json
from pathlib import Path

import httpx
import pytest

from fake.control import FakeModeMiddleware, build_router
from fake.config import SETTINGS
from fake import state


@pytest.fixture
async def client(app):
    if not any(getattr(r, "path", "").startswith("/_fake") for r in app.routes):
        app.include_router(build_router("test"))
    transport = httpx.ASGITransport(app=FakeModeMiddleware(app, "test"))
    async with httpx.AsyncClient(transport=transport, base_url="http://fake") as c:
        yield c


async def test_health_has_fake_header(client):
    response = await client.get("/health")
    assert response.status_code == 200
    assert response.headers["x-fake-ai"] == "true"


@pytest.mark.parametrize("path", ["/search", "/topic", "/voca", "/voca/word/image", "/admin/ingest/voca"])
async def test_paid_or_out_of_scope_paths_are_blocked(client, path):
    response = await client.post(path, json={})
    assert response.status_code == 501
    assert state.stats.llm["claude"].calls == 0 and state.stats.llm["ollama"].calls == 0


async def test_correct_endpoint_returns_real_router_response_and_counts_tag(client):
    response = await client.post("/correct", json={"content": "I goed to school. [req:essay-1]"})
    assert response.status_code == 200
    result = json.loads(response.json()["result"])
    assert result["overallScore"] == 70
    assert state.stats.tags["essay-1"] == {"received": 1, "succeeded": 1, "failed": 0}


async def test_error_rate_fault_injects_5xx_before_llm(client):
    await client.put("/_fake/faults", json={"path_prefix": "/correct", "error_rate": 1.0, "status": 503})
    response = await client.post("/correct", json={"content": "text [req:essay-2]"})
    assert response.status_code == 503
    assert state.stats.tags["essay-2"] == {"received": 1, "succeeded": 0, "failed": 1}
    assert state.stats.llm["ollama"].calls == 0
    assert state.stats.http["/correct"]["injected_faults"] == 1


async def test_outage_then_recovery(client):
    await client.post("/_fake/faults/outage", json={"path_prefix": "/correct", "seconds": 30})
    assert (await client.post("/correct", json={"content": "x [req:essay-3]"})).status_code == 503
    stats = (await client.get("/_fake/stats")).json()
    assert "/correct" in stats["faults"]["outages"]

    await client.delete("/_fake/faults")
    assert (await client.post("/correct", json={"content": "x [req:essay-3]"})).status_code == 200
    assert state.stats.tags["essay-3"] == {"received": 2, "succeeded": 1, "failed": 1}


async def test_rest_chat_uses_fake_stt_llm_and_tts(client):
    files = {"file": ("a.m4a", b"\x00" * 30_000, "audio/mp4")}
    response = await client.post("/chat", files=files, data={"history": "[]"})
    assert response.status_code == 200
    body = response.json()
    assert body["user_text"] == SETTINGS.stt_text
    assert body["ai_text"] == SETTINGS.ollama.chunk_text * SETTINGS.ollama.stream_chunks
    assert body["audio_data"] == ""


async def test_config_endpoint_exposes_settings(client):
    body = (await client.get("/_fake/config")).json()
    assert body["fake_mode"] is True
    assert body["settings"]["ollama"]["correct_ms"] == SETTINGS.ollama.correct_ms


def _package_names(path: Path) -> set[str]:
    names = set()
    for line in path.read_text().splitlines():
        line = line.split("#", 1)[0].strip()
        if line:
            names.add(line.split("[")[0].split(">")[0].split("=")[0].split("<")[0].strip().lower())
    return names


def test_requirements_in_sync_with_real_ai_server():
    real = Path("/ai/requirements.txt")
    if not real.exists():
        pytest.skip("WeSpeak-AI requirements.txt not mounted")
    gpu_only = {"faster-whisper", "nvidia-cublas-cu12", "nvidia-cudnn-cu12"}
    fake_only = {"httpx"}
    ours = _package_names(Path(__file__).parent.parent / "requirements.txt") - fake_only
    assert _package_names(real) - gpu_only == ours
