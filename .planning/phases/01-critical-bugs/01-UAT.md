---
status: testing
phase: 01-critical-bugs
source: 01-01-SUMMARY.md, 01-02-SUMMARY.md
started: 2026-03-06T00:00:00Z
updated: 2026-03-06T00:10:00Z
---

## Current Test

number: 2
name: MSW can be disabled for backend testing
expected: |
  Set `NEXT_PUBLIC_ENABLE_MSW=false` in .env.local, restart dev server. Navigate to a page. DevTools Network shows real API calls going to the backend, not intercepted by MSW.
awaiting: user response

## Tests

### 1. API proxy routing to backend
expected: DevTools Network tab shows `/api/v1/*` requests routed to `localhost:8080` (configurable via NEXT_PUBLIC_API_PROXY_TARGET env var)
result: issue
reported: "API requests from apiClient.ts are being intercepted by MSW (mockServiceWorker.js). The Size column shows \"(ServiceWorker)\" for all /api/v1/* requests (threads, walk-diaries, hotspot, rooms, walk). Requests are not reaching the live backend at localhost:8080. MSW needs to be disabled or removed so requests go to the actual backend."
severity: blocker

### 2. MSW can be disabled for backend testing
expected: Set `NEXT_PUBLIC_ENABLE_MSW=false` in .env.local, restart dev server. Navigate to a page. DevTools Network shows real API calls going to the backend, not intercepted by MSW.
result: [pending]

### 3. Global error boundary catches root layout crashes
expected: Inject a throw error into a root layout provider (e.g., MSWProvider), reload page. Instead of white screen, `global-error.tsx` recovery UI appears: amber background, "Something went wrong" heading, "Try again" button.
result: [pending]

### 4. Per-route error boundaries catch route-level crashes
expected: Trigger an error on any route (e.g., dashboard) by injecting a throw in a component. Instead of full-page crash, `error.tsx` for that route shows error message and "Try again" button. Other routes remain unaffected.
result: [pending]

### 5. Radar (around-me) polling stops after 3 failures
expected: Open `/around-me` page, stop the backend or disable network. Wait ~30 seconds for 3 polling cycles. Browser console shows "[useRadarLogic] Polling stopped after 3 consecutive failures". DevTools Network tab shows no more `/api/v1/pets/radar` requests after that message.
result: [pending]

### 6. Chat room message polling stops after 3 failures
expected: Open `/chat/{roomId}`, stop backend or disconnect network. Wait for 3 polling attempts. Browser console shows "[ChatRoom] Message polling stopped after 3 consecutive failures". No further polling requests in Network tab.
result: [pending]

### 7. Dashboard shows loading fallback instead of blank screen
expected: Open `/dashboard`. While user profile is loading, page shows "Loading..." text centered on page, not a blank white screen. After profile loads, dashboard content appears.
result: [pending]

### 8. Around-me shows loading fallback during load
expected: Open `/around-me`. During data load, page shows "Loading..." text, not blank. After data loads, map and radar appear.
result: [pending]

### 9. Corrupted localStorage doesn't crash root layout
expected: Open DevTools console, run `localStorage.setItem('auth_state', 'invalid-json-like-string')`. Reload page. Page loads normally (or auth redirects gracefully) instead of crashing. Root layout's localStorage guard rejects the malformed data.
result: [pending]

### 10. Feed and profile pages handle null API data gracefully
expected: Open `/feed` and `/profile/[memberId]`. If API returns null or undefined for diary lists, page doesn't crash. Instead shows empty state or loading fallback. Type guards and optional chaining prevent undefined access errors.
result: [pending]

### 11. Reusable ErrorBoundary component exists and is importable
expected: Component file `src/components/common/ErrorBoundary.tsx` exists. Import it: `import { ErrorBoundary } from '@/components/common/ErrorBoundary'`. It accepts `children` and `fallback` props and catches errors in wrapped components.
result: [pending]

### 12. API mismatch catalog documents all mismatches
expected: File `.planning/phases/01-critical-bugs/01-API-MISMATCH-CATALOG.md` exists with 150+ lines documenting 29 API calls and 14 identified mismatches (URL, method, payload issues) ready for Phase 2 fixes.
result: [pending]

## Summary

total: 12
passed: 0
issues: 1
pending: 11
skipped: 0

## Gaps

- truth: "API requests to /api/v1/* are routed to the Spring Boot backend at localhost:8080 via Next.js rewrites"
  status: failed
  reason: "User reported: API requests from apiClient.ts are being intercepted by MSW (mockServiceWorker.js). The Size column shows \"(ServiceWorker)\" for all /api/v1/* requests (threads, walk-diaries, hotspot, rooms, walk). Requests are not reaching the live backend at localhost:8080. MSW needs to be disabled or removed so requests go to the actual backend."
  severity: blocker
  test: 1
  root_cause: ""
  artifacts: []
  missing: []
  debug_session: ""
