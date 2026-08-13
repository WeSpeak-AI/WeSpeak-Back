<!--
Sync Impact Report
- Version change: (template, unversioned) → 1.0.0
- Rationale: Initial ratification. Template placeholders replaced with concrete,
  project-specific principles. MAJOR bump to 1.0.0 to establish the baseline.
- Modified principles: all placeholders → concrete principles
  - [PRINCIPLE_1_NAME] → I. 계층형 아키텍처와 모듈 경계 (Layered Architecture & Module Boundaries)
  - [PRINCIPLE_2_NAME] → II. 일관된 API 계약과 예외 처리 (Consistent API Contract & Error Handling)
  - [PRINCIPLE_3_NAME] → III. 변경 승인 게이트 (Change-Approval Gate) — NON-NEGOTIABLE
  - [PRINCIPLE_4_NAME] → IV. 서비스별 데이터 소유와 이벤트 기반 통신 (Database-per-Service & Event-Driven Communication)
  - [PRINCIPLE_5_NAME] → V. 유지보수성과 업계 표준 관행 (Maintainability & Industry-Standard Practices)
- Added sections:
  - Additional Constraints (Technology & Security Constraints)
  - Development Workflow & Quality Gates
- Removed sections: none
- Templates requiring updates:
  - .specify/templates/plan-template.md ✅ compatible (Constitution Check gate is generic)
  - .specify/templates/spec-template.md ✅ compatible (no mandatory-section conflicts)
  - .specify/templates/tasks-template.md ✅ compatible (task categories cover principle-driven work)
- Deferred TODOs: none
-->

# WeSpeak Backend Constitution

WeSpeak(영어 학습 플랫폼)의 백엔드 서버를 위한 최상위 원칙 문서입니다.
Spring Boot 3 / Java 21 기반 Gradle 멀티모듈 마이크로서비스 아키텍처를 대상으로 합니다.
본 문서는 모든 설계·구현·리뷰 결정에 우선하며, 충돌 시 본 문서가 최종 기준입니다.

## Core Principles

### I. 계층형 아키텍처와 모듈 경계 (Layered Architecture & Module Boundaries)

각 서비스는 `Controller → Service → Repository` 계층을 유지해야 한다(MUST).

- Controller는 요청/응답 변환과 검증에만 관여하며 비즈니스 로직을 포함하지 않는다(MUST NOT).
- 비즈니스 로직은 Service 계층에 위치하며, 서비스 인터페이스와 구현(`*Service` / `*ServiceImpl`)
  분리 관행을 따른다(SHOULD).
- 영속성 접근은 Repository를 통해서만 수행한다(MUST).
- 공통 관심사는 `core/*` 모듈(core-common, core-domain, core-infra, core-jwt, core-event 등)에
  둔다. 각 `modules/*` 서비스는 다른 서비스 모듈의 내부 코드에 직접 의존하지 않는다(MUST NOT).

**근거**: 명확한 계층·모듈 경계는 마이크로서비스의 독립 배포와 테스트, 유지보수성을 보장하는
가장 기본적인 조건이다.

### II. 일관된 API 계약과 예외 처리 (Consistent API Contract & Error Handling)

모든 HTTP 응답은 공통 `ApiResponse<T>` 형식을 사용해야 한다(MUST).

- 실패 응답은 `ErrorCode` 열거형과 `BusinessException`을 통해 표현하며,
  개별 컨트롤러에서 임의의 오류 포맷을 만들지 않는다(MUST NOT).
- 예외는 `GlobalExceptionHandler`에서 중앙 처리하고, 새 오류 상황은 `ErrorCode`에
  코드·HTTP 상태·메시지를 추가해 등록한다(MUST).
- 신규/변경 API는 인증 요구 여부, 요청·응답 스키마, 오류 코드를 명시해야 한다(MUST).

**근거**: 클라이언트(React Native 앱)와 서비스 간 계약의 일관성은 통합 비용과 회귀 위험을
크게 낮춘다.

### III. 변경 승인 게이트 (Change-Approval Gate) — NON-NEGOTIABLE

에이전트/기여자는 소스 코드를 수정하기 전에 반드시 사용자(메인테이너)의 명시적 승인을
받아야 한다(MUST).

- 코드 변경 전, 변경 대상 파일·의도·영향 범위를 먼저 제시하고 승인을 기다린다(MUST).
- 승인 없이 소스 코드를 편집·삭제·이동하지 않는다(MUST NOT). 읽기·탐색·분석은 승인 없이
  가능하다.
- 한 번의 승인은 해당 변경 범위에만 적용되며, 범위를 벗어나는 후속 변경에는 새 승인이
  필요하다(MUST).

**근거**: 메인테이너가 명시적으로 요구한 통제 방식으로, 의도치 않은 변경과 회귀를 방지하고
변경 이력의 예측 가능성을 확보한다.

### IV. 서비스별 데이터 소유와 이벤트 기반 통신 (Database-per-Service & Event-Driven Communication)

서비스는 자신이 소유한 데이터에만 직접 접근하며, 다른 서비스의 데이터에 직접 접근하지
않는다(MUST NOT — 예: 타 서비스 소유 테이블 직접 조회/쓰기).

- 서비스 간 상태 전달·부수효과는 Kafka 이벤트(`core-event`)로 수행한다(SHOULD).
- 이벤트 발행 시 데이터 정합성이 필요한 경로는 Outbox 패턴
  (`core-outbox-message-relay`)을 사용해 원자성을 보장한다(MUST).
- 식별자는 Snowflake ID 생성 규칙을 따른다(MUST).

**근거**: 현재 진행 중인 database-per-service 방향과 일치하며, 서비스 결합도를 낮춰 독립
확장·배포와 장애 격리를 가능하게 한다.

### V. 유지보수성과 업계 표준 관행 (Maintainability & Industry-Standard Practices)

구현은 Spring/Java 생태계의 널리 통용되는 관행을 우선한다(SHOULD).

- 의존성 주입은 생성자 주입(`@RequiredArgsConstructor` + `final`)을 사용한다(MUST).
- 설정·비밀값은 코드에 하드코딩하지 않고 환경변수/설정 파일로 외부화한다(MUST NOT hardcode secrets).
- 공개 API·서비스 로직의 핵심 분기와 이벤트/동시성 로직에는 자동화 테스트를 추가한다(SHOULD).
- 실험적/특이한 방식보다, 이미 코드베이스에서 쓰이는 관행과 표준 라이브러리를 재사용한다(SHOULD).

**근거**: 팀·후임자가 예측 가능하게 코드를 이해·확장할 수 있어야 유지보수 비용이 낮아진다.

## Additional Constraints (Technology & Security Constraints)

- **기술 스택 고정**: Java 21, Spring Boot 4.0.4, Spring Cloud Gateway, MySQL 8, Redis 7,
  Apache Kafka(Confluent 7.5), Gradle 멀티모듈. 스택 변경은 거버넌스 절차를 통해서만
  가능하다(MUST).
- **인증·인가**: 외부 요청 인증은 API Gateway(JWT 검증)에서 수행한다. 각 서비스는 게이트웨이가
  전달한 신원 정보를 신뢰 경계로 취급하며, 민감 엔드포인트는 별도 인가 검증을 둔다(SHOULD).
- **비밀 관리**: JWT 시크릿, OAuth 자격증명, R2 키 등은 `.env`/환경변수로만 주입하고
  저장소에 커밋하지 않는다(MUST NOT commit secrets).
- **외부 연동**: AI 서버(FastAPI), Cloudflare R2, TTS 등 외부 연동은 `core-webclient` 등
  공통 클라이언트 설정을 재사용한다(SHOULD).

## Development Workflow & Quality Gates

- **브랜치·PR**: 기능 작업은 `feat/*` 등 목적 기반 브랜치에서 진행하고 PR로 병합한다(SHOULD).
- **변경 승인**: 소스 코드 변경은 Principle III에 따라 사전 승인을 받은 뒤 진행한다(MUST).
- **빌드 게이트**: 병합 전 `./gradlew build`(영향 모듈 대상)가 통과해야 한다(MUST).
- **리뷰 게이트**: 리뷰어는 본 헌법의 원칙(계층 경계, API 계약, 데이터 소유, 승인 게이트)
  준수 여부를 확인한다(MUST). 원칙 위반은 반드시 근거와 함께 정당화되거나 수정되어야 한다.
- **문서 동기화**: 아키텍처/계약에 영향을 주는 변경은 `README.md` 및 관련 스펙 문서를 함께
  갱신한다(SHOULD).

## Governance

- 본 헌법은 다른 모든 관행에 우선한다(supersedes). 충돌 시 본 문서를 기준으로 판단한다.
- **개정 절차**: 개정은 (1) 변경 제안과 근거 문서화, (2) 메인테이너 승인, (3) 영향받는 템플릿
  (plan/spec/tasks) 및 가이드 문서 동기화를 거쳐야 한다(MUST).
- **버전 정책(SemVer)**:
  - MAJOR: 원칙의 제거/재정의 등 하위 호환이 깨지는 거버넌스 변경.
  - MINOR: 원칙·섹션 추가 또는 실질적 지침 확장.
  - PATCH: 문구 명확화·오타 수정 등 비의미적 개선.
- **준수 검토**: 모든 PR/리뷰는 원칙 준수를 확인해야 하며, 복잡도 도입은 정당화되어야 한다.
- 런타임 개발 가이드가 필요하면 `README.md`를 우선 참조한다.

**Version**: 1.0.0 | **Ratified**: 2026-07-19 | **Last Amended**: 2026-07-19
