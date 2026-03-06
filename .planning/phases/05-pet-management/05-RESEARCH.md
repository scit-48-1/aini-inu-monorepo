# Phase 5: Pet Management - Research

**Researched:** 2026-03-06
**Domain:** Frontend pet CRUD, master data selectors, profile rewiring
**Confidence:** HIGH

## Summary

Phase 5 rewires all pet management UI in `aini-inu-frontend/` to use the real backend API via the Phase 2 `api/pets.ts` service layer. The API layer is already complete with all 8 endpoint functions typed and ready. The core work is: (1) replacing the legacy `DogFormFields` + `useDogForm` hook (which use hardcoded `DOG_BREEDS`/`DOG_PERSONALITIES` constants and the old `DogType` model) with new components that fetch master data from backend endpoints, (2) rewiring `ProfileDogs` from `DogType` to `PetResponse`, and (3) adding main pet switching, delete confirmation, and 10-pet limit enforcement.

The existing `SignupPetStep.tsx` (Phase 3) already demonstrates the correct pattern: fetching breeds from `getBreeds()` API, using `breedId` (number) instead of breed name strings, and submitting via `createPet()`. This pattern should be extended for the full pet form with personalities, walking styles, photo upload, and MBTI.

**Primary recommendation:** Build a new `PetForm` component that fetches all three master data endpoints on mount, replaces the legacy `DogFormFields`/`useDogForm`, and works for both create and edit modes. Eliminate the `DogType` intermediary mapping in `MyProfileView` by passing `PetResponse[]` directly to a rewired `ProfileDogs`.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- Full pet form includes all PetCreateRequest fields: name, breedId, birthDate, gender, size, isNeutered, mbti, photoUrl, certificationNumber, walkingStyles, personalityIds
- birthDate: canonical date input only (DEC-003) -- no age input field
- Name: max 10 chars enforced (PRD SS8.1)
- Breed: dropdown populated from `GET /api/v1/breeds` master data
- Personality: multi-select populated from `GET /api/v1/personalities` master data
- Walking styles: multi-select populated from `GET /api/v1/walking-styles` master data
- Photo upload: uses Phase 2 presigned URL upload utility (`api/upload.ts`)
- Edit form pre-populates all fields from existing PetResponse
- ProfileDogs rewired from old `DogType` to `PetResponse`
- Cards show: photo, name, breed, age, personality tags, walking styles, main pet indicator
- "Add New Partner" button -- disabled when 10 pets reached with message explaining limit
- Main pet indicated visually on card (crown/star icon or badge)
- Switch via card action menu or direct tap on indicator
- `PATCH /api/v1/pets/{petId}/main` call with immediate UI update
- Delete requires confirmation (modal or dialog)
- Cannot delete main pet without first switching main to another pet (if applicable -- follow backend validation)
- Add button disabled at 10 pets with clear message: "최대 10마리까지 등록할 수 있습니다"
- Pet count visible near the add button
- 5-state coverage: default/loading/empty/error/success

### Claude's Discretion
- Pet form layout (single-page vs multi-section)
- Edit flow UX (modal from card click vs dedicated page)
- Master data dropdown styling and search behavior
- Card layout density and visual weight of personality/walk-style tags
- Delete confirmation dialog design
- Main pet indicator icon choice and placement

### Deferred Ideas (OUT OF SCOPE)
None -- discussion stayed within phase scope
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| PET-01 | 반려견 등록 -- birthDate canonical 단일 입력 (DEC-003) | PetCreateRequest schema verified; SignupPetStep pattern reusable; master data endpoints confirmed |
| PET-02 | 반려견 수정 (PET-PATCH) | PetUpdateRequest schema verified -- all fields optional (null = no change); walkingStyleCodes is preferred field |
| PET-03 | 반려견 삭제 (PET-DELETE) | DELETE endpoint confirmed; backend returns P006 for non-owner, P001 for not found |
| PET-04 | 메인 반려견 변경 (PET-MAIN-PATCH) | PATCH /pets/{petId}/main confirmed; returns MainPetChangeResponse {id, name, isMain} |
| PET-05 | 견종 마스터 조회 (PET-BREEDS-GET) | GET /breeds returns BreedResponse[] {id, name, size}; already used in SignupPetStep |
| PET-06 | 성향 마스터 조회 (PET-PERSONALITIES-GET) | GET /personalities returns PersonalityResponse[] {id, name, code}; replaces DOG_PERSONALITIES constant |
| PET-07 | 산책스타일 마스터 조회 (PET-WALKING-STYLES-GET) | GET /walking-styles returns WalkingStyleResponse[] {id, name, code}; replaces hardcoded walk style buttons |
| PET-08 | 이름 최대 10자, 회원당 최대 10마리 제한 | Backend validates name 1-10 chars; 10-pet limit enforced client-side (disable add button) |
</phase_requirements>

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| React 19 | 19.x | UI framework | Project standard |
| Next.js 16 | 16.x | App Router, page routing | Project standard |
| sonner | existing | Toast notifications | Established in Phase 2+ |
| lucide-react | existing | Icon library | Used throughout project |
| Tailwind CSS 4 | 4.x | Styling | Project standard |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `@/api/pets.ts` | Phase 2 | All 8 pet API functions | Every API call |
| `@/api/upload.ts` | Phase 2 | presigned URL image upload | Pet photo upload |
| `@/lib/utils` (cn) | existing | Conditional classNames | All styling logic |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Custom form state | react-hook-form | Overkill for this scope; project uses useState pattern consistently |
| Zustand pet store | Local component state | Pet data is profile-scoped, not global; fetchData pattern established |

## Architecture Patterns

### Recommended Component Structure
```
src/
├── components/
│   └── profile/
│       ├── ProfileDogs.tsx          # REWRITE: DogType -> PetResponse
│       ├── PetForm.tsx              # NEW: Full pet create/edit form
│       ├── PetCard.tsx              # NEW (optional): Individual pet card
│       ├── DogRegisterModal.tsx     # REWRITE: Use PetForm, remove DogFormFields
│       ├── DogDetailModal.tsx       # REWRITE: DogType -> PetResponse, add delete/main-switch
│       └── DeleteConfirmDialog.tsx  # NEW: Confirmation dialog for pet deletion
├── hooks/
│   └── useMasterData.ts            # NEW: Fetch breeds/personalities/walkingStyles
└── (remove or deprecate)
    ├── hooks/forms/useDogForm.ts    # DEPRECATED: Uses hardcoded constants
    └── components/shared/forms/DogFormFields.tsx  # DEPRECATED: Uses DogType model
```

### Pattern 1: Master Data Hook
**What:** Custom hook that fetches breeds, personalities, and walking styles on mount, caches results
**When to use:** Any component rendering the pet form (create or edit)
**Example:**
```typescript
// Source: Established project pattern (useState + useEffect)
function useMasterData() {
  const [breeds, setBreeds] = useState<BreedResponse[]>([]);
  const [personalities, setPersonalities] = useState<PersonalityResponse[]>([]);
  const [walkingStyles, setWalkingStyles] = useState<WalkingStyleResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(false);

  useEffect(() => {
    Promise.all([getBreeds(), getPersonalities(), getWalkingStyles()])
      .then(([b, p, w]) => { setBreeds(b); setPersonalities(p); setWalkingStyles(w); })
      .catch(() => { setError(true); toast.error('마스터 데이터를 불러오는데 실패했습니다.'); })
      .finally(() => setIsLoading(false));
  }, []);

  return { breeds, personalities, walkingStyles, isLoading, error };
}
```

### Pattern 2: Eliminate DogType Mapping
**What:** Remove `mapPetResponseToDogType` intermediary; pass PetResponse directly
**When to use:** ProfileDogs, DogDetailModal, all pet card rendering
**Example:**
```typescript
// BEFORE (current MyProfileView):
setDogs((petsRes || []).map(mapPetResponseToDogType));
// <ProfileDogs dogs={dogs} ... />  // dogs: DogType[]

// AFTER:
setPets(petsRes || []);
// <ProfileDogs pets={pets} ... />  // pets: PetResponse[]
```

### Pattern 3: Image Upload via Presigned URL
**What:** Use `uploadImageFlow` from `api/upload.ts` for pet photos
**When to use:** Pet form photo upload (replacing base64 approach)
**Example:**
```typescript
// Source: api/upload.ts (Phase 2)
import { uploadImageFlow } from '@/api/upload';

const handlePhotoUpload = async (file: File) => {
  try {
    const imageUrl = await uploadImageFlow(file, 'PET_PROFILE');
    setFormData(prev => ({ ...prev, photoUrl: imageUrl }));
  } catch {
    toast.error('사진 업로드에 실패했습니다.');
  }
};
```

### Anti-Patterns to Avoid
- **Using DogType as intermediary:** Don't map PetResponse -> DogType -> render. Use PetResponse directly.
- **Hardcoded breed/personality constants:** Don't use `DOG_BREEDS` or `DOG_PERSONALITIES` from constants. Fetch from backend.
- **Base64 image handling:** Don't use `optimizeImage` + base64 for photos. Use presigned URL upload.
- **Age input field:** DEC-003 forbids age input. Only birthDate date picker allowed.
- **Client-side breed text matching:** Old form used text input + local array filtering. Use breedId dropdown from API.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Breed list | Hardcoded `DOG_BREEDS` constant | `getBreeds()` API | Backend is source of truth; breeds may change |
| Personality options | Hardcoded `DOG_PERSONALITIES` constant | `getPersonalities()` API | Backend owns master data |
| Walk style options | Hardcoded 4-item array | `getWalkingStyles()` API | Backend has real codes |
| Image upload | Base64 + canvas resize | `uploadImageFlow()` from upload.ts | Presigned URL is the established pattern |
| API calls | Direct fetch | `api/pets.ts` functions | Already typed and wrapped with apiClient |

**Key insight:** The entire legacy form system (`useDogForm` + `DogFormFields` + constants) was built before the backend API existed. It uses string breed names, Korean personality strings, and a 4-option walk style selector. The backend uses numeric IDs, code strings, and a separate master data API. The form must be rebuilt around backend types.

## Common Pitfalls

### Pitfall 1: PetResponse.walkingStyles Type Mismatch
**What goes wrong:** Frontend `api/pets.ts` types `walkingStyles` as `WalkingStyleResponse[]` but backend returns `List<String>` (code strings only, e.g., `["LEISURELY", "SOCIAL"]`).
**Why it happens:** Frontend type was written speculatively before verifying backend response shape.
**How to avoid:** Fix the PetResponse interface: `walkingStyles: string[]` to match backend. When rendering walk style names, cross-reference with the master data from `getWalkingStyles()`.
**Warning signs:** Runtime errors accessing `.name` or `.code` on string values.

### Pitfall 2: PetUpdateRequest walkingStyles vs walkingStyleCodes
**What goes wrong:** Backend has both `walkingStyles` (legacy) and `walkingStyleCodes` (preferred) fields in PetUpdateRequest.
**Why it happens:** Backend has a migration path; both fields work, but `walkingStyleCodes` is explicitly documented as "권장 필드" (recommended field).
**How to avoid:** Use `walkingStyleCodes` for update requests. The frontend PetUpdateRequest type already includes both fields.
**Warning signs:** Walking styles not saving on edit.

### Pitfall 3: PetCreateRequest.walkingStyles is string[] (codes)
**What goes wrong:** Sending WalkingStyleResponse objects instead of code strings.
**Why it happens:** Confusion between master data response type and request payload type.
**How to avoid:** `walkingStyles` in create/update requests are `string[]` of codes (e.g., `["LEISURELY"]`), not IDs. `personalityIds` are `number[]` of IDs. Different patterns for different fields.

### Pitfall 4: Edit Form Missing Fields
**What goes wrong:** PetUpdateRequest cannot change gender, size, or breedId. Only name, birthDate, isNeutered, mbti, photoUrl, personalityIds, walkingStyleCodes are updatable.
**Why it happens:** Backend intentionally restricts which fields can be changed after creation.
**How to avoid:** Edit form should show gender/size/breed as read-only or disabled. Only render editable controls for updatable fields.
**Warning signs:** Users expect to change breed but nothing happens.

### Pitfall 5: Main Pet Delete Constraint
**What goes wrong:** Deleting the only main pet or deleting without switching main first.
**Why it happens:** Backend may reject or allow based on validation rules.
**How to avoid:** Follow backend validation (error code handling). If backend rejects with an error, display the backend message via toast. Optionally prevent client-side if pet is main and other pets exist.

### Pitfall 6: Stale Dogs State After Mutations
**What goes wrong:** After create/edit/delete, the pet list doesn't refresh properly.
**Why it happens:** Not calling `fetchData()` after mutation, or calling it but the old closure captures stale state.
**How to avoid:** Use the existing `fetchData` callback pattern from MyProfileView. After any pet mutation, call `fetchData()` to refetch all data.

## Code Examples

### Create Pet with Full Fields
```typescript
// Source: api/pets.ts types + OpenAPI spec
import { createPet } from '@/api/pets';
import { uploadImageFlow } from '@/api/upload';

const handleCreate = async (formData: FormState) => {
  let photoUrl: string | undefined;
  if (formData.photoFile) {
    photoUrl = await uploadImageFlow(formData.photoFile, 'PET_PROFILE');
  }

  await createPet({
    name: formData.name,               // required, 1-10 chars
    breedId: formData.breedId,          // required, number
    birthDate: formData.birthDate,      // required, YYYY-MM-DD
    gender: formData.gender,            // required, "MALE" | "FEMALE"
    size: formData.size,                // required, "SMALL" | "MEDIUM" | "LARGE"
    isNeutered: formData.isNeutered,    // required, boolean
    mbti: formData.mbti || undefined,   // optional, max 4 chars
    photoUrl,                           // optional
    certificationNumber: formData.certificationNumber || undefined,  // optional, max 15 chars
    walkingStyles: formData.selectedWalkingStyleCodes,  // string[] of codes
    personalityIds: formData.selectedPersonalityIds,     // number[] of IDs
  });
};
```

### Update Pet (Partial PATCH)
```typescript
// Source: OpenAPI PetUpdateRequest schema
import { updatePet } from '@/api/pets';

// Only send changed fields; null/undefined = no change
await updatePet(petId, {
  name: nameChanged ? newName : undefined,
  birthDate: birthDateChanged ? newBirthDate : undefined,
  isNeutered: neuteredChanged ? newIsNeutered : undefined,
  mbti: mbtiChanged ? newMbti : undefined,
  photoUrl: photoChanged ? newPhotoUrl : undefined,
  personalityIds: personalitiesChanged ? newPersonalityIds : undefined,
  walkingStyleCodes: walkStylesChanged ? newWalkStyleCodes : undefined,
  // NOTE: gender, size, breedId are NOT updatable
});
```

### Set Main Pet with Optimistic UI
```typescript
// Source: Established Phase 4 optimistic pattern
import { setMainPet } from '@/api/pets';

const handleSetMain = async (petId: number) => {
  // Optimistic update
  setPets(prev => prev.map(p => ({
    ...p,
    isMain: p.id === petId,
  })));

  try {
    await setMainPet(petId);
    toast.success('대표 반려견이 변경되었습니다.');
  } catch {
    // Rollback on failure
    await fetchData();
    toast.error('대표 반려견 변경에 실패했습니다.');
  }
};
```

### Rendering Walk Style Names from Codes
```typescript
// PetResponse.walkingStyles is string[] (codes like "LEISURELY")
// Need to cross-reference with master data for display names
const walkStyleMap = useMemo(
  () => new Map(masterData.walkingStyles.map(ws => [ws.code, ws.name])),
  [masterData.walkingStyles]
);

// In render:
{pet.walkingStyles.map(code => (
  <Badge key={code}>{walkStyleMap.get(code) || code}</Badge>
))}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `DOG_BREEDS` hardcoded constant | `getBreeds()` API | Phase 2 (api layer) | Breed dropdown must use API |
| `DOG_PERSONALITIES` constant | `getPersonalities()` API | Phase 2 (api layer) | Personality selector must use API |
| Hardcoded walk style buttons | `getWalkingStyles()` API | Phase 2 (api layer) | Walk style selector must use API |
| `DogType` model (string IDs, string breed) | `PetResponse` (numeric IDs, BreedResponse) | Phase 2 (api layer) | All pet rendering uses PetResponse |
| Base64 image + canvas resize | Presigned URL upload | Phase 2 (INFRA-05) | Pet photos use uploadImageFlow |
| `useDogForm` hook | Direct useState | Phase 5 (now) | Legacy hook incompatible with API types |

**Deprecated/outdated:**
- `useDogForm` hook: Uses `DOG_BREEDS` constant, string breed names, `DogType` model -- incompatible with backend API
- `DogFormFields` component: Coupled to `useDogForm`, uses `DOG_PERSONALITIES` constant, has age field (violates DEC-003)
- `mapPetResponseToDogType()` in MyProfileView: Unnecessary type conversion layer
- `DogType` in `types/index.ts`: Should not be used for pet management; PetResponse is canonical

## Open Questions

1. **Walking style code format**
   - What we know: Backend returns string codes (e.g., `["LEISURELY"]`), master data endpoint returns `{id, name, code}`
   - What's unclear: Exact code values returned by `/walking-styles`. PetCreateRequest takes `string[]` for walkingStyles.
   - Recommendation: Fetch master data on form mount and use code values. Display names via cross-reference.

2. **10-pet limit enforcement location**
   - What we know: User decision says client-side disable at 10 pets
   - What's unclear: Whether backend also validates (likely yes, but not documented in OpenAPI)
   - Recommendation: Enforce client-side (disable button), also handle backend 400 gracefully

3. **Main pet deletion behavior**
   - What we know: User decision says "follow backend validation"
   - What's unclear: Exact backend error code for deleting a main pet
   - Recommendation: Attempt delete, show backend error message on failure. Optionally warn client-side.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | No test runner configured (frontend) |
| Config file | none |
| Quick run command | `cd aini-inu-frontend && npm run lint` |
| Full suite command | `cd aini-inu-frontend && npm run build` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| PET-01 | Pet registration form submits correctly | manual-only | N/A (no test runner) | N/A |
| PET-02 | Pet edit form pre-populates and patches | manual-only | N/A | N/A |
| PET-03 | Pet deletion with confirmation | manual-only | N/A | N/A |
| PET-04 | Main pet switch updates UI | manual-only | N/A | N/A |
| PET-05 | Breeds dropdown populated from API | manual-only | N/A | N/A |
| PET-06 | Personalities multi-select from API | manual-only | N/A | N/A |
| PET-07 | Walking styles multi-select from API | manual-only | N/A | N/A |
| PET-08 | Name 10-char limit, 10-pet limit enforced | manual-only | N/A | N/A |

### Sampling Rate
- **Per task commit:** `cd aini-inu-frontend && npm run lint && npm run build`
- **Per wave merge:** Same (no test suite)
- **Phase gate:** Lint + build green, manual UAT verification

### Wave 0 Gaps
None -- frontend has no test infrastructure to set up (as documented in CLAUDE.md)

## Sources

### Primary (HIGH confidence)
- OpenAPI spec: `common-docs/openapi/openapi.v1.json` -- PetCreateRequest, PetUpdateRequest, PetResponse, BreedResponse, PersonalityResponse, WalkingStyleResponse, MainPetChangeResponse schemas verified
- Backend source: `PetResponse.java` -- confirmed `walkingStyles` is `List<String>` (codes), not `WalkingStyleResponse[]`
- Existing code: `api/pets.ts`, `SignupPetStep.tsx`, `ProfileDogs.tsx`, `DogRegisterModal.tsx`, `DogDetailModal.tsx`, `useDogForm.ts`, `DogFormFields.tsx` -- all read and analyzed
- CONTEXT.md -- locked decisions from user discussion

### Secondary (MEDIUM confidence)
- Backend validation behavior (10-pet limit, main pet delete constraint) -- inferred from error codes in OpenAPI but not explicitly tested

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- all libraries already in use, no new dependencies
- Architecture: HIGH -- clear pattern from existing code, straightforward rewiring task
- Pitfalls: HIGH -- type mismatch between api/pets.ts and backend confirmed by reading source code

**Research date:** 2026-03-06
**Valid until:** 2026-04-06 (stable -- no external dependency changes expected)
