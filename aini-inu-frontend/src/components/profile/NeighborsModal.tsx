'use client';

import React, { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { X, Users, MessageSquare, Search, Loader2 } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import { cn } from '@/lib/utils';
import { getFollowers, getFollowing } from '@/api/members';
import type { MemberFollowResponse } from '@/api/members';
import { useRouter } from 'next/navigation';
import { toast } from 'sonner';

interface NeighborsModalProps {
  isOpen: boolean;
  onClose: () => void;
  initialType: 'FOLLOWERS' | 'FOLLOWING';
  memberId?: number;  // optional — undefined means current logged-in user (/members/me/...)
}

export const NeighborsModal: React.FC<NeighborsModalProps> = ({
  isOpen,
  onClose,
  initialType,
  memberId,
}) => {
  const router = useRouter();
  const [activeTab, setActiveTab] = useState<'FOLLOWERS' | 'FOLLOWING'>(initialType);
  const [users, setUsers] = useState<MemberFollowResponse[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [hasMore, setHasMore] = useState(false);
  const [page, setPage] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');

  useEffect(() => {
    if (isOpen) {
      setActiveTab(initialType);
      setUsers([]);
      setPage(0);
      fetchUsers(initialType, 0, true);
    }
  }, [isOpen, initialType]);

  const fetchUsers = async (type: 'FOLLOWERS' | 'FOLLOWING', pageNum: number, reset = false) => {
    setIsLoading(true);
    try {
      const res = type === 'FOLLOWERS'
        ? await getFollowers({ memberId, page: pageNum, size: 20 })
        : await getFollowing({ memberId, page: pageNum, size: 20 });
      setUsers(prev => reset ? res.content : [...prev, ...res.content]);
      setHasMore(res.hasNext);
    } catch (e) {
      console.error(e);
      toast.error('이웃 목록을 불러오는데 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleTabChange = (tab: 'FOLLOWERS' | 'FOLLOWING') => {
    setActiveTab(tab);
    setUsers([]);
    setPage(0);
    fetchUsers(tab, 0, true);
  };

  const handleLoadMore = () => {
    const nextPage = page + 1;
    setPage(nextPage);
    fetchUsers(activeTab, nextPage, false);
  };

  if (!isOpen) return null;

  const filteredUsers = users.filter(u =>
    u.nickname.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const formatFollowedAt = (followedAt: string) => {
    try {
      return new Date(followedAt).toLocaleDateString('ko-KR', { year: 'numeric', month: 'short', day: 'numeric' });
    } catch {
      return '';
    }
  };

  return createPortal(
    <div
      className="fixed inset-0 z-[4000] bg-navy-900/40 backdrop-blur-md flex items-center justify-center p-6 animate-in fade-in duration-300"
      onClick={e => e.target === e.currentTarget && onClose()}
    >
      <Card className="w-full max-w-md bg-white shadow-2xl animate-in zoom-in-95 duration-500 border-none overflow-hidden flex flex-col h-[600px] rounded-[48px]">
        <div className="p-8 shrink-0 space-y-6 border-b border-zinc-50">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-amber-50 rounded-xl flex items-center justify-center text-amber-500">
                <Users size={24} />
              </div>
              <Typography variant="h3" className="text-navy-900 font-serif">이웃 목록</Typography>
            </div>
            <button onClick={onClose} className="p-2 text-zinc-300 hover:text-navy-900 transition-colors">
              <X size={32} />
            </button>
          </div>

          <div className="flex bg-zinc-50 p-1 rounded-2xl border border-zinc-100">
            <button
              onClick={() => handleTabChange('FOLLOWERS')}
              className={cn(
                "flex-1 py-3 rounded-xl text-xs font-black transition-all",
                activeTab === 'FOLLOWERS' ? "bg-white text-navy-900 shadow-md" : "text-zinc-400"
              )}
            >
              팔로워 {activeTab === 'FOLLOWERS' ? users.length : ''}
            </button>
            <button
              onClick={() => handleTabChange('FOLLOWING')}
              className={cn(
                "flex-1 py-3 rounded-xl text-xs font-black transition-all",
                activeTab === 'FOLLOWING' ? "bg-white text-navy-900 shadow-md" : "text-zinc-400"
              )}
            >
              팔로잉 {activeTab === 'FOLLOWING' ? users.length : ''}
            </button>
          </div>

          <div className="relative group">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-300 group-focus-within:text-amber-500 transition-colors" size={18} />
            <input
              type="text"
              placeholder="닉네임으로 검색..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full bg-zinc-50 border-none rounded-2xl py-4 pl-12 pr-6 font-bold text-sm text-navy-900 focus:ring-4 ring-amber-500/10 transition-all shadow-inner"
            />
          </div>
        </div>

        <div className="flex-1 overflow-y-auto p-4 space-y-2 no-scrollbar">
          {isLoading && users.length === 0 ? (
            <div className="h-full flex flex-col items-center justify-center opacity-20 gap-4">
              <Loader2 className="animate-spin" size={40} />
              <Typography variant="label">이웃 정보를 불러오는 중...</Typography>
            </div>
          ) : filteredUsers.length === 0 ? (
            <div className="h-full flex flex-col items-center justify-center opacity-30 gap-2">
              <Users size={48} strokeWidth={1} />
              <Typography variant="label">이웃이 없습니다.</Typography>
            </div>
          ) : (
            <>
              {filteredUsers.map((user) => (
                <div
                  key={user.id}
                  onClick={() => {
                    router.push(`/profile/${user.id}`);
                    onClose();
                  }}
                  className="flex items-center justify-between p-4 hover:bg-zinc-50 rounded-[24px] transition-all group cursor-pointer active:scale-[0.98]"
                >
                  <div className="flex items-center gap-4">
                    <div className="relative shrink-0">
                      <div className="w-12 h-12 rounded-full overflow-hidden shadow-sm border border-zinc-100">
                        <img
                          src={user.profileImageUrl || '/default-avatar.png'}
                          className="w-full h-full object-cover"
                          alt={user.nickname}
                        />
                      </div>
                    </div>
                    <div>
                      <Typography variant="body" className="font-black text-sm text-navy-900 leading-tight">
                        {user.nickname}
                      </Typography>
                      <Typography variant="label" className="text-[10px] text-zinc-400 lowercase tracking-widest">
                        @{user.nickname}
                      </Typography>
                      {user.followedAt && (
                        <Typography variant="label" className="text-[9px] text-zinc-300">
                          {formatFollowedAt(user.followedAt)}
                        </Typography>
                      )}
                    </div>
                  </div>

                  <div className="flex gap-2">
                    <Button
                      variant="ghost"
                      size="sm"
                      className="w-10 h-10 p-0 rounded-xl text-zinc-300 hover:text-amber-500 hover:bg-amber-50"
                      onClick={(e) => {
                        e.stopPropagation();
                        toast.info('준비 중인 기능입니다.');
                      }}
                    >
                      <MessageSquare size={18} />
                    </Button>
                  </div>
                </div>
              ))}

              {hasMore && (
                <div className="flex justify-center pt-2 pb-4">
                  <Button
                    variant="outline"
                    size="sm"
                    className="rounded-xl text-xs font-bold"
                    onClick={handleLoadMore}
                    disabled={isLoading}
                  >
                    {isLoading ? <Loader2 className="animate-spin" size={16} /> : '더 보기'}
                  </Button>
                </div>
              )}
            </>
          )}
        </div>
      </Card>
    </div>,
    document.body
  );
};
