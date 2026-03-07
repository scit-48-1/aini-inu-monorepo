---
phase: 08-chat-system
verified: 2026-03-07T10:30:00Z
status: passed
score: 19/19 must-haves verified
re_verification:
  previous_status: passed
  previous_score: 15/15
  gaps_closed:
    - "Messages display stably without flickering when polling is active"
    - "Polling does not cause infinite API calls to mark-read endpoint"
    - "Auto-scroll only fires on genuinely new messages, not on every poll cycle"
    - "Optimistic pending messages are preserved across polling cycles"
    - "Message send works correctly without being disrupted by polling"
    - "Leave room disconnects WebSocket before navigating"
    - "Leave room removes room from cached list so it does not reappear"
    - "Leave room uses ConfirmModal (danger variant) instead of window.confirm"
    - "No persistent error notifications after leaving a room"
    - "ChatList re-fetches rooms when navigating back to /chat"
    - "Dashboard recent friends cards have consistent size regardless of count"
    - "Clicking a dashboard friend navigates to their actual member profile"
  gaps_remaining: []
  regressions: []
---

# Phase 8: Chat System Verification Report

**Phase Goal:** Rewire the frontend chat system to use the real backend API (api/chat.ts) with WebSocket real-time messaging, replacing the old mock chatService
**Verified:** 2026-03-07T10:30:00Z
**Status:** passed
**Re-verification:** Yes -- post-UAT gap closure (plans 08-04, 08-05)

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | User sees chat room list with loading/empty/error states | VERIFIED | ChatList.tsx (319 lines) renders loading spinner, error with retry ("채팅 목록을 불러올 수 없습니다."), empty state with MessageSquare icon |
| 2 | User can start a 1:1 direct chat that reuses existing room | VERIFIED | ChatStartModal.tsx calls `createDirectRoom({ partnerId })` and navigates to `/chat/${room.chatRoomId}` |
| 3 | Group chat rooms display participant count | VERIFIED | ChatList.tsx shows "그룹" badge for GROUP rooms; ChatHeader shows group pet names |
| 4 | WebSocket STOMP client connects with JWT auth | VERIFIED | useChatWebSocket.ts creates Client with brokerURL, beforeConnect refreshes JWT from useAuthStore, subscribes to `/topic/chat-rooms/${roomId}/events` |
| 5 | User sees message history loaded cursor-based (newest first) | VERIFIED | page.tsx calls `getMessages(roomId, { size: 20 })`, stores nextCursor and hasMore from CursorResponse |
| 6 | User scrolls up to load older messages without scroll jump | VERIFIED | MessageList.tsx uses IntersectionObserver on sentinel div at top, scroll position preservation via requestAnimationFrame + scrollHeight delta |
| 7 | User can send a message (max 500 chars) with optimistic display | VERIFIED | ChatInput has `maxLength={500}` with character counter; page.tsx handleSend uses addPendingMessage -> sendMessage -> removePendingMessage pattern |
| 8 | Failed message shows retry bubble with resend button | VERIFIED | MessageList.tsx renders failed pending messages with red border and RefreshCw button calling onRetry |
| 9 | Real-time messages arrive via WebSocket or polling fallback | VERIFIED | useChatWebSocket handles CHAT_MESSAGE_CREATED events via STOMP; on WS error/disconnect falls back to 5s polling via setInterval |
| 10 | Message status indicators show created/delivered/read | VERIFIED | StatusIcon component: Check (zinc-300) for created, CheckCheck (zinc-400) for delivered, CheckCheck (blue-500) for read |
| 11 | User can confirm walk and see confirm state per participant | VERIFIED | ChatHeader shows CheckCircle button with confirmedCount/totalCount display; page.tsx calls confirmWalk/cancelWalkConfirm |
| 12 | User can leave chat room with ConfirmModal (danger variant) and redirect | VERIFIED | page.tsx uses ConfirmModal with variant="danger", handleLeaveConfirm calls disconnect() then leaveRoom then clearMessages/removeRoom then router.push('/chat') |
| 13 | User can write a one-time non-editable review (score 1-5 + comment) | VERIFIED | WalkReviewModal.tsx calls createReview with { revieweeId, score, comment }; star rating 1-5; tags concatenated into comment |
| 14 | Review button only visible after walk confirmed and no existing review | VERIFIED | page.tsx line 351: `walkConfirm?.allConfirmed === true && !myReview?.exists` gates review button |
| 15 | Messages display stably without flickering under polling | VERIFIED | useChatStore.mergeMessages (line 114) uses shallow equality check (same length + same IDs) to skip no-op updates; polling calls mergeMessages not setMessages |
| 16 | Mark-read effect uses primitive dependency, no infinite loops | VERIFIED | page.tsx line 122-123: `latestMessageId = messages[messages.length - 1]?.id`; useEffect depends on latestMessageId not messages array |
| 17 | Auto-scroll depends on messages.length not array reference | VERIFIED | MessageList.tsx lines 61/65/77: useEffect deps are `[messages.length]` and `[messages.length, pendingMessages.length]` |
| 18 | Leave room disconnects WS before API call and cleans store | VERIFIED | page.tsx handleLeaveConfirm (line 262): disconnect() -> leaveRoom(roomId) -> clearMessages() -> removeRoom(roomId) -> router.push('/chat') |
| 19 | Dashboard friend cards consistent size and correct profile navigation | VERIFIED | RecentFriends.tsx cards have `min-w-[160px] max-w-[200px] flex-shrink-0`; dashboard/page.tsx extracts partner.memberId from getRoom detail (line 77: `id: String(partner.memberId)`) |

**Score:** 19/19 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/api/chat.ts` | API layer for all chat endpoints | VERIFIED (233 lines) | All functions imported by components: getRooms, getRoom, getMessages, sendMessage, createDirectRoom, markMessagesRead, confirmWalk, cancelWalkConfirm, leaveRoom, getMyReview, createReview |
| `src/store/useChatStore.ts` | Zustand store for chat state | VERIFIED (169 lines) | All required actions including mergeMessages (shallow equality), removeRoom, clearMessages |
| `src/hooks/useChatWebSocket.ts` | STOMP connection manager hook | VERIFIED (183 lines) | STOMP Client, polling fallback with mergeMessages, JWT refresh, exposed disconnect() method |
| `src/components/chat/ChatList.tsx` | Room list with api/chat.ts | VERIFIED (319 lines) | Uses getRooms() with SliceResponse pagination, visibilitychange re-fetch listener |
| `src/components/chat/ChatStartModal.tsx` | Direct chat creation | VERIFIED (244 lines) | Uses createDirectRoom/getFollowing/searchMembers, DisplayUser unified type |
| `src/app/chat/[id]/page.tsx` | Chat room page orchestrator | VERIFIED (411 lines) | Full orchestration including ConfirmModal for leave, WS disconnect-first pattern |
| `src/components/chat/MessageList.tsx` | Message list with cursor scroll | VERIFIED (225 lines) | IntersectionObserver, messages.length-based auto-scroll, StatusIcon, pending/failed rendering |
| `src/components/chat/ChatHeader.tsx` | Header with participant info | VERIFIED (225 lines) | Connection mode dot, walk confirm button with count, MoreVertical with leave |
| `src/components/chat/ChatInput.tsx` | Input with 500 char limit | VERIFIED (97 lines) | maxLength={500}, character counter, red at >=450, quick reply chips |
| `src/components/shared/modals/WalkReviewModal.tsx` | Review modal | VERIFIED (183 lines) | createReview from api/chat, star rating 1-5, tags-to-comment, success auto-close |
| `src/components/chat/ProfileExplorer.tsx` | Profile panel | VERIFIED (46 lines) | partnerId as number, String() conversion at ProfileView boundary |
| `src/components/dashboard/RecentFriends.tsx` | Friend cards with flex-shrink-0 | VERIFIED (84 lines) | min-w-[160px] max-w-[200px] flex-shrink-0 on cards |
| `src/app/dashboard/page.tsx` | Dashboard with partner memberId | VERIFIED (123 lines) | getRoom per room, extracts partner.memberId for friend id |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| ChatList.tsx | api/chat.ts | getRooms() | WIRED | Line 9: import { getRooms } from '@/api/chat' |
| ChatStartModal.tsx | api/chat.ts | createDirectRoom() | WIRED | Line 14: import { createDirectRoom } from '@/api/chat' |
| useChatWebSocket.ts | @stomp/stompjs | Client import | WIRED | Line 4: import { Client } from '@stomp/stompjs'; package.json: "^7.3.0" |
| useChatWebSocket.ts polling | useChatStore.mergeMessages | mergeMessages call | WIRED | Line 53: selects mergeMessages; Line 69: mergeMessages(result.content) |
| page.tsx | useChatWebSocket.ts | useChatWebSocket hook | WIRED | Line 24+71: import and useChatWebSocket(roomId, !!room) |
| page.tsx mark-read | latestMessageId primitive | useEffect dep | WIRED | Line 122-123: latestMessageId derived; Line 143: useEffect dep on latestMessageId |
| page.tsx handleLeaveConfirm | disconnect() | WS disconnect before API | WIRED | Line 266: disconnect() called before leaveRoom(roomId) |
| page.tsx handleLeaveConfirm | useChatStore | removeRoom + clearMessages | WIRED | Lines 270-271: clearMessages() then removeRoom(roomId) |
| page.tsx | ConfirmModal | Leave confirmation | WIRED | Line 30: import ConfirmModal; Lines 399-408: rendered with variant="danger" |
| WalkReviewModal.tsx | api/chat.ts | createReview | WIRED | Line 10: import { createReview }; called at line 52 |
| dashboard/page.tsx | api/chat.ts | getRoom per room | WIRED | Line 7: import { getRooms, getRoom }; Line 64: getRoom(r.chatRoomId) |
| dashboard/page.tsx | RecentFriends | partner.memberId as id | WIRED | Line 77: id: String(partner.memberId) |
| MessageList.tsx auto-scroll | messages.length | useEffect dep | WIRED | Lines 65, 77: deps are [messages.length] and [messages.length, pendingMessages.length] |
| ChatList.tsx | visibilitychange | re-fetch on tab focus | WIRED | Lines 48-54: addEventListener('visibilitychange') triggers fetchRooms() |
| Old chatService | (deleted) | No remaining imports | VERIFIED | chatService.ts does not exist; grep finds zero imports across src/ |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|------------|-------------|--------|----------|
| CHAT-01 | 08-01 | Chat room list with loading/empty/error states | SATISFIED | ChatList.tsx with three states + retry button |
| CHAT-02 | 08-02 | Chat room detail view | SATISFIED | page.tsx loads room via getRoom() |
| CHAT-03 | 08-01, 08-04 | 1:1 direct chat creation with room reuse | SATISFIED | ChatStartModal calls createDirectRoom(); polling fix preserves chat flow |
| CHAT-04 | 08-02, 08-04 | Cursor-based message pagination | SATISFIED | getMessages with cursor/size, prependMessages on scroll; mergeMessages prevents flicker |
| CHAT-05 | 08-02, 08-04 | Message sending | SATISFIED | sendMessage with optimistic addPendingMessage; mergeMessages preserves pending across polls |
| CHAT-06 | 08-02 | Message status real-time (created/delivered/read) | SATISFIED | StatusIcon + STOMP event handlers |
| CHAT-07 | 08-02 | Failed message retry bubbles | SATISFIED | PendingMessage with failed status + RefreshCw retry button |
| CHAT-08 | 08-03, 08-05 | Leave room | SATISFIED | ConfirmModal(danger) + disconnect-first + store cleanup + redirect |
| CHAT-09 | 08-03, 08-05 | Walk confirmation | SATISFIED | confirmWalk/cancelWalkConfirm with count display |
| CHAT-10 | 08-03 | One-time non-editable review creation | SATISFIED | createReview with score + comment; myReview.exists check |
| CHAT-11 | 08-03 | My review check | SATISFIED | getMyReview fetched on mount; review button gated by exists flag |
| CHAT-12 | 08-01, 08-05 | WebSocket STOMP with JWT auth | SATISFIED | useChatWebSocket with Client, beforeConnect JWT refresh, exposed disconnect() |
| CHAT-13 | 08-02, 08-05 | 500-char message limit + 5s polling fallback | SATISFIED | maxLength={500} + POLLING_INTERVAL = 5000 |
| CHAT-14 | 08-01, 08-05 | Group chat display | SATISFIED | ChatList shows "그룹" badge; ChatHeader shows group pet names |

No orphaned requirements. All 14 CHAT-* requirements mapped to Phase 8 in REQUIREMENTS.md are covered by plans and verified in code.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| (none) | - | - | - | All 13 verified files clean of TODO/FIXME/PLACEHOLDER/HACK markers |

No stub patterns detected. No empty return components. No console.log-only handlers. No unused state variables.

### Human Verification Required

### 1. WebSocket STOMP Connection

**Test:** Open a chat room with a running backend that supports WebSocket STOMP
**Expected:** Connection mode indicator dot turns green (emerald); messages arrive in real-time without page refresh
**Why human:** Requires live backend with WebSocket support to verify actual connection establishment

### 2. Polling Fallback Stability

**Test:** Open a chat room without WebSocket support (or disconnect network briefly)
**Expected:** Connection dot turns amber (polling mode); messages update every 5 seconds without flickering, infinite loops, or scroll jumps
**Why human:** Requires simulating WebSocket failure at runtime; the core fix from plan 08-04

### 3. Scroll Position Preservation on Load-Older

**Test:** Scroll up in a chat room with many messages until sentinel triggers load-older
**Expected:** Older messages prepend without scroll position jumping; user stays at the same visual position
**Why human:** Scroll behavior depends on browser rendering timing and DOM measurement

### 4. Review Flow End-to-End

**Test:** In a chat room where all participants have confirmed walk, click review button, rate 1-5 stars, select tags, submit
**Expected:** Review submits successfully, review button disappears, success animation plays
**Why human:** Full user flow spanning multiple states and modal interaction

### 5. Leave Room with ConfirmModal

**Test:** Click three-dot menu, select leave, see ConfirmModal (danger variant), confirm
**Expected:** Modal matches service design, WebSocket disconnects, room removed from list, no persistent error notifications on subsequent pages
**Why human:** Requires verifying visual design consistency and absence of error toasts post-leave

### 6. Dashboard Friend Cards Sizing

**Test:** Navigate to dashboard with 1 recent friend vs 5+ recent friends
**Expected:** Cards maintain 160-200px width in both cases; single card does not stretch to full width
**Why human:** Visual layout verification at different data counts

### 7. Dashboard Friend Profile Navigation

**Test:** Click a friend card on dashboard
**Expected:** Navigates to /profile/{memberId} and loads the correct member profile (not a chat room ID)
**Why human:** Requires running backend to verify profile loads correctly

### Gaps Summary

No gaps found. All 19 observable truths are verified through code inspection. All 14 requirements (CHAT-01 through CHAT-14) are satisfied with implementation evidence. All key links are wired. The 4 UAT-identified issues (message flickering, send failure, leave room bugs, dashboard friend bugs) have been addressed by plans 08-04 and 08-05 with verifiable code changes:

1. **Message flickering (UAT #3, #4):** mergeMessages with shallow equality check replaces naive setMessages in polling path; mark-read useEffect depends on latestMessageId primitive; auto-scroll depends on messages.length.
2. **Leave room (UAT #9):** ConfirmModal replaces window.confirm; disconnect() called before leaveRoom API; clearMessages + removeRoom cleanup store; ChatList re-fetches on visibilitychange.
3. **Dashboard friends (UAT #11):** flex-shrink-0 + max-w-[200px] on cards; getRoom per room extracts partner.memberId for correct profile navigation.

Old chatService has been fully removed with zero remaining imports. The @stomp/stompjs dependency is installed at ^7.3.0. Seven items require human verification with a running backend.

---

_Verified: 2026-03-07T10:30:00Z_
_Verifier: Claude (gsd-verifier)_
