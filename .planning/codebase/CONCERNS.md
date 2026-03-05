# CONCERNS

## Scope and Priority
- This list is prioritized for current repo focus: backend contract/stability and `common-docs` sync first, frontend refactor-readiness second.
- Evidence paths are included inline for each concern.

## P0-P1 (Backend / Contract / Security)
1. Password-based auth contract is effectively not enforced.
- `AuthService.loginWithEmail(...)` issues tokens by email lookup and does not verify password: `aini-inu-backend/src/main/java/scit/ainiinu/member/service/AuthService.java`.
- Password is still required by request DTOs (`AuthLoginRequest`, `MemberSignupRequest`): `aini-inu-backend/src/main/java/scit/ainiinu/member/dto/request/AuthLoginRequest.java`, `aini-inu-backend/src/main/java/scit/ainiinu/member/dto/request/MemberSignupRequest.java`.
- Member model has no password field: `aini-inu-backend/src/main/java/scit/ainiinu/member/entity/Member.java`.

2. Refresh token is stored as raw token string despite `token_hash` naming.
- Raw token value is persisted directly in `saveRefreshToken(...)`: `aini-inu-backend/src/main/java/scit/ainiinu/member/service/AuthService.java`.
- Column is named `token_hash`, which implies hashing but currently stores plain token: `aini-inu-backend/src/main/java/scit/ainiinu/member/entity/RefreshToken.java`.

3. Spring Security is globally `permitAll`; auth relies on MVC interceptor boundaries.
- Security chain allows every request: `aini-inu-backend/src/main/java/scit/ainiinu/common/config/SecurityConfig.java`.
- JWT check is attached only via interceptor path config: `aini-inu-backend/src/main/java/scit/ainiinu/common/config/WebConfig.java`, `aini-inu-backend/src/main/java/scit/ainiinu/common/security/interceptor/JwtAuthInterceptor.java`.

4. WebSocket access control is weak for production use.
- CORS/origin policy for STOMP endpoint allows `*`: `aini-inu-backend/src/main/java/scit/ainiinu/chat/config/WebSocketConfig.java`.
- Events are published to predictable room topics (`/topic/chat-rooms/{id}/events`) without room-scoped subscription guard in this layer: `aini-inu-backend/src/main/java/scit/ainiinu/chat/realtime/StompChatRealtimePublisher.java`.

5. Public test-token endpoint is live by design; deployment guard is process-dependent.
- Public controller exists in runtime code: `aini-inu-backend/src/main/java/scit/ainiinu/common/security/controller/TestAuthController.java`.
- PRD locks this as a documented exception (DEC-031): `common-docs/PROJECT_PRD.md`.

6. Production hardening defaults are not separated by profile.
- Single `application.properties` includes default DB credentials, dev JWT fallback secret, SQL debug output, and package debug logging: `aini-inu-backend/src/main/resources/application.properties`.
- No `application-prod.properties` is present under `src/main/resources`.

## P1-P2 (Backend Reliability / Performance)
7. Schema lifecycle strategy is fragile (`ddl-auto=update` + always-on SQL init + seed data).
- `spring.jpa.hibernate.ddl-auto=update` with `spring.sql.init.mode=always`: `aini-inu-backend/src/main/resources/application.properties`.
- Large local sample seed executes from startup config: `aini-inu-backend/src/main/resources/db/seed/10_core_sample_seed.sql`, `aini-inu-backend/src/main/resources/db/seed/20_status_edge_seed.sql`.

8. DB bootstrap assumes extension privileges.
- Runtime DDL creates extensions (`vector`, `hstore`, `uuid-ossp`): `aini-inu-backend/src/main/resources/db/ddl/03_lostpet_indexes_constraints.sql`.
- Local docker init also pre-creates these extensions: `aini-inu-backend/docker/postgres/init/01_extensions.sql`.

9. Presigned upload state is in-memory and unbounded.
- Token contexts stored in `ConcurrentHashMap` with no background eviction path: `aini-inu-backend/src/main/java/scit/ainiinu/community/service/ImageUploadService.java`.
- Multi-instance/restart behavior can invalidate active upload flows.

10. Multiple N+1 query hotspots in high-traffic read paths.
- Post like-status lookup per post in feed slice: `aini-inu-backend/src/main/java/scit/ainiinu/community/service/PostService.java`.
- Thread list/map count checks per thread: `aini-inu-backend/src/main/java/scit/ainiinu/walk/service/WalkThreadService.java`.
- Diary list resolves linked thread per diary: `aini-inu-backend/src/main/java/scit/ainiinu/walk/service/WalkDiaryService.java`.
- Member search/follow mapping loads personalities per member response: `aini-inu-backend/src/main/java/scit/ainiinu/member/service/MemberService.java`.
- Lost-pet analyze resolves each candidate by `findById` in loop: `aini-inu-backend/src/main/java/scit/ainiinu/lostpet/service/LostPetAnalyzeService.java`.

11. Concurrency race risk around capacity and direct-room creation.
- Thread apply path does count-then-insert without explicit locking around capacity: `aini-inu-backend/src/main/java/scit/ainiinu/walk/service/WalkThreadService.java`.
- Direct chat room creation uses find-then-create pattern, susceptible to duplicate room race under concurrent requests: `aini-inu-backend/src/main/java/scit/ainiinu/chat/service/ChatRoomService.java`.

12. Member search repository method likely has precedence bug.
- Derived method name mixes `Or` and `And` (`findByNicknameContainingIgnoreCaseOrLinkedNicknameContainingIgnoreCaseAndIdNot`), which can produce unintended filtering: `aini-inu-backend/src/main/java/scit/ainiinu/member/repository/MemberRepository.java`.

13. External API clients lack explicit timeout policies in code path.
- Animal registry client uses `RestClient.create()` directly: `aini-inu-backend/src/main/java/scit/ainiinu/pet/service/AnimalCertificationService.java`.
- LostPet chat client `RestTemplate` bean is created without explicit connect/read timeout tuning in config class: `aini-inu-backend/src/main/java/scit/ainiinu/lostpet/config/LostPetClientConfig.java`.

14. Lost-pet scoring currently uses heuristic string location matching rather than geospatial distance.
- Distance score compares normalized text substrings: `aini-inu-backend/src/main/java/scit/ainiinu/lostpet/service/LostPetCandidateScoringService.java`.

## P2 (Docs / Contract Sync)
15. OpenAPI snapshot sync is process-driven, not CI-enforced.
- Snapshot update is manual script flow: `aini-inu-backend/scripts/export-openapi.sh`.
- Snapshot governance is documented, but test suite does not directly assert `common-docs/openapi/openapi.v1.json` freshness: `common-docs/openapi/README.md`.

## P3 (Frontend Pre-Refactor, Keep Changes Minimal)
16. Frontend API integration is inconsistent (relative API base vs hardcoded localhost call).
- Relative `/api/v1` client: `aini-inu-frontend/src/services/api/apiClient.ts`.
- Hardcoded backend URL path in service: `aini-inu-frontend/src/services/geminiService.ts`.

17. Frontend type safety debt remains high in runtime paths.
- Many `any` usages across hooks/components/services and mocks (examples: `aini-inu-frontend/src/hooks/useRadarLogic.ts`, `aini-inu-frontend/src/components/chat/MessageList.tsx`, `aini-inu-frontend/src/app/feed/page.tsx`, `aini-inu-frontend/src/mocks/handlers.ts`).
- Existing progress report already flags this residual debt: `aini-inu-frontend/PROGRESS_REPORT.md`.

18. PRD default coordinate policy mismatch in frontend fallback state.
- PRD lock: Seoul City Hall (`37.566295, 126.977945`): `common-docs/PROJECT_PRD.md`.
- Frontend defaults still point to Seoul Forest (`37.5445, 127.0445`): `aini-inu-frontend/src/store/useConfigStore.ts`, `aini-inu-frontend/src/services/api/locationService.ts`.

