'use client';

import React, { useEffect, useRef } from 'react';
import { Bell } from 'lucide-react';
import { useNotificationStore } from '@/store/useNotificationStore';
import { getNotifications, markAsRead as markAsReadApi, markAllAsRead as markAllAsReadApi } from '@/api/notifications';
import { NotificationItem } from './NotificationItem';
import type { NotificationItem as NotificationItemType } from '@/api/notifications';
import { useRouter } from 'next/navigation';

function getNavigationPath(notification: NotificationItemType): string {
  switch (notification.type) {
    case 'CHAT_NEW_MESSAGE':
    case 'WALK_CONFIRM':
      return notification.referenceId ? `/chat/${notification.referenceId}` : '/chat';
    case 'WALK_APPLICATION':
      return '/around-me';
    case 'COMMENT_ON_POST':
      return '/feed';
    default:
      return '/dashboard';
  }
}

export const NotificationPanel: React.FC = () => {
  const router = useRouter();
  const panelRef = useRef<HTMLDivElement>(null);
  const { notifications, isOpen, setNotifications, setOpen } = useNotificationStore();
  const storeMarkAsRead = useNotificationStore((s) => s.markAsRead);
  const storeMarkAllAsRead = useNotificationStore((s) => s.markAllAsRead);

  useEffect(() => {
    if (!isOpen) return;

    getNotifications(0, 20)
      .then((data) => setNotifications(data.content))
      .catch(() => {});
  }, [isOpen, setNotifications]);

  useEffect(() => {
    if (!isOpen) return;
    function handleClickOutside(e: MouseEvent) {
      if (panelRef.current && !panelRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [isOpen, setOpen]);

  if (!isOpen) return null;

  const handleItemClick = async (notification: NotificationItemType) => {
    if (!notification.isRead) {
      storeMarkAsRead(notification.id);
      markAsReadApi(notification.id).catch(() => {});
    }
    setOpen(false);
    router.push(getNavigationPath(notification));
  };

  const handleMarkAllAsRead = async () => {
    storeMarkAllAsRead();
    markAllAsReadApi().catch(() => {});
  };

  return (
    <div
      ref={panelRef}
      className="fixed left-28 bottom-16 w-80 max-h-96 bg-white rounded-2xl shadow-2xl border border-zinc-100 z-50 overflow-hidden animate-in fade-in zoom-in-95 duration-200 lg:left-28 lg:bottom-16 max-lg:left-1/2 max-lg:-translate-x-1/2 max-lg:bottom-24"
    >
      <div className="flex items-center justify-between px-4 py-3 border-b border-zinc-100">
        <span className="text-sm font-black text-zinc-900">알림</span>
        <button
          onClick={handleMarkAllAsRead}
          className="text-[11px] font-bold text-amber-500 hover:text-amber-600 transition-colors"
        >
          모두 읽음
        </button>
      </div>

      <div className="overflow-y-auto max-h-[320px] divide-y divide-zinc-50">
        {notifications.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-12 text-zinc-300">
            <Bell size={32} strokeWidth={1.5} />
            <span className="text-xs font-bold mt-2">알림이 없습니다</span>
          </div>
        ) : (
          notifications.map((n) => (
            <NotificationItem key={n.id} notification={n} onClick={handleItemClick} />
          ))
        )}
      </div>
    </div>
  );
};
