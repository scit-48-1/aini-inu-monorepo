---
status: diagnosed
trigger: "After leaving a chat room: persistent error notifications, stale room list, window.confirm instead of design-language modal"
created: 2026-03-07T00:00:00Z
updated: 2026-03-07T00:00:00Z
---

## Current Focus

hypothesis: Three independent defects in the leave-room flow — no WS disconnect, no store cleanup, wrong UI pattern
test: Code review of handleLeave, useChatWebSocket cleanup, useChatStore actions, and ChatList re-fetch
expecting: Confirming absence of disconnect/cleanup/re-fetch logic and presence of window.confirm
next_action: return diagnosis

## Symptoms

expected: After leaving a chat room, user is redirected to /chat with the room removed from the list, no further errors, and a styled confirmation dialog before leaving
actual: (1) Persistent "no chat room access" error toasts on every page, (2) left room still visible in chat list, (3) browser-native window.confirm used instead of app's ConfirmModal
errors: "no chat room access" error notifications recurring
reproduction: Enter a chat room, use the kebab menu to leave, observe errors and stale list
started: Since initial chat implementation (phase 08)

## Eliminated

(none)

## Evidence

- timestamp: 2026-03-07T00:01:00Z
  checked: handleLeave in page.tsx (lines 250-260)
  found: Calls leaveRoom(roomId) then router.push('/chat') — does NOT disconnect WebSocket or clear store state
  implication: WebSocket remains active after leave, will keep trying to access the room and get 403 errors

- timestamp: 2026-03-07T00:02:00Z
  checked: useChatWebSocket cleanup (lines 159-164)
  found: Cleanup only runs on unmount/dependency change (roomId, enabled). The `enabled` flag is `!!room` from page state. After leaveRoom(), `room` state is never set to null, and router.push may not unmount fast enough — but more critically, the STOMP client has reconnectDelay=5000 and will auto-reconnect
  implication: Even if component unmounts, the STOMP reconnect loop may fire before deactivate() runs. The handleLeave should explicitly disconnect before navigating.

- timestamp: 2026-03-07T00:03:00Z
  checked: useChatStore for any removeRoom action
  found: No removeRoom action exists. setRooms replaces all rooms. No mechanism to remove a single room from cache.
  implication: After leaving, the Zustand store `rooms` array still contains the left room

- timestamp: 2026-03-07T00:04:00Z
  checked: ChatList component (lines 45-47)
  found: fetchRooms() is called only on mount (useEffect with [fetchRooms] dep). No mechanism to re-fetch when navigating back to /chat after leaving a room.
  implication: ChatList uses stale cached data; the left room persists in the list until a full page refresh

- timestamp: 2026-03-07T00:05:00Z
  checked: handleLeave for confirmation UI (line 251)
  found: Uses `window.confirm('...')` — a browser-native dialog
  implication: Breaks design language. The app has a reusable ConfirmModal component at src/components/common/ConfirmModal.tsx with danger variant support, and DeleteConfirmDialog as another example

- timestamp: 2026-03-07T00:06:00Z
  checked: useChatWebSocket onStompError and onWebSocketError handlers (lines 123-131)
  found: Both handlers fall back to polling mode, which calls getMessages(roomId) every 5 seconds
  implication: After leave, if WS errors out, polling kicks in and repeatedly hits the API for a room the user no longer has access to, causing persistent error notifications

## Resolution

root_cause: The handleLeave function only calls the API and navigates — it does not disconnect the WebSocket, clear the store, or remove the room from the cached room list. Additionally, it uses window.confirm instead of the app's existing ConfirmModal component.
fix: (pending)
verification: (pending)
files_changed: []
