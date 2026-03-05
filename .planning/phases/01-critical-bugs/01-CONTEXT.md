# Phase 1: Critical Bugs - Context

**Gathered:** 2026-03-06
**Status:** Ready for planning

<domain>
## Phase Boundary

Stabilize the frontend so every page loads without JavaScript runtime crashes, infinite re-render loops, or unhandled exceptions. This phase does NOT fix API contract mismatches — it wraps them defensively. API alignment is Phase 2 scope.

</domain>

<decisions>
## Implementation Decisions

### Testing environment
- Test against the live backend (Docker at localhost:8080), not MSW mocks
- MSW disabled (`NEXT_PUBLIC_ENABLE_MSW=false`) during all bug hunting
- Use TestAuthController to generate JWT tokens for testing authenticated pages (manual token retrieval, paste into localStorage or similar)
- Docker backend assumed always running — no health check scripts needed

### Fix scope boundary
- Minimal defensive patches only: try-catch, error boundaries, null guards, infinite loop breaks
- Do NOT fix API URL/method/payload mismatches — those are Phase 2 (INFRA-01/02)
- Goal = pages don't crash, not that API calls succeed
- Catalog all API mismatches found during audit (wrong URL, method, payload shapes) as a deliverable for Phase 2

### Page audit order
- Priority by user flow importance, auth-first:
  1. Landing → Login → Signup
  2. Dashboard
  3. Around-me
  4. Feed
  5. Chat
  6. Profile
  7. Settings
- Only actual JavaScript runtime crashes are in scope — blank sections from missing API data are NOT Phase 1
- Add React error boundaries proactively around major page sections as a safety net (not just where crashes are found)

### Claude's Discretion
- Error boundary component design and granularity
- Specific try-catch placement strategy
- Bug catalog format and structure
- How to organize the audit pass (static analysis vs runtime testing vs both)

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets
- `apiClient.ts` (`src/services/api/apiClient.ts`): Central HTTP client with 8-second timeout and ApiResponse unwrapping — all API calls go through this
- `useUserStore.ts` (`src/store/useUserStore.ts`): Zustand store with auth state — currently only manages MSW mock state
- `useConfigStore.ts` (`src/store/useConfigStore.ts`): Zustand config store with persist middleware

### Established Patterns
- All pages use `'use client'` directive — no server components
- Error handling: try-catch with `console.error` + `toast.error` in hooks/services
- No React error boundaries exist anywhere in the codebase currently
- `cn()` utility (`src/lib/utils.ts`) for conditional classnames via clsx + tailwind-merge

### Integration Points
- `layout.tsx`: Root layout with auth guard, sidebar, providers — error boundaries should wrap at this level or per-page
- `src/mocks/MSWProvider.tsx`: MSW toggle point — controlled by `NEXT_PUBLIC_ENABLE_MSW` env var
- `TestAuthController` on backend: Dev-only endpoint to generate JWTs for any memberId

</code_context>

<specifics>
## Specific Ideas

- User explicitly wants Phase 1 to be "stop the bleeding" — crash prevention, not API correctness
- The API mismatch catalog produced in Phase 1 should feed directly into Phase 2 planning as a ready-made fix list
- Error boundaries should be proactive (added to all major sections), not reactive (only where crashes are found)

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 01-critical-bugs*
*Context gathered: 2026-03-06*
