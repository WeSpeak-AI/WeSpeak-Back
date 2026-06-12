# WeSpeak Backend

영어 학습 플랫폼 **WeSpeak**의 백엔드 서버입니다.  
Spring Boot 기반 멀티모듈 마이크로서비스 아키텍처로 구성되어 있습니다.

---

## 시스템 아키텍처

```
[React Native App]
        │
        ▼
     [Nginx]  ← Reverse Proxy
        │
        ▼
  [API Gateway]  :9000  ← JWT 인증 / 라우팅
        │
   ┌────┼────────────────────────────────┐
   │    │                                │
   ▼    ▼    ▼      ▼      ▼      ▼     ▼
 Auth  User  Voca  Write  Read  Conv  Search
 9001  9002  9003  9004   9005  9006   9007
   │    │    │      │      │      │
   └────┴────┴──────┴──────┴──────┘
                    │
                 [MySQL]  [Redis]  [Kafka]

                          [Reading Service]
                                 │ WebSocket
                          [AI Server (FastAPI)]
                           STT + Feedback 생성

                   [Cloudflare R2]
                   도서/단어장 표지 이미지
```

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3, Spring Cloud Gateway |
| Database | MySQL 8.0, Redis 7 |
| Message Queue | Apache Kafka (Confluent 7.5) |
| Auth | Google OAuth 2.0, JWT |
| Storage | Cloudflare R2 (S3 Compatible) |
| ID Generation | Snowflake ID |
| Infra | Docker, Docker Compose |
| Build | Gradle (Multi-module) |

---

## 모듈 구조

```
WeSpeak-Back/
├── api/
│   └── api-gateway          # Spring Cloud Gateway (JWT 검증, 라우팅)
│
├── modules/
│   ├── module-auth          # 회원가입, Google OAuth, JWT 발급
│   ├── module-user          # 사용자 프로필, 티켓, 학습 통계
│   ├── module-voca          # 단어장, 커스텀 단어장
│   ├── module-writing       # 영작문 AI 첨삭
│   ├── module-reading       # 도서 읽기, WebSocket + AI 요약 피드백
│   ├── module-conversation  # AI 회화 연습
│   └── module-searchword    # 단어 검색 (AI 임베딩)
│
└── core/
    ├── core-common          # 공통 예외, 응답 형식
    ├── core-domain          # JPA Entity (공유 도메인)
    ├── core-infra           # 공통 Repository, Snowflake
    ├── core-jwt             # JWT 유틸리티
    ├── core-event           # Kafka 이벤트 정의
    ├── core-outbox-message-relay  # Outbox 패턴 구현
    ├── core-data-serializer # 직렬화 유틸
    └── core-webclient       # 공통 WebClient 설정
```

---

## 주요 기능

### Reading (도서 읽기)
- 레벨별 도서 목록 제공 (Beginner / Intermediate / Advanced)
- 사용자 진행 페이지 자동 저장
- **WebSocket** 기반 오디오 스트리밍 → AI 서버(FastAPI)에서 STT + 피드백 생성
- 피드백은 클라이언트 TTS(expo-speech)로 재생

### Vocabulary (단어장)
- 기본 제공 단어장 + 사용자 커스텀 단어장
- AI 임베딩 기반 단어 검색
- Cloudflare R2에 단어장 표지 이미지 업로드

### Writing (영작문)
- 에세이 작성 및 AI 첨삭
- 학습 완료 이벤트 발행 (Kafka)

### Conversation (AI 회화)
- 주제 기반 AI 회화 연습
- 학습 통계 연동

### Auth
- Google OAuth 2.0 소셜 로그인
- JWT Access / Refresh Token

---

## 이벤트 아키텍처 (Kafka + Outbox)

서비스 간 직접 호출 대신 **Outbox 패턴**으로 비동기 이벤트를 발행합니다.

| 이벤트 | 발행 서비스 | 처리 |
|--------|------------|------|
| `STUDY_COMPLETED` | reading, writing, conversation | 학습 통계 갱신 |
| `VOCA_GENERATED` | voca | 단어장 생성 완료 알림 |
| `TOPIC_UPDATED` | conversation | 회화 주제 업데이트 |

---

## 실행 방법

### 환경 변수 설정

각 서비스의 `src/main/resources/application.yml`은 `.gitignore`에 포함되어 있습니다.  
서버에서 직접 생성하거나 환경 변수로 주입합니다.

필수 환경 변수:

```env
DB_USERNAME=root
DB_PASSWORD=your_password
TOKEN_SECRET=your_jwt_secret
GOOGLE_CLIENT_ID=your_google_client_id
R2_ACCESS_KEY=your_r2_access_key
R2_SECRET_KEY=your_r2_secret_key
R2_ACCOUNT_ID=your_r2_account_id
```

### Docker Compose로 전체 실행

```bash
docker compose up --build -d
```

### 개별 서비스 빌드

```bash
./gradlew :modules:module-reading:bootJar
```

---

## 포트 정보

| 서비스 | 포트 |
|--------|------|
| API Gateway | 9000 |
| Auth Service | 9001 |
| User Service | 9002 |
| Voca Service | 9003 |
| Writing Service | 9004 |
| Reading Service | 9005 |
| Conversation Service | 9006 |
| Search Service | 9007 |
| MySQL | 3306 |
| Redis | 6379 |
| Kafka | 9092 |

---

## API 문서 (주요 엔드포인트)

모든 요청은 `Authorization: Bearer <token>` 헤더 필요 (로그인 제외)

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/auth/login/google` | Google OAuth 로그인 |
| GET | `/api/users/profile` | 내 프로필 조회 |
| GET | `/api/books` | 도서 목록 |
| GET | `/api/books/{bookId}/pages` | 도서 페이지 조회 |
| WS | `/ws/reading/{bookPageId}` | 요약 피드백 WebSocket |
| GET | `/api/voca-books` | 단어장 목록 |
| POST | `/api/writings` | 영작문 제출 |
| GET | `/api/conversations` | 회화 목록 |
| GET | `/api/search/words` | 단어 검색 |
