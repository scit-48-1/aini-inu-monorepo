# Technology Stack

**Analysis Date:** 2026-03-06

## Languages

**Primary:**
- Java 21 - Backend API server (`aini-inu-backend/`)
- TypeScript 5.9 - Frontend client (`aini-inu-frontend/`)

**Secondary:**
- SQL (PostgreSQL dialect) - DDL scripts in `aini-inu-backend/src/main/resources/db/ddl/`, seed data in `aini-inu-backend/src/main/resources/db/seed/`
- Bash - Build/deployment scripts in `aini-inu-backend/scripts/`

## Runtime

**Backend:**
- JDK 21 (Eclipse Temurin 21 JRE in production Docker image)
- Gradle 8.14.4 (wrapper in `aini-inu-backend/gradle/wrapper/`)
- Lockfile: None (Gradle dependency locking not enabled)

**Frontend:**
- Node.js (version not pinned; no `.nvmrc` or `.node-version` present)
- npm (lockfile: `aini-inu-frontend/package-lock.json` present)

## Frameworks

**Core:**
- Spring Boot 3.5.10 - Backend REST API (`aini-inu-backend/build.gradle`)
- Next.js 16.1.6 - Frontend App Router (`aini-inu-frontend/package.json`)
- React 19.2.3 - UI rendering (`aini-inu-frontend/package.json`)

**Testing:**
- JUnit 5 (via `spring-boot-starter-test`) - Backend unit/integration tests
- H2 Database (in-memory, PostgreSQL compat mode) - Backend test DB
- Spring Security Test - Security-related test utilities
- No frontend test runner configured

**Build/Dev:**
- Gradle 8.14.4 - Backend build tool (`aini-inu-backend/gradlew`)
- npm - Frontend package manager
- Docker / Docker Compose - Containerized deployment (`aini-inu-backend/Dockerfile`, `aini-inu-backend/docker-compose.yml`)

## Key Dependencies

### Backend (`aini-inu-backend/build.gradle`)

**Critical:**
- `spring-boot-starter-data-jpa` - ORM and database access via Hibernate + JPA
- `spring-boot-starter-security` - Security framework (all defaults disabled; auth via custom JWT interceptor)
- `spring-boot-starter-web` - REST API serving
- `spring-boot-starter-websocket` - STOMP WebSocket for real-time chat
- `spring-boot-starter-validation` - Bean Validation (Jakarta Validation)
- `io.jsonwebtoken:jjwt-api:0.12.6` + `jjwt-impl` + `jjwt-jackson` - JWT token generation and validation

**AI / Vector Search:**
- `spring-ai-starter-model-google-genai-embedding` (Spring AI 1.1.2) - Google Gemini text embeddings for lost-pet matching
- `spring-ai-starter-vector-store-pgvector` (Spring AI 1.1.2) - PgVector similarity search

**Infrastructure:**
- `org.postgresql:postgresql` - PostgreSQL JDBC driver
- `com.h2database:h2` (test only) - In-memory test database
- `me.paulschwarz:spring-dotenv:4.0.0` - Load `.env` files as Spring properties
- `io.github.resilience4j:resilience4j-spring-boot3:2.2.0` - Retry policies for inter-service calls
- `spring-boot-starter-aop` - AOP support (required by Resilience4j annotations)
- `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6` - Swagger UI and OpenAPI spec generation
- `org.projectlombok:lombok` - Boilerplate reduction (compile-time only)

### Frontend (`aini-inu-frontend/package.json`)

**Critical:**
- `zustand@5.0.11` - Client-side state management (stores in `src/store/`)
- `leaflet@1.9.4` + `react-leaflet@5.0.0` - Interactive maps for walk features
- `lucide-react@0.563.0` - Icon library
- `sonner@2.0.7` - Toast notifications
- `next-themes@0.4.6` - Dark/light theme toggling
- `clsx@2.1.1` + `tailwind-merge@3.4.0` - Conditional className utility (`cn()` in `src/lib/utils.ts`)
- `react-daum-postcode@3.2.0` - Korean address search (Daum/Kakao Postcode API)
- `react-pageflip@2.0.3` - Page flip animation (walk diary book view)
- `canvas-confetti@1.9.4` - Confetti effects

**AI / External:**
- `@google/genai@1.40.0` - Google Generative AI SDK (client-side, used in `src/services/geminiService.ts`)

**Dev/Build:**
- `tailwindcss@4.1.18` + `@tailwindcss/postcss@4.1.18` - Tailwind CSS 4 via PostCSS
- `msw@2.12.9` - Mock Service Worker for frontend-only development (`src/mocks/`)
- `eslint@9` + `eslint-config-next@16.1.6` - Linting
- `typescript@5.9.3` - Type checking

## Configuration

**Environment (Backend):**
- `.env` (local development) and `.env.docker` (Docker Compose) - loaded by `spring-dotenv`
- `.env.example` and `.env.docker.example` committed as templates
- Main config file: `aini-inu-backend/src/main/resources/application.properties`
- Test config file: `aini-inu-backend/src/test/resources/application-test.properties`

**Required env vars (backend):**
- `JWT_SECRET` - HMAC-SHA256 signing key (32+ chars)
- `GEMINI_API_KEY` - Google Gemini embedding API key
- `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` - PostgreSQL connection
- `ANIMAL_REGISTRY_API_KEY` - Korean government animal registry API key

**Optional env vars (backend):**
- `GEMINI_EMBEDDING_MODEL` (default: `text-embedding-004`)
- `LOSTPET_CHAT_BASE_URL` (default: `http://localhost:18081`)
- `COMMUNITY_STORAGE_PUBLIC_BASE_URL` (default: `http://localhost:8080`)
- `COMMUNITY_STORAGE_PRESIGNED_EXPIRES_SECONDS` (default: `300`)
- `SPRING_AI_PGVECTOR_*` - Vector store tuning (dimensions, distance type, index type)

**Environment (Frontend):**
- No `.env.local` committed; configured at developer workstation level
- `NEXT_PUBLIC_ENABLE_MSW` - Toggle MSW mocking in dev
- `NEXT_PUBLIC_API_PROXY_TARGET` - Backend URL (default `http://localhost:8080`)
- `NEXT_PUBLIC_WS_URL` - WebSocket URL

**Build:**
- `aini-inu-backend/build.gradle` - Gradle build config with Spring AI BOM
- `aini-inu-frontend/tsconfig.json` - TypeScript config (target ES2017, strict mode, path alias `@/*` -> `./src/*`)
- `aini-inu-frontend/next.config.ts` - Next.js config (remote image patterns for picsum, unsplash, dicebear, gstatic, pstatic, kakao)
- `aini-inu-frontend/postcss.config.mjs` - PostCSS with Tailwind CSS 4 plugin
- `aini-inu-frontend/eslint.config.mjs` - ESLint flat config with next/core-web-vitals and next/typescript

## Platform Requirements

**Development:**
- JDK 21 for backend compilation
- Node.js + npm for frontend
- PostgreSQL 16 + pgvector extension (or Docker Compose: `pgvector/pgvector:pg16`)
- Docker optional (for containerized local dev via `docker-compose.yml`)

**Production:**
- Docker (multi-stage build: Gradle 8.14.3 + JDK 21 builder, Eclipse Temurin 21 JRE runtime)
- PostgreSQL 16 with `vector`, `hstore`, and `uuid-ossp` extensions (initialized in `aini-inu-backend/docker/postgres/init/01_extensions.sql`)
- Port 8080 (backend), Port 3000 (frontend)

**CI/CD:**
- No CI pipeline detected (no `.github/workflows/` directory)

---

*Stack analysis: 2026-03-06*
