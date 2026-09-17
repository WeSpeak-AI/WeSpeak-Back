# AI 서버 지연 기준값 측정 (STT · Ollama)

실제 AI 서버(WeSpeak-AI)에서 **STT 시간, Ollama LLM 지연(첫 토큰 · 전체), 첨삭 시간**을 측정한다.
이 값은 부하 실험용 가짜 LLM/STT 모드의 지연 설정 기준으로 쓴다(`specs/005-portfolio-tech-writeup/measurement-plan.md`).

## 구성

| 파일 | 설명 |
|---|---|
| `audio/short.m4a` · `medium.m4a` · `long.m4a` | 영어 발화 2초 · 9초 · 22초. 앱 녹음 프리셋(`HIGH_QUALITY`)과 같은 m4a · AAC 44.1kHz 스테레오 |
| `measure_ai_baseline.py` | AI 서버 컨테이너 안에서 실행하는 측정 스크립트 (추가 설치 없음) |
| `parse_ai_logs.py` | 측정 구간의 서버 로그를 시나리오별로 집계 (표준 라이브러리만 사용) |

## 측정 시나리오 (기본값 기준 약 60건)

| 시나리오 | 내용 | 확인할 것 |
|---|---|---|
| `warmup-*` | 첫 호출 (집계 제외) | — |
| `chat-c1-{short,medium,long}` | gRPC Chat, 동시 1개 × 5회 | 음성 길이별 STT 시간, Ollama 첫 토큰 · 전체 시간 |
| `chat-c2-medium`, `chat-c3-medium` | gRPC Chat, 동시 2 · 3개 × 3라운드 | **STT 대기열**(한 번에 1건), Ollama 동시 처리 시 지연 증가 |
| `feedback-c1-medium` | gRPC Feedback, 동시 1개 × 5회 | 리딩 피드백 첫 토큰 · 전체 시간 |
| `correct-c1-{short,medium,long}` | REST `/correct`, 동시 1개 × 3회 | 에세이 길이별 첨삭 시간 |
| `correct-c2-medium`, `correct-c3-medium` | REST `/correct`, 동시 2 · 3개 × 3라운드 | Ollama 동시 처리 시 첨삭 지연 증가 |

- 동시 요청은 **최대 3개**로 제한한다. `llm.py`의 Ollama 슬롯(3개)을 넘으면 Claude로 넘어가기 때문이다.
- 클라이언트 지표: `stt_ms`(요청 시작 → 인식 텍스트 도착, 대기열 포함), `ttft_ms`(인식 텍스트 → 첫 LLM 청크),
  `llm_total_ms`(인식 텍스트 → 마지막 청크), `e2e_ms`
- 서버 로그 지표: `stt_elapsed_ms`(대기열 제외 순수 STT), `chat_llm_ms` · `feedback_llm_ms` · `correction_ms`,
  시나리오별 **Ollama / Claude 호출 수**

## 실행 방법 (GPU 호스트)

### 0. 사전 확인

- 측정 중에는 **다른 사용자 트래픽이 없는 시간**에 실행한다. 다른 요청이 섞이면 Ollama 슬롯이 넘쳐 Claude로 처리될 수 있다.
  (집계 스크립트가 Claude 호출을 감지하면 경고한다.)
- 환경 기록용으로 아래 출력도 함께 저장해 둔다.

```bash
nvidia-smi --query-gpu=name,memory.total,driver_version --format=csv > env.txt
docker exec wespeak-ai sh -c 'env | grep -E "^(PROVIDER|OLLAMA_MODEL|WHISPER_MODEL|WHISPER_DEVICE|WHISPER_COMPUTE_TYPE|STT_PROVIDER|OLLAMA_MAX_CONCURRENT)="' >> env.txt
docker exec wespeak-ai python -c "from app.config import settings as s; print(s.provider, s.ollama_model, s.whisper_model, s.whisper_device, s.whisper_compute_type, s.stt_provider)" >> env.txt
```

### 1. 스크립트 복사 후 실행

```bash
# 이 디렉터리(loadtest/ai-baseline)를 GPU 호스트로 옮긴 뒤
docker cp ai-baseline wespeak-ai:/tmp/ai-baseline
docker exec wespeak-ai python /tmp/ai-baseline/measure_ai_baseline.py
```

- 일부만 다시 돌리려면 `--only chat,feedback,correct` 중 필요한 것만 지정한다.
- 반복 횟수 조정: `--runs 5`(동시 1개 시나리오), `--rounds 3`(동시 2 · 3개 시나리오)

### 2. 결과와 로그 가져오기

실행이 끝나면 마지막에 `docker logs` 명령이 출력된다. 그 명령을 **호스트에서** 그대로 실행한다.

```bash
docker cp wespeak-ai:/tmp/ai-baseline/results.json .
docker logs -t --since <출력된 시작 시각> --until <출력된 종료 시각> wespeak-ai > ai-logs.txt 2>&1
```

### 3. 집계

```bash
python parse_ai_logs.py --results results.json --logs ai-logs.txt
```

`results.json`, `ai-logs.txt`, `env.txt`, 콘솔 출력(측정 스크립트 · 집계 스크립트)을 함께 보관한다.

---

## Claude 경로 지연 측정 (유료 API)

`measure_claude_baseline.py`는 컨테이너 안 **별도 프로세스**에서만 `settings.provider`를 `claude`로 바꾸고
서비스 함수(`chat_service.chat_stream` · `feedback_service.get_feedback_stream` · `correction_service.correct_essay`)를
직접 호출한다. 실행 중인 서버 프로세스와 운영 트래픽에는 영향이 없다.

- STT는 호출하지 않는다. 입력은 Ollama 측정 때 STT가 인식한 텍스트와 같은 에세이 · 책 본문을 쓴다
- 기본 34건: 회화(짧음 · 중간 · 긴 발화 × 3, 동시 3 × 2라운드), 리딩 피드백 × 3, 첨삭(짧음 · 중간 · 긴 에세이 × 3, 동시 3 × 2라운드)
- 지표: `ttft_ms`(호출 → 첫 청크, STT 없음), `total_ms`, 출력 글자 수 · 초당 글자 수(Claude는 청크 크기가 Ollama와 달라 **글자/초로 비교**)
- httpx 요청 로그로 실제 Claude 호출 수를 세고, 첨삭 structured output 실패(fallback) 횟수도 기록한다
- `--yes` 없이 실행하면 호출 계획만 출력하고 끝난다

```bash
# (서버) ai-baseline 폴더가 있는 위치에서
docker exec wespeak-ai rm -rf /tmp/ai-baseline
docker cp ai-baseline wespeak-ai:/tmp/ai-baseline
docker exec wespeak-ai python /tmp/ai-baseline/measure_claude_baseline.py              # 계획 확인
docker exec wespeak-ai python /tmp/ai-baseline/measure_claude_baseline.py --yes | tee measure-claude-output.txt
docker cp wespeak-ai:/tmp/ai-baseline/results-claude.json .
```

결과 파일: `measure-claude-output.txt`, `results-claude.json`
