---
phase: 05-pet-management
plan: 04
subsystem: ui
tags: [react, pet-form, upload, frontend-bugfix]

# Dependency graph
requires:
  - phase: 05-pet-management
    provides: PetForm component, pet API types
provides:
  - Correct pet image upload using PET_PHOTO purpose
  - Edit-mode birthDate handling that does not force re-entry
affects: []

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Conditional payload inclusion: omit optional fields when empty rather than sending empty strings"

key-files:
  created: []
  modified:
    - aini-inu-frontend/src/components/profile/PetForm.tsx
    - aini-inu-frontend/src/api/pets.ts

key-decisions:
  - "birthDate made optional in PetCreateRequest since backend stores computed age, not raw birthDate"
  - "Edit-mode birth date field marked as optional in UI with (선택) label"

patterns-established:
  - "Conditional payload pattern: build base object, then conditionally add optional fields"

requirements-completed: [PET-01, PET-02]

# Metrics
duration: 1min
completed: 2026-03-06
---

# Phase 05 Plan 04: UAT Gap Closure Summary

**Fixed pet image upload purpose mismatch (PET_PROFILE -> PET_PHOTO) and edit-mode birthDate blocking submission**

## Performance

- **Duration:** 1 min
- **Started:** 2026-03-06T07:49:24Z
- **Completed:** 2026-03-06T07:50:50Z
- **Tasks:** 1
- **Files modified:** 2

## Accomplishments
- Pet image upload now uses correct 'PET_PHOTO' purpose string matching backend UploadPurpose enum
- Edit mode no longer requires birthDate re-entry (field is optional, omitted from payload when empty)
- PetCreateRequest type updated to make birthDate optional for edit-mode compatibility

## Task Commits

Each task was committed atomically:

1. **Task 1: Fix upload purpose and edit-mode birthDate handling** - `ea3262e` (fix)

## Files Created/Modified
- `aini-inu-frontend/src/components/profile/PetForm.tsx` - Fixed upload purpose string, conditional birthDate validation, conditional payload inclusion
- `aini-inu-frontend/src/api/pets.ts` - Made birthDate optional in PetCreateRequest interface

## Decisions Made
- Made birthDate optional in PetCreateRequest type since the backend Pet entity stores computed age (Integer), not raw birthDate -- edit mode cannot populate birthDate from response
- Added "(선택)" label to birthDate field in edit mode and removed HTML required attribute to match the optional behavior

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Made birthDate optional in PetCreateRequest type**
- **Found during:** Task 1
- **Issue:** PetCreateRequest had birthDate as required (string), but edit-mode payload omits it when empty -- TypeScript error
- **Fix:** Changed birthDate from `string` to `string?` (optional) in PetCreateRequest interface
- **Files modified:** aini-inu-frontend/src/api/pets.ts
- **Verification:** tsc --noEmit passes
- **Committed in:** ea3262e

**2. [Rule 1 - Bug] Made HTML required attribute conditional on birth date input**
- **Found during:** Task 1
- **Issue:** HTML required attribute on date input would block form submission in edit mode even though JS validation was conditional
- **Fix:** Changed `required` to `required={!isEdit}` and updated label to show "(선택)" in edit mode
- **Files modified:** aini-inu-frontend/src/components/profile/PetForm.tsx
- **Verification:** npm run build succeeds
- **Committed in:** ea3262e

---

**Total deviations:** 2 auto-fixed (1 blocking, 1 bug)
**Impact on plan:** Both fixes necessary for correctness. No scope creep.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Phase 05 pet management UAT gaps are fully closed
- Both UAT Test 4 (image upload) and Test 7 (birthDate in edit) root causes resolved

---
*Phase: 05-pet-management*
*Completed: 2026-03-06*
