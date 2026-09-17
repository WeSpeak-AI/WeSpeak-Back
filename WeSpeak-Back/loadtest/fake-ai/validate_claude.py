"""
가짜 모드에서 Claude 경로 지연이 실측과 비슷한지 확인한다.
measure_claude_baseline.py를 그대로 쓰되, 먼저 가짜 교체를 적용한 뒤 실행한다 (별도 프로세스라 서버와 무관).

    docker exec wespeak-ai python validate_claude.py --yes --out /tmp/results-claude-fake.json
"""

import asyncio
import os
import sys
from pathlib import Path

AI_DIR = os.getenv("WESPEAK_AI_DIR", "/ai")
BASELINE_DIR = os.getenv("AI_BASELINE_DIR", "/loadtest/ai-baseline")
sys.path.insert(0, AI_DIR)
sys.path.insert(0, str(Path(__file__).parent))
sys.path.insert(0, BASELINE_DIR)

from fake import bootstrap  # noqa: E402

bootstrap.install()

import measure_claude_baseline  # noqa: E402

if __name__ == "__main__":
    sys.argv = [sys.argv[0], "--app-dir", AI_DIR, *sys.argv[1:]]
    asyncio.run(measure_claude_baseline.main())
