# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Overview

Monorepo for **aini-inu**, a pet community platform (lost-pet search, walk diary, chat, community feed). Three modules:
- `aini-inu-backend` — Spring Boot 3.5 / Java 21 / Gradle API server
- `aini-inu-frontend` — Next.js 16 / React 19 / TypeScript / Tailwind CSS 4 client
- `common-docs` — Shared contract artifacts (PRD, OpenAPI snapshot, images)

## Build & Run Commands

### Backend
```bash
cd aini-inu-backend
./gradlew test                    # Run all tests (H2 in-memory, no Docker needed)
./gradlew test --tests '*.WalkDiaryServiceTest'  # Run a single test class
./gradlew bootRun                 # Start server (requires PostgreSQL + .env)
./scripts/docker-up.sh            # Start PostgreSQL + backend via Docker Compose
./scripts/docker-down.sh          # Stop Docker containers
./scripts/docker-logs.sh          # View backend logs
./scripts/export-openapi.sh       # Export OpenAPI JSON to common-docs/openapi/
```

### Frontend
```bash
cd aini-inu-frontend
npm run dev                       # Start dev server (localhost:3000)
npm run build                     # Production build
npm run lint                      # ESLint
npm run check:assets              # Validate image links against common-docs/images
```

## Architecture

### Backend (`scit.ainiinu.*`)

Domain-first package layout. Each domain has: `controller/`, `service/`, `repository/`, `entity/`, `dto/` (with `request/` and `response/` sub-packages), and `exception/`.

**Domains:** `member`, `pet`, `walk`, `chat`, `lostpet`, `community`

**Cross-cutting (`common/`):**
- `response/ApiResponse<T>` — All endpoints return this envelope: `{success, status, data, errorCode, message}`
- `security/` — JWT auth with `@CurrentMember` annotation for controller parameter injection
- `exception/ErrorCode` — Centralized error codes
- `entity/vo/Location` — Shared value object for lat/lng

**Key integrations (lostpet domain):**
- Spring AI + Google Gemini embeddings + PgVector for AI-powered lost pet matching
- Resilience4j retry on chat direct-connect calls
- Internal HTTP client integration to chat service for creating chat rooms on match

### Frontend

Next.js App Router (`src/app/`). All pages are `'use client'` components. State management via Zustand stores (`src/store/`). API calls go through `src/services/api/apiClient.ts` which expects the backend's `ApiResponse` envelope and unwraps `.data`. MSW is configured for mock development (`src/mocks/`).

**Path alias:** `@/*` maps to `./src/*`

**Frontend env vars** (in `.env.local`):
- `NEXT_PUBLIC_ENABLE_MSW` — Toggle MSW mocking
- `NEXT_PUBLIC_API_PROXY_TARGET` — Backend URL (default `http://localhost:8080`)
- `NEXT_PUBLIC_WS_URL` — WebSocket URL

## Testing

**Backend tests** use H2 in-memory with PostgreSQL compatibility mode. Annotate integration tests with `@IntegrationTestProfile` (activates `test` profile). Test profile (`application-test.properties`) disables SQL init and AI/vector store auto-configuration.

Test categories follow a naming convention:
- `*Test` — Unit tests (Mockito-based)
- `*SliceTest` — Spring slice tests (`@WebMvcTest`, `@DataJpaTest`)
- `*ContractTest` — OpenAPI/HTTP contract validation
- `*IntegrationTest` — Full Spring Boot context tests

**Frontend** has no test runner configured; validation is via `npm run lint` and `npm run build`.

## Source of Truth & Priority

When code, docs, or contracts conflict, resolve in this order:
1. `common-docs/PROJECT_PRD.md` (product requirements)
2. `common-docs/openapi/openapi.v1.json` (API contract snapshot)
3. Backend implementation
4. Frontend adaptation

## Working Rules

- API changes must update all layers: controller, service, DTO/schema, tests, and OpenAPI snapshot
- Entity changes must stay consistent with DDL scripts in `src/main/resources/db/ddl/`
- Seed data is in `src/main/resources/db/seed/` — loaded automatically on startup (disabled in test profile)
- Backend secrets go in `.env` (local) or `.env.docker` (Docker) — never commit these
- Frontend is in pre-refactor state; prefer isolated changes that reduce coupling over large feature additions
- Image assets live in `common-docs/images` (source of truth) and are symlinked into `aini-inu-frontend/public`
