# Aini-Inu Architecture (Current State)

## Scope and Priority
- This monorepo currently prioritizes backend correctness and contract stability in `aini-inu-backend`.
- Documentation synchronization is second priority in `common-docs`.
- Frontend work in `aini-inu-frontend` is treated as pre-refactor and should avoid structural expansion.

## Decision and Contract Hierarchy
1. Product policy and vocabulary are anchored in `common-docs/PROJECT_PRD.md`.
2. API contract snapshot is tracked in `common-docs/openapi/openapi.v1.json`.
3. Runtime API behavior is implemented in `aini-inu-backend/src/main/java/scit/ainiinu/**`.
4. Frontend adaptation follows backend and docs, not the other way around.

## Backend Runtime Architecture
- Main entry point is `aini-inu-backend/src/main/java/scit/ainiinu/AiniInuApplication.java`.
- The backend is a domain-first Spring Boot monolith organized by bounded contexts under `scit.ainiinu`.
- Shared cross-cutting concerns live under `aini-inu-backend/src/main/java/scit/ainiinu/common`.
- Response envelope standard is centralized in `aini-inu-backend/src/main/java/scit/ainiinu/common/response/ApiResponse.java`.
- OpenAPI runtime generation and security mapping are configured in `aini-inu-backend/src/main/java/scit/ainiinu/common/config/OpenApiConfig.java`.

## Request Processing Flow (HTTP)
1. Requests enter controllers such as `aini-inu-backend/src/main/java/scit/ainiinu/walk/controller/WalkThreadController.java`.
2. JWT authentication is enforced by `aini-inu-backend/src/main/java/scit/ainiinu/common/security/interceptor/JwtAuthInterceptor.java`.
3. Authenticated member context is injected through `aini-inu-backend/src/main/java/scit/ainiinu/common/security/resolver/CurrentMemberArgumentResolver.java`.
4. Business logic executes in services such as `aini-inu-backend/src/main/java/scit/ainiinu/walk/service/WalkThreadService.java`.
5. Persistence is handled by repositories like `aini-inu-backend/src/main/java/scit/ainiinu/walk/repository/WalkThreadRepository.java`.
6. Exceptions are normalized by `aini-inu-backend/src/main/java/scit/ainiinu/common/exception/GlobalExceptionHandler.java`.
7. API responses are returned as `ApiResponse<T>` to keep contract consistency.

## Domain Modules (Backend)
- `walk`: threads and diaries (`.../walk/controller/WalkDiaryController.java`, `.../walk/entity/WalkDiary.java`).
- `chat`: room lifecycle, messages, reviews, and walk confirm (`.../chat/controller/ChatController.java`).
- `community`: posts, comments, image upload, story projection (`.../community/controller/PostController.java`, `.../community/controller/StoryController.java`).
- `lostpet`: reporting, AI analysis, candidate scoring, and match workflow (`.../lostpet/controller/LostPetController.java`, `.../lostpet/service/LostPetAnalyzeService.java`).
- `member`: auth/profile/follow and token lifecycle (`.../member/controller/AuthController.java`, `.../member/service/AuthService.java`).
- `pet`: pet metadata and profile ownership (`.../pet/controller/PetController.java`).

## Realtime and Integration
- STOMP websocket broker setup is in `aini-inu-backend/src/main/java/scit/ainiinu/chat/config/WebSocketConfig.java`.
- STOMP auth interception is in `aini-inu-backend/src/main/java/scit/ainiinu/chat/config/ChatStompAuthChannelInterceptor.java`.
- Realtime event publishing path is implemented by `aini-inu-backend/src/main/java/scit/ainiinu/chat/realtime/StompChatRealtimePublisher.java`.
- Lost-pet AI integration abstractions are in `aini-inu-backend/src/main/java/scit/ainiinu/lostpet/integration/ai/LostPetAiClient.java`.
- Lost-pet chat direct-connection integration is in `aini-inu-backend/src/main/java/scit/ainiinu/lostpet/integration/chat/ChatRoomDirectClientImpl.java`.

## Data and Initialization Architecture
- Core runtime datasource and integration properties are in `aini-inu-backend/src/main/resources/application.properties`.
- DDL alignment scripts are maintained under `aini-inu-backend/src/main/resources/db/ddl/*.sql`.
- Seed baselines are maintained under `aini-inu-backend/src/main/resources/db/seed/*.sql`.
- Dockerized local runtime lives in `aini-inu-backend/docker-compose.yml` and helper scripts under `aini-inu-backend/scripts/`.

## Contract and Docs Synchronization
- OpenAPI snapshot governance is documented in `common-docs/openapi/README.md`.
- Snapshot export is automated by `aini-inu-backend/scripts/export-openapi.sh`.
- The script writes the canonical snapshot to `common-docs/openapi/openapi.v1.json`.
- Policy and terminology lock (Story vs WalkDiary) is maintained in `common-docs/PROJECT_PRD.md`.

## Testing Architecture
- Test suite root is `aini-inu-backend/src/test/java/scit/ainiinu`.
- Contract tests validate runtime OpenAPI in `aini-inu-backend/src/test/java/scit/ainiinu/common/contract/OpenApiAuthContractTest.java`.
- Request schema contract checks are in `aini-inu-backend/src/test/java/scit/ainiinu/common/contract/OpenApiRequestSchemaContractTest.java`.
- Domain contract/integration/unit coverage is distributed by package, for example `.../walk`, `.../community`, and `.../lostpet`.
- Test profile and DB isolation are configured by `aini-inu-backend/src/test/resources/application-test.properties`.

## Frontend Position (Pre-Refactor)
- Frontend is Next.js App Router based in `aini-inu-frontend/src/app`.
- Current layout/session behavior is centralized in `aini-inu-frontend/src/app/layout.tsx`.
- API consumption abstraction is in `aini-inu-frontend/src/services/api/apiClient.ts`.
- Mock-first development path remains active through `aini-inu-frontend/src/mocks/MSWProvider.tsx` and `aini-inu-frontend/src/mocks/handlers.ts`.
- Image source-of-truth is shared from docs via symlinks in `aini-inu-frontend/public` to `common-docs/images`.

## Architectural Implication for Ongoing Work
- Backend and `common-docs` must move together whenever API behavior changes.
- OpenAPI and PRD updates are not optional add-ons; they are part of contract completion.
- Frontend changes should mainly reduce coupling and align with the existing backend contract.
