'use client';

import React, { useState, useEffect } from 'react';
import { MessageSquare, Search, Plus } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Typography } from '@/components/ui/Typography';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import { chatService, ChatRoomType } from '@/services/api/chatService';
import { ChatStartModal } from '@/components/chat/ChatStartModal';

type ChatTab = 'ACTIVE' | 'PAST';

export function ChatList() {
  const [activeTab, setActiveTab] = useState<ChatTab>('ACTIVE');
  const [searchQuery, setSearchQuery] = useState('');
  const [rooms, setRooms] = useState<ChatRoomType[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isChatStartModalOpen, setIsChatStartModalOpen] = useState(false);
  const params = useParams();
  const currentId = params?.id as string;

  const fetchRooms = async () => {
    try {
      const data = await chatService.getRooms();
      setRooms(data);
    } catch (e) {
      console.error('Failed to fetch chat rooms:', e);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchRooms();
  }, []);

  const filteredRooms = (rooms || []).filter(room => {
    // [TODO] 서버사이드 필터링이 정석이나, 프로토타입 환경이므로 클라이언트 필터링 유지
    const matchSearch = room?.partner?.nickname?.toLowerCase().includes(searchQuery.toLowerCase()) || 
                        room?.lastMessage?.toLowerCase().includes(searchQuery.toLowerCase());
    return matchSearch;
  });

  return (
    <div className="h-full w-full flex flex-col bg-white border-r border-zinc-100 overflow-hidden">
      <header className="p-6 space-y-6 shrink-0">
        <div className="flex items-center justify-between">
          <div className="space-y-0.5">
            <Typography variant="h3" className="text-xl md:text-2xl text-navy-900 font-serif lowercase italic leading-none">
              Inu <span className="text-amber-500">Chat</span>
            </Typography>
            <Typography variant="label" className="text-zinc-400 font-black tracking-[0.3em] text-[10px] uppercase">
              Connections
            </Typography>
          </div>
          <button
            onClick={() => setIsChatStartModalOpen(true)}
            className="w-10 h-10 bg-navy-900 text-white rounded-xl flex items-center justify-center shadow-lg hover:bg-amber-500 hover:text-navy-900 transition-all active:scale-95 group"
          >
            <Plus size={20} className="group-hover:rotate-90 transition-transform duration-500" />
          </button>
        </div>

        <div className="relative group">
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-300 group-focus-within:text-amber-500 transition-colors" size={16} />
          <input 
            type="text" 
            placeholder="검색..." 
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full bg-zinc-50 border-none rounded-2xl py-3.5 pl-12 pr-6 text-xs font-bold focus:outline-none focus:ring-4 ring-navy-900/5 transition-all shadow-inner" 
          />
        </div>
      </header>

      <div className="px-6 shrink-0">
        <div className="flex gap-6 border-b border-zinc-100">
           {[
             { id: 'ACTIVE', label: 'Messages', count: rooms?.length || 0 },
             { id: 'PAST', label: 'History', count: 0 },
           ].map(tab => (
             <button
               key={tab.id}
               onClick={() => setActiveTab(tab.id as ChatTab)}
               className={cn(
                 "py-4 flex items-center gap-2 font-black text-[10px] uppercase tracking-widest transition-all border-b-2 -mb-px",
                 activeTab === tab.id 
                  ? 'border-navy-900 text-navy-900' 
                  : 'border-transparent text-zinc-300 hover:text-zinc-500'
               )}
             >
               {tab.label}
             </button>
           ))}
        </div>
      </div>

      <div className="flex-1 overflow-y-auto no-scrollbar p-4 space-y-3">
        {isLoading ? (
          <div className="py-20 text-center space-y-4 opacity-20">
             <div className="w-8 h-8 border-4 border-navy-900 border-t-transparent rounded-full animate-spin mx-auto" />
          </div>
        ) : filteredRooms.length > 0 ? (
          filteredRooms.map((room) => (
            <Link key={room.id} href={`/chat/${room.id}`} className="block group">
              <div className={cn(
                "p-4 rounded-[24px] flex items-center gap-4 transition-all duration-300 relative overflow-hidden",
                currentId === room.id
                  ? "bg-navy-900 text-white shadow-xl translate-x-2"
                  : (room.unreadCount > 0 
                      ? "bg-amber-50 ring-1 ring-amber-100" 
                      : "bg-transparent hover:bg-zinc-50")
              )}>
                <div className="relative shrink-0">
                  <div className={cn(
                    "w-12 h-12 rounded-[18px] p-0.5 shadow-md transition-transform duration-500",
                    currentId === room.id ? "bg-white/20" : "bg-white"
                  )}>
                    <img src={room?.partner?.avatar || ''} alt={room?.partner?.nickname || ''} className="w-full h-full rounded-[16px] object-cover" />
                  </div>
                  {(room.unreadCount || 0) > 0 && currentId !== room.id && (
                    <div className="absolute -top-1.5 -right-1.5 bg-red-500 text-white text-[8px] font-black w-5 h-5 rounded-full flex items-center justify-center border-2 border-white shadow-lg animate-bounce">
                      {room.unreadCount}
                    </div>
                  )}
                </div>

                <div className="flex-1 min-w-0 space-y-1">
                  <div className="flex items-center justify-between">
                    <Typography variant="body" className={cn(
                      "font-black text-sm truncate",
                      currentId === room.id ? "text-white" : "text-navy-900"
                    )}>
                      {room?.partner?.nickname || '알 수 없는 이웃'}
                    </Typography>
                    <Typography variant="label" className={cn(
                      "text-[8px] font-black uppercase tracking-tighter shrink-0",
                      currentId === room.id ? "text-white/40" : "text-zinc-300"
                    )}>
                      {room.lastMessageTime ? new Date(room.lastMessageTime).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'}) : ''}
                    </Typography>
                  </div>

                  <Typography variant="body" className={cn(
                    "text-[11px] truncate leading-none",
                    currentId === room.id ? "text-white/60" : ((room.unreadCount || 0) > 0 ? "text-navy-900 font-black" : "text-zinc-400")
                  )}>
                    {room.lastMessage || '대화가 시작되었습니다.'}
                  </Typography>
                </div>
              </div>
            </Link>
          ))
        ) : (
          <div className="py-20 text-center space-y-4 opacity-20">
             <MessageSquare size={32} className="mx-auto text-navy-900" />
             <Typography variant="label" className="font-black uppercase tracking-widest text-[8px]">Empty</Typography>
          </div>
        )}
      </div>

      <ChatStartModal
        isOpen={isChatStartModalOpen}
        onClose={() => setIsChatStartModalOpen(false)}
        onRoomCreated={fetchRooms}
      />
    </div>
  );
}
