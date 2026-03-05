'use client';

import React, { useState, useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import { X, Search, UserPlus, Check, Loader2 } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import { cn } from '@/lib/utils';
import { memberService } from '@/services/api/memberService';
import { UserType } from '@/types';
import { useRouter } from 'next/navigation';
import { toast } from 'sonner';

interface MemberSearchModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const MemberSearchModal: React.FC<MemberSearchModalProps> = ({ isOpen, onClose }) => {
  const router = useRouter();
  const inputRef = useRef<HTMLInputElement>(null);

  const [query, setQuery] = useState('');
  const [results, setResults] = useState<UserType[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [followingIds, setFollowingIds] = useState<Set<string>>(new Set());
  const [loadingIds, setLoadingIds] = useState<Set<string>>(new Set());

  // 모달 오픈 시 팔로잉 목록 미리 로드
  useEffect(() => {
    if (!isOpen) { setQuery(''); setResults([]); return; }
    inputRef.current?.focus();
    memberService.getFollowing()
      .then((data) => setFollowingIds(new Set((data || []).map((u: UserType) => u.id))))
      .catch(() => {});
  }, [isOpen]);

  // 검색어 debounce
  useEffect(() => {
    const timer = setTimeout(async () => {
      if (!query.trim()) { setResults([]); return; }
      setIsLoading(true);
      try {
        const data = await memberService.searchMembers(query.trim());
        setResults(data || []);
      } catch {
        setResults([]);
      } finally {
        setIsLoading(false);
      }
    }, 300);
    return () => clearTimeout(timer);
  }, [query]);

  const handleFollowToggle = async (e: React.MouseEvent, user: UserType) => {
    e.stopPropagation();
    setLoadingIds(prev => new Set(prev).add(user.id));
    try {
      if (followingIds.has(user.id)) {
        await memberService.unfollow(user.id);
        setFollowingIds(prev => { const s = new Set(prev); s.delete(user.id); return s; });
        toast.success(`${user.nickname}님 팔로우를 취소했습니다.`);
      } else {
        await memberService.follow(user.id);
        setFollowingIds(prev => new Set(prev).add(user.id));
        toast.success(`${user.nickname}님을 팔로우했습니다.`);
      }
    } catch {
      toast.error('처리 중 오류가 발생했습니다.');
    } finally {
      setLoadingIds(prev => { const s = new Set(prev); s.delete(user.id); return s; });
    }
  };

  if (!isOpen) return null;

  return createPortal(
    <div
      className="fixed inset-0 z-[4000] bg-navy-900/40 backdrop-blur-md flex items-center justify-center p-6 animate-in fade-in duration-300"
      onClick={(e) => e.target === e.currentTarget && onClose()}
    >
      <Card className="w-full max-w-md bg-white shadow-2xl animate-in zoom-in-95 duration-500 border-none overflow-hidden flex flex-col h-[600px] rounded-[48px]">
        {/* 헤더 */}
        <div className="p-8 shrink-0 space-y-6 border-b border-zinc-50">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-amber-50 rounded-xl flex items-center justify-center text-amber-500">
                <UserPlus size={22} />
              </div>
              <Typography variant="h3" className="text-navy-900 font-serif">친구 찾기</Typography>
            </div>
            <button onClick={onClose} className="p-2 text-zinc-300 hover:text-navy-900 transition-colors">
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
            <Typography variant="label" className="text-[10px] text-zinc-300 mt-2 ml-2">닉네임 또는 @아이디로 검색할 수 있어요.</Typography>
          </div>
        </div>

        {/* 결과 목록 */}
        <div className="flex-1 overflow-y-auto p-4 space-y-2 no-scrollbar">
          {!query.trim() ? (
            <div className="h-full flex flex-col items-center justify-center gap-3 opacity-30">
              <Search size={48} strokeWidth={1} />
              <Typography variant="label">닉네임을 입력해 이웃을 찾아보세요.</Typography>
            </div>
          ) : isLoading ? (
            <div className="h-full flex flex-col items-center justify-center opacity-20 gap-4">
              <Loader2 className="animate-spin" size={40} />
            </div>
          ) : results.length === 0 ? (
            <div className="h-full flex flex-col items-center justify-center gap-3 opacity-30">
              <UserPlus size={48} strokeWidth={1} />
              <Typography variant="label">&apos;{query}&apos;에 해당하는 이웃이 없어요.</Typography>
            </div>
          ) : (
            results.map((user) => {
              const isFollowing = followingIds.has(user.id);
              const isThisLoading = loadingIds.has(user.id);
              return (
                <div
                  key={user.id}
                  onClick={() => { router.push(`/profile/${user.id}`); onClose(); }}
                  className="flex items-center justify-between p-4 hover:bg-zinc-50 rounded-[24px] transition-all cursor-pointer active:scale-[0.98]"
                >
                  <div className="flex items-center gap-4 min-w-0">
                    <div className="relative shrink-0">
                      <div className="w-12 h-12 rounded-full overflow-hidden shadow-sm border border-zinc-100">
                        <img src={user.avatar || '/AINIINU_ROGO_B.png'} className="w-full h-full object-cover" alt={user.nickname} />
                      </div>
                      {user.dogs?.[0] && (
                        <div className="absolute -bottom-1 -right-1 w-6 h-6 rounded-full border-2 border-white overflow-hidden shadow-sm">
                          <img src={user.dogs[0].image || '/AINIINU_ROGO_B.png'} className="w-full h-full object-cover" alt="Dog" />
                        </div>
                      )}
                    </div>
                    <div className="min-w-0">
                      <Typography variant="body" className="font-black text-sm text-navy-900 leading-tight truncate">
                        {user.nickname}
                      </Typography>
                      <Typography variant="label" className="text-[10px] text-amber-500/80 font-black truncate">
                        {user.handle || `@user_${user.id}`}
                      </Typography>
                      {user.dogs?.[0] && (
                        <Typography variant="label" className="text-[10px] text-zinc-400 truncate">
                          {user.dogs[0].name} · {user.dogs[0].breed}
                        </Typography>
                      )}
                    </div>
                  </div>

                  <button
                    onClick={(e) => handleFollowToggle(e, user)}
                    disabled={isThisLoading}
                    className={cn(
                      'shrink-0 flex items-center gap-1.5 px-4 py-2 rounded-2xl text-xs font-black transition-all',
                      isFollowing
                        ? 'bg-zinc-100 text-zinc-400 hover:bg-red-50 hover:text-red-400'
                        : 'bg-amber-500 text-white hover:bg-amber-600 shadow-sm'
                    )}
                  >
                    {isThisLoading ? (
                      <Loader2 size={12} className="animate-spin" />
                    ) : isFollowing ? (
                      <><Check size={12} /> 팔로잉</>
                    ) : (
                      <><UserPlus size={12} /> 팔로우</>
                    )}
                  </button>
                </div>
              );
            })
          )}
        </div>
      </Card>
    </div>,
    document.body
  );
};
