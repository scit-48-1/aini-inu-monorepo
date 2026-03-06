---
status: resolved
trigger: "Investigate why pet image upload fails during pet registration"
created: 2026-03-06T00:00:00Z
updated: 2026-03-06T09:00:00Z
---

## Current Focus

hypothesis: Frontend sends purpose 'PET_PROFILE' but backend enum only has 'PET_PHOTO'
test: Compare frontend purpose string to backend UploadPurpose enum values
expecting: Mismatch causes INVALID_UPLOAD_PURPOSE error on presigned URL request
next_action: Return diagnosis

## Symptoms

expected: Pet photo uploads successfully during pet registration
actual: Image upload fails with error toast
errors: Backend throws INVALID_UPLOAD_PURPOSE (UploadPurpose.from() cannot match 'PET_PROFILE')
reproduction: Attempt to upload a photo in the pet registration form
started: Since PetForm was created with uploadImageFlow integration

## Eliminated

(none needed -- root cause found on first hypothesis)

## Evidence

- timestamp: 2026-03-06
  checked: PetForm.tsx line 74 -- purpose string passed to uploadImageFlow
  found: Purpose is hardcoded as 'PET_PROFILE'
  implication: This string is sent to backend as the presigned URL purpose

- timestamp: 2026-03-06
  checked: UploadPurpose.java enum values
  found: Enum values are PROFILE, PET_PHOTO, POST, WALK_DIARY, LOST_PET, SIGHTING -- no PET_PROFILE
  implication: 'PET_PROFILE' does not match any enum value, causing INVALID_UPLOAD_PURPOSE exception

- timestamp: 2026-03-06
  checked: UploadPurpose.from() method
  found: Case-insensitive match against enum names, throws BusinessException on no match
  implication: Confirms the mismatch causes a 400-level error before upload even begins

## Resolution

root_cause: Frontend passes 'PET_PROFILE' as the upload purpose, but the backend UploadPurpose enum defines 'PET_PHOTO' for pet images -- the presigned URL request fails before the actual file upload ever happens
fix: Change 'PET_PROFILE' to 'PET_PHOTO' in PetForm.tsx line 74
verification: (pending)
files_changed: []
