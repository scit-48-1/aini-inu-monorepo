# Project Research Summary

**Project:** AINI INU Frontend Contract Refactor
**Domain:** Contract-alignment frontend refactor for a social dog-walking platform
**Researched:** 2026-03-05
**Confidence:** MEDIUM-HIGH

## Executive Summary

This project is a frontend stabilization and alignment effort, not a net-new product build. The target is to make `aini-inu-frontend` behave exactly per locked PRD/OpenAPI/backend contract while reducing refactor risk through strict API boundaries and incremental migration. The recommended delivery style is a contract-first strangler approach: centralize transport and endpoint ownership, move domain services behind typed facades, then migrate high-risk screens in vertical slices.

Experts would treat this as a reliability architecture problem first and a UI refactor second. The recommended path is: generate/anchor OpenAPI types, normalize `ApiResponse<T>` decode and typed error taxonomy in one transport layer, keep server-state and UI-state responsibilities separate (SWR or equivalent for server cache; Zustand for UI-local/session state), and isolate external integrations (geocoding/AI) from backend-contract modules. This pattern gives deterministic behavior and makes contract drift visible early.

The key risks are contract illusion (docs snapshot vs runtime drift), mixed API call styles, weak typing (`any` leakage), and high-complexity flows (around-me policy, chat lifecycle/realtime). Mitigation is straightforward but must be enforced early: phase-0 contract matrix verification, lint/ownership guardrails that block ad-hoc calls, compatibility adapters before UI cutover, and evidence-based regression gates (real-backend smoke + artifacted UAT).

## Key Findings

### Recommended Stack

The current stack is already suitable for this refactor goal: Next.js 16 + React 19 + TypeScript + Tailwind v4, with MSW available for controlled testing. The gap is not framework capability; it is contract discipline and boundary enforcement. The strongest stack recommendation is to add OpenAPI-driven type generation and formalize a single transport+facade pipeline for all backend-contract calls.

**Core technologies:**
- `next@16.1.6` + `react@19.2.3`: app/runtime foundation aligned with server-client split patterns.
- `typescript@5.9.3`: enforce contract DTO fidelity and reduce runtime mismatch.
- `openapi-typescript` (recommended addition): generate contract types and remove manual DTO drift.
- Central API transport (`httpClient` + envelope decoder + typed errors): single source of network behavior.
- `msw@2.12.9`: contract-focused mock/testing harness for phased migration.
- Zustand + SWR boundary (recommended): keep UI-local state separate from canonical server data.

### Expected Features

Research converges on “table stakes first, differentiation later.” The non-negotiable baseline is contract correctness across core user journeys, including strict payload constraints, auth/session transitions, around-me geolocation policy, chat lifecycle parity, and Story-vs-WalkDiary semantics.

**Must have (table stakes):**
- Contract-safe API gateway normalization and endpoint/method realignment across all core domains.
- Auth/session lifecycle correctness with consistent unauthorized recovery on protected routes.
- Input constraint and payload canonicalization (including locked PRD field rules).
- Around-me policy lock: one-time geolocation, Seoul City Hall fallback, manual refresh only.
- Chat lifecycle parity: cursor pagination, retry/review/leave behaviors, and error-driven UX mapping.
- Full state-transition completeness (`loading/empty/error/success`) for major screens.

**Should have (competitive):**
- OpenAPI-driven typed SDK workflow to prevent future drift.
- Domain composition refactor (page orchestration -> hooks/services) to lower coupling.
- Shared async-state primitives and standardized optimistic update/rollback strategy.
- Route-level contract coverage matrix artifact for reviewability and handoff quality.

**Defer (v2+):**
- Notification/push expansion (explicitly Phase2 by PRD policy).
- Non-PRD feature expansion or large visual redesign unrelated to contract gaps.
- Stack replacement efforts that do not directly improve contract alignment.

### Architecture Approach

Architecture research recommends a strict layered model with hard boundaries: UI -> feature hooks/actions -> API modules/facades -> transport -> contracts, plus separate integration adapters for non-contract external APIs. Migration should use compatibility seams so existing service exports remain stable while internals are replaced, enabling route-by-route cutover without big-bang risk.

**Major components:**
1. Transport core (`httpClient/envelope/errors/query/auth`) — one protocol/error policy for all backend calls.
2. Contract layer (`endpoints + DTO types`) — canonical method/path/schema ownership.
3. API modules by domain (`auth/member/thread/post/chat`) — typed use-case-facing facades.
4. Feature orchestration hooks/actions — deterministic screen state transitions and retries.
5. Integration adapters (`geocoding/ai`) — isolate non-backend-contract dependencies.

### Critical Pitfalls

1. **Snapshot-only contract trust** — diff PRD, `openapi.v1.json`, and live `/v3/api-docs` in Phase 0 before migration.
2. **Mixed network call styles** — block ad-hoc/direct URL calls via lint/PR rules and route all calls through modules.
3. **Envelope/error inconsistency** — centralize `ApiResponse<T>` decode and typed error mapping with contract tests.
4. **UI-first migration without adapters** — cut over API boundaries first, then migrate screens in vertical slices.
5. **Mock drift from backend reality** — pair MSW tests with real-backend smoke runs for critical flows.

## Implications for Roadmap

Based on research, suggested phase structure:

### Phase 1: Contract Baseline and API Boundary Lock
**Rationale:** All downstream work depends on reliable endpoint ownership and decode/error consistency.
**Delivers:** Contract matrix, endpoint constants, transport core, compatibility wrappers, no-direct-call guardrails.
**Addresses:** API gateway normalization, endpoint realignment, error-code mapping baseline.
**Avoids:** Pitfalls 1, 2, 3, 4.

### Phase 2: Auth, Member, and Input Canonicalization Slice
**Rationale:** Auth/session and payload rules are cross-cutting prerequisites for safe user-flow migration.
**Delivers:** Auth/member module migration, typed error UX, form constraint enforcement, canonical payload mapping.
**Uses:** OpenAPI types, centralized transport, state-scope boundaries.
**Implements:** Layered module/facade pattern with compatibility seams.
**Avoids:** Pitfalls 5, 7, 9.

### Phase 3: Around-Me and Feed/Story Behavioral Alignment
**Rationale:** Around-me policy and Story/WalkDiary semantics are policy-sensitive and regression-prone.
**Delivers:** Around-me fallback/refresh policy lock, feed/story parity, thread/post flow contract correctness.
**Addresses:** Geolocation policy, story-vs-diary separation, state transition parity for high-traffic routes.
**Avoids:** Pitfalls 7, 8, 11.

### Phase 4: Chat Lifecycle and Realtime Hardening
**Rationale:** Chat has the highest interaction complexity and room/event isolation risks.
**Delivers:** Room/message contract parity, pagination/retry/review rules, subscription lifecycle cleanup and isolation tests.
**Addresses:** Full chat contract behavior and edge-case race handling.
**Avoids:** Pitfalls 10, 11.

### Phase 5: Integration Adapter Cleanup and Evidence Gates
**Rationale:** Final hardening should remove remaining ad-hoc integration paths and lock regression safety.
**Delivers:** External adapter normalization (`location`, `ai`), route-level contract coverage matrix, real-backend smoke + agent-browser UAT artifacts.
**Addresses:** Contract-alignment regression safety net and release confidence.
**Avoids:** Pitfalls 6, 12.

### Phase Ordering Rationale

- This order follows strict dependency flow: contract baseline -> cross-cutting auth/input -> high-risk route parity -> realtime hardening -> validation gates.
- Grouping by architecture boundaries (transport/contracts/modules/features/integrations) minimizes churn and keeps migration reversible.
- Early guardrails prevent repeated rewrites and directly suppress the most expensive pitfalls identified in research.

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 3:** Around-me geolocation and Story-vs-WalkDiary behavior need precise PRD/runtime validation in edge conditions.
- **Phase 4:** Chat realtime transport and room isolation require deeper implementation research and E2E strategy.
- **Phase 5:** Real-backend UAT automation scope and evidence standards may need dedicated planning detail.

Phases with standard patterns (skip research-phase):
- **Phase 1:** API centralization, endpoint constants, typed transport/errors are established patterns.
- **Phase 2:** Auth/member slice migration and payload canonicalization follow known frontend contract-hardening practices.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Versions and tooling are directly verifiable; recommendations match current runtime and constraints. |
| Features | HIGH | Table-stakes and exclusions are strongly anchored in PRD/OpenAPI policy mapping. |
| Architecture | MEDIUM-HIGH | Target layering and seams are coherent and practical, but final cutover cost depends on hidden coupling in legacy pages. |
| Pitfalls | MEDIUM-HIGH | Risks are concrete and experience-backed; some depend on runtime behavior that still needs live verification. |

**Overall confidence:** MEDIUM-HIGH

### Gaps to Address

- OpenAPI snapshot vs live backend drift is not yet validated domain-by-domain; Phase 1 must start with a runtime diff matrix.
- Current chat realtime implementation details (subscription lifecycle and filtering) need explicit verification before phase planning.
- Auth edge semantics (test-token/dev-only behavior vs production UX) require firm environment-policy handling during implementation.
- Scope of real-backend automated UAT evidence (required routes/assertions/artifact format) should be finalized before Phase 5.

## Sources

### Primary (HIGH confidence)
- `.planning/research/STACK.md` — runtime stack, contract-alignment technology recommendations, adoption order.
- `.planning/research/FEATURES.md` — table stakes, differentiators, anti-features, sequencing dependencies.
- `.planning/research/ARCHITECTURE.md` — layered architecture, migration seams, build order, boundary rules.
- `.planning/research/PITFALLS.md` — phase-indexed failure modes, warning signs, prevention strategies.
- `.planning/PROJECT.md` — project scope, constraints, and value statement.

### Secondary (MEDIUM confidence)
- `common-docs/PROJECT_PRD.md` (referenced by research files) — product policy and UX/state requirements.
- `common-docs/openapi/openapi.v1.json` (referenced by research files) — API schema and endpoint contract baseline.

### Tertiary (LOW confidence)
- None.

---
*Research completed: 2026-03-05*
*Ready for roadmap: yes*
