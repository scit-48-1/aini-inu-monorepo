# STACK

## 1) Scope and Current Priority
- Monorepo modules are `aini-inu-backend`, `common-docs`, `aini-inu-frontend` (`AGENTS.md`).
- Current delivery center is backend + docs; frontend is explicitly pre-refactor (`AGENTS.md`).
- Contract/source-of-truth order is PRD -> OpenAPI snapshot -> backend implementation -> frontend adaptation (`AGENTS.md`, `common-docs/PROJECT_PRD.md`).

## 2) Module Map (Practical)
- Backend runtime/API: `aini-inu-backend/src/main/java/**`, `aini-inu-backend/src/main/resources/**`.
- Product + contract docs: `common-docs/PROJECT_PRD.md`, `common-docs/openapi/openapi.v1.json`.
- Frontend (legacy/pre-refactor state): `aini-inu-frontend/src/**`.

## 3) Backend Core Stack (Primary)
- Language/runtime: Java 21 toolchain (`aini-inu-backend/build.gradle`).
- Framework: Spring Boot 3.5.10 (`aini-inu-backend/build.gradle`).
- Web/API: `spring-boot-starter-web`, validation, springdoc-openapi (`aini-inu-backend/build.gradle`).
- Data layer: Spring Data JPA + PostgreSQL driver (`aini-inu-backend/build.gradle`).
- Security: Spring Security dependency + custom interceptor auth (`aini-inu-backend/build.gradle`, `aini-inu-backend/src/main/java/scit/ainiinu/common/config/SecurityConfig.java`, `aini-inu-backend/src/main/java/scit/ainiinu/common/security/interceptor/JwtAuthInterceptor.java`).
- Auth token: JJWT (`jjwt-api/impl/jackson`) + provider (`aini-inu-backend/build.gradle`, `aini-inu-backend/src/main/java/scit/ainiinu/common/security/jwt/JwtTokenProvider.java`).
- Realtime: Spring WebSocket/STOMP (`aini-inu-backend/build.gradle`, `aini-inu-backend/src/main/java/scit/ainiinu/chat/config/WebSocketConfig.java`).
- Resilience: resilience4j retry for outbound chat call (`aini-inu-backend/build.gradle`, `aini-inu-backend/src/main/java/scit/ainiinu/lostpet/integration/chat/ChatRoomDirectClientImpl.java`).
- AI retrieval: Spring AI Google GenAI embedding + pgvector vector store (`aini-inu-backend/build.gradle`, `aini-inu-backend/src/main/java/scit/ainiinu/lostpet/integration/ai/LostPetAiClientImpl.java`).

## 4) Data and Storage Stack (Primary)
- Main DB target is PostgreSQL + pgvector (`aini-inu-backend/src/main/resources/application.properties`, `aini-inu-backend/docker-compose.yml`).
- DDL/seed bootstrapping is SQL-init based (`aini-inu-backend/src/main/resources/application.properties`, `aini-inu-backend/src/main/resources/db/ddl/*.sql`, `aini-inu-backend/src/main/resources/db/seed/*.sql`).
- Lost-pet vector table/index is maintained in DDL (`aini-inu-backend/src/main/resources/db/ddl/03_lostpet_indexes_constraints.sql`).
- Community image storage is local filesystem (MVP), not cloud object storage (`aini-inu-backend/src/main/java/scit/ainiinu/community/config/CommunityStorageProperties.java`, `aini-inu-backend/src/main/java/scit/ainiinu/community/service/ImageUploadService.java`).

## 5) Contract and Documentation Stack (Primary)
- Product policy is locked in PRD (`common-docs/PROJECT_PRD.md`).
- API snapshot is OpenAPI 3.1 JSON (`common-docs/openapi/openapi.v1.json`).
- Runtime OpenAPI generation and sync script: `aini-inu-backend/scripts/export-openapi.sh`.
- OpenAPI governance and update rules: `common-docs/openapi/README.md`.
- API envelope convention uses `ApiResponse<T>` (`aini-inu-backend/src/main/java/scit/ainiinu/common/response/ApiResponse.java`).

## 6) Testing and Quality Stack
- Test framework: JUnit Platform via `spring-boot-starter-test` (`aini-inu-backend/build.gradle`).
- Security test helper: `spring-security-test` (`aini-inu-backend/build.gradle`).
- Test DB baseline is H2 in PostgreSQL mode (`aini-inu-backend/src/test/resources/application-test.properties`).
- Contract/slice/integration tests exist across domains (examples: `aini-inu-backend/src/test/java/scit/ainiinu/community/contract/StoryOpenApiContractTest.java`, `aini-inu-backend/src/test/java/scit/ainiinu/chat/integration/ChatWebSocketIntegrationTest.java`).

## 7) Build and Runtime Operations
- Build tool/wrapper: Gradle wrapper (`aini-inu-backend/gradlew`, `aini-inu-backend/gradle/wrapper/gradle-wrapper.properties`).
- Container build is multi-stage (Gradle builder -> Temurin JRE) (`aini-inu-backend/Dockerfile`).
- Local compose runtime includes `pgvector/pgvector:pg16` and backend service (`aini-inu-backend/docker-compose.yml`).

## 8) Frontend Stack Snapshot (Secondary, Pre-Refactor)
- Framework versions currently in repo: Next 16.1.6 + React 19.2.3 + TypeScript 5 (`aini-inu-frontend/package.json`).
- Styling/state/tooling: Tailwind CSS 4, Zustand, ESLint 9, MSW (`aini-inu-frontend/package.json`, `aini-inu-frontend/src/mocks/handlers.ts`).
- API access layer uses fixed base path `/api/v1` (`aini-inu-frontend/src/services/api/apiClient.ts`).
- Map/location libraries include Leaflet + Nominatim fetch + Daum postcode (`aini-inu-frontend/src/components/common/DynamicMap.tsx`, `aini-inu-frontend/src/services/api/locationService.ts`, `aini-inu-frontend/src/app/around-me/page.tsx`).
- Legacy drift signal exists in progress docs (historical snapshot vs current package versions) (`aini-inu-frontend/PROGRESS_REPORT.md`, `aini-inu-frontend/package.json`).
