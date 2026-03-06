'use client';

import React, { useState } from 'react';
import { MapPin, Zap, Footprints, ArrowRight, Users, Loader2, Crown } from 'lucide-react';
import { cn, calculateDistance, formatRemainingTime } from '@/lib/utils';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import { Badge } from '@/components/ui/Badge';
import type { ThreadSummaryResponse } from '@/api/threads';

interface RadarSidebarProps {
  isLoading: boolean;
  threads: ThreadSummaryResponse[];
  hasNext: boolean;
  coordinates: [number, number];
  currentTime: Date;
  isExpired: (startTime: string) => boolean;
  currentUserId: number | undefined;
  onCardClick: (threadId: number) => void;
  onLoadMore: () => void;
  onDeleteThread: (threadId: number) => void;
  onEditThread: (threadId: number) => void;
  myActiveThread?: ThreadSummaryResponse | null;
}

function getRemainingBadge(startTime: string, currentTime: Date): { label: string; expired: boolean } {
  try {
    const start = new Date(startTime);
    const expiry = new Date(start.getTime() + 60 * 60 * 1000);
    const diff = expiry.getTime() - currentTime.getTime();
    if (diff <= 0) return { label: '만료됨', expired: true };
    return { label: formatRemainingTime(diff), expired: false };
  } catch {
    return { label: '알 수 없음', expired: false };
  }
}

function formatDistance(km: number): string {
  if (km < 1) return `${Math.round(km * 1000)}m`;
  return `${km.toFixed(1)}km`;
}

export const RadarSidebar: React.FC<RadarSidebarProps> = ({
  isLoading,
  threads,
  hasNext,
  coordinates,
  currentTime,
  isExpired,
  currentUserId: _currentUserId,
  onCardClick,
  onLoadMore,
  onDeleteThread: _onDeleteThread,
  onEditThread: _onEditThread,
  myActiveThread,
}) => {
  const [sortBy, setSortBy] = useState<'DISTANCE' | 'TIME'>('DISTANCE');
  const [loadingMore, setLoadingMore] = useState(false);

  // Exclude my active thread from main list (it's pinned at top separately)
  const filteredThreads = myActiveThread
    ? threads.filter((t) => t.id !== myActiveThread.id)
    : threads;

  const sortedThreads = [...filteredThreads].sort((a, b) => {
    if (sortBy === 'DISTANCE') {
      const da = calculateDistance(coordinates[0], coordinates[1], a.latitude, a.longitude);
      const db = calculateDistance(coordinates[0], coordinates[1], b.latitude, b.longitude);
      return da - db;
    }
    // TIME: lexicographic sort on ISO strings
    return a.startTime.localeCompare(b.startTime);
  });

  const handleLoadMore = async () => {
    setLoadingMore(true);
    try {
      await onLoadMore();
    } finally {
      setLoadingMore(false);
    }
  };

  return (
    <div className="flex-1 flex flex-col gap-8 overflow-y-auto no-scrollbar min-w-[350px]">
      {/* Header */}
      <div className="flex items-center justify-between px-4">
        <div className="space-y-1">
          <Typography variant="h3" className="text-3xl font-black">
            동네 산책 <span className="text-amber-500 italic font-serif">스레드</span>
          </Typography>
          <Typography variant="body" className="text-xs text-zinc-400 font-bold">
            지금 우리 동네에서 이웃들이 메이트를 기다리고 있어요.
          </Typography>
        </div>
        <div className="flex bg-zinc-50 p-1 rounded-xl border border-zinc-100">
          <button
            onClick={() => setSortBy('DISTANCE')}
            className={cn(
              'px-3 py-1.5 rounded-lg text-[10px] font-black transition-all',
              sortBy === 'DISTANCE' ? 'bg-white text-navy-900 shadow-sm' : 'text-zinc-300',
            )}
          >
            거리순
          </button>
          <button
            onClick={() => setSortBy('TIME')}
            className={cn(
              'px-3 py-1.5 rounded-lg text-[10px] font-black transition-all',
              sortBy === 'TIME' ? 'bg-white text-navy-900 shadow-sm' : 'text-zinc-300',
            )}
          >
            시간순
          </button>
        </div>
      </div>

      {/* My active thread — pinned at top */}
      {myActiveThread && (
        <div className="px-1">
          <Card
            interactive
            className="relative p-6 bg-gradient-to-br from-amber-50 to-white border-2 border-amber-300 shadow-xl rounded-[48px] ring-2 ring-amber-200/50"
            onClick={() => onCardClick(myActiveThread.id)}
          >
            {/* Pinned badge */}
            <div className="absolute top-4 right-4 flex items-center gap-1 bg-amber-500 text-white px-3 py-1 rounded-full text-[10px] font-black shadow-md">
              <Crown size={10} /> 내 모집글
            </div>

            {/* Content */}
            <div className="flex gap-5">
              <div className="relative shrink-0">
                <div className="w-20 h-20 rounded-[32px] bg-amber-100 flex items-center justify-center border-4 border-white shadow-xl overflow-hidden">
                  <Footprints size={28} className="text-amber-400" />
                </div>
              </div>
              <div className="flex-1 space-y-2 pt-1 pr-16">
                <Typography variant="h3" className="text-lg font-black text-navy-900 line-clamp-1">
                  {myActiveThread.title}
                </Typography>
                <div className="flex items-center gap-1 text-[10px] text-zinc-400 font-bold">
                  <MapPin size={10} className="text-amber-500 shrink-0" />
                  <span className="truncate max-w-[140px]">{myActiveThread.placeName}</span>
                  <span className="text-amber-500/40 mx-1">|</span>
                  <span className="text-amber-600 font-black">
                    {formatDistance(calculateDistance(coordinates[0], coordinates[1], myActiveThread.latitude, myActiveThread.longitude))}
                  </span>
                </div>
                <div className="flex items-center gap-3 text-xs font-black text-zinc-300 uppercase tracking-widest">
                  <Users size={13} /> {myActiveThread.currentParticipants}/{myActiveThread.maxParticipants}
                  <Badge variant="amber" className="bg-zinc-50 text-zinc-500 border-none text-[10px] px-2">
                    {myActiveThread.chatType === 'INDIVIDUAL' ? '1:1' : '그룹'}
                  </Badge>
                </div>
              </div>
            </div>
          </Card>
        </div>
      )}

      {/* Thread list */}
      <div className="space-y-6 pb-24 px-1">
        {isLoading ? (
          <div className="p-20 text-center opacity-20">
            <Loader2 className="animate-spin mx-auto" size={40} />
          </div>
        ) : sortedThreads.length === 0 ? (
          <Card className="p-12 text-center bg-white border-card-border shadow-xl rounded-[48px]">
            <Footprints size={40} className="mx-auto text-zinc-200 mb-4" />
            <Typography variant="h3" className="text-lg font-black text-zinc-400 mb-2">
              주변에 산책 스레드가 없어요
            </Typography>
            <Typography variant="body" className="text-sm text-zinc-300">
              RECRUIT 탭에서 모집을 시작해보세요!
            </Typography>
          </Card>
        ) : (
          sortedThreads.map((thread) => {
            const { label: remainingLabel, expired } = getRemainingBadge(thread.startTime, currentTime);
            const distanceKm = calculateDistance(
              coordinates[0],
              coordinates[1],
              thread.latitude,
              thread.longitude,
            );
            const isParticipating = thread.applied || thread.isApplied;
            const expired2 = isExpired(thread.startTime);

            return (
              <Card
                key={thread.id}
                interactive
                className={cn(
                  'relative p-8 bg-white border-card-border shadow-xl rounded-[48px]',
                  expired2 && 'opacity-60',
                )}
                onClick={() => onCardClick(thread.id)}
              >
                {/* Top row: place + distance + badges */}
                <div className="flex justify-between items-start mb-6">
                  <div className="flex items-center gap-2">
                    <div className="flex items-center gap-1 text-[10px] text-zinc-400 font-bold">
                      <MapPin size={10} className="text-amber-500 shrink-0" />
                      <span className="truncate max-w-[120px]">{thread.placeName}</span>
                      <span className="text-amber-500/40 mx-1">|</span>
                      <span className="text-amber-600 font-black">{formatDistance(distanceKm)}</span>
                    </div>
                  </div>
                  <div className="flex flex-col items-end gap-2">
                    {thread.currentParticipants >= thread.maxParticipants ? (
                      <Badge variant="default" className="bg-blue-100 text-blue-600 border-none px-3 py-1 text-[10px]">
                        모집 완료
                      </Badge>
                    ) : expired || expired2 ? (
                      <Badge variant="default" className="bg-red-100 text-red-500 border-none px-3 py-1 text-[10px]">
                        만료됨
                      </Badge>
                    ) : (
                      <Badge variant="amber" className="bg-amber-50 text-amber-600 border-none px-3 py-1 text-[10px] flex items-center gap-1">
                        <Zap size={10} fill="currentColor" /> {remainingLabel}
                      </Badge>
                    )}
                    {isParticipating && (
                      <Badge variant="default" className="bg-emerald-100 text-emerald-600 border-none px-3 py-1 text-[10px]">
                        참여 중
                      </Badge>
                    )}
                  </div>
                </div>

                {/* Content */}
                <div className="flex gap-6 mb-8">
                  <div className="relative shrink-0">
                    <div className="w-28 h-28 rounded-[40px] bg-zinc-100 flex items-center justify-center border-4 border-white shadow-2xl overflow-hidden">
                      <Footprints size={36} className="text-zinc-300" />
                    </div>
                  </div>
                  <div className="flex-1 space-y-3 pt-2">
                    <Typography variant="h3" className="text-2xl font-black text-navy-900 line-clamp-1">
                      {thread.title}
                    </Typography>
                    {thread.description && (
                      <Typography variant="body" className="text-base text-zinc-400 line-clamp-2 leading-relaxed">
                        &quot;{thread.description}&quot;
                      </Typography>
                    )}
                  </div>
                </div>

                {/* Footer */}
                <div className="flex items-center justify-between pt-8 border-t border-zinc-50">
                  <div className="flex items-center gap-4">
                    <Badge variant="amber" className="bg-zinc-50 text-zinc-500 border-none text-[10px] px-2">
                      {thread.chatType === 'INDIVIDUAL' ? '1:1' : '그룹'}
                    </Badge>
                    <div className="flex items-center gap-2 text-xs font-black text-zinc-300 uppercase tracking-widest">
                      <Users size={14} /> {thread.currentParticipants}/{thread.maxParticipants}
                    </div>
                  </div>
                  <div className="w-12 h-12 bg-navy-900 text-white rounded-2xl flex items-center justify-center shadow-xl">
                    <ArrowRight size={20} />
                  </div>
                </div>
              </Card>
            );
          })
        )}

        {/* Load more */}
        {hasNext && !isLoading && (
          <button
            onClick={handleLoadMore}
            disabled={loadingMore}
            className="w-full py-4 rounded-[32px] border border-zinc-100 bg-white text-sm font-black text-zinc-400 hover:text-navy-900 hover:border-zinc-200 transition-all disabled:opacity-50"
          >
            {loadingMore ? <Loader2 size={16} className="animate-spin mx-auto" /> : '더 보기'}
          </button>
        )}
      </div>
    </div>
  );
};
