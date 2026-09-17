import os
import sys
from pathlib import Path

import pytest

# fake.config를 import하기 전에 시간 배율을 줄여 테스트를 빠르게 돌린다.
# 컨테이너의 FAKE_TIME_SCALE(서버용)과 무관하게 적용하며, 실제 속도로 검증하려면 FAKE_TEST_TIME_SCALE=1.0
os.environ["FAKE_TIME_SCALE"] = os.getenv("FAKE_TEST_TIME_SCALE", "0.05")
os.environ.setdefault("ANTHROPIC_API_KEY", "fake-not-used")
os.environ.setdefault("OPENAI_API_KEY", "fake-not-used")
os.environ.setdefault("OLLAMA_BASE_URL", "http://127.0.0.1:9")
os.environ["PROVIDER"] = "ollama"

AI_DIR = os.getenv("WESPEAK_AI_DIR", "/ai")
sys.path.insert(0, AI_DIR)
sys.path.insert(0, str(Path(__file__).parent.parent))
os.chdir(Path(__file__).parent.parent)  # 실제 .env를 읽지 않도록

from fake import bootstrap, state  # noqa: E402
from fake.config import SETTINGS  # noqa: E402

APP = bootstrap.install()


@pytest.fixture(autouse=True)
def clean_state():
    state.reset_stats()
    state.faults.rules.clear()
    state.faults.outages.clear()
    original_serial = SETTINGS.stt_serial
    yield
    SETTINGS.stt_serial = original_serial


@pytest.fixture
def app():
    return APP


@pytest.fixture
def scale():
    return SETTINGS.time_scale
