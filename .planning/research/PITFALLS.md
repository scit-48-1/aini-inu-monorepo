# Frontend Contract-Alignment Refactor Pitfalls

## Phase Map (for recommended timing)
- Phase 0: Contract Baseline and Inventory
- Phase 1: API Layer Consolidation
- Phase 2: Type and Schema Hardening
- Phase 3: Screen Flow Alignment
- Phase 4: Realtime and Edge-Case Hardening
- Phase 5: UAT and Regression Gates

## Pitfalls

### 1) Treating `openapi.v1.json` as always-current without runtime verification
- Warning signs:
- Endpoint exists in Swagger `/v3/api-docs` but missing or different in local generated client.
- Frontend fixes pass against mock data but fail against bootRun backend.
- Prevention strategy:
- At Phase 0 start, diff three sources per domain: PRD, `common-docs/openapi/openapi.v1.json`, live `/v3/api-docs`.
- Maintain a contract matrix (`method + path + request + response + error`) before touching UI code.
- Recommended phase: Phase 0

### 2) Keeping mixed API call styles (central client + ad-hoc direct URLs)
- Warning signs:
- New calls appear outside `src/services/api/*`.
- Hardcoded `http://localhost:*` or duplicated auth header logic in feature files.
- Prevention strategy:
- Block direct network usage from pages/components via lint rule or PR checklist.
- Move each domain to one gateway module and expose typed functions only.
- Remove `geminiService`-style hardcoded backend URLs or wrap behind environment-aware adapter.
- Recommended phase: Phase 1

### 3) Ignoring response envelope and error shape consistency (`ApiResponse<T>`)
- Warning signs:
- Components parse `data` directly from `fetch` result without envelope unwrap.
- Error toasts show generic fallback because code cannot read backend error code/message.
- Prevention strategy:
- Define one shared response/error decoder in API client layer.
- Add contract tests for representative success/error payloads (401, 403, 404, 409, 422, 500).
- Recommended phase: Phase 1

### 4) Migrating UI first before contract-safe API adapters are ready
- Warning signs:
- Same endpoint logic duplicated in old and new screen files with diverging behavior.
- Frequent regressions when toggling between routes after partial refactor.
- Prevention strategy:
- Introduce compatibility adapters at domain boundary first, then migrate screens in slices.
- Use per-route cutover checklist: endpoint parity, status handling parity, fallback state parity.
- Recommended phase: Phase 1

### 5) Allowing `any` types in runtime contract paths
- Warning signs:
- `any` propagates from hooks/services into component props.
- Runtime undefined errors appear after backend field changes.
- Prevention strategy:
- Replace `any` with domain DTOs derived from OpenAPI schemas (or Zod/validator mirrors).
- Enforce `noImplicitAny` for touched files and fail CI on new `any` in API/hook layers.
- Recommended phase: Phase 2

### 6) Trusting MSW mocks that are looser than real backend behavior
- Warning signs:
- Mock handler returns fields/statuses not present in OpenAPI examples.
- Frontend passes tests but fails when run against bootRun backend.
- Prevention strategy:
- Rebuild mock handlers from contract fixtures, not hand-written guess payloads.
- Add paired tests: one against MSW, one smoke integration against real backend for same route.
- Recommended phase: Phase 2

### 7) Missing state-transition parity (loading/empty/error/success) per PRD
- Warning signs:
- Screens only handle success path; loading and empty states share same blank UI.
- Retry behavior differs between similar pages (feed vs around-me vs chat).
- Prevention strategy:
- Define page-level state machine spec before UI edits.
- Add story/test cases for each state transition and API error class.
- Recommended phase: Phase 3

### 8) Shipping location-default mismatch with PRD policy
- Warning signs:
- Fallback map coordinates differ from PRD lock (Seoul City Hall).
- Nearby/radar features show inconsistent initial area across pages.
- Prevention strategy:
- Centralize default coordinate constant in one config source and import everywhere.
- Add regression test asserting fallback location equals PRD baseline.
- Recommended phase: Phase 3

### 9) Assuming auth semantics that backend does not strictly enforce
- Warning signs:
- Frontend UX claims password validation guarantees while backend currently issues tokens by email lookup path.
- Confusing login failures/successes when test-token endpoint is used in non-test flow.
- Prevention strategy:
- Frontend must rely on documented contract outcomes only, not inferred security behavior.
- Separate test-token tooling from user-facing auth routes; hide behind explicit dev flag.
- Recommended phase: Phase 3

### 10) Realtime chat integration without strict room/event filtering
- Warning signs:
- Message list updates from unrelated rooms during long sessions.
- STOMP subscriptions are not cleaned up on route change/unmount.
- Prevention strategy:
- Scope subscriptions by active room id and enforce unsubscribe lifecycle in hook cleanup.
- Add E2E test for room-switch isolation and duplicate-subscription prevention.
- Recommended phase: Phase 4

### 11) Overlooking backend edge constraints in UX (capacity, race, eventual consistency)
- Warning signs:
- Double-submit on join/apply causes inconsistent participant count in UI.
- Direct chat create action intermittently creates duplicate room entries in list refresh.
- Prevention strategy:
- Add idempotency guards in UI actions (disable button + in-flight key + optimistic rollback).
- Normalize server conflict responses into deterministic user feedback and refresh strategy.
- Recommended phase: Phase 4

### 12) Running UAT only manually without evidence-driven regression gate
- Warning signs:
- “Works on my machine” claims with no screenshot or script evidence.
- Previously fixed contract mismatch reappears after unrelated refactor.
- Prevention strategy:
- Build agent-browser UAT suite for critical flows: auth, around-me, walk matching, chat, feed, profile/settings.
- Store screenshots and pass/fail logs under `.planning/uat/` as release gate artifacts.
- Recommended phase: Phase 5

## Minimum Exit Criteria for Contract-Alignment Refactor
- No direct API calls from pages/components; all network calls routed through domain API modules.
- No new `any` in contract paths (services/hooks/page data loaders).
- Critical user flows pass against real backend (not only MSW) with evidence.
- PRD-locked defaults and terminology match runtime UI behavior.
