'use client';

import React, { useState, useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import { X, Search, User, Loader2 } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import { cn } from '@/lib/utils';
import { searchMembers } from '@/api/members';
import type { MemberResponse } from '@/api/members';
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
  const [results, setResults] = useState<MemberResponse[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [hasMore, setHasMore] = useState(false);

  // Reset and focus on open
  useEffect(() => {
    if (!isOpen) {
      setQuery('');
      setResults([]);
      return;
    }
    // Auto-focus on next tick to ensure modal is mounted
    setTimeout(() => {
      inputRef.current?.focus();
    }, 50);
  }, [isOpen]);

  // Debounced search-as-you-type
  useEffect(() => {
    if (!query.trim()) {
      setResults([]);
      setHasMore(false);
      return;
    }
    const timer = setTimeout(async () => {
      setIsSearching(true);
      try {
        const res = await searchMembers(query.trim(), { page: 0, size: 20 });
        setResults(res.content);
        setHasMore(res.hasNext);
      } catch {
        toast.error('검색에 실패했습니다.');
        setResults([]);
      } finally {
        setIsSearching(false);
      }
    }, 300);
    return () => clearTimeout(timer);
  }, [query]);

  const handleResultClick = (member: MemberResponse) => {
    router.push(`/profile/${member.id}`);
    onClose();
  };

  const getMannerColor = (score: number) => {
    if (score >= 9) return 'bg-emerald-500';
    if (score >= 7) return 'bg-amber-500';
    if (score >= 4) return 'bg-blue-500';
    return 'bg-zinc-400';
  };

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
                <Search size={22} />
              </div>
              <Typography variant="h3" className="text-navy-900 font-serif">회원 검색</Typography>
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
              placeholder="회원 이름으로 검색해보세요"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              className="w-full bg-zinc-50 border-none rounded-2xl py-4 pl-12 pr-6 font-bold text-sm text-navy-900 focus:ring-4 ring-amber-500/10 transition-all shadow-inner outline-none"
            />
          </div>
        </div>

        {/* Results */}
        <div className="flex-1 overflow-y-auto p-4 space-y-2 no-scrollbar">
          {!query.trim() ? (
            <div className="h-full flex flex-col items-center justify-center gap-3 opacity-30">
              <Search size={48} strokeWidth={1} />
              <Typography variant="label">회원 이름으로 검색해보세요</Typography>
            </div>
          ) : isSearching ? (
            <div className="h-full flex flex-col items-center justify-center opacity-20 gap-4">
              <Loader2 className="animate-spin" size={40} />
            </div>
          ) : results.length === 0 ? (
            <div className="h-full flex flex-col items-center justify-center gap-3 opacity-30">
              <User size={48} strokeWidth={1} />
              <Typography variant="label">검색 결과가 없습니다</Typography>
            </div>
          ) : (
            <>
              {results.map((member) => (
                <div
                  key={member.id}
                  onClick={() => handleResultClick(member)}
                  className="flex items-center gap-4 p-4 hover:bg-zinc-50 rounded-[24px] transition-all cursor-pointer active:scale-[0.98]"
                >
                  {/* Avatar */}
                  <div className="shrink-0 w-12 h-12 rounded-full overflow-hidden shadow-sm border border-zinc-100 bg-zinc-50 flex items-center justify-center">
                    {member.profileImageUrl ? (
                      <img
                        src={member.profileImageUrl}
                        className="w-full h-full object-cover"
                        alt={member.nickname}
                      />
                    ) : (
                      <User size={24} className="text-zinc-300" />
                    )}
                  </div>

                  {/* Info */}
                  <div className="flex-1 min-w-0">
                    <Typography variant="body" className="font-black text-sm text-navy-900 leading-tight truncate">
                      {member.nickname}
                    </Typography>
                    {member.selfIntroduction && (
                      <Typography variant="label" className="text-[10px] text-zinc-400 truncate">
                        {member.selfIntroduction.length > 50
                          ? `${member.selfIntroduction.slice(0, 50)}...`
                          : member.selfIntroduction}
                      </Typography>
                    )}
                    {/* Manner temperature */}
                    <div className="flex items-center gap-1.5 mt-1">
                      <span className="text-[9px] font-black text-zinc-400 uppercase tracking-tighter">매너온도</span>
                      <span
                        className={cn(
                          'text-[10px] font-black px-1.5 py-0.5 rounded-md text-white',
                          getMannerColor(member.mannerTemperature)
                        )}
                      >
                        {member.mannerTemperature}/10
                      </span>
                    </div>
                  </div>
                </div>
              ))}
              {hasMore && (
                <div className="text-center py-2">
                  <Typography variant="label" className="text-[10px] text-zinc-300">
                    더 많은 결과가 있습니다
                  </Typography>
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
