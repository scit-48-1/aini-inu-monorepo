---
phase: 08-chat-system
plan: 01
subsystem: ui
tags: [stomp, websocket, zustand, chat, react]

requires:
  - phase: 02-common-infrastructure
    provides: api/chat.ts, api/members.ts, apiClient, types
provides:
  - useChatStore Zustand store for chat state management
  - useChatWebSocket STOMP hook with polling fallback
  - ChatList rewired to api/chat.ts with error/loading/empty states
  - ChatStartModal rewired to createDirectRoom with number IDs
affects: [08-chat-system]

tech-stack:
  added: ["@stomp/stompjs"]
  patterns: ["STOMP over WebSocket with polling fallback", "O(1) message dedup via Set"]

key-files:
  created:
    - aini-inu-frontend/src/store/useChatStore.ts
    - aini-inu-frontend/src/hooks/useChatWebSocket.ts
  modified:
    - aini-inu-frontend/src/components/chat/ChatList.tsx
    - aini-inu-frontend/src/components/chat/ChatStartModal.tsx
    - aini-inu-frontend/package.json

key-decisions:
  - "DisplayUser unified type in ChatStartModal maps both MemberFollowResponse and MemberResponse to common interface"
  - "Placeholder avatar /AINIINU_ROGO_B.png used for room list since ChatRoomSummaryResponse lacks participant avatar"
  - "Client-side tab filtering (ACTIVE/PAST) since getRooms status param may not be supported"

patterns-established:
  - "STOMP Client stored in useRef, not state (avoids re-render on internal client changes)"
  - "Polling and WS mutually exclusive -- clearPolling before WS connect, startPolling on disconnect"
  - "beforeConnect refreshes JWT token from useAuthStore for reconnection scenarios"

requirements-completed: [CHAT-01, CHAT-03, CHAT-12, CHAT-14]

duration: 3min
completed: 2026-03-07
---

# Phase 8 Plan 1: Chat Infrastructure & Room List Summary

**STOMP WebSocket hook with polling fallback, Zustand chat store with O(1) dedup, and ChatList/ChatStartModal rewired to api/chat.ts**

## Performance

- **Duration:** 3 min
- **Started:** 2026-03-06T19:50:14Z
- **Completed:** 2026-03-06T19:53:33Z
- **Tasks:** 2
- **Files modified:** 5

## Accomplishments
- Installed @stomp/stompjs and created STOMP WebSocket hook with automatic polling fallback on connection failure
- Created Zustand chat store with message deduplication (O(1) via Set), pending message tracking, and room list cache
- Rewired ChatList to use getRooms() from api/chat.ts with loading/empty/error states and pagination
- Rewired ChatStartModal to use createDirectRoom() and getFollowing() from new API layer

## Task Commits

Each task was committed atomically:

1. **Task 1: Install @stomp/stompjs, create useChatStore + useChatWebSocket** - `aec1b38` (feat)
2. **Task 2: Rewire ChatList and ChatStartModal to api/chat.ts** - `bcb39c1` (feat)

## Files Created/Modified
- `aini-inu-frontend/src/store/useChatStore.ts` - Zustand store for chat rooms, messages, pending sends, connection mode
- `aini-inu-frontend/src/hooks/useChatWebSocket.ts` - STOMP client hook with JWT auth, event handling, polling fallback
- `aini-inu-frontend/src/components/chat/ChatList.tsx` - Room list using getRooms() with error retry and pagination
- `aini-inu-frontend/src/components/chat/ChatStartModal.tsx` - Direct chat creation using createDirectRoom() with number IDs
- `aini-inu-frontend/package.json` - Added @stomp/stompjs dependency

## Decisions Made
- DisplayUser unified type in ChatStartModal maps both MemberFollowResponse (from getFollowing) and MemberResponse (from searchMembers) to common interface for consistent rendering
- Placeholder avatar /AINIINU_ROGO_B.png used for room list since ChatRoomSummaryResponse lacks participant avatar data
- Client-side tab filtering for ACTIVE/PAST since status param support in getRooms is uncertain
- startingChatId changed from string to number to match memberId type from new API

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed unused `get` parameter in useChatStore**
- **Found during:** Task 2 (lint verification)
- **Issue:** Zustand create callback declared `get` parameter but never used it, triggering @typescript-eslint/no-unused-vars
- **Fix:** Removed `get` from the destructured parameters
- **Files modified:** aini-inu-frontend/src/store/useChatStore.ts
- **Verification:** npm run lint passes for useChatStore.ts
- **Committed in:** bcb39c1 (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (1 bug)
**Impact on plan:** Minor lint fix, no scope creep.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Chat store and WebSocket hook ready for Plan 02 (message view) consumption
- ChatList and ChatStartModal fully wired to new API layer
- STOMP event types defined for CHAT_MESSAGE_CREATED, DELIVERED, and READ events

---
*Phase: 08-chat-system*
*Completed: 2026-03-07*
