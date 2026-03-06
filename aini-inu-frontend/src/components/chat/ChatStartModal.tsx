'use client';

import React, { useState, useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import { X, Search, MessageCircle, Loader2, Users } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import { cn } from '@/lib/utils';
import {
  searchMembers,
  getFollowing,
  type MemberFollowResponse,
} from '@/api/members';
import { createDirectRoom } from '@/api/chat';
import { useRouter } from 'next/navigation';
import { toast } from 'sonner';

interface ChatStartModalProps {
  isOpen: boolean;
  onClose: () => void;
  onRoomCreated: () => void;
}

// Unified display type for both following users and search results
interface DisplayUser {
  memberId: number;
  nickname: string;
  profileImageUrl: string | null;
}

export const ChatStartModal: React.FC<ChatStartModalProps> = ({
  isOpen,
  onClose,
  onRoomCreated,
}) => {
  const router = useRouter();
  const inputRef = useRef<HTMLInputElement>(null);

  const [query, setQuery] = useState('');
  const [followingUsers, setFollowingUsers] = useState<DisplayUser[]>([]);
  const [searchResults, setSearchResults] = useState<DisplayUser[]>([]);
  const [isLoadingFollowing, setIsLoadingFollowing] = useState(false);
  const [isSearching, setIsSearching] = useState(false);
  const [startingChatId, setStartingChatId] = useState<number | null>(null);

  // Load following list on modal open
  useEffect(() => {
    if (!isOpen) {
      setQuery('');
      setSearchResults([]);
      return;
    }
    inputRef.current?.focus();
    setIsLoadingFollowing(true);
    getFollowing()
      .then((result) => {
        const users: DisplayUser[] = (result.content ?? []).map(
          (f: MemberFollowResponse) => ({
            memberId: f.id,
            nickname: f.nickname,
            profileImageUrl: f.profileImageUrl,
          }),
        );
        setFollowingUsers(users);
      })
      .catch(() => setFollowingUsers([]))
      .finally(() => setIsLoadingFollowing(false));
  }, [isOpen]);

  // Debounced search
  useEffect(() => {
    if (!query.trim()) {
      setSearchResults([]);
      return;
    }
    const timer = setTimeout(async () => {
      setIsSearching(true);
      try {
        const result = await searchMembers(query.trim());
        const users: DisplayUser[] = (result.content ?? []).map((m) => ({
          memberId: m.id,
          nickname: m.nickname,
          profileImageUrl: m.profileImageUrl,
        }));
        setSearchResults(users);
      } catch {
        setSearchResults([]);
      } finally {
        setIsSearching(false);
      }
    }, 300);
    return () => clearTimeout(timer);
  }, [query]);

  const handleStartChat = async (user: DisplayUser) => {
    if (startingChatId !== null) return;
    setStartingChatId(user.memberId);
    try {
      const room = await createDirectRoom({ partnerId: user.memberId });
      onRoomCreated();
      onClose();
      router.push(`/chat/${room.chatRoomId}`);
    } catch {
      toast.error('채팅방을 만들 수 없습니다.');
    } finally {
      setStartingChatId(null);
    }
  };

  const displayUsers = query.trim() ? searchResults : followingUsers;
  const isLoading = query.trim() ? isSearching : isLoadingFollowing;

  if (!isOpen) return null;

  return createPortal(
    <div
      className="fixed inset-0 z-[4000] bg-navy-900/40 backdrop-blur-md flex items-center justify-center p-6 animate-in fade-in duration-300"
      onClick={(e) => e.target === e.currentTarget && onClose()}
    >
      <Card className="w-full max-w-md bg-white shadow-2xl animate-in zoom-in-95 duration-500 border-none overflow-hidden flex flex-col h-[600px] rounded-[48px]">
        {/* Header */}
        <div className="p-8 shrink-0 space-y-6 border-b border-zinc-50">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-amber-50 rounded-xl flex items-center justify-center text-amber-500">
                <MessageCircle size={22} />
              </div>
              <div>
                <Typography variant="h3" className="text-navy-900 font-serif">
                  새 대화 시작
                </Typography>
                <Typography
                  variant="label"
                  className="text-zinc-400 text-[10px] font-black uppercase tracking-widest normal-case"
                >
                  {!query.trim() ? '팔로잉 이웃' : '검색 결과'}
                </Typography>
              </div>
            </div>
            <button
              onClick={onClose}
              className="p-2 text-zinc-300 hover:text-navy-900 transition-colors"
            >
              <X size={28} />
            </button>
          </div>

          <div className="relative group">
            <Search
              className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-300 group-focus-within:text-amber-500 transition-colors"
              size={18}
            />
            <input
              ref={inputRef}
              type="text"
              placeholder="닉네임 또는 @아이디 검색..."
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              className="w-full bg-zinc-50 border-none rounded-2xl py-4 pl-12 pr-6 font-bold text-sm text-navy-900 focus:ring-4 ring-amber-500/10 transition-all shadow-inner outline-none"
            />
          </div>
        </div>

        {/* List */}
        <div className="flex-1 overflow-y-auto p-4 space-y-2 no-scrollbar">
          {isLoading ? (
            <div className="h-full flex flex-col items-center justify-center opacity-20 gap-4">
              <Loader2 className="animate-spin" size={40} />
            </div>
          ) : displayUsers.length === 0 ? (
            <div className="h-full flex flex-col items-center justify-center gap-3 opacity-30">
              {query.trim() ? (
                <>
                  <Search size={48} strokeWidth={1} />
                  <Typography variant="label">
                    &apos;{query}&apos;에 해당하는 이웃이 없어요.
                  </Typography>
                </>
              ) : (
                <>
                  <Users size={48} strokeWidth={1} />
                  <Typography variant="label">
                    아직 팔로잉하는 이웃이 없어요.
                  </Typography>
                  <Typography variant="label" className="text-[10px]">
                    동네 탐색에서 이웃을 찾아보세요.
                  </Typography>
                </>
              )}
            </div>
          ) : (
            displayUsers.map((user) => {
              const isStarting = startingChatId === user.memberId;
              return (
                <button
                  key={user.memberId}
                  onClick={() => handleStartChat(user)}
                  disabled={startingChatId !== null}
                  className="w-full flex items-center justify-between p-4 hover:bg-zinc-50 rounded-[24px] transition-all cursor-pointer active:scale-[0.98] disabled:opacity-60 text-left"
                >
                  <div className="flex items-center gap-4 min-w-0">
                    <div className="relative shrink-0">
                      <div className="w-12 h-12 rounded-full overflow-hidden shadow-sm border border-zinc-100">
                        <img
                          src={user.profileImageUrl || '/AINIINU_ROGO_B.png'}
                          className="w-full h-full object-cover"
                          alt={user.nickname}
                        />
                      </div>
                    </div>
                    <div className="min-w-0">
                      <Typography
                        variant="body"
                        className="font-black text-sm text-navy-900 leading-tight truncate"
                      >
                        {user.nickname}
                      </Typography>
                    </div>
                  </div>

                  <div
                    className={cn(
                      'shrink-0 flex items-center gap-1.5 px-4 py-2 rounded-2xl text-xs font-black transition-all bg-navy-900 text-white hover:bg-amber-500 hover:text-navy-900 shadow-sm',
                      isStarting && 'bg-amber-500 text-navy-900',
                    )}
                  >
                    {isStarting ? (
                      <Loader2 size={12} className="animate-spin" />
                    ) : (
                      <>
                        <MessageCircle size={12} /> 채팅
                      </>
                    )}
                  </div>
                </button>
              );
            })
          )}
        </div>
      </Card>
    </div>,
    document.body,
  );
};
