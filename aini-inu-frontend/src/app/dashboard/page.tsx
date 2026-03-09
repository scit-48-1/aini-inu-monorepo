'use client';

import React, { useState, useEffect, useCallback } from 'react';
import { useProfile } from '@/hooks/useProfile';
import { getWalkStats } from '@/api/members';
import { getHotspots, getThreads } from '@/api/threads';
import { getRooms, getRoom, getMyReview } from '@/api/chat';
import { getMyPets } from '@/api/pets';
import { useUserStore } from '@/store/useUserStore';
import { useDashboardStore, type SectionState } from '@/store/useDashboardStore';
import { RefreshCw } from 'lucide-react';
import { AIBanner } from '@/components/dashboard/AIBanner';
import { DashboardHero } from '@/components/dashboard/DashboardHero';
import { RecentFriends } from '@/components/dashboard/RecentFriends';
import { LocalFeedPreview } from '@/components/dashboard/LocalFeedPreview';
import { PendingReviewCard } from '@/components/dashboard/PendingReviewCard';
import { PendingReviewModal, type PendingReview } from '@/components/dashboard/PendingReviewModal';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';

// --- Helper: has data (success or refreshing) ---

function hasData<T>(state: SectionState<T>): state is { status: 'success'; data: T } | { status: 'refreshing'; data: T } {
  return state.status === 'success' || state.status === 'refreshing';
}

// --- Section error fallback ---

function SectionErrorFallback({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <div className="p-12 flex flex-col items-center gap-4 bg-zinc-50/50 rounded-[32px] border border-zinc-100/50">
      <Typography variant="body" className="text-zinc-400 text-sm">{message}</Typography>
      <Button variant="ghost" size="sm" onClick={onRetry} className="gap-2">
        <RefreshCw size={14} /> 다시 시도
      </Button>
    </div>
  );
}

// --- Section loading skeleton ---

function SectionSkeleton() {
  return (
    <div className="h-32 bg-zinc-50/50 rounded-[32px] border border-zinc-100/50 animate-pulse" />
  );
}

export default function DashboardPage() {
  const { profile: userProfile } = useProfile();

  // Dashboard store selectors
  const walkStats = useDashboardStore((s) => s.walkStats);
  const hotspots = useDashboardStore((s) => s.hotspots);
  const threads = useDashboardStore((s) => s.threads);
  const myPets = useDashboardStore((s) => s.myPets);
  const recentFriends = useDashboardStore((s) => s.recentFriends);
  const setWalkStats = useDashboardStore((s) => s.setWalkStats);
  const setHotspots = useDashboardStore((s) => s.setHotspots);
  const setThreads = useDashboardStore((s) => s.setThreads);
  const setMyPets = useDashboardStore((s) => s.setMyPets);
  const setRecentFriends = useDashboardStore((s) => s.setRecentFriends);
  const markFetched = useDashboardStore((s) => s.markFetched);
  const shouldFetch = useDashboardStore((s) => s.shouldFetch);

  const mainPet = myPets.find(p => p.isMain) || myPets[0];
  const mainDog = mainPet
    ? { name: mainPet.name, image: mainPet.photoUrl || '/images/dog-portraits/Mixed Breed.png', breed: mainPet.breed?.name || '믹스견' }
    : { name: '댕댕이', image: '/images/dog-portraits/Mixed Breed.png', breed: '믹스견' };

  // Pending reviews (local state — not cached)
  const [pendingReviews, setPendingReviews] = useState<PendingReview[]>([]);
  const [pendingReviewModalOpen, setPendingReviewModalOpen] = useState(false);

  // --- Fetch functions with stale-while-revalidate ---

  const fetchWalkStats = useCallback(async (isRefresh = false) => {
    if (isRefresh && hasData(walkStats)) {
      setWalkStats({ status: 'refreshing', data: walkStats.data });
    } else if (!hasData(walkStats)) {
      setWalkStats({ status: 'loading' });
    }
    try {
      const data = await getWalkStats();
      setWalkStats({ status: 'success', data });
    } catch {
      if (!hasData(walkStats)) {
        setWalkStats({ status: 'error', message: '산책 활동을 불러오지 못했습니다.' });
      }
    }
  }, [walkStats, setWalkStats]);

  const fetchHotspots = useCallback(async (isRefresh = false) => {
    if (isRefresh && hasData(hotspots)) {
      setHotspots({ status: 'refreshing', data: hotspots.data });
    } else if (!hasData(hotspots)) {
      setHotspots({ status: 'loading' });
    }
    try {
      const data = await getHotspots();
      if (data.length === 0) {
        setHotspots({ status: 'empty' });
      } else {
        setHotspots({ status: 'success', data });
      }
    } catch {
      if (!hasData(hotspots)) {
        setHotspots({ status: 'error', message: '추천 정보를 불러오지 못했습니다.' });
      }
    }
  }, [hotspots, setHotspots]);

  const fetchThreads = useCallback(async (isRefresh = false) => {
    if (isRefresh && hasData(threads)) {
      setThreads({ status: 'refreshing', data: threads.data });
    } else if (!hasData(threads)) {
      setThreads({ status: 'loading' });
    }
    try {
      let lat = 37.5666;
      let lng = 126.9784;
      try {
        const pos = await new Promise<GeolocationPosition>((resolve, reject) =>
          navigator.geolocation.getCurrentPosition(resolve, reject, { timeout: 5000 })
        );
        lat = pos.coords.latitude;
        lng = pos.coords.longitude;
      } catch {
        // Use default Seoul coordinates
      }
      const res = await getThreads({ page: 0, size: 3, latitude: lat, longitude: lng, radius: 5 });
      if (res.content.length === 0) {
        setThreads({ status: 'empty' });
      } else {
        setThreads({ status: 'success', data: res.content });
      }
    } catch {
      if (!hasData(threads)) {
        setThreads({ status: 'error', message: '동네 소식을 불러오지 못했습니다.' });
      }
    }
  }, [threads, setThreads]);

  // --- Pending review detection ---

  const detectPendingReviews = useCallback(async () => {
    try {
      const roomsRes = await getRooms({ page: 0, size: 20, origin: 'WALK' });
      const rooms = roomsRes.content;
      if (rooms.length === 0) {
        setPendingReviews([]);
        return;
      }

      const [detailResults, reviewResults] = await Promise.all([
        Promise.allSettled(rooms.map((r) => getRoom(r.chatRoomId))),
        Promise.allSettled(rooms.map((r) => getMyReview(r.chatRoomId))),
      ]);

      const currentId = Number(useUserStore.getState().profile?.id) || 0;
      const pending: PendingReview[] = [];

      rooms.forEach((room, i) => {
        const reviewResult = reviewResults[i];
        if (reviewResult.status !== 'fulfilled' || reviewResult.value.exists) return;

        const detailResult = detailResults[i];
        if (detailResult.status !== 'fulfilled') return;
        const detail = detailResult.value;
        if (!detail.walkConfirmed) return;

        const partner = detail.participants.find((p) => p.memberId !== currentId && !p.left);
        if (!partner) return;

        pending.push({
          chatRoomId: detail.chatRoomId,
          displayName: room.displayName,
          partnerId: partner.memberId,
          partnerNickname: partner.nickname || `Member ${partner.memberId}`,
          profileImageUrl: partner.profileImageUrl,
        });
      });

      setPendingReviews(pending);
    } catch {
      setPendingReviews([]);
    }
  }, []);

  // --- Fetch my pets ---

  const fetchMyPets = useCallback(async () => {
    try {
      const pets = await getMyPets();
      setMyPets(pets);
    } catch {
      if (myPets.length === 0) setMyPets([]);
    }
  }, [myPets.length, setMyPets]);

  // --- Recent friends ---

  const fetchRecentFriends = useCallback(async () => {
    try {
      const roomsRes = await getRooms({ page: 0, size: 5 });
      const roomSummaries = roomsRes.content.slice(0, 5);
      const detailResults = await Promise.allSettled(
        roomSummaries.map((r) => getRoom(r.chatRoomId)),
      );
      const currentId = Number(useUserStore.getState().profile?.id) || 0;
      const seenMembers = new Set<number>();
      const friends: { id: string; roomId: string; name: string; img: string; score: number }[] = [];
      for (const res of detailResults) {
        if (res.status !== 'fulfilled') continue;
        const detail = res.value;
        const partner = detail.participants.find((p) => p.memberId !== currentId && !p.left);
        if (!partner || seenMembers.has(partner.memberId)) continue;
        seenMembers.add(partner.memberId);
        const petNames = partner.pets?.map((p) => p.name).join(', ');
        friends.push({
          id: String(partner.memberId),
          roomId: String(detail.chatRoomId),
          name: petNames || partner.nickname || '산책 친구',
          img: partner.profileImageUrl || '/AINIINU_ROGO_B.png',
          score: 7.0,
        });
      }
      setRecentFriends(friends);
    } catch {
      if (recentFriends.length === 0) setRecentFriends([]);
    }
  }, [recentFriends.length, setRecentFriends]);

  // --- Main data fetch ---

  useEffect(() => {
    if (!userProfile) return;
    if (!shouldFetch()) return;

    const isRefresh = useDashboardStore.getState().lastFetchedAt !== null;

    const fetchAll = async () => {
      await Promise.allSettled([
        fetchWalkStats(isRefresh),
        fetchHotspots(isRefresh),
        fetchThreads(isRefresh),
        fetchMyPets(),
        fetchRecentFriends(),
        detectPendingReviews(),
      ]);
      markFetched();
    };

    fetchAll();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [userProfile]);

  if (!userProfile) {
    return (
      <div className="flex items-center justify-center h-full">
        <p className="text-zinc-400">Loading...</p>
      </div>
    );
  }

  return (
    <div className="p-6 md:p-10 space-y-10 animate-in fade-in duration-700 h-full overflow-y-auto no-scrollbar">
      <div className="max-w-7xl mx-auto space-y-10 pb-20">
        {/* (1) Pending Review Card -- conditional */}
        <PendingReviewCard
          pendingCount={pendingReviews.length}
          onClick={() => setPendingReviewModalOpen(true)}
        />

        {/* (2) AI Banner -- hotspots */}
        {hotspots.status === 'loading' && <SectionSkeleton />}
        {hotspots.status === 'error' && (
          <SectionErrorFallback message={hotspots.message} onRetry={() => fetchHotspots()} />
        )}
        {hotspots.status === 'empty' && (
          <AIBanner hotspots={[]} dogName={mainDog.name} />
        )}
        {hasData(hotspots) && (
          <AIBanner hotspots={hotspots.data} dogName={mainDog.name} />
        )}

        {/* (3) Dashboard Hero -- userProfile from useProfile + walkStats */}
        {walkStats.status === 'loading' && <SectionSkeleton />}
        {walkStats.status === 'error' && (
          <SectionErrorFallback message={walkStats.message} onRetry={() => fetchWalkStats()} />
        )}
        {(hasData(walkStats) || walkStats.status === 'empty') && (
          <DashboardHero
            userProfile={userProfile}
            mainDog={mainDog}
            walkStats={hasData(walkStats) ? walkStats.data : null}
          />
        )}

        {/* (4) Recent Friends -- keep existing pattern */}
        <RecentFriends friends={recentFriends} />

        {/* (5) Local Feed Preview -- threads */}
        {threads.status === 'loading' && <SectionSkeleton />}
        {threads.status === 'error' && (
          <LocalFeedPreview threads={[]} error={threads.message} onRetry={() => fetchThreads()} />
        )}
        {threads.status === 'empty' && (
          <LocalFeedPreview threads={[]} />
        )}
        {hasData(threads) && (
          <LocalFeedPreview threads={threads.data} />
        )}

        {/* Pending Review Modal */}
        <PendingReviewModal
          isOpen={pendingReviewModalOpen}
          onClose={() => setPendingReviewModalOpen(false)}
          pendingReviews={pendingReviews}
          onReviewSubmitted={detectPendingReviews}
        />
      </div>
    </div>
  );
}
