'use client';

import { useEffect, useRef, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import { useAuthStore } from '@/store/useAuthStore';
import { useChatStore } from '@/store/useChatStore';
import { useUserStore } from '@/store/useUserStore';
import { getMessages, type ChatMessageResponse } from '@/api/chat';

// --- Realtime event types (from backend SSE/STOMP) ---

interface ChatRealtimeEvent {
  type: 'CHAT_MESSAGE_CREATED' | 'CHAT_MESSAGE_DELIVERED' | 'CHAT_MESSAGE_READ';
  data: MessageCreatedData | MessageDeliveredData | MessageReadData;
}

interface MessageCreatedData {
  roomId: number;
  message: ChatMessageResponse;
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

// --- Constants ---

const RECONNECT_DELAY = 5000;
const HEARTBEAT_INCOMING = 10000;
const HEARTBEAT_OUTGOING = 10000;
const POLLING_INTERVAL = 5000;

// --- Hook ---

export function useChatWebSocket(roomId: number, enabled: boolean) {
  const clientRef = useRef<Client | null>(null);
  const pollingRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const connectionMode = useChatStore((s) => s.connectionMode);
  const setConnectionMode = useChatStore((s) => s.setConnectionMode);
  const addMessage = useChatStore((s) => s.addMessage);
  const updateMessageStatus = useChatStore((s) => s.updateMessageStatus);
  const deduplicateMessage = useChatStore((s) => s.deduplicateMessage);
  const mergeMessages = useChatStore((s) => s.mergeMessages);

  const clearPolling = useCallback(() => {
    if (pollingRef.current) {
      clearInterval(pollingRef.current);
      pollingRef.current = null;
    }
  }, []);

  const startPolling = useCallback(() => {
    // Polling and WS are mutually exclusive
    clearPolling();

    pollingRef.current = setInterval(async () => {
      try {
        const currentRoom = useChatStore.getState().currentRoomId;
        if (currentRoom !== roomId) return;
        const result = await getMessages(roomId);
        mergeMessages(result.content);
      } catch {
        // Silent — polling is fallback, don't spam errors
      }
    }, POLLING_INTERVAL);
  }, [roomId, clearPolling, mergeMessages]);

  useEffect(() => {
    if (!enabled || !roomId) {
      setConnectionMode('disconnected');
      return;
    }

    const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = (process.env.NEXT_PUBLIC_WS_URL || `${wsProtocol}//${window.location.hostname}:8080`).replace(/\/ws\/?$/, '');
    const brokerURL = `${wsUrl}/ws/chat-rooms/${roomId}`;

    console.log('[ChatWS] Connecting to:', brokerURL);

    const stompClient = new Client({
      brokerURL,
      reconnectDelay: RECONNECT_DELAY,
      heartbeatIncoming: HEARTBEAT_INCOMING,
      heartbeatOutgoing: HEARTBEAT_OUTGOING,

      // Refresh token before each connect attempt (Pitfall 2)
      beforeConnect: () => {
        const token = useAuthStore.getState().getAccessToken();
        if (token) {
          stompClient.connectHeaders = {
            Authorization: `Bearer ${token}`,
          };
        }
      },

      onConnect: () => {
        // WS connected — stop polling if active
        clearPolling();
        setConnectionMode('ws');

        stompClient.subscribe(
          `/topic/chat-rooms/${roomId}/events`,
          (frame) => {
            try {
              const event: ChatRealtimeEvent = JSON.parse(frame.body);
              handleEvent(event);
            } catch {
              // Malformed frame — ignore
            }
          },
        );
      },

      onDisconnect: () => {
        setConnectionMode('polling');
        startPolling();
      },

      onStompError: () => {
        setConnectionMode('polling');
        startPolling();
      },

      onWebSocketError: () => {
        setConnectionMode('polling');
        startPolling();
      },
    });

    function handleEvent(event: ChatRealtimeEvent) {
      switch (event.type) {
        case 'CHAT_MESSAGE_CREATED': {
          const data = event.data as MessageCreatedData;
          addMessage(data.message);
          deduplicateMessage(data.message);
          break;
        }
        case 'CHAT_MESSAGE_DELIVERED': {
          const data = event.data as MessageDeliveredData;
          updateMessageStatus(data.messageId, { status: 'DELIVERED' });
          break;
        }
        case 'CHAT_MESSAGE_READ': {
          const data = event.data as MessageReadData;
          const currentMemberId = Number(useUserStore.getState().profile?.id) || 0;
          // Only mark as READ when the *other* person read the message
          if (data.memberId !== currentMemberId) {
            updateMessageStatus(data.messageId, { status: 'READ' });
          }
          break;
        }
      }
    }

    // Store in ref (not state — anti-pattern from RESEARCH)
    clientRef.current = stompClient;
    stompClient.activate();

    return () => {
      clearPolling();
      stompClient.deactivate();
      clientRef.current = null;
      setConnectionMode('disconnected');
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [roomId, enabled]);

  const disconnect = useCallback(() => {
    clearPolling();
    if (clientRef.current) {
      clientRef.current.deactivate();
      clientRef.current = null;
    }
    setConnectionMode('disconnected');
  }, [clearPolling, setConnectionMode]);

  return {
    connectionMode,
    isConnected: connectionMode === 'ws',
    disconnect,
  };
}
