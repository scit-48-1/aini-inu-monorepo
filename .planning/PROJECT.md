# AINI INU Frontend Contract Refactor

## What This Is

아이니이누(Aini-inu) 반려견 산책 소셜 매칭 플랫폼의 프론트엔드를 백엔드 계약 기준으로 전면 리팩토링하는 프로젝트다. 백엔드(`aini-inu-backend`)와 문서(`common-docs`)는 읽기 전용 기준이며, 수정은 `aini-inu-frontend` 내부에서만 수행한다. 목표는 API 계약 정렬, 화면 플로우 안정화, 런타임 에러 제거를 통해 시연 가능한 품질로 끌어올리는 것이다.

## Core Value

프론트엔드의 모든 사용자 흐름이 백엔드 Swagger/OpenAPI 계약과 100% 일치하며 크래시 없이 동작해야 한다.

## Requirements

### Validated

- ✓ 백엔드는 도메인별 API(인증/회원/반려견/산책/채팅/커뮤니티/실종)를 Spring Boot로 이미 제공한다 — existing
- ✓ OpenAPI 계약(`common-docs/openapi/openapi.v1.json`)과 PRD(`common-docs/PROJECT_PRD.md`)가 기준 문서로 존재한다 — existing
- ✓ 프론트엔드는 Next.js 기반으로 다수 화면(로그인, 대시보드, around-me, chat, feed, profile, settings)을 이미 보유한다 — existing
- ✓ 프론트엔드에는 API 호출 레이어(`aini-inu-frontend/src/services/api`)와 MSW 기반 mock 흐름이 이미 존재한다 — existing

### Active

- [ ] 프론트엔드 API URL/path/method 및 요청/응답 파싱을 Swagger 계약에 맞게 전면 정렬한다
- [ ] API 호출부를 프론트엔드 내 중앙화된 레이어로 통합하고 중복/분산 호출을 정리한다
- [ ] 인증 화면부터 시작해 핵심 사용자 플로우 UI/UX 상태 전이(로딩/빈/에러/성공)를 PRD 기준으로 정렬한다
- [ ] 산책 매칭, 반려견 프로필, 마이페이지/설정까지 전체 화면을 순차 정비한다
- [ ] 누락 기능을 구현해 PRD In-Scope API 사용 경로를 완성한다
- [ ] agent-browser 기반 자동 UAT와 스크린샷 증빙을 확보한다
- [ ] 런타임 에러를 0건으로 만든다

### Out of Scope

- `aini-inu-backend/` 코드 수정 — 백엔드는 동결 상태이며 계약 기준으로만 참조한다
- `common-docs/` 문서 수정 — PRD/OpenAPI는 단일 기준으로 읽기 전용 유지
- 프론트 신규 대형 기능 확장 — 본 프로젝트는 계약 정렬/안정화/누락 보완이 우선

## Context

- 현재 프론트와 백엔드는 같은 PRD 출발점이지만 바이브코딩 과정에서 계약이 불일치한 상태다
- 주요 불일치 영역: API endpoint/path/method, 요청/응답 스키마 파싱, 구현 누락 기능, UI/UX 상태 흐름
- 백엔드는 Docker로 상시 구동되어 있으며 런타임 계약 확인 기준은 Swagger UI/`/v3/api-docs`다
- 작업 우선순위는 크리티컬 버그 탐지 → API 레이어 정렬 → 화면별 계약 정렬 → 누락 기능 → 전체 UAT다
- 검증 도구는 `agent-browser`를 적극 활용하고, `vercel-react-best-practices`, `vercel-composition-patterns`, UI 가이드 스킬을 리팩토링 기준으로 사용한다

## Constraints

- **Scope**: 수정은 `aini-inu-frontend/` 내부만 허용 — 백엔드/문서 변경 금지
- **Contract**: 1순위 기준은 백엔드 코드+Swagger(OpenAPI) — 프론트는 계약에 맞춰 적응해야 함
- **Docs**: 2순위 기준은 `common-docs/PROJECT_PRD.md` 및 관련 문서 — UX/플로우 정렬 기준
- **Quality**: 로컬 MVP라도 런타임 에러/불안정 코드 허용 불가 — 전체 화면 정비 필요
- **Architecture**: API 호출은 중앙화된 프론트 레이어로 통합 — 화면별 ad-hoc 호출 지양
- **Verification**: 단계별 브라우저 자동 검증 및 `.planning/uat/` 스크린샷 증빙 필수

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| 백엔드/문서를 단일 진실원본으로 고정 | 계약 불일치의 근본 원인을 제거하기 위해 | — Pending |
| 리팩토링 수정 범위를 프론트엔드로 제한 | 시스템 경계와 책임을 명확히 유지하기 위해 | — Pending |
| 단계적 로드맵(버그 조사 → 계약 정렬 → 기능 보완 → 통합 UAT) 적용 | 크래시/계약/UX/누락 기능을 통제 가능한 순서로 해결하기 위해 | — Pending |
| 검증에서 agent-browser 자동 탐색을 기본 채택 | 수동 검증 누락을 줄이고 재현 가능한 증빙을 확보하기 위해 | — Pending |

---
*Last updated: 2026-03-05 after initialization*
