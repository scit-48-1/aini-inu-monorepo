'use client';

import React, { useState, useEffect, useMemo } from 'react';
import { useProfile } from '@/hooks/useProfile';
import { threadService } from '@/services/api/threadService';
import { memberService } from '@/services/api/memberService';
import { getRooms } from '@/api/chat';
import { AIBanner } from '@/components/dashboard/AIBanner';
import { DashboardHero } from '@/components/dashboard/DashboardHero';
import { RecentFriends } from '@/components/dashboard/RecentFriends';
import { LocalFeedPreview } from '@/components/dashboard/LocalFeedPreview';
import { DraftNotification } from '@/components/dashboard/DraftNotification';
import { ThreadType } from '@/types';

export default function DashboardPage() {
  const { profile: userProfile } = useProfile();
  // Safe Access: 강아지가 없어도 앱이 죽지 않도록 방어
  const mainDog = userProfile?.dogs?.[0] || {
    name: '댕댕이',
    image: '/images/dog-portraits/Mixed Breed.png',
    breed: '믹스견'
  };

  const [hotspot, setHotspot] = useState({ region: '성수동 서울숲', count: 12 });
  const [threads, setThreads] = useState<ThreadType[]>([]);
  const [draftCount, setDraftCount] = useState(0);
  const [recentFriends, setRecentFriends] = useState<{ id: string; roomId: string; name: string; img: string; score: number }[]>([]);
  const [grassData, setGrassData] = useState<number[]>(new Array(126).fill(0));

  const totalWalks = useMemo(() => grassData.reduce((a, b) => a + (b > 0 ? 1 : 0), 0), [grassData]);

  const fetchData = async () => {
    try {
      const [threadData, diaryRes, hotspotRes, roomsRes, walkStats] = await Promise.allSettled([
        threadService.getThreads(),
        threadService.getWalkDiaries(),
        threadService.getHotspots(),
        getRooms({ page: 0, size: 5 }),
        memberService.getWalkStats()
      ]);

      if (threadData.status === 'fulfilled' && threadData.value) {
        setThreads(threadData.value.slice(0, 3));
      }

      if (diaryRes.status === 'fulfilled' && diaryRes.value && typeof diaryRes.value === 'object') {
        const count = Object.values(diaryRes.value).filter((d: any) => d?.isDraft).length;
        setDraftCount(count);
      }

      if (hotspotRes.status === 'fulfilled' && hotspotRes.value) {
        setHotspot(hotspotRes.value);
      }

      if (walkStats.status === 'fulfilled' && walkStats.value) {
        setGrassData(walkStats.value);
      }

      // Recent Friends from Chat History -- ChatRoomSummaryResponse lacks partner details,
      // so we show room IDs as placeholder friends for now
      if (roomsRes.status === 'fulfilled' && roomsRes.value?.content) {
        const friends = roomsRes.value.content.slice(0, 5).map((r) => ({
          id: String(r.chatRoomId),
          roomId: String(r.chatRoomId),
          name: `Chat ${r.chatRoomId}`,
          img: '/AINIINU_ROGO_B.png',
          score: 7.0,
        }));
        setRecentFriends(friends);
      }
    } catch (e) {
      console.error('Dashboard fetchData error:', e);
    }
  };

  useEffect(() => {
    // 렌더링 이후 비동기로 데이터 페칭 시작
    const init = async () => {
      await fetchData();
    };
    init();
  }, []); // fetchData를 의존성에서 제거하거나 useCallback 처리 필요 (현재는 빈 배열로 초기 1회 보장)

  if (!userProfile) return <div className="flex items-center justify-center h-full"><p className="text-zinc-400">Loading...</p></div>;

  return (
    <div className="p-6 md:p-10 space-y-10 animate-in fade-in duration-700 h-full overflow-y-auto no-scrollbar">
      <div className="max-w-7xl mx-auto space-y-10 pb-20">
        <DraftNotification draftCount={draftCount} />

        <AIBanner hotspot={hotspot} dogName={mainDog.name} />

        <DashboardHero
          userProfile={userProfile}
          mainDog={mainDog}
          grassData={grassData}
          totalWalks={totalWalks}
        />

        <RecentFriends friends={recentFriends} />

        <LocalFeedPreview threads={threads} />
      </div>
    </div>
  );
}
