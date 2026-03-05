# Aini-Inu Repository Structure

## Root Layout
- `.` (workspace umbrella; contains app roots and planning/docs folders)
- `.planning/codebase` (generated codebase map outputs, including this file)
- `aini-inu-backend` (Spring Boot backend project root)
- `aini-inu-frontend` (Next.js frontend project root)
- `common-docs` (shared PRD/OpenAPI/assets repository)
- `.codex/skills` and `.agents/skills` (agent skill metadata and workflows)

## High-Level Tree (Operational Directories)
- `aini-inu-backend/build.gradle`, `aini-inu-backend/settings.gradle`, `aini-inu-backend/gradlew`
- `aini-inu-backend/src/main/java/scit/ainiinu/*` (domain and common backend code)
- `aini-inu-backend/src/main/resources/application.properties`
- `aini-inu-backend/src/main/resources/db/ddl/*`
- `aini-inu-backend/src/main/resources/db/seed/*`
- `aini-inu-backend/src/test/java/scit/ainiinu/*`
- `aini-inu-backend/scripts/*` (docker helpers and OpenAPI export)
- `aini-inu-frontend/package.json`, `aini-inu-frontend/next.config.ts`, `aini-inu-frontend/tsconfig.json`
- `aini-inu-frontend/src/app/*` (Next App Router routes/layout)
- `aini-inu-frontend/src/components/*` (feature UI + design primitives)
- `aini-inu-frontend/src/services/*` (API, auth, and external integration clients)
- `aini-inu-frontend/src/store/*` (Zustand state stores)
- `aini-inu-frontend/src/mocks/*` (MSW setup and handlers)
- `aini-inu-frontend/public/*` (runtime assets, mostly symlinked to `common-docs/images`)
- `common-docs/PROJECT_PRD.md`
- `common-docs/openapi/openapi.v1.json`
- `common-docs/images/*` (logos, portraits, favicon, and web assets)

## Backend Directory Breakdown
- `aini-inu-backend/src/main/java/scit/ainiinu/common`
- config (`.../common/config/SecurityConfig.java`, `.../common/config/WebConfig.java`, `.../common/config/OpenApiConfig.java`, `.../common/config/JpaConfig.java`)
- security (`.../common/security/jwt/JwtTokenProvider.java`, `.../common/security/interceptor/JwtAuthInterceptor.java`)
- response/exception (`.../common/response/ApiResponse.java`, `.../common/exception/GlobalExceptionHandler.java`)
- `aini-inu-backend/src/main/java/scit/ainiinu/member` (auth/profile/follow/member personality)
- `aini-inu-backend/src/main/java/scit/ainiinu/pet` (pet CRUD, catalogs, certification integration)
- `aini-inu-backend/src/main/java/scit/ainiinu/walk` (thread recruitment and diary features)
- `aini-inu-backend/src/main/java/scit/ainiinu/chat` (chat rooms/messages/reviews + websocket realtime)
- `aini-inu-backend/src/main/java/scit/ainiinu/community` (posts/comments/stories/image upload)
- `aini-inu-backend/src/main/java/scit/ainiinu/lostpet` (reporting, candidate scoring, AI match workflow)

## Backend Resource and Script Layout
- Runtime config: `aini-inu-backend/src/main/resources/application.properties`
- DDL scripts: `aini-inu-backend/src/main/resources/db/ddl/01_walk_indexes_constraints.sql` ... `06_legacy_story_cleanup.sql`
- Seed scripts: `aini-inu-backend/src/main/resources/db/seed/00_lookup_seed.sql`, `10_core_sample_seed.sql`, `20_status_edge_seed.sql`, `99_reset_sequences.sql`
- Infra scripts:
- `aini-inu-backend/scripts/docker-up.sh`
- `aini-inu-backend/scripts/docker-down.sh`
- `aini-inu-backend/scripts/docker-logs.sh`
- `aini-inu-backend/scripts/export-openapi.sh`

## Backend Test Structure
- Tests are grouped by domain and test type under `aini-inu-backend/src/test/java/scit/ainiinu`.
- Representative folders:
- `.../community/contract`, `.../community/integration`, `.../community/service`
- `.../walk/controller`, `.../walk/integration`, `.../walk/repository`, `.../walk/service`
- `.../lostpet/contract`, `.../lostpet/integration`, `.../lostpet/unit`
- `.../chat/controller`, `.../chat/integration`, `.../chat/service`
- Common API contract tests are under `.../common/contract`.

## Frontend Directory Breakdown
- `aini-inu-frontend/src/app` (route-level pages and shared app shell)
- examples: `.../app/layout.tsx`, `.../app/page.tsx`, `.../app/dashboard/page.tsx`, `.../app/chat/[id]/page.tsx`
- `aini-inu-frontend/src/components`
- feature slices: `around-me`, `chat`, `dashboard`, `feed`, `profile`, `signup`
- shared/UI: `common`, `shared/forms`, `shared/modals`, `ui`
- `aini-inu-frontend/src/hooks` and `.../hooks/forms` (feature hooks and form orchestration)
- `aini-inu-frontend/src/services/api` (`apiClient.ts`, `memberService.ts`, `postService.ts`, `threadService.ts`, `chatService.ts`, `locationService.ts`)
- `aini-inu-frontend/src/services/authService.ts` and `.../services/geminiService.ts`
- `aini-inu-frontend/src/store` (`useUserStore.ts`, `useConfigStore.ts`)
- `aini-inu-frontend/src/constants`, `.../lib`, `.../types`
- `aini-inu-frontend/src/mocks` (`MSWProvider.tsx`, `browser.ts`, `handlers.ts`)

## Frontend Runtime Asset Structure
- Public asset links in `aini-inu-frontend/public` are mostly symlinks:
- `public/images -> ../../common-docs/images`
- `public/AINIINU_ROGO_B.png -> ../../common-docs/images/AINIINU_ROGO_B.png`
- `public/AINIINU_ROGO_W.png -> ../../common-docs/images/AINIINU_ROGO_W.png`
- `public/favicon.ico -> ../../common-docs/images/favicon.ico`
- Link integrity is validated by `aini-inu-frontend/scripts/check-image-links.sh`.

## Shared Documentation Structure
- Product/requirements source: `common-docs/PROJECT_PRD.md`
- API snapshot source: `common-docs/openapi/openapi.v1.json`
- OpenAPI maintenance guide: `common-docs/openapi/README.md`
- Image source-of-truth guide: `common-docs/images/README.md`

## Structure Summary
- The repository is physically split into backend/frontend/docs roots, but logically connected through:
- API contract snapshots (`common-docs/openapi`)
- shared static assets (`common-docs/images` linked into `aini-inu-frontend/public`)
- onboarding/runtime scripts (`aini-inu-backend/scripts/*`, `aini-inu-frontend/scripts/*`).
