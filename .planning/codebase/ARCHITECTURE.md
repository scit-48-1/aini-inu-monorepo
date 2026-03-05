# Architecture

**Analysis Date:** 2026-03-06

## Pattern Overview

**Overall:** Monorepo with domain-first layered backend (Spring Boot) and client-side rendered Next.js frontend, connected by a shared contract layer (`common-docs/openapi`).

**Key Characteristics:**
- Domain-first package layout on backend: each domain owns its controller, service, repository, entity, DTO, and exception sub-packages
- Unified API response envelope (`ApiResponse<T>`) returned by every REST endpoint
- Custom interceptor-based JWT authentication (Spring Security is present but fully disabled; auth is handled by `JwtAuthInterceptor`)
- Frontend is entirely `'use client'` (no SSR/RSC) with MSW mock layer for offline development
- OpenAPI contract snapshot in `common-docs/openapi/openapi.v1.json` serves as single source of truth between frontend and backend

## Layers

**Presentation Layer (Backend Controllers):**
- Purpose: Accept HTTP requests, delegate to services, wrap responses in `ApiResponse<T>`
- Location: `aini-inu-backend/src/main/java/scit/ainiinu/{domain}/controller/`
- Contains: `@RestController` classes, OpenAPI `@Operation` annotations, `@Valid` request binding
- Depends on: Service layer, `common/response/ApiResponse`, `common/security/annotation/CurrentMember`
- Used by: Frontend API client, external consumers

**Service Layer (Backend Services):**
- Purpose: Business logic, transaction management, authorization checks
- Location: `aini-inu-backend/src/main/java/scit/ainiinu/{domain}/service/`
- Contains: `@Service` classes with `@Transactional(readOnly = true)` at class level, `@Transactional` on mutating methods
- Depends on: Repository layer, entity layer, domain-specific error codes
- Used by: Controllers

**Repository Layer (Backend Data Access):**
- Purpose: JPA data access with Spring Data repositories
- Location: `aini-inu-backend/src/main/java/scit/ainiinu/{domain}/repository/`
- Contains: `JpaRepository` interfaces with custom query methods
- Depends on: Entity layer
- Used by: Service layer

**Entity Layer (Backend Domain Model):**
- Purpose: JPA entities representing database tables, with domain behavior
- Location: `aini-inu-backend/src/main/java/scit/ainiinu/{domain}/entity/`
- Contains: `@Entity` classes extending `BaseTimeEntity`, enums, value objects
- Depends on: `common/entity/BaseTimeEntity`, `common/entity/vo/Location`
- Used by: Repository and Service layers

**DTO Layer (Backend Data Transfer):**
- Purpose: Request/response objects for API boundary
- Location: `aini-inu-backend/src/main/java/scit/ainiinu/{domain}/dto/` with `request/` and `response/` sub-packages
- Contains: Records or classes with validation annotations (`@Valid`), `@Schema` OpenAPI metadata
- Depends on: Nothing (pure data classes)
- Used by: Controllers (binding) and Services (conversion)

**Cross-Cutting Layer (Backend Common):**
- Purpose: Shared infrastructure: auth, error handling, response wrappers, config
- Location: `aini-inu-backend/src/main/java/scit/ainiinu/common/`
- Contains: Security interceptor/resolver, global exception handler, API response envelope, base entities
- Depends on: Nothing domain-specific
- Used by: All domain layers

**Integration Layer (Backend - LostPet only):**
- Purpose: External service adapters (AI, chat cross-domain calls)
- Location: `aini-inu-backend/src/main/java/scit/ainiinu/lostpet/integration/`
- Contains: Interface-based clients (`LostPetAiClient`, `ChatRoomDirectClient`) with impl classes
- Depends on: Spring AI, Resilience4j, HTTP client
- Used by: LostPet service layer

**Frontend Pages Layer:**
- Purpose: Route definitions and page-level components
- Location: `aini-inu-frontend/src/app/`
- Contains: `page.tsx` and `layout.tsx` files using Next.js App Router
- Depends on: Components, hooks, stores
- Used by: Next.js router

**Frontend Components Layer:**
- Purpose: Reusable UI building blocks organized by feature/domain
- Location: `aini-inu-frontend/src/components/`
- Contains: Feature-grouped components (`chat/`, `profile/`, `feed/`, `around-me/`, `dashboard/`), shared (`common/`, `shared/`, `ui/`), signup wizard steps
- Depends on: Hooks, stores, types, services
- Used by: Pages

**Frontend Services Layer:**
- Purpose: API communication with backend
- Location: `aini-inu-frontend/src/services/`
- Contains: `api/apiClient.ts` (base fetch wrapper), domain-specific service modules (`memberService.ts`, `chatService.ts`, etc.)
- Depends on: `apiClient.ts`, types
- Used by: Hooks, stores, components

**Frontend State Layer:**
- Purpose: Global client-side state management
- Location: `aini-inu-frontend/src/store/`
- Contains: Zustand stores (`useUserStore.ts`, `useConfigStore.ts`)
- Depends on: Services
- Used by: Hooks, components

## Data Flow

**Typical API Request (Authenticated):**

1. Frontend component calls service function (e.g., `memberService.getMe()`)
2. `apiClient.ts` prepends `/api/v1`, sets `Content-Type: application/json`, sends fetch with 8s timeout
3. Next.js dev proxy forwards to backend (configured via `NEXT_PUBLIC_API_PROXY_TARGET`)
4. `JwtAuthInterceptor.preHandle()` extracts Bearer token from `Authorization` header
5. `JwtTokenProvider.validateAndGetMemberId()` validates JWT, returns `memberId`
6. `memberId` stored as request attribute; `CurrentMemberArgumentResolver` injects it into controller method
7. Controller delegates to service, which queries repository
8. Service returns DTO; controller wraps in `ApiResponse.success(data)` -> `ResponseEntity.ok()`
9. `apiClient.ts` checks `result.success`, unwraps `result.data`, returns typed `T` to caller

**Unauthenticated Endpoint Flow:**

1. Controller method or class annotated with `@Public`
2. `JwtAuthInterceptor` detects `@Public` and skips token validation
3. No `memberId` available (controller must not use `@CurrentMember(required = true)`)

**Chat Real-Time Flow (WebSocket/STOMP):**

1. Client connects to `/ws/chat-rooms/{roomId}` via STOMP
2. `ChatStompAuthChannelInterceptor` validates JWT from STOMP headers
3. Messages published via `ChatRealtimePublisher` interface -> `StompChatRealtimePublisher` impl
4. Events broadcast to STOMP topics (`/topic/...`, `/queue/...`)
5. Application destination prefix: `/app`; user destination prefix: `/user`

**LostPet AI Analysis Flow:**

1. `LostPetController.analyze()` receives analysis request
2. `LostPetAnalyzeService` delegates to `LostPetAiClient.analyze()` (interface)
3. `LostPetAiClientImpl` uses Spring AI Google Gemini embeddings + PgVector similarity search
4. Results scored by `LostPetCandidateScoringService`, stored as `LostPetSearchSession` + `LostPetSearchCandidate`
5. On match approval, `LostPetMatchApprovalService` calls `ChatRoomDirectClient.createDirectRoom()` to create a chat room
6. `ChatRoomDirectClientImpl` makes internal HTTP call with Resilience4j retry (2 attempts, 250ms wait)

**State Management (Frontend):**

- `useUserStore` (Zustand): holds authenticated user profile, fetched via `memberService.getMe()`
- `useConfigStore` (Zustand + persist): holds location, push, 2FA preferences; persisted to localStorage
- Custom hooks (`useProfile`, `useMemberProfile`, `useMyDogs`, etc.) wrap store access and API calls
- Auth state tracked via localStorage key `aini_inu_v6_db` with `currentUserId` field (MSW-era pattern)

## Key Abstractions

**ApiResponse<T>:**
- Purpose: Uniform API response envelope across all endpoints
- Location: `aini-inu-backend/src/main/java/scit/ainiinu/common/response/ApiResponse.java`
- Pattern: Factory methods `success(T)` and `error(ErrorCode)`, fields: `{success, status, data, errorCode, message}`

**ErrorCode Interface + Domain Enums:**
- Purpose: Typed, centralized error codes per domain
- Location: `aini-inu-backend/src/main/java/scit/ainiinu/common/exception/ErrorCode.java` (interface)
- Implementations:
  - `common/exception/CommonErrorCode.java` (C001-C999)
  - `member/exception/MemberErrorCode.java`
  - `pet/exception/PetErrorCode.java`
  - `walk/exception/WalkDiaryErrorCode.java`, `walk/exception/ThreadErrorCode.java`
  - `chat/exception/ChatErrorCode.java`
  - `community/exception/CommunityErrorCode.java`
  - `lostpet/error/LostPetErrorCode.java`
- Pattern: Each enum implements `ErrorCode` with `getHttpStatus()`, `getCode()`, `getMessage()`

**BusinessException:**
- Purpose: Domain exception carrying an `ErrorCode`
- Location: `aini-inu-backend/src/main/java/scit/ainiinu/common/exception/BusinessException.java`
- Pattern: Throw `new BusinessException(SomeDomainErrorCode.SPECIFIC_ERROR)`, caught by `GlobalExceptionHandler`

**BaseTimeEntity:**
- Purpose: Automatic `createdAt`/`updatedAt` auditing for all entities
- Location: `aini-inu-backend/src/main/java/scit/ainiinu/common/entity/BaseTimeEntity.java`
- Pattern: `@MappedSuperclass` with `@CreatedDate`/`@LastModifiedDate` via JPA Auditing

**Location Value Object:**
- Purpose: Shared lat/lng/placeName/address VO with validation and Haversine distance calculation
- Location: `aini-inu-backend/src/main/java/scit/ainiinu/common/entity/vo/Location.java`
- Pattern: `@Embeddable`, created via `Location.of(placeName, lat, lng, address)`, used by walk/lostpet domains

**Pagination Wrappers:**
- Purpose: Standardized pagination response shapes
- Location: `aini-inu-backend/src/main/java/scit/ainiinu/common/response/`
- Types:
  - `SliceResponse<T>` - infinite scroll (most endpoints)
  - `CursorResponse<T>` - cursor-based (chat messages)
  - `PageResponse<T>` - traditional page-based

**@CurrentMember Annotation + Resolver:**
- Purpose: Inject authenticated member ID into controller method parameters
- Location: `aini-inu-backend/src/main/java/scit/ainiinu/common/security/annotation/CurrentMember.java`, `common/security/resolver/CurrentMemberArgumentResolver.java`
- Pattern: `@CurrentMember Long memberId` (required=true default), `@CurrentMember(required = false) Long memberId` for optional auth

**@Public Annotation:**
- Purpose: Mark endpoints that bypass JWT authentication
- Location: `aini-inu-backend/src/main/java/scit/ainiinu/common/security/annotation/Public.java`
- Pattern: Applied to method or class; `JwtAuthInterceptor` checks for it before token validation

**apiClient (Frontend):**
- Purpose: Centralized HTTP client that unwraps `ApiResponse.data`
- Location: `aini-inu-frontend/src/services/api/apiClient.ts`
- Pattern: `apiClient.get<T>('/endpoint')` returns `Promise<T>` (unwrapped from `ApiResponse`), 8s timeout, auto JSON headers

## Entry Points

**Backend Application:**
- Location: `aini-inu-backend/src/main/java/scit/ainiinu/AiniInuApplication.java`
- Triggers: `./gradlew bootRun` or Docker
- Responsibilities: Spring Boot bootstrap

**Backend REST API Controllers (8 controllers):**
- `aini-inu-backend/.../member/controller/MemberController.java` - `/api/v1/members/**`
- `aini-inu-backend/.../pet/controller/PetController.java` - `/api/v1/pets/**`, `/api/v1/breeds`, `/api/v1/personalities`, `/api/v1/walking-styles`
- `aini-inu-backend/.../walk/controller/WalkThreadController.java` - `/api/v1/threads/**`
- `aini-inu-backend/.../walk/controller/WalkDiaryController.java` - `/api/v1/walk-diaries/**`
- `aini-inu-backend/.../chat/controller/ChatController.java` - `/api/v1/chat-rooms/**`
- `aini-inu-backend/.../community/controller/PostController.java` - `/api/v1/posts/**`
- `aini-inu-backend/.../community/controller/ImageController.java` - `/api/v1/images/**`
- `aini-inu-backend/.../lostpet/controller/LostPetController.java` - `/api/v1/lost-pets/**`
- `aini-inu-backend/.../common/security/controller/TestAuthController.java` - `/api/v1/test/auth/**` (dev only)

**Backend WebSocket:**
- Location: `aini-inu-backend/src/main/java/scit/ainiinu/chat/config/WebSocketConfig.java`
- Endpoint: `/ws/chat-rooms/{roomId}` (STOMP)
- Triggers: Client STOMP connect

**Frontend Application:**
- Location: `aini-inu-frontend/src/app/layout.tsx`
- Triggers: `npm run dev`
- Responsibilities: Root layout, MSWProvider, ThemeProvider, Sidebar, auth guard

**Frontend Pages:**
- `/` - Landing (`aini-inu-frontend/src/app/page.tsx`)
- `/login` - Login (`aini-inu-frontend/src/app/login/page.tsx`)
- `/signup` - Signup (`aini-inu-frontend/src/app/signup/page.tsx`)
- `/dashboard` - Dashboard (`aini-inu-frontend/src/app/dashboard/page.tsx`)
- `/feed` - Community feed (`aini-inu-frontend/src/app/feed/page.tsx`)
- `/chat` - Chat room list (`aini-inu-frontend/src/app/chat/page.tsx`)
- `/chat/[id]` - Chat room detail (`aini-inu-frontend/src/app/chat/[id]/page.tsx`)
- `/around-me` - Walk radar/map (`aini-inu-frontend/src/app/around-me/page.tsx`)
- `/profile` - Own profile (`aini-inu-frontend/src/app/profile/page.tsx`)
- `/profile/[memberId]` - Other user profile (`aini-inu-frontend/src/app/profile/[memberId]/page.tsx`)
- `/settings` - Settings (`aini-inu-frontend/src/app/settings/page.tsx`)

## Error Handling

**Strategy:** Domain-specific error codes thrown as `BusinessException`, caught centrally by `GlobalExceptionHandler`

**Patterns:**
- Domain services throw `new BusinessException(DomainErrorCode.SPECIFIC_ERROR)` for business rule violations
- `GlobalExceptionHandler` catches `BusinessException`, `MemberException` (legacy), `MethodArgumentNotValidException`, and generic `Exception`
- All error responses wrapped in `ApiResponse.error(errorCode)` with `{success: false, errorCode: "X001", message: "..."}`
- Validation errors (`@Valid` failures) return field-level error map in `data` field with code `C002`
- Frontend `apiClient.ts` checks `response.ok` and `result.success`; throws `Error(result.message)` on failure

**Error Code Namespacing:**
- `C0xx` - Common (bad input, validation)
- `C1xx` - Auth (unauthorized, invalid/expired token)
- `C2xx` - Forbidden
- `C3xx` - Not found
- `C999` - Internal server error
- Domain-specific codes defined in each domain's `ErrorCode` enum

## Cross-Cutting Concerns

**Logging:**
- Backend: SLF4J via Lombok `@Slf4j`, level `debug` for `scit.ainiinu` package
- Frontend: `console.error` / `console.log` (no structured logging)

**Validation:**
- Backend: Jakarta Bean Validation (`@Valid`) on request DTOs, plus manual validation in entity constructors and service methods
- Frontend: Form-level validation in custom hooks (`useSignupForm`, `useDogForm`, etc.)

**Authentication:**
- Custom JWT interceptor (`JwtAuthInterceptor`) on `/api/**` paths
- Spring Security present but fully disabled (all requests permitted; auth delegated to interceptor)
- `@Public` annotation bypasses auth; `@CurrentMember` injects member ID
- WebSocket auth via `ChatStompAuthChannelInterceptor`
- Frontend auth guard in `layout.tsx` checks `localStorage` for `currentUserId`

**CORS:**
- Configured in `WebConfig`: allows `localhost:3000` and `localhost:5173`
- Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
- Credentials: allowed; Authorization header exposed

**API Versioning:**
- All REST endpoints prefixed with `/api/v1/`
- No multi-version support currently

**Soft Delete:**
- Used in walk domain (`WalkDiary.softDelete()` sets `deletedAt`)
- Repository queries filter by `deletedAtIsNull`

**Optimistic Locking:**
- `@Version` field on `WalkDiary` entity

**Auditing:**
- `BaseTimeEntity` provides `createdAt`/`updatedAt` via JPA Auditing (`@EnableJpaAuditing` not explicit -- triggered by `@EntityListeners(AuditingEntityListener.class)`)

---

*Architecture analysis: 2026-03-06*
