# 가짜 모드 AI 서버 이미지. 실제 WeSpeak-AI 코드는 이미지에 넣지 않고 실행 시 /ai에 읽기 전용으로 마운트한다.
FROM python:3.12-slim

WORKDIR /fake

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY fake ./fake
COPY fake_main.py validate_claude.py pytest.ini ./
COPY tests ./tests

ENV WESPEAK_AI_DIR=/ai \
    PYTHONUNBUFFERED=1

EXPOSE 8000
EXPOSE 50051

CMD ["python", "fake_main.py"]
