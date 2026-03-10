'use client';

import React, { useState, useEffect, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { RefreshCw, Footprints, Clock, ThermometerSun, User } from 'lucide-react';
import { toast } from 'sonner';
import { cn } from '@/lib/utils';
import { getActiveWalkers } from '@/api/walkingSession';
import type { WalkingUserResponse } from '@/api/walkingSession';
import { useUserStore } from '@/store/useUserStore';

function formatElapsed(startedAt: string): string {
  const diffMs = Date.now() - Date.parse(startedAt);
  const minutes = Math.floor(diffMs / 60_000);
  if (minutes < 60) return `${minutes}분`;
  const hours = Math.floor(minutes / 60);
  const remainMinutes = minutes % 60;
  return `${hours}시간 ${remainMinutes}분`;
}

export const WalkingStatusSection: React.FC = () => {
  const router = useRouter();
  const [walkers, setWalkers] = useState<WalkingUserResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [, setTick] = useState(0);

  const profile = useUserStore((s) => s.profile);
  const currentUserId = profile ? Number(profile.id) : undefined;

  const fetchWalkers = useCallback(async () => {
    try {
      const data = await getActiveWalkers();
      setWalkers(data);
    } catch {
      toast.error('산책중인 유저 목록을 불러오는데 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchWalkers();
  }, [fetchWalkers]);

  // Tick every minute to update elapsed time display
  useEffect(() => {
    const interval = setInterval(() => setTick((t) => t + 1), 60_000);
    return () => clearInterval(interval);
  }, []);

  const handleRefresh = useCallback(async () => {
    setIsRefreshing(true);
    await fetchWalkers();
    setIsRefreshing(false);
  }, [fetchWalkers]);

  const mySession = walkers.find((w) => w.memberId === currentUserId);
  const otherWalkers = walkers.filter((w) => w.memberId !== currentUserId);

  if (isLoading) {
    return (
      <div className="flex-1 flex items-center justify-center">
        <p className="text-zinc-400 text-sm">로딩중...</p>
      </div>
    );
  }

  return (
    <div className="flex-1 overflow-y-auto no-scrollbar p-2">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-2">
          <Footprints size={20} className="text-green-500" />
          <h3 className="text-lg font-black text-black">
            산책중인 유저 <span className="text-green-500">{walkers.length}</span>
          </h3>
        </div>
        <button
          onClick={handleRefresh}
          disabled={isRefreshing}
          className="flex items-center gap-1.5 text-xs font-black text-black bg-zinc-50/50 px-3 py-2 rounded-full border border-zinc-100 hover:bg-white hover:border-green-500 hover:shadow-sm transition-all active:scale-95 disabled:opacity-50"
        >
          <RefreshCw size={14} className={cn('text-green-500', isRefreshing && 'animate-spin')} />
          새로고침
        </button>
      </div>

      {mySession && (
        <div
          onClick={() => router.push(`/profile/${mySession.memberId}`)}
          className="mb-6 bg-green-50 border border-green-200 rounded-[24px] p-5 cursor-pointer hover:shadow-md transition-all"
        >
          <div className="flex items-center gap-2 mb-2">
            <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse" />
            <span className="text-xs font-black text-green-700">나의 산책</span>
          </div>
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-green-100 rounded-full flex items-center justify-center overflow-hidden">
              {mySession.profileImageUrl ? (
                <img src={mySession.profileImageUrl} alt="" className="w-full h-full object-cover" />
              ) : (
                <User size={20} className="text-green-600" />
              )}
            </div>
            <div className="flex-1">
              <p className="text-sm font-black text-green-800">{mySession.nickname}</p>
              <div className="flex items-center gap-3 mt-0.5">
                <span className="flex items-center gap-1 text-xs text-green-600">
                  <Clock size={10} /> {formatElapsed(mySession.walkingStartedAt)}
                </span>
                <span className="flex items-center gap-1 text-xs text-green-600">
                  <ThermometerSun size={10} /> {mySession.mannerTemperature.toFixed(1)}
                </span>
              </div>
            </div>
          </div>
        </div>
      )}

      {walkers.length === 0 && (
        <div className="flex flex-col items-center justify-center py-20 text-zinc-400">
          <Footprints size={48} className="mb-4 opacity-30" />
          <p className="text-sm font-black">현재 산책중인 유저가 없습니다</p>
        </div>
      )}

      <div className="space-y-3">
        {otherWalkers.map((walker) => (
          <div
            key={walker.memberId}
            onClick={() => router.push(`/profile/${walker.memberId}`)}
            className="bg-white border border-zinc-100 rounded-[20px] p-4 hover:border-green-200 hover:shadow-sm transition-all cursor-pointer"
          >
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-zinc-50 rounded-full flex items-center justify-center overflow-hidden">
                {walker.profileImageUrl ? (
                  <img src={walker.profileImageUrl} alt="" className="w-full h-full object-cover" />
                ) : (
                  <User size={20} className="text-zinc-400" />
                )}
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-black text-black truncate">{walker.nickname}</p>
                <div className="flex items-center gap-3 mt-0.5">
                  <span className="flex items-center gap-1 text-xs text-zinc-500">
                    <Clock size={10} /> {formatElapsed(walker.walkingStartedAt)}
                  </span>
                  <span className="flex items-center gap-1 text-xs text-zinc-500">
                    <ThermometerSun size={10} /> {walker.mannerTemperature.toFixed(1)}
                  </span>
                </div>
              </div>
              <div className="w-2 h-2 bg-green-400 rounded-full animate-pulse" />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
