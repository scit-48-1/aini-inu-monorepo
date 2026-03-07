'use client';

import React, { useEffect, useRef, useCallback } from 'react';
import { RefreshCw, Loader2 } from 'lucide-react';
import { cn } from '@/lib/utils';
import type { ChatMessageResponse } from '@/api/chat';
import type { PendingMessage } from '@/store/useChatStore';

interface MessageListProps {
  messages: ChatMessageResponse[];
  pendingMessages: PendingMessage[];
  currentMemberId: number;
  onLoadOlder: () => Promise<void>;
  hasMore: boolean;
  isLoadingOlder: boolean;
  onRetry: (clientMessageId: string) => void;
}

function formatTime(iso: string): string {
  try {
    return new Date(iso).toLocaleTimeString([], {
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return '';
  }
}

function StatusText({ status }: { status: string }) {
  switch (status) {
    case 'READ':
    case 'read':
      return <span className="text-[9px] text-blue-500 font-bold">읽음</span>;
    default:
      return <span className="text-[9px] text-zinc-400 font-bold">전송완료</span>;
  }
}

export const MessageList: React.FC<MessageListProps> = ({
  messages,
  pendingMessages,
  currentMemberId,
  onLoadOlder,
  hasMore,
  isLoadingOlder,
  onRetry,
}) => {
  const scrollRef = useRef<HTMLDivElement>(null);
  const sentinelRef = useRef<HTMLDivElement>(null);
  const isInitialLoad = useRef(true);
  const prevScrollHeightRef = useRef(0);

  // Auto-scroll to bottom on initial load
  useEffect(() => {
    if (isInitialLoad.current && scrollRef.current && messages.length > 0) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
      isInitialLoad.current = false;
    }
  }, [messages.length]);

  // Auto-scroll to bottom on new messages (not on load-older)
  useEffect(() => {
    const el = scrollRef.current;
    if (!el) return;
    // Only auto-scroll if user is near bottom (within 150px)
    const isNearBottom =
      el.scrollHeight - el.scrollTop - el.clientHeight < 150;
    if (isNearBottom) {
      el.scrollTop = el.scrollHeight;
    }
  }, [messages.length, pendingMessages.length]);

  // Scroll position preservation after prepending older messages
  const preserveScrollOnPrepend = useCallback(() => {
    const el = scrollRef.current;
    if (!el) return;
    const newScrollHeight = el.scrollHeight;
    const delta = newScrollHeight - prevScrollHeightRef.current;
    if (delta > 0) {
      el.scrollTop += delta;
    }
  }, []);

  // IntersectionObserver for reverse infinite scroll
  useEffect(() => {
    const sentinel = sentinelRef.current;
    const container = scrollRef.current;
    if (!sentinel || !container) return;

    const observer = new IntersectionObserver(
      (entries) => {
        const entry = entries[0];
        if (entry?.isIntersecting && hasMore && !isLoadingOlder) {
          prevScrollHeightRef.current = container.scrollHeight;
          onLoadOlder().then(() => {
            requestAnimationFrame(preserveScrollOnPrepend);
          });
        }
      },
      { root: container, threshold: 0.1 },
    );

    observer.observe(sentinel);
    return () => observer.disconnect();
  }, [hasMore, isLoadingOlder, onLoadOlder, preserveScrollOnPrepend]);

  const lastMyMsgIndex = pendingMessages.length === 0
    ? messages.findLastIndex(m => m.sender.memberId === currentMemberId)
    : -1;

  const lastPendingIndex = pendingMessages.length > 0
    ? pendingMessages.findLastIndex(pm => pm.status === 'pending')
    : -1;

  return (
    <div
      ref={scrollRef}
      className="flex-1 overflow-y-auto p-4 md:p-6 space-y-6 no-scrollbar bg-[#fdfbf7]"
    >
      {/* Sentinel for loading older messages */}
      <div ref={sentinelRef} className="h-1" />

      {isLoadingOlder && (
        <div className="flex justify-center py-2">
          <Loader2 className="animate-spin text-amber-500" size={20} />
        </div>
      )}

      {messages.map((msg, i) => {
        const isMe = msg.sender.memberId === currentMemberId;
        const prevMsg = messages[i - 1];
        const isSameSender =
          prevMsg?.sender.memberId === msg.sender.memberId;
        const timeStr = formatTime(msg.sentAt);

        return (
          <div
            key={msg.id}
            className={cn(
              'flex gap-3 max-w-[85%] md:max-w-[70%] animate-in fade-in slide-in-from-bottom-2 duration-300',
              isMe ? 'ml-auto flex-row-reverse' : 'mr-auto',
              isSameSender ? 'mt-1' : 'mt-4',
            )}
          >
            <div
              className={cn(
                'flex flex-col',
                isMe ? 'items-end' : 'items-start',
              )}
            >
              <div
                className={cn(
                  'flex items-end gap-1.5',
                  isMe ? 'flex-row-reverse' : '',
                )}
              >
                <div
                  className={cn(
                    'px-4 py-2.5 rounded-[20px] shadow-sm border transition-all text-sm font-medium leading-relaxed break-words whitespace-pre-wrap max-w-full',
                    isMe
                      ? 'bg-navy-900 text-white border-navy-900 rounded-br-none'
                      : 'bg-white border-zinc-100 text-navy-900 rounded-bl-none',
                  )}
                >
                  {msg.content}
                </div>
                <div
                  className={cn(
                    'flex flex-col items-end gap-0.5 shrink-0 mb-1',
                    !isMe && 'items-start',
                  )}
                >
                  <span className="text-[9px] text-zinc-300 font-bold">
                    {timeStr}
                  </span>
                  {isMe && i === lastMyMsgIndex && <StatusText status={msg.status} />}
                </div>
              </div>
            </div>
          </div>
        );
      })}

      {/* Pending messages */}
      {pendingMessages.map((pm, pi) => (
        <div
          key={pm.clientMessageId}
          className="flex gap-3 max-w-[85%] md:max-w-[70%] ml-auto flex-row-reverse"
        >
          <div className="flex flex-col items-end">
            <div className="flex items-end gap-1.5 flex-row-reverse">
              <div
                className={cn(
                  'px-4 py-2.5 rounded-[20px] shadow-sm border transition-all text-sm font-medium leading-relaxed break-words whitespace-pre-wrap max-w-full bg-navy-900 text-white border-navy-900 rounded-br-none',
                  pm.status === 'pending' && 'opacity-70',
                  pm.status === 'failed' && 'border-red-500 border-2',
                )}
              >
                {pm.content}
              </div>
              <div className="flex flex-col items-end gap-0.5 shrink-0 mb-1">
                <span className="text-[9px] text-zinc-300 font-bold">
                  {formatTime(pm.sentAt)}
                </span>
                {pm.status === 'pending' && pi === lastPendingIndex ? (
                  <StatusText status="CREATED" />
                ) : pm.status === 'failed' ? (
                  <button
                    onClick={() => onRetry(pm.clientMessageId)}
                    className="p-0.5 text-red-500 hover:text-red-600 transition-colors"
                    title="다시 보내기"
                  >
                    <RefreshCw size={12} />
                  </button>
                ) : null}
              </div>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
};
