# fake-ai — WeSpeak-AI 가짜 모드 (부하 실험용)

실제 **WeSpeak-AI 코드를 수정 · 복사하지 않고**, 실행 시 `app/`을 읽기 전용으로 마운트한 뒤 LLM · STT · TTS만 가짜로 교체해
GPU · 유료 API 없이 측정 기반 지연을 재현하는 AI 서버다. 기준값은 `specs/005-portfolio-tech-writeup/measurement-plan.md` 5장.

## 무엇이 실제이고 무엇이 가짜인가

| 실제 코드 그대로 | 가짜로 교체 (`fake/`) |
|---|---|
| FastAPI 라우터, gRPC servicer(Chat · Feedback), 서비스(프롬프트 · structured output · fallback), **Ollama 3슬롯 → Claude 라우팅**(`routed_*_llm`) | `llm.py`의 LLM 객체 4개(`_ollama_chat` · `_ollama_structured` · `_claude_chat` · `_claude_structured`), `stt_service.transcribe` · `get_model`, `tts_service`, `get_image_client`(차단) |

- 교체는 `fake/bootstrap.py`가 `app.main` import **전에** 수행한다. 교체 대상 이름이 원본에 없으면 시작 단계에서 실패한다.
- 컨테이너에는 실제 키가 없다(`ANTHROPIC_API_KEY=fake-not-used`). 교체가 빠진 곳이 있어도 유료 호출은 인증 실패로 끝난다.
- `/search` · `/topic` · `/voca*` · `/admin/ingest*`는 501 (유료 외부 API 사용 또는 실험 범위 밖).
- 모든 HTTP 응답에 `X-Fake-AI: true` 헤더, 시작 로그에 경고 배너와 마운트한 WeSpeak-AI 브랜치@커밋을 남긴다.

## 실행

```bash
cd loadtest/fake-ai
docker network create wespeak-ai_default   # 없을 때만 (WeSpeak-Back compose와 공유하는 external 네트워크)
docker compose up -d --build               # 컨테이너 이름 wespeak-ai, 포트 8000(REST) · 50051(gRPC)
curl -i localhost:8000/health              # x-fake-ai: true
curl localhost:8000/_fake/config           # 적용된 파라미터 · WeSpeak-AI 커밋
```

- WeSpeak-AI 경로가 기본 구조(`WeSpeak/WeSpeak-AI`)와 다르면 `WESPEAK_AI_DIR=/path/to/WeSpeak-AI docker compose up -d`
- 실제 GPU 서버의 `wespeak-ai` 컨테이너와 이름 · 포트가 같으므로 **같은 머신에서 동시에 띄우지 않는다.**

## 설정 (환경변수, 기본값 = 2026-09-13 실측)

| 변수 | 기본값 | 설명 |
|---|---|---|
| `FAKE_TIME_SCALE` | 1.0 | 모든 지연 배율 (기능 확인용으로 0.05 등) |
| `FAKE_SEED` | 42 | 오류 주입 · 429 난수 시드 |
| `FAKE_STT_SERIAL` | true | true: 실제 `max_workers=1` 실행기에서 대기(대기열 재현) / false: 병렬(전송 방식 비교용) |
| `FAKE_STT_BASE_MS` · `FAKE_STT_PER_AUDIO_SEC_MS` | 290 · 37 | STT 지연 = base + 음성 초당 ms (음성 길이 = 바이트 ÷ `FAKE_STT_BYTES_PER_SEC`(12,400)) |
| `FAKE_STT_GPU_CONTENTION` | 0.25 | Ollama 생성 중일 때 STT 지연 증가 비율 |
| `FAKE_OLLAMA_TTFT_MS` · `_TTFT_CONCURRENT_MS` | 190 · 275 | GPU를 다른 요청(다른 LLM 생성 **또는 STT**)과 나눠 쓰는 중이면 concurrent 값 |
| `FAKE_OLLAMA_PREFILL_MS_PER_1K_CHARS` · `FAKE_TTFT_REFERENCE_PROMPT_CHARS` | 6.8 · 600 | 프롬프트가 기준(회화 598자)보다 길면 1,000자당 TTFT 증가 (리딩 피드백 17,464자 → 약 +115ms). Claude는 0 |
| `FAKE_OLLAMA_CHUNK_MS` · `_CHUNK_SLOWDOWN` · `_STREAM_CHUNKS` | 57 · 0.09 · 35 | 청크 간격 × (1 + slowdown × (동시 수 − 1)), 응답 청크 수 고정 |
| `FAKE_OLLAMA_CORRECT_SHORT_MS` · `_CORRECT_MS` · `_CORRECT_SLOWDOWN` | 16,200 · 33,000 · 0.265 | 첨삭 소요. 처리 중 동시 수를 계속 반영(동시 3이면 전원 ×1.53) |
| `FAKE_CLAUDE_TTFT_MS` · `_CHUNK_MS` · `_STREAM_CHUNKS` | 550 · 25 · 20 | |
| `FAKE_CLAUDE_CORRECT_SHORT_MS` · `_CORRECT_MS` · `_CORRECT_SLOWDOWN` | 3,900 · 5,500 · 0.035 | |
| `FAKE_CLAUDE_RATE_LIMIT_RATE` | 0 | Claude 호출이 429로 실패할 확률 |
| `FAKE_TTS_MS` | 0 | REST `/chat`의 TTS 지연 |
| `FAKE_CORRECT_SHORT_THRESHOLD_CHARS` | 250 | 첨삭 사용자 메시지가 이 길이 미만이면 짧은 에세이 |

**단순화:** 스트리밍 응답 청크 수를 제공자별로 고정한다. TTFT와 첨삭 시간은 실측과 맞지만, 회화 · 피드백 **전체 시간**은
실제 출력 길이와 달라질 수 있으므로 포트폴리오의 전체 시간 수치는 실측값을 쓴다.

## 제어 API (`/_fake/*`)

```bash
curl localhost:8000/_fake/stats                     # 제공자별 LLM 호출 · 동시 처리 수, STT 대기열, 경로별 요청, 요청 태그 집계
curl -X POST localhost:8000/_fake/stats/reset
curl -X PUT localhost:8000/_fake/faults -H 'content-type: application/json' \
     -d '{"path_prefix": "/correct", "error_rate": 0.1, "status": 503}'   # 10% 확률로 즉시 5xx
curl -X POST localhost:8000/_fake/faults/outage -H 'content-type: application/json' \
     -d '{"path_prefix": "/correct", "seconds": 30}'                     # 30초 동안 전부 503
curl -X DELETE localhost:8000/_fake/faults
```

- **요청 태그:** 요청 본문에 `[req:<id>]`를 넣으면(예: 에세이 본문 끝) ID별 `received / succeeded / failed`를 센다 → 재시도 복구 비율 계산용
- **유료 LLM 호출 수:** `stats.llm.claude.calls`

## 테스트 · 실측 재현 확인

```bash
docker compose run --rm --no-deps -T wespeak-ai python -m pytest -q            # 기본 배율 0.05
FAKE_TEST_TIME_SCALE=1.0 docker compose run --rm --no-deps -T -e FAKE_TEST_TIME_SCALE wespeak-ai python -m pytest -q

# Ollama 경로: 실측과 같은 스크립트로 측정 후 비교 (약 12분)
docker exec wespeak-ai python /loadtest/ai-baseline/measure_ai_baseline.py --app-dir /ai --out /tmp/results-fake.json
docker cp wespeak-ai:/tmp/results-fake.json ../ai-baseline/validation-results-fake.json
python compare_with_baseline.py --real ../ai-baseline/results.json --fake ../ai-baseline/validation-results-fake.json

# Claude 경로
docker exec -w /fake wespeak-ai python validate_claude.py --yes --out /tmp/results-claude-fake.json
docker cp wespeak-ai:/tmp/results-claude-fake.json ../ai-baseline/validation-results-claude-fake.json
python compare_with_baseline.py --kind claude --real ../ai-baseline/results-claude.json --fake ../ai-baseline/validation-results-claude-fake.json
```
