'use client';

import React, { useState, useEffect } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { ChatHeader } from '@/components/chat/ChatHeader';
import { MessageList } from '@/components/chat/MessageList';
import { ChatInput } from '@/components/chat/ChatInput';
import { ProfileExplorer } from '@/components/chat/ProfileExplorer';
import { chatService } from '@/services/api/chatService';
import { ChatRoom } from '@/types';
import { memberService } from '@/services/api/memberService';
import { threadService } from '@/services/api/threadService';
import { Loader2 } from 'lucide-react';
import { toast } from 'sonner';

type ChatRoomType = ChatRoom;

export default function ChatRoomPage() {
  const params = useParams();
  const roomId = params?.id as string;
  const router = useRouter();

  const [room, setRoom] = useState<ChatRoomType | null>(null);
  const [messages, setMessages] = useState<any[]>([]);
  const [inputText, setInputText] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isProfileOpen, setIsProfileOpen] = useState(false);
  const [currentUser, setCurrentUser] = useState<any>(null);
  const [partnerHasRecentDiary, setPartnerHasRecentDiary] = useState(false);

  const fetchData = async () => {
    try {
      const [rooms, msgs, user] = await Promise.all([
        chatService.getRooms(),
        chatService.getMessages(roomId),
        memberService.getMe(),
      ]);

      const currentRoom = rooms.find(r => r.id === roomId);
      if (!currentRoom) {
        toast.error('채팅방을 찾을 수 없습니다.');
        router.push('/chat');
        return;
      }

      setRoom(currentRoom);
      setMessages(msgs);
      setCurrentUser(user);

      // ChatHeader amber ring 표시를 위한 파트너 다이어리 조회
      try {
        const partnerDiaries = await threadService.getWalkDiaries(currentRoom.partner.id);
        const now = Date.now();
        const oneDayMs = 24 * 60 * 60 * 1000;
        const hasRecent = Object.values(partnerDiaries || {}).some((d: any) => {
          const ts = d.createdAt
            ? new Date(d.createdAt).getTime()
            : d.walkDate
              ? new Date(d.walkDate.replace(/\./g, '-')).getTime()
              : 0;
          return ts > 0 && now - ts <= oneDayMs;
        });
        setPartnerHasRecentDiary(hasRecent);
      } catch { /* 무시 */ }
    } catch (e) {
      console.error(e);
      toast.error('대화 내용을 불러오는데 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (roomId) fetchData();

    // [Prototype] Polling for new messages simulation
    const interval = setInterval(async () => {
      if (!roomId) return;
      try {
        const msgs = await chatService.getMessages(roomId);
        setMessages(msgs);
      } catch (e) { console.error(e); }
    }, 3000);

    return () => clearInterval(interval);
  }, [roomId]);

  const handleSendMessage = async () => {
    if (!inputText.trim()) return;
    try {
      const sentMsg = await chatService.sendMessage(roomId, inputText);
      setMessages([...messages, sentMsg]);
      setInputText('');
    } catch {
      toast.error('메시지 전송 실패');
    }
  };

  if (isLoading || !room || !currentUser) {
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
          partner={room.partner}
          isConfirmed={false}
          hasRecentDiary={partnerHasRecentDiary}
          showInfo={isProfileOpen}
          onShowInfoToggle={() => setIsProfileOpen(!isProfileOpen)}
          onBack={() => router.push('/chat')}
          roomTitle={room.partner.nickname}
        />

        <MessageList
          messages={messages}
          currentUserId={currentUser.id}
          partner={room.partner}
        />

        <ChatInput
          inputText={inputText}
          setInputText={setInputText}
          onSend={handleSendMessage}
        />
      </div>

      {/* 4th Pane: Profile Explorer */}
      <ProfileExplorer
        partnerId={room.partner.id}
        isOpen={isProfileOpen}
        onClose={() => setIsProfileOpen(false)}
      />
    </div>
  );
}
