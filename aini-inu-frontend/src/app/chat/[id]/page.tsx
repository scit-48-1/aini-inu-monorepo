'use client';

import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { ChatHeader } from '@/components/chat/ChatHeader';
import { MessageList } from '@/components/chat/MessageList';
import { ChatInput } from '@/components/chat/ChatInput';
import { ProfileExplorer } from '@/components/chat/ProfileExplorer';
import {
  getRoom,
  getMessages,
  sendMessage,
  markMessagesRead,
  getWalkConfirm,
  confirmWalk,
  cancelWalkConfirm,
  leaveRoom,
  type ChatRoomDetailResponse,
  type WalkConfirmResponse,
} from '@/api/chat';
import { useChatStore } from '@/store/useChatStore';
import { useChatWebSocket } from '@/hooks/useChatWebSocket';
import { useUserStore } from '@/store/useUserStore';
import { Loader2, ArrowLeft } from 'lucide-react';
import { toast } from 'sonner';
import { Typography } from '@/components/ui/Typography';

export default function ChatRoomPage() {
  const params = useParams();
  const roomId = Number(params?.id);
  const router = useRouter();

  const profile = useUserStore((s) => s.profile);
  const currentMemberId = Number(profile?.id) || 0;

  const [room, setRoom] = useState<ChatRoomDetailResponse | null>(null);
  const [inputText, setInputText] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(false);
  const [isProfileOpen, setIsProfileOpen] = useState(false);
  const [walkConfirm, setWalkConfirm] = useState<WalkConfirmResponse | null>(null);

  // Cursor pagination state
  const [nextCursor, setNextCursor] = useState<string | null>(null);
  const [hasMore, setHasMore] = useState(false);
  const [isLoadingOlder, setIsLoadingOlder] = useState(false);

  // Chat store
  const messages = useChatStore((s) => s.messages);
  const pendingMessages = useChatStore((s) => s.pendingMessages);
  const setMessages = useChatStore((s) => s.setMessages);
  const prependMessages = useChatStore((s) => s.prependMessages);
  const addPendingMessage = useChatStore((s) => s.addPendingMessage);
  const removePendingMessage = useChatStore((s) => s.removePendingMessage);
  const markPendingFailed = useChatStore((s) => s.markPendingFailed);

  // WebSocket hook -- enables once room is loaded
  const { connectionMode } = useChatWebSocket(roomId, !!room);

  // Mark-read debounce ref
  const markReadTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Initial data fetch
  useEffect(() => {
    if (!roomId || isNaN(roomId)) {
      setError(true);
      setIsLoading(false);
      return;
    }

    let cancelled = false;

    async function fetchInitial() {
      try {
        const [roomData, msgData, walkConfirmData] = await Promise.all([
          getRoom(roomId),
          getMessages(roomId, { size: 20 }),
          getWalkConfirm(roomId).catch(() => null),
        ]);

        if (cancelled) return;

        setRoom(roomData);
        setMessages(msgData.content);
        setNextCursor(msgData.nextCursor);
        setHasMore(msgData.hasMore);
        setWalkConfirm(walkConfirmData);
      } catch (e) {
        console.error(e);
        if (!cancelled) {
          setError(true);
          toast.error('채팅방을 불러오는데 실패했습니다.');
        }
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    }

    fetchInitial();
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [roomId]);

  // Mark messages as read on mount and when new messages arrive
  useEffect(() => {
    if (!room || messages.length === 0) return;

    const latestMessage = messages[messages.length - 1];
    if (!latestMessage) return;

    // Debounce 2 seconds
    if (markReadTimer.current) clearTimeout(markReadTimer.current);
    markReadTimer.current = setTimeout(() => {
      markMessagesRead(roomId, {
        messageId: latestMessage.id,
        readAt: new Date().toISOString(),
      }).catch(() => {
        // Silent -- mark-read failure is non-critical
      });
    }, 2000);

    return () => {
      if (markReadTimer.current) clearTimeout(markReadTimer.current);
    };
  }, [room, messages, roomId]);

  // Load older messages (cursor pagination)
  const handleLoadOlder = useCallback(async () => {
    if (!nextCursor || isLoadingOlder) return;
    setIsLoadingOlder(true);
    try {
      const result = await getMessages(roomId, {
        cursor: nextCursor,
        size: 20,
      });
      prependMessages(result.content);
      setNextCursor(result.nextCursor);
      setHasMore(result.hasMore);
    } catch (e) {
      console.error('Failed to load older messages:', e);
    } finally {
      setIsLoadingOlder(false);
    }
  }, [roomId, nextCursor, isLoadingOlder, prependMessages]);

  // Send message (optimistic)
  const handleSend = useCallback(async () => {
    const content = inputText.trim();
    if (!content) return;

    const clientMessageId = crypto.randomUUID();
    const pending = {
      clientMessageId,
      content,
      status: 'pending' as const,
      sentAt: new Date().toISOString(),
    };

    setInputText('');
    addPendingMessage(pending);

    try {
      const serverMsg = await sendMessage(roomId, {
        content,
        messageType: 'TEXT',
        clientMessageId,
      });
      removePendingMessage(clientMessageId);
      // The addMessage in store handles dedup if WS already delivered it
      useChatStore.getState().addMessage(serverMsg);
    } catch {
      markPendingFailed(clientMessageId);
      toast.error('메시지 전송에 실패했습니다.');
    }
  }, [
    inputText,
    roomId,
    addPendingMessage,
    removePendingMessage,
    markPendingFailed,
  ]);

  // Retry failed message
  const handleRetry = useCallback(
    async (clientMessageId: string) => {
      const pending = pendingMessages.find(
        (p) => p.clientMessageId === clientMessageId,
      );
      if (!pending) return;

      // Reset to pending status
      useChatStore.getState().markPendingFailed(clientMessageId);
      const store = useChatStore.getState();
      store.removePendingMessage(clientMessageId);
      store.addPendingMessage({
        ...pending,
        status: 'pending',
        sentAt: new Date().toISOString(),
      });

      try {
        const serverMsg = await sendMessage(roomId, {
          content: pending.content,
          messageType: 'TEXT',
          clientMessageId,
        });
        useChatStore.getState().removePendingMessage(clientMessageId);
        useChatStore.getState().addMessage(serverMsg);
      } catch {
        useChatStore.getState().markPendingFailed(clientMessageId);
        toast.error('메시지 전송에 실패했습니다.');
      }
    },
    [roomId, pendingMessages],
  );

  // Walk confirm handler
  const handleConfirmWalk = useCallback(async () => {
    try {
      const result = await confirmWalk(roomId, { action: 'CONFIRM' });
      setWalkConfirm(result);
    } catch {
      toast.error('산책 확인에 실패했습니다.');
    }
  }, [roomId]);

  // Cancel walk confirm handler
  const handleCancelConfirm = useCallback(async () => {
    try {
      await cancelWalkConfirm(roomId);
      const refreshed = await getWalkConfirm(roomId).catch(() => null);
      setWalkConfirm(refreshed);
    } catch {
      toast.error('산책 확인 취소에 실패했습니다.');
    }
  }, [roomId]);

  // Leave room handler
  const handleLeave = useCallback(async () => {
    const confirmed = window.confirm('채팅방을 나가시겠습니까?');
    if (!confirmed) return;

    try {
      await leaveRoom(roomId);
      router.push('/chat');
    } catch {
      toast.error('채팅방 나가기에 실패했습니다.');
    }
  }, [roomId, router]);

  // Derive partnerId for ProfileExplorer
  const partnerId =
    room?.participants.find((p) => p.memberId !== currentMemberId)?.memberId ??
    0;

  // Error state
  if (error) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center gap-4 bg-[#fdfbf7]">
        <Typography variant="body" className="text-zinc-500">
          채팅방을 찾을 수 없습니다.
        </Typography>
        <button
          onClick={() => router.push('/chat')}
          className="flex items-center gap-2 px-4 py-2 bg-zinc-100 rounded-xl text-sm font-bold text-navy-900 hover:bg-zinc-200 transition-colors"
        >
          <ArrowLeft size={16} />
          채팅 목록으로
        </button>
      </div>
    );
  }

  // Loading state
  if (isLoading || !room) {
    return (
      <div className="flex-1 flex items-center justify-center bg-[#fdfbf7]">
        <Loader2 className="animate-spin text-amber-500" size={40} />
      </div>
    );
  }

  return (
    <div className="flex w-full h-full relative overflow-hidden bg-[#fdfbf7]">
      <div className="flex-1 flex flex-col min-w-0 transition-all duration-500 ease-in-out">
        <ChatHeader
          room={room}
          currentMemberId={currentMemberId}
          showInfo={isProfileOpen}
          onShowInfoToggle={() => setIsProfileOpen(!isProfileOpen)}
          onBack={() => router.push('/chat')}
          connectionMode={connectionMode}
          walkConfirmState={walkConfirm}
          onConfirmWalk={handleConfirmWalk}
          onCancelConfirm={handleCancelConfirm}
          onLeave={handleLeave}
        />

        <MessageList
          messages={messages}
          pendingMessages={pendingMessages}
          currentMemberId={currentMemberId}
          onLoadOlder={handleLoadOlder}
          hasMore={hasMore}
          isLoadingOlder={isLoadingOlder}
          onRetry={handleRetry}
        />

        <ChatInput
          inputText={inputText}
          setInputText={setInputText}
          onSend={handleSend}
          isArchived={room.status === 'CLOSED'}
        />
      </div>

      {/* Profile Explorer */}
      <ProfileExplorer
        partnerId={partnerId}
        isOpen={isProfileOpen}
        onClose={() => setIsProfileOpen(false)}
      />
    </div>
  );
}
