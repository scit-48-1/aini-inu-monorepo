# Aini-inu Frontend Realignment

## What This Is

반려견 산책 소셜 매칭 플랫폼 "아이니이누(Aini-inu)"의 프론트엔드를 백엔드 API 계약(OpenAPI/Swagger) 기준으로 전면 리팩토링하는 프로젝트. 9개 도메인, 73개 API 엔드포인트, 31개 DEC 정책 잠금값을 프론트엔드에서 100% 준수하도록 정렬한다.

## Core Value

프론트엔드의 모든 API 호출이 백엔드 Swagger 명세와 100% 일치하고, PRD 요구사항이 빠짐없이 구현되어 런타임 에러 0건을 달성하는 것.

## Requirements

### Validated

<!-- 백엔드에서 이미 구현·검증된 기능 (읽기 전용, 절대 수정 금지) -->

- ✓ JWT 기반 인증/인가 (JwtAuthInterceptor, @CurrentMember) — backend
- ✓ ApiResponse<T> 봉투 패턴 (success/status/data/errorCode/message) — backend
- ✓ 회원 관리 API (가입, 로그인, 프로필 CRUD, 팔로우/차단) — backend 13 endpoints
- ✓ 반려견 CRUD + 마스터 데이터 (견종/성격/산책스타일) — backend 8 endpoints
- ✓ 산책 스레드 (주변 탐색/레이더/모집/참여) — backend 9 endpoints
- ✓ 산책일기 CRUD + 스토리 — backend 6 endpoints
- ✓ 채팅 시스템 (방 생성/메시지/WebSocket STOMP) — backend 13 endpoints
- ✓ 커뮤니티 피드 (게시글/댓글/좋아요/이미지) — backend 10 endpoints
- ✓ 실종 반려견 (제보/AI 분석/매칭) — backend 7 endpoints
- ✓ 이미지 업로드 (presigned URL 기반) — backend
- ✓ 페이지네이션 (SliceResponse/CursorResponse/PageResponse) — backend
- ✓ 도메인별 에러 코드 체계 (ErrorCode enum) — backend
- ✓ OpenAPI 계약 스냅샷 (common-docs/openapi/openapi.v1.json) — contract

### Active

<!-- 프론트엔드 리팩토링 범위 — 이번 마일스톤 목표 -->

- [ ] 크리티컬 런타임 버그 전수 조사 및 수정
- [ ] 공통 API 레이어 중앙화 + ApiResponse<T> 봉투 처리 통합
- [ ] 공통 에러 핸들링 (에러 코드 매핑, 사용자 메시지, 상태 전이)
- [ ] 이미지 업로드 유틸 공통화 (presigned URL 플로우)
- [ ] 인증/회원가입 3단계 플로우 (FR-AUTH, 3+1 endpoints)
- [ ] 회원 프로필/관계 화면 정렬 (FR-MEMBER, 13 endpoints)
- [ ] 반려견 CRUD + 마스터 데이터 화면 정렬 (FR-PET, 8 endpoints)
- [ ] 산책 스레드 화면 — around-me/레이더/모집 (FR-WALK, 9 endpoints)
- [ ] 산책일기 + 스토리 화면 정렬 (FR-WALK/FR-COMMUNITY, 6+1 endpoints)
- [ ] 채팅 시스템 화면 + WebSocket 연동 (FR-CHAT, 13 endpoints)
- [ ] 커뮤니티/피드 화면 — 게시글/댓글/좋아요 (FR-COMMUNITY, 10 endpoints)
- [ ] 실종 반려견 화면 — 제보/AI 분석/매칭 (FR-LOST, 7 endpoints)
- [ ] 대시보드 — 복합 도메인 조합 (FR-DASH)
- [ ] 설정 화면 + 전체 통합 UAT + 정리 (FR-SET + 최종 검증)

### Out of Scope

- 백엔드 코드 수정 — 읽기 전용, 절대 수정 금지
- common-docs 수정 — 읽기 전용, 절대 수정 금지
- 새로운 API 엔드포인트 추가 — 기존 73개 명세 내에서만 작업
- SSR/RSC 전환 — 현재 'use client' 패턴 유지
- 모바일 앱 개발 — 웹 프론트엔드만 대상
- CI/CD 파이프라인 구축 — 이번 범위 아님

## Context

**현재 상태:**
- 프론트와 백엔드가 하나의 PRD로 각자 바이브코딩 → API 계약 전면 불일치
- API URL/path/method, 요청/응답 파싱, 봉투 패턴(ApiResponse<T>) 미준수
- 백엔드에 있는 기능이 프론트에서 다수 누락/미구현
- PRD 기준 UI/UX(상태 전이, 에러/빈 상태, 플로우) 구현 불일치
- 특정 기능에서 런타임 에러 발생
- MSW 목 데이터 기반 개발 흔적 잔존 (localStorage 패턴 등)

**참조 문서:**
- PRD: `common-docs/PROJECT_PRD.md`
- OpenAPI: `common-docs/openapi/openapi.v1.json`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- 코드베이스 맵: `.planning/codebase/`

**절대 기준 (PRD §0.1 문서 거버넌스):**
1순위: aini-inu-backend/ 코드 + Swagger(OpenAPI)
2순위: common-docs/PROJECT_PRD.md
충돌 시: DEC 잠금값 우선, API 표현은 Swagger 우선

## Constraints

- **수정 범위**: aini-inu-frontend/ 내부만 수정 가능 — 백엔드/common-docs 변경 0건
- **API 계약**: 73개 엔드포인트가 Swagger 명세와 100% 일치해야 함
- **봉투 패턴**: 모든 API 응답은 ApiResponse<T> 구조로 공통 처리
- **DEC 정책**: PRD §15 DEC 31개 정책 잠금값 프론트에서 준수
- **UI/UX 플로우**: PRD §8.3 기준 5종 상태 (default/loading/empty/error/success) 충족
- **Tech Stack**: Next.js 16 / React 19 / TypeScript 5.9 / Tailwind CSS 4 / Zustand 5
- **백엔드 의존**: Docker 상시 구동 (http://localhost:8080)
- **FR 커버리지**: PRD §8 FR 34개 요구사항 전부 프론트에서 구현

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| 프론트엔드만 수정, 백엔드 읽기 전용 | API 계약은 백엔드가 진실의 원천 | — Pending |
| 12 Phase 순차 리팩토링 | 도메인별 의존성 순서 (인프라 → 인증 → 도메인 → 통합) | — Pending |
| API 레이어 중앙화 우선 (Phase 2) | 모든 도메인 화면이 공통 API 레이어에 의존 | — Pending |
| agent-browser로 UAT 자동화 | verify-work에서 스크린샷 증빙 확보 | — Pending |
| Vercel Skills 참조 | execute 시 React/UI 최적화 규칙 자동 적용 | — Pending |

---
*Last updated: 2026-03-06 after initialization*
