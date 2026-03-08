---
status: resolved
trigger: "LocalFeedPreview shows threads not near user; card click navigates to /around-me without selecting thread"
created: 2026-03-08T00:00:00Z
updated: 2026-03-08T00:00:00Z
---

## Current Focus

hypothesis: Two distinct bugs - (1) getThreads called without lat/lng so backend returns all threads unfiltered, (2) Link href is bare "/around-me" with no threadId query param
test: confirmed by reading source
expecting: N/A - root cause confirmed
next_action: return diagnosis

## Symptoms

expected: Dashboard LocalFeedPreview shows only threads near user's location; clicking a card navigates to /around-me with that thread selected
actual: Shows all threads regardless of location; clicking navigates to /around-me with no thread selected
errors: none (functional bug, not crash)
reproduction: Visit /dashboard, observe LocalFeedPreview section
started: Phase 11 dashboard rewire

## Eliminated

(none needed - root cause found on first pass)

## Evidence

- timestamp: 2026-03-08T00:00:00Z
  checked: dashboard/page.tsx line 95
  found: `getThreads({ page: 0, size: 3 })` - no latitude, longitude, or radius params passed
  implication: Backend GET /threads accepts optional latitude/longitude/radius params for geo-filtering, but dashboard never sends them, so backend returns all threads unfiltered

- timestamp: 2026-03-08T00:00:00Z
  checked: api/threads.ts ThreadListParams interface (lines 135-141)
  found: Interface defines optional latitude, longitude, radius fields - they exist but are unused by dashboard caller
  implication: The API layer supports location filtering; the dashboard just doesn't use it

- timestamp: 2026-03-08T00:00:00Z
  checked: WalkThreadController.java lines 86-97
  found: Backend GET /threads accepts @RequestParam(required=false) Double latitude/longitude/radius and passes to service
  implication: Backend geo-filtering works when params are provided; omitting them returns all threads

- timestamp: 2026-03-08T00:00:00Z
  checked: LocalFeedPreview.tsx line 71
  found: `<Link key={thread.id} href="/around-me">` - bare URL, no query params, no threadId
  implication: Navigation loses thread context; /around-me page has no way to know which thread was clicked

- timestamp: 2026-03-08T00:00:00Z
  checked: around-me/page.tsx
  found: Page uses useRadarLogic() hook which provides selectThread function, but no code reads query params (like ?threadId=X) on mount to auto-select a thread
  implication: Even if LocalFeedPreview passed a threadId query param, /around-me wouldn't read it without additional code

## Resolution

root_cause: Two bugs - (1) dashboard fetchThreads() calls getThreads({page:0, size:3}) without passing user coordinates, so backend returns all threads globally instead of nearby ones; (2) LocalFeedPreview card Link uses bare href="/around-me" without passing threadId, so /around-me has no way to select the clicked thread
fix: (not applied - diagnosis only)
verification: (not applied)
files_changed: []
