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
  getMyReview,
  type ChatRoomDetailResponse,
  type WalkConfirmResponse,
  type MyChatReviewResponse,
} from '@/api/chat';
import { useChatStore } from '@/store/useChatStore';
import { useChatWebSocket } from '@/hooks/useChatWebSocket';
import { useUserStore } from '@/store/useUserStore';
import { WalkReviewModal } from '@/components/shared/modals/WalkReviewModal';
import { Loader2, ArrowLeft, PenLine } from 'lucide-react';
import { toast } from 'sonner';
import { Typography } from '@/components/ui/Typography';
import { ConfirmModal } from '@/components/common/ConfirmModal';

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
  const [myReview, setMyReview] = useState<MyChatReviewResponse | null>(null);
  const [isReviewModalOpen, setIsReviewModalOpen] = useState(false);
  const [reviewTargetId, setReviewTargetId] = useState<number>(0);
  const [reviewTargetName, setReviewTargetName] = useState('');
  const [isLeaveConfirmOpen, setIsLeaveConfirmOpen] = useState(false);

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

  // Store actions for leave cleanup
  const removeRoom = useChatStore((s) => s.removeRoom);
  const clearMessages = useChatStore((s) => s.clearMessages);

  // WebSocket hook -- enables once room is loaded
  const { connectionMode, disconnect } = useChatWebSocket(roomId, !!room);

  // Mark-read debounce ref
  const markReadTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Initial data fetch
  useEffect(() => {
    if (!roomId || isNaN(roomId)) {
      setError(true);
      setIsLoading(false);
      return;
    }

    // Clear previous room's messages when switching rooms
    clearMessages();

    let cancelled = false;

    async function fetchInitial() {
      try {
        const [roomData, msgData, walkConfirmData, myReviewData] = await Promise.all([
          getRoom(roomId),
          getMessages(roomId, { size: 20 }),
          getWalkConfirm(roomId).catch(() => null),
          getMyReview(roomId).catch(() => null),
        ]);

        if (cancelled) return;

        setRoom(roomData);
        setMessages(msgData.content);
        setNextCursor(msgData.nextCursor);
        setHasMore(msgData.hasMore);
        setWalkConfirm(walkConfirmData);
        setMyReview(myReviewData);
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

  // Derive latest message ID as primitive to avoid re-firing on array ref changes
  const latestMessageId =
    messages.length > 0 ? messages[messages.length - 1]?.id : null;

  // Mark messages as read on mount and when new messages arrive
  useEffect(() => {
    if (!room || !latestMessageId) return;

    // Debounce 2 seconds
    if (markReadTimer.current) clearTimeout(markReadTimer.current);
    markReadTimer.current = setTimeout(() => {
      markMessagesRead(roomId, {
        messageId: latestMessageId,
        readAt: new Date().toISOString(),
      }).catch(() => {
        // Silent -- mark-read failure is non-critical
      });
    }, 2000);

    return () => {
      if (markReadTimer.current) clearTimeout(markReadTimer.current);
    };
  }, [room, latestMessageId, roomId]);

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
        messageType: 'USER',
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
          messageType: 'USER',
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

  // Leave room handler -- opens confirm modal
  const handleLeave = useCallback(() => {
    setIsLeaveConfirmOpen(true);
  }, []);

  // Actual leave logic after confirmation
  const handleLeaveConfirm = useCallback(async () => {
    setIsLeaveConfirmOpen(false);
    try {
      // 1. Disconnect WebSocket FIRST to stop polling/reconnect
      disconnect();
      // 2. Call leave API
      await leaveRoom(roomId);
      // 3. Clean up store
      clearMessages();
      removeRoom(roomId);
      // 4. Navigate
      router.push('/chat');
    } catch {
      toast.error('채팅방 나가기에 실패했습니다.');
    }
  }, [roomId, router, disconnect, clearMessages, removeRoom]);

  // Open review modal
  const handleOpenReview = useCallback(() => {
    if (!room) return;
    const others = room.participants.filter(
      (p) => p.memberId !== currentMemberId && !p.left,
    );
    if (others.length === 0) return;

    // For INDIVIDUAL rooms: auto-select the other participant
    // For GROUP rooms: select first other (future: picker)
    const target = others[0];
    setReviewTargetId(target.memberId);
    const petNames = target.pets?.map((p) => p.name).join(', ');
    setReviewTargetName(petNames || `Member ${target.memberId}`);
    setIsReviewModalOpen(true);
  }, [room, currentMemberId]);

  // Review submitted callback
  const handleReviewSubmitted = useCallback(async () => {
    const refreshed = await getMyReview(roomId).catch(() => null);
    setMyReview(refreshed);
  }, [roomId]);

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

        {/* Review button -- only visible when allConfirmed and no existing review */}
        {walkConfirm?.allConfirmed === true && !myReview?.exists ? (
          <div className="px-4 py-2 bg-white/60 border-b border-zinc-50">
            <button
              onClick={handleOpenReview}
              className="flex items-center gap-2 px-4 py-2 bg-amber-50 text-amber-700 rounded-xl text-sm font-bold hover:bg-amber-100 transition-colors"
            >
              <PenLine size={16} />
              리뷰 작성
            </button>
          </div>
        ) : null}

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

      {/* Walk Review Modal */}
      <WalkReviewModal
        isOpen={isReviewModalOpen}
        onClose={() => setIsReviewModalOpen(false)}
        revieweeId={reviewTargetId}
        revieweeName={reviewTargetName}
        chatRoomId={roomId}
        onReviewSubmitted={handleReviewSubmitted}
      />

      {/* Leave Room Confirm Modal */}
      <ConfirmModal
        isOpen={isLeaveConfirmOpen}
        title="채팅방 나가기"
        message="채팅방을 나가면 대화 내용이 삭제됩니다.\n정말 나가시겠습니까?"
        confirmLabel="나가기"
        cancelLabel="취소"
        onConfirm={handleLeaveConfirm}
        onCancel={() => setIsLeaveConfirmOpen(false)}
        variant="danger"
      />
    </div>
  );
}
