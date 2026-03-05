---
phase: 03-authentication
plan: 02
subsystem: auth
tags: [signup, react, nextjs, zustand, form-validation, api-integration]

# Dependency graph
requires:
  - phase: 03-01
    provides: AuthProvider, useAuthStore with setTokens, useUserStore with fetchProfile(force), members.ts signup/createProfile, pets.ts createPet/getBreeds, upload.ts uploadImageFlow
provides:
  - 3-step signup flow: Account -> Profile -> Pet -> Complete
  - SignupAccountStep with email+password+nickname, inline validation, per-field API error mapping
  - SignupProfileStep with nickname edit, optional image upload, optional self-introduction
  - SignupPetStep with birthDate (not age), breed dropdown, always-visible skip button
  - SignupComplete redirects to /dashboard, calls fetchProfile(true)
affects: [04-dashboard, 05-profile, 06-pet-management]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Per-step API calls in multi-step signup (each step calls its own endpoint before advancing)
    - Inline API error mapping (M007->email field, M003->nickname field) with suppressToast
    - Password strength criteria badges (5 criteria shown inline as user types)
    - Optional image upload with local preview then uploadImageFlow presigned URL flow

key-files:
  created:
    - aini-inu-frontend/src/components/signup/SignupAccountStep.tsx
    - aini-inu-frontend/src/components/signup/SignupProfileStep.tsx
    - aini-inu-frontend/src/components/signup/SignupPetStep.tsx
  modified:
    - aini-inu-frontend/src/app/signup/page.tsx
    - aini-inu-frontend/src/components/signup/SignupComplete.tsx
    - aini-inu-frontend/src/api/members.ts

key-decisions:
  - "signup/createProfile in members.ts accept optional ApiRequestOptions param to enable suppressToast per call site"
  - "SignupPage removes mounted/useEffect hydration guard (react-hooks/set-state-in-effect lint rule violation)"
  - "ImageIcon alias from lucide-react used in SignupProfileStep to avoid jsx-a11y/alt-text false positive on <Image> component"
  - "SignupComplete auto-redirects to /dashboard after 5s; also calls fetchProfile(true) to populate user state"

patterns-established:
  - "Inline API error mapping: catch ApiError, check errorCode, setFieldErrors for M007/M003, toast.error for others"
  - "suppressToast: true passed on signup calls that handle errors inline to prevent double-toasting"

requirements-completed: [AUTH-02, AUTH-05, AUTH-06]

# Metrics
duration: 7min
completed: 2026-03-06
---

# Phase 3 Plan 02: Signup Flow Summary

**3-step signup flow (Account->Profile->Pet->Complete) with per-step API calls, password strength badges, inline field errors for M007/M003, birthDate canonical input (DEC-003), and always-visible skip on pet step**

## Performance

- **Duration:** 7 min
- **Started:** 2026-03-05T23:24:18Z
- **Completed:** 2026-03-05T23:30:21Z
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments

- Rewrote signup/page.tsx with corrected step order ACCOUNT->PROFILE->PET->COMPLETE, removing DaumPostcode, useSignupForm, ManagerStep
- Created SignupAccountStep: email + password (5-criteria badges) + nickname, calls POST /members/signup, tokens passed to parent via onComplete callback
- Created SignupProfileStep: editable nickname (pre-filled), optional image upload via uploadImageFlow presigned flow, optional 200-char self-introduction
- Created SignupPetStep: birthDate canonical date input (no age field, DEC-003), breed dropdown from getBreeds(), always-visible skip button
- Updated SignupComplete: removed petName prop, added fetchProfile(true) on mount, 5-second auto-redirect to /dashboard

## Task Commits

Each task was committed atomically:

1. **Task 1: Rewrite signup page orchestration and Account step** - `707cba8` (feat)
2. **Task 2: Profile step, Pet step, and SignupComplete wiring** - `20018e2` (feat)

## Files Created/Modified

- `aini-inu-frontend/src/app/signup/page.tsx` - Orchestrates ACCOUNT->PROFILE->PET->COMPLETE steps, stores tokens from Account, passes nickname to Profile
- `aini-inu-frontend/src/components/signup/SignupAccountStep.tsx` - Email+password+nickname form, 5-criteria password badges, inline M007/M003 errors, calls POST /members/signup
- `aini-inu-frontend/src/components/signup/SignupProfileStep.tsx` - Nickname (pre-filled), optional image upload with preview, optional self-intro textarea, calls POST /members/profile
- `aini-inu-frontend/src/components/signup/SignupPetStep.tsx` - BirthDate date input, breed dropdown (getBreeds), gender/size/neutered, skip button, calls POST /pets
- `aini-inu-frontend/src/components/signup/SignupComplete.tsx` - Confetti, fetchProfile(true), 5s auto-redirect to /dashboard
- `aini-inu-frontend/src/api/members.ts` - signup/createProfile functions updated to accept optional ApiRequestOptions

## Decisions Made

- `signup` and `createProfile` in `members.ts` now accept an optional `ApiRequestOptions` parameter. This allows call sites to pass `{ suppressToast: true }` for inline error handling without double-toasting.
- Removed `mounted`/`useEffect` hydration guard from `SignupPage`. The original code had this pattern but it triggers `react-hooks/set-state-in-effect` lint error in the new file. Since the signup page is `'use client'` and the guard's only purpose was hydration safety, removing it is safe with the app router.
- `ImageIcon` alias from lucide-react used in SignupProfileStep to avoid `jsx-a11y/alt-text` false positive — the linter was treating `<Image>` (lucide icon component) as an HTML `<img>` element and expecting an `alt` prop.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Updated members.ts signup/createProfile to accept ApiRequestOptions**
- **Found during:** Task 1 (SignupAccountStep implementation)
- **Issue:** Plan specified using `{ suppressToast: true }` on the signup call, but the existing `signup()` function signature only accepted `MemberSignupRequest` — no options parameter
- **Fix:** Added optional `options?: ApiRequestOptions` parameter to both `signup()` and `createProfile()` in members.ts, passing it through to `apiClient.post()`
- **Files modified:** aini-inu-frontend/src/api/members.ts
- **Verification:** TypeScript compiled with no errors after fix
- **Committed in:** 707cba8 (Task 1 commit)

**2. [Rule 1 - Bug] Removed mounted/useEffect hydration guard triggering lint error**
- **Found during:** Task 2 (lint verification)
- **Issue:** `useEffect(() => setMounted(true), [])` pattern triggers `react-hooks/set-state-in-effect` lint error in the rewritten file
- **Fix:** Removed mounted guard entirely — signup page is `'use client'` with no SSR-sensitive content
- **Files modified:** aini-inu-frontend/src/app/signup/page.tsx
- **Verification:** `npm run build` passed cleanly, `npx eslint` on modified files shows no errors
- **Committed in:** 20018e2 (Task 2 commit)

**3. [Rule 1 - Bug] Renamed Image to ImageIcon import in SignupProfileStep**
- **Found during:** Task 2 (lint verification)
- **Issue:** `<Image>` lucide-react icon component was triggering `jsx-a11y/alt-text` false positive — linter treated the React component as an HTML `<img>` element
- **Fix:** Renamed import to `ImageIcon` to avoid name collision with HTML img element detection
- **Files modified:** aini-inu-frontend/src/components/signup/SignupProfileStep.tsx
- **Verification:** `npx eslint` on modified files shows no errors
- **Committed in:** 20018e2 (Task 2 commit)

---

**Total deviations:** 3 auto-fixed (3 bugs — all in execution/lint verification)
**Impact on plan:** All auto-fixes necessary for correctness. No scope creep. Plan intent fully preserved.

## Issues Encountered

None — all issues were caught during TypeScript check and lint verification steps, resolved immediately.

## Next Phase Readiness

- Complete authentication flow is now implemented: login (03-01) + signup (03-02)
- Tokens are stored in useAuthStore after signup, profile is fetched on complete step
- Ready for Phase 4 (dashboard) which will use the auth context and user store
- Old dead code files (AccountStep.tsx, ManagerStep.tsx, PetStep.tsx) remain in components/signup/ — deferred cleanup per plan

---
*Phase: 03-authentication*
*Completed: 2026-03-06*
