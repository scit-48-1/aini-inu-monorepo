# INTEGRATIONS

## 1) Scope and Priority Boundary
- This integration map is centered on backend + `common-docs` because they are current repo priorities (`AGENTS.md`).
- Frontend integrations are listed as secondary/pre-refactor context only (`AGENTS.md`, `aini-inu-frontend/**`).

## 2) Inbound Integrations (Into Backend)
### 2.1 REST API clients -> Backend
- Protocol: HTTP/JSON under `/api/v1/**`.
- Entrypoints: domain controllers under `aini-inu-backend/src/main/java/scit/ainiinu/*/controller/*.java`.
- Contract source: runtime OpenAPI + snapshot at `common-docs/openapi/openapi.v1.json`.
- Envelope convention required by backend: `ApiResponse<T>` in `aini-inu-backend/src/main/java/scit/ainiinu/common/response/ApiResponse.java`.

### 2.2 JWT bearer auth -> Backend interceptor
- Header contract: `Authorization: Bearer <token>`.
- Token issue/verify: `aini-inu-backend/src/main/java/scit/ainiinu/common/security/jwt/JwtTokenProvider.java`.
- Request auth gate: `aini-inu-backend/src/main/java/scit/ainiinu/common/security/interceptor/JwtAuthInterceptor.java`.
- MVC wiring and exclusions: `aini-inu-backend/src/main/java/scit/ainiinu/common/config/WebConfig.java`.

### 2.3 WebSocket STOMP chat clients -> Backend
- STOMP endpoint: `/ws/chat-rooms/{roomId}` in `aini-inu-backend/src/main/java/scit/ainiinu/chat/config/WebSocketConfig.java`.
- Broker topics: `/topic`, `/queue` and app prefix `/app` (`WebSocketConfig.java`).
- CONNECT auth handling: `aini-inu-backend/src/main/java/scit/ainiinu/chat/config/ChatStompAuthChannelInterceptor.java`.
- Event publishing destination: `/topic/chat-rooms/{chatRoomId}/events` in `aini-inu-backend/src/main/java/scit/ainiinu/chat/realtime/StompChatRealtimePublisher.java`.

### 2.4 Image upload clients -> Local storage flow
- Presigned URL issue/upload endpoints are in `aini-inu-backend/src/main/java/scit/ainiinu/community/controller/ImageController.java`.
- Core flow implementation: `aini-inu-backend/src/main/java/scit/ainiinu/community/service/ImageUploadService.java`.
- Storage config (public URL, local dir, expiry): `aini-inu-backend/src/main/java/scit/ainiinu/community/config/CommunityStorageProperties.java` and `aini-inu-backend/src/main/resources/application.properties`.

## 3) Outbound Integrations (Backend -> External/Other Services)
### 3.1 Animal Registry public API (Government data)
- Client implementation: `aini-inu-backend/src/main/java/scit/ainiinu/pet/service/AnimalCertificationService.java`.
- Transport: Spring `RestClient` GET with query params.
- Config keys: `animal.registry.api.key`, `animal.registry.api.url` in `aini-inu-backend/src/main/resources/application.properties`.
- Current configured endpoint: `http://apis.data.go.kr/1543061/animalInfoSrvc/animalInfo`.

### 3.2 LostPet -> Chat direct room HTTP integration
- Client implementation: `aini-inu-backend/src/main/java/scit/ainiinu/lostpet/integration/chat/ChatRoomDirectClientImpl.java`.
- HTTP client bean: `aini-inu-backend/src/main/java/scit/ainiinu/lostpet/config/LostPetClientConfig.java`.
- Config keys: `lostpet.chat.base-url`, `lostpet.chat.direct-create-path` in `aini-inu-backend/src/main/resources/application.properties`.
- Retry policy: `resilience4j.retry.instances.lostpetChatDirect.*` in `application.properties`.
- Error taxonomy classes: `ChatDirectAuthException`, `ChatDirectConnectException`, `ChatDirectResponseSchemaException` under `aini-inu-backend/src/main/java/scit/ainiinu/lostpet/integration/chat/`.

### 3.3 LostPet AI retrieval (Spring AI + pgvector)
- AI client implementation: `aini-inu-backend/src/main/java/scit/ainiinu/lostpet/integration/ai/LostPetAiClientImpl.java`.
- Dependencies: Spring AI Google GenAI embedding + pgvector in `aini-inu-backend/build.gradle`.
- Config keys: `spring.ai.google.genai.embedding.api-key`, `spring.ai.vectorstore.pgvector.*`, `lostpet.ai.vector-top-k` in `aini-inu-backend/src/main/resources/application.properties`.
- Vector schema baseline: `lostpet_vector_store` table/index in `aini-inu-backend/src/main/resources/db/ddl/03_lostpet_indexes_constraints.sql`.

### 3.4 Database platform integration
- Primary runtime DB: PostgreSQL/pgvector (`aini-inu-backend/docker-compose.yml`, `aini-inu-backend/src/main/resources/application.properties`).
- Init extensions for local container: `aini-inu-backend/docker/postgres/init/01_extensions.sql`.
- Bootstrapped schema/data source paths: `spring.sql.init.*` in `application.properties`.

## 4) Contract/Tooling Integrations
### 4.1 Runtime OpenAPI export to docs repo module
- Export script: `aini-inu-backend/scripts/export-openapi.sh`.
- Pull source from runtime endpoint: `/v3/api-docs`.
- Write target snapshot: `common-docs/openapi/openapi.v1.json`.
- Sync rule documentation: `common-docs/openapi/README.md`.

## 5) Frontend External Integrations (Secondary, Pre-Refactor)
### 5.1 Backend API integration path
- Shared client base path `/api/v1`: `aini-inu-frontend/src/services/api/apiClient.ts`.
- Domain API wrappers: `aini-inu-frontend/src/services/api/threadService.ts`, `aini-inu-frontend/src/services/api/chatService.ts`, `aini-inu-frontend/src/services/api/memberService.ts`.

### 5.2 Geocoding and map providers
- Nominatim geocoding call: `aini-inu-frontend/src/services/api/locationService.ts`.
- Leaflet map + Carto tiles: `aini-inu-frontend/src/components/common/DynamicMap.tsx`.
- Daum postcode script/client usage: `aini-inu-frontend/src/app/around-me/page.tsx`, `aini-inu-frontend/src/components/signup/ManagerStep.tsx`.

### 5.3 Frontend-side Gemini usage (diagnostic/legacy path)
- Next route using `@google/genai`: `aini-inu-frontend/src/app/api/check-key/route.ts`.
- Direct backend analyze call + mock fallback: `aini-inu-frontend/src/services/geminiService.ts`, `aini-inu-frontend/src/lib/mockApi.ts`.

### 5.4 Dev/test mocking integration
- MSW provider and handlers: `aini-inu-frontend/src/mocks/MSWProvider.tsx`, `aini-inu-frontend/src/mocks/handlers.ts`.
- Indicates mixed integration mode (real backend + mock APIs) during refactor transition.

## 6) Practical Ops Notes
- Backend integration changes should be synchronized with PRD/OpenAPI per governance in `common-docs/PROJECT_PRD.md` and `common-docs/openapi/README.md`.
- Integration stability focus should remain backend-first until frontend refactor boundary is finalized (`AGENTS.md`).
