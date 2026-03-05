# Aini-Inu Architecture

## Scope
- This document summarizes the current architecture across `aini-inu-backend`, `aini-inu-frontend`, and `common-docs`.
- The map is based on runtime/config/code anchors such as `aini-inu-backend/build.gradle`, `aini-inu-backend/src/main/resources/application.properties`, and `aini-inu-frontend/package.json`.

## Workspace Topology
- The root workspace is an umbrella directory with three active project roots: `aini-inu-backend`, `aini-inu-frontend`, and `common-docs`.
- Backend and frontend are maintained as independent app roots (`aini-inu-backend/.git`, `aini-inu-frontend/.git`), while shared contracts/assets live in `common-docs`.
- API contract snapshots are exported to `common-docs/openapi/openapi.v1.json` via `aini-inu-backend/scripts/export-openapi.sh`.
- Static design assets are source-of-truth in `common-docs/images` and consumed via symlinks under `aini-inu-frontend/public`.

## Backend Architecture (Spring Boot 3.5, Java 21)
- Runtime entrypoint: `aini-inu-backend/src/main/java/scit/ainiinu/AiniInuApplication.java`.
- Core stack is defined in `aini-inu-backend/build.gradle` (Web, JPA, Validation, WebSocket, Security, Spring AI, OpenAPI, Resilience4j).
- Domain-oriented package split under `aini-inu-backend/src/main/java/scit/ainiinu`: `member`, `pet`, `walk`, `chat`, `community`, `lostpet`, plus `common`.
- Typical request flow is Controller -> Service -> Repository -> Entity, e.g.:
- `aini-inu-backend/src/main/java/scit/ainiinu/community/controller/PostController.java`
- `aini-inu-backend/src/main/java/scit/ainiinu/community/service/PostService.java`
- `aini-inu-backend/src/main/java/scit/ainiinu/community/repository/PostRepository.java`
- `aini-inu-backend/src/main/java/scit/ainiinu/community/entity/Post.java`
- Cross-domain references are mostly ID-based (example: `authorId`, `memberId`) to reduce direct aggregate coupling across contexts.

## Backend Security and API Contract Layer
- Security filter defaults are disabled in `aini-inu-backend/src/main/java/scit/ainiinu/common/config/SecurityConfig.java`.
- JWT auth is enforced at MVC interceptor level:
- `aini-inu-backend/src/main/java/scit/ainiinu/common/config/WebConfig.java`
- `aini-inu-backend/src/main/java/scit/ainiinu/common/security/interceptor/JwtAuthInterceptor.java`
- `aini-inu-backend/src/main/java/scit/ainiinu/common/security/resolver/CurrentMemberArgumentResolver.java`
- Public endpoint bypass is annotation-driven via `@Public` handling in both interceptor and OpenAPI customizer (`aini-inu-backend/src/main/java/scit/ainiinu/common/config/OpenApiConfig.java`).
- API response envelope is standardized by `aini-inu-backend/src/main/java/scit/ainiinu/common/response/ApiResponse.java`.
- Exception mapping is centralized in `aini-inu-backend/src/main/java/scit/ainiinu/common/exception/GlobalExceptionHandler.java`.

## Backend Communication Patterns
- Synchronous REST is the primary external interface (`/api/v1/**` controllers in domain packages).
- Realtime chat events use STOMP over WebSocket:
- endpoint config in `aini-inu-backend/src/main/java/scit/ainiinu/chat/config/WebSocketConfig.java`
- publish abstraction in `aini-inu-backend/src/main/java/scit/ainiinu/chat/realtime/ChatRealtimePublisher.java`
- STOMP implementation in `aini-inu-backend/src/main/java/scit/ainiinu/chat/realtime/StompChatRealtimePublisher.java`
- event emission from `aini-inu-backend/src/main/java/scit/ainiinu/chat/service/MessageService.java`.
- Lost-pet analysis integrates vector similarity and remote chat-room linking:
- vector client in `aini-inu-backend/src/main/java/scit/ainiinu/lostpet/integration/ai/LostPetAiClientImpl.java`
- chat integration in `aini-inu-backend/src/main/java/scit/ainiinu/lostpet/integration/chat/ChatRoomDirectClientImpl.java`
- orchestration service in `aini-inu-backend/src/main/java/scit/ainiinu/lostpet/service/LostPetAnalyzeService.java` and `aini-inu-backend/src/main/java/scit/ainiinu/lostpet/service/LostPetMatchApprovalService.java`.

## Data and Runtime Infrastructure
- Primary datastore is PostgreSQL + pgvector, wired in `aini-inu-backend/docker-compose.yml`.
- Schema/bootstrap SQL is loaded from `aini-inu-backend/src/main/resources/db/ddl/*.sql` and `aini-inu-backend/src/main/resources/db/seed/*.sql`.
- Local/docker env profiles are documented in `aini-inu-backend/README_FRONTEND_ONBOARDING.md` and `.env` templates.
- Common entity audit fields are provided by `aini-inu-backend/src/main/java/scit/ainiinu/common/entity/BaseTimeEntity.java` with auditing enabled in `aini-inu-backend/src/main/java/scit/ainiinu/common/config/JpaConfig.java`.

## Frontend Architecture (Next.js App Router)
- Runtime framework: Next.js + React + TypeScript (`aini-inu-frontend/package.json`, `aini-inu-frontend/tsconfig.json`).
- App shell and global providers are composed in `aini-inu-frontend/src/app/layout.tsx` (`ThemeProvider`, `MSWProvider`, sidebar shell, route-based auth gate).
- Route entrypoints are under `aini-inu-frontend/src/app`, e.g.:
- `aini-inu-frontend/src/app/dashboard/page.tsx`
- `aini-inu-frontend/src/app/feed/page.tsx`
- `aini-inu-frontend/src/app/chat/[id]/page.tsx`
- UI is componentized by feature folders in `aini-inu-frontend/src/components/*` and shared primitives in `aini-inu-frontend/src/components/ui/*`.
- Client state uses Zustand stores in `aini-inu-frontend/src/store/useUserStore.ts` and `aini-inu-frontend/src/store/useConfigStore.ts`.

## Frontend Data Access and Mocking Strategy
- Service layer wrappers live under `aini-inu-frontend/src/services/api/*` and route through `aini-inu-frontend/src/services/api/apiClient.ts` (`/api/v1` base).
- Development mock interception is done by MSW:
- bootstrap `aini-inu-frontend/src/mocks/MSWProvider.tsx`
- worker `aini-inu-frontend/src/mocks/browser.ts`
- handlers `aini-inu-frontend/src/mocks/handlers.ts`.
- This creates a mock-first local UX path, while backend integration contracts are tracked separately via `common-docs/openapi/openapi.v1.json`.
- Additional external fetch usage exists for geocoding in `aini-inu-frontend/src/services/api/locationService.ts` and Gemini diagnostic route in `aini-inu-frontend/src/app/api/check-key/route.ts`.

## Testing and Verification Architecture
- Backend tests are layered by purpose in `aini-inu-backend/src/test/java/scit/ainiinu`:
- unit/service (example `.../member/service/AuthServiceTest.java`)
- slice/contract (example `.../community/contract/StoryListContractTest.java`)
- integration (example `.../chat/integration/ChatWebSocketIntegrationTest.java`).
- Frontend has lint/build checks and asset integrity validation via `aini-inu-frontend/scripts/check-image-links.sh`.

## Architectural Summary
- The system is a multi-repo workspace with clear role separation:
- backend: domain/business and API runtime (`aini-inu-backend`)
- frontend: App Router UI and client orchestration (`aini-inu-frontend`)
- shared contracts/assets: PRD, OpenAPI snapshots, and static media (`common-docs`).
- Cross-repo coupling is intentionally explicit through generated API snapshots and symlinked asset contracts, not through shared runtime code.
