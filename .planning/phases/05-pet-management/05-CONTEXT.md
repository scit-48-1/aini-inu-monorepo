# Phase 5: Pet Management - Context

**Gathered:** 2026-03-06
**Status:** Ready for planning

<domain>
## Phase Boundary

Rewire all pet management screens to the Phase 2 `api/pets.ts` infrastructure. Users can register, edit, and delete pets, set a main pet, and all form selects are populated from master data endpoints. Frontend-only modifications (aini-inu-frontend/).

**API endpoints in scope:**
- `POST /api/v1/pets` (FR-PET-001, PET-CREATE)
- `PATCH /api/v1/pets/{petId}` (FR-PET-001, PET-PATCH)
- `DELETE /api/v1/pets/{petId}` (FR-PET-001, PET-DELETE)
- `PATCH /api/v1/pets/{petId}/main` (FR-PET-002, PET-MAIN-PATCH)
- `GET /api/v1/breeds` (FR-PET-003, PET-BREEDS-GET)
- `GET /api/v1/personalities` (FR-PET-003, PET-PERSONALITIES-GET)
- `GET /api/v1/walking-styles` (FR-PET-003, PET-WALKING-STYLES-GET)
- `GET /api/v1/pets` (implicit, PET-LIST — used by profile)

**Applicable DEC:**
- DEC-003: birthDate canonical single input, age input not allowed
- PRD SS8.1: Pet name max 10 chars, max 10 pets per member

**Modification scope:** aini-inu-frontend/ only. Backend and common-docs are read-only.

</domain>

<decisions>
## Implementation Decisions

### Pet CRUD form
- Full pet form includes all PetCreateRequest fields: name, breedId, birthDate, gender, size, isNeutered, mbti, photoUrl, certificationNumber, walkingStyles, personalityIds
- birthDate: canonical date input only (DEC-003) — no age input field
- Name: max 10 chars enforced (PRD SS8.1)
- Breed: dropdown populated from `GET /api/v1/breeds` master data
- Personality: multi-select populated from `GET /api/v1/personalities` master data
- Walking styles: multi-select populated from `GET /api/v1/walking-styles` master data
- Photo upload: uses Phase 2 presigned URL upload utility (`api/upload.ts`)
- Edit form pre-populates all fields from existing PetResponse

### Pet list display
- ProfileDogs rewired from old `DogType` to `PetResponse`
- Cards show: photo, name, breed, age, personality tags, walking styles, main pet indicator
- "Add New Partner" button (already exists) — disabled when 10 pets reached with message explaining limit

### Main pet switching (FR-PET-002)
- Main pet indicated visually on card (crown/star icon or badge)
- Switch via card action menu or direct tap on indicator
- `PATCH /api/v1/pets/{petId}/main` call with immediate UI update

### Pet deletion
- Delete requires confirmation (modal or dialog)
- Cannot delete main pet without first switching main to another pet (if applicable — follow backend validation)

### 10-pet limit (PRD SS8.1)
- Add button disabled at 10 pets with clear message: "최대 10마리까지 등록할 수 있습니다"
- Pet count visible near the add button

### 5-state coverage (PRD SS8.3)
- Default: pet list loaded with all cards visible
- Loading: spinner/skeleton while fetching pets and master data
- Empty: "Register your first pet" prompt with add button
- Error: fetch failed state with retry
- Success: toast on create/edit/delete success

### Claude's Discretion
- Pet form layout (single-page vs multi-section)
- Edit flow UX (modal from card click vs dedicated page)
- Master data dropdown styling and search behavior
- Card layout density and visual weight of personality/walk-style tags
- Delete confirmation dialog design
- Main pet indicator icon choice and placement

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets
- `api/pets.ts`: All 8 endpoint functions typed and ready — `getMyPets`, `createPet`, `updatePet`, `deletePet`, `setMainPet`, `getBreeds`, `getPersonalities`, `getWalkingStyles`
- `SignupPetStep.tsx`: Working pet registration form (basic fields: name, breed, birthDate, gender, size, neutered) — can be extended or used as reference for the full form
- `ProfileDogs.tsx`: Pet card grid with "Add New Partner" button — needs DogType -> PetResponse rewire
- `api/upload.ts`: Presigned URL upload utility from Phase 2

### Established Patterns
- `'use client'` on all pages and components
- Composition pattern: explicit variant components (Phase 3/4 pattern)
- Toast-only errors via sonner (Korean messages from backend)
- `cn()` utility for conditional classNames
- Optimistic UI with failure rollback (established in Phase 4 follow toggle)

### Integration Points
- `ProfileDogs.tsx` in profile page — pet tab content, wired via ProfileTabs
- `SignupPetStep.tsx` — shares pet creation logic, could share form component
- `api/pets.ts` — source of truth for all pet API calls
- `useUserStore` — may need refresh after pet changes if profile displays pet count

</code_context>

<specifics>
## Specific Ideas

- User provided explicit DoD: CRUD + image upload + main pet change + master data selectors + birthDate-only + 5-state coverage
- Phase 3 signup pet step already works for basic registration — extend pattern for full management
- Old `PetStep.tsx` (MSW-era) uses `DogFormFields` and `DOG_BREEDS` constants — should be fully replaced with backend master data

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 05-pet-management*
*Context gathered: 2026-03-06*
