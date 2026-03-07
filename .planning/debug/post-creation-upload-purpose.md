---
status: diagnosed
trigger: "Post creation fails with '업로드 목적 또는 파일 정보가 올바르지 않습니다' (Upload purpose or file info is invalid)"
created: 2026-03-07T00:00:00Z
updated: 2026-03-07T00:00:00Z
---

## Current Focus

hypothesis: Frontend sends 'COMMUNITY_POST' as upload purpose but backend enum only has 'POST'
test: Compare frontend purpose string to backend UploadPurpose enum values
expecting: Mismatch between frontend string and backend enum
next_action: report root cause

## Symptoms

expected: Post creation with image upload should succeed via presigned URL flow
actual: Fails with error "업로드 목적 또는 파일 정보가 올바르지 않습니다" (CO008 INVALID_UPLOAD_PURPOSE)
errors: CommunityErrorCode.INVALID_UPLOAD_PURPOSE thrown during UploadPurpose.from()
reproduction: Attempt to create a post with an image in the community feed
started: After Phase 09 rewired post creation to use presigned URL upload

## Eliminated

(none needed -- root cause found on first hypothesis)

## Evidence

- timestamp: 2026-03-07T00:00:00Z
  checked: usePostForm.ts line 42
  found: Frontend calls uploadImageFlow(f, 'COMMUNITY_POST') -- purpose string is 'COMMUNITY_POST'
  implication: This string is sent as the `purpose` field in the presigned URL request body

- timestamp: 2026-03-07T00:00:00Z
  checked: UploadPurpose.java enum values
  found: Enum values are PROFILE, PET_PHOTO, POST, WALK_DIARY, LOST_PET, SIGHTING -- no COMMUNITY_POST
  implication: UploadPurpose.from("COMMUNITY_POST") fails because no enum value matches

- timestamp: 2026-03-07T00:00:00Z
  checked: UploadPurpose.from() method
  found: Does case-insensitive match against enum names; throws INVALID_UPLOAD_PURPOSE on no match
  implication: 'COMMUNITY_POST' does not match any of the 6 enum values, causing the error

- timestamp: 2026-03-07T00:00:00Z
  checked: CommunityErrorCode.INVALID_UPLOAD_PURPOSE
  found: Maps to CO008 with message "업로드 목적 또는 파일 정보가 올바르지 않습니다"
  implication: Confirms this is the exact error the user sees

## Resolution

root_cause: Frontend passes 'COMMUNITY_POST' as the upload purpose string, but backend UploadPurpose enum expects 'POST'. The mismatch causes UploadPurpose.from() to throw INVALID_UPLOAD_PURPOSE.
fix: Change the purpose string in usePostForm.ts line 42 from 'COMMUNITY_POST' to 'POST'
verification: (not applied -- diagnose only)
files_changed: []
