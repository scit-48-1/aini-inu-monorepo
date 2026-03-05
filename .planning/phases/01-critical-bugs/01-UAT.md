---
status: complete
phase: 01-critical-bugs
source: 01-01-SUMMARY.md, 01-02-SUMMARY.md
started: 2026-03-06T00:00:00Z
updated: 2026-03-06T00:20:00Z
---

## Current Test

[testing complete]

## Tests

### 1. API proxy routing to backend
expected: DevTools Network tab shows `/api/v1/*` requests routed to `localhost:8080` (configurable via NEXT_PUBLIC_API_PROXY_TARGET env var)
result: pass (after fix: MSW removed, infinite loop fixed)

### 2. MSW can be disabled for backend testing
expected: MSW fully removed. No "(ServiceWorker)" in Network tab. All requests go through Next.js proxy to backend.
result: pass

### 3. Global error boundary catches root layout crashes
expected: Inject a throw error into a root layout provider (e.g., ThemeProvider), reload page. Instead of white screen, `global-error.tsx` recovery UI appears with "Try again" button.
result: pass

### 4. Per-route error boundaries catch route-level crashes
expected: Trigger an error on any route (e.g., dashboard) by injecting a throw in a component. Instead of full-page crash, `error.tsx` for that route shows error message and "Try again" button. Other routes remain unaffected.
result: pass

### 5. Radar (around-me) polling stops after 3 failures
expected: Open `/around-me` page, stop the backend or disable network. Wait ~30 seconds for 3 polling cycles. Browser console shows "[useRadarLogic] Polling stopped after 3 consecutive failures". DevTools Network tab shows no more `/api/v1/pets/radar` requests after that message.
result: pass

### 6. Chat room message polling stops after 3 failures
expected: Open `/chat/{roomId}`, stop backend or disconnect network. Wait for 3 polling attempts. Browser console shows "[ChatRoom] Message polling stopped after 3 consecutive failures". No further polling requests in Network tab.
result: pass

### 7. Dashboard shows loading fallback instead of blank screen
expected: Open `/dashboard`. While user profile is loading, page shows "Loading..." text centered on page, not a blank white screen. After profile loads, dashboard content appears.
result: pass

### 8. Around-me shows loading fallback during load
expected: Open `/around-me`. During data load, page shows "Loading..." text, not blank. After data loads, map and radar appear.
result: pass

### 9. Corrupted localStorage doesn't crash root layout
expected: Set corrupted localStorage data, reload page. Page loads normally or redirects to /login instead of crashing.
result: pass

### 10. Feed and profile pages handle null API data gracefully
expected: Open `/feed` and `/profile/[memberId]`. If API returns null or undefined for diary lists, page doesn't crash. Instead shows empty state or loading fallback. Type guards and optional chaining prevent undefined access errors.
result: pass

### 11. Reusable ErrorBoundary component exists and is importable
expected: Component file `src/components/common/ErrorBoundary.tsx` exists. Import it: `import { ErrorBoundary } from '@/components/common/ErrorBoundary'`. It accepts `children` and `fallback` props and catches errors in wrapped components.
result: pass

### 12. API mismatch catalog documents all mismatches
expected: File `.planning/phases/01-critical-bugs/01-API-MISMATCH-CATALOG.md` exists with 150+ lines documenting 29 API calls and 14 identified mismatches (URL, method, payload issues) ready for Phase 2 fixes.
result: pass

## Summary

total: 12
passed: 12
issues: 1 (diagnosed and fixed)
pending: 0
skipped: 0

## Gaps

- truth: "API requests to /api/v1/* are routed to the Spring Boot backend at localhost:8080 via Next.js rewrites"
  status: fixed
  reason: "User reported: API requests were being intercepted by MSW. Root cause: MSW handlers in src/mocks/handlers.ts were configured to intercept all /api/v1/* requests before they could reach the backend proxy."
  severity: blocker
  test: 1
  root_cause: "MSW request handlers intercepting all /api/v1/* requests in src/mocks/handlers.ts, preventing requests from reaching the Next.js proxy rewrites to the backend"
  artifacts:
    - path: "aini-inu-frontend/src/mocks/MSWProvider.tsx"
      issue: "MSW provider enabled in root layout, starting service worker that intercepts fetch requests"
    - path: "aini-inu-frontend/src/mocks/handlers.ts"
      issue: "Defined request handlers for all /api/v1/* routes, intercepting before proxy rewrite could occur"
    - path: "aini-inu-frontend/public/mockServiceWorker.js"
      issue: "Service worker script registered by MSW, running in browser and intercepting network calls"
  missing:
    - "Remove src/mocks/ directory entirely"
    - "Remove src/lib/mockApi.ts mock data definitions"
    - "Remove public/mockServiceWorker.js service worker file"
    - "Remove MSWProvider import and usage from src/app/layout.tsx"
    - "Remove mock fallbacks from geminiService.ts and useDogForm.ts"
  debug_session: ""
