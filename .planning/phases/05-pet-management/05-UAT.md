---
status: diagnosed
phase: 05-pet-management
source: [05-01-SUMMARY.md, 05-02-SUMMARY.md, 05-03-SUMMARY.md]
started: 2026-03-06T00:00:00Z
updated: 2026-03-06T06:30:00Z
---

## Current Test

[testing complete]

## Tests

### 1. Empty State - No Pets
expected: On your profile page (My Profile), when you have no registered pets, ProfileDogs shows a Dog icon, Korean message (e.g. "아직 등록된 반려견이 없어요"), and an amber register button that opens the pet registration modal when clicked.
result: pass

### 2. Pet Card Grid
expected: When you have pets registered, each pet card shows: photo (or logo fallback), name, breed, age, up to 3 personality badges, and walkingStyle codes as amber badges. The main pet has an amber Crown icon in the top-right corner of the card image.
result: pass
note: Initial report of missing age/personality/walkingStyle was due to corrupt seed data in DB, not a code issue. Confirmed working with real pet data.

### 3. 10-Pet Limit
expected: When you have 10 pets registered, the add button shows "{n}/10마리" count and is disabled, with the message "최대 10마리까지 등록할 수 있습니다" displayed.
result: pass

### 4. Register New Pet
expected: Clicking the add pet button opens DogRegisterModal with PetForm in create mode. The form has fields for: name, breed (dropdown), birthDate, gender (button group), size (button group), isNeutered toggle, MBTI, photo upload, certificationNumber, walkingStyles (multi-select badges), personalities (multi-select badges). Submitting creates the pet and the list refreshes.
result: issue
reported: "펫 등록 폼의 항목은 모두 뜨지만... 펫 이미지 등록에 실패하네.. 백엔드 문제일까?"
severity: major

### 5. PetForm Validation
expected: Submitting PetForm with missing required fields shows Korean toast.warning messages (e.g. for missing name, breed, etc.) and does not submit.
result: pass

### 6. View Pet Details
expected: Clicking a pet card opens DogDetailModal showing the pet's details (PetResponse fields). The modal has a main-pet toggle and a delete button.
result: pass

### 7. Edit Pet
expected: In DogDetailModal (or via an edit action), DogRegisterModal opens in edit mode. Breed, gender, and size fields are shown as disabled read-only text. Saving updates the pet and the list refreshes.
result: issue
reported: "다른것은 모두 잘 되는데, 수정하기를 누른 순간 birthDate만 빈값으로 되어 있어. 그래서 수정을 원한다면 반드시 birthDate을 지정해줘야하네"
severity: major

### 8. Delete Pet
expected: Clicking delete in DogDetailModal opens a DeleteConfirmDialog showing the pet's name. Confirming deletion removes the pet and closes the modal. The list refreshes showing the pet is gone.
result: pass

### 9. Set Main Pet
expected: Toggling the main pet switch in DogDetailModal updates immediately (optimistic UI). A success toast appears. The pet cards update to show/remove the Crown icon on the appropriate card.
result: pass

### 10. Master Data Loading
expected: When opening the pet registration form, breed, personality, and walkingStyle options load from the backend. If loading fails, a Korean error toast "마스터 데이터를 불러오는데 실패했습니다." appears.
result: pass

## Summary

total: 10
passed: 8
issues: 2
pending: 0
skipped: 0

## Gaps

- truth: "Pet image upload works during pet registration"
  status: failed
  reason: "User reported: 펫 등록 폼의 항목은 모두 뜨지만... 펫 이미지 등록에 실패하네.. 백엔드 문제일까?"
  severity: major
  test: 4
  root_cause: "Frontend sends upload purpose 'PET_PROFILE' but backend UploadPurpose enum expects 'PET_PHOTO', causing INVALID_UPLOAD_PURPOSE exception"
  artifacts:
    - path: "aini-inu-frontend/src/components/profile/PetForm.tsx"
      issue: "Line 74 passes 'PET_PROFILE' as purpose -- should be 'PET_PHOTO'"
    - path: "aini-inu-backend/src/main/java/scit/ainiinu/community/dto/UploadPurpose.java"
      issue: "Defines PET_PHOTO (not PET_PROFILE) as the enum value for pet image uploads"
  missing:
    - "Change 'PET_PROFILE' to 'PET_PHOTO' in PetForm.tsx line 74"
  debug_session: ".planning/debug/pet-image-upload-fails.md"

- truth: "Edit modal pre-fills birthDate with the pet's existing value"
  status: failed
  reason: "User reported: 다른것은 모두 잘 되는데, 수정하기를 누른 순간 birthDate만 빈값으로 되어 있어. 그래서 수정을 원한다면 반드시 birthDate을 지정해줘야하네"
  severity: major
  test: 7
  root_cause: "Two-layer gap: (1) backend PetResponse returns only computed 'age' not raw 'birthDate', (2) PetForm.tsx line 49 hardcodes useState('') instead of reading from initialData"
  artifacts:
    - path: "aini-inu-backend/src/main/java/scit/ainiinu/pet/dto/response/PetResponse.java"
      issue: "Missing birthDate field; only returns computed age"
    - path: "aini-inu-frontend/src/api/pets.ts"
      issue: "PetResponse interface lacks birthDate field"
    - path: "aini-inu-frontend/src/components/profile/PetForm.tsx"
      issue: "Line 49 hardcodes birthDate to '' instead of reading from initialData"
  missing:
    - "Add birthDate: LocalDate to PetResponse.java and map in from() builder"
    - "Add birthDate?: string to PetResponse interface in api/pets.ts"
    - "Change PetForm line 49 to useState(initialData?.birthDate || '')"
  debug_session: ".planning/debug/birthdate-empty-edit-modal.md"
