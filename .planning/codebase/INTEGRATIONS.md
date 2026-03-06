# External Integrations

**Analysis Date:** 2026-03-06

## APIs & External Services

### Google Gemini (AI Embeddings)

- **Purpose:** Generate text embeddings for lost-pet AI matching. Sighting descriptions are vectorized and stored in PgVector; search queries use cosine similarity to find matching sightings.
- **SDK/Client:** Spring AI `spring-ai-starter-model-google-genai-embedding` (BOM version 1.1.2)
- **Auth env var:** `GEMINI_API_KEY`
- **Model:** `text-embedding-004` (configurable via `GEMINI_EMBEDDING_MODEL`)
- **Backend integration files:**
  - `aini-inu-backend/src/main/java/scit/ainiinu/lostpet/integration/ai/LostPetAiClientImpl.java` - Vector search and document indexing
  - `aini-inu-backend/src/main/java/scit/ainiinu/lostpet/integration/ai/LostPetAiClient.java` - Interface
  - `aini-inu-backend/src/main/java/scit/ainiinu/lostpet/integration/ai/LostPetAiCandidate.java` - Result DTO
  - `aini-inu-backend/src/main/java/scit/ainiinu/lostpet/integration/ai/LostPetAiResult.java` - Result wrapper
- **Configuration in:** `aini-inu-backend/src/main/resources/application.properties` (lines 58-68)
- **Graceful degradation:** Uses `ObjectProvider<VectorStore>` so the app starts even if the vector store is unavailable; indexing silently skips when unavailable.
- **Test profile:** Spring AI and PgVector auto-configuration excluded in `aini-inu-backend/src/test/resources/application-test.properties`

### Google Generative AI (Frontend, Client-Side)

- **Purpose:** Dog image analysis (breed detection) from the frontend
- **SDK/Client:** `@google/genai@1.40.0` npm package
- **Integration file:** `aini-inu-frontend/src/services/geminiService.ts`
- **Fallback:** When the backend is unavailable, falls back to `mockApi.analyzeImage()` in `aini-inu-frontend/src/lib/mockApi.ts`
- **Note:** The `geminiService.ts` currently calls `http://localhost:8080/api/v1/pets/analyze` (hardcoded URL, not using the apiClient proxy pattern)

### Korean Animal Registry API

- **Purpose:** Verify pet registration against the national government database
- **Endpoint:** `http://apis.data.go.kr/1543061/animalInfoSrvc/animalInfo`
- **Auth env var:** `ANIMAL_REGISTRY_API_KEY`
- **Configuration in:** `aini-inu-backend/src/main/resources/application.properties` (lines 36-37)
- **Frontend mock:** `aini-inu-frontend/src/lib/mockApi.ts` provides `mockApi.verifyDog()` for testing without the real API

### Nominatim (OpenStreetMap Geocoding)

- **Purpose:** Convert Korean addresses to lat/lng coordinates for map features
- **Client:** Direct `fetch()` call from the frontend (no SDK)
- **Endpoint:** `https://nominatim.openstreetmap.org/search?format=json`
- **Integration file:** `aini-inu-frontend/src/services/api/locationService.ts`
- **Auth:** None (free public API)
- **Timeout:** 5 seconds with `AbortController`
- **Fallback:** Returns default coordinates (Seoul Forest: `[37.5445, 127.0445]`) on any error

### Daum/Kakao Postcode API

- **Purpose:** Korean address search modal for user registration and location input
- **SDK/Client:** `react-daum-postcode@3.2.0` npm package
- **Auth:** None (embedded widget, no API key required)

### DiceBear Avatars

- **Purpose:** Auto-generated user avatars
- **Endpoint:** `https://api.dicebear.com/7.x/avataaars/svg?seed=...`
- **Used in:** MSW mock handlers (`aini-inu-frontend/src/mocks/handlers.ts`) and seed data
- **Configured in:** `aini-inu-frontend/next.config.ts` (remote image pattern)

## Internal Service Communication

### Chat Direct Connect (LostPet -> Chat)

- **Purpose:** Automatically create a chat room when a lost-pet match is found, connecting the pet owner with the sighting finder
- **Protocol:** HTTP POST via `RestTemplate`
- **Target:** Configurable chat service (default: `http://localhost:18081`)
- **Path:** `/api/v1/chat-rooms/direct` (configurable via `LOSTPET_CHAT_DIRECT_CREATE_PATH`)
- **Auth:** Forwards the caller's `Authorization` header
- **Resilience:** Resilience4j `@Retry` with name `lostpetChatDirect` (max 2 attempts, 250ms wait)
  - Retries on: `ChatDirectConnectException` (server errors, connection failures)
  - Does NOT retry on: `ChatDirectAuthException` (401/403), `ChatDirectResponseSchemaException` (client errors, invalid response)
- **Integration files:**
  - `aini-inu-backend/src/main/java/scit/ainiinu/lostpet/integration/chat/ChatRoomDirectClientImpl.java` - Implementation
  - `aini-inu-backend/src/main/java/scit/ainiinu/lostpet/integration/chat/ChatRoomDirectClient.java` - Interface
  - `aini-inu-backend/src/main/java/scit/ainiinu/lostpet/config/LostPetClientConfig.java` - `chatRestTemplate` bean
  - `aini-inu-backend/src/main/java/scit/ainiinu/lostpet/integration/chat/ChatDirect*Exception.java` - Exception hierarchy

## Data Storage

### PostgreSQL 16 + PgVector

- **Purpose:** Primary relational database and vector similarity search
- **Docker image:** `pgvector/pgvector:pg16` (in `aini-inu-backend/docker-compose.yml`)
- **Connection env vars:** `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- **Default connection:** `jdbc:postgresql://localhost:5432/ainiinu` (user: `ainiinu`)
- **ORM:** Hibernate via Spring Data JPA
  - DDL strategy: `spring.jpa.hibernate.ddl-auto=update` (production)
  - DDL strategy: `create-drop` (test profile)
- **Extensions:** `vector`, `hstore`, `uuid-ossp` (initialized in `aini-inu-backend/docker/postgres/init/01_extensions.sql`)
- **Vector store config:**
  - Table: `lostpet_vector_store` (configurable via `SPRING_AI_PGVECTOR_TABLE_NAME`)
  - Schema: `public`
  - Dimensions: 768
  - Distance type: `COSINE_DISTANCE`
  - Index type: `HNSW`
- **DDL alignment scripts:** `aini-inu-backend/src/main/resources/db/ddl/` (6 migration scripts)
- **Seed data:** `aini-inu-backend/src/main/resources/db/seed/` (4 seed files, loaded on startup via `spring.sql.init`)

### H2 (Test Only)

- **Purpose:** In-memory test database replacing PostgreSQL
- **Connection:** `jdbc:h2:mem:ainidb-test;MODE=PostgreSQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1`
- **Config:** `aini-inu-backend/src/test/resources/application-test.properties`
- **Note:** SQL init mode set to `never` in test profile

### Local Filesystem (Community Uploads)

- **Purpose:** Image upload storage for community posts
- **Base directory:** `../common-docs/storage` (relative to backend root, configurable via `COMMUNITY_STORAGE_LOCAL_BASE_DIR`)
- **Docker volume:** `aini_inu_backend_uploads` mounted at `/app/var/uploads`
- **Service:** `aini-inu-backend/src/main/java/scit/ainiinu/community/service/ImageUploadService.java`
- **Config:** `aini-inu-backend/src/main/java/scit/ainiinu/community/config/CommunityStorageProperties.java`
- **Upload flow:** Presigned-URL pattern (token-based, expires in 300s by default)
  - Request presigned URL -> Receive upload URL + token -> PUT bytes to upload URL
  - Max file size: 10 MB
  - Allowed MIME: `image/jpeg`, `image/png`, `image/webp`
- **Serving:** Local files served via `GET /api/v1/images/local?key=...`

### Browser localStorage (Frontend)

- **Purpose:** Client-side state persistence
- **Zustand persist store:** `aini-inu-frontend/src/store/useConfigStore.ts` (key: `aini-inu-config`)
- **MSW mock database:** `aini-inu-frontend/src/mocks/handlers.ts` (key: `aini_inu_v6_db`) - localStorage-backed mock DB for frontend-only development

**Caching:** None detected (no Redis, Caffeine, or Spring Cache configured)

## Authentication & Identity

### Custom JWT Authentication

- **Provider:** Custom implementation (no OAuth2/OIDC provider)
- **Algorithm:** HMAC-SHA256 via jjwt library
- **Token types:**
  - Access Token: 1 hour TTL
  - Refresh Token: 14 days TTL
- **Implementation files:**
  - `aini-inu-backend/src/main/java/scit/ainiinu/common/security/jwt/JwtTokenProvider.java` - Token generation and validation
  - `aini-inu-backend/src/main/java/scit/ainiinu/common/security/interceptor/JwtAuthInterceptor.java` - HTTP interceptor for `/api/**`
  - `aini-inu-backend/src/main/java/scit/ainiinu/common/security/resolver/CurrentMemberArgumentResolver.java` - `@CurrentMember` parameter injection
  - `aini-inu-backend/src/main/java/scit/ainiinu/common/security/annotation/CurrentMember.java` - Annotation
  - `aini-inu-backend/src/main/java/scit/ainiinu/common/security/annotation/Public.java` - Mark endpoints as public (skip auth)
  - `aini-inu-backend/src/main/java/scit/ainiinu/common/security/dto/LoginMember.java` - Auth context DTO
- **Spring Security status:** Dependency included but ALL default behaviors disabled in `aini-inu-backend/src/main/java/scit/ainiinu/common/config/SecurityConfig.java` (CSRF, form login, HTTP basic, logout all disabled; all requests permitted). Auth enforcement is done entirely by the custom `JwtAuthInterceptor`.
- **CORS:** Configured in `aini-inu-backend/src/main/java/scit/ainiinu/common/config/WebConfig.java` for `localhost:3000` and `localhost:5173`

### WebSocket Authentication

- **Protocol:** STOMP over WebSocket
- **Endpoint:** `/ws/chat-rooms/{roomId}`
- **Auth:** JWT validated via `ChatStompAuthChannelInterceptor` on STOMP CONNECT frames
- **Config:** `aini-inu-backend/src/main/java/scit/ainiinu/chat/config/WebSocketConfig.java`
- **Broker:** Simple in-memory broker (topics: `/topic`, `/queue`; app prefix: `/app`; user prefix: `/user`)

## API Documentation

### SpringDoc OpenAPI

- **Purpose:** Auto-generated API documentation and Swagger UI
- **Library:** `springdoc-openapi-starter-webmvc-ui:2.8.6`
- **Swagger UI:** Available at `/swagger-ui.html` (excluded from JWT interceptor)
- **OpenAPI JSON:** Available at `/v3/api-docs`
- **Config:** `aini-inu-backend/src/main/java/scit/ainiinu/common/config/OpenApiConfig.java`
- **Snapshot export:** `aini-inu-backend/scripts/export-openapi.sh` exports to `common-docs/openapi/openapi.v1.json`
- **API group:** `v1` matching paths `/api/v1/**`

## Monitoring & Observability

**Error Tracking:** None detected (no Sentry, Datadog, or similar integration)

**Logs:**
- Backend: SLF4J + Logback (Spring Boot default), debug level for `scit.ainiinu` package
- Frontend: `console.log` / `console.error` / `console.warn` throughout service files

**Health Check:** `/actuator/health` endpoint excluded from JWT auth interceptor (available but no explicit Actuator starter dependency)

## CI/CD & Deployment

**Hosting:** Not determined (no deployment configuration detected)

**CI Pipeline:** None configured (no `.github/workflows/`, no Jenkinsfile, no CircleCI config)

**Containerization:**
- `aini-inu-backend/Dockerfile` - Multi-stage build (Gradle 8.14.3 + JDK 21 builder -> Temurin 21 JRE runtime)
- `aini-inu-backend/docker-compose.yml` - PostgreSQL (pgvector:pg16) + backend service
- Scripts: `aini-inu-backend/scripts/docker-up.sh`, `docker-down.sh`, `docker-logs.sh`

## Environment Configuration

**Required env vars (must be set for production):**
- `JWT_SECRET` - JWT signing key (32+ chars for HS256)
- `GEMINI_API_KEY` - Google Gemini API key for embeddings
- `SPRING_DATASOURCE_URL` - PostgreSQL JDBC URL
- `SPRING_DATASOURCE_USERNAME` - PostgreSQL username
- `SPRING_DATASOURCE_PASSWORD` - PostgreSQL password
- `ANIMAL_REGISTRY_API_KEY` - Korean animal registry API key

**Secrets location:**
- Backend: `.env` (local) or `.env.docker` (Docker) in `aini-inu-backend/` (both gitignored)
- Templates: `.env.example` and `.env.docker.example` committed as reference

## Webhooks & Callbacks

**Incoming:**
- `PUT /api/v1/images/presigned-upload/{token}` - Presigned upload callback endpoint for community image uploads

**Outgoing:**
- None detected

## Frontend API Communication

**API Client:** `aini-inu-frontend/src/services/api/apiClient.ts`
- Base URL: `/api/v1` (relative, expects Next.js proxy or same-origin)
- Timeout: 8 seconds per request
- Expects backend `ApiResponse<T>` envelope: `{ success, status, data, errorCode, message }`
- Automatically unwraps `.data` field on success

**Service modules:**
- `aini-inu-frontend/src/services/authService.ts` - Login, signup, email verification
- `aini-inu-frontend/src/services/api/memberService.ts` - Profile, dogs, followers, stats
- `aini-inu-frontend/src/services/api/chatService.ts` - Chat rooms and messages
- `aini-inu-frontend/src/services/api/postService.ts` - Community posts, likes, comments, stories
- `aini-inu-frontend/src/services/api/threadService.ts` - Walk threads, diaries, hotspots
- `aini-inu-frontend/src/services/api/locationService.ts` - Geocoding (Nominatim)
- `aini-inu-frontend/src/services/geminiService.ts` - Dog image AI analysis

**MSW (Mock Service Worker):**
- Config: `aini-inu-frontend/src/mocks/browser.ts`, `aini-inu-frontend/src/mocks/handlers.ts`
- Provider: `aini-inu-frontend/src/mocks/MSWProvider.tsx` - Wraps app in dev mode, waits for SW controller
- Activates in development mode (`process.env.NODE_ENV === 'development'`)
- localStorage-backed mock database (`aini_inu_v6_db`) with seed data for all mock endpoints

## Remote Image Sources

Configured in `aini-inu-frontend/next.config.ts`:
- `picsum.photos` - Placeholder images
- `images.unsplash.com` - Stock photos
- `api.dicebear.com` - Generated avatars
- `*.gstatic.com` - Google static assets
- `clova-phinf.pstatic.net` - Naver CLOVA assets
- `developers.kakao.com` - Kakao developer assets

---

*Integration audit: 2026-03-06*
