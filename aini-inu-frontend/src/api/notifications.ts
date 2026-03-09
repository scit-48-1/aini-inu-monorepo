import { apiClient } from './client';

export interface NotificationItem {
  id: number;
  type: 'CHAT_NEW_MESSAGE' | 'WALK_CONFIRM' | 'WALK_APPLICATION' | 'COMMENT_ON_POST';
  title: string;
  message: string;
  referenceId: number | null;
  referenceType: string | null;
  isRead: boolean;
  createdAt: string;
}

export interface NotificationSlice {
  content: NotificationItem[];
  pageNumber: number;
  pageSize: number;
  first: boolean;
  last: boolean;
  hasNext: boolean;
}

export function getNotifications(page = 0, size = 20) {
  return apiClient.get<NotificationSlice>(`/notifications?page=${page}&size=${size}`);
}

export function getUnreadCount() {
  return apiClient.get<{ unreadCount: number }>('/notifications/unread-count');
}

export function markAsRead(id: number) {
  return apiClient.patch<void>(`/notifications/${id}/read`);
}

export function markAllAsRead() {
  return apiClient.patch<void>('/notifications/read-all');
}
