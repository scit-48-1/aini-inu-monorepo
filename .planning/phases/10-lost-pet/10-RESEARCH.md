# Phase 10: Lost Pet - Research

**Researched:** 2026-03-08
**Domain:** Frontend rewire -- lost pet report, sighting, AI analysis, match approval, chat connect
**Confidence:** HIGH

## Summary

Phase 10 is a frontend-only phase that rewires the existing EMERGENCY tab on the around-me page to use 7 backend API endpoints across lost-pets and sightings domains. The API layer (`api/lostPets.ts`) is already typed and wired -- all 7 functions exist with correct endpoint paths. The existing UI components (`EmergencyReportForm.tsx`, `AICandidateList.tsx`) need substantial rewiring: the report form must drop its `geminiService` dependency and use presigned URL upload + backend create/analyze chain, while the candidate list must be retyped from `AICandidate` (matchRate percentage) to `LostPetMatchCandidateResponse` (score breakdown with similarity/distance/recency + rank).

The critical flow is: create lost pet report -> auto-trigger AI analysis -> display candidates in modal -> user approves match -> navigate to chat room. Error handling for `L500_AI_ANALYZE_FAILED` uses the established `ApiError.errorCode` pattern. Session re-entry is simplified by the backend's optional `sessionId` parameter on `GET /lost-pets/{id}/match` which defaults to the latest session.

**Primary recommendation:** Structure work as (1) API types verification + forms rewire, (2) list/detail/session re-entry, (3) AI analysis flow + candidate modal + match approval + chat navigation. Keep the existing around-me page structure and add sub-tabs within EMERGENCY.

<user_constraints>

## User Constraints (from CONTEXT.md)

### Locked Decisions
- Image upload with preview shown before submission (no pre-submit AI analysis)
- Breed: manual text input (no dropdown, no client-side AI detection)
- lastSeenAt: manual datetime picker (no auto-fill)
- lastSeenLocation: DaumPostcode (same pattern as RecruitForm in walk threads)
- Remove `geminiService.ts` dependency from EmergencyReportForm entirely
- Sighting form: simpler quick form focused on speed (photo first, then location + memo)
- foundLocation: DaumPostcode; foundAt: datetime picker
- Sub-tabs within EMERGENCY: "신고/제보 작성" | "내 신고 목록"
- "신고/제보 작성" tab: existing LOST/FOUND toggle preserved + form
- "내 신고 목록" tab: list of user's lost pet reports with status
- Remove "준비 중" overlay from around-me page EMERGENCY tab
- Detail view: Inline expand on card click (no modal or page navigation)
- Expanded card shows: pet info, photo, status, description
- "분석 완료" badge on cards that have completed analysis sessions
- AI analysis: Auto-trigger immediately after report creation (POST /lost-pets -> POST /lost-pets/analyze chained)
- Full-screen loading overlay during analysis ("실종 동물 AI 분석 중..." animation)
- On success: automatically open candidate modal with results
- On failure (500 + L500_AI_ANALYZE_FAILED): show clear error message, no session created (DEC-005)
- No manual fallback or retry from failure state -- error display only
- Candidates shown in modal (max 10 candidates)
- Score display: total score prominently + breakdown (similarity/distance/recency) shown smaller
- Retype from AICandidate (matchRate) to LostPetMatchCandidateResponse (score breakdown + rank)
- Existing AICandidateList.tsx grid layout reused in modal context
- Session re-entry: clicking badged card expands + opens candidate modal (GET /lost-pets/{id}/match?sessionId=X)
- Fixed candidate order on re-entry (backend guarantees this)
- Match approval: POST /lost-pets/{id}/match with {sessionId, sightingId} -> returns chatRoomId
- On success: navigate to /chat/{chatRoomId} (Phase 8 direct chat pattern)
- All views implement 5-state pattern: default/loading/empty/error/success (PRD 8.3)
- DEC-005: Analysis failure returns 500 + L500_AI_ANALYZE_FAILED error code
- DEC-006: Chat room created only after explicit user approval of a match candidate

### Claude's Discretion
- Loading overlay animation details
- Exact card layout and spacing in list/expand views
- Candidate modal sizing and grid columns
- Error message wording for analysis failure
- Sub-tab visual style (pill toggle vs underline tabs)

### Deferred Ideas (OUT OF SCOPE)
None -- discussion stayed within phase scope

</user_constraints>

<phase_requirements>

## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| LOST-01 | 실종 등록 (FR-LOST-001, `LOST-CREATE`) | `createLostPet` API exists; presigned URL upload pattern from Phase 9; DaumPostcode for location; datetime picker for lastSeenAt |
| LOST-02 | 제보 등록 (FR-LOST-001, `SIGHTING-CREATE`) | `createSighting` API exists; simpler form with photo + location + memo; same presigned upload + DaumPostcode |
| LOST-03 | AI 분석 -- 유사 제보 후보 세션 스냅샷 조회 (FR-LOST-002, `LOST-ANALYZE`) | `analyzeLostPet` API chains after create; returns sessionId + candidates; auto-trigger pattern |
| LOST-04 | 매칭 조회 -- 재진입 순서 고정 (FR-LOST-002, `LOST-MATCH`) | `getMatches` API with optional sessionId (defaults to latest); backend guarantees rank order; candidate modal |
| LOST-05 | 매칭 승인 -> 채팅 연결 (FR-LOST-003/DEC-006, `LOST-MATCH-APPROVE`) | `approveMatch` returns chatRoomId; navigate to /chat/{chatRoomId}; Phase 8 direct chat pattern |
| LOST-06 | 분석 실패 처리 -- L500_AI_ANALYZE_FAILED (FR-LOST-004/DEC-005) | ApiError.errorCode check for 'L500_AI_ANALYZE_FAILED'; suppress default toast, show custom error UI |
| LOST-07 | 긴급 제보 UI -- around-me 제보 탭 (PRD 8.3) | Remove overlay; add sub-tabs; wire EMERGENCY tab to full lost pet flow |

</phase_requirements>

## Standard Stack

### Core (Already in project)
| Library | Purpose | Why Standard |
|---------|---------|--------------|
| `api/lostPets.ts` | All 7 API functions typed and ready | Phase 2 infrastructure |
| `api/upload.ts` (`uploadImageFlow`) | Presigned URL image upload | Phase 2 INFRA-05 |
| `react-daum-postcode` | Address picker for locations | Already used in RecruitForm |
| `sonner` (toast) | Error/success notifications | Project standard |
| `lucide-react` | Icons | Project standard |
| `@/api/types` (`ApiError`, `AsyncState`) | Error handling + state types | Phase 2 INFRA-03/07 |

### No New Dependencies
This phase requires zero new npm packages. Everything is already in the project.

## Architecture Patterns

### Recommended Component Structure
```
src/
  components/around-me/
    EmergencyReportForm.tsx     # REWIRE: lost pet + sighting create forms (LOST/FOUND toggle)
    AICandidateList.tsx         # REWIRE: retype to LostPetMatchCandidateResponse
    LostPetListPanel.tsx        # NEW: "내 신고 목록" sub-tab
    LostPetCandidateModal.tsx   # NEW: modal wrapper for candidate display + approval
    EmergencySubTabs.tsx        # NEW: sub-tab controller within EMERGENCY
  app/around-me/page.tsx        # MODIFY: remove overlay, wire sub-tabs
```

### Pattern 1: Chained API Flow (Create -> Analyze)
**What:** Auto-trigger analysis immediately after report creation
**When to use:** LOST-01 + LOST-03 chained flow
**Example:**
```typescript
// Create report, then auto-analyze
const handleCreateAndAnalyze = async (formData: LostPetCreateRequest) => {
  setAnalysisState('loading'); // full-screen overlay
  try {
    const report = await createLostPet(formData);
    const analyzeResult = await analyzeLostPet({
      lostPetId: report.lostPetId,
      imageUrl: formData.photoUrl,
    });
    setSessionId(analyzeResult.sessionId);
    setCandidates(analyzeResult.candidates);
    setAnalysisState('success');
    setShowCandidateModal(true);
  } catch (err) {
    if (err instanceof ApiError && err.errorCode === 'L500_AI_ANALYZE_FAILED') {
      setAnalysisState('error');
      // Show analysis-specific error UI, no session created
    } else {
      throw err; // let global handler deal with other errors
    }
  }
};
```

### Pattern 2: Presigned URL Upload (File -> URL)
**What:** Upload image via presigned URL before form submission
**When to use:** Both lost pet report and sighting creation
**Example:**
```typescript
// Same pattern as usePostForm in Phase 9
const imageUrl = await uploadImageFlow(file, 'LOST_PET');
// Then use imageUrl in create request
await createLostPet({ ...formData, photoUrl: imageUrl });
```

### Pattern 3: Session Re-entry
**What:** Re-open candidate modal for reports with completed analysis
**When to use:** LOST-04 -- clicking a "분석 완료" badged card
**Example:**
```typescript
// Backend defaults to latest session when sessionId omitted
const candidates = await getMatches(lostPetId, { sessionId: knownSessionId });
// candidates come pre-sorted by rank (backend guaranteed)
```

### Pattern 4: Error Code-Specific Handling
**What:** Catch ApiError with specific errorCode to show custom UI instead of default toast
**When to use:** LOST-06 -- L500_AI_ANALYZE_FAILED
**Example:**
```typescript
try {
  const result = await analyzeLostPet(request);
} catch (err) {
  if (err instanceof ApiError && err.errorCode === 'L500_AI_ANALYZE_FAILED') {
    // suppressToast is NOT needed -- apiClient already showed toast
    // But we need custom UI state for the full-screen overlay
    setAnalysisError('AI 분석에 실패했습니다. 잠시 후 다시 시도해주세요.');
  }
}
```

### Pattern 5: Inline Expand Card
**What:** Card click expands to show detail inline (no modal/navigation)
**When to use:** "내 신고 목록" list
**Example:**
```typescript
// Track expanded card ID
const [expandedId, setExpandedId] = useState<number | null>(null);
// Toggle on click
const handleCardClick = (id: number) => {
  setExpandedId(prev => prev === id ? null : id);
};
```

### Anti-Patterns to Avoid
- **Don't use geminiService:** The entire `analyzeDogImage` import chain must be removed from EmergencyReportForm. All AI analysis goes through backend `POST /lost-pets/analyze`.
- **Don't use base64 images:** Old form used FileReader -> base64. New flow uses File -> presigned URL upload -> URL string.
- **Don't store candidates in global state:** Candidates are session-scoped. Keep them in local component state within the modal/analysis flow.
- **Don't manually sort candidates:** Backend guarantees rank order via `rankOrder ASC`. Never re-sort on frontend.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Image upload | base64 FileReader + custom upload | `uploadImageFlow(file, purpose)` from `api/upload.ts` | Handles presigned URL flow, token extraction, error handling |
| Address input | Custom address form | `DaumPostcode` from `react-daum-postcode` | Already used in RecruitForm, reliable Korean address resolution |
| Error code matching | Manual HTTP status parsing | `ApiError.errorCode` from `api/types.ts` | apiClient already parses envelope and creates typed ApiError |
| Pagination | Custom offset tracking | `SliceResponse<T>` + `PaginationParams` | Established pattern from Phase 2 |

## Common Pitfalls

### Pitfall 1: Frontend Type Mismatches with Backend DTOs
**What goes wrong:** Frontend `LostPetCreateRequest.lastSeenAt` is typed as `string` but backend expects `LocalDateTime` (ISO 8601 format)
**Why it happens:** Frontend types in `api/lostPets.ts` were generated from Swagger but may use `string` for datetime fields
**How to avoid:** Send ISO 8601 strings (e.g., `2026-03-08T14:30:00`) -- Jackson auto-deserializes these to `LocalDateTime`. The frontend type stays `string` but values must be ISO format.
**Warning signs:** 400 errors on create with date parsing messages

### Pitfall 2: Score Types are BigDecimal on Backend
**What goes wrong:** Backend scores (`scoreSimilarity`, `scoreDistance`, etc.) are `BigDecimal`, serialized as numbers with decimal precision. Frontend types use `number` which is correct but display may show excessive decimals.
**How to avoid:** Use `toFixed(1)` or similar for display formatting of score values.

### Pitfall 3: Analyze Request Requires image OR imageUrl
**What goes wrong:** Backend validates `@AssertTrue isImageProvided()` -- at least one of `image` or `imageUrl` must be non-blank.
**Why it happens:** `image` is for base64, `imageUrl` is for URL. In the presigned URL flow, use `imageUrl` only.
**How to avoid:** After `uploadImageFlow`, pass `{ lostPetId, imageUrl: uploadedUrl }` to `analyzeLostPet`. Don't set `image` field.

### Pitfall 4: sessionId Optional in getMatches
**What goes wrong:** Frontend `getMatches` currently requires `sessionId` in params, but backend allows it to be optional (defaults to latest session).
**Why it happens:** Frontend type constraint is stricter than backend.
**How to avoid:** For re-entry, you can pass `sessionId` from the analyze response. For safety, also support omitting it. The API type may need `sessionId` made optional.

### Pitfall 5: Match Approval Requires Authorization Header
**What goes wrong:** Backend `approveMatch` controller takes `@RequestHeader("Authorization")` and passes it to `LostPetMatchApprovalService` for internal chat room creation.
**Why it happens:** The approval flow internally creates a chat room via HTTP call to chat service, forwarding the auth header.
**How to avoid:** The apiClient already injects the Authorization header automatically. No special handling needed. But be aware that if auth expires between analysis and approval, the approval may fail with 401.

### Pitfall 6: EmergencyReportForm Props Interface Change
**What goes wrong:** Current `EmergencyReportForm` expects `{ isSubmitting, isSuccess, onSubmit, optimizeImage }` props that are tightly coupled to the old gemini-based flow.
**Why it happens:** Complete prop interface redesign is needed.
**How to avoid:** Redesign the component interface from scratch. The new form needs: image file handling (not base64), DaumPostcode integration, datetime picker, breed text input, and backend API submission. Consider whether to keep the component name or create fresh.

### Pitfall 7: "분석 완료" Badge Detection
**What goes wrong:** `LostPetSummaryResponse` does NOT include `sessionId` or any analysis status indicator.
**Why it happens:** The summary endpoint only returns `{ lostPetId, petName, status, lastSeenAt }`.
**How to avoid:** Two options: (1) Fetch detail + try `getMatches` for each report to check session existence (expensive), or (2) Track analyzed report IDs locally after successful analysis in current session. Option 2 is simpler but loses state on refresh. Consider storing analyzed IDs in component state and re-checking via `getMatches` (without sessionId) when expanding a card -- if it returns candidates, show badge.

## Code Examples

### Image Upload + Create Lost Pet
```typescript
// Source: established pattern from usePostForm + api/upload.ts
const handleSubmitLostPet = async (file: File, formFields: Omit<LostPetCreateRequest, 'photoUrl'>) => {
  setIsSubmitting(true);
  try {
    const photoUrl = await uploadImageFlow(file, 'LOST_PET');
    const report = await createLostPet({ ...formFields, photoUrl });
    return report;
  } finally {
    setIsSubmitting(false);
  }
};
```

### Create Sighting (Quick Form)
```typescript
// Source: api/lostPets.ts + api/upload.ts
const handleSubmitSighting = async (file: File, foundAt: string, foundLocation: string, memo?: string) => {
  const photoUrl = await uploadImageFlow(file, 'SIGHTING');
  const sighting = await createSighting({ photoUrl, foundAt, foundLocation, memo });
  toast.success('제보가 등록되었습니다!');
  return sighting;
};
```

### Match Approval -> Chat Navigation
```typescript
// Source: api/lostPets.ts + Phase 8 chat navigation pattern
const handleApproveMatch = async (
  lostPetId: number,
  sessionId: number,
  sightingId: number,
  router: ReturnType<typeof useRouter>,
) => {
  const result = await approveMatch(lostPetId, { sessionId, sightingId });
  if (result.chatRoomId) {
    router.push(`/chat/${result.chatRoomId}`);
  }
};
```

### Error Code Check for Analysis Failure
```typescript
// Source: api/types.ts ApiError pattern (see SignupAccountStep.tsx)
import { ApiError } from '@/api/types';

catch (err) {
  if (err instanceof ApiError && err.errorCode === 'L500_AI_ANALYZE_FAILED') {
    setAnalysisState('error');
    setErrorMessage('AI 분석에 실패했습니다.');
    // No retry button per DEC-005
  }
}
```

## State of the Art

| Old Approach (Current Code) | New Approach (Phase 10) | Impact |
|------------------------------|--------------------------|--------|
| `geminiService.analyzeDogImage()` client-side AI | `analyzeLostPet()` backend AI via Spring AI + Gemini | Remove entire client AI dependency |
| FileReader base64 image | `uploadImageFlow(file, purpose)` presigned URL | Standard upload pattern |
| `AICandidate` with `matchRate: number` | `LostPetMatchCandidateResponse` with score breakdown | Richer match information |
| "준비 중" overlay on EMERGENCY tab | Full sub-tab UI with forms + list | Feature activation |
| `onSubmit(image, result, memo, mode)` callback | Direct API calls within component | Simpler prop interface |

**Deprecated/outdated:**
- `geminiService.ts` import in EmergencyReportForm: must be removed entirely
- `AICandidate` interface in AICandidateList: replaced by `LostPetMatchCandidateResponse` / `LostPetAnalyzeCandidateResponse`
- `DOG_BREEDS` constant import in EmergencyReportForm: breed is now manual text input, no suggestion dropdown needed
- `optimizeImage` prop: no longer needed with presigned URL flow

## Open Questions

1. **"분석 완료" badge detection without summary-level session info**
   - What we know: `LostPetSummaryResponse` has no session/analysis fields. Backend has `GET /lost-pets/{id}/match` that returns latest session candidates.
   - What's unclear: Whether we should make N+1 calls to detect analysis status, or track locally.
   - Recommendation: Track sessionId locally after analysis. On list mount, optionally check via `getMatches(id, {})` for the first few items. If the list is small (user's own reports), this is acceptable.

2. **Sighting form `purpose` value for presigned upload**
   - What we know: Backend presigned URL accepts a `purpose` string field. Community posts use `'POST'`.
   - What's unclear: Exact purpose string for lost pet vs sighting uploads.
   - Recommendation: Use `'LOST_PET'` for reports and `'SIGHTING'` for sighting uploads. If backend rejects, check allowed purpose enum values.

3. **LostPetDetailResponse missing `description` and `breed`**
   - What we know: `LostPetCreateRequest` has `description` and `breed` fields, but `LostPetDetailResponse` only returns `petName`, `photoUrl`, `lastSeenAt`, `lastSeenLocation`, `status`.
   - What's unclear: Whether the expanded card can show breed/description.
   - Recommendation: Show only the fields available in DetailResponse. The expanded card shows: petName, photoUrl, status, lastSeenAt, lastSeenLocation. Breed and description are submitted but not retrievable via detail endpoint.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | None (frontend has no test runner) |
| Config file | N/A |
| Quick run command | `cd aini-inu-frontend && npm run lint` |
| Full suite command | `cd aini-inu-frontend && npm run build` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| LOST-01 | Lost pet report creation | manual-only | `npm run build` (compile check) | N/A |
| LOST-02 | Sighting creation | manual-only | `npm run build` (compile check) | N/A |
| LOST-03 | AI analysis chain | manual-only | `npm run build` (compile check) | N/A |
| LOST-04 | Match query re-entry | manual-only | `npm run build` (compile check) | N/A |
| LOST-05 | Match approval + chat | manual-only | `npm run build` (compile check) | N/A |
| LOST-06 | Analysis failure handling | manual-only | `npm run build` (compile check) | N/A |
| LOST-07 | EMERGENCY tab UI | manual-only | `npm run build` (compile check) | N/A |

### Sampling Rate
- **Per task commit:** `cd aini-inu-frontend && npm run lint && npm run build`
- **Per wave merge:** `cd aini-inu-frontend && npm run build`
- **Phase gate:** Build green + lint clean

### Wave 0 Gaps
None -- frontend validation is lint + build only (no test runner configured per CLAUDE.md).

## Sources

### Primary (HIGH confidence)
- Backend DTOs: All 12 Java record/class files in `aini-inu-backend/src/main/java/scit/ainiinu/lostpet/dto/` -- verified field names, types, validation annotations
- Backend Controllers: `LostPetController.java` (5 endpoints) + `SightingController.java` (1 endpoint) -- verified HTTP methods, paths, parameters
- Frontend API: `api/lostPets.ts` -- verified all 7 functions match backend endpoints
- Frontend patterns: `api/upload.ts`, `hooks/forms/usePostForm.ts`, `api/client.ts` -- established presigned URL, error handling, toast patterns
- Existing UI: `EmergencyReportForm.tsx`, `AICandidateList.tsx`, `around-me/page.tsx` -- current state verified

### Secondary (MEDIUM confidence)
- Error code `L500_AI_ANALYZE_FAILED`: verified in `LostPetErrorCode.java` and integration tests
- Session re-entry: verified `sessionId` is optional in `LostPetController.matchCandidates` and `LostPetMatchQueryService.findCandidates`

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- all libraries already in project, no new dependencies
- Architecture: HIGH -- patterns are direct extensions of Phase 9 (community feed) and Phase 6 (walk threads) with DaumPostcode
- Pitfalls: HIGH -- verified all backend DTOs against frontend types, identified 7 specific pitfalls with mitigations

**Research date:** 2026-03-08
**Valid until:** 2026-04-08 (stable -- frontend-only phase with known backend API)
