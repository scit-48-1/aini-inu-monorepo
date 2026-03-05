---
phase: 01-critical-bugs
verified: 2026-03-06T00:00:00Z
status: gaps_found
score: 7/9 must-haves verified
re_verification: false
gaps:
  - truth: "All existing API calls use correct URL paths, HTTP methods, and payload shapes matching the Swagger spec"
    status: failed
    reason: "14 API mismatches were cataloged but deliberately NOT fixed in Phase 1. The CONTEXT.md locked decision explicitly deferred URL/method/payload corrections to Phase 2. However, this truth is a direct ROADMAP Success Criterion (SC3) and BUG-03 requirement. Plan-02 claimed BUG-03 but only produced a catalog, not fixes."
    artifacts:
      - path: ".planning/phases/01-critical-bugs/01-API-MISMATCH-CATALOG.md"
        issue: "Documents 14 mismatches — none were corrected. This is a planning artifact, not a fix."
    missing:
      - "Fix 4 chat URL mismatches: /chat/rooms -> /chat-rooms (getRooms, getMessages, sendMessage, getOrCreateRoom)"
      - "Fix 5 HTTP method mismatches: PUT->PATCH for updateMe, updateDog, saveWalkDiary, updateThread, updatePost"
      - "Fix 5 pets/dogs URL mismatches: /members/me/dogs -> /pets"
      - "Fix follow/unfollow URL: /members/me/follow/{id} -> /members/me/follows/{id}"
      - "Fix searchMembers URL: /members?q= -> /members/search?q="
      - "Fix joinThread URL: /threads/{id}/join -> /threads/{threadId}/apply"
      - "Add patch() method to apiClient"
      - "Add JWT Authorization headers to apiClient"

  - truth: "Browser network tab shows zero 4xx/5xx errors caused by frontend request malformation"
    status: failed
    reason: "Follows directly from SC3 gap. With 14 API mismatches still in the codebase, real backend calls will produce 4xx errors (wrong URLs, wrong methods). This is a direct ROADMAP Success Criterion (SC4). No code-level fix was applied — only defensive guards prevent crashes, not 4xx responses."
    artifacts:
      - path: "aini-inu-frontend/src/services/api/chatService.ts"
        issue: "getRooms calls /chat/rooms instead of /chat-rooms — produces 404 on real backend"
      - path: "aini-inu-frontend/src/services/api/memberService.ts"
        issue: "getMyDogs calls /members/me/dogs instead of /pets — produces 404 on real backend"
    missing:
      - "Fix service file URL/method mismatches (deferred to Phase 2 per CONTEXT.md)"
      - "Update REQUIREMENTS.md BUG-03 traceability status from Pending to Complete once fixes land"

human_verification:
  - test: "Load dashboard page with NEXT_PUBLIC_ENABLE_MSW=false and real backend running"
    expected: "Page renders with loading fallback while profile loads; no white screen; no unhandled promise rejections in browser console"
    why_human: "Cannot verify runtime behavior or browser console output programmatically"
  - test: "Disable internet and trigger a polled API failure 3+ times on around-me page"
    expected: "Console shows '[useRadarLogic] Polling stopped after 3 consecutive failures'; polling stops; no infinite retry flood"
    why_human: "Polling behavior requires live runtime observation; cannot simulate consecutive failures via grep"
  - test: "Crash the root layout deliberately (e.g., force throw in a provider) and reload"
    expected: "global-error.tsx recovery UI appears with 'Something went wrong' heading and 'Try again' button"
    why_human: "Requires browser to trigger root layout crash and render global-error.tsx"
  - test: "Open /chat/{id} with a real backend room and verify message polling stops after 3 failures"
    expected: "Console shows '[ChatRoom] Message polling stopped after 3 consecutive failures' after 3 failed poll attempts"
    why_human: "Requires real backend or network interruption to trigger 3 consecutive failures"
---

# Phase 1: Critical Bugs Verification Report

**Phase Goal:** Fix the critical frontend crash vectors that cause white-screen failures and unhandled promise rejections, so the app reaches a stable baseline for Phase 2 integration work.
**Verified:** 2026-03-06
**Status:** gaps_found
**Re-verification:** No — initial verification

---

## Goal Achievement

The phase delivered strong crash-prevention infrastructure (error boundaries, polling auto-stop, null guards, loading fallbacks) and a comprehensive API mismatch catalog. However, two of the four ROADMAP Success Criteria are not met: the API mismatches were cataloged but not fixed, which was a deliberate locked decision in CONTEXT.md. This creates a discrepancy: Plan-02 claimed BUG-03 as complete, but BUG-03's requirement text ("Fix network errors from API call mismatches") and the ROADMAP SC3/SC4 require actual fixes, not just documentation.

The REQUIREMENTS.md traceability table correctly reflects this: BUG-03 status remains "Pending".

### Observable Truths

#### From Plan 01 (BUG-01, BUG-02)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Every route has an error.tsx boundary preventing full-page crashes | VERIFIED | All 9 per-route error.tsx files exist and are substantive (25-30 lines each, 'use client', useEffect logging, reset button) |
| 2 | Root layout crash caught by global-error.tsx with recovery UI | VERIFIED | `src/app/global-error.tsx` exists, 36 lines, has own `<html lang="ko"><body>` tags, amber "Try again" button |
| 3 | API calls from frontend reach Spring Boot backend via Next.js rewrites | VERIFIED | `next.config.ts` rewrites `/api/v1/:path*` to `${NEXT_PUBLIC_API_PROXY_TARGET}/api/v1/:path*` |
| 4 | MSW can be disabled via NEXT_PUBLIC_ENABLE_MSW=false | VERIFIED | `MSWProvider.tsx` line 11: `process.env.NEXT_PUBLIC_ENABLE_MSW !== 'false'` condition gates worker.start() |
| 5 | Reusable ErrorBoundary component exists for intra-page sections | VERIFIED | `src/components/common/ErrorBoundary.tsx`, 47 lines, named class export, getDerivedStateFromError + componentDidCatch |

#### From Plan 02 (BUG-02, BUG-03)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 6 | useRadarLogic stops polling after 3 consecutive failures | VERIFIED | `failCountRef` + `pollingIntervalRef` pattern at lines 38-39, 49, 60-67 in useRadarLogic.ts; clears interval + logs warning |
| 7 | Chat room message polling stops after 3 consecutive failures | VERIFIED | `pollFailCountRef` + `pollIntervalRef` at lines 32-33, 87, 91-96 in chat/[id]/page.tsx |
| 8 | All existing API calls use correct URL paths, HTTP methods, and payload shapes | FAILED | 14 mismatches documented in 01-API-MISMATCH-CATALOG.md remain uncorrected by design decision (CONTEXT.md) |
| 9 | Browser shows zero 4xx/5xx errors from frontend request malformation | FAILED | Follows from Truth 8; real backend calls to /chat/rooms, /members/me/dogs, etc. will produce 4xx responses |

**Score:** 7/9 truths verified

---

### Required Artifacts

#### Plan 01 Artifacts

| Artifact | Status | Details |
|----------|--------|---------|
| `aini-inu-frontend/src/app/global-error.tsx` | VERIFIED | 36 lines; own html/body tags; 'use client'; useEffect logging; amber reset button |
| `aini-inu-frontend/src/app/error.tsx` | VERIFIED | 30 lines; 'use client'; [Route Error: root] tag; reset handler |
| `aini-inu-frontend/src/components/common/ErrorBoundary.tsx` | VERIFIED | 47 lines; named class export `ErrorBoundary`; getDerivedStateFromError; componentDidCatch; fallback prop |
| `aini-inu-frontend/next.config.ts` | VERIFIED | Contains `async rewrites()` with `/api/v1/:path*` -> `${target}/api/v1/:path*` |
| `aini-inu-frontend/src/mocks/MSWProvider.tsx` | VERIFIED | Contains `NEXT_PUBLIC_ENABLE_MSW !== 'false'` gate on worker.start() |
| `aini-inu-frontend/src/app/dashboard/error.tsx` | VERIFIED | Exists; 'use client'; [Route Error: dashboard] tag |
| `aini-inu-frontend/src/app/around-me/error.tsx` | VERIFIED | Exists |
| `aini-inu-frontend/src/app/feed/error.tsx` | VERIFIED | Exists |
| `aini-inu-frontend/src/app/chat/error.tsx` | VERIFIED | Exists |
| `aini-inu-frontend/src/app/chat/[id]/error.tsx` | VERIFIED | Exists |
| `aini-inu-frontend/src/app/profile/[memberId]/error.tsx` | VERIFIED | Exists |
| `aini-inu-frontend/src/app/settings/error.tsx` | VERIFIED | Exists |
| `aini-inu-frontend/src/app/login/error.tsx` | VERIFIED | Exists |
| `aini-inu-frontend/src/app/signup/error.tsx` | VERIFIED | Exists |

#### Plan 02 Artifacts

| Artifact | Status | Details |
|----------|--------|---------|
| `aini-inu-frontend/src/hooks/useRadarLogic.ts` | VERIFIED | Contains `failCountRef` (line 38), `pollingIntervalRef` (line 39); failure counting and clearInterval at lines 60-67; reset on success at line 49 |
| `aini-inu-frontend/src/app/chat/[id]/page.tsx` | VERIFIED | Contains `pollFailCountRef` (line 32), `pollIntervalRef` (line 33); polling stops after 3 failures at lines 91-96; `rooms?.find()` optional chaining at line 43; `currentRoom?.partner?.id` at line 56; `room?.partner?.nickname` at line 139; `room?.partner?.id` at line 157. Note: `room.partner` (without chaining) used at lines 133 and 145 as a prop, but is guarded by `if (!room)` exit at line 121. |
| `aini-inu-frontend/src/app/layout.tsx` | VERIFIED | Line 37: `typeof db !== 'object' \|\| db === null \|\| !db.currentUserId` guard added |
| `aini-inu-frontend/src/app/dashboard/page.tsx` | VERIFIED | `!userProfile` returns loading div (line 104); `typeof diaryRes.value === 'object'` guard (line 54) before Object.values |
| `aini-inu-frontend/src/app/around-me/page.tsx` | VERIFIED | Line 124: `if (!mounted \|\| isLoading)` returns loading div; line 125: `if (!userProfile)` returns loading div |
| `aini-inu-frontend/src/app/feed/page.tsx` | VERIFIED | Line 53: `diaryRes && typeof diaryRes === 'object'` guard before setFollowingDiaries |
| `.planning/phases/01-critical-bugs/01-API-MISMATCH-CATALOG.md` | VERIFIED | 150 lines; 29 API calls audited; 14 mismatches documented by type; Phase 2 recommendations section present |

---

### Key Link Verification

#### Plan 01 Key Links

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `next.config.ts` | `http://localhost:8080/api/v1/*` | Next.js rewrites | WIRED | `source: '/api/v1/:path*'`, `destination: \`${target}/api/v1/:path*\`` |
| `MSWProvider.tsx` | `process.env.NEXT_PUBLIC_ENABLE_MSW` | env var check before worker.start | WIRED | Line 11: `process.env.NEXT_PUBLIC_ENABLE_MSW !== 'false'` as second condition in if-guard |

#### Plan 02 Key Links

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `useRadarLogic.ts` | polling interval | failCountRef >= 3 clears interval | WIRED | Lines 60-67: `failCountRef.current >= 3` calls `clearInterval(pollingIntervalRef.current)` |
| `chat/[id]/page.tsx` | polling interval | pollFailCountRef >= 3 stops polling | WIRED | Lines 91-96: `pollFailCountRef.current >= 3` calls `clearInterval(pollIntervalRef.current)` |
| `01-API-MISMATCH-CATALOG.md` | Phase 2 planning | Documents all frontend-to-backend API mismatches | WIRED | 150-line catalog with categorized mismatches and Phase 2 grouping recommendations |

---

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|------------|-------------|--------|----------|
| BUG-01 | 01-01-PLAN.md | 전체 프론트엔드 런타임 에러 전수 조사 및 분류 (Full audit and classification) | SATISFIED | 01-RESEARCH.md (553 lines) contains full static audit; 01-API-MISMATCH-CATALOG.md documents all 29 API calls with 14 mismatches |
| BUG-02 | 01-01-PLAN.md, 01-02-PLAN.md | 크리티컬 런타임 에러 즉시 수정 (페이지 크래시, 무한 루프, 렌더링 실패) | SATISFIED | Error boundaries for all routes; polling auto-stop after 3 failures; null guards; loading fallbacks replacing blank returns |
| BUG-03 | 01-02-PLAN.md | API 호출 불일치로 인한 네트워크 에러 수정 (잘못된 URL/method/payload) | BLOCKED | CONTEXT.md locked decision deferred URL/method/payload fixes to Phase 2. Plan-02 claimed BUG-03 but only produced a catalog. 14 mismatches remain in service files. REQUIREMENTS.md traceability table correctly shows BUG-03 as "Pending". |

**Discrepancy note:** Plan-02 frontmatter lists `requirements: [BUG-02, BUG-03]` implying BUG-03 is addressed, and the SUMMARY marks `requirements-completed: [BUG-02, BUG-03]`. However, BUG-03's requirement text requires *fixing* mismatches, not cataloging them. The ROADMAP SC3 and SC4 are explicit: "correct URL paths, HTTP methods, and payload shapes" and "zero 4xx/5xx errors caused by frontend request malformation." These are not met.

---

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `src/app/chat/[id]/page.tsx` | 133, 145 | `room.partner` used without optional chaining as a prop | Warning | `room` is guarded by `!room` exit at line 121, so this is safe in the render path. However, `partner` itself could be undefined on a valid room (e.g., group chats), which would propagate undefined to ChatHeader and MessageList. Not a crash-stopper given the guard, but leaves residual risk. |
| `src/app/dashboard/page.tsx` | - | `profile/[memberId]/page.tsx` is a thin wrapper; guards were applied to `ProfileView.tsx` | Info | Summary documents this deviation; guards are in the correct place per plan decision. |

---

### Human Verification Required

#### 1. Real Backend Integration Test

**Test:** Set `NEXT_PUBLIC_ENABLE_MSW=false` in `.env.local`, start the Docker backend, load `/dashboard`.
**Expected:** Page shows loading fallback while profile loads; no white-screen; no unhandled promise rejections in browser console; DevTools Network tab shows `/api/v1/members/me` routed to `localhost:8080`.
**Why human:** Cannot verify runtime fetch routing or absence of console errors programmatically.

#### 2. Polling Auto-Stop Behavior

**Test:** On the `/around-me` page, disconnect network or stop the backend, then wait for polling to trigger 3 times (~30 seconds).
**Expected:** Browser console shows `[useRadarLogic] Polling stopped after 3 consecutive failures`; subsequent network tab shows no more fetch calls at 10-second intervals.
**Why human:** Polling behavior requires live runtime observation with DevTools; cannot simulate via grep.

#### 3. Global Error Recovery UI

**Test:** Inject a throw in a root-layout provider (e.g., MSWProvider), reload the page.
**Expected:** `global-error.tsx` renders with "Something went wrong" heading, error message, and amber "Try again" button on an off-white background.
**Why human:** Requires browser to trigger root layout crash; cannot verify rendering path with static analysis.

#### 4. Chat Room Polling Failure Recovery

**Test:** Open `/chat/{id}` in the browser, stop the backend, wait ~10 seconds (3 poll cycles at 3s each).
**Expected:** Console shows `[ChatRoom] Message polling stopped after 3 consecutive failures`; no further polling requests visible in DevTools.
**Why human:** Requires controlled network failure and real-time DevTools observation.

---

### Gaps Summary

Two of the four ROADMAP Phase 1 Success Criteria are unmet:

**SC3 (All API calls use correct URL/method/payload)** and **SC4 (Zero 4xx/5xx errors from request malformation)** are not satisfied. The CONTEXT.md locked decision explicitly deferred these fixes to Phase 2, and the Phase 1 team produced a comprehensive catalog instead. This was a scoped decision — not an oversight — but it means BUG-03 is incomplete as defined in REQUIREMENTS.md.

The catalog itself is excellent (150 lines, 29 API calls audited, 14 mismatches categorized with Priority 1-4 fix recommendations) and positions Phase 2 well. The crash-prevention work (BUG-01 and BUG-02) is fully delivered and solid.

**Root cause of both gaps:** Deliberate scope boundary in CONTEXT.md ("Do NOT fix API URL/method/payload mismatches — those are Phase 2"). This is a valid phase-scoping decision but creates a discrepancy between the ROADMAP's success criteria and what Phase 1 delivered.

**Recommended resolution:** Either (a) update the ROADMAP Phase 1 success criteria to remove SC3/SC4 and move them to Phase 2, or (b) address BUG-03 properly in a Phase 2 gap-closure plan that fixes the 14 mismatches documented in the catalog.

---

*Verified: 2026-03-06*
*Verifier: Claude (gsd-verifier)*
