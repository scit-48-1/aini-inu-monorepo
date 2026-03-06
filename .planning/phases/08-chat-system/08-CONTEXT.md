# Phase 8: Chat System - Context

**Gathered:** 2026-03-07
**Status:** Ready for planning

<domain>
## Phase Boundary

Real-time chat system: room list/detail, 1:1 direct chat, message send/receive with cursor pagination, WebSocket STOMP live updates with REST polling fallback, walk confirmation, post-walk reviews. Frontend-only modifications (aini-inu-frontend/).

**API endpoints in scope (13 endpoints):**
- `GET /api/v1/chat-rooms` (FR-CHAT-001, room list -- SliceResponse)
- `GET /api/v1/chat-rooms/{id}` (FR-CHAT-001, room detail)
- `POST /api/v1/chat-rooms/direct` (FR-CHAT-001A, 1:1 direct create -- reuses existing room)
- `GET /api/v1/chat-rooms/{id}/messages` (FR-CHAT-002, cursor-based messages)
- `POST /api/v1/chat-rooms/{id}/messages` (FR-CHAT-002, send message)
- `POST /api/v1/chat-rooms/{id}/messages/read` (FR-CHAT-002A, mark read)
- `GET /api/v1/chat-rooms/{id}/walk-confirm` (FR-CHAT-004, walk confirm status)
- `POST /api/v1/chat-rooms/{id}/walk-confirm` (FR-CHAT-004, confirm walk)
- `DELETE /api/v1/chat-rooms/{id}/walk-confirm` (FR-CHAT-004, cancel confirm)
- `POST /api/v1/chat-rooms/{id}/leave` (FR-CHAT-004, leave room)
- `POST /api/v1/chat-rooms/{id}/reviews` (FR-CHAT-004, create review)
- `GET /api/v1/chat-rooms/{id}/reviews` (FR-CHAT-004, list reviews)
- `GET /api/v1/chat-rooms/{id}/reviews/me` (FR-CHAT-004, my review check)

**Applicable DEC policies:**
- DEC-007: Review once per room/target, no edit, no re-write
- DEC-015: Failed send shows retry bubble with resend button
- DEC-016: Polling fallback interval 5 seconds
- DEC-017: Messages cursor-paginated, newest first, scroll up for older
- DEC-021: WebSocket STOMP for real-time (created/delivered/read), REST polling fallback

**PRD constraints:**
- Chat message max 500 chars (PRD SS8.1)
- Group chat capacity 3-10 members (PRD SS8.1)
- Review score 1-5 stars (PRD SS8.1)
- 5-state UI coverage: default/loading/empty/error/success (PRD SS8.3)

**Modification scope:** aini-inu-frontend/ only. Backend and common-docs are read-only.

</domain>

<decisions>
## Implementation Decisions

### WebSocket + Polling Strategy
- Attempt WebSocket STOMP connection to `/ws/chat-rooms/{roomId}` with JWT auth on connect
- On successful WS: receive real-time events for message created/delivered/read status updates
- On WS disconnect or connection failure: fall back to REST polling at 5-second intervals (DEC-016)
- Polling fetches messages via `GET /chat-rooms/{id}/messages` cursor endpoint

### Message Loading + Pagination
- Cursor-based pagination: newest messages first (DEC-017)
- Initial load: fetch latest page, render at bottom of scroll area
- Scroll up to load older messages (reverse infinite scroll)
- New incoming messages (via WS or poll) append at bottom

### Message Send + Retry
- Send via `POST /chat-rooms/{id}/messages` with content, messageType, clientMessageId (UUID)
- Max 500 chars enforced on input (PRD SS8.1)
- On send failure: message stays in list as failed bubble with retry button (DEC-015)
- Retry re-sends same clientMessageId for idempotency

### Message Status Indicators
- Show status per message: created / delivered / read (DEC-021)
- Status updates arrive via WebSocket events or polling
- Mark messages read via `POST /chat-rooms/{id}/messages/read` when room is open

### Room List
- Fetch via `GET /chat-rooms` with SliceResponse pagination
- Loading/empty/error states (CHAT-01)
- Group chat rooms display participant count; 1:1 rooms show partner info
- Active/past tab filtering based on room status

### 1:1 Direct Chat
- `POST /chat-rooms/direct` with partnerId
- Reuses existing room if one exists (FR-CHAT-001A)
- ChatStartModal rewired to use `api/chat.ts` createDirectRoom

### Walk Confirm
- Walk confirm actions surfaced in chat room UI (header or action bar)
- `GET /walk-confirm` to check current state on room entry
- `POST /walk-confirm` with action to confirm
- `DELETE /walk-confirm` to cancel confirmation
- Show confirm state per participant; allConfirmed triggers visual indicator

### Leave Room
- Leave action via `POST /chat-rooms/{id}/leave`
- Confirmation dialog before leaving
- After leave: redirect to room list, room shows left state

### Review
- Rewire existing WalkReviewModal to use `api/chat.ts` createReview
- Check `GET /reviews/me` on room entry to determine if review already written
- One-time per room/target, no edit (DEC-007)
- Score 1-5, comment field
- Review button only visible after walk is confirmed

### Claude's Discretion
- WebSocket/STOMP library choice (e.g., @stomp/stompjs, sockjs-client)
- Exact retry bubble visual design
- Walk confirm button placement (header vs action bar vs inline)
- Message status indicator icons/text
- Scroll position management details for cursor pagination
- Group chat participant list layout
- Quick reply chip content

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets
- `api/chat.ts`: All 13 endpoint functions fully typed (ChatRoomSummaryResponse, ChatRoomDetailResponse, ChatMessageResponse, ChatReviewCreateRequest, WalkConfirmResponse, etc.) -- direct replacement for old `services/api/chatService.ts`
- `api/types.ts`: SliceResponse, CursorResponse, CursorPaginationParams types ready
- `components/chat/ChatList.tsx`: Room list UI with search, tabs, room cards -- needs rewire from old chatService to `api/chat.ts`
- `components/chat/MessageList.tsx`: Message bubble rendering -- needs rewire from `any[]` to ChatMessageResponse, senderId->sender.memberId, timestamp->sentAt
- `components/chat/ChatInput.tsx`: Input with quick replies and send button -- needs 500 char limit, retry integration
- `components/chat/ChatHeader.tsx`: Header with partner info, profile toggle -- needs rewire from UserType to participant data
- `components/chat/ChatStartModal.tsx`: Direct chat creation modal -- needs rewire to createDirectRoom
- `components/shared/modals/WalkReviewModal.tsx`: Star rating + tags + comment UI -- needs rewire to ChatReviewCreateRequest (score + comment, adapt tags to comment string)
- `app/chat/layout.tsx`: Desktop sidebar + main content responsive layout -- functional, minimal changes needed
- `app/chat/page.tsx`: Placeholder page -- functional as-is
- `app/chat/[id]/page.tsx`: Chat room page -- heavy rewire needed (old service, 3s polling, wrong types)

### Established Patterns
- `'use client'` on all pages and components
- Toast-only errors via sonner (Korean messages)
- Optimistic UI with failure rollback (Phase 4/5 pattern)
- Phase 2 API modules: inline types, `apiClient` envelope unwrap
- Zustand stores for global state (`useUserStore`, `useAuthStore`)

### Integration Points
- `services/api/chatService.ts`: OLD module to be fully replaced by `api/chat.ts`
- `types/index.ts` ChatRoom/ChatMessage: Legacy types to be replaced by api/chat.ts types
- `/around-me` thread apply flow: navigates to `/chat/{chatRoomId}` on success (Phase 6)
- `store/useUserStore.ts`: Current user ID for message ownership detection
- `NEXT_PUBLIC_WS_URL` env var: WebSocket URL configured in `.env.local`

</code_context>

<specifics>
## Specific Ideas

- User provided full FR/DEC/DoD specification as acceptance criteria
- WebSocket STOMP at `/ws/chat-rooms/{roomId}` with JWT auth (DEC-021)
- REST polling 5-second fallback when WS unavailable (DEC-016)
- Cursor pagination newest-first with scroll-up for older (DEC-017)
- Failed send retry bubble with resend button (DEC-015)
- Review: 1 time per room/target, no edit/re-write (DEC-007)
- Group chat 3-10 members capacity (PRD SS8.1)

</specifics>

<deferred>
## Deferred Ideas

None -- discussion stayed within phase scope

</deferred>

---

*Phase: 08-chat-system*
*Context gathered: 2026-03-07*
