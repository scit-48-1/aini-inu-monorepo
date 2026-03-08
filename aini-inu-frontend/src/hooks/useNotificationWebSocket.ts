'use client';

import { useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import { useAuthStore } from '@/store/useAuthStore';

export interface NotificationEvent {
  type: string;
  payload: Record<string, unknown>;
  occurredAt: string;
}

const RECONNECT_DELAY = 5000;
const HEARTBEAT_INCOMING = 10000;
const HEARTBEAT_OUTGOING = 10000;

export function useNotificationWebSocket(
  enabled: boolean,
  onNotification: (event: NotificationEvent) => void,
) {
  const clientRef = useRef<Client | null>(null);
  const callbackRef = useRef(onNotification);
  callbackRef.current = onNotification;

  useEffect(() => {
    if (!enabled) return;

    const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = (
      process.env.NEXT_PUBLIC_WS_URL ||
      `${wsProtocol}//${window.location.hostname}:8080`
    ).replace(/\/ws\/?$/, '');
    const brokerURL = `${wsUrl}/ws/notifications`;

    const stompClient = new Client({
      brokerURL,
      reconnectDelay: RECONNECT_DELAY,
      heartbeatIncoming: HEARTBEAT_INCOMING,
      heartbeatOutgoing: HEARTBEAT_OUTGOING,

      beforeConnect: () => {
        const token = useAuthStore.getState().getAccessToken();
        if (token) {
          stompClient.connectHeaders = {
            Authorization: `Bearer ${token}`,
          };
        }
      },

      onConnect: () => {
        stompClient.subscribe('/user/queue/notifications', (frame) => {
          try {
            const event: NotificationEvent = JSON.parse(frame.body);
            callbackRef.current(event);
          } catch {
            // Malformed frame — ignore
          }
        });
      },
    });

    clientRef.current = stompClient;
    stompClient.activate();

    return () => {
      stompClient.deactivate();
      clientRef.current = null;
    };
  }, [enabled]);
}
