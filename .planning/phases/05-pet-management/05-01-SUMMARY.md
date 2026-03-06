---
phase: 05-pet-management
plan: 01
subsystem: frontend/pet
tags: [pet, profile, master-data, type-fix]
dependency_graph:
  requires: []
  provides: [useMasterData, PetForm, ProfileDogs-PetResponse]
  affects: [MyProfileView, ProfileView, PetForm, ProfileDogs]
tech_stack:
  added: []
  patterns: [useState+useEffect hook, Promise.all master data fetch, badge multi-select]
key_files:
  created:
    - aini-inu-frontend/src/hooks/useMasterData.ts
    - aini-inu-frontend/src/components/profile/PetForm.tsx
  modified:
    - aini-inu-frontend/src/api/pets.ts
    - aini-inu-frontend/src/components/profile/ProfileDogs.tsx
    - aini-inu-frontend/src/components/profile/MyProfileView.tsx
    - aini-inu-frontend/src/components/profile/ProfileView.tsx
decisions:
  - PetResponse.walkingStyles fixed to string[] to match backend List<String> response
  - useMasterData fetches breeds/personalities/walkingStyles via Promise.all on mount
  - PetForm edit mode shows breed/gender/size as read-only (backend rejects changes)
  - ProfileDogs main pet indicator uses Crown icon (lucide-react) in top-right of card image
  - ProfileView.tsx uses 'as any' cast on ProfileDogs (legacy memberService rewire deferred to Plan 02)
  - mapPetResponseToDogType removed from MyProfileView; dogs/selectedDog state renamed to pets/selectedPet
metrics:
  duration: 3 min
  completed: "2026-03-06"
  tasks_completed: 2
  files_changed: 6
---

# Phase 5 Plan 1: PetResponse Type Fix, useMasterData Hook, PetForm Component Summary

**One-liner:** Fixed PetResponse.walkingStyles type mismatch, created useMasterData hook fetching 3 master data endpoints, rewired ProfileDogs from DogType to PetResponse with crown indicator and 10-pet limit.

## Tasks Completed

| # | Task | Commit | Files |
|---|------|--------|-------|
| 1 | Fix PetResponse type, create useMasterData hook and PetForm component | 27dff26 | pets.ts, useMasterData.ts, PetForm.tsx, MyProfileView.tsx |
| 2 | Rewire ProfileDogs and MyProfileView to use PetResponse directly | f430c0e | ProfileDogs.tsx, MyProfileView.tsx, ProfileView.tsx |

## What Was Built

### useMasterData Hook
- Fetches breeds, personalities, and walkingStyles from backend master data endpoints via `Promise.all`
- Returns `{ breeds, personalities, walkingStyles, isLoading, error }`
- Toast error on failure: `'마스터 데이터를 불러오는데 실패했습니다.'`
- Cleanup via cancelled flag to prevent state updates on unmount

### PetForm Component
- Supports both `create` and `edit` modes via `mode` prop
- All PetCreateRequest fields: name (max 10), breedId dropdown, birthDate (date input only), gender button group, size button group, isNeutered toggle, mbti (max 4), photoUrl (uploadImageFlow), certificationNumber (max 15), walkingStyles (multi-select badges), personalityIds (multi-select badges)
- Edit mode: breed, gender, size shown as disabled read-only text (not updatable per backend)
- Validation with Korean `toast.warning()` messages before submit
- Photo upload via `uploadImageFlow(file, 'PET_PROFILE')` with preview

### ProfileDogs Rewrite
- Props changed from `{ dogs: DogType[], onDogClick, onAddClick }` to `{ pets: PetResponse[], onPetClick, onAddClick }`
- Cards render: photoUrl (fallback to logo), name, breed.name, age, personalities (Badge, limit 3), walkingStyles (codes as amber badges)
- Main pet indicator: amber Crown icon (lucide-react) at top-right corner of card image area when `pet.isMain === true`
- 10-pet limit: add button disabled with count `{n}/10마리` and message `최대 10마리까지 등록할 수 있습니다`

### MyProfileView Update
- `mapPetResponseToDogType` function removed entirely
- State renamed: `dogs` → `pets` (PetResponse[]), `selectedDog` → `selectedPet`, `editingDog` → `editingPet`
- `isAnyDogVerified` now uses `pets.some(p => p.isCertified)` directly
- DogRegisterModal/DogDetailModal: `as any` cast (rewire deferred to Plan 02)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed mapPetResponseToDogType after walkingStyles type change**
- **Found during:** Task 1 verification (TypeScript check)
- **Issue:** `mapPetResponseToDogType` accessed `walkingStyles[0]?.name` but after fix `walkingStyles` is `string[]` not `WalkingStyleResponse[]`
- **Fix:** Changed to `walkingStyles[0] || ''` (access element directly as string code)
- **Files modified:** `MyProfileView.tsx`
- **Commit:** 27dff26

**2. [Rule 3 - Blocking] Fixed ProfileView.tsx build failure**
- **Found during:** Task 2 build verification
- **Issue:** `ProfileView.tsx` (older component) still referenced `ProfileDogs` with `dogs`/`onDogClick` props which no longer exist
- **Fix:** Applied `as any` cast matching the plan's approach for legacy modal compatibility; added TODO comment for Plan 02 rewire
- **Files modified:** `ProfileView.tsx`
- **Commit:** f430c0e

## Self-Check: PASSED

- [x] `aini-inu-frontend/src/hooks/useMasterData.ts` — created
- [x] `aini-inu-frontend/src/components/profile/PetForm.tsx` — created
- [x] `aini-inu-frontend/src/api/pets.ts` — walkingStyles fixed to string[]
- [x] `aini-inu-frontend/src/components/profile/ProfileDogs.tsx` — rewired to PetResponse
- [x] `aini-inu-frontend/src/components/profile/MyProfileView.tsx` — DogType mapping removed
- [x] Commits 27dff26 and f430c0e exist
- [x] Build passes (`npm run build` exits 0)
