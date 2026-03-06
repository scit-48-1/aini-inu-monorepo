---
status: diagnosed
trigger: "Messages keep appearing and disappearing (flickering) in chat room. Server logs show continuous API calls (infinite loop). Messages fail to send."
created: 2026-03-07T00:00:00Z
updated: 2026-03-07T00:00:00Z
---

## Current Focus

hypothesis: Multiple compounding bugs -- polling overwrites store causing flicker, mark-read effect re-triggers on every messages change creating loop, and WS connection failure cascades into polling mode amplifying all issues
test: Code trace through all files
expecting: N/A -- diagnosis complete
next_action: Return structured diagnosis

## Symptoms

expected: Messages display stably, send successfully, no excessive API calls
actual: Messages flicker (appear/disappear), infinite API calls in server logs, message sending fails
errors: Continuous API calls visible in server logs
reproduction: Open any chat room, observe flickering and network tab
started: Phase 08 implementation

## Eliminated

(none -- diagnosis on first pass)

## Evidence

- timestamp: 2026-03-07T00:01:00Z
  checked: useChatWebSocket.ts polling fallback (lines 62-73)
  found: startPolling calls setMessages(result.content) every 5s, which REPLACES the entire messages array. This overwrites any optimistic pending state and causes visual flicker as messages disappear then reappear.
  implication: Root cause of flickering -- polling replaces messages wholesale instead of merging

- timestamp: 2026-03-07T00:02:00Z
  checked: page.tsx mark-read useEffect (lines 116-136)
  found: useEffect depends on [room, messages, roomId]. setMessages from polling creates a new array reference every 5s, which triggers this effect. This effect calls markMessagesRead API every 2s (debounced). Combined with polling every 5s, this creates a continuous stream of API calls.
  implication: Infinite API call loop -- polling triggers mark-read, both running on tight intervals

- timestamp: 2026-03-07T00:03:00Z
  checked: useChatStore.ts setMessages (line 109)
  found: setMessages is a naive replacement: `set({ messages: msgs })`. No dedup, no merge with existing state. Every poll response replaces the entire array, creating new object references for all messages.
  implication: Every poll cycle causes a full re-render of MessageList even if data hasn't changed

- timestamp: 2026-03-07T00:04:00Z
  checked: useChatWebSocket.ts WS error handlers (lines 118-131)
  found: onDisconnect, onStompError, and onWebSocketError all call startPolling(). If WS cannot connect (common in dev without WS server), it immediately falls back to polling, triggering the polling-based infinite loop.
  implication: In dev environment without WS server, polling is always active, making all issues worse

- timestamp: 2026-03-07T00:05:00Z
  checked: useChatWebSocket.ts useEffect dependencies (line 166)
  found: The main useEffect depends on [roomId, enabled] but captures addMessage, deduplicateMessage, updateMessageStatus, setMessages via closure from component scope (lines 50-53). These are zustand selectors that return new function refs on each render if the store shape changes. However, zustand's create() returns stable references so this is not the primary loop driver.
  implication: Not the primary bug, but the polling fallback is the real trigger

- timestamp: 2026-03-07T00:06:00Z
  checked: Message send flow in page.tsx (lines 158-192) and api/chat.ts sendMessage (lines 159-167)
  found: sendMessage calls apiClient.post to `/chat-rooms/${chatRoomId}/messages` with body {content, messageType, clientMessageId}. The endpoint and payload match the backend ChatController.createMessage. The API layer and endpoint are correctly wired. Send failures are likely caused by auth issues (no valid JWT token in dev) or the backend not running, not a code bug in the send path itself.
  implication: Message send failure is an environment/auth issue, not a code structural bug

- timestamp: 2026-03-07T00:07:00Z
  checked: MessageList.tsx auto-scroll useEffect (lines 68-77)
  found: Depends on [messages, pendingMessages]. Since polling replaces messages array every 5s, this effect fires every 5s too, causing scroll position jumps which contribute to the visual flickering sensation.
  implication: Scroll behavior compounds the flicker

## Resolution

root_cause: Polling fallback replaces the entire messages array every 5 seconds via setMessages (naive overwrite), which triggers cascading re-renders in mark-read useEffect (creating infinite API calls) and MessageList scroll effects (creating visual flicker). The WS connection fails in dev, so polling is always active.
fix: (see diagnosis below)
verification: N/A -- diagnosis only
files_changed: []
