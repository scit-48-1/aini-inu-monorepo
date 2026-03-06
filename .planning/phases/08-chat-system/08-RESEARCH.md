# Phase 8: Chat System - Research

**Researched:** 2026-03-07
**Domain:** Real-time chat UI (WebSocket STOMP + REST fallback), cursor pagination, walk confirmation, reviews
**Confidence:** HIGH

## Summary

Phase 8 is a frontend-only rewire of the existing chat components to use the `api/chat.ts` module (already built in Phase 2) and add real-time WebSocket STOMP connectivity, cursor-based message pagination, retry bubbles, walk confirmation, and reviews. The backend WebSocket infrastructure is fully implemented: STOMP endpoint at `/ws/chat-rooms/{roomId}`, JWT auth via CONNECT headers, and three event types published to `/topic/chat-rooms/{roomId}/events` (CHAT_MESSAGE_CREATED, CHAT_MESSAGE_DELIVERED, CHAT_MESSAGE_READ).

The existing chat components (ChatList, MessageList, ChatInput, ChatHeader, ChatStartModal, WalkReviewModal) all exist but are wired to the old `services/api/chatService.ts` with incorrect types (string IDs, `any[]` messages, wrong field names). The primary work is: (1) rewire to `api/chat.ts` with proper types, (2) add `@stomp/stompjs` for WebSocket with polling fallback, (3) implement reverse infinite scroll for cursor pagination, (4) add retry bubble for failed sends, (5) wire walk confirm and review features.

**Primary recommendation:** Install `@stomp/stompjs` (no SockJS needed -- backend endpoint does not use SockJS), build a `useChatWebSocket` hook that manages STOMP connection lifecycle with automatic polling fallback, and rewire all existing components to `api/chat.ts` types incrementally.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- WebSocket STOMP connection to `/ws/chat-rooms/{roomId}` with JWT auth on CONNECT
- On WS disconnect/failure: fall back to REST polling at 5-second intervals (DEC-016)
- Cursor-based pagination: newest messages first (DEC-017)
- Send via `POST /chat-rooms/{id}/messages` with content, messageType, clientMessageId (UUID)
- Max 500 chars enforced on input (PRD SS8.1)
- On send failure: message stays in list as failed bubble with retry button (DEC-015)
- Retry re-sends same clientMessageId for idempotency
- Message status per message: created / delivered / read (DEC-021)
- Mark messages read via `POST /chat-rooms/{id}/messages/read` when room is open
- Room list with SliceResponse pagination, loading/empty/error states
- 1:1 direct chat via `POST /chat-rooms/direct` with partnerId, reuses existing room
- Walk confirm: GET/POST/DELETE endpoints, confirm state per participant, allConfirmed visual
- Leave room via `POST /chat-rooms/{id}/leave`, confirmation dialog, redirect to list
- Review: rewire WalkReviewModal to `api/chat.ts` createReview, check `GET /reviews/me` first
- Review: one-time per room/target, no edit (DEC-007), score 1-5
- Review button only visible after walk confirmed
- Group chat capacity 3-10 members (PRD SS8.1)
- 5-state UI coverage: default/loading/empty/error/success (PRD SS8.3)

### Claude's Discretion
- WebSocket/STOMP library choice (recommendation: @stomp/stompjs)
- Exact retry bubble visual design
- Walk confirm button placement (header vs action bar vs inline)
- Message status indicator icons/text
- Scroll position management details for cursor pagination
- Group chat participant list layout
- Quick reply chip content

### Deferred Ideas (OUT OF SCOPE)
None -- discussion stayed within phase scope
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| CHAT-01 | Room list with loading/empty/error states | Rewire ChatList from old chatService to `getRooms()` with SliceResponse; apply INFRA-07 state pattern |
| CHAT-02 | Room detail view | Rewire to `getRoom()` returning ChatRoomDetailResponse with participants array |
| CHAT-03 | 1:1 direct chat, reuses existing room | Rewire ChatStartModal to `createDirectRoom({ partnerId: number })` |
| CHAT-04 | Cursor-based message loading, newest first, scroll up for older | Use `getMessages()` with CursorResponse; reverse infinite scroll pattern |
| CHAT-05 | Message send | Use `sendMessage()` with ChatMessageCreateRequest (content, messageType, clientMessageId UUID) |
| CHAT-06 | Real-time message status via WebSocket STOMP | `@stomp/stompjs` Client subscribing to `/topic/chat-rooms/{id}/events`; three event types |
| CHAT-07 | Failed send retry bubble | Optimistic append with pending state; on error show retry button; re-send with same clientMessageId |
| CHAT-08 | Leave room | `leaveRoom()` API call with confirmation dialog |
| CHAT-09 | Walk confirmation | `getWalkConfirm()`, `confirmWalk()`, `cancelWalkConfirm()` APIs; UI in chat room header/action bar |
| CHAT-10 | Review creation, one-time, no edit | Rewire WalkReviewModal to `createReview()`; map tags to comment string |
| CHAT-11 | My review check | `getMyReview()` returns `{ exists, review }` to disable review button |
| CHAT-12 | WebSocket STOMP connection with JWT auth | `@stomp/stompjs` Client with `connectHeaders: { Authorization: 'Bearer ...' }` |
| CHAT-13 | 500 char limit, 5s polling fallback | Input maxLength + char counter; polling interval in fallback mode |
| CHAT-14 | Group chat 3-10 member capacity | Display participant count; UI-only enforcement (backend validates) |
</phase_requirements>

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| @stomp/stompjs | ^7.0 | STOMP over WebSocket client | Only maintained STOMP client for browsers; built-in TypeScript types; no SockJS dependency needed; handles reconnect/heartbeat |

### Supporting (Already Installed)
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| zustand | 5.0.11 | State management | Chat room state, connection status, message queue |
| sonner | 2.0.7 | Toast notifications | Error/retry messages in Korean |
| lucide-react | 0.563 | Icons | Message status indicators, retry, walk confirm |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| @stomp/stompjs | sockjs-client + @stomp/stompjs | Backend endpoint does NOT use SockJS (no `withSockJS()`); adding SockJS would be unnecessary overhead |
| @stomp/stompjs | Raw WebSocket | Would need to implement STOMP framing manually; no heartbeat/reconnect |
| Zustand store for messages | React state only | Zustand enables sharing WS connection status across components; worth it for disconnect/reconnect indicator |

**Installation:**
```bash
cd aini-inu-frontend && npm install @stomp/stompjs
```

## Architecture Patterns

### Recommended Project Structure
```
src/
├── api/chat.ts                          # Already built (13 endpoints) -- no changes needed
├── hooks/
│   └── useChatWebSocket.ts              # NEW: STOMP connection manager hook
├── store/
│   └── useChatStore.ts                  # NEW: Chat room state (messages, connection status, pending sends)
├── components/chat/
│   ├── ChatList.tsx                     # REWIRE: old chatService -> api/chat.ts getRooms()
│   ├── ChatHeader.tsx                   # REWIRE: UserType -> ChatParticipantResponse; add walk confirm/leave actions
│   ├── MessageList.tsx                  # REWIRE: any[] -> ChatMessageResponse; add cursor scroll, status indicators, retry bubble
│   ├── ChatInput.tsx                    # MODIFY: add 500 char limit, character counter
│   ├── ChatStartModal.tsx              # REWIRE: old chatService -> createDirectRoom()
│   └── ProfileExplorer.tsx             # MINIMAL: partnerId string -> number
├── components/shared/modals/
│   └── WalkReviewModal.tsx             # REWIRE: onReviewSubmit -> ChatReviewCreateRequest (score + comment)
└── app/chat/
    ├── layout.tsx                       # NO CHANGE
    ├── page.tsx                         # NO CHANGE
    └── [id]/page.tsx                    # HEAVY REWIRE: orchestrate WS + polling + all new features
```

### Pattern 1: WebSocket + Polling Dual Mode
**What:** A hook that tries WebSocket STOMP first, falls back to 5s polling on failure, and automatically reconnects when WS becomes available again.
**When to use:** Chat room detail page (`/chat/[id]`)
**Example:**
```typescript
// useChatWebSocket.ts
import { Client, IMessage } from '@stomp/stompjs';
import { useAuthStore } from '@/store/useAuthStore';

type ConnectionMode = 'ws' | 'polling' | 'disconnected';

function createStompClient(roomId: number, token: string): Client {
  // Backend WS URL: must connect directly to backend (not via Next.js rewrite)
  const wsUrl = process.env.NEXT_PUBLIC_WS_URL || 'ws://localhost:8080';
  return new Client({
    brokerURL: `${wsUrl}/ws/chat-rooms/${roomId}`,
    connectHeaders: { Authorization: `Bearer ${token}` },
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
  });
}
```

### Pattern 2: Reverse Infinite Scroll (Cursor Pagination)
**What:** Load newest messages first, scroll to bottom on initial load, trigger older page fetch when scrolling to top.
**When to use:** MessageList component
**Example:**
```typescript
// Scroll position preservation on prepend
const scrollContainer = scrollRef.current;
const prevScrollHeight = scrollContainer.scrollHeight;
// prepend older messages...
const newScrollHeight = scrollContainer.scrollHeight;
scrollContainer.scrollTop = newScrollHeight - prevScrollHeight;
```

### Pattern 3: Optimistic Send with Retry
**What:** Immediately append message to UI with 'pending' status, update to 'created' on API success or 'failed' on error. Failed messages show retry button that re-sends with same clientMessageId.
**When to use:** Message sending
**Example:**
```typescript
interface PendingMessage {
  clientMessageId: string;  // crypto.randomUUID()
  content: string;
  status: 'pending' | 'failed';
  sentAt: string;
}
```

### Pattern 4: ChatRealtimeEvent Shape
**What:** Backend publishes events with `{ type, data }` structure to `/topic/chat-rooms/{roomId}/events`
**Event types from backend (verified from source code):**
```typescript
// CHAT_MESSAGE_CREATED
{ type: 'CHAT_MESSAGE_CREATED', data: { roomId: number, message: ChatMessageResponse } }

// CHAT_MESSAGE_DELIVERED
{ type: 'CHAT_MESSAGE_DELIVERED', data: { roomId: number, messageId: number, memberId: number, deliveredAt: string } }

// CHAT_MESSAGE_READ
{ type: 'CHAT_MESSAGE_READ', data: { roomId: number, messageId: number, memberId: number, readAt: string } }
```

### Anti-Patterns to Avoid
- **Polling and WS simultaneously:** Only one mode active at a time. WS success disables polling; WS disconnect enables polling.
- **Refetching entire message list on poll:** Poll should use cursor to fetch only new messages since last known message, not reload everything.
- **Storing WS client in React state:** Store in ref; STOMP Client is mutable and should not trigger re-renders.
- **Connecting WS in render:** Connect in useEffect cleanup pattern only.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| STOMP framing | Manual WebSocket frame parsing | @stomp/stompjs Client | Heartbeat, reconnect, subscription management, error handling |
| UUID generation | Custom ID generator | crypto.randomUUID() | Built into modern browsers; used for clientMessageId |
| Scroll position restoration | Manual scroll math | IntersectionObserver + scrollHeight diff | Reliable across browsers for reverse infinite scroll |
| Debounced polling | Custom setInterval wrapper | useEffect + cleanup pattern with ref | Prevents memory leaks and stale closures |

**Key insight:** The STOMP protocol has enough complexity (framing, heartbeat negotiation, subscription IDs) that using the established library saves significant debugging time.

## Common Pitfalls

### Pitfall 1: WebSocket URL Not Proxied
**What goes wrong:** Next.js `rewrites()` in next.config.ts only proxies `/api/v1/*`. WebSocket connections to `/ws/chat-rooms/*` will fail if routed through Next.js dev server.
**Why it happens:** The current rewrite config does not include WS endpoints.
**How to avoid:** Use `NEXT_PUBLIC_WS_URL` env var pointing directly to backend (`ws://localhost:8080`). The STOMP client must use this direct URL, not a relative path.
**Warning signs:** STOMP connection immediately fails with 404 or upgrade failure.

### Pitfall 2: Token Stale on Reconnect
**What goes wrong:** @stomp/stompjs reconnects automatically, but uses the original `connectHeaders` from initial config. If the JWT expired during disconnect, reconnect fails with 401.
**Why it happens:** The token is captured at Client creation time.
**How to avoid:** Use `beforeConnect` callback to refresh token from `useAuthStore` before each connection attempt.
**Warning signs:** Reconnection loops with auth errors after token refresh.

### Pitfall 3: Old ChatService Type Mismatch
**What goes wrong:** Existing components use `string` IDs (`roomId`, `currentUserId`, `partnerId`), but `api/chat.ts` uses `number` IDs (`chatRoomId: number`, `sender.memberId: number`).
**Why it happens:** Old chatService was prototyped with string IDs; backend uses Long/number.
**How to avoid:** Systematic type conversion during rewire. Key mappings:
- `room.id` (string) -> `room.chatRoomId` (number)
- `msg.senderId` (string) -> `msg.sender.memberId` (number)
- `msg.timestamp` (string) -> `msg.sentAt` (string)
- `msg.content || msg.text` -> `msg.content` (only)
- `room.partner` (UserType) -> `room.participants` (ChatParticipantResponse[])
- `room.lastMessage` (string) -> `room.lastMessage` (ChatMessageResponse | null)
**Warning signs:** TypeScript compilation errors, runtime `undefined` from wrong field access.

### Pitfall 4: Scroll Jump on Message Prepend
**What goes wrong:** Prepending older messages to the top of the list shifts scroll position, making the user lose their place.
**Why it happens:** DOM height changes push content down.
**How to avoid:** Capture `scrollHeight` before prepend, calculate delta after prepend, adjust `scrollTop` by delta.
**Warning signs:** User sees a "jump" when loading older messages.

### Pitfall 5: Duplicate Messages from WS + REST
**What goes wrong:** A message arrives via WebSocket AND is also fetched by a poll cycle, creating duplicates.
**Why it happens:** Race condition between WS event and polling response.
**How to avoid:** Deduplicate by `clientMessageId` or `message.id`. Use a Set/Map for O(1) lookup.
**Warning signs:** Same message appears twice in the message list.

### Pitfall 6: WalkReviewModal Tag-to-Comment Mapping
**What goes wrong:** WalkReviewModal uses selectedTags array, but `ChatReviewCreateRequest` only has `score` and `comment` (no tags field).
**Why it happens:** Backend review model is simpler than the UI prototype.
**How to avoid:** Concatenate selected tags into the comment string, or simplify the modal to just score + comment textarea.
**Warning signs:** Tags silently dropped on submit.

### Pitfall 7: SockJS Not Needed
**What goes wrong:** Adding sockjs-client when the backend does not use `.withSockJS()`.
**Why it happens:** Many tutorials assume SockJS is required for Spring WebSocket.
**How to avoid:** Verified from `WebSocketConfig.java`: `registry.addEndpoint("/ws/chat-rooms/{roomId}").setAllowedOriginPatterns("*")` -- no `.withSockJS()` call. Use native WebSocket via @stomp/stompjs `brokerURL`.
**Warning signs:** Connection fails because SockJS HTTP handshake gets 404.

## Code Examples

### STOMP Client Connection with JWT (Verified Pattern)
```typescript
// Source: @stomp/stompjs docs + backend WebSocketConfig.java + ChatStompAuthChannelInterceptor.java
import { Client, IMessage } from '@stomp/stompjs';

const WS_BASE = process.env.NEXT_PUBLIC_WS_URL || 'ws://localhost:8080';

function connectToRoom(roomId: number, getToken: () => string | null, onEvent: (event: ChatRealtimeEvent) => void): Client {
  const client = new Client({
    brokerURL: `${WS_BASE}/ws/chat-rooms/${roomId}`,
    connectHeaders: {
      Authorization: `Bearer ${getToken() || ''}`,
    },
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect: () => {
      // Subscribe to room events topic
      client.subscribe(`/topic/chat-rooms/${roomId}/events`, (message: IMessage) => {
        const event = JSON.parse(message.body);
        onEvent(event);
      });
    },
    onStompError: (frame) => {
      console.error('STOMP error:', frame.headers['message']);
    },
    // Refresh token before each (re)connect
    beforeConnect: async () => {
      const token = getToken();
      if (token) {
        client.connectHeaders = { Authorization: `Bearer ${token}` };
      }
    },
  });
  client.activate();
  return client;
}
```

### ChatRealtimeEvent TypeScript Interface
```typescript
// Source: backend ChatRealtimeEvent.java + ChatRealtimeEventHandler.java
interface ChatRealtimeEvent {
  type: 'CHAT_MESSAGE_CREATED' | 'CHAT_MESSAGE_DELIVERED' | 'CHAT_MESSAGE_READ';
  data: MessageCreatedData | MessageDeliveredData | MessageReadData;
}

interface MessageCreatedData {
  roomId: number;
  message: ChatMessageResponse;  // from api/chat.ts
}

interface MessageDeliveredData {
  roomId: number;
  messageId: number;
  memberId: number;
  deliveredAt: string;
}

interface MessageReadData {
  roomId: number;
  messageId: number;
  memberId: number;
  readAt: string;
}
```

### Reverse Infinite Scroll Pattern
```typescript
// Scroll to bottom on initial load, preserve position on prepend
const handleLoadOlder = async () => {
  if (!nextCursor || isLoadingOlder) return;
  setIsLoadingOlder(true);
  const container = scrollRef.current!;
  const prevHeight = container.scrollHeight;

  const older = await getMessages(roomId, { cursor: nextCursor, size: 20 });
  setMessages(prev => [...older.content, ...prev]);
  setNextCursor(older.nextCursor);
  setHasMore(older.hasMore);

  // Restore scroll position
  requestAnimationFrame(() => {
    container.scrollTop = container.scrollHeight - prevHeight;
  });
  setIsLoadingOlder(false);
};
```

### Message Send with Optimistic Update + Retry
```typescript
const handleSend = async (content: string) => {
  const clientMessageId = crypto.randomUUID();
  const pending: PendingMessage = {
    clientMessageId,
    content,
    status: 'pending',
    sentAt: new Date().toISOString(),
  };
  // Optimistic append
  setPendingMessages(prev => [...prev, pending]);

  try {
    const sent = await sendMessage(roomId, {
      content,
      messageType: 'TEXT',
      clientMessageId,
    });
    // Replace pending with real message
    setPendingMessages(prev => prev.filter(m => m.clientMessageId !== clientMessageId));
    setMessages(prev => [...prev, sent]);
  } catch {
    // Mark as failed for retry
    setPendingMessages(prev =>
      prev.map(m => m.clientMessageId === clientMessageId ? { ...m, status: 'failed' } : m)
    );
    toast.error('메시지 전송에 실패했습니다.');
  }
};
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `chatService.getRooms()` returns `ChatRoom[]` | `getRooms()` returns `SliceResponse<ChatRoomSummaryResponse>` | Phase 2 api/chat.ts | Must access `.content` array, handle pagination |
| `chatService.getMessages(roomId: string)` returns `ChatMessage[]` | `getMessages(roomId: number, params?)` returns `CursorResponse<ChatMessageResponse>` | Phase 2 api/chat.ts | Cursor-based, access `.content`, `.nextCursor`, `.hasMore` |
| `msg.senderId` string field | `msg.sender.memberId` number field | Phase 2 types | Nested object, number type |
| `msg.timestamp` | `msg.sentAt` | Phase 2 types | Different field name |
| 3-second polling only | WebSocket STOMP primary + 5s polling fallback | This phase | Major architecture change |
| `room.partner` (UserType) | `room.participants` (ChatParticipantResponse[]) | Phase 2 types | Array of participants, not single partner |
| No message status | created/delivered/read indicators | This phase | New UI elements |
| `chatService.getOrCreateRoom(partnerId: string)` | `createDirectRoom({ partnerId: number })` | Phase 2 api/chat.ts | Different API shape |

**Deprecated/outdated:**
- `services/api/chatService.ts`: Full replacement by `api/chat.ts` -- delete after rewire
- `types/index.ts` ChatRoom/ChatMessage: Legacy types replaced by api/chat.ts types
- `ChatRoomType` export from chatService: No longer needed

## Open Questions

1. **NEXT_PUBLIC_WS_URL env var not currently used**
   - What we know: CLAUDE.md mentions it exists, but no code references it. `next.config.ts` only rewrites `/api/v1/*`.
   - What's unclear: What value to use in development. Backend likely runs on `ws://localhost:8080`.
   - Recommendation: Default to `ws://localhost:8080` if env var is not set. Document in `.env.local.example`.

2. **Group chat room display differs from 1:1**
   - What we know: `ChatRoomSummaryResponse` has `chatType` string. 1:1 rooms have 2 participants; group rooms have 3-10.
   - What's unclear: How room title/avatar should display for group rooms (backend may not provide a room name).
   - Recommendation: For group rooms, show first 2-3 participant names joined, use a stacked avatar pattern.

3. **Mark-read timing strategy**
   - What we know: API is `POST /chat-rooms/{id}/messages/read` with `{ messageId, readAt }`.
   - What's unclear: Should we mark read on every new message received, or batch/debounce?
   - Recommendation: Mark read when room is focused/visible, batch to latest messageId, debounce 1-2 seconds.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | ESLint + `npm run build` (TypeScript type check) |
| Config file | eslint.config.mjs, tsconfig.json |
| Quick run command | `npm run lint` |
| Full suite command | `npm run build` |

### Phase Requirements Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| CHAT-01 | Room list renders with states | manual-only | `npm run build` (type check) | N/A |
| CHAT-02 | Room detail loads | manual-only | `npm run build` | N/A |
| CHAT-03 | Direct chat creates room | manual-only | `npm run build` | N/A |
| CHAT-04 | Cursor pagination loads older | manual-only | `npm run build` | N/A |
| CHAT-05 | Message sends | manual-only | `npm run build` | N/A |
| CHAT-06 | WS events update UI | manual-only | `npm run build` | N/A |
| CHAT-07 | Retry bubble on failure | manual-only | `npm run build` | N/A |
| CHAT-08 | Leave room works | manual-only | `npm run build` | N/A |
| CHAT-09 | Walk confirm actions | manual-only | `npm run build` | N/A |
| CHAT-10 | Review creation | manual-only | `npm run build` | N/A |
| CHAT-11 | My review check | manual-only | `npm run build` | N/A |
| CHAT-12 | WS STOMP connects | manual-only | `npm run build` | N/A |
| CHAT-13 | 500 char limit enforced | manual-only | `npm run build` | N/A |
| CHAT-14 | Group chat capacity display | manual-only | `npm run build` | N/A |

**Justification for manual-only:** Frontend has no test runner configured (per CLAUDE.md). Validation is via `npm run lint` and `npm run build` for type/compilation safety.

### Sampling Rate
- **Per task commit:** `npm run lint && npm run build`
- **Per wave merge:** `npm run build`
- **Phase gate:** Full build green before `/gsd:verify-work`

### Wave 0 Gaps
None -- no test infrastructure to set up (frontend uses lint + build only).

## Sources

### Primary (HIGH confidence)
- Backend source: `WebSocketConfig.java` -- STOMP endpoint `/ws/chat-rooms/{roomId}`, broker prefixes `/topic`, `/queue`, app prefix `/app`
- Backend source: `ChatStompAuthChannelInterceptor.java` -- JWT auth on STOMP CONNECT via `Authorization: Bearer {token}` header
- Backend source: `ChatRealtimeEventHandler.java` -- three event types: CHAT_MESSAGE_CREATED, CHAT_MESSAGE_DELIVERED, CHAT_MESSAGE_READ
- Backend source: `StompChatRealtimePublisher.java` -- publishes to `/topic/chat-rooms/{roomId}/events`
- Backend source: `ChatRealtimeEvent.java` -- `{ type: string, data: T }` shape
- Frontend source: `api/chat.ts` -- all 13 endpoint functions with typed request/response interfaces
- Frontend source: `api/types.ts` -- CursorResponse, SliceResponse, CursorPaginationParams types
- Frontend source: `api/client.ts` -- apiClient with JWT injection, 401 refresh, toast error handling

### Secondary (MEDIUM confidence)
- [@stomp/stompjs GitHub](https://github.com/stomp-js/stompjs) -- Client class API, beforeConnect callback, connectHeaders
- [@stomp/stompjs guide](https://stomp-js.github.io/guide/stompjs/using-stompjs-v5.html) -- usage patterns, reconnect, heartbeat
- [STOMP Client API docs](https://stomp-js.github.io/api-docs/latest/classes/Client.html) -- Client constructor options

### Tertiary (LOW confidence)
- None

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- @stomp/stompjs is the only maintained STOMP client; backend WS config verified from source
- Architecture: HIGH -- all backend event shapes, endpoint paths, and auth mechanisms verified from Java source code
- Pitfalls: HIGH -- derived from actual code analysis (type mismatches, missing WS proxy, no SockJS)
- Code examples: HIGH -- patterns derived from verified backend source + established @stomp/stompjs API

**Research date:** 2026-03-07
**Valid until:** 2026-04-07 (stable domain, backend source is fixed)
