---
status: diagnosed
phase: 08-chat-system
source: [08-01-SUMMARY.md, 08-02-SUMMARY.md, 08-03-SUMMARY.md]
started: 2026-03-07T00:00:00Z
updated: 2026-03-07T00:10:00Z
---

## Current Test
<!-- OVERWRITE each test - shows where we are -->

[testing complete]

## Tests

### 1. Chat Room List
expected: Navigate to /chat. Room list loads showing your chat rooms with participant names and last message preview. Loading spinner shows while fetching. If no rooms exist, an empty state message appears.
result: pass

### 2. Start New Chat
expected: Click the new chat button on /chat. A modal opens showing users you follow. Select a user and confirm. A new direct chat room is created and you are navigated to the room.
result: pass

### 3. Chat Room Messages
expected: Open a chat room. Message history loads with newest messages visible first. Scroll up to load older messages (cursor-based pagination). Scroll position is preserved when older messages load.
result: issue
reported: "메시지가 계속 보였다 안보였다하고 있어. 서버에 기록을 보니 계속 호출하고 있는 로그가 보이고, 심지어 나의 메시지는 전송이 안되고 있어."
severity: blocker

### 4. Send Message
expected: Type a message in the chat input and press send. Message appears immediately in the list (optimistic). If send fails, a retry bubble appears on the failed message allowing you to resend.
result: issue
reported: "retry bubble은 보여. 그렇지만 계속 메시지 전송이 실패하고 있어."
severity: blocker

### 5. Message Status Indicators
expected: Sent messages show status icons: single check for created, double check for delivered, filled double check for read.
result: skipped
reason: 메시지 전송 자체가 안 되어서 확인 불가

### 6. Chat Input Character Limit
expected: Chat input enforces a 500-character maximum. A character counter is visible and turns red when approaching the limit (450+). Cannot type beyond 500 characters.
result: pass

### 7. Connection Mode Indicator
expected: Chat header shows a connection status indicator dot. Green when connected via WebSocket, yellow/orange when using polling fallback.
result: pass

### 8. Walk Confirm Toggle
expected: In a chat room, a walk confirm button (check circle icon) is visible. Click to confirm walk participation. Shows participant count (e.g., 1/2 confirmed). Click again to cancel confirmation.
result: pass

### 9. Leave Room
expected: In the chat room, click the more options menu (three dots). Select "Leave". A confirmation dialog appears. Confirm to leave the room and be redirected to /chat.
result: issue
reported: "채팅방은 나가지는데, 나가겠냐는 모달/dialog 디자인이 서비스 디자인과 안 맞음. 그리고 나간 이후 어느 페이지를 가든 채팅방 접속 권한이 없다는 알림이 계속 나타나고, 챗룸 목록에서 해당 채팅방이 여전히 존재함."
severity: blocker

### 10. Walk Review Modal
expected: After all participants confirm the walk (allConfirmed), a review button becomes visible. Click it to open a review modal with score input and tag selection. Tags are submitted as part of the comment. Review button disappears after successful submission.
result: skipped
reason: 나중에 확인 예정

### 11. Dashboard Chat Rooms
expected: Navigate to the dashboard. Recent chat rooms section loads and displays rooms correctly (migrated from old chatService to new API layer).
result: issue
reported: "최근 산책한 친구들 목록이 1개일 때 엄청 크게 보임 (5개 이상일 때는 적절). 클릭하면 '프로필을 불러오는데 실패했습니다' 에러 표시됨."
severity: major

## Summary

total: 11
passed: 5
issues: 4
pending: 0
skipped: 2

## Gaps

- truth: "Message history loads stably and messages can be sent successfully"
  status: failed
  reason: "User reported: 메시지가 계속 보였다 안보였다하고 있어. 서버에 기록을 보니 계속 호출하고 있는 로그가 보이고, 심지어 나의 메시지는 전송이 안되고 있어."
  severity: blocker
  test: 3
  root_cause: "Polling fallback replaces entire messages array every 5s via naive setMessages, triggering cascading re-renders in mark-read useEffect (infinite API calls) and MessageList scroll effect (visual flicker)"
  artifacts:
    - path: "aini-inu-frontend/src/hooks/useChatWebSocket.ts"
      issue: "Polling calls setMessages(result.content) doing wholesale replacement instead of merging/diffing"
    - path: "aini-inu-frontend/src/store/useChatStore.ts"
      issue: "setMessages is naive setter with no shallow-equality check, always creates new array reference"
    - path: "aini-inu-frontend/src/app/chat/[id]/page.tsx"
      issue: "Mark-read useEffect depends on messages array reference, fires every poll cycle"
    - path: "aini-inu-frontend/src/components/chat/MessageList.tsx"
      issue: "Auto-scroll useEffect depends on messages array, fires every poll cycle causing scroll jumps"
  missing:
    - "Polling should merge not replace — use addMessage with dedup instead of setMessages"
    - "setMessages needs shallow equality check before updating state"
    - "Mark-read effect should depend on latestMessageId (primitive) not messages array"
    - "Auto-scroll effect should depend on messages.length or latest ID not full array"
  debug_session: ".planning/debug/chat-flickering-infinite-loop.md"
- truth: "Leave room redirects cleanly without persistent errors, and room is removed from list"
  status: failed
  reason: "User reported: 채팅방은 나가지는데, 나가겠냐는 모달/dialog 디자인이 서비스 디자인과 안 맞음. 나간 이후 어느 페이지를 가든 채팅방 접속 권한이 없다는 알림이 계속 나타나고, 챗룸 목록에서 해당 채팅방이 여전히 존재함."
  severity: blocker
  test: 9
  root_cause: "handleLeave only calls leaveRoom API and navigates — does not disconnect WebSocket, clear store state, or remove room from cached list. window.confirm used instead of existing ConfirmModal component."
  artifacts:
    - path: "aini-inu-frontend/src/app/chat/[id]/page.tsx"
      issue: "handleLeave does not disconnect WS, does not clear store, uses window.confirm"
    - path: "aini-inu-frontend/src/hooks/useChatWebSocket.ts"
      issue: "No exposed disconnect() method for imperative teardown; cleanup only via React effect unmount"
    - path: "aini-inu-frontend/src/store/useChatStore.ts"
      issue: "Missing removeRoom(roomId) action to evict room from cached rooms array"
    - path: "aini-inu-frontend/src/components/chat/ChatList.tsx"
      issue: "No re-fetch trigger when navigating back to /chat; relies on stale mount-time data"
  missing:
    - "Expose disconnect() from useChatWebSocket or set enabled flag to false before navigating"
    - "Add removeRoom(roomId) action to useChatStore and call it in handleLeave"
    - "ChatList should re-fetch rooms on navigation or invalidate cache on leave"
    - "Replace window.confirm with existing ConfirmModal component (variant=danger)"
  debug_session: ".planning/debug/chat-leave-room-bugs.md"
- truth: "Dashboard recent chat rooms display correctly at any count and navigate properly on click"
  status: failed
  reason: "User reported: 최근 산책한 친구들 목록이 1개일 때 엄청 크게 보임 (5개 이상일 때는 적절). 클릭하면 '프로필을 불러오는데 실패했습니다' 에러 표시됨."
  severity: major
  test: 11
  root_cause: "Flex cards lack flex-shrink-0 so single item stretches to fill container. Dashboard maps chatRoomId as friend id instead of partner memberId, causing profile page to request non-existent member."
  artifacts:
    - path: "aini-inu-frontend/src/components/dashboard/RecentFriends.tsx"
      issue: "Cards have min-w-[160px] but no max-w or flex-shrink-0, single item stretches"
    - path: "aini-inu-frontend/src/app/dashboard/page.tsx"
      issue: "Maps id: String(r.chatRoomId) instead of partner memberId"
  missing:
    - "Add flex-shrink-0 and max-w constraint to card elements in RecentFriends"
    - "Use partner memberId instead of chatRoomId for friend id (may need getRoom per room or backend change)"
  debug_session: ".planning/debug/dashboard-recent-friends.md"
- truth: "Message sends successfully and appears in chat"
  status: failed
  reason: "User reported: retry bubble은 보여. 그렇지만 계속 메시지 전송이 실패하고 있어."
  severity: blocker
  test: 4
  root_cause: "Related to Test 3 root cause — polling wholesale replacement causes state thrashing that interferes with optimistic send flow. Send failure itself may be auth/environment issue (endpoint and payload shape match backend correctly)."
  artifacts:
    - path: "aini-inu-frontend/src/hooks/useChatWebSocket.ts"
      issue: "Polling overwrites messages including pending/optimistic ones"
    - path: "aini-inu-frontend/src/app/chat/[id]/page.tsx"
      issue: "Optimistic send flow disrupted by polling-triggered re-renders"
  missing:
    - "Fix polling merge logic (same as Test 3) to preserve pending messages"
    - "Verify auth token is present in send request headers"
  debug_session: ".planning/debug/chat-flickering-infinite-loop.md"
