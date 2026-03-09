'use client';

import React, { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import { getActivityStats } from '@/api/members';
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

function isLoading<T>(state: SectionState<T>) {
  return state.status === 'idle' || state.status === 'loading';
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
  // Subscribe to profile with a stable selector (not entire store)
  const userProfile = useUserStore((s) => s.profile);
  const fetchProfile = useUserStore((s) => s.fetchProfile);

  // Trigger profile fetch on mount (same as useProfile but without full-store subscription)
  useEffect(() => {
    fetchProfile();
  }, [fetchProfile]);

  // Dashboard store — subscribe to individual slices (Zustand only re-renders when selected value changes)
  const activityStats = useDashboardStore((s) => s.activityStats);
  const hotspots = useDashboardStore((s) => s.hotspots);
  const threads = useDashboardStore((s) => s.threads);
  const myPets = useDashboardStore((s) => s.myPets);
  const myPetsLoaded = useDashboardStore((s) => s.myPetsLoaded);
  const recentFriends = useDashboardStore((s) => s.recentFriends);
  const recentFriendsLoaded = useDashboardStore((s) => s.recentFriendsLoaded);

  // Stable mainDog — only recompute when myPets actually changes
  const mainDog = useMemo(() => {
    const mainPet = myPets.find(p => p.isMain) || myPets[0];
    return mainPet
      ? { name: mainPet.name, image: mainPet.photoUrl || '/images/dog-portraits/Mixed Breed.png', breed: mainPet.breed?.name || '믹스견' }
      : { name: '댕댕이', image: '/images/dog-portraits/Mixed Breed.png', breed: '믹스견' };
  }, [myPets]);

  // Stable userProfile subset for DashboardHero (avoid passing full profile object)
  const heroProfile = useMemo(() => ({
    nickname: userProfile?.nickname,
    mannerScore: userProfile?.mannerScore,
    location: userProfile?.location,
  }), [userProfile?.nickname, userProfile?.mannerScore, userProfile?.location]);

  // Pending reviews (local state — not cached)
  const [pendingReviews, setPendingReviews] = useState<PendingReview[]>([]);
  const [pendingReviewModalOpen, setPendingReviewModalOpen] = useState(false);

  // --- Fetch all dashboard data ---

  const fetchingRef = useRef(false);

  const fetchAllData = useCallback(async () => {
    if (fetchingRef.current) return;
    fetchingRef.current = true;

    const store = useDashboardStore.getState();
    const isRefresh = store.lastFetchedAt !== null;

    // Set loading states upfront (single batch via direct store access)
    if (!isRefresh) {
      useDashboardStore.setState({
        activityStats: { status: 'loading' },
        hotspots: { status: 'loading' },
        threads: { status: 'loading' },
      });
    }

    try {
      await Promise.allSettled([
        // activityStats
        (async () => {
          try {
            const data = await getActivityStats();
            useDashboardStore.setState({ activityStats: { status: 'success', data } });
          } catch {
            const s = useDashboardStore.getState();
            if (!hasData(s.activityStats)) {
              useDashboardStore.setState({ activityStats: { status: 'error', message: '활동 통계를 불러오지 못했습니다.' } });
            }
          }
        })(),
        // hotspots
        (async () => {
          try {
            const data = await getHotspots();
            useDashboardStore.setState({
              hotspots: data.length === 0 ? { status: 'empty' } : { status: 'success', data },
            });
          } catch {
            const s = useDashboardStore.getState();
            if (!hasData(s.hotspots)) {
              useDashboardStore.setState({ hotspots: { status: 'error', message: '추천 정보를 불러오지 못했습니다.' } });
            }
          }
        })(),
        // threads
        (async () => {
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
            useDashboardStore.setState({
              threads: res.content.length === 0 ? { status: 'empty' } : { status: 'success', data: res.content },
            });
          } catch {
            const s = useDashboardStore.getState();
            if (!hasData(s.threads)) {
              useDashboardStore.setState({ threads: { status: 'error', message: '동네 소식을 불러오지 못했습니다.' } });
            }
          }
        })(),
        // myPets
        (async () => {
          try {
            const pets = await getMyPets();
            useDashboardStore.getState().setMyPets(pets);
          } catch {
            const s = useDashboardStore.getState();
            if (s.myPets.length === 0) s.setMyPets([]);
          }
        })(),
        // recentFriends
        (async () => {
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
            useDashboardStore.getState().setRecentFriends(friends);
          } catch {
            const s = useDashboardStore.getState();
            if (s.recentFriends.length === 0) s.setRecentFriends([]);
          }
        })(),
        // pendingReviews
        (async () => {
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
        })(),
      ]);

      useDashboardStore.getState().markFetched();
    } finally {
      fetchingRef.current = false;
    }
  }, []);

  // --- Retry handlers (stable refs) ---

  const retryActivityStats = useCallback(async () => {
    useDashboardStore.setState({ activityStats: { status: 'loading' } });
    try {
      const data = await getActivityStats();
      useDashboardStore.setState({ activityStats: { status: 'success', data } });
    } catch {
      useDashboardStore.setState({ activityStats: { status: 'error', message: '활동 통계를 불러오지 못했습니다.' } });
    }
  }, []);

  const retryHotspots = useCallback(async () => {
    useDashboardStore.setState({ hotspots: { status: 'loading' } });
    try {
      const data = await getHotspots();
      useDashboardStore.setState({
        hotspots: data.length === 0 ? { status: 'empty' } : { status: 'success', data },
      });
    } catch {
      useDashboardStore.setState({ hotspots: { status: 'error', message: '추천 정보를 불러오지 못했습니다.' } });
    }
  }, []);

  const retryThreads = useCallback(async () => {
    useDashboardStore.setState({ threads: { status: 'loading' } });
    try {
      let lat = 37.5666, lng = 126.9784;
      try {
        const pos = await new Promise<GeolocationPosition>((resolve, reject) =>
          navigator.geolocation.getCurrentPosition(resolve, reject, { timeout: 5000 })
        );
        lat = pos.coords.latitude;
        lng = pos.coords.longitude;
      } catch { /* default coords */ }
      const res = await getThreads({ page: 0, size: 3, latitude: lat, longitude: lng, radius: 5 });
      useDashboardStore.setState({
        threads: res.content.length === 0 ? { status: 'empty' } : { status: 'success', data: res.content },
      });
    } catch {
      useDashboardStore.setState({ threads: { status: 'error', message: '동네 소식을 불러오지 못했습니다.' } });
    }
  }, []);

  // --- Main data fetch ---

  useEffect(() => {
    if (!userProfile) return;
    if (!useDashboardStore.getState().shouldFetch()) return;
    fetchAllData();
  }, [!!userProfile, fetchAllData]);

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
        {isLoading(hotspots) && <SectionSkeleton />}
        {hotspots.status === 'error' && (
          <SectionErrorFallback message={hotspots.message} onRetry={retryHotspots} />
        )}
        {hotspots.status === 'empty' && (
          <AIBanner hotspots={[]} dogName={mainDog.name} />
        )}
        {hasData(hotspots) && (
          <AIBanner hotspots={hotspots.data} dogName={mainDog.name} />
        )}

        {/* (3) Dashboard Hero -- activityStats + myPets must both be ready */}
        {(isLoading(activityStats) || !myPetsLoaded) && <SectionSkeleton />}
        {activityStats.status === 'error' && myPetsLoaded && (
          <SectionErrorFallback message={activityStats.message} onRetry={retryActivityStats} />
        )}
        {myPetsLoaded && (hasData(activityStats) || activityStats.status === 'empty') && (
          <DashboardHero
            userProfile={heroProfile}
            mainDog={mainDog}
            activityStats={hasData(activityStats) ? activityStats.data : null}
          />
        )}

        {/* (4) Recent Friends */}
        {!recentFriendsLoaded ? <SectionSkeleton /> : <RecentFriends friends={recentFriends} />}

        {/* (5) Local Feed Preview -- threads */}
        {isLoading(threads) && <SectionSkeleton />}
        {threads.status === 'error' && (
          <LocalFeedPreview threads={[]} error={threads.message} onRetry={retryThreads} />
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
          onReviewSubmitted={fetchAllData}
        />
      </div>
    </div>
  );
}
