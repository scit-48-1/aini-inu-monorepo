# Phase 10: Lost Pet - Context

**Gathered:** 2026-03-08
**Status:** Ready for planning

<domain>
## Phase Boundary

Rewire the lost-pet frontend to use backend API endpoints (7 endpoints across lost-pets and sightings). Users can report lost pets, submit sightings with image upload, trigger AI analysis to find matches, view candidate results from session snapshots, approve a match to create a direct chat room, and handle analysis failures gracefully. All work is frontend-only (`aini-inu-frontend/`).

</domain>

<decisions>
## Implementation Decisions

### Report & Sighting Flow
- Rewire EmergencyReportForm to use `api/lostPets.ts` endpoints instead of client-side `geminiService.ts`
- Lost pet report: `POST /api/v1/lost-pets` with presigned URL image upload (Phase 2/9 pattern)
- Sighting report: `POST /api/v1/sightings` with presigned URL image upload
- Two sub-modes in EMERGENCY tab: "내 아이 실종" (lost report) / "유기견 제보" (sighting) — existing toggle preserved
- Lost pet list/detail: `GET /api/v1/lost-pets`, `GET /api/v1/lost-pets/{id}`

### AI Analysis + Matching UX
- AI analysis: `POST /api/v1/lost-pets/analyze` — backend handles AI, not client-side Gemini
- Candidate results displayed from `LostPetAnalyzeCandidateResponse` (scores: similarity, distance, recency, total + rank)
- Re-entry: `GET /api/v1/lost-pets/{id}/match?sessionId=X` retrieves session snapshot with fixed candidate order
- AICandidateList must be retyped from `AICandidate` (hardcoded matchRate) to `LostPetMatchCandidateResponse` (score breakdown)

### Match Approval + Chat Connect
- User explicitly approves a match candidate (DEC-006: approval required before chat creation)
- Approve: `POST /api/v1/lost-pets/{id}/match` with `{sessionId, sightingId}` — returns `chatRoomId`
- On success: navigate to `/chat/{chatRoomId}` (Phase 8 direct chat pattern)
- Analysis failure (500 + `L500_AI_ANALYZE_FAILED`): show clear error message, no session created (DEC-005)
- No manual/fallback submission on failure — error state only

### Tab Integration
- EMERGENCY tab on around-me page: remove "준비 중" overlay, enable full functionality
- Existing FIND/RECRUIT tabs unchanged

### State Coverage
- All views must implement 5-state pattern: default/loading/empty/error/success (PRD §8.3)

### Claude's Discretion
- Loading skeleton design during AI analysis
- Exact layout/spacing of candidate cards
- Error message wording for analysis failure
- Whether to show score breakdown or just total score on candidate cards

</decisions>

<specifics>
## Specific Ideas

- DEC-005: Analysis failure returns 500 + L500_AI_ANALYZE_FAILED error code; no manual fallback, no session created on failure
- DEC-006: Chat room created only after explicit user approval of a match candidate
- PRD §8.3: Session expiry triggers re-analysis transition
- Image upload uses presigned URL flow consistent with Phase 9 community posts

</specifics>

<code_context>
## Existing Code Insights

### Reusable Assets
- `api/lostPets.ts`: All 7 API functions already typed and wired to apiClient (createLostPet, getLostPets, getLostPet, analyzeLostPet, getMatches, approveMatch, createSighting)
- `EmergencyReportForm.tsx`: Existing form UI with LOST/FOUND toggle, image upload, AI analysis button — needs rewire from geminiService to backend API
- `AICandidateList.tsx`: Existing candidate card grid UI — needs retype from AICandidate to LostPetMatchCandidateResponse
- Presigned URL upload utility from Phase 2 (`api/upload.ts` or equivalent)
- Phase 8 chat navigation pattern for post-approval redirect

### Established Patterns
- Presigned URL image upload: get URL -> PUT binary -> use returned URL in create request (Phase 9)
- 5-state pattern: loading/empty/error/success states with dedicated UI (Phase 2 INFRA-07)
- Optimistic UI not needed here — AI analysis is async server-side, no optimistic patterns
- Error toast from apiClient for standard errors; special handling for L500_AI_ANALYZE_FAILED

### Integration Points
- Around-me page (`app/around-me/page.tsx`): EMERGENCY tab currently disabled behind overlay — remove overlay and wire real functionality
- Chat navigation: `router.push('/chat/{chatRoomId}')` after match approval
- `geminiService.ts` import in EmergencyReportForm must be replaced with `api/lostPets.ts` calls

</code_context>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 10-lost-pet*
*Context gathered: 2026-03-08*
