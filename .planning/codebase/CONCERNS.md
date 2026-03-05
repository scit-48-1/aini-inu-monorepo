# Codebase Concerns

**Analysis Date:** 2026-03-06

## Tech Debt

**Plaintext Password Storage (CRITICAL):**
- Issue: Passwords are stored and compared as plaintext strings. The `AuthService.loginWithEmail()` method performs `member.getPassword().equals(request.getPassword())` and `AuthService.signup()` saves `request.getPassword()` directly to the `Member` entity without hashing.
- Files: `aini-inu-backend/src/main/java/scit/ainiinu/member/service/AuthService.java` (lines 76, 100-103), `aini-inu-backend/src/main/java/scit/ainiinu/member/entity/Member.java` (line 37)
- Impact: Any database breach exposes all user credentials in cleartext. This is a P0 security issue.
- Fix approach: Integrate `BCryptPasswordEncoder` (Spring Security is already a dependency, see `SecurityConfig.java` comment about BCrypt). Hash on signup, compare hashes on login. Requires a data migration for existing seed data.

**Refresh Token Stored as Plaintext (Not Actually Hashed):**
- Issue: The `RefreshToken` entity has a column named `token_hash` but stores the raw JWT string, not a hash. `AuthService.saveRefreshToken()` saves `tokenValue` directly, and `refresh()` looks it up by `findByTokenHash(requestToken)` comparing the raw token.
- Files: `aini-inu-backend/src/main/java/scit/ainiinu/member/service/AuthService.java` (lines 46, 137), `aini-inu-backend/src/main/java/scit/ainiinu/member/entity/RefreshToken.java` (line 34-35)
- Impact: A database breach exposes all active refresh tokens. Attackers can impersonate any user for up to 14 days.
- Fix approach: Hash the refresh token with SHA-256 before storing. Store the hash, compare hashes on lookup. The column name is already correct; only the application logic needs to change.

**Frontend Does Not Send JWT Auth Headers:**
- Issue: `apiClient.ts` never attaches an `Authorization: Bearer <token>` header to requests. There is no token storage mechanism (no localStorage/cookie read for tokens) and no interceptor to inject auth headers.
- Files: `aini-inu-frontend/src/services/api/apiClient.ts` (entire file), `aini-inu-frontend/src/services/authService.ts` (login returns token but nothing stores/uses it)
- Impact: All authenticated API calls will fail against the real backend. The frontend currently only works with MSW mocks. This is the primary blocker for backend integration.
- Fix approach: Store token from login response (localStorage or httpOnly cookie). Add an auth header interceptor to `apiClient.ts`. Implement token refresh logic using the refresh token. Add 401 response handling to redirect to login.

**`hibernate.ddl-auto=update` in Production Config:**
- Issue: The main `application.properties` uses `spring.jpa.hibernate.ddl-auto=update`, which auto-applies schema changes on startup. This is dangerous for production databases.
- Files: `aini-inu-backend/src/main/resources/application.properties` (line 10)
- Impact: Hibernate may silently add/alter columns in production, causing data loss or schema corruption. Columns are never dropped automatically, leading to orphaned columns.
- Fix approach: Use `validate` in production. Manage schema changes through explicit DDL scripts in `src/main/resources/db/ddl/` (the pattern already exists). Use environment-specific profiles.

**In-Memory Presigned Upload Token Store:**
- Issue: `ImageUploadService` stores presigned upload contexts in a `ConcurrentHashMap` (in-memory). These tokens are lost on server restart and are not shared across instances.
- Files: `aini-inu-backend/src/main/java/scit/ainiinu/community/service/ImageUploadService.java` (line 43)
- Impact: Upload tokens become invalid after server restart. Horizontal scaling will break presigned uploads (token generated on instance A, upload hits instance B). No cleanup of expired tokens -- they leak memory.
- Fix approach: Move token storage to Redis or a database table with TTL. Add a scheduled cleanup for expired tokens.

**Pervasive `any` Types in Frontend:**
- Issue: The frontend uses `any` type extensively (100+ occurrences across components, services, hooks, and mock handlers). Core component props like `ProfileEditModal.user`, `DogRegisterModal.editingDog`, signup step data, and service functions are typed as `any`.
- Files: `aini-inu-frontend/src/components/profile/ProfileEditModal.tsx` (line 17), `aini-inu-frontend/src/components/signup/AccountStep.tsx` (lines 13-14), `aini-inu-frontend/src/components/signup/ManagerStep.tsx` (lines 14-15), `aini-inu-frontend/src/components/signup/PetStep.tsx` (lines 12-13), `aini-inu-frontend/src/services/authService.ts` (line 28), `aini-inu-frontend/src/mocks/handlers.ts` (throughout)
- Impact: No compile-time type safety. Refactoring is error-prone. Runtime bugs are likely when API response shapes change.
- Fix approach: Define proper TypeScript interfaces for all API request/response shapes in `src/types/`. Type component props explicitly. Use the OpenAPI snapshot (`common-docs/openapi/openapi.v1.json`) as the source of truth for DTO shapes.

**All Frontend Pages Are `'use client'`:**
- Issue: All 74 `.tsx`/`.ts` files containing components are marked `'use client'`. No server components exist despite using Next.js App Router (which defaults to server components).
- Files: Every page and component under `aini-inu-frontend/src/app/` and `aini-inu-frontend/src/components/`
- Impact: Loses all SSR/streaming benefits of Next.js App Router. Larger JavaScript bundles sent to the client. No SEO for any page (everything renders client-side). The landing page (`src/app/page.tsx`, 523 lines) with static marketing content is fully client-rendered.
- Fix approach: Convert static/presentational pages (landing, layout shells) to server components. Keep interactive components as `'use client'` leaf nodes. This is a significant refactor and should be done incrementally.

**Cross-Domain Coupling (Walk -> Chat):**
- Issue: `WalkThreadService` directly imports and uses `ChatRoomRepository` and `ChatParticipantRepository` from the chat domain to create chat rooms when users apply to walk threads.
- Files: `aini-inu-backend/src/main/java/scit/ainiinu/walk/service/WalkThreadService.java` (lines 9-12, 61-62, 406-448)
- Impact: Walk domain is tightly coupled to chat domain internals. Changes to chat entity structure break walk service. Cannot independently test or deploy these domains.
- Fix approach: Extract a `ChatRoomFacade` interface in the chat domain that walk service consumes. Or use domain events (Spring `ApplicationEvent`) so walk publishes "thread joined" and chat listens to create rooms.

## Known Bugs

**Logout Only Clears MSW Mock State:**
- Symptoms: `useUserStore.logout()` manipulates `localStorage` key `aini_inu_v6_db` (MSW mock database) but never calls the backend `/auth/logout` endpoint or clears any actual JWT tokens from storage.
- Files: `aini-inu-frontend/src/store/useUserStore.ts` (lines 48-59)
- Trigger: User clicks logout in the UI.
- Workaround: The frontend only works with MSW mocks currently, so this is not yet user-visible. Will become a bug when backend integration is implemented.

**`MemberException` Handler Is Redundant:**
- Symptoms: `GlobalExceptionHandler` has separate handlers for `BusinessException` and `MemberException`. Since `MemberException` likely extends `BusinessException`, the order of handler resolution depends on Spring internals and may cause inconsistent error responses.
- Files: `aini-inu-backend/src/main/java/scit/ainiinu/common/exception/GlobalExceptionHandler.java` (lines 23-52)
- Trigger: Any `MemberException` thrown -- which handler is invoked depends on Spring's internal ordering.
- Workaround: Both handlers produce the same response shape, so the effect is identical. But this creates maintenance confusion.

## Security Considerations

**Passwords Stored in Plaintext:**
- Risk: Complete credential exposure on database breach.
- Files: `aini-inu-backend/src/main/java/scit/ainiinu/member/service/AuthService.java` (lines 76, 100-103)
- Current mitigation: None.
- Recommendations: Use BCrypt (already available via Spring Security dependency). Hash on signup, verify on login. Rotate all existing passwords.

**CORS Allows Only Localhost Origins:**
- Risk: Production deployment will fail to serve the frontend unless CORS origins are updated. Forgetting to restrict CORS in production could enable cross-site attacks.
- Files: `aini-inu-backend/src/main/java/scit/ainiinu/common/config/WebConfig.java` (lines 64-76)
- Current mitigation: Comment in code says "Production: add real domain". Localhost-only origins in config.
- Recommendations: Externalize CORS allowed origins to environment variables. Add production domain before deployment.

**JWT Secret Has a Weak Default:**
- Risk: If the `JWT_SECRET` environment variable is not set, the fallback is a hardcoded string in `application.properties`. Anyone reading the source code can forge valid JWTs.
- Files: `aini-inu-backend/src/main/resources/application.properties` (line 31)
- Current mitigation: Comment warns against using the default. Environment variable override exists.
- Recommendations: Remove the default fallback entirely. Fail-fast on startup if `JWT_SECRET` is not set. Use `@Value` with no default so the app won't start without it.

**No Rate Limiting on Auth Endpoints:**
- Risk: Brute-force attacks on login, signup, and token refresh endpoints.
- Files: `aini-inu-backend/src/main/java/scit/ainiinu/common/security/controller/TestAuthController.java`, `aini-inu-backend/src/main/java/scit/ainiinu/member/controller/MemberController.java`
- Current mitigation: None.
- Recommendations: Add rate limiting via Spring Boot filter or API gateway. Limit login attempts per IP and per account.

**Test Auth Controller May Be Active in Production:**
- Risk: `TestAuthController` generates arbitrary JWT tokens for any `memberId`. If this controller is not profile-gated, it allows anyone to impersonate any user.
- Files: `aini-inu-backend/src/main/java/scit/ainiinu/common/security/controller/TestAuthController.java`
- Current mitigation: Unknown -- need to verify if this controller has `@Profile("test")` annotation.
- Recommendations: Ensure `@Profile("!prod")` or `@ConditionalOnProperty` gate. Or remove entirely and use integration test helpers instead.

**No Input Sanitization for Chat Messages:**
- Risk: XSS attacks via chat message content rendered in the frontend. Messages are stored and retrieved without sanitization.
- Files: `aini-inu-backend/src/main/java/scit/ainiinu/chat/service/MessageService.java` (line 85), `aini-inu-frontend/src/components/chat/MessageList.tsx` (line 66: `{msg.content || msg.text}`)
- Current mitigation: React auto-escapes JSX content by default, which mitigates basic XSS. But if `dangerouslySetInnerHTML` is ever added, this becomes exploitable.
- Recommendations: Add server-side content validation/sanitization for chat messages. Enforce max length.

## Performance Bottlenecks

**Map Threads Loads All Recruiting Threads Into Memory:**
- Problem: `getMapThreads()` calls `findByStatus(RECRUITING)` which loads ALL recruiting threads, then filters by distance in Java code.
- Files: `aini-inu-backend/src/main/java/scit/ainiinu/walk/service/WalkThreadService.java` (lines 112-143), `aini-inu-backend/src/main/java/scit/ainiinu/walk/repository/WalkThreadRepository.java` (line 23)
- Cause: No spatial query or bounding-box filter at the database level. Distance calculation happens in application code after full table scan.
- Improvement path: Use PostgreSQL `earthdistance` extension or PostGIS for spatial queries. Add a bounding-box WHERE clause: `latitude BETWEEN ? AND ? AND longitude BETWEEN ? AND ?`. This reduces the data transfer from DB to app.

**N+1 Query Pattern in Post Listing:**
- Problem: `PostService.getPosts()` calls `postLikeRepository.existsByPostAndMemberId(post, memberId)` inside `Slice.map()` -- one query per post in the page.
- Files: `aini-inu-backend/src/main/java/scit/ainiinu/community/service/PostService.java` (line 60)
- Cause: Like-check is done per-post instead of batched.
- Improvement path: Batch-load liked post IDs for the current user with a single query: `SELECT post_id FROM post_like WHERE member_id = ? AND post_id IN (...)`. Build a `Set<Long>` and check membership.

**N+1 Query Pattern in Thread Summary:**
- Problem: `toSummaryResponse()` and `toThreadResponse()` each call `walkThreadApplicationRepository.countByThreadIdAndStatus()` per thread. When listing threads, this produces N+1 queries.
- Files: `aini-inu-backend/src/main/java/scit/ainiinu/walk/service/WalkThreadService.java` (lines 283-308, 310-356)
- Cause: Participant count is queried individually for each thread.
- Improvement path: Use a bulk query to fetch participant counts for all thread IDs in one shot, then join in memory.

**Hotspot Calculation Loads All Threads:**
- Problem: `getHotspots()` loads all threads matching status and time filter, then groups by `placeName` in Java.
- Files: `aini-inu-backend/src/main/java/scit/ainiinu/walk/service/WalkThreadService.java` (lines 261-281)
- Cause: Aggregation done in Java instead of SQL.
- Improvement path: Use a `GROUP BY place_name` query with `COUNT(*)` directly in the repository. Return only the aggregated results.

## Fragile Areas

**ImageUploadService Path Traversal Prevention:**
- Files: `aini-inu-backend/src/main/java/scit/ainiinu/community/service/ImageUploadService.java` (lines 135-142)
- Why fragile: The path traversal check (`resolved.startsWith(baseDir)`) depends on correct `normalize()` behavior. The base directory is a relative path (`../common-docs/storage`) resolved at runtime. Changes to the working directory or base-dir config can break the security check.
- Safe modification: Always use absolute paths. Add integration tests for path traversal attempts. Consider using an allow-list for objectKey characters.
- Test coverage: No dedicated test for path traversal protection found in test suite.

**Walk Thread Expiration Logic Scattered Across Service:**
- Files: `aini-inu-backend/src/main/java/scit/ainiinu/walk/service/WalkThreadService.java` (lines 118, 209-211, 269, 358-368)
- Why fragile: Thread expiration is checked in 4+ places with `thread.isExpired(now)`. The `validateActiveThreadLimit()` method side-effects by calling `thread.expire()` during validation. If a new code path forgets the expiration check, expired threads will be treated as active.
- Safe modification: Add a repository-level filter or use a `@PrePersist`/`@PreUpdate` lifecycle hook that auto-expires threads. Or add a scheduled job to expire threads.
- Test coverage: Walk service has tests but the scattered expiration logic is easy to miss in new features.

**Chat Domain Entity Graph Complexity:**
- Files: `aini-inu-backend/src/main/java/scit/ainiinu/chat/service/ChatRoomService.java` (lines 157-207)
- Why fragile: `toDetailResponse()` performs 4 sequential queries (participants, participant pets, pets, last message) with manual Map joins. Adding a new field to the response requires understanding the entire chain.
- Safe modification: Consider using a single JPQL/SQL query with JOIN FETCH to load the full graph. Or create a read-optimized projection/view.
- Test coverage: Chat service has slice and integration tests, but the complex mapping logic is hard to verify without dedicated unit tests for the mapping.

## Scaling Limits

**In-Memory Presigned Upload Token Map:**
- Current capacity: Works for single server instance.
- Limit: Breaks with multiple instances (load balancing). Tokens leak memory (no TTL eviction). Server restart loses all pending uploads.
- Scaling path: Move to Redis with TTL keys or a database table with scheduled cleanup.

**WebSocket Chat Realtime (Single-Instance):**
- Current capacity: `ChatRealtimePublisher` publishes events locally.
- Limit: With multiple backend instances, WebSocket connections are pinned to one server. Messages published on instance A are not seen by clients connected to instance B.
- Scaling path: Use Redis Pub/Sub or a message broker (e.g., RabbitMQ) as the realtime event bus.

**Full Table Scan for Map Threads:**
- Current capacity: Works with small thread counts (< 1000).
- Limit: As threads grow, `findByStatus(RECRUITING)` returns increasingly large result sets. Distance filtering in Java is O(n).
- Scaling path: Add spatial indexing (PostGIS/earthdistance) and bounding-box queries.

## Dependencies at Risk

**Spring AI (Pre-1.0 Rapid Evolution):**
- Risk: Spring AI APIs change frequently between milestones. The `VectorStore`, `SearchRequest`, and `Document` APIs may break on version bumps.
- Impact: LostPet AI matching feature breaks on dependency update.
- Migration plan: Pin exact Spring AI version. Monitor changelogs. Abstract the vector store behind a local interface (`LostPetAiClient` already does this, which is good).

**MSW (Mock Service Worker) as Primary Frontend "Backend":**
- Risk: The entire frontend data layer relies on MSW handlers (`src/mocks/handlers.ts`, 727 lines). These mocks may drift from the actual backend API contract.
- Impact: Frontend develops against mock behavior that differs from real backend. Integration bugs surface late.
- Migration plan: Use OpenAPI contract (`common-docs/openapi/openapi.v1.json`) to auto-generate MSW handlers or validate existing ones. Implement actual API integration in `apiClient.ts` with auth support.

## Missing Critical Features

**No Frontend Authentication Flow:**
- Problem: The frontend has no mechanism to store, retrieve, or refresh JWT tokens. Login returns tokens but they are never persisted or used in subsequent requests.
- Blocks: All authenticated API calls. Backend integration. Multi-user testing.

**No Password Hashing:**
- Problem: BCryptPasswordEncoder is available (Spring Security dependency) but unused.
- Blocks: Any production deployment. Security compliance.

**No Expired Thread Cleanup Job:**
- Problem: Walk threads are only expired when explicitly accessed (lazy expiration). Threads that are never viewed remain in RECRUITING status forever.
- Blocks: Accurate thread counts, hotspot calculations, and map view at scale.

## Test Coverage Gaps

**No Frontend Tests:**
- What's not tested: All React components, hooks, stores, and service modules have zero test coverage.
- Files: Entire `aini-inu-frontend/src/` directory (74+ component files, 10+ hooks, 2 stores)
- Risk: UI regressions go undetected. Refactoring is high-risk.
- Priority: High -- especially for shared hooks (`useRadarLogic.ts`, `useSignupForm.ts`) and API client (`apiClient.ts`).

**Backend: 242 Source Files, 68 Test Files (28% File Coverage):**
- What's not tested: Several service classes lack corresponding test classes. `ImageUploadService` has no tests. `StoryService` has no tests. Auth flow integration is not tested end-to-end.
- Files: `aini-inu-backend/src/main/java/scit/ainiinu/community/service/ImageUploadService.java`, `aini-inu-backend/src/main/java/scit/ainiinu/community/service/StoryService.java`
- Risk: Upload logic (path traversal, token expiry) and story aggregation logic could break silently.
- Priority: High for `ImageUploadService` (security-sensitive), Medium for `StoryService`.

**No Security Tests:**
- What's not tested: JWT interceptor behavior with invalid/expired/missing tokens. Authorization boundary tests (accessing resources that belong to other users). Path traversal in image uploads.
- Files: `aini-inu-backend/src/main/java/scit/ainiinu/common/security/interceptor/JwtAuthInterceptor.java`, `aini-inu-backend/src/main/java/scit/ainiinu/community/service/ImageUploadService.java`
- Risk: Auth bypasses and unauthorized access may go undetected.
- Priority: High.

---

*Concerns audit: 2026-03-06*
