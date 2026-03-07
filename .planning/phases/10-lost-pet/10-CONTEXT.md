# Phase 10: Lost Pet - Context

**Gathered:** 2026-03-08
**Status:** Ready for planning

<domain>
## Phase Boundary

Rewire the lost-pet frontend to use backend API endpoints (7 endpoints across lost-pets and sightings). Users can report lost pets, submit sightings with image upload, trigger AI analysis to find matches, view candidate results from session snapshots, approve a match to create a direct chat room, and handle analysis failures gracefully. All work is frontend-only (`aini-inu-frontend/`).

</domain>

<decisions>
## Implementation Decisions

### Report Form Design
- Image upload with preview shown before submission (no pre-submit AI analysis)
- Breed: manual text input (no dropdown, no client-side AI detection)
- lastSeenAt: manual datetime picker (no auto-fill)
- lastSeenLocation: DaumPostcode (same pattern as RecruitForm in walk threads)
- Remove `geminiService.ts` dependency from EmergencyReportForm entirely

### Sighting Form Design
- Simpler quick form focused on speed (photo first, then location + memo)
- Fewer fields than lost report — optimized for passersby who spotted a dog
- foundLocation: DaumPostcode
- foundAt: datetime picker
- Image upload with preview

### EMERGENCY Tab Structure
- Sub-tabs within EMERGENCY: "신고/제보 작성" | "내 신고 목록"
- "신고/제보 작성" tab: existing LOST/FOUND toggle preserved + form
- "내 신고 목록" tab: list of user's lost pet reports with status
- Remove "준비 중" overlay from around-me page EMERGENCY tab

### Detail View (내 신고 목록)
- Inline expand on card click — no modal or page navigation for detail
- Expanded card shows: pet info, photo, status, description
- "분석 완료" badge on cards that have completed analysis sessions

### AI Analysis Flow
- Auto-trigger immediately after report creation (POST /lost-pets -> POST /lost-pets/analyze chained)
- Full-screen loading overlay during analysis ("실종 동물 AI 분석 중..." animation)
- On success: automatically open candidate modal with results
- On failure (500 + L500_AI_ANALYZE_FAILED): show clear error message, no session created (DEC-005)
- No manual fallback or retry from failure state — error display only

### Candidate Display
- Candidates shown in modal (separate from inline expand) — max 10 candidates
- Score display: total score prominently + breakdown (similarity/distance/recency) shown smaller
- Retype from `AICandidate` (matchRate) to `LostPetMatchCandidateResponse` (score breakdown + rank)
- Existing `AICandidateList.tsx` grid layout reused in modal context

### Session Re-entry
- Cards with completed analysis show "분석 완료" badge in list
- Clicking badged card: expand + immediately open candidate modal (GET /lost-pets/{id}/match?sessionId=X)
- Fixed candidate order on re-entry (backend guarantees this)

### Match Approval + Chat Connect
- User explicitly approves a match candidate in modal (DEC-006: approval required before chat creation)
- Approve: POST /lost-pets/{id}/match with {sessionId, sightingId} -> returns chatRoomId
- On success: navigate to /chat/{chatRoomId} (Phase 8 direct chat pattern)

### State Coverage
- All views implement 5-state pattern: default/loading/empty/error/success (PRD 8.3)

### Claude's Discretion
- Loading overlay animation details
- Exact card layout and spacing in list/expand views
- Candidate modal sizing and grid columns
- Error message wording for analysis failure
- Sub-tab visual style (pill toggle vs underline tabs)

</decisions>

<specifics>
## Specific Ideas

- DEC-005: Analysis failure returns 500 + L500_AI_ANALYZE_FAILED error code; no manual fallback, no session created on failure
- DEC-006: Chat room created only after explicit user approval of a match candidate
- PRD 8.3: Session expiry triggers re-analysis transition
- Image upload uses presigned URL flow consistent with Phase 9 community posts
- Sighting form should feel quick and lightweight — the reporter is likely on the street and needs to submit fast
- Candidate modal similar to existing AICandidateList grid (1-3 columns responsive)

</specifics>

<code_context>
## Existing Code Insights

### Reusable Assets
- `api/lostPets.ts`: All 7 API functions already typed and wired to apiClient (createLostPet, getLostPets, getLostPet, analyzeLostPet, getMatches, approveMatch, createSighting)
- `EmergencyReportForm.tsx`: Existing form UI with LOST/FOUND toggle, image upload — needs full rewire from geminiService to backend API
- `AICandidateList.tsx`: Existing candidate grid UI — needs retype from AICandidate to LostPetMatchCandidateResponse
- DaumPostcode already used in RecruitForm and around-me location modal
- Presigned URL upload utility from Phase 2 (api/upload or equivalent)
- Phase 8 chat navigation pattern for post-approval redirect

### Established Patterns
- Presigned URL image upload: get URL -> PUT binary -> use returned URL in create request (Phase 9)
- 5-state pattern: loading/empty/error/success states (Phase 2 INFRA-07)
- Inline expand: new pattern for this phase (not used elsewhere, but user preference)
- DaumPostcode modal: already used in around-me page and RecruitForm
- Sub-tab within main tab: new pattern — around-me FIND/RECRUIT/EMERGENCY are top-level, sub-tabs within EMERGENCY is new

### Integration Points
- Around-me page (`app/around-me/page.tsx`): EMERGENCY tab — remove disabled overlay, wire sub-tabs and real functionality
- Chat navigation: `router.push('/chat/{chatRoomId}')` after match approval
- `geminiService.ts` import in EmergencyReportForm must be removed
- AroundMeHeader may need update if EMERGENCY sub-tabs affect header display

</code_context>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 10-lost-pet*
*Context gathered: 2026-03-08*
