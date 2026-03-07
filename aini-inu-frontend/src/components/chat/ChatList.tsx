'use client';

import React, { useState, useEffect, useCallback } from 'react';
import { MessageSquare, Search, Plus, RefreshCw } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Typography } from '@/components/ui/Typography';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import { getRooms, type ChatRoomSummaryResponse } from '@/api/chat';
import { ChatStartModal } from '@/components/chat/ChatStartModal';

type ChatTab = 'DM' | 'WALK' | 'LOST_PET';

export function ChatList() {
  const [activeTab, setActiveTab] = useState<ChatTab>('DM');
  const [searchQuery, setSearchQuery] = useState('');
  const [rooms, setRooms] = useState<ChatRoomSummaryResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [hasError, setHasError] = useState(false);
  const [hasNext, setHasNext] = useState(false);
  const [page, setPage] = useState(0);
  const [isChatStartModalOpen, setIsChatStartModalOpen] = useState(false);
  const params = useParams();
  const currentId = params?.id ? Number(params.id) : null;

  const fetchRooms = useCallback(async (pageNum = 0, append = false) => {
    try {
      setHasError(false);
      if (!append) setIsLoading(true);
      const result = await getRooms({ page: pageNum, size: 20, status: 'ACTIVE', origin: activeTab });
      if (append) {
        setRooms((prev) => [...prev, ...result.content]);
      } else {
        setRooms(result.content);
      }
      setHasNext(result.hasNext);
      setPage(pageNum);
    } catch {
      if (!append) setHasError(true);
    } finally {
      setIsLoading(false);
    }
  }, [activeTab]);

  useEffect(() => {
    fetchRooms();

    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible') {
        fetchRooms();
      }
    };
    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => document.removeEventListener('visibilitychange', handleVisibilityChange);
  }, [fetchRooms]);

  // Client-side search filter only (tab filtering is server-side)
  const filteredRooms = rooms.filter((room) => {
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      const matchSearch =
        room.lastMessage?.content?.toLowerCase().includes(q) ||
        room.displayName?.toLowerCase().includes(q) ||
        room.roomTitle?.toLowerCase().includes(q);
      return matchSearch ?? false;
    }
    return true;
  });

  const handleLoadMore = () => {
    if (hasNext) {
      fetchRooms(page + 1, true);
    }
  };

  // Derive display name from room
  function getRoomDisplayName(room: ChatRoomSummaryResponse): string {
    if (room.origin === 'WALK' || room.origin === 'LOST_PET') {
      return room.roomTitle ?? room.displayName ?? '채팅';
    }
    if (room.displayName) {
      return room.displayName;
    }
    if (room.chatType === 'INDIVIDUAL') {
      return '1:1 채팅';
    }
    return '그룹';
  }

  // Derive display time
  function getDisplayTime(room: ChatRoomSummaryResponse): string {
    const timeStr = room.lastMessage?.sentAt ?? room.updatedAt;
    if (!timeStr) return '';
    try {
      return new Date(timeStr).toLocaleTimeString([], {
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return '';
    }
  }

  return (
    <div className="h-full w-full flex flex-col bg-white border-r border-zinc-100 overflow-hidden">
      <header className="p-6 space-y-6 shrink-0">
        <div className="flex items-center justify-between">
          <div className="space-y-0.5">
            <Typography
              variant="h3"
              className="text-xl md:text-2xl text-navy-900 font-serif lowercase italic leading-none"
            >
              Inu <span className="text-amber-500">Chat</span>
            </Typography>
            <Typography
              variant="label"
              className="text-zinc-400 font-black tracking-[0.3em] text-[10px] uppercase"
            >
              Connections
            </Typography>
          </div>
          <button
            onClick={() => setIsChatStartModalOpen(true)}
            className="w-10 h-10 bg-navy-900 text-white rounded-xl flex items-center justify-center shadow-lg hover:bg-amber-500 hover:text-navy-900 transition-all active:scale-95 group"
          >
            <Plus
              size={20}
              className="group-hover:rotate-90 transition-transform duration-500"
            />
          </button>
        </div>

        <div className="relative group">
          <Search
            className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-300 group-focus-within:text-amber-500 transition-colors"
            size={16}
          />
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
            { id: 'DM' as const, label: 'Messages' },
            { id: 'WALK' as const, label: '산책' },
            { id: 'LOST_PET' as const, label: '실종 신고' },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={cn(
                'py-4 flex items-center gap-2 font-black text-[10px] uppercase tracking-widest transition-all border-b-2 -mb-px',
                activeTab === tab.id
                  ? 'border-navy-900 text-navy-900'
                  : 'border-transparent text-zinc-300 hover:text-zinc-500',
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
        ) : hasError ? (
          <div className="py-20 text-center space-y-4">
            <Typography
              variant="label"
              className="text-zinc-500 text-xs"
            >
              채팅 목록을 불러올 수 없습니다.
            </Typography>
            <button
              onClick={() => fetchRooms()}
              className="inline-flex items-center gap-2 px-4 py-2 bg-navy-900 text-white rounded-xl text-xs font-bold hover:bg-amber-500 hover:text-navy-900 transition-all"
            >
              <RefreshCw size={14} />
              다시 시도
            </button>
          </div>
        ) : filteredRooms.length > 0 ? (
          <>
            {filteredRooms.map((room) => (
              <Link
                key={room.chatRoomId}
                href={`/chat/${room.chatRoomId}`}
                className="block group"
              >
                <div
                  className={cn(
                    'p-4 rounded-[24px] flex items-center gap-4 transition-all duration-300 relative overflow-hidden',
                    currentId === room.chatRoomId
                      ? 'bg-navy-900 text-white shadow-xl translate-x-2'
                      : 'bg-transparent hover:bg-zinc-50',
                  )}
                >
                  <div className="relative shrink-0">
                    <div
                      className={cn(
                        'w-12 h-12 rounded-[18px] p-0.5 shadow-md transition-transform duration-500',
                        currentId === room.chatRoomId
                          ? 'bg-white/20'
                          : 'bg-white',
                      )}
                    >
                      <img
                        src="/AINIINU_ROGO_B.png"
                        alt={getRoomDisplayName(room)}
                        className="w-full h-full rounded-[16px] object-cover"
                      />
                    </div>
                  </div>

                  <div className="flex-1 min-w-0 space-y-1">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <Typography
                          variant="body"
                          className={cn(
                            'font-black text-sm truncate',
                            currentId === room.chatRoomId
                              ? 'text-white'
                              : 'text-navy-900',
                          )}
                        >
                          {getRoomDisplayName(room)}
                        </Typography>
                        {room.chatType === 'GROUP' && (
                          <span
                            className={cn(
                              'text-[8px] font-black px-1.5 py-0.5 rounded-md',
                              currentId === room.chatRoomId
                                ? 'bg-white/20 text-white'
                                : 'bg-amber-50 text-amber-600',
                            )}
                          >
                            그룹
                          </span>
                        )}
                      </div>
                      <Typography
                        variant="label"
                        className={cn(
                          'text-[8px] font-black uppercase tracking-tighter shrink-0',
                          currentId === room.chatRoomId
                            ? 'text-white/40'
                            : 'text-zinc-300',
                        )}
                      >
                        {getDisplayTime(room)}
                      </Typography>
                    </div>

                    <Typography
                      variant="body"
                      className={cn(
                        'text-[11px] truncate leading-none',
                        currentId === room.chatRoomId
                          ? 'text-white/60'
                          : 'text-zinc-400',
                      )}
                    >
                      {room.lastMessage?.content ?? '대화가 시작되었습니다.'}
                    </Typography>
                  </div>
                </div>
              </Link>
            ))}
            {hasNext && (
              <button
                onClick={handleLoadMore}
                className="w-full py-3 text-center text-xs font-bold text-zinc-400 hover:text-navy-900 transition-colors"
              >
                더 보기
              </button>
            )}
          </>
        ) : (
          <div className="py-20 text-center space-y-4 opacity-20">
            <MessageSquare size={32} className="mx-auto text-navy-900" />
            <Typography
              variant="label"
              className="font-black uppercase tracking-widest text-[8px]"
            >
              Empty
            </Typography>
          </div>
        )}
      </div>

      <ChatStartModal
        isOpen={isChatStartModalOpen}
        onClose={() => setIsChatStartModalOpen(false)}
        onRoomCreated={() => fetchRooms()}
      />
    </div>
  );
}
