# Repository Structure Map (Practical)

## Monorepo Root
- `AGENTS.md`: repository-level operating rules and priorities.
- `aini-inu-backend`: primary runtime implementation and API contract execution.
- `common-docs`: primary product policy and contract snapshot storage.
- `aini-inu-frontend`: pre-refactor client application (adaptation layer).
- `.planning/codebase`: analysis outputs for architecture and structure.

## Backend Module (`aini-inu-backend`)
- `aini-inu-backend/build.gradle`: Spring Boot 3.5 + Java 21 + Spring AI + OpenAPI dependencies.
- `aini-inu-backend/settings.gradle`: Gradle project identity.
- `aini-inu-backend/src/main/java/scit/ainiinu/AiniInuApplication.java`: backend bootstrap entrypoint.
- `aini-inu-backend/src/main/java/scit/ainiinu/common/config/OpenApiConfig.java`: OpenAPI grouping and bearer security config.
- `aini-inu-backend/src/main/java/scit/ainiinu/common/config/WebConfig.java`: interceptor, resolver, and CORS wiring.
- `aini-inu-backend/src/main/java/scit/ainiinu/common/security/interceptor/JwtAuthInterceptor.java`: JWT gate for `/api/**`.
- `aini-inu-backend/src/main/java/scit/ainiinu/common/response/ApiResponse.java`: response envelope contract.
- `aini-inu-backend/src/main/java/scit/ainiinu/common/exception/GlobalExceptionHandler.java`: global error translation.

## Backend Domain Packages
- `aini-inu-backend/src/main/java/scit/ainiinu/member`: auth/profile/follow domain.
- `aini-inu-backend/src/main/java/scit/ainiinu/pet`: pet catalog and pet profile domain.
- `aini-inu-backend/src/main/java/scit/ainiinu/walk`: walk thread + walk diary domain.
- `aini-inu-backend/src/main/java/scit/ainiinu/chat`: chat rooms/messages/reviews + websocket realtime.
- `aini-inu-backend/src/main/java/scit/ainiinu/community`: posts/comments/story projection + image upload.
- `aini-inu-backend/src/main/java/scit/ainiinu/lostpet`: report/analyze/match workflow with AI and external chat link.
- Representative controllers: `.../member/controller/AuthController.java`, `.../walk/controller/WalkThreadController.java`, `.../community/controller/PostController.java`, `.../lostpet/controller/LostPetController.java`.
- Representative services: `.../walk/service/WalkThreadService.java`, `.../community/service/StoryService.java`, `.../lostpet/service/LostPetAnalyzeService.java`.
- Representative DTO split: `.../walk/dto/request/ThreadCreateRequest.java`, `.../chat/dto/response/ChatRoomDetailResponse.java`.

## Backend Data and Ops
- `aini-inu-backend/src/main/resources/application.properties`: datasource, JWT, lostpet, community storage, Spring AI settings.
- `aini-inu-backend/src/main/resources/db/ddl/01_walk_indexes_constraints.sql`: schema/index constraints baseline.
- `aini-inu-backend/src/main/resources/db/ddl/06_legacy_story_cleanup.sql`: legacy cleanup alignment script.
- `aini-inu-backend/src/main/resources/db/seed/00_lookup_seed.sql`: lookup seed.
- `aini-inu-backend/src/main/resources/db/seed/10_core_sample_seed.sql`: core sample data.
- `aini-inu-backend/scripts/export-openapi.sh`: runtime OpenAPI export to docs.
- `aini-inu-backend/scripts/docker-up.sh`: local backend + DB startup helper.
- `aini-inu-backend/docker-compose.yml`: dockerized runtime composition.

## Backend Testing Layout
- `aini-inu-backend/src/test/java/scit/ainiinu/common/contract/OpenApiAuthContractTest.java`: auth/security contract verification.
- `aini-inu-backend/src/test/java/scit/ainiinu/common/contract/OpenApiRequestSchemaContractTest.java`: request schema contract verification.
- `aini-inu-backend/src/test/java/scit/ainiinu/community/contract/StoryOpenApiContractTest.java`: story schema exposure check.
- `aini-inu-backend/src/test/java/scit/ainiinu/walk/integration/WalkDiaryCrudIntegrationTest.java`: walk diary integration path.
- `aini-inu-backend/src/test/resources/application-test.properties`: isolated test profile and H2 config.

## Docs Module (`common-docs`)
- `common-docs/PROJECT_PRD.md`: product policy, feature requirements, terminology lock.
- `common-docs/openapi/openapi.v1.json`: tracked runtime API snapshot.
- `common-docs/openapi/README.md`: snapshot governance and update flow.
- `common-docs/images/README.md`: image source-of-truth and sync rules.
- `common-docs/images/*`: shared asset source consumed by frontend symlinks.

## Frontend Module (`aini-inu-frontend`, Pre-Refactor)
- `aini-inu-frontend/package.json`: Next.js runtime scripts (`dev`, `build`, `lint`).
- `aini-inu-frontend/next.config.ts`: frontend runtime image policy.
- `aini-inu-frontend/src/app/layout.tsx`: global shell, route guard, providers.
- `aini-inu-frontend/src/app/around-me/page.tsx`: neighborhood radar route.
- `aini-inu-frontend/src/app/chat/page.tsx`: chat route entry.
- `aini-inu-frontend/src/services/api/apiClient.ts`: API envelope-aware fetch wrapper.
- `aini-inu-frontend/src/services/api/threadService.ts`: thread API adaptation.
- `aini-inu-frontend/src/hooks/useRadarLogic.ts`: route-level domain logic extraction target.
- `aini-inu-frontend/src/mocks/MSWProvider.tsx`: mock activation boundary.
- `aini-inu-frontend/src/mocks/handlers.ts`: mock contract simulation (still substantial).
- `aini-inu-frontend/public/images -> ../../common-docs/images`: symlinked shared asset path.

## Practical Navigation Order for Current Priorities
1. Start policy checks at `common-docs/PROJECT_PRD.md`.
2. Verify contract surface in `common-docs/openapi/openapi.v1.json`.
3. Trace runtime behavior in `aini-inu-backend/src/main/java/scit/ainiinu/**`.
4. Verify tests in `aini-inu-backend/src/test/java/scit/ainiinu/**`.
5. Touch `aini-inu-frontend` only when backend/doc contract impact requires adaptation.
