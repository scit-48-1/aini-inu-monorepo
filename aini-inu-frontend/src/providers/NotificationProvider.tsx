'use client';

import React, { useEffect } from 'react';
import { toast } from 'sonner';
import { useNotificationWebSocket } from '@/hooks/useNotificationWebSocket';
import { useNotificationStore } from '@/store/useNotificationStore';
import { getUnreadCount } from '@/api/notifications';
import { useAuth } from '@/providers/AuthProvider';
import type { NotificationItem } from '@/api/notifications';

export function NotificationProvider({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth();
  const { addNotification, incrementUnread, setUnreadCount } = useNotificationStore();

  useEffect(() => {
    if (!isAuthenticated) return;
    getUnreadCount()
      .then((data) => setUnreadCount(data.unreadCount))
      .catch(() => {});
  }, [isAuthenticated, setUnreadCount]);

  useNotificationWebSocket(isAuthenticated, (event) => {
    const payload = event.payload;
    const notification: NotificationItem = {
      id: (payload.notificationId as number) ?? 0,
      type: event.type as NotificationItem['type'],
      title: (payload.title as string) ?? '',
      message: (payload.message as string) ?? '',
      referenceId: (payload.referenceId as number) || null,
      referenceType: (payload.referenceType as string) || null,
      isRead: false,
      createdAt: event.occurredAt,
    };

    if (notification.id) {
      addNotification(notification);
    }
    incrementUnread();
    toast.info(notification.title, {
      description: notification.message,
      duration: 4000,
    });
  });

  return <>{children}</>;
}
