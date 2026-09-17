"""
가짜 모드 HTTP 계층: ASGI 미들웨어(미지원 경로 차단 · 오류 주입 · 요청 태그 집계 · X-Fake-AI 헤더)와 제어 API(/_fake/*).
"""

import json
import re
import time
from dataclasses import asdict

from fastapi import APIRouter
from pydantic import BaseModel, Field

from fake import state
from fake.config import SETTINGS

# 유료 외부 API(Pinecone · Upstage · OpenAI 이미지 · Claude)를 쓰거나 실험 대상이 아닌 경로
BLOCKED_PREFIXES = ("/search", "/topic", "/voca", "/admin/ingest")
TAG_PATTERN = re.compile(rb"\[req:([A-Za-z0-9_.\-]{1,64})\]")
MAX_SCAN_BYTES = 1024 * 1024


class FakeModeMiddleware:
    def __init__(self, app, ai_commit: str):
        self.app = app
        self.ai_commit = ai_commit

    async def __call__(self, scope, receive, send):
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return

        path = scope["path"]
        send = self._with_fake_header(send)
        if path.startswith("/_fake"):
            await self.app(scope, receive, send)
            return

        http_stats = state.stats.http[path]
        http_stats["requests"] += 1

        if path.startswith(BLOCKED_PREFIXES):
            http_stats["blocked"] += 1
            await _json_response(send, 501, {"detail": f"fake AI mode: {path} is not supported"})
            return

        body, receive = await _buffer_body(receive)
        tag = _extract_tag(body)
        if tag:
            state.stats.tags[tag]["received"] += 1

        fault_status = state.active_fault(path)
        if fault_status is not None:
            http_stats["injected_faults"] += 1
            if tag:
                state.stats.tags[tag]["failed"] += 1
            await _json_response(send, fault_status, {"detail": f"fake AI mode: injected fault {fault_status}"})
            return

        status_holder = {}

        async def capture_send(message):
            if message["type"] == "http.response.start":
                status_holder["status"] = message["status"]
            await send(message)

        try:
            await self.app(scope, receive, capture_send)
        finally:
            if tag:
                succeeded = status_holder.get("status", 500) < 400
                state.stats.tags[tag]["succeeded" if succeeded else "failed"] += 1

    @staticmethod
    def _with_fake_header(send):
        async def wrapped(message):
            if message["type"] == "http.response.start":
                message.setdefault("headers", [])
                message["headers"] = list(message["headers"]) + [(b"x-fake-ai", b"true")]
            await send(message)
        return wrapped


async def _buffer_body(receive):
    chunks, more = [], True
    while more:
        message = await receive()
        if message["type"] != "http.request":
            # 연결 끊김 등은 그대로 전달
            async def passthrough():
                return message
            return b"".join(chunks), passthrough
        chunks.append(message.get("body", b""))
        more = message.get("more_body", False)
    body = b"".join(chunks)
    replayed = False

    async def replay():
        nonlocal replayed
        if not replayed:
            replayed = True
            return {"type": "http.request", "body": body, "more_body": False}
        return await receive()

    return body, replay


def _extract_tag(body: bytes):
    match = TAG_PATTERN.search(body[:MAX_SCAN_BYTES])
    return match.group(1).decode() if match else None


async def _json_response(send, status: int, payload: dict):
    body = json.dumps(payload).encode()
    await send({"type": "http.response.start", "status": status,
                "headers": [(b"content-type", b"application/json"), (b"content-length", str(len(body)).encode())]})
    await send({"type": "http.response.body", "body": body})


# ── 제어 API ────────────────────────────────────────────────────────────────

class FaultRequest(BaseModel):
    path_prefix: str = "/correct"
    error_rate: float = Field(ge=0.0, le=1.0)
    status: int = 503


class OutageRequest(BaseModel):
    path_prefix: str = "/correct"
    seconds: float = Field(gt=0)


def build_router(ai_commit: str) -> APIRouter:
    router = APIRouter(prefix="/_fake")

    @router.get("/config")
    async def config():
        return {"fake_mode": True, "wespeak_ai_commit": ai_commit, "settings": asdict(SETTINGS),
                "blocked_prefixes": BLOCKED_PREFIXES}

    @router.get("/stats")
    async def stats():
        s = state.stats
        now = time.monotonic()
        return {
            "llm": {provider: asdict(p) for provider, p in s.llm.items()},
            "stt": {"calls": s.stt_calls, "waiting": s.stt_waiting, "running": s.stt_running, "max_waiting": s.stt_max_waiting},
            "http": dict(s.http),
            "tags": dict(s.tags),
            "faults": {
                "rules": [asdict(r) for r in state.faults.rules],
                "outages": {prefix: round(until - now, 1) for prefix, until in state.faults.outages.items() if until > now},
            },
        }

    @router.post("/stats/reset")
    async def reset():
        state.reset_stats()
        return {"reset": True}

    @router.put("/faults")
    async def put_fault(request: FaultRequest):
        state.faults.rules = [r for r in state.faults.rules if r.path_prefix != request.path_prefix]
        state.faults.rules.append(state.FaultRule(request.path_prefix, request.error_rate, request.status))
        return {"rules": [asdict(r) for r in state.faults.rules]}

    @router.post("/faults/outage")
    async def outage(request: OutageRequest):
        state.faults.outages[request.path_prefix] = time.monotonic() + request.seconds
        return {"outage": request.path_prefix, "seconds": request.seconds}

    @router.delete("/faults")
    async def clear_faults():
        state.faults.rules.clear()
        state.faults.outages.clear()
        return {"cleared": True}

    return router
