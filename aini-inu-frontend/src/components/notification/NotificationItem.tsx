'use client';

import React from 'react';
import { MessageSquare, Footprints, UserPlus, MessageCircle } from 'lucide-react';
import { cn } from '@/lib/utils';
import type { NotificationItem as NotificationItemType } from '@/api/notifications';

const TYPE_CONFIG = {
  CHAT_NEW_MESSAGE: { icon: MessageSquare, color: 'text-blue-500', bg: 'bg-blue-50' },
  WALK_CONFIRM: { icon: Footprints, color: 'text-emerald-500', bg: 'bg-emerald-50' },
  WALK_APPLICATION: { icon: UserPlus, color: 'text-amber-500', bg: 'bg-amber-50' },
  COMMENT_ON_POST: { icon: MessageCircle, color: 'text-purple-500', bg: 'bg-purple-50' },
} as const;

function formatRelativeTime(dateString: string): string {
  const now = Date.now();
  const date = new Date(dateString).getTime();
  const diffMs = now - date;
  const diffMin = Math.floor(diffMs / 60000);

  if (diffMin < 1) return '방금 전';
  if (diffMin < 60) return `${diffMin}분 전`;
  const diffHour = Math.floor(diffMin / 60);
  if (diffHour < 24) return `${diffHour}시간 전`;
  const diffDay = Math.floor(diffHour / 24);
  if (diffDay < 7) return `${diffDay}일 전`;
  return new Date(dateString).toLocaleDateString('ko-KR');
}

interface Props {
  notification: NotificationItemType;
  onClick: (notification: NotificationItemType) => void;
}

export const NotificationItem: React.FC<Props> = ({ notification, onClick }) => {
  const config = TYPE_CONFIG[notification.type];
  const Icon = config.icon;

  return (
    <button
      onClick={() => onClick(notification)}
      className={cn(
        'w-full flex items-start gap-3 px-4 py-3 text-left transition-colors hover:bg-zinc-50',
        !notification.isRead && 'bg-amber-50/50',
      )}
    >
      <div className={cn('p-2 rounded-full shrink-0', config.bg)}>
        <Icon size={16} className={config.color} />
      </div>
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2">
          <span className="text-xs font-bold text-zinc-900 truncate">{notification.title}</span>
          {!notification.isRead && (
            <span className="w-1.5 h-1.5 rounded-full bg-amber-500 shrink-0" />
          )}
        </div>
        <p className="text-[11px] text-zinc-500 truncate mt-0.5">{notification.message}</p>
        <span className="text-[10px] text-zinc-300 font-bold mt-1 block">
          {formatRelativeTime(notification.createdAt)}
        </span>
      </div>
    </button>
  );
};
