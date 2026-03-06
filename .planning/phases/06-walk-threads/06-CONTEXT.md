# Phase 6: Walk Threads - Context

**Gathered:** 2026-03-06
**Status:** Ready for planning

<domain>
## Phase Boundary

Rewire all walk thread screens to Phase 2 `api/threads.ts` infrastructure. Users can create, browse, join, and manage walk threads with map-based exploration and proper GPS/chat-type/validation enforcement. Frontend-only modifications (aini-inu-frontend/).

**API endpoints in scope (9 endpoints):**
- `POST /api/v1/threads` (FR-WALK-001, thread create)
- `GET /api/v1/threads` (FR-WALK-001, paginated list — SliceResponse)
- `GET /api/v1/threads/map` (FR-WALK-003, coordinate-based map markers)
- `GET /api/v1/threads/{threadId}` (FR-WALK-001, detail)
- `PATCH /api/v1/threads/{threadId}` (FR-WALK-001, update)
- `DELETE /api/v1/threads/{threadId}` (FR-WALK-001, delete)
- `POST /api/v1/threads/{threadId}/apply` (FR-WALK-002, apply with petIds)
- `DELETE /api/v1/threads/{threadId}/apply` (FR-WALK-002, cancel)
- `GET /api/v1/threads/hotspot` (FR-WALK-003, hotspot aggregation)

**Applicable DEC policies:**
- DEC-008: Non-pet-owners cannot create threads; apply is allowed
- DEC-009: chatType (INDIVIDUAL/GROUP) is required on create
- DEC-012: Apply = immediate participation + chat room entry
- DEC-013: Capacity exceeded = instant reject (first-come)
- DEC-014: Duplicate apply = idempotent success
- DEC-020: 5km radius, manual refresh only, start+60min expiry
- DEC-026: navigator.geolocation.getCurrentPosition() on entry
- DEC-027: Permission denied / timeout / unsupported = Seoul City Hall fallback
- DEC-028: lat/lng decimal precision 6 digits
- DEC-029: No automatic periodic re-fetch
- DEC-030: Seoul City Hall lat=37.566295, lng=126.977945

**PRD constraints:**
- Thread title max 30 chars, intro/description max 500 chars (PRD SS8.1)
- Radar default radius 5km (PRD SS8.1)
- 5-state UI coverage: default/loading/empty/error/success (PRD SS8.3)

**Modification scope:** aini-inu-frontend/ only. Backend and common-docs are read-only.

</domain>

<decisions>
## Implementation Decisions

### Page structure (around-me)
- Keep 3-tab layout: FIND / RECRUIT / EMERGENCY
- EMERGENCY tab stays but is disabled ("Coming soon" / "준비 중" state) — Phase 10 will activate it
- Do NOT delete existing EMERGENCY code; just gate it with a disabled overlay
- Default view: map + sidebar list simultaneously (current layout preserved)
- Desktop: left map + right sidebar list. Mobile: map above + list below

### GPS acquisition (DEC-026, DEC-027, DEC-030)
- On /around-me entry: call `navigator.geolocation.getCurrentPosition()` once
- Success: use coordinates as map center + pass to `/threads/map` endpoint
- Permission denied / timeout / unsupported: fallback to Seoul City Hall (37.566295, 126.977945)
- lat/lng rounded to 6 decimal places (DEC-028)
- No auto-polling / no periodic re-fetch (DEC-029) — manual "재탐색" button only

### Thread list & map data
- FIND tab sidebar: use `GET /threads` (paginated SliceResponse) with load-more
- FIND tab map: use `GET /threads/map` with GPS coordinates + 5km radius
- Both calls triggered on initial GPS acquisition and on manual re-search button click
- Hotspot markers: fire icon or popularity marker on map at hotspot coordinates; click shows region name + count popup
- Thread expiry: start time + 60 minutes = expired; show remaining time badge, hide expired threads

### Thread creation form (RECRUIT tab)
- Full form fields: title (max 30 chars), walkDate, startTime, endTime, description (max 500 chars), chatType, maxParticipants, petIds, location
- chatType: toggle switch, default INDIVIDUAL, toggle to GROUP; tooltip explaining each mode
- Pet selection: card-style multi-select from user's pets (via `api/pets.ts` getMyPets)
- Location: structured LocationRequest (placeName, latitude, longitude, address) — keep DaumPostcode for address search, combine with GPS coords
- Calls `api/threads.ts` createThread with ThreadCreateRequest
- Non-pet-owner block (DEC-008): show info card "반려견을 등록해야 모집글을 작성할 수 있어요" + button navigating to profile for pet registration

### Thread apply flow (DEC-012, DEC-013, DEC-014)
- Apply button sends ThreadApplyRequest with petIds (pet selection UI before apply)
- Success: toast "참여 완료!" with "채팅방 가기" action button → navigates to `/chat/{chatRoomId}`
- Capacity exceeded (DEC-013): backend returns error → toast with clear message "정원이 초과되었습니다"
- Duplicate apply (DEC-014): idempotent — treat as success, show same toast
- Cancel: DELETE endpoint, toast confirmation, refresh thread detail

### Thread edit/delete (owner only)
- Edit: navigate to RECRUIT tab pre-filled with existing data (current pattern)
- Delete: confirmation dialog, then DELETE endpoint, refresh list
- Owner detection: compare authorId with current user's memberId

### 5-state coverage (PRD SS8.3)
- Default: thread list loaded with map markers visible
- Loading: skeleton/spinner while fetching threads and GPS
- Empty: "주변에 산책 스레드가 없어요" prompt with recruit CTA
- Error: fetch failed with retry button
- Success: toast on create/edit/delete/apply success

### Claude's Discretion
- Exact form layout (single-page vs sections)
- Skeleton/spinner design
- Map marker icon design and styling
- Hotspot popup visual design
- Responsive breakpoints for map/list layout
- Thread card design in sidebar list
- Pet selection UI for apply flow (inline vs modal)

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets
- `api/threads.ts`: All 9 endpoint functions fully typed (ThreadCreateRequest, ThreadResponse, ThreadSummaryResponse, ThreadMapResponse, ThreadApplyRequest, ThreadApplyResponse, ThreadHotspotResponse) — direct replacement for old `services/api/threadService.ts`
- `api/pets.ts`: getMyPets for pet selection in create/apply forms
- `components/around-me/`: AroundMeHeader, RadarMapSection, RadarSidebar, RecruitForm — existing layout to rewire
- `components/common/DynamicMap.tsx`: Leaflet map wrapper
- `hooks/useRadarLogic.ts`: Core logic hook — needs complete rewrite (wrong API calls, 10s polling, old types)
- `components/ui/Card.tsx`, `Button.tsx`, `Badge.tsx`: Design primitives
- `lib/utils.ts`: `calculateDistance()`, `getRemainingTimeStr()`, `cn()` utilities

### Established Patterns
- `'use client'` on all pages and components
- Toast-only errors via sonner (Korean messages)
- `cn()` for conditional classNames
- Optimistic UI with failure rollback (Phase 4/5 pattern)
- Phase 2 API modules: inline types, `apiClient` envelope unwrap

### Integration Points
- `src/app/around-me/page.tsx`: Page entry, needs rewiring from old threadService to `api/threads.ts`
- `useRadarLogic.ts`: Complete rewrite — replace polling with manual refresh, use correct API endpoints
- `services/api/threadService.ts`: OLD module to be replaced — all calls should migrate to `api/threads.ts`
- `types/index.ts` ThreadType: Legacy type — components should use ThreadResponse/ThreadSummaryResponse from `api/threads.ts`
- `/chat/{id}` route: Navigation target after successful apply
- `store/useConfigStore.ts`: GPS coordinates state (lastCoordinates, setCoordinates)

</code_context>

<specifics>
## Specific Ideas

- User provided full FR/DEC/DoD specification as acceptance criteria
- GPS: single getCurrentPosition() call on entry, no periodic re-fetch
- Seoul City Hall fallback coordinates explicitly specified: lat=37.566295, lng=126.977945
- Apply response includes chatRoomId — user wants toast with action button to navigate to chat (not auto-redirect)
- Hotspot display: marker icons on map (not card list, not heatmap)
- EMERGENCY tab: keep but disable with "준비 중" state until Phase 10

</specifics>

<deferred>
## Deferred Ideas

- EMERGENCY tab full functionality (lost pet reports, AI matching, sighting) — Phase 10
- Walk diary integration from around-me — Phase 7
- Chat room UI after apply navigation — Phase 8

</deferred>

---

*Phase: 06-walk-threads*
*Context gathered: 2026-03-06*
