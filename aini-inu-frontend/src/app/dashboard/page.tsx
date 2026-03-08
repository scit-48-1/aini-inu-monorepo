'use client';

import React, { useState, useEffect, useCallback } from 'react';
import { useProfile } from '@/hooks/useProfile';
import { getWalkStats } from '@/api/members';
import { getHotspots, getThreads } from '@/api/threads';
import { getRooms, getRoom, getMyReview } from '@/api/chat';
import { getMyPets } from '@/api/pets';
import type { PetResponse } from '@/api/pets';
import { useUserStore } from '@/store/useUserStore';
import { RefreshCw } from 'lucide-react';
import { AIBanner } from '@/components/dashboard/AIBanner';
import { DashboardHero } from '@/components/dashboard/DashboardHero';
import { RecentFriends } from '@/components/dashboard/RecentFriends';
import { LocalFeedPreview } from '@/components/dashboard/LocalFeedPreview';
import { PendingReviewCard } from '@/components/dashboard/PendingReviewCard';
import { PendingReviewModal, type PendingReview } from '@/components/dashboard/PendingReviewModal';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import type { WalkStatsResponse } from '@/api/members';
import type { ThreadHotspotResponse, ThreadSummaryResponse } from '@/api/threads';

// --- Per-section state pattern ---

type SectionState<T> =
  | { status: 'loading' }
  | { status: 'error'; message: string }
  | { status: 'empty' }
  | { status: 'success'; data: T };

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
  const [myPets, setMyPets] = useState<PetResponse[]>([]);

  const mainPet = myPets.find(p => p.isMain) || myPets[0];
  const mainDog = mainPet
    ? { name: mainPet.name, image: mainPet.photoUrl || '/images/dog-portraits/Mixed Breed.png', breed: mainPet.breed?.name || '믹스견' }
    : { name: '댕댕이', image: '/images/dog-portraits/Mixed Breed.png', breed: '믹스견' };

  // Per-section states
  const [walkStats, setWalkStats] = useState<SectionState<WalkStatsResponse>>({ status: 'loading' });
  const [hotspots, setHotspots] = useState<SectionState<ThreadHotspotResponse[]>>({ status: 'loading' });
  const [threads, setThreads] = useState<SectionState<ThreadSummaryResponse[]>>({ status: 'loading' });
  const [recentFriends, setRecentFriends] = useState<{ id: string; roomId: string; name: string; img: string; score: number }[]>([]);
  const [pendingReviews, setPendingReviews] = useState<PendingReview[]>([]);
  const [pendingReviewModalOpen, setPendingReviewModalOpen] = useState(false);

  // --- Retry functions (per-section) ---

  const fetchWalkStats = useCallback(async () => {
    setWalkStats({ status: 'loading' });
    try {
      const data = await getWalkStats();
      setWalkStats({ status: 'success', data });
    } catch {
      setWalkStats({ status: 'error', message: '산책 활동을 불러오지 못했습니다.' });
    }
  }, []);

  const fetchHotspots = useCallback(async () => {
    setHotspots({ status: 'loading' });
    try {
      const data = await getHotspots();
      if (data.length === 0) {
        setHotspots({ status: 'empty' });
      } else {
        setHotspots({ status: 'success', data });
      }
    } catch {
      setHotspots({ status: 'error', message: '추천 정보를 불러오지 못했습니다.' });
    }
  }, []);

  const fetchThreads = useCallback(async () => {
    setThreads({ status: 'loading' });
    try {
      const res = await getThreads({ page: 0, size: 3 });
      if (res.content.length === 0) {
        setThreads({ status: 'empty' });
      } else {
        setThreads({ status: 'success', data: res.content });
      }
    } catch {
      setThreads({ status: 'error', message: '동네 소식을 불러오지 못했습니다.' });
    }
  }, []);

  // --- Pending review detection ---

  const detectPendingReviews = useCallback(async () => {
    try {
      const roomsRes = await getRooms({ page: 0, size: 20 });
      const rooms = roomsRes.content;
      if (rooms.length === 0) {
        setPendingReviews([]);
        return;
      }

      // Check review status for each room
      const reviewResults = await Promise.allSettled(
        rooms.map((r) => getMyReview(r.chatRoomId)),
      );

      // Find rooms without reviews
      const roomsWithoutReview = rooms.filter((_, i) => {
        const result = reviewResults[i];
        return result.status === 'fulfilled' && !result.value.exists;
      });

      if (roomsWithoutReview.length === 0) {
        setPendingReviews([]);
        return;
      }

      // Get room details to extract partner info
      const detailResults = await Promise.allSettled(
        roomsWithoutReview.map((r) => getRoom(r.chatRoomId)),
      );

      const currentId = Number(useUserStore.getState().profile?.id) || 0;
      const pending: PendingReview[] = [];

      detailResults.forEach((res, i) => {
        if (res.status !== 'fulfilled') return;
        const detail = res.value;
        const partner = detail.participants.find((p) => p.memberId !== currentId && !p.left);
        if (!partner) return;
        pending.push({
          chatRoomId: detail.chatRoomId,
          displayName: roomsWithoutReview[i].displayName,
          partnerId: partner.memberId,
          partnerNickname: partner.nickname || `Member ${partner.memberId}`,
        });
      });

      setPendingReviews(pending);
    } catch {
      // Pending review detection failure is non-critical
      setPendingReviews([]);
    }
  }, []);

  // --- Fetch my pets ---

  const fetchMyPets = useCallback(async () => {
    try {
      const pets = await getMyPets();
      setMyPets(pets);
    } catch {
      // Non-critical: fallback to empty (default mainDog used)
      setMyPets([]);
    }
  }, []);

  // --- Recent friends (keep existing pattern) ---

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
      setRecentFriends([]);
    }
  }, []);

  // --- Main data fetch (parallel with Promise.allSettled) ---

  useEffect(() => {
    if (!userProfile) return;

    const fetchAll = async () => {
      await Promise.allSettled([
        fetchWalkStats(),
        fetchHotspots(),
        fetchThreads(),
        fetchMyPets(),
        fetchRecentFriends(),
        detectPendingReviews(),
      ]);
    };

    fetchAll();
  }, [userProfile, fetchWalkStats, fetchHotspots, fetchThreads, fetchMyPets, fetchRecentFriends, detectPendingReviews]);

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
          <SectionErrorFallback message={hotspots.message} onRetry={fetchHotspots} />
        )}
        {hotspots.status === 'empty' && (
          <AIBanner hotspots={[]} dogName={mainDog.name} />
        )}
        {hotspots.status === 'success' && (
          <AIBanner hotspots={hotspots.data} dogName={mainDog.name} />
        )}

        {/* (3) Dashboard Hero -- userProfile from useProfile + walkStats */}
        {walkStats.status === 'loading' && <SectionSkeleton />}
        {walkStats.status === 'error' && (
          <SectionErrorFallback message={walkStats.message} onRetry={fetchWalkStats} />
        )}
        {(walkStats.status === 'success' || walkStats.status === 'empty') && (
          <DashboardHero
            userProfile={userProfile}
            mainDog={mainDog}
            walkStats={walkStats.status === 'success' ? walkStats.data : null}
          />
        )}

        {/* (4) Recent Friends -- keep existing pattern */}
        <RecentFriends friends={recentFriends} />

        {/* (5) Local Feed Preview -- threads */}
        {threads.status === 'loading' && <SectionSkeleton />}
        {threads.status === 'error' && (
          <LocalFeedPreview threads={[]} error={threads.message} onRetry={fetchThreads} />
        )}
        {threads.status === 'empty' && (
          <LocalFeedPreview threads={[]} />
        )}
        {threads.status === 'success' && (
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
