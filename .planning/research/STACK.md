# Frontend Contract-Alignment Refactor: Stack Research

Date: 2026-03-05
Scope: `aini-inu-frontend` only. Backend and docs are contract source-of-truth (read-only).

## 1. Current State

### 1.1 Confirmed from Context
- Frontend runtime: `next@16.1.6`, `react@19.2.3`, `react-dom@19.2.3`.
- Language/tooling: TypeScript (`typescript@5.9.3`), ESLint 9, Next ESLint config.
- Styling: Tailwind CSS v4 (`tailwindcss@4.1.18`, `@tailwindcss/postcss`).
- State/UI libs in use: Zustand, Sonner, Lucide.
- Mock/testing infra present: `msw@2.12.9`.
- Mapping and special UI libs present: Leaflet/react-leaflet, react-pageflip, daum-postcode.
- Project direction in `.planning/PROJECT.md`: contract mismatch cleanup, API layer centralization, runtime error removal, staged screen-by-screen stabilization.
- Repo policy in `AGENTS.md`: backend contract integrity and docs sync are higher priority than frontend convenience.

### 1.2 Current Risk Profile for Contract Alignment
- Risk A: Manual request/response typing can drift from OpenAPI.
- Risk B: API calls spread across pages/components can create path/method inconsistency.
- Risk C: UI state and server state can mix in Zustand, causing stale or invalid contract assumptions.
- Risk D: Legacy page-coupled logic increases refactor blast radius.

## 2. Recommended Stack and Patterns (Next.js 16 + React 19)

### 2.1 P0 Contract Safety Baseline (Start Here)

| Area | Recommendation | Why | Confidence |
|---|---|---|---|
| Contract typing | Add OpenAPI type generation (`openapi-typescript`) and commit generated types under `src/contracts/` | Removes hand-written DTO drift; keeps frontend types synchronized to contract snapshots | High |
| API boundary | Enforce one typed API client entry (`src/services/api/client.ts`) and domain modules (`src/services/api/{domain}`) | Prevents ad-hoc `fetch` usage and path/method drift | High |
| Envelope handling | Normalize backend envelope (`ApiResponse<T>`) in one mapper/guard layer before UI use | Keeps error/success parsing consistent across all screens | High |
| Domain adapters | Map contract DTO -> UI model in adapter files per domain | Isolates backend naming/shape changes from component tree | High |
| Lint guardrail | Add lint rule/pattern check: no direct HTTP calls outside API layer | Prevents regression during incremental refactor | High |

### 2.2 P1 Data Fetching and State Patterns

| Area | Recommendation | Why | Confidence |
|---|---|---|---|
| Server vs client data | Use Server Components for initial read-heavy page data; use Client Components for interaction-only state | Matches Next.js 16 architecture and reduces client bundle/network waterfalls | High |
| Client fetch cache | Introduce SWR for client-side revalidation/dedup (where user interaction requires client fetching) | Practical, small surface area, good fit for incremental migration | Medium |
| Global state scope | Keep Zustand for UI/session-local state only; do not store canonical server resources there | Separates server truth from UI convenience state | High |
| Mutation flow | Keep mutation functions in API modules; explicitly revalidate/invalidate impacted keys after success | Reduces stale UI and hidden coupling | High |

### 2.3 P1 Component Architecture for Safe Refactor

| Area | Recommendation | Why | Confidence |
|---|---|---|---|
| Composition | Replace boolean-prop-heavy components with explicit variant components or compound components | Lowers accidental behavior combinations during migration | Medium |
| Boundary split | Page = orchestration, feature components = rendering + event wiring, hooks/services = domain logic | Shrinks per-change impact and improves testability | High |
| Effect hygiene | Prefer event-driven updates and derived state over broad `useEffect` syncing | Prevents race conditions and rerender loops | Medium |

### 2.4 P2 Runtime Validation and Reliability Enhancers

| Area | Recommendation | Why | Confidence |
|---|---|---|---|
| Runtime schema checks | Add optional Zod validation at API boundary for high-risk endpoints first | Catches contract divergence early without requiring full rewrite | Medium |
| Contract tests | Keep MSW; add contract-focused tests against generated types and sample payloads | Detects parsing and envelope regressions quickly | High |
| Progressive rollout | Use strangler pattern (route-by-route or feature-by-feature migration) | Maintains delivery pace while containing risk | High |

## 3. Suggested Target Shape (Practical)

```text
src/
  contracts/
    openapi.generated.ts
  services/
    api/
      client.ts
      errors.ts
      auth/
      member/
      pet/
      walk/
      chat/
      community/
      lostpet/
  features/
    <domain>/
      hooks/
      adapters/
      components/
  app/
    (routes...)
```

Key rule: components never call raw backend URLs directly; they only use feature hooks/services.

## 4. What to Avoid

- Big-bang rewrite of all pages at once.
- Direct `fetch/axios` calls from page/component files.
- Manual duplicate DTO definitions across multiple folders.
- Treating Zustand as server-state cache.
- Silent fallback on contract parse errors (must fail visibly with typed error handling).
- Mixing multiple API access paths without ownership (e.g., same endpoint from page, hook, and util separately).
- Boolean-prop proliferation for complex component modes.
- Premature feature expansion before contract alignment and runtime stability.

## 5. Confidence Summary

| Decision Cluster | Confidence | Rationale |
|---|---|---|
| Contract-first typing + centralized API boundary | High | Directly addresses known mismatch root causes and aligns with repo guardrails |
| Server/client split and state-scope rules | High | Matches Next.js 16 + React 19 architecture and reduces regressions |
| SWR introduction for client revalidation | Medium | Good fit for incremental migration, but requires disciplined key ownership |
| Runtime Zod validation on selected endpoints | Medium | High value for risky endpoints, but migration cost should be staged |
| Composition-pattern-driven component refactor | Medium | Improves maintainability, but benefits depend on consistent adoption |

## 6. Recommended Adoption Order

1. Add OpenAPI type generation and central API client boundary.
2. Move one high-traffic domain to adapter-based API modules.
3. Enforce “no direct HTTP outside API layer” guardrail.
4. Split server state vs UI state responsibilities (SWR/Zustand boundary).
5. Refactor component APIs away from boolean proliferation in touched screens.
6. Add selective runtime validation and contract-focused tests.

