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

  // Messages for current room
  messages: ChatMessageResponse[];

  // Optimistic sends awaiting server confirmation
  pendingMessages: PendingMessage[];

  // Room list cache
  rooms: ChatRoomSummaryResponse[];

  // Actions
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

export const useChatStore = create<ChatState>()((set, get) => ({
  connectionMode: 'disconnected',
  messages: [],
  pendingMessages: [],
  rooms: [],

  setConnectionMode: (mode) => set({ connectionMode: mode }),

  addMessage: (msg) =>
    set((state) => {
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
      const idSet = buildIdSet(state.messages);
      const newMsgs = msgs.filter(
        (m) => !idSet.has(`id:${m.id}`),
      );
      if (newMsgs.length === 0) return state;
      return { messages: [...newMsgs, ...state.messages] };
    }),

  setMessages: (msgs) => set({ messages: msgs }),

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
