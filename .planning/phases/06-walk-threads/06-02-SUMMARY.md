---
phase: 06-walk-threads
plan: 02
subsystem: ui
tags: [react, nextjs, threads, pets, daum-postcode, form, validation]

# Dependency graph
requires:
  - phase: 06-01
    provides: useRadarLogic hook with myPets, editingThreadId, coordinates, handleRefresh
  - phase: 05-01
    provides: PetResponse type and getMyPets API from @/api/pets
provides:
  - RecruitForm component with full ThreadCreateRequest fields
  - Non-pet-owner block message linking to profile
  - ChatType toggle (INDIVIDUAL/GROUP) with tooltips
  - Edit mode that pre-fills from getThread()
  - Inline DaumPostcode address search
  - Pet multi-select using PetResponse
affects: [06-03, phase-07]

# Tech tracking
tech-stack:
  added: []
  patterns: [inline DaumPostcode modal inside form component, ISO datetime composition from separate date+time inputs]

key-files:
  created: []
  modified:
    - aini-inu-frontend/src/components/around-me/RecruitForm.tsx
    - aini-inu-frontend/src/app/around-me/page.tsx

key-decisions:
  - "RecruitForm manages DaumPostcode internally (not through page.tsx modal) — keeps location concern inside the form"
  - "ISO datetime composed from walkDate + time input on-the-fly rather than storing pre-composed string"
  - "Edit mode uses separate isFetchingEdit state rather than reusing isSubmitting to avoid UI flicker"
  - "Non-pet-owner block rendered as early return before any form state — simpler than conditional inside form"

patterns-established:
  - "Form with date+time inputs: compose ISO string at submit time using walkDate+HH:mm template"
  - "Pet multi-select: toggle array of ids with includes() check"

requirements-completed: [WALK-01, WALK-02, WALK-03, WALK-10, WALK-13, WALK-14]

# Metrics
duration: 2min
completed: 2026-03-06
---

# Phase 06 Plan 02: Walk Threads - RecruitForm Rewrite Summary

**RecruitForm rewritten with all ThreadCreateRequest fields (title, walkDate, startTime, endTime, chatType, maxParticipants, location, petIds), non-pet-owner block, edit mode pre-fill, and inline DaumPostcode**

## Performance

- **Duration:** 2 min
- **Started:** 2026-03-06T09:35:29Z
- **Completed:** 2026-03-06T09:37:23Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments
- Rewrote RecruitForm to accept myPets (PetResponse[]), editingThreadId, coordinates, and onSuccess props
- Non-pet-owner block: shows card with alert icon and link to /profile when myPets.length === 0
- ChatType toggle: two styled buttons (INDIVIDUAL/GROUP) with hover tooltips, defaults to INDIVIDUAL
- Edit mode: useEffect fetches getThread(editingThreadId) and pre-fills all fields including petIds
- Pet multi-select: card grid with photo/name, toggles selectedPetIds[], checkmark overlay on selected
- DaumPostcode: inline search panel inside the form (not a separate modal), sets placeName and address
- Submit disabled until title + walkDate + startTime + selectedPetIds are all filled
- Description limit enforced with inline error and disabled submit
- Wired page.tsx RECRUIT tab to pass new props; removed all legacy DogType/ThreadType prop wiring

## Task Commits

Each task was committed atomically:

1. **Task 1 + Task 2: Rewrite RecruitForm + wire page.tsx** - `20dcc45` (feat)

**Plan metadata:** (to be committed with SUMMARY)

## Files Created/Modified
- `aini-inu-frontend/src/components/around-me/RecruitForm.tsx` - Full rewrite with all required fields
- `aini-inu-frontend/src/app/around-me/page.tsx` - Updated RECRUIT tab to pass new RecruitForm props

## Decisions Made
- RecruitForm owns DaumPostcode internally rather than delegating to page.tsx modal — cleaner separation, form handles all location concerns
- Separate date and time inputs with ISO composition at submit time — better UX and no hidden state coupling
- isFetchingEdit state separate from isSubmitting — prevents the submit button from showing "loading" state during initial data fetch
- Non-pet-owner guard is an early return before any form state setup — simpler control flow

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- RecruitForm is fully functional with all ThreadCreateRequest fields
- Page.tsx RECRUIT tab now uses real API props via useRadarLogic
- Ready for Plan 03: thread detail view and apply flow

---
*Phase: 06-walk-threads*
*Completed: 2026-03-06*
