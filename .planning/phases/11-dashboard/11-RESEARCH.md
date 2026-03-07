# Phase 11: Dashboard - Research

**Researched:** 2026-03-08
**Domain:** React client-side composition, multi-API aggregation, partial-failure UX
**Confidence:** HIGH

## Summary

Phase 11 is a **frontend-only** refactoring of the existing dashboard page (`src/app/dashboard/page.tsx`). The current implementation already uses `Promise.allSettled` for parallel API fetching but relies on legacy service imports (`threadService`, `memberService`) and legacy types (`ThreadType`, `UserType`). The work involves: (1) rewiring all API calls to canonical `@/api/*` modules, (2) replacing `DraftNotification` with a new `PendingReviewCard` that detects unwritten reviews via chat room inspection, (3) converting the walk stats heatmap grid from fixed 126-cell `number[]` to dynamic `WalkStatsResponse.points` based rendering, (4) rewiring `AIBanner` to use `ThreadHotspotResponse[]` (array, not single object), and (5) ensuring each section has independent loading/error/success states.

All required API endpoints already exist and are implemented in `@/api/members.ts`, `@/api/threads.ts`, and `@/api/chat.ts`. No backend changes needed. The existing `WalkReviewModal` component can be reused for the pending review submission flow.

**Primary recommendation:** Decompose the monolithic `fetchData()` in page.tsx into per-section data fetching with individual error states, then build each section component to own its 5-state lifecycle (default/loading/empty/error/success).

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- DraftNotification removed (dead code -- no isDraft concept in backend)
- PendingReviewCard replaces it (topmost, shown only when unwritten reviews exist)
- RecentFriends retained (existing feature preservation, API cleanup only)
- Final render order: (1) PendingReviewCard (conditional) -> (2) AIBanner (hotspot coaching) -> (3) DashboardHero (greeting + manner + stats) -> (4) RecentFriends (chat history) -> (5) LocalFeedPreview (latest threads)
- All components remove old service imports -> rewire to `@/api/*` modules
- Legacy types (`ThreadType`, `UserType`) -> proper API response types (`ThreadResponse`, `MemberResponse`, `WalkStatsResponse`, `ThreadHotspotResponse`)
- Pending review UX: card click opens modal (not auto-popup), multiple reviews listed in modal, submit failure keeps modal open with error + retry, dismiss allowed
- Detection logic: `getRooms()` -> per-room `reviews/me` check -> filter rooms without review
- Walk stats heatmap: `WalkStatsResponse.points` -> 7-row N-col grid (dynamic based on windowDays, not fixed 126)
- Empty dates filled with count=0; totalWalks from API directly; streak/successRate computed from points array
- Hotspot recommendation: highest count from `getHotspots()` array
- Data fetching: `Promise.allSettled` for 5 parallel calls (getMe + getWalkStats + getHotspots + getThreads + getRooms)
- Per-section independent error boundary / loading
- Section 5-state guarantee: default / loading / empty / error / success
- Partial failure: one section fail -> only that section shows error fallback with retry button

### Claude's Discretion
- Suspense boundary vs custom loading state implementation approach
- Error fallback card visual design
- Loading skeleton vs spinner choice
- PendingReviewCard visual design (can reference DraftNotification dark style)

### Deferred Ideas (OUT OF SCOPE)
- Walk completion + diary unwritten detection notification (no backend endpoint exists)
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| DASH-01 | Greeting/manner score/walk activity stats display (MEM-ME-GET + MEM-WALK-STATS-GET) | `getMe()` returns `MemberResponse` with `mannerTemperature`; `getWalkStats()` returns `WalkStatsResponse` with `points[]`, `totalWalks`, `windowDays`. DashboardHero component exists, needs type rewire from `UserType`+`number[]` to `MemberResponse`+`WalkStatsResponse`. |
| DASH-02 | Walk recommendation cards from hotspot data (THR-HOTSPOT-GET) | `getHotspots()` returns `ThreadHotspotResponse[]` (array of `{region, count}`). AIBanner exists, needs rewire from single `{region, count}` to selecting max-count from array. |
| DASH-03 | Neighborhood latest threads summary (THR-LIST) | `getThreads()` returns `SliceResponse<ThreadSummaryResponse>`. LocalFeedPreview exists, needs rewire from `ThreadType[]` to `ThreadSummaryResponse[]`. Field mapping: `title`, `description`, `placeName` (was `location`/`place`), `startTime`, `status`. Note: `ThreadSummaryResponse` lacks `author` field -- card design must adapt. |
| DASH-04 | Pending review modal with submit/retry (CHAT-ROOMS-GET + CHAT-REVIEW-CREATE) | `getRooms()` returns rooms; `getMyReview(roomId)` returns `{exists, review}` per room; `createReview(roomId, data)` submits. Existing `WalkReviewModal` can be reused/adapted for submission. New `PendingReviewCard` component needed. |
| DASH-05 | Per-section partial failure fallback | Each of 5 API calls via `Promise.allSettled` independently handled. Per-section error/loading/success state management needed. |
</phase_requirements>

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| React | 19 | Component rendering | Project standard, `use()` hook |
| Next.js | 16 | App Router, page structure | Project standard |
| Zustand | latest | Global state (useUserStore) | Already in use for auth/profile |
| lucide-react | latest | Icons | Already used across all components |
| sonner | latest | Toast notifications | Project standard for error/success toasts |
| tailwindcss | 4 | Styling | Project standard |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `@/api/members` | n/a | getMe, getWalkStats | DASH-01 data |
| `@/api/threads` | n/a | getHotspots, getThreads | DASH-02, DASH-03 data |
| `@/api/chat` | n/a | getRooms, getMyReview, createReview | DASH-04 pending review detection + submission |
| `@/components/ui/*` | n/a | Card, Typography, Button, Badge | Existing UI primitives |
| `WalkReviewModal` | n/a | Review submission form | Reuse for DASH-04 review submission |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Custom loading/error states | React Suspense boundaries | Suspense requires RSC or lazy loading; this project is fully 'use client', custom states give more control over per-section granularity |
| SWR/React Query | Direct Promise.allSettled | Already established pattern in dashboard; adding data-fetching library for one page is overkill |

## Architecture Patterns

### Recommended Component Structure
```
src/app/dashboard/
  page.tsx                    # Orchestrator: parallel fetch, distribute data to sections
  error.tsx                   # Existing Next.js error boundary (keep)
src/components/dashboard/
  PendingReviewCard.tsx       # NEW: replaces DraftNotification
  PendingReviewModal.tsx      # NEW: multi-review selection + WalkReviewModal integration
  AIBanner.tsx                # MODIFY: rewire to ThreadHotspotResponse[]
  DashboardHero.tsx           # MODIFY: rewire to MemberResponse + WalkStatsResponse
  RecentFriends.tsx           # KEEP: already uses @/api/chat (cleanup only)
  LocalFeedPreview.tsx        # MODIFY: rewire to ThreadSummaryResponse[]
  DraftNotification.tsx       # DELETE: dead code
  MemberSearchModal.tsx       # KEEP: no changes
src/utils/
  walkStatsGrid.ts            # NEW: points[] -> 7-row grid transformation utility
```

### Pattern 1: Per-Section State Management
**What:** Each section manages its own loading/error/data state independently
**When to use:** Dashboard aggregation pages where partial failure is acceptable
**Example:**
```typescript
// In page.tsx
type SectionState<T> =
  | { status: 'loading' }
  | { status: 'error'; error: string; retry: () => void }
  | { status: 'empty' }
  | { status: 'success'; data: T };

const [heroState, setHeroState] = useState<SectionState<{ me: MemberResponse; stats: WalkStatsResponse }>>({ status: 'loading' });
const [hotspotState, setHotspotState] = useState<SectionState<ThreadHotspotResponse[]>>({ status: 'loading' });
// ... etc for each section
```

### Pattern 2: Promise.allSettled with Per-Result Handling
**What:** Fire all API calls in parallel, handle each result independently
**When to use:** Multi-API aggregation where one failure should not block others
**Example:**
```typescript
const [meResult, statsResult, hotspotsResult, threadsResult, roomsResult] =
  await Promise.allSettled([
    getMe(),
    getWalkStats(),
    getHotspots(),
    getThreads({ page: 0, size: 3 }),
    getRooms({ page: 0, size: 20 }),
  ]);

// Each result handled independently
if (meResult.status === 'fulfilled' && statsResult.status === 'fulfilled') {
  setHeroState({ status: 'success', data: { me: meResult.value, stats: statsResult.value }});
} else {
  setHeroState({ status: 'error', error: '...', retry: () => fetchHero() });
}
```

### Pattern 3: Heatmap Grid Transformation
**What:** Convert `WalkStatsResponse.points` (date+count pairs) into 7-row grid cells
**When to use:** Walk activity grass rendering
**Example:**
```typescript
function pointsToGrid(points: WalkStatsPointResponse[], windowDays: number): number[] {
  // Create date->count map from points
  const countMap = new Map(points.map(p => [p.date, p.count]));
  // Generate all dates in window, fill missing with 0
  const grid: number[] = [];
  const start = new Date(/* startDate */);
  for (let i = 0; i < windowDays; i++) {
    const d = new Date(start);
    d.setDate(d.getDate() + i);
    const key = d.toISOString().split('T')[0];
    grid.push(countMap.get(key) ?? 0);
  }
  return grid; // 7-row grid via CSS grid-rows-7 grid-flow-col
}
```

### Pattern 4: Pending Review Detection
**What:** Scan chat rooms for missing reviews
**When to use:** DASH-04 pending review card
**Example:**
```typescript
async function detectPendingReviews(currentMemberId: number): Promise<PendingReview[]> {
  const roomsRes = await getRooms({ page: 0, size: 50 });
  const rooms = roomsRes.content;

  // Check each room for existing review
  const reviewChecks = await Promise.allSettled(
    rooms.map(r => getMyReview(r.chatRoomId))
  );

  // Filter rooms without review
  return rooms
    .filter((room, i) => {
      const check = reviewChecks[i];
      return check.status === 'fulfilled' && !check.value.exists;
    })
    .map(room => ({
      chatRoomId: room.chatRoomId,
      displayName: room.displayName,
      // ... partner info extracted from room detail
    }));
}
```

### Anti-Patterns to Avoid
- **Monolithic fetchData with shared try/catch:** Current pattern catches all errors in one block. Each section must handle its own errors independently.
- **Legacy service imports:** `threadService` and `memberService` use old `apiClient` from `services/api/apiClient.ts`, not the canonical `@/api/client.ts`. Mixing causes dual interceptor issues.
- **Fixed grid size (126 cells):** Current heatmap hardcodes `new Array(126).fill(0)`. Use `windowDays` from API response.
- **Using `UserType` props when `MemberResponse` is available:** `useUserStore` already maps `MemberResponse` to `UserType`, but components should accept the actual API types directly where possible.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Review submission form | New review form component | Existing `WalkReviewModal` | Already handles star rating, tags, comment, createReview API, loading states |
| Toast notifications | Custom error/success UI | `sonner` toast | Project-wide toast standard |
| Member profile data | Direct API call in component | `useUserStore` / `useProfile` hook | Handles caching, dedup, MemberResponse->UserType mapping |
| Date manipulation | Manual date string parsing | `new Date()` + `toISOString().split('T')[0]` | Standard JS Date API sufficient for date iteration |

## Common Pitfalls

### Pitfall 1: ThreadSummaryResponse Lacks Author Field
**What goes wrong:** `LocalFeedPreview` currently renders `thread.author?.avatar`, `thread.author?.nickname`, `thread.author?.mannerScore` -- none of these exist on `ThreadSummaryResponse`.
**Why it happens:** The list endpoint returns a summary, not full detail.
**How to avoid:** Redesign LocalFeedPreview cards to show only fields available in `ThreadSummaryResponse`: `title`, `description`, `placeName`, `startTime`, `endTime`, `chatType`, `currentParticipants`, `maxParticipants`, `status`.
**Warning signs:** Undefined avatar/nickname in rendered cards.

### Pitfall 2: getHotspots Returns Array, Not Single Object
**What goes wrong:** Current code sets `setHotspot(hotspotRes.value)` expecting a single `{region, count}`. Actual API returns `ThreadHotspotResponse[]`.
**Why it happens:** Old `threadService.getHotspots()` returned a single object; canonical `@/api/threads.ts` correctly returns array.
**How to avoid:** Select max-count item from array: `hotspots.reduce((max, h) => h.count > max.count ? h : max)`.
**Warning signs:** "[Object object]" in AI banner text.

### Pitfall 3: MemberResponse.mannerTemperature vs UserType.mannerScore
**What goes wrong:** If DashboardHero switches to `MemberResponse` directly, the field name is `mannerTemperature` not `mannerScore`.
**Why it happens:** `useUserStore` maps `mannerTemperature` -> `mannerScore` in `mapMemberToUser()`.
**How to avoid:** If keeping `useProfile()` hook (which returns `UserType`), use `profile.mannerScore`. If using `MemberResponse` directly from `getMe()`, use `mannerTemperature`.
**Warning signs:** Manner score shows as `undefined/10`.

### Pitfall 4: Pending Review N+1 API Calls
**What goes wrong:** Checking `getMyReview(roomId)` for every room creates N additional API calls.
**Why it happens:** No batch endpoint exists for checking review status across rooms.
**How to avoid:** Limit rooms checked (e.g., first 20 most recent). The `getRooms` call already supports pagination. Consider capping at rooms from last 30 days.
**Warning signs:** Slow dashboard load with many chat rooms.

### Pitfall 5: WalkStatsResponse.points May Be Sparse
**What goes wrong:** Grid shows incorrect layout if points array has gaps (days with no walks are not included).
**Why it happens:** Backend may only return dates with count > 0.
**How to avoid:** Use `startDate`, `endDate`, and `windowDays` to generate all dates, then fill from points map. Always iterate from startDate for windowDays count.
**Warning signs:** Heatmap grid has fewer cells than expected, misaligned rows.

### Pitfall 6: useProfile Auto-Fetch Race Condition
**What goes wrong:** `useProfile()` triggers `fetchProfile()` in an effect, but dashboard also calls `getMe()` directly, resulting in duplicate API calls.
**Why it happens:** Dashboard needs `MemberResponse` for hero section but `useProfile()` already fetches it.
**How to avoid:** Use `useUserStore.getState().profile` for greeting/manner display (already fetched by AuthProvider/useProfile). Only call `getMe()` directly if `MemberResponse` fields beyond `UserType` are needed (they aren't for dashboard).
**Warning signs:** Double `/members/me` request in network tab.

## Code Examples

### Walk Stats Grid Transformation
```typescript
// Source: WalkStatsResponse type from @/api/members.ts
import type { WalkStatsResponse, WalkStatsPointResponse } from '@/api/members';

export function pointsToGridCounts(stats: WalkStatsResponse): number[] {
  const countMap = new Map<string, number>();
  for (const p of stats.points) {
    countMap.set(p.date, p.count);
  }

  const grid: number[] = [];
  const start = new Date(stats.startDate + 'T00:00:00');
  for (let i = 0; i < stats.windowDays; i++) {
    const d = new Date(start);
    d.setDate(d.getDate() + i);
    const key = d.toISOString().split('T')[0];
    grid.push(countMap.get(key) ?? 0);
  }
  return grid;
}
```

### Pending Review Detection
```typescript
// Source: @/api/chat.ts types
import { getRooms, getMyReview, getRoom } from '@/api/chat';
import type { ChatRoomSummaryResponse } from '@/api/chat';

interface PendingReview {
  chatRoomId: number;
  displayName: string;
  partnerId: number;
  partnerNickname: string;
}

async function detectPendingReviews(currentMemberId: number): Promise<PendingReview[]> {
  const roomsRes = await getRooms({ page: 0, size: 20 });
  const rooms = roomsRes.content;

  const checks = await Promise.allSettled(
    rooms.map(async (room) => {
      const myReview = await getMyReview(room.chatRoomId);
      if (myReview.exists) return null; // Already reviewed

      const detail = await getRoom(room.chatRoomId);
      const partner = detail.participants.find(
        p => p.memberId !== currentMemberId && !p.left
      );
      if (!partner) return null;

      return {
        chatRoomId: room.chatRoomId,
        displayName: room.displayName,
        partnerId: partner.memberId,
        partnerNickname: partner.nickname ?? `Member ${partner.memberId}`,
      };
    })
  );

  return checks
    .filter((r): r is PromiseFulfilledResult<PendingReview | null> => r.status === 'fulfilled')
    .map(r => r.value)
    .filter((v): v is PendingReview => v !== null);
}
```

### Error Fallback Section Component
```typescript
// Reusable per-section error fallback
interface SectionErrorFallbackProps {
  message: string;
  onRetry: () => void;
}

function SectionErrorFallback({ message, onRetry }: SectionErrorFallbackProps) {
  return (
    <Card className="p-8 flex flex-col items-center gap-4 border-dashed border-2 border-zinc-100">
      <Typography variant="body" className="text-zinc-400 text-sm">{message}</Typography>
      <Button variant="ghost" size="sm" onClick={onRetry}>
        다시 시도
      </Button>
    </Card>
  );
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `threadService.getHotspots()` returns `{region, count}` | `getHotspots()` returns `ThreadHotspotResponse[]` | Phase 6 | Must pick max-count from array |
| `memberService.getWalkStats()` returns `number[]` | `getWalkStats()` returns `WalkStatsResponse` with structured points | Phase 4 | Grid must use date-based iteration |
| `DraftNotification` for diary drafts | PendingReviewCard for unwritten reviews | Phase 11 (now) | Different detection logic (chat rooms, not diaries) |
| `UserType` in dashboard components | `MemberResponse` / `useUserStore` profile | Phase 4 | Field name differences (mannerScore vs mannerTemperature) |

**Deprecated/outdated:**
- `threadService` (in `services/api/threadService.ts`): Uses old `apiClient`, returns legacy types. Remove imports.
- `memberService` (in `services/api/memberService.ts`): Uses old `apiClient`. Remove imports.
- `DraftNotification` component: Backend has no `isDraft` concept. Delete entirely.
- `ThreadType` / `UserType` in dashboard: Replace with canonical API response types.

## Open Questions

1. **ThreadSummaryResponse card design without author info**
   - What we know: `ThreadSummaryResponse` has no `authorId`, no author avatar/nickname/mannerScore
   - What's unclear: How simplified should the LocalFeedPreview cards be?
   - Recommendation: Show thread title, description, placeName, time range, participant count, status badge. Drop author section entirely since the data isn't available.

2. **Pending review room scope**
   - What we know: `getRooms()` returns all rooms. Checking all rooms for reviews could be expensive.
   - What's unclear: Should we filter by room status (e.g., only ACTIVE rooms) or time range?
   - Recommendation: Filter by `status` if the API supports it. Limit to page 0, size 20 as a reasonable cap.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | None (frontend has no test runner) |
| Config file | none |
| Quick run command | `cd aini-inu-frontend && npm run lint` |
| Full suite command | `cd aini-inu-frontend && npm run build` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| DASH-01 | Greeting, manner score, walk stats render | manual-only | `npm run build` (compile check) | n/a |
| DASH-02 | Hotspot recommendation card | manual-only | `npm run build` (compile check) | n/a |
| DASH-03 | Latest threads summary cards | manual-only | `npm run build` (compile check) | n/a |
| DASH-04 | Pending review modal submit/retry | manual-only | `npm run build` (compile check) | n/a |
| DASH-05 | Per-section partial failure fallback | manual-only | `npm run build` (compile check) | n/a |

**Justification for manual-only:** Frontend has no test runner configured per CLAUDE.md. Validation is via `npm run lint` and `npm run build`. All DASH requirements involve UI rendering behavior that requires visual/integration verification.

### Sampling Rate
- **Per task commit:** `cd aini-inu-frontend && npm run lint && npm run build`
- **Per wave merge:** `cd aini-inu-frontend && npm run build`
- **Phase gate:** Build green + lint clean before `/gsd:verify-work`

### Wave 0 Gaps
None -- no test infrastructure to set up (frontend uses lint + build only).

## Sources

### Primary (HIGH confidence)
- `@/api/members.ts` lines 79-91 - WalkStatsResponse type definition with points[], windowDays, totalWalks
- `@/api/threads.ts` lines 112-115 - ThreadHotspotResponse type (region + count)
- `@/api/threads.ts` lines 76-91 - ThreadSummaryResponse type (no author field)
- `@/api/chat.ts` lines 96-100 - MyChatReviewResponse type (exists + review)
- `@/api/chat.ts` lines 203-210 - createReview function
- `src/app/dashboard/page.tsx` - Current dashboard implementation with Promise.allSettled
- `src/components/dashboard/DraftNotification.tsx` - Component to be replaced
- `src/components/shared/modals/WalkReviewModal.tsx` - Reusable review submission modal

### Secondary (MEDIUM confidence)
- Project CLAUDE.md - Frontend has no test runner; validation via lint + build
- Phase 11 CONTEXT.md - User decisions constraining implementation

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All libraries already in use, no new dependencies
- Architecture: HIGH - Pattern well-defined by existing code and CONTEXT.md decisions
- Pitfalls: HIGH - Verified by direct code inspection of current types vs API response types

**Research date:** 2026-03-08
**Valid until:** 2026-04-07 (stable -- no external dependency changes expected)
