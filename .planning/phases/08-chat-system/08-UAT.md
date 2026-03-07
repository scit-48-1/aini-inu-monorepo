---
status: testing
phase: 08-chat-system
source: [08-04-SUMMARY.md, 08-05-SUMMARY.md]
started: 2026-03-07T01:15:00Z
updated: 2026-03-07T01:25:00Z
---

## Current Test
<!-- OVERWRITE each test - shows where we are -->

number: 1
name: Message Stability (No Flickering)
expected: |
  Open a chat room with existing messages. Messages load and remain stable — no flickering, no disappearing/reappearing. Server logs should NOT show rapid repeated API calls. Messages stay visible continuously.
awaiting: user response (retest after hotfix)

## Tests

### 1. Message Stability (No Flickering)
expected: Open a chat room with existing messages. Messages load and remain stable — no flickering, no disappearing/reappearing. Server logs should NOT show rapid repeated API calls. Messages stay visible continuously.
result: issue
reported: "메시지가 계속 보였다 안보였다하고 있어. 서버에 채팅방의 갯수만큼 select요청이 온거 같아. 메시지 전송도 안되고 CH400_INVALID_REQUEST. 한 채팅방에서 실패한 메시지가 다른 채팅방들에도 나타남."
severity: blocker
hotfix: "messageType 'TEXT' → 'USER' (백엔드는 USER/SYSTEM만 허용), clearMessages() on room switch (실패메시지 방간 공유 방지)"

### 2. Send Message Successfully
expected: Type a message in the chat input and press send. Message appears immediately (optimistic send). The message persists and is confirmed by the server. No retry bubble appears for successful sends.
result: [pending]

### 3. Message Status Indicators
expected: Sent messages show status icons: single check for created, double check for delivered, filled double check for read.
result: [pending]

### 4. Leave Room — ConfirmModal Dialog
expected: In the chat room, click the more options menu (three dots). Select "Leave". A styled ConfirmModal with danger variant appears (red-themed, matching service design) — NOT the browser native confirm dialog.
result: [pending]

### 5. Leave Room — Clean Redirect
expected: After confirming leave in the modal, you are redirected to /chat. No error notifications appear on any page. The left room no longer appears in the chat room list.
result: [pending]

### 6. Dashboard Recent Friends — Layout
expected: Navigate to the dashboard. Recent friends section displays friend cards at consistent sizes (160-200px width). With only 1 friend, the card does NOT stretch to fill the entire container width.
result: [pending]

### 7. Dashboard Recent Friends — Profile Navigation
expected: Click on a friend card in the recent friends section. You are navigated to the correct member's profile page (/profile/{memberId}). No "프로필을 불러오는데 실패했습니다" error appears.
result: [pending]

### 8. Walk Review Modal
expected: After all participants confirm the walk (allConfirmed), a review button becomes visible. Click it to open a review modal with score input and tag selection. Tags are submitted as part of the comment. Review button disappears after successful submission.
result: [pending]

## Summary

total: 8
passed: 0
issues: 1
pending: 7
skipped: 0

## Gaps

- truth: "Message history loads stably without flickering and messages can be sent"
  status: failed
  reason: "User reported: messageType 'TEXT' not recognized by backend (expects 'USER'/'SYSTEM'), causing CH400_INVALID_REQUEST. Failed messages shared across rooms due to global store without clearMessages on room switch."
  severity: blocker
  test: 1
  root_cause: "Frontend sends messageType:'TEXT' but backend ChatMessageType enum only has USER/SYSTEM. Store messages/pendingMessages are global and not cleared on room switch."
  artifacts:
    - path: "aini-inu-frontend/src/app/chat/[id]/page.tsx"
      issue: "messageType: 'TEXT' instead of 'USER' in sendMessage calls; no clearMessages() on roomId change"
  missing:
    - "Change messageType from 'TEXT' to 'USER'"
    - "Call clearMessages() at start of roomId useEffect"
