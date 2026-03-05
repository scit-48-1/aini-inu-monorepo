# INTEGRATIONS

## Scope
- This document maps external and cross-service integrations currently wired in code.
- Evidence comes from backend config/code (`aini-inu-backend/src/main/**`) and frontend service/runtime paths (`aini-inu-frontend/src/**`).

## Integration Inventory (At a Glance)
| Integration | Direction | Transport | Key Config | Primary References |
|---|---|---|---|---|
| Frontend <-> Backend API | Frontend -> Backend | HTTP/JSON | Base path `/api/v1` | `aini-inu-frontend/src/services/api/apiClient.ts`, `aini-inu-backend/src/main/java/scit/ainiinu/*/controller/*Controller.java` |
| Backend <-> PostgreSQL/pgvector | Backend -> DB | JDBC | `SPRING_DATASOURCE_*`, `spring.ai.vectorstore.pgvector.*` | `aini-inu-backend/src/main/resources/application.properties`, `aini-inu-backend/docker-compose.yml` |
| Backend <-> Google GenAI Embedding | Backend -> Google | Spring AI model API | `GEMINI_API_KEY`, `GEMINI_EMBEDDING_MODEL` | `aini-inu-backend/build.gradle`, `aini-inu-backend/src/main/resources/application.properties` |
| Backend <-> Animal Registry API | Backend -> Public API | HTTP GET | `animal.registry.api.*` | `aini-inu-backend/src/main/java/scit/ainiinu/pet/service/AnimalCertificationService.java`, `aini-inu-backend/src/main/resources/application.properties` |
| Backend <-> External Chat Service | Backend -> Internal/External service | HTTP POST | `LOSTPET_CHAT_BASE_URL`, `LOSTPET_CHAT_DIRECT_CREATE_PATH` | `aini-inu-backend/src/main/java/scit/ainiinu/lostpet/integration/chat/ChatRoomDirectClientImpl.java` |
| Chat Realtime Socket | Client -> Backend | WebSocket/STOMP | endpoint `/ws/chat-rooms/{roomId}` | `aini-inu-backend/src/main/java/scit/ainiinu/chat/config/WebSocketConfig.java` |
| Frontend <-> Nominatim | Frontend -> OSM | HTTP GET | none | `aini-inu-frontend/src/services/api/locationService.ts` |
| Frontend <-> Daum Postcode | Frontend -> Daum widget | Browser SDK | none | `aini-inu-frontend/src/app/around-me/page.tsx` |
| Frontend API Route <-> Google GenAI | Next server route -> Google | SDK call | `NEXT_PUBLIC_GEMINI_API_KEY` | `aini-inu-frontend/src/app/api/check-key/route.ts` |
| Community Image Storage (local) | Backend -> Local FS | local file I/O + HTTP URLs | `community.storage.*` | `aini-inu-backend/src/main/java/scit/ainiinu/community/service/ImageUploadService.java` |

## Integration Details

### 1) Frontend -> Backend REST APIs
- Client base URL is relative `/api/v1` in `aini-inu-frontend/src/services/api/apiClient.ts`.
- API consumers are split by domain in `aini-inu-frontend/src/services/api/chatService.ts`, `threadService.ts`, `postService.ts`, `memberService.ts`, and `authService.ts`.
- Backend exposes domain APIs under `/api/v1/**` controllers in:
  - `aini-inu-backend/src/main/java/scit/ainiinu/chat/controller/ChatController.java`
  - `aini-inu-backend/src/main/java/scit/ainiinu/walk/controller/WalkThreadController.java`
  - `aini-inu-backend/src/main/java/scit/ainiinu/walk/controller/WalkDiaryController.java`
  - `aini-inu-backend/src/main/java/scit/ainiinu/member/controller/MemberController.java`
  - `aini-inu-backend/src/main/java/scit/ainiinu/member/controller/AuthController.java`
  - `aini-inu-backend/src/main/java/scit/ainiinu/community/controller/PostController.java`
- CORS for `/api/**` allows `http://localhost:3000` and `http://localhost:5173` in `aini-inu-backend/src/main/java/scit/ainiinu/common/config/WebConfig.java`.
- Note: No active Next.js rewrite/proxy is defined in `aini-inu-frontend/next.config.ts`; split-origin local runs rely on infra/proxy outside this file.

### 2) Backend -> PostgreSQL + pgvector
- Datasource defaults and credentials are declared in `aini-inu-backend/src/main/resources/application.properties`.
- Local infra provisions pgvector-enabled Postgres image `pgvector/pgvector:pg16` in `aini-inu-backend/docker-compose.yml`.
- Vector extension and table/index setup are codified in `aini-inu-backend/src/main/resources/db/ddl/03_lostpet_indexes_constraints.sql` (`CREATE EXTENSION vector`, `lostpet_vector_store`, HNSW index).
- Lost-pet vector search reads from `VectorStore` in `aini-inu-backend/src/main/java/scit/ainiinu/lostpet/integration/ai/LostPetAiClientImpl.java`.

### 3) Backend -> Google GenAI Embeddings (Spring AI)
- Dependency wiring is in `aini-inu-backend/build.gradle`:
  - `org.springframework.ai:spring-ai-starter-model-google-genai-embedding`
  - `org.springframework.ai:spring-ai-starter-vector-store-pgvector`
- Runtime properties are in `aini-inu-backend/src/main/resources/application.properties`:
  - `spring.ai.google.genai.embedding.api-key`
  - `spring.ai.google.genai.embedding.text.options.model`
- Local env examples are in `aini-inu-backend/.env.example` and `aini-inu-backend/.env.docker.example`.

### 4) Backend -> External Animal Registry API
- Outbound call implementation: `aini-inu-backend/src/main/java/scit/ainiinu/pet/service/AnimalCertificationService.java`.
- Endpoint and key config live in `aini-inu-backend/src/main/resources/application.properties` (`animal.registry.api.url`, `animal.registry.api.key`).
- Integration semantics:
  - HTTP GET with query params (`serviceKey`, `dog_reg_no`, `_type=json`).
  - Business exceptions map API/network failures to domain error codes.

### 5) Backend -> LostPet Chat Direct Service
- Outbound client: `aini-inu-backend/src/main/java/scit/ainiinu/lostpet/integration/chat/ChatRoomDirectClientImpl.java`.
- Base URL/path from `aini-inu-backend/src/main/resources/application.properties` (`lostpet.chat.base-url`, `lostpet.chat.direct-create-path`).
- Retry policy is configured via Resilience4j in `aini-inu-backend/src/main/resources/application.properties` and activated with `@Retry` in `ChatRoomDirectClientImpl`.
- Client bean is registered in `aini-inu-backend/src/main/java/scit/ainiinu/lostpet/config/LostPetClientConfig.java`.

### 6) Client <-> Backend Realtime (STOMP)
- Backend STOMP endpoint is `/ws/chat-rooms/{roomId}` from `aini-inu-backend/src/main/java/scit/ainiinu/chat/config/WebSocketConfig.java`.
- Broker destinations use `/topic` and `/queue`; app prefix `/app`; user prefix `/user` in the same file.
- Authorization on CONNECT is enforced by `aini-inu-backend/src/main/java/scit/ainiinu/chat/config/ChatStompAuthChannelInterceptor.java` (expects `Authorization: Bearer <token>`).
- Event publication target `/topic/chat-rooms/{roomId}/events` is in `aini-inu-backend/src/main/java/scit/ainiinu/chat/realtime/StompChatRealtimePublisher.java`.

### 7) Frontend -> Nominatim Geocoding
- Geocoding calls hit `https://nominatim.openstreetmap.org/search` in `aini-inu-frontend/src/services/api/locationService.ts`.
- Integration behavior is client-side with timeout + fallback coordinates in the same file.

### 8) Frontend -> Daum Postcode
- Address search modal uses `react-daum-postcode` in `aini-inu-frontend/src/app/around-me/page.tsx`.
- Selection callback updates local location state in that same page via `setCurrentLocation(data.address)`.

### 9) Next Route -> Google GenAI SDK (diagnostic)
- Diagnostic API route in `aini-inu-frontend/src/app/api/check-key/route.ts` invokes `@google/genai`.
- It reads `process.env.NEXT_PUBLIC_GEMINI_API_KEY` and runs model `gemini-1.5-flash`.
- This is independent from backend Spring AI flow.

### 10) Backend -> Local Filesystem Image Storage
- Presigned-like upload token flow is implemented in `aini-inu-backend/src/main/java/scit/ainiinu/community/service/ImageUploadService.java`.
- Config properties are bound by `aini-inu-backend/src/main/java/scit/ainiinu/community/config/CommunityStorageProperties.java` and `community.storage.*` settings in `aini-inu-backend/src/main/resources/application.properties`.
- Docker runtime mounts upload volume in `aini-inu-backend/docker-compose.yml`.

## Environment Variables by Integration
- Auth/security: `JWT_SECRET` (`aini-inu-backend/.env.example`, `aini-inu-backend/.env.docker.example`).
- AI/vector: `GEMINI_API_KEY`, `GEMINI_EMBEDDING_MODEL`, `SPRING_AI_PGVECTOR_*` (`aini-inu-backend/.env.example`).
- Chat bridge: `LOSTPET_CHAT_BASE_URL`, `LOSTPET_CHAT_DIRECT_CREATE_PATH` (`aini-inu-backend/.env.example`).
- DB: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` (`aini-inu-backend/.env.example`).
- Community upload: `COMMUNITY_STORAGE_PRESIGNED_EXPIRES_SECONDS` (`aini-inu-backend/.env.docker.example`).
- Frontend diagnostic key: `NEXT_PUBLIC_GEMINI_API_KEY` (read in `aini-inu-frontend/src/app/api/check-key/route.ts`).

## Current Contract Drift Hotspots (Code vs Code)
- Chat path mismatch: frontend uses `/chat/rooms*` in `aini-inu-frontend/src/services/api/chatService.ts`, backend exposes `/chat-rooms*` in `aini-inu-backend/src/main/java/scit/ainiinu/chat/controller/ChatController.java`.
- Direct-room creation mismatch: frontend posts `/chat/rooms`, backend expects `POST /api/v1/chat-rooms/direct` (`chatService.ts` vs `ChatController.java`).
- Follow endpoint mismatch: frontend uses `/members/me/follow/{id}` in `aini-inu-frontend/src/services/api/memberService.ts`, backend exposes `/members/me/follows/{id}` in `MemberController.java`.
- Member search mismatch: frontend uses `/members?q=...` in `memberService.ts`, backend exposes `/members/search?q=...` in `MemberController.java`.
- Thread apply mismatch: frontend calls `/threads/{id}/join` in `aini-inu-frontend/src/services/api/threadService.ts`, backend exposes `/threads/{id}/apply` in `WalkThreadController.java`.
- Walk diary save mismatch: frontend posts `/walk-diaries/{id}` in `threadService.ts`, backend exposes `PATCH /walk-diaries/{diaryId}` in `WalkDiaryController.java`.
- Post update mismatch: frontend uses `PUT /posts/{id}` in `postService.ts`, backend exposes `PATCH /posts/{postId}` in `PostController.java`.
- Auth feature mismatch: frontend has `/auth/email/send` and `/auth/email/verify` calls in `aini-inu-frontend/src/services/authService.ts`, but these endpoints are absent from `aini-inu-backend/src/main/java/scit/ainiinu/member/controller/AuthController.java`.
- Pet analyze mismatch: frontend calls `POST /api/v1/pets/analyze` in `aini-inu-frontend/src/services/geminiService.ts`, but `PetController.java` does not define `/pets/analyze`.

## Quick Verification Entry Points
- Backend OpenAPI runtime endpoint: `/v3/api-docs` (documented in `common-docs/openapi/README.md`, exported by `aini-inu-backend/scripts/export-openapi.sh`).
- Backend Swagger UI: `/swagger-ui/index.html` (noted in `aini-inu-backend/README_FRONTEND_ONBOARDING.md`).
- Docker baseline for integration smoke checks: `aini-inu-backend/scripts/docker-up.sh`.
