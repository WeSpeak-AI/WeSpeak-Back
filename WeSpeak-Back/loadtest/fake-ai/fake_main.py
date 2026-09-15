"""
가짜 모드 AI 서버 진입점.

실제 WeSpeak-AI의 app/ 디렉터리를 WESPEAK_AI_DIR(기본 /ai)에 읽기 전용으로 마운트해 두고 실행한다.
실제 라우터 · gRPC servicer · 서비스 · 라우팅 코드는 그대로 쓰고, LLM · STT · TTS만 fake/ 구현으로 교체한다.
"""

import logging
import os
import sys
from pathlib import Path

AI_DIR = os.getenv("WESPEAK_AI_DIR", "/ai")
sys.path.insert(0, AI_DIR)
sys.path.insert(0, str(Path(__file__).parent))

import uvicorn  # noqa: E402

from fake import bootstrap  # noqa: E402
from fake.config import SETTINGS  # noqa: E402
from fake.control import FakeModeMiddleware, build_router  # noqa: E402


def create_app():
    app = bootstrap.install()  # app.main import 시 실제 setup_logging()이 실행된다
    ai_commit = bootstrap.read_ai_commit(AI_DIR)
    app.include_router(build_router(ai_commit))
    logger = logging.getLogger("fake.main")
    logger.warning("=" * 72)
    logger.warning("FAKE AI MODE — LLM/STT/TTS are simulated. Not for production traffic.")
    logger.warning("wespeak-ai code: %s  time_scale=%s  stt_serial=%s", ai_commit, SETTINGS.time_scale, SETTINGS.stt_serial)
    logger.warning("=" * 72)
    return FakeModeMiddleware(app, ai_commit)


if __name__ == "__main__":
    os.chdir(Path(__file__).parent)  # app/config.py의 env_file=".env"가 실제 .env를 읽지 않도록
    uvicorn.run(create_app(), host="0.0.0.0", port=int(os.getenv("PORT", "8000")))
