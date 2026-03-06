# Phase 6: Walk Threads - Research

**Researched:** 2026-03-06
**Domain:** Frontend rewiring -- walk thread CRUD, map exploration, GPS acquisition, apply flow
**Confidence:** HIGH

## Summary

Phase 6 is a frontend-only rewire of the around-me page (`/around-me`) to use the Phase 2 `api/threads.ts` module instead of the legacy `services/api/threadService.ts`. The existing codebase has a complete but misaligned implementation: `useRadarLogic.ts` uses wrong API calls (old threadService), wrong types (ThreadType from `types/index.ts`), 10-second polling (violates DEC-029), and missing features (no GPS acquisition, no chatType, no apply-with-pets flow, no hotspot markers).

The `api/threads.ts` module is already fully typed and matches the OpenAPI spec. All 9 endpoints are implemented. The work is component rewiring and logic rewriting -- no new API functions needed. Key challenges: (1) replacing ThreadType with ThreadSummaryResponse/ThreadMapResponse throughout components, (2) implementing GPS acquisition with Seoul City Hall fallback, (3) adding chatType toggle and pet selection to the create form, (4) building the apply-with-pets flow that returns chatRoomId for navigation.

**Primary recommendation:** Rewrite `useRadarLogic.ts` from scratch as a clean hook using `api/threads.ts` types, then rewire each component (RecruitForm, RadarSidebar, RadarMapSection) to consume the new types. Do NOT attempt incremental patching of the existing hook -- the type mismatch is too deep.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- Keep 3-tab layout: FIND / RECRUIT / EMERGENCY
- EMERGENCY tab stays but is disabled ("Coming soon" / "준비 중" state) -- Phase 10 will activate it
- Do NOT delete existing EMERGENCY code; just gate it with a disabled overlay
- Default view: map + sidebar list simultaneously (current layout preserved)
- Desktop: left map + right sidebar list. Mobile: map above + list below
- On /around-me entry: call `navigator.geolocation.getCurrentPosition()` once
- Success: use coordinates as map center + pass to `/threads/map` endpoint
- Permission denied / timeout / unsupported: fallback to Seoul City Hall (37.566295, 126.977945)
- lat/lng rounded to 6 decimal places (DEC-028)
- No auto-polling / no periodic re-fetch (DEC-029) -- manual "재탐색" button only
- FIND tab sidebar: use `GET /threads` (paginated SliceResponse) with load-more
- FIND tab map: use `GET /threads/map` with GPS coordinates + 5km radius
- Both calls triggered on initial GPS acquisition and on manual re-search button click
- Hotspot markers: fire icon or popularity marker on map at hotspot coordinates; click shows region name + count popup
- Thread expiry: start time + 60 minutes = expired; show remaining time badge, hide expired threads
- Full form fields: title (max 30 chars), walkDate, startTime, endTime, description (max 500 chars), chatType, maxParticipants, petIds, location
- chatType: toggle switch, default INDIVIDUAL, toggle to GROUP; tooltip explaining each mode
- Pet selection: card-style multi-select from user's pets (via `api/pets.ts` getMyPets)
- Location: structured LocationRequest (placeName, latitude, longitude, address) -- keep DaumPostcode for address search, combine with GPS coords
- Non-pet-owner block (DEC-008): show info card with button navigating to profile for pet registration
- Apply button sends ThreadApplyRequest with petIds (pet selection UI before apply)
- Success: toast with "채팅방 가기" action button navigating to `/chat/{chatRoomId}`
- Capacity exceeded (DEC-013): backend returns error, toast "정원이 초과되었습니다"
- Duplicate apply (DEC-014): idempotent, treat as success
- Cancel: DELETE endpoint, toast confirmation, refresh thread detail
- Edit: navigate to RECRUIT tab pre-filled with existing data
- Delete: confirmation dialog, then DELETE endpoint, refresh list
- Owner detection: compare authorId with current user's memberId
- 5-state UI coverage: default/loading/empty/error/success

### Claude's Discretion
- Exact form layout (single-page vs sections)
- Skeleton/spinner design
- Map marker icon design and styling
- Hotspot popup visual design
- Responsive breakpoints for map/list layout
- Thread card design in sidebar list
- Pet selection UI for apply flow (inline vs modal)

### Deferred Ideas (OUT OF SCOPE)
- EMERGENCY tab full functionality (lost pet reports, AI matching, sighting) -- Phase 10
- Walk diary integration from around-me -- Phase 7
- Chat room UI after apply navigation -- Phase 8
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| WALK-01 | Thread create -- non-pet-owner blocked | RecruitForm rewire with DEC-008 pet check via getMyPets; ThreadCreateRequest matches OpenAPI |
| WALK-02 | Thread update | ThreadPatchRequest via updateThread; pre-fill form with getThread detail |
| WALK-03 | Thread delete | deleteThread with confirmation dialog; owner detection via authorId |
| WALK-04 | Thread list | getThreads returns SliceResponse<ThreadSummaryResponse> with load-more pagination |
| WALK-05 | Thread detail | getThread returns ThreadResponse; used in pin popup and apply flow |
| WALK-06 | Thread apply -- immediate entry, capacity reject, idempotent duplicate | applyToThread returns ThreadApplyResponse with chatRoomId; 409 = capacity exceeded |
| WALK-07 | Thread apply cancel | cancelApplication DELETE endpoint; refresh detail after |
| WALK-08 | Map exploration | getThreadMap with latitude/longitude/radius params; Leaflet markers from ThreadMapResponse |
| WALK-09 | Hotspot display | getHotspots returns region+count; render as fire-icon markers on map |
| WALK-10 | chatType required on create | ThreadCreateRequest.chatType enum INDIVIDUAL/GROUP; toggle in form |
| WALK-11 | GPS auto-acquire on entry, Seoul City Hall fallback | navigator.geolocation.getCurrentPosition; fallback 37.566295, 126.977945 |
| WALK-12 | Manual re-search only, no auto-polling | Remove 10s polling from useRadarLogic; add "재탐색" button |
| WALK-13 | Title max 30, intro max 500, 60min expiry | Form validation + getRemainingTimeStr for expiry badge |
| WALK-14 | Required fields: title, time, participating pets | Form validation: disabled submit until title + startTime + petIds.length > 0 |
</phase_requirements>

## Standard Stack

### Core (already installed)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Next.js | 16.1.6 | App Router, page routing | Project framework |
| React | 19.2.3 | UI components | Project framework |
| react-leaflet | ^5.0.0 | Map rendering (DynamicMap wrapper) | Already used in RadarMapSection |
| leaflet | ^1.9.4 | Map engine | Underlying map library |
| zustand | ^5.0.11 | GPS coordinates state (useConfigStore) | Project state management |
| sonner | ^2.0.7 | Toast notifications (Korean messages) | Project toast library |
| lucide-react | ^0.563.0 | Icons | Project icon library |
| react-daum-postcode | ^3.2.0 | Address search for location input | Already used in AroundMePage |
| tailwindcss | ^4.1.18 | Styling | Project CSS framework |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `api/threads.ts` | internal | 9 endpoint functions + types | All thread API calls |
| `api/pets.ts` | internal | getMyPets for pet selection | Create form and apply flow |
| `lib/utils.ts` | internal | calculateDistance, getRemainingTimeStr, cn | Distance calc, time display, classnames |

### No Additional Packages Needed
All required libraries are already installed. No new npm installs required for this phase.

## Architecture Patterns

### Recommended File Structure
```
src/
├── hooks/
│   └── useRadarLogic.ts          # COMPLETE REWRITE -- new hook using api/threads.ts types
├── app/
│   └── around-me/
│       └── page.tsx              # Rewire to new useRadarLogic, add GPS acquisition
├── components/
│   └── around-me/
│       ├── AroundMeHeader.tsx    # Minor: EMERGENCY tab disabled overlay
│       ├── RadarMapSection.tsx   # Rewire: ThreadMapResponse markers + hotspot markers
│       ├── RadarSidebar.tsx      # Rewire: ThreadSummaryResponse cards + load-more
│       ├── RecruitForm.tsx       # Major rewrite: full form with chatType, location, validation
│       ├── EmergencyReportForm.tsx  # KEEP AS-IS (gated behind disabled overlay)
│       └── AICandidateList.tsx      # KEEP AS-IS (gated behind disabled overlay)
```

### Pattern 1: GPS Acquisition on Mount
**What:** Single `navigator.geolocation.getCurrentPosition()` call on page entry
**When to use:** On /around-me page mount, before any API calls
**Example:**
```typescript
// In useRadarLogic or AroundMePage
useEffect(() => {
  if (!navigator.geolocation) {
    // Fallback: Seoul City Hall
    setCoordinates([37.566295, 126.977945]);
    return;
  }
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      const lat = Number(pos.coords.latitude.toFixed(6));
      const lng = Number(pos.coords.longitude.toFixed(6));
      setCoordinates([lat, lng]);
    },
    () => {
      // Permission denied / timeout / unsupported
      setCoordinates([37.566295, 126.977945]);
    },
    { timeout: 10000, enableHighAccuracy: false }
  );
}, []); // Run once on mount
```

### Pattern 2: Dual Data Fetch (List + Map)
**What:** Fire both `getThreads` (paginated list) and `getThreadMap` (coordinate markers) simultaneously
**When to use:** After GPS coordinates acquired and on manual re-search
**Example:**
```typescript
const fetchThreadData = useCallback(async (lat: number, lng: number) => {
  try {
    const [listData, mapData, hotspots] = await Promise.all([
      getThreads({ page: 0, size: 20 }),
      getThreadMap({ latitude: lat, longitude: lng, radius: 5 }),
      getHotspots(),
    ]);
    setThreadList(listData);
    setMapMarkers(mapData);
    setHotspotData(hotspots);
  } catch (err) {
    toast.error('스레드를 불러오는데 실패했습니다.');
  }
}, []);
```

### Pattern 3: Apply Flow with Chat Navigation
**What:** Apply returns chatRoomId; show toast with action button to navigate
**When to use:** After successful thread apply
**Example:**
```typescript
const handleApply = async (threadId: number, petIds: number[]) => {
  try {
    const result = await applyToThread(threadId, { petIds });
    toast.success('참여 완료!', {
      action: {
        label: '채팅방 가기',
        onClick: () => router.push(`/chat/${result.chatRoomId}`),
      },
    });
    // Refresh thread detail to update participant count
    await refreshThreadDetail(threadId);
  } catch (err) {
    if (err instanceof ApiError) {
      // Capacity exceeded (409 or specific errorCode)
      toast.error('정원이 초과되었습니다');
    } else {
      toast.error('참여 신청에 실패했습니다.');
    }
  }
};
```

### Pattern 4: Owner Detection
**What:** Compare thread's `authorId` with current user's `memberId` from auth store
**When to use:** Conditionally showing edit/delete buttons
**Example:**
```typescript
// Source: useUserStore from Phase 3
import { useUserStore } from '@/store/useUserStore';
const user = useUserStore((s) => s.user);
const isOwner = thread.authorId === user?.id;
```

### Anti-Patterns to Avoid
- **Polling:** Old useRadarLogic polls every 10s. Must be completely removed (DEC-029). Only manual "재탐색" button triggers refetch.
- **Legacy ThreadType:** Do NOT use `ThreadType` from `types/index.ts`. Use `ThreadSummaryResponse`, `ThreadMapResponse`, `ThreadResponse` from `api/threads.ts`.
- **Old threadService:** Do NOT import from `services/api/threadService.ts`. All calls go through `api/threads.ts`.
- **String IDs:** Old ThreadType uses `id: string`. New API types use `id: number`. All ID comparisons must use numbers.
- **Mixing list/map data:** `getThreads` returns paginated list for sidebar. `getThreadMap` returns coordinate-only data for markers. They serve different purposes and should not be conflated.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Distance calculation | Custom haversine | `calculateDistance()` from `lib/utils.ts` | Already tested and working |
| Remaining time display | Custom time formatter | `getRemainingTimeStr()` from `lib/utils.ts` | Already handles edge cases |
| Map rendering | Raw Leaflet | `DynamicMap` component (next/dynamic SSR-safe) | Already handles SSR, markers, circles |
| Address search | Custom geocoding | `react-daum-postcode` | Already integrated in page |
| API envelope unwrapping | Manual parsing | `apiClient` from `api/client.ts` | Handles ApiResponse unwrap, error codes |
| Toast notifications | Custom toast | `sonner` with Korean messages | Project standard |
| Conditional classnames | Manual string concat | `cn()` from `lib/utils.ts` | Project standard |

## Common Pitfalls

### Pitfall 1: ThreadSummaryResponse lacks walkDate field
**What goes wrong:** ThreadSummaryResponse (list endpoint) has `startTime` and `endTime` but NO `walkDate`. ThreadResponse (detail endpoint) has `walkDate`.
**Why it happens:** List view is a summary; full fields only in detail.
**How to avoid:** For expiry calculation in list view, use `startTime` (which is a full date-time string per OpenAPI `format: date-time`). The existing `getRemainingTimeStr` expects `HH:mm` -- need to parse ISO date-time to extract time portion.
**Warning signs:** "시간 협의" showing for all threads means time parsing failed.

### Pitfall 2: startTime format mismatch
**What goes wrong:** OpenAPI spec shows `startTime` as `format: date-time` (ISO 8601: `2026-03-06T14:00:00`), but old code expects `HH:mm` string. `getRemainingTimeStr` splits on `:` expecting simple time.
**Why it happens:** Backend returns full ISO datetime, old code assumed time-only string.
**How to avoid:** Parse `startTime` as a Date object or extract time portion before passing to existing utils. Consider writing a wrapper or updating `getRemainingTimeStr` to handle both formats.
**Warning signs:** NaN in time display, or incorrect remaining time calculations.

### Pitfall 3: DynamicMap expects MapMarker type
**What goes wrong:** `DynamicMap` component accepts `markers: MapMarker[]` where `MapMarker` has `{ id: string, lat?: number, lng?: number, image?, thumbnail?, isEmergency? }`. ThreadMapResponse has `{ threadId: number, latitude: number, longitude: number }` -- different field names and types.
**Why it happens:** DynamicMap was built for legacy types.
**How to avoid:** Create a mapper function: `ThreadMapResponse -> MapMarker`. Map `threadId` -> `id` (as string), `latitude` -> `lat`, `longitude` -> `lng`. Or update DynamicMap to accept a generic marker type.
**Warning signs:** Empty map with no markers despite API returning data.

### Pitfall 4: Hotspot markers have no coordinates
**What goes wrong:** `ThreadHotspotResponse` has `{ region: string, count: number }` -- NO latitude/longitude. Cannot place markers on map without coordinates.
**Why it happens:** Hotspot endpoint returns aggregated region names, not coordinates.
**How to avoid:** Hotspot display will need to be region-name-based (popup/card list near map) rather than coordinate-based markers. OR use a known mapping of region names to coordinates. This is a design decision -- the CONTEXT.md says "marker icons on map" but the API does not provide coordinates.
**Warning signs:** Attempting to render hotspot markers and getting undefined lat/lng.

### Pitfall 5: Non-pet-owner detection timing
**What goes wrong:** Need to check if user has pets before showing create form, but `getMyPets()` is an async call.
**Why it happens:** Pet ownership must be determined before rendering the RECRUIT tab content.
**How to avoid:** Fetch pets on hook mount (along with GPS). Cache in hook state. If `pets.length === 0`, show the DEC-008 block message instead of the form.
**Warning signs:** Flash of create form before the block message appears.

### Pitfall 6: Thread expiry logic -- 60 minutes from startTime
**What goes wrong:** DEC-020 says threads expire after `startTime + 60 minutes`. Old code uses a clock interval to check. Without polling, the UI won't auto-update expiry state.
**Why it happens:** No periodic re-fetch means expired threads stay visible until manual refresh.
**How to avoid:** Use a local timer (setInterval every 60s) that only updates the `currentTime` state for expiry badge rendering -- this is display-only, not a data fetch. Filter expired threads client-side.
**Warning signs:** Expired threads showing "0분 전 시작" indefinitely.

### Pitfall 7: SliceResponse load-more in sidebar
**What goes wrong:** `getThreads` returns `SliceResponse<ThreadSummaryResponse>` with `hasNext` boolean. Need to implement load-more button that appends next page.
**Why it happens:** Old code fetched all threads at once. New API is paginated.
**How to avoid:** Track `page` state. On load-more click, fetch `page + 1`, append `content` to existing list. Disable button when `hasNext === false` or `last === true`.
**Warning signs:** Duplicate threads appearing or missing threads after page change.

## Code Examples

### Thread Create Request (verified against OpenAPI)
```typescript
// Source: OpenAPI spec ThreadCreateRequest
const createData: ThreadCreateRequest = {
  title: '오후 공원 산책 같이 하실 분!',        // required, max 30 chars
  description: '성수동 서울숲 근처에서 산책해요', // required, max 500 chars
  walkDate: '2026-03-06',                       // required, format: date
  startTime: '2026-03-06T14:00:00',             // required, format: date-time
  endTime: '2026-03-06T15:00:00',               // optional, format: date-time
  chatType: 'INDIVIDUAL',                       // required, enum: INDIVIDUAL | GROUP
  maxParticipants: 5,                           // required
  location: {                                   // required
    placeName: '서울숲',                          // required
    latitude: 37.544500,                         // required
    longitude: 127.044500,                       // required
    address: '서울시 성동구 성수동',               // optional
  },
  petIds: [1, 3],                               // required
};
```

### Apply Response Handling
```typescript
// Source: OpenAPI spec ThreadApplyResponse
// Success response shape:
{
  threadId: 42,
  chatRoomId: 15,
  applicationStatus: 'JOINED',    // enum: JOINED | CANCELED
  idempotentReplay: false,         // true if duplicate apply
  isIdempotentReplay: false,       // same (duplicate field)
}
// Both idempotentReplay and isIdempotentReplay exist -- use either
// DEC-014: if idempotentReplay === true, treat same as fresh success
```

### MapMarker Adapter
```typescript
// Transform ThreadMapResponse to DynamicMap's MapMarker format
import type { MapMarker } from '@/types';
import type { ThreadMapResponse } from '@/api/threads';

function toMapMarker(t: ThreadMapResponse): MapMarker {
  return {
    id: String(t.threadId),
    lat: t.latitude,
    lng: t.longitude,
    // ThreadMapResponse has no image -- use a default or generate from title
    image: undefined,
    isEmergency: false,
  };
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `threadService.getThreads(lat, lng)` | `getThreads({ page, size })` + `getThreadMap({ latitude, longitude, radius })` | Phase 2 | Separate list vs map data sources |
| `threadService.joinThread(id)` | `applyToThread(threadId, { petIds })` | Phase 2 | Apply requires pet selection, returns chatRoomId |
| `threadService.updateThread(id, data)` (PUT) | `updateThread(threadId, data)` (PATCH) | Phase 2 | PATCH for partial updates |
| `ThreadType` with string IDs | `ThreadSummaryResponse` / `ThreadResponse` with number IDs | Phase 2 | All IDs are numbers |
| 10-second polling | Manual refresh only | DEC-029 | No auto-polling |
| locationService.getCoordinates | navigator.geolocation.getCurrentPosition | DEC-026 | Direct browser GPS |

**Deprecated/outdated:**
- `services/api/threadService.ts`: Entire file replaced by `api/threads.ts`
- `ThreadType` from `types/index.ts`: Replaced by ThreadSummaryResponse/ThreadResponse
- `locationService`: Not needed for GPS -- use browser API directly
- `memberService.getMe()` in useRadarLogic: Use `useUserStore` for current user

## Open Questions

1. **Hotspot markers without coordinates**
   - What we know: `ThreadHotspotResponse` returns `{ region: string, count: number }` with no lat/lng
   - What's unclear: How to place hotspot markers on a map without coordinates
   - Recommendation: Display hotspots as a small badge/overlay on the map corner showing "region: count" list, rather than positioned markers. OR hardcode known Seoul district center coordinates as a lookup table. The CONTEXT says "marker icons on map" -- but this is blocked by API design. Use a region-name-to-coordinate lookup if available, otherwise use a list overlay.

2. **Thread expiry display without auto-refresh**
   - What we know: Threads expire at startTime + 60 min. No auto-polling allowed.
   - What's unclear: Should expired threads disappear from the view in real-time or only on manual refresh?
   - Recommendation: Use a local `setInterval` (every 60s) that updates `currentTime` for display-only expiry filtering. This is not a data fetch -- just client-side filtering of already-loaded data.

3. **DynamicMap marker customization for non-image markers**
   - What we know: ThreadMapResponse has no image URL. Current DynamicMap creates markers from pet images.
   - What's unclear: What to show as the marker icon when no image is available
   - Recommendation: Create a generic thread marker icon (using L.divIcon with a paw/walk icon) for thread markers. Keep the existing pet-image marker for detail view only.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | No frontend test runner configured |
| Config file | none |
| Quick run command | `cd aini-inu-frontend && npm run lint` |
| Full suite command | `cd aini-inu-frontend && npm run build` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| WALK-01 | Thread create, non-pet-owner block | manual-only | `npm run build` (compile check) | N/A |
| WALK-02 | Thread update pre-fill form | manual-only | `npm run build` | N/A |
| WALK-03 | Thread delete with confirmation | manual-only | `npm run build` | N/A |
| WALK-04 | Thread list paginated | manual-only | `npm run build` | N/A |
| WALK-05 | Thread detail display | manual-only | `npm run build` | N/A |
| WALK-06 | Apply with petIds, capacity reject, idempotent | manual-only | `npm run build` | N/A |
| WALK-07 | Apply cancel | manual-only | `npm run build` | N/A |
| WALK-08 | Map markers from getThreadMap | manual-only | `npm run build` | N/A |
| WALK-09 | Hotspot display | manual-only | `npm run build` | N/A |
| WALK-10 | chatType toggle on create | manual-only | `npm run build` | N/A |
| WALK-11 | GPS acquire + fallback | manual-only | `npm run build` | N/A |
| WALK-12 | Manual re-search only | manual-only | `npm run build` | N/A |
| WALK-13 | Validation: title 30, desc 500, 60min expiry | manual-only | `npm run build` | N/A |
| WALK-14 | Required fields validation | manual-only | `npm run build` | N/A |

**Justification for manual-only:** Frontend has no test runner configured (per CLAUDE.md). Validation is via `npm run lint` (ESLint) and `npm run build` (TypeScript compile + Next.js build). All behavioral verification requires browser-based UAT.

### Sampling Rate
- **Per task commit:** `cd aini-inu-frontend && npm run lint && npm run build`
- **Per wave merge:** `cd aini-inu-frontend && npm run build`
- **Phase gate:** Full build green + manual UAT before `/gsd:verify-work`

### Wave 0 Gaps
None -- no test infrastructure to set up. Validation is lint + build (already configured).

## Sources

### Primary (HIGH confidence)
- OpenAPI spec `common-docs/openapi/openapi.v1.json` - Verified all 9 thread endpoints, request/response schemas, parameter types, enum values
- `api/threads.ts` - Verified all function signatures match OpenAPI spec
- `api/pets.ts` - Verified getMyPets for pet selection
- `api/types.ts` - Verified SliceResponse, PaginationParams, ApiError types

### Secondary (MEDIUM confidence)
- Existing component source code (AroundMeHeader, RadarMapSection, RadarSidebar, RecruitForm, DynamicMap) - Analyzed for rewire scope
- `useRadarLogic.ts` - Analyzed for what must change (polling, types, API calls)
- `useConfigStore.ts` - Verified GPS coordinate state management

### Tertiary (LOW confidence)
- Hotspot marker positioning: API returns region name only, no coordinates -- display strategy needs validation

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All libraries already installed and verified
- Architecture: HIGH - Existing components provide clear rewire targets, API types fully verified against OpenAPI
- Pitfalls: HIGH - Identified from direct source code analysis (type mismatches, format differences, missing fields)
- Hotspot display: LOW - API limitation (no coordinates) creates uncertainty in implementation approach

**Research date:** 2026-03-06
**Valid until:** 2026-04-06 (stable frontend rewire, no external dependency changes expected)
