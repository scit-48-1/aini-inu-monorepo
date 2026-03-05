# Codebase Concerns

**Analysis Date:** 2026-03-05

## Tech Debt

**Authentication model is incomplete (email-only login despite password inputs):**
- Issue: Signup/login request DTOs require passwords, but domain/service logic does not store or verify password hashes.
- Files: `aini-inu-backend/src/main/java/scit/ainiinu/member/dto/request/MemberSignupRequest.java`, `aini-inu-backend/src/main/java/scit/ainiinu/member/entity/Member.java`, `aini-inu-backend/src/main/java/scit/ainiinu/member/service/AuthService.java`, `aini-inu-backend/src/test/java/scit/ainiinu/member/service/AuthServiceTest.java`
- Impact: Account security is effectively email-lookup based; credential-based authentication guarantees are absent.
- Fix approach: Add password hash column + migration, use BCrypt/Argon2 verification in login flow, and add regression/security tests.

**Security-bypass test endpoint is embedded as a supported contract:**
- Issue: Test token issuance endpoint is public and explicitly asserted in OpenAPI contract tests.
- Files: `aini-inu-backend/src/main/java/scit/ainiinu/common/security/controller/TestAuthController.java`, `aini-inu-backend/src/test/java/scit/ainiinu/common/contract/OpenApiAuthContractTest.java`, `aini-inu-backend/src/main/java/scit/ainiinu/common/config/OpenApiConfig.java`
- Impact: High-risk path can survive into non-dev deployments unless operationally gated.
- Fix approach: Gate by profile/feature flag, exclude from non-local OpenAPI groups, remove production exposure requirement from contract tests.

**Frontend/backend API contracts are diverged and partially mock-coupled:**
- Issue: Frontend service endpoints and response assumptions do not align with backend controller contracts.
- Files: `aini-inu-frontend/src/services/api/memberService.ts`, `aini-inu-frontend/src/services/api/threadService.ts`, `aini-inu-frontend/src/services/api/chatService.ts`, `aini-inu-frontend/src/app/login/page.tsx`, `aini-inu-backend/src/main/java/scit/ainiinu/pet/controller/PetController.java`, `aini-inu-backend/src/main/java/scit/ainiinu/walk/controller/WalkThreadController.java`, `aini-inu-backend/src/main/java/scit/ainiinu/chat/controller/ChatController.java`
- Impact: Real backend integration can fail with 404/401 or schema mismatch; production behavior depends on mock assumptions.
- Fix approach: Regenerate typed client from OpenAPI and enforce contract tests between frontend calls and backend spec.

**Production-unsafe defaults are in the main properties file:**
- Issue: Schema auto-update, SQL seed init always-on, SQL/debug logging, and weak default fallbacks live in base config.
- Files: `aini-inu-backend/src/main/resources/application.properties`
- Impact: Startup-time data mutation risk, noisy logs under load, and accidental unsafe runtime config in shared environments.
- Fix approach: Move dev-only defaults to profile-specific files and make prod profile fail-fast for missing secrets.

## Known Bugs

**Member search query precedence bug can include current user unexpectedly:**
- Symptoms: Searching by nickname can return the requester despite intended exclusion.
- Files: `aini-inu-backend/src/main/java/scit/ainiinu/member/repository/MemberRepository.java`, `aini-inu-backend/src/main/java/scit/ainiinu/member/service/MemberService.java`
- Trigger: Derived method `findByNicknameContainingIgnoreCaseOrLinkedNicknameContainingIgnoreCaseAndIdNot` applies `idNot` only to one branch.
- Workaround: Use query keywords that do not match own nickname, or patch with explicit JPQL/Querydsl parentheses.

**Login response shape mismatch on real backend path:**
- Symptoms: UI expects `nickname` after login, but backend login response is token-focused.
- Files: `aini-inu-frontend/src/app/login/page.tsx`, `aini-inu-frontend/src/services/authService.ts`, `aini-inu-backend/src/main/java/scit/ainiinu/member/dto/response/LoginResponse.java`
- Trigger: Running frontend against backend instead of MSW mock handlers.
- Workaround: Fetch profile after token setup and avoid relying on login payload nickname.

**Thread/chat/member API path mismatches break direct integration:**
- Symptoms: Calls to `/join`, `/chat/rooms`, `/members/me/dogs`, `/me/follow/...` do not match backend endpoints.
- Files: `aini-inu-frontend/src/services/api/threadService.ts`, `aini-inu-frontend/src/services/api/chatService.ts`, `aini-inu-frontend/src/services/api/memberService.ts`, `aini-inu-backend/src/main/java/scit/ainiinu/walk/controller/WalkThreadController.java`, `aini-inu-backend/src/main/java/scit/ainiinu/chat/controller/ChatController.java`, `aini-inu-backend/src/main/java/scit/ainiinu/member/controller/MemberController.java`, `aini-inu-backend/src/main/java/scit/ainiinu/pet/controller/PetController.java`
- Trigger: Disabling mock API layer or moving to real API gateway.
- Workaround: Temporarily proxy through compatibility adapter; long-term fix is endpoint harmonization.

## Security Considerations

**Spring Security is effectively bypassed at filter level:**
- Risk: `permitAll` with custom interceptor-only auth means non-intercepted routes/channels can be exposed by mistake.
- Files: `aini-inu-backend/src/main/java/scit/ainiinu/common/config/SecurityConfig.java`, `aini-inu-backend/src/main/java/scit/ainiinu/common/config/WebConfig.java`
- Current mitigation: `JwtAuthInterceptor` on `/api/**` plus `@Public` annotations.
- Recommendations: Enforce auth in `SecurityFilterChain`, keep interceptor only for context extraction.

**Public test token minting endpoint is deploy-time hazard:**
- Risk: Anyone can mint JWT for arbitrary member IDs if endpoint is reachable.
- Files: `aini-inu-backend/src/main/java/scit/ainiinu/common/security/controller/TestAuthController.java`
- Current mitigation: Warning comments only.
- Recommendations: Restrict by profile (`local`), IP allowlist, or remove entirely.

**Refresh token material is stored as raw value despite `tokenHash` naming:**
- Risk: DB compromise directly yields reusable refresh tokens.
- Files: `aini-inu-backend/src/main/java/scit/ainiinu/member/service/AuthService.java`, `aini-inu-backend/src/main/java/scit/ainiinu/member/entity/RefreshToken.java`, `aini-inu-backend/src/main/java/scit/ainiinu/member/repository/RefreshTokenRepository.java`
- Current mitigation: Expiration + rotation semantics.
- Recommendations: Hash refresh tokens at rest, compare via secure digest lookup strategy.

**WebSocket origin policy is wide open:**
- Risk: Any origin can initiate websocket handshake; increases abuse and CSWSH-like exposure surface.
- Files: `aini-inu-backend/src/main/java/scit/ainiinu/chat/config/WebSocketConfig.java`, `aini-inu-backend/src/main/java/scit/ainiinu/chat/config/ChatStompAuthChannelInterceptor.java`
- Current mitigation: JWT check at STOMP CONNECT.
- Recommendations: Restrict `setAllowedOriginPatterns`, add handshake-level checks and throttling.

**Frontend auth state is localStorage-driven and client-tamperable:**
- Risk: Route-guard behavior can be bypassed locally; false sense of authenticated state.
- Files: `aini-inu-frontend/src/app/layout.tsx`, `aini-inu-frontend/src/store/useUserStore.ts`, `aini-inu-frontend/src/mocks/handlers.ts`
- Current mitigation: Backend still validates when truly used.
- Recommendations: Store server-issued tokens securely and enforce server-side authorization for all protected data.

**Server route uses `NEXT_PUBLIC` Gemini key semantics:**
- Risk: Key intended for public exposure is used for server-side model calls and diagnostic output.
- Files: `aini-inu-frontend/src/app/api/check-key/route.ts`
- Current mitigation: None visible.
- Recommendations: Use server-only env vars and protect diagnostics behind admin-only controls.

## Performance Bottlenecks

**Post list like-state check is N+1:**
- Problem: For each post in slice, `existsByPostAndMemberId` triggers extra query.
- Files: `aini-inu-backend/src/main/java/scit/ainiinu/community/service/PostService.java`, `aini-inu-backend/src/main/java/scit/ainiinu/community/repository/PostLikeRepository.java`
- Cause: Per-item existence lookups in mapping loop.
- Improvement path: Batch fetch liked post IDs for member and map in memory.

**Thread listing/map endpoints do per-thread counting and in-memory filtering:**
- Problem: `countByThreadIdAndStatus` and membership checks are repeated per thread; map endpoint loads all recruiting threads then computes distances in app layer.
- Files: `aini-inu-backend/src/main/java/scit/ainiinu/walk/service/WalkThreadService.java`, `aini-inu-backend/src/main/java/scit/ainiinu/walk/repository/WalkThreadApplicationRepository.java`, `aini-inu-backend/src/main/java/scit/ainiinu/walk/repository/WalkThreadRepository.java`
- Cause: No aggregated query/geospatial pushdown.
- Improvement path: Use projection queries with joined counts and DB-level geo/radius filtering.

**Chat room summary fetch incurs repeated last-message lookups:**
- Problem: `findTopByChatRoomIdOrderByIdDesc` is called per room in summary mapping.
- Files: `aini-inu-backend/src/main/java/scit/ainiinu/chat/service/ChatRoomService.java`, `aini-inu-backend/src/main/java/scit/ainiinu/chat/repository/MessageRepository.java`
- Cause: Per-room lookup pattern.
- Improvement path: Fetch latest message IDs in one query (subquery/window function).

**Lost-pet analyze path has candidate-level DB round trips:**
- Problem: AI candidate stream calls `sightingRepository.findById` for each candidate.
- Files: `aini-inu-backend/src/main/java/scit/ainiinu/lostpet/service/LostPetAnalyzeService.java`
- Cause: Per-candidate entity fetch.
- Improvement path: Bulk `findAllById` then map scores.

**Verbose SQL/debug logging enabled in base runtime config:**
- Problem: `show-sql=true` and debug logging increase I/O and latency under traffic.
- Files: `aini-inu-backend/src/main/resources/application.properties`
- Cause: Dev defaults in shared config.
- Improvement path: Disable in production profile.

## Fragile Areas

**Custom auth relies on annotation discipline across controllers:**
- Files: `aini-inu-backend/src/main/java/scit/ainiinu/common/security/interceptor/JwtAuthInterceptor.java`, `aini-inu-backend/src/main/java/scit/ainiinu/common/security/annotation/Public.java`, `aini-inu-backend/src/main/java/scit/ainiinu/common/security/resolver/CurrentMemberArgumentResolver.java`
- Why fragile: Missing/incorrect `@Public` or `@CurrentMember` usage can silently alter endpoint security behavior.
- Safe modification: Introduce security-focused integration tests with real filter chain + interceptor.
- Test coverage: Many slice tests stub interceptor behavior instead of validating real auth enforcement.

**Presigned upload context is in-memory and single-instance only:**
- Files: `aini-inu-backend/src/main/java/scit/ainiinu/community/service/ImageUploadService.java`
- Why fragile: Restart or multi-node routing invalidates issued upload tokens.
- Safe modification: Move token state to Redis/DB with TTL.
- Test coverage: Unit/integration tests exist but do not validate multi-instance token continuity.

**Frontend runtime behavior depends on MSW/local mock DB semantics:**
- Files: `aini-inu-frontend/src/mocks/MSWProvider.tsx`, `aini-inu-frontend/src/mocks/handlers.ts`, `aini-inu-frontend/src/services/api/apiClient.ts`
- Why fragile: Small env/config changes (mock disabled, staging backend enabled) can break major flows.
- Safe modification: Add strict env mode + smoke test against real backend contract.
- Test coverage: No first-party frontend test suite detected.

## Scaling Limits

**WebSocket broker is in-process simple broker:**
- Current capacity: Single-node memory-bound pub/sub.
- Files: `aini-inu-backend/src/main/java/scit/ainiinu/chat/config/WebSocketConfig.java`, `aini-inu-backend/src/main/java/scit/ainiinu/chat/realtime/StompChatRealtimePublisher.java`
- Limit: Horizontal scale does not share broker state/events.
- Scaling path: External broker-backed STOMP (Redis/RabbitMQ/Kafka bridge) + sticky/session strategy.

**Upload token context cannot scale across instances:**
- Current capacity: Tokens valid only on issuing process memory.
- Files: `aini-inu-backend/src/main/java/scit/ainiinu/community/service/ImageUploadService.java`
- Limit: Multi-instance deployments create intermittent 4xx upload failures.
- Scaling path: Shared TTL store.

**Thread map/hotspot queries degrade with dataset growth:**
- Current capacity: App-layer loops over large recruiting sets.
- Files: `aini-inu-backend/src/main/java/scit/ainiinu/walk/service/WalkThreadService.java`
- Limit: CPU and DB query amplification as thread volume increases.
- Scaling path: DB aggregation/materialized views/geospatial indexes.

## Dependencies at Risk

**Client-side geocoding depends on public Nominatim API at runtime:**
- Risk: External rate limits/outages directly affect UX.
- Files: `aini-inu-frontend/src/services/api/locationService.ts`
- Impact: Location resolution degrades unpredictably; fallback coordinates may hide errors.
- Migration plan: Backend geocode proxy with caching and provider failover.

**Vector store bootstrap assumes DB extension privileges:**
- Risk: Managed PostgreSQL environments may block extension creation at app startup.
- Files: `aini-inu-backend/src/main/resources/db/ddl/03_lostpet_indexes_constraints.sql`, `aini-inu-backend/src/main/resources/application.properties`
- Impact: Lost-pet AI search path can fail during deployment.
- Migration plan: Provision extensions via infra migrations, not runtime init scripts.

## Missing Critical Features

**Credential-grade authentication is not implemented end-to-end:**
- Problem: Password lifecycle (hashing, verification, reset, lockout) is missing.
- Files: `aini-inu-backend/src/main/java/scit/ainiinu/member/entity/Member.java`, `aini-inu-backend/src/main/java/scit/ainiinu/member/service/AuthService.java`
- Blocks: Production-ready account security posture.

**Frontend token lifecycle and auth header injection are missing:**
- Problem: API client does not attach bearer token, and refresh/revoke flow is not wired.
- Files: `aini-inu-frontend/src/services/api/apiClient.ts`, `aini-inu-frontend/src/services/authService.ts`
- Blocks: Stable real-backend usage for protected endpoints.

**Environment separation for security-sensitive routes/config is incomplete:**
- Problem: Test token API and debug/dev defaults are not strongly isolated by profile.
- Files: `aini-inu-backend/src/main/java/scit/ainiinu/common/security/controller/TestAuthController.java`, `aini-inu-backend/src/main/resources/application.properties`
- Blocks: Safe production hardening and compliance checks.

## Test Coverage Gaps

**Auth enforcement is frequently mocked in controller slice tests:**
- What's not tested: Real `JwtAuthInterceptor` + request pipeline behavior.
- Files: `aini-inu-backend/src/test/java/scit/ainiinu/member/controller/MemberControllerTest.java`, `aini-inu-backend/src/test/java/scit/ainiinu/walk/controller/WalkThreadControllerTest.java`, `aini-inu-backend/src/test/java/scit/ainiinu/lostpet/contract/LostPetControllerSliceTest.java`
- Risk: False confidence on security behavior and accidental public exposure.
- Priority: High

**No targeted regression test for member search precedence bug:**
- What's not tested: Self-exclusion correctness for nickname/linked nickname combined predicate.
- Files: `aini-inu-backend/src/main/java/scit/ainiinu/member/repository/MemberRepository.java`
- Risk: Privacy/UX regressions in member discovery.
- Priority: Medium

**No concurrency/race tests for thread apply capacity control:**
- What's not tested: Simultaneous apply requests near `maxParticipants` boundary.
- Files: `aini-inu-backend/src/main/java/scit/ainiinu/walk/service/WalkThreadService.java`
- Risk: Overbooking and inconsistent participant counts under burst traffic.
- Priority: High

**Frontend has no first-party automated test suite:**
- What's not tested: UI/API contract, auth state transitions, mock-vs-real backend behavior.
- Files: `aini-inu-frontend/package.json`, `aini-inu-frontend/src/`
- Risk: Integration drift reaches runtime.
- Priority: High

---

*Concerns audit: 2026-03-05*
*Update as issues are fixed or new ones discovered*
