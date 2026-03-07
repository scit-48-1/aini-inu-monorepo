import { create } from 'zustand';
import type { ChatMessageResponse, ChatRoomSummaryResponse } from '@/api/chat';

// --- Types ---

export interface PendingMessage {
  clientMessageId: string;
  content: string;
  status: 'pending' | 'failed';
  sentAt: string;
}

type ConnectionMode = 'ws' | 'polling' | 'disconnected';

interface ChatState {
  // Connection
  connectionMode: ConnectionMode;

  // Current room ID — used to filter incoming messages
  currentRoomId: number | null;

  // Messages for current room
  messages: ChatMessageResponse[];

  // Optimistic sends awaiting server confirmation
  pendingMessages: PendingMessage[];

  // Room list cache
  rooms: ChatRoomSummaryResponse[];

  // Actions
  setCurrentRoomId: (roomId: number | null) => void;
  setConnectionMode: (mode: ConnectionMode) => void;
  addMessage: (msg: ChatMessageResponse) => void;
  updateMessageStatus: (
    messageId: number,
    update: Partial<Pick<ChatMessageResponse, 'status'>>,
  ) => void;
  addPendingMessage: (msg: PendingMessage) => void;
  removePendingMessage: (clientMessageId: string) => void;
  markPendingFailed: (clientMessageId: string) => void;
  setRooms: (rooms: ChatRoomSummaryResponse[]) => void;
  prependMessages: (msgs: ChatMessageResponse[]) => void;
  setMessages: (msgs: ChatMessageResponse[]) => void;
  mergeMessages: (msgs: ChatMessageResponse[]) => void;
  removeRoom: (roomId: number) => void;
  clearMessages: () => void;
  deduplicateMessage: (msg: ChatMessageResponse) => void;
}

// O(1) dedup lookup — rebuilt from messages array on bulk ops
function buildIdSet(messages: ChatMessageResponse[]): Set<string> {
  const set = new Set<string>();
  for (const m of messages) {
    set.add(`id:${m.id}`);
    if (m.clientMessageId) set.add(`cid:${m.clientMessageId}`);
  }
  return set;
}

export const useChatStore = create<ChatState>()((set) => ({
  connectionMode: 'disconnected',
  currentRoomId: null,
  messages: [],
  pendingMessages: [],
  rooms: [],

  setCurrentRoomId: (roomId) => set({ currentRoomId: roomId }),
  setConnectionMode: (mode) => set({ connectionMode: mode }),

  addMessage: (msg) =>
    set((state) => {
      // Ignore messages that don't belong to the current room
      if (state.currentRoomId !== null && msg.roomId !== state.currentRoomId) return state;
      const idSet = buildIdSet(state.messages);
      if (idSet.has(`id:${msg.id}`)) return state;
      return { messages: [...state.messages, msg] };
    }),

  updateMessageStatus: (messageId, update) =>
    set((state) => ({
      messages: state.messages.map((m) =>
        m.id === messageId ? { ...m, ...update } : m,
      ),
    })),

  addPendingMessage: (msg) =>
    set((state) => ({
      pendingMessages: [...state.pendingMessages, msg],
    })),

  removePendingMessage: (clientMessageId) =>
    set((state) => ({
      pendingMessages: state.pendingMessages.filter(
        (m) => m.clientMessageId !== clientMessageId,
      ),
    })),

  markPendingFailed: (clientMessageId) =>
    set((state) => ({
      pendingMessages: state.pendingMessages.map((m) =>
        m.clientMessageId === clientMessageId
          ? { ...m, status: 'failed' as const }
          : m,
      ),
    })),

  setRooms: (rooms) => set({ rooms }),

  prependMessages: (msgs) =>
    set((state) => {
      const filtered = state.currentRoomId !== null
        ? msgs.filter((m) => m.roomId === state.currentRoomId)
        : msgs;
      const idSet = buildIdSet(state.messages);
      const newMsgs = filtered.filter(
        (m) => !idSet.has(`id:${m.id}`),
      );
      if (newMsgs.length === 0) return state;
      return { messages: [...newMsgs, ...state.messages] };
    }),

  setMessages: (msgs) =>
    set((state) => {
      if (state.currentRoomId !== null && msgs.length > 0 && msgs[0].roomId !== state.currentRoomId) {
        return state;
      }
      return { messages: msgs };
    }),

  mergeMessages: (msgs) =>
    set((state) => {
      // Filter out messages that don't belong to the current room
      const filtered = state.currentRoomId !== null
        ? msgs.filter((m) => m.roomId === state.currentRoomId)
        : msgs;
      if (filtered.length === 0) return state;

      // Shallow equality check: same length and same IDs in order → no update
      if (
        filtered.length === state.messages.length &&
        filtered.every((m, i) => m.id === state.messages[i]?.id)
      ) {
        return state;
      }

      // Merge: keep existing messages not in incoming (e.g. optimistic sends),
      // layer incoming on top
      const incomingMap = new Map<number, ChatMessageResponse>();
      for (const m of filtered) {
        incomingMap.set(m.id, m);
      }

      const incomingIds = new Set(incomingMap.keys());
      // Existing messages NOT in incoming batch (preserve optimistic sends)
      const preserved = state.messages.filter((m) => !incomingIds.has(m.id));

      // Build merged array: incoming + preserved, sorted by id ascending
      const merged = [...incomingMap.values(), ...preserved].sort(
        (a, b) => a.id - b.id,
      );

      return { messages: merged };
    }),

  removeRoom: (roomId) =>
    set((state) => ({
      rooms: state.rooms.filter((r) => r.chatRoomId !== roomId),
    })),

  clearMessages: () => set({ messages: [], pendingMessages: [], currentRoomId: null }),

  deduplicateMessage: (msg) =>
    set((state) => {
      // When server confirms a message we sent optimistically,
      // remove matching pending message by clientMessageId
      const pending = state.pendingMessages;
      const matchIdx = pending.findIndex(
        (p) => p.clientMessageId === msg.clientMessageId,
      );
      if (matchIdx === -1) return state;

      // Remove from pending, ensure server message is in messages
      const newPending = pending.filter((_, i) => i !== matchIdx);
      const idSet = buildIdSet(state.messages);
      const newMessages = idSet.has(`id:${msg.id}`)
        ? state.messages
        : [...state.messages, msg];

      return { pendingMessages: newPending, messages: newMessages };
    }),
}));
