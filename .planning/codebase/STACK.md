# STACK

## Scope
- This document maps the implemented technology stack in this repository (`/Users/keonhongkoo/Desktop/github/aini-inu`) with file-backed evidence.
- Primary stack sources are `aini-inu-backend/build.gradle`, `aini-inu-backend/src/main/resources/application.properties`, `aini-inu-frontend/package.json`, and `aini-inu-frontend/tsconfig.json`.

## Repository Shape
- Backend service: `aini-inu-backend`.
- Frontend app: `aini-inu-frontend`.
- Shared specs/docs: `common-docs`.
- Planning outputs: `.planning/codebase`.

## Backend Stack (`aini-inu-backend`)

### Runtime and Language
- Java 21 toolchain is pinned via `JavaLanguageVersion.of(21)` in `aini-inu-backend/build.gradle`.
- Gradle wrapper is configured at `aini-inu-backend/gradle/wrapper/gradle-wrapper.properties` (distribution `gradle-8.14.4-bin.zip`).

### Core Framework
- Spring Boot `3.5.10` with dependency management plugin is declared in `aini-inu-backend/build.gradle`.
- Service is a conventional Spring MVC/JPA application rooted at `aini-inu-backend/src/main/java/scit/ainiinu/AiniInuApplication.java`.

### Web/API Layer
- REST controllers are organized by domain under `aini-inu-backend/src/main/java/scit/ainiinu/*/controller/*Controller.java`.
- Global API envelope pattern uses `ApiResponse<T>` in `aini-inu-backend/src/main/java/scit/ainiinu/common/response/ApiResponse.java`.
- OpenAPI generation is configured with springdoc in `aini-inu-backend/src/main/java/scit/ainiinu/common/config/OpenApiConfig.java` and dependency `org.springdoc:springdoc-openapi-starter-webmvc-ui` in `aini-inu-backend/build.gradle`.

### Data and Persistence
- Spring Data JPA starter is enabled in `aini-inu-backend/build.gradle`.
- Primary DB target is PostgreSQL via `org.postgresql:postgresql` and datasource defaults in `aini-inu-backend/src/main/resources/application.properties`.
- SQL bootstrap and seed pipeline are configured through `spring.sql.init.*` in `aini-inu-backend/src/main/resources/application.properties` and files in `aini-inu-backend/src/main/resources/db/ddl` + `aini-inu-backend/src/main/resources/db/seed`.
- JPA auditing is enabled by `@EnableJpaAuditing` in `aini-inu-backend/src/main/java/scit/ainiinu/common/config/JpaConfig.java`.

### Security and Auth
- JWT stack uses `io.jsonwebtoken:jjwt-*` dependencies from `aini-inu-backend/build.gradle`.
- Token generation/validation is implemented in `aini-inu-backend/src/main/java/scit/ainiinu/common/security/jwt/JwtTokenProvider.java`.
- Request authentication is interceptor-based (`JwtAuthInterceptor`) rather than filter-chain auth, configured in `aini-inu-backend/src/main/java/scit/ainiinu/common/config/WebConfig.java` and `aini-inu-backend/src/main/java/scit/ainiinu/common/config/SecurityConfig.java`.

### Realtime
- WebSocket/STOMP broker is enabled in `aini-inu-backend/src/main/java/scit/ainiinu/chat/config/WebSocketConfig.java`.
- STOMP auth interception is handled in `aini-inu-backend/src/main/java/scit/ainiinu/chat/config/ChatStompAuthChannelInterceptor.java`.
- Realtime event publishing uses `SimpMessagingTemplate` in `aini-inu-backend/src/main/java/scit/ainiinu/chat/realtime/StompChatRealtimePublisher.java`.

### AI / Semantic Search
- Spring AI BOM `1.1.2` and Google GenAI embedding + PgVector starters are declared in `aini-inu-backend/build.gradle`.
- Embedding/vector config lives in `aini-inu-backend/src/main/resources/application.properties` (`spring.ai.google.genai.*`, `spring.ai.vectorstore.pgvector.*`).
- Lost-pet semantic retrieval logic is implemented in `aini-inu-backend/src/main/java/scit/ainiinu/lostpet/integration/ai/LostPetAiClientImpl.java`.
- Vector DB schema alignment exists in `aini-inu-backend/src/main/resources/db/ddl/03_lostpet_indexes_constraints.sql`.

### Resilience / Integration Infrastructure
- Resilience4j retry is enabled through dependency `io.github.resilience4j:resilience4j-spring-boot3` in `aini-inu-backend/build.gradle`.
- Retry policy for lost-pet chat bridge is configured in `aini-inu-backend/src/main/resources/application.properties` (`resilience4j.retry.instances.lostpetChatDirect.*`).
- Outbound HTTP client wiring uses `RestTemplate` bean in `aini-inu-backend/src/main/java/scit/ainiinu/lostpet/config/LostPetClientConfig.java`.

### Packaging and Local Infra
- Multi-stage container image build is defined in `aini-inu-backend/Dockerfile` (Gradle builder + Temurin JRE runtime).
- Local compose orchestration uses `pgvector/pgvector:pg16` and backend service in `aini-inu-backend/docker-compose.yml`.
- Backend utility scripts include `aini-inu-backend/scripts/docker-up.sh` and `aini-inu-backend/scripts/export-openapi.sh`.

### Testing Stack
- Test dependencies include Spring Boot Test, Spring Security Test, and H2 in `aini-inu-backend/build.gradle`.
- Backend test profile uses H2 PostgreSQL mode in `aini-inu-backend/src/test/resources/application-test.properties`.
- Test styles present: unit (`MockitoExtension`), slice (`@WebMvcTest`), and integration (`@SpringBootTest`) across `aini-inu-backend/src/test/java/scit/ainiinu/**`.

## Frontend Stack (`aini-inu-frontend`)

### Runtime and Language
- Next.js `16.1.6` + React `19.2.3` + TypeScript `5.9.x` are pinned in `aini-inu-frontend/package.json`.
- Strict TS config with path alias `@/*` is set in `aini-inu-frontend/tsconfig.json`.

### UI and Styling
- Tailwind CSS v4 pipeline is configured via `aini-inu-frontend/package.json` and `aini-inu-frontend/postcss.config.mjs` (`@tailwindcss/postcss`).
- Iconography uses `lucide-react` from `aini-inu-frontend/package.json`.
- Theme switching uses `next-themes` and provider wiring in `aini-inu-frontend/src/components/common/ThemeProvider.tsx` + `aini-inu-frontend/src/app/layout.tsx`.

### State and Client Data Flow
- Client state is managed with Zustand + persist middleware in `aini-inu-frontend/src/store/useConfigStore.ts` and `aini-inu-frontend/src/store/useUserStore.ts`.
- API calls use custom fetch wrapper in `aini-inu-frontend/src/services/api/apiClient.ts` with base `/api/v1`.
- Domain-level API clients live in `aini-inu-frontend/src/services/api/*Service.ts`.

### Mocking and Local Simulation
- MSW is included as dev dependency in `aini-inu-frontend/package.json`.
- Browser worker setup is in `aini-inu-frontend/src/mocks/browser.ts` and runtime bootstrapping in `aini-inu-frontend/src/mocks/MSWProvider.tsx`.
- Additional mock fallback logic exists in `aini-inu-frontend/src/lib/mockApi.ts`.

### Mapping and Geo UI
- Map stack uses `leaflet` + `react-leaflet` dependencies (`aini-inu-frontend/package.json`) and rendering in `aini-inu-frontend/src/components/common/DynamicMap.tsx`.
- Around-me orchestration logic is in `aini-inu-frontend/src/hooks/useRadarLogic.ts`.

### Frontend Build and Quality Commands
- Core npm scripts are declared in `aini-inu-frontend/package.json` (`dev`, `build`, `start`, `lint`, `check:assets`).
- ESLint setup is in `aini-inu-frontend/eslint.config.mjs`.

## Shared Contract/Docs Layer
- Product/requirement source lives in `common-docs/PROJECT_PRD.md`.
- API snapshot process is defined in `common-docs/openapi/README.md` and exported by `aini-inu-backend/scripts/export-openapi.sh` to `common-docs/openapi/openapi.v1.json`.

## Practical Stack Notes
- Backend is already provisioned for realtime STOMP (`WebSocketConfig`) while current frontend chat screen uses polling in `aini-inu-frontend/src/app/chat/[id]/page.tsx`.
- Backend is production-oriented on PostgreSQL/pgvector, but tests intentionally isolate on H2 (`application-test.properties`).
- Frontend network layer assumes same-origin `/api/v1` by default (`apiClient.ts`), so reverse-proxy or route bridging is required for split-host deployments.
