---
phase: 08-chat-system
plan: 04
subsystem: ui
tags: [react, zustand, polling, chat, useEffect]

requires:
  - phase: 08-chat-system
    provides: chat store, WebSocket hook, chat room page, message list component
provides:
  - mergeMessages store action with shallow equality check
  - Stable polling that preserves optimistic messages
  - Primitive-based effect dependencies preventing infinite loops
affects: [08-chat-system]

tech-stack:
  added: []
  patterns: [shallow-equality merge for polling, primitive useEffect dependencies]

key-files:
  created: []
  modified:
    - aini-inu-frontend/src/store/useChatStore.ts
    - aini-inu-frontend/src/hooks/useChatWebSocket.ts
    - aini-inu-frontend/src/app/chat/[id]/page.tsx
    - aini-inu-frontend/src/components/chat/MessageList.tsx

key-decisions:
  - "mergeMessages uses Map-based merge to preserve optimistic sends not yet confirmed by server"
  - "Mark-read effect depends on latestMessageId (number) not messages array to prevent infinite API calls"
  - "Auto-scroll depends on messages.length not array reference to prevent visual flickering"

patterns-established:
  - "Polling merge pattern: shallow equality check before state update to prevent unnecessary re-renders"
  - "Primitive dependency pattern: derive primitive values from arrays for useEffect dependencies"

requirements-completed: [CHAT-03, CHAT-04, CHAT-05]

duration: 2min
completed: 2026-03-07
---

# Phase 08 Plan 04: Message Flickering Fix Summary

**mergeMessages with shallow equality check eliminates polling-induced infinite loops and visual flickering in chat rooms**

## Performance

- **Duration:** 2 min
- **Started:** 2026-03-07T01:03:25Z
- **Completed:** 2026-03-07T01:05:13Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments
- Added mergeMessages store action that prevents unnecessary state updates when polled data is identical
- Fixed mark-read useEffect to depend on latestMessageId primitive instead of messages array reference
- Fixed auto-scroll useEffect to depend on messages.length instead of array reference
- Added removeRoom, clearMessages, and disconnect utility functions for future use

## Task Commits

Each task was committed atomically:

1. **Task 1: Add mergeMessages to useChatStore and rewire polling** - `5b8f6e2` (feat)
2. **Task 2: Fix mark-read and auto-scroll effect dependencies** - `46292ea` (fix)

## Files Created/Modified
- `aini-inu-frontend/src/store/useChatStore.ts` - Added mergeMessages, removeRoom, clearMessages actions
- `aini-inu-frontend/src/hooks/useChatWebSocket.ts` - Rewired polling to use mergeMessages, added disconnect()
- `aini-inu-frontend/src/app/chat/[id]/page.tsx` - Mark-read effect now depends on latestMessageId primitive
- `aini-inu-frontend/src/components/chat/MessageList.tsx` - Auto-scroll depends on messages.length

## Decisions Made
- mergeMessages uses Map-based merge to preserve optimistic sends not yet confirmed by server
- Mark-read effect depends on latestMessageId (number) not messages array to prevent infinite API calls
- Auto-scroll depends on messages.length not array reference to prevent visual flickering

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed removeRoom using chatRoomId instead of roomId**
- **Found during:** Task 1
- **Issue:** ChatRoomSummaryResponse uses `chatRoomId` not `roomId`
- **Fix:** Changed filter to use `r.chatRoomId !== roomId`
- **Files modified:** aini-inu-frontend/src/store/useChatStore.ts
- **Verification:** TypeScript compilation passes
- **Committed in:** 5b8f6e2 (Task 1 commit)

---

**Total deviations:** 1 auto-fixed (1 bug)
**Impact on plan:** Trivial field name fix. No scope creep.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Chat polling is now stable with no infinite loops
- All gap closure plans for phase 08 complete

---
*Phase: 08-chat-system*
*Completed: 2026-03-07*
