# Frontend Contract-Alignment Refactor: Feature Research

## Scope and Constraints
- Objective: realign `aini-inu-frontend` behavior to PRD v1.3 + OpenAPI contract without backend or docs edits.
- Allowed changes: frontend code only (`aini-inu-frontend/**`).
- Read-only references: `.planning/PROJECT.md`, `common-docs/PROJECT_PRD.md`, `common-docs/openapi/openapi.v1.json`.
- Contract precedence: `PROJECT_PRD.md` policy lock -> OpenAPI path/schema -> frontend adaptation.

## Table Stakes (Must-Have)
| Feature | Why It Is Table Stakes | Complexity | Frontend Dependencies | Contract Dependencies |
|---|---|---|---|---|
| Contract-safe API gateway normalization | Existing service calls include path/method drift and ad-hoc payload/response assumptions; all domain calls must consistently map to envelope (`ApiResponse<T>`) handling and error-code-aware failures. | High | `src/services/api/apiClient.ts`, `src/services/api/*Service.ts`, calling hooks/pages | All FR API refs in PRD §8, DEC-001, DEC-031 |
| Domain endpoint realignment by module | Current frontend domain services need endpoint remap to canonical resources (Auth/Member/Pet/Thread/Diary/Chat/Lost/Community/Upload) and method semantics (e.g., create vs apply vs patch vs leave). | High | `src/services/api/threadService.ts`, `chatService.ts`, `memberService.ts`, `postService.ts`, `authService.ts` | FR-AUTH/FR-MEMBER/FR-PET/FR-WALK/FR-CHAT/FR-LOST/FR-COMMUNITY |
| Auth and session lifecycle contract alignment | Login/signup/refresh/logout flow must follow locked contract and transition guards (auth failure, token refresh, unauthorized recovery) for all protected routes. | Medium-High | `src/services/authService.ts`, route guards in `src/app/**`, auth state store(s) | FR-AUTH-001~004, UI/UX coverage table (auth/권한 상태) |
| Input constraint and payload canonicalization | Form payloads must enforce PRD numeric/string limits and canonical fields (`birthDate` required canonical, no `age` input source of truth, diary/post/content constraints). | High | `src/hooks/forms/useSignupForm.ts`, `useDogForm.ts`, `useDiaryForm.ts`, `usePostForm.ts`, form components | FR-GLOBAL limits, DEC-003, DEC-004, FR-PET-001, FR-WALK-004 |
| Around-me geolocation policy implementation | `/around-me` behavior is explicitly locked: initial one-time `getCurrentPosition`, failure fallback to Seoul City Hall coordinates, manual refresh only, no periodic auto-refresh. | High | `src/hooks/useRadarLogic.ts`, `src/services/api/locationService.ts`, `src/app/around-me/page.tsx`, around-me components | FR-WALK-007/008, DEC-026~030 |
| Chat contract behavior parity | Room list/detail, cursor message pagination, send failure retry UX, review/write-once policy, leave/confirm flows must align with required chat lifecycle rules. | High | `src/services/api/chatService.ts`, `src/app/chat/**`, `src/components/chat/**`, review modal components | FR-CHAT-001~004, DEC-007, DEC-015~017, DEC-021 |
| Story vs WalkDiary semantic separation | Story must remain derived/readonly exposure over walk diaries (24h expiration/grouped icon policy) and not become independent CRUD in UI state assumptions. | Medium | `src/services/api/postService.ts`, feed/story components, diary hooks/components | PRD §4.2, FR-COMMUNITY-004, DEC-022~025 |
| State machine completeness for key flows | Each core flow must satisfy minimal state set (`loading/empty/error/success` + validation/auth/transition) to meet PRD coverage and prevent silent breakage. | Medium | Route pages (`src/app/*/page.tsx`), domain components, shared modals/forms | PRD §8.3, §10.2 |
| Error-code-to-UX mapping standardization | Domain errors must map to actionable UX (validation vs auth vs permission vs network), including locked lost-analysis failure semantics. | Medium | API layer + domain hooks + toast/modal message utilities | PRD §11.3, DEC-005 |
| Contract-alignment regression safety net | Frontend test/UAT harness must prove major routes and contract branches (happy/failure/empty/auth) with reproducible evidence. | Medium | Existing test setup, MSW handlers, browser UAT scripts/screenshots under planning workflow | PROJECT constraints (verification), PRD DoD §16 |

## Differentiators (High-Leverage, Non-Baseline)
| Feature | Differentiation Value | Complexity | Frontend Dependencies | Contract Dependencies |
|---|---|---|---|---|
| Contract-first typed SDK generation (OpenAPI-driven) | Reduces future drift by generating domain request/response types and service stubs from the contract snapshot, minimizing manual endpoint entropy. | High | Build tooling + `src/services/api/**` migration + type imports across hooks/components | OpenAPI snapshot consistency with PRD §8 |
| Domain composition refactor for pages | Isolates route-level orchestration into domain hooks/services to lower coupling and make post-refactor maintenance safer. | Medium-High | `src/app/**/page.tsx`, `src/hooks/**`, shared components | PROJECT requirement: frontend refactor readiness |
| Shared async-state primitives (query/mutation patterns) | Creates consistent loading/error/retry semantics across domains and prevents duplicated state logic per page. | Medium | Service callers and page-level state containers | PRD §8.3/§10.2 state guarantees |
| Unified optimistic update + rollback policy | Makes like/follow/apply/retry interactions predictable while preserving contract-correct rollback behavior on failure. | Medium | Feed/profile/around-me/chat interactive components + hooks | DEC-018, follow/apply/retry-related FRs |
| Route-level contract coverage matrix artifact | Keeps a living mapping from each route to FR/API refs and implemented states, improving reviewability and handoff quality during incremental cleanup. | Low-Medium | Planning docs + lightweight frontend metadata/tests | PRD §8 matrix + DoD §16 alignment |

## Anti-Features (Explicitly Excluded)
| Anti-Feature | Why Excluded Now | Risk If Included |
|---|---|---|
| Backend endpoint/model/transaction changes | Out of scope by project constraint; backend is contract source for this workstream. | Breaks ownership boundary and invalidates contract-baseline refactor.
| PRD/OpenAPI edits as shortcut for frontend mismatch | Docs are read-only in this task; frontend must adapt to locked policy/contract. | Silent governance drift and unresolved real integration defects.
| Phase2 notifications/push expansion | PRD locks notifications to Phase2; current refactor target is Phase1 contract stabilization. | Scope creep and delayed stabilization.
| New non-PRD product features (UI innovation-first work) | Objective is contract alignment and runtime stability, not feature expansion. | Regressions in core flows and test surface explosion.
| Large visual redesign unrelated to contract gaps | Cosmetic overhaul does not solve API/state mismatch risks. | Consumes time while preserving integration defects.
| Replacing stack primitives (framework/store) during alignment | Infrastructure swaps add migration risk without direct contract payoff. | High regression probability and slower validation loop.

## Dependency Notes for Sequencing
1. API gateway normalization should precede domain endpoint realignment to avoid repeated rewrites.
2. Form/payload canonicalization should run before chat/around-me/feed behavioral fixes where input constraints are preconditions.
3. Around-me geolocation policy and chat lifecycle parity are highest-risk contract surfaces and should receive early verification.
4. Differentiators should start only after table-stakes pass route-level smoke checks.

## Recommended Implementation Order (Frontend-Only)
1. API gateway + endpoint/method realignment.
2. Auth/session + global error mapping.
3. Form constraints and payload canonicalization.
4. Around-me policy lock implementation.
5. Chat lifecycle parity and retry/review rules.
6. Story/WalkDiary semantics and feed/profile convergence.
7. Cross-route state completeness + regression harness.
8. Optional differentiators (typed SDK, composition hardening).
