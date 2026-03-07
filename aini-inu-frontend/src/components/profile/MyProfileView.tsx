'use client';

import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { createPortal } from 'react-dom';
import { useRouter } from 'next/navigation';
import { Loader2, Activity, RefreshCw } from 'lucide-react';

import { getMe, getWalkStats, getFollowers, getFollowing } from '@/api/members';
import type { MemberResponse, WalkStatsResponse } from '@/api/members';
import { getMyPets } from '@/api/pets';
import type { PetResponse } from '@/api/pets';

import { useUserStore } from '@/store/useUserStore';
import { useWalkDiaries } from '@/hooks/useWalkDiaries';
import { useMemberReviews } from '@/hooks/useMemberReviews';

import { ProfileHeader } from '@/components/profile/ProfileHeader';
import { ProfileTabs, ProfileTab } from '@/components/profile/ProfileTabs';
import { ProfileFeed } from '@/components/profile/ProfileFeed';
import { ProfileDogs } from '@/components/profile/ProfileDogs';
import { ProfileHistory } from '@/components/profile/ProfileHistory';
import { ProfileReviews } from '@/components/profile/ProfileReviews';
import { ProfileEditModal } from '@/components/profile/ProfileEditModal';
import { DogRegisterModal } from '@/components/profile/DogRegisterModal';
import { DogDetailModal } from '@/components/profile/DogDetailModal';
import { PostDetailModal } from '@/components/profile/PostDetailModal';
import { DiaryBookModal } from '@/components/profile/DiaryBookModal';
import { DiaryCreateModal } from '@/components/profile/DiaryCreateModal';
import { NeighborsModal } from '@/components/profile/NeighborsModal';
import { ConfirmModal } from '@/components/common/ConfirmModal';

import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import { cn } from '@/lib/utils';

import type { WalkDiaryResponse, WalkDiaryCreateRequest } from '@/api/diaries';
import type { PostResponse } from '@/api/community';

// --- Walk Stats helpers ---

function transformWalkStats(stats: WalkStatsResponse): number[] {
  const { startDate, points, windowDays } = stats;
  const start = new Date(startDate);
  const data = new Array(windowDays).fill(0);
  const dateMap = new Map(points.map(p => [p.date, p.count]));
  for (let i = 0; i < windowDays; i++) {
    const d = new Date(start);
    d.setDate(d.getDate() + i);
    const key = d.toISOString().slice(0, 10);
    data[i] = dateMap.get(key) ?? 0;
  }
  return data;
}

// --- Walk Stats Heatmap component ---

interface WalkHeatmapProps {
  grassData: number[];
  totalWalks: number;
}

const WalkHeatmap: React.FC<WalkHeatmapProps> = ({ grassData, totalWalks }) => {
  const streak = useMemo(() => {
    let count = 0;
    for (let i = grassData.length - 1; i >= 0; i--) {
      if (grassData[i] > 0) count++;
      else if (count > 0) break;
    }
    return count;
  }, [grassData]);

  const successRate = useMemo(() => {
    if (grassData.length === 0) return 0;
    return Math.round((totalWalks / grassData.length) * 100);
  }, [grassData, totalWalks]);

  return (
    <div className="px-6 lg:px-12 pb-4">
      <div className="bg-zinc-50/50 rounded-[32px] border border-zinc-100/50 p-6 space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Activity size={14} className="text-amber-500" />
            <Typography variant="label" className="text-amber-600 font-black uppercase tracking-widest text-[9px]">Walk Activity</Typography>
          </div>
          <Typography variant="h3" className="text-xl font-black text-navy-900 leading-none">
            {totalWalks} <span className="text-sm text-zinc-400 font-medium">Diaries</span>
          </Typography>
        </div>

        <div className="flex gap-3 items-start">
          <div className="flex flex-col justify-between py-4 text-zinc-300 font-black text-[7px] uppercase tracking-widest h-[120px] shrink-0">
            <span className="leading-none">Sun</span>
            <span className="leading-none opacity-0">Mon</span>
            <span className="leading-none">Tue</span>
            <span className="leading-none opacity-0">Wed</span>
            <span className="leading-none">Thu</span>
            <span className="leading-none opacity-0">Fri</span>
            <span className="leading-none">Sat</span>
          </div>
          <div className="flex-1 overflow-x-auto no-scrollbar">
            <div className="grid grid-flow-col grid-rows-7 gap-1 p-2 min-w-max">
              {grassData.map((val, i) => (
                <div
                  key={i}
                  className={cn(
                    "w-3 h-3 rounded-[2px] transition-all hover:scale-150 hover:z-10 cursor-help shadow-sm",
                    val === 0 ? "bg-zinc-200/50" :
                    val === 1 ? "bg-amber-200" :
                    val === 2 ? "bg-amber-400" :
                    val === 3 ? "bg-amber-500" : "bg-amber-600"
                  )}
                  title={`${val} walk(s)`}
                />
              ))}
            </div>
          </div>
        </div>

        <div className="flex items-center justify-between px-1">
          <div className="flex gap-6">
            <div>
              <Typography variant="label" className="text-zinc-400 text-[8px] font-black tracking-widest">STREAK</Typography>
              <Typography variant="body" className="text-navy-900 font-black text-base leading-none mt-0.5">{streak} Days</Typography>
            </div>
            <div>
              <Typography variant="label" className="text-zinc-400 text-[8px] font-black tracking-widest">SUCCESS</Typography>
              <Typography variant="body" className="text-navy-900 font-black text-base leading-none mt-0.5">{successRate}%</Typography>
            </div>
          </div>
          <div className="flex items-center gap-2 text-[9px] font-black text-zinc-300 uppercase tracking-widest">
            <span>Less</span>
            <div className="flex gap-1">
              <div className="w-2.5 h-2.5 rounded-sm bg-zinc-200/50" />
              <div className="w-2.5 h-2.5 rounded-sm bg-amber-200" />
              <div className="w-2.5 h-2.5 rounded-sm bg-amber-400" />
              <div className="w-2.5 h-2.5 rounded-sm bg-amber-600" />
            </div>
            <span>More</span>
          </div>
        </div>
      </div>
    </div>
  );
};

// --- Main Component ---

export const MyProfileView: React.FC = () => {
  const router = useRouter();
  const { fetchProfile: fetchGlobalProfile } = useUserStore();

  // Data state
  const [member, setMember] = useState<MemberResponse | null>(null);
  const [pets, setPets] = useState<PetResponse[]>([]);
  const [posts, setPosts] = useState<PostResponse[]>([]);
  const [followerCount, setFollowerCount] = useState(0);
  const [followingCount, setFollowingCount] = useState(0);
  const [walkStats, setWalkStats] = useState<WalkStatsResponse | null>(null);

  // UI state
  const [isLoading, setIsLoading] = useState(true);
  const [hasError, setHasError] = useState(false);
  const [activeTab, setActiveTab] = useState<ProfileTab>('FEED');

  // Modal state
  const [isEditProfileOpen, setIsEditProfileOpen] = useState(false);
  const [isRegisterDogOpen, setIsRegisterDogOpen] = useState(false);
  const [editingPet, setEditingPet] = useState<PetResponse | null>(null);
  const [isNeighborsModalOpen, setIsNeighborsModalOpen] = useState(false);
  const [neighborsModalType, setNeighborsModalType] = useState<'FOLLOWERS' | 'FOLLOWING'>('FOLLOWERS');
  const [selectedPet, setSelectedPet] = useState<PetResponse | null>(null);
  const [selectedHistory, setSelectedHistory] = useState<WalkDiaryResponse | null>(null);
  const [selectedPost, setSelectedPost] = useState<PostResponse | null>(null);
  const [zoomedPhoto, setZoomedPhoto] = useState<string | null>(null);

  // Diary CRUD modal state
  const [isDiaryCreateOpen, setIsDiaryCreateOpen] = useState(false);
  const [editingDiary, setEditingDiary] = useState<WalkDiaryResponse | null>(null);
  const [diaryDeleteTargetId, setDiaryDeleteTargetId] = useState<number | null>(null);

  const {
    diaries,
    isLoading: diariesLoading,
    hasNext,
    fetchDiaries,
    loadMore,
    handleCreate,
    handleUpdate,
    handleDelete,
  } = useWalkDiaries();

  const {
    reviews,
    summary: reviewSummary,
    isLoading: reviewsLoading,
    hasNext: reviewsHasNext,
    fetchReviews,
    loadMore: loadMoreReviews,
  } = useMemberReviews();

  const hasRecentDiary = useMemo(() => {
    if (!diaries || diaries.length === 0) return false;
    const now = Date.now();
    const oneDayMs = 24 * 60 * 60 * 1000;
    return diaries.some((d) => {
      const ts = d.createdAt ? new Date(d.createdAt).getTime() : 0;
      return ts > 0 && now - ts <= oneDayMs;
    });
  }, [diaries]);

  const grassData = useMemo(
    () => (walkStats ? transformWalkStats(walkStats) : []),
    [walkStats],
  );

  const fetchData = useCallback(async () => {
    setIsLoading(true);
    setHasError(false);
    try {
      const [memberRes, petsRes, followersRes, followingRes] = await Promise.all([
        getMe(),
        getMyPets(),
        getFollowers({ size: 1000 }),
        getFollowing({ size: 1000 }),
      ]);
      setMember(memberRes);
      setPets(petsRes || []);
      setFollowerCount(followersRes?.content?.length ?? 0);
      setFollowingCount(followingRes?.content?.length ?? 0);
      await Promise.all([fetchDiaries(0), fetchReviews(0)]);
    } catch (e) {
      console.error('MyProfileView fetchData error:', e);
      setHasError(true);
    } finally {
      setIsLoading(false);
    }
    // Fetch walk stats separately (non-blocking)
    try {
      const stats = await getWalkStats();
      setWalkStats(stats);
    } catch {
      // Walk stats failure is non-fatal
    }
  }, [fetchDiaries, fetchReviews]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  // --- Loading state ---
  if (isLoading && !member) {
    return (
      <div className="h-full min-h-[200px] flex items-center justify-center opacity-20">
        <Loader2 className="animate-spin" size={48} />
      </div>
    );
  }

  // --- Error state ---
  if (hasError || !member) {
    return (
      <div className="p-20 text-center flex flex-col items-center gap-6">
        <Typography variant="body" className="text-zinc-400 font-bold">프로필을 불러오는데 실패했습니다.</Typography>
        <Button variant="outline" size="sm" className="flex items-center gap-2" onClick={fetchData}>
          <RefreshCw size={14} /> 다시 시도
        </Button>
      </div>
    );
  }

  // --- Walk stats heatmap slot ---
  const walkStatsSlot = grassData.length > 0 ? (
    <WalkHeatmap grassData={grassData} totalWalks={walkStats?.totalWalks || 0} />
  ) : null;

  return (
    <>
      <ProfileHeader
        member={member}
        postCount={posts.length}
        followerCount={followerCount}
        followingCount={followingCount}
        isAnyDogVerified={pets.some(p => p.isCertified)}
        isMe={true}
        hasRecentDiary={hasRecentDiary}
        onEditClick={() => setIsEditProfileOpen(true)}
        onSettingsClick={() => router.push('/settings')}
        onFollowersClick={() => { setNeighborsModalType('FOLLOWERS'); setIsNeighborsModalOpen(true); }}
        onFollowingClick={() => { setNeighborsModalType('FOLLOWING'); setIsNeighborsModalOpen(true); }}
        walkStatsSlot={walkStatsSlot}
      />

      <ProfileTabs activeTab={activeTab} onTabChange={setActiveTab} />

      <div className="pb-32 px-1 lg:px-0">
        {activeTab === 'FEED' && (
          <ProfileFeed
            posts={posts}
            onPostClick={(p) => setSelectedPost(p)}
          />
        )}
        {activeTab === 'DOGS' && (
          <ProfileDogs
            pets={pets}
            onPetClick={setSelectedPet}
            onAddClick={() => setIsRegisterDogOpen(true)}
          />
        )}
        {activeTab === 'HISTORY' && (
          <ProfileHistory
            diaries={diaries}
            isLoading={diariesLoading}
            hasNext={hasNext}
            onLoadMore={loadMore}
            onDiaryClick={(d) => setSelectedHistory(d)}
            onCreateClick={() => { setEditingDiary(null); setIsDiaryCreateOpen(true); }}
          />
        )}
        {activeTab === 'REVIEWS' && (
          <ProfileReviews
            reviews={reviews}
            summary={reviewSummary}
            isLoading={reviewsLoading}
            hasNext={reviewsHasNext}
            onLoadMore={loadMoreReviews}
          />
        )}
      </div>

      <NeighborsModal
        isOpen={isNeighborsModalOpen}
        onClose={() => { setIsNeighborsModalOpen(false); fetchData(); }}
        initialType={neighborsModalType}
      />

      <ProfileEditModal
        isOpen={isEditProfileOpen}
        onClose={() => setIsEditProfileOpen(false)}
        member={member}
        onSaved={async () => {
          await fetchData();
          fetchGlobalProfile(true);
        }}
      />

      <DogRegisterModal
        isOpen={isRegisterDogOpen}
        onClose={() => { setIsRegisterDogOpen(false); setEditingPet(null); }}
        editingPet={editingPet ?? undefined}
        onSaved={async () => { await fetchData(); }}
      />

      <DogDetailModal
        isOpen={!!selectedPet}
        onClose={() => setSelectedPet(null)}
        pet={selectedPet}
        onEdit={() => {
          setEditingPet(selectedPet);
          setIsRegisterDogOpen(true);
          setSelectedPet(null);
        }}
        onDeleted={async () => {
          setSelectedPet(null);
          await fetchData();
        }}
        onMainChanged={async () => {
          setSelectedPet(null);
          await fetchData();
        }}
        onZoom={setZoomedPhoto}
      />

      <PostDetailModal
        isOpen={!!selectedPost}
        onClose={() => setSelectedPost(null)}
        post={selectedPost}
        onUpdated={(updatedPost) => {
          setPosts(prev => prev.map(p => p.id === updatedPost.id ? updatedPost : p));
          setSelectedPost(updatedPost);
        }}
        onDeleted={(postId) => {
          setPosts(prev => prev.filter(p => p.id !== postId));
          setSelectedPost(null);
        }}
      />

      <DiaryBookModal
        isOpen={!!selectedHistory}
        onClose={() => setSelectedHistory(null)}
        mode="profile"
        selectedDiaryId={selectedHistory?.id}
        diaries={diaries}
        onSaveSuccess={fetchData}
        onDelete={(diaryId: number) => {
          setDiaryDeleteTargetId(diaryId);
        }}
      />

      <DiaryCreateModal
        isOpen={isDiaryCreateOpen}
        onClose={() => { setIsDiaryCreateOpen(false); setEditingDiary(null); }}
        editDiary={editingDiary ?? undefined}
        onSubmit={async (data) => {
          if (editingDiary) {
            await handleUpdate(editingDiary.id, data);
          } else {
            await handleCreate(data as WalkDiaryCreateRequest);
          }
          setIsDiaryCreateOpen(false);
          setEditingDiary(null);
        }}
      />

      <ConfirmModal
        isOpen={diaryDeleteTargetId !== null}
        title="일기 삭제"
        message={`산책일기를 삭제하시겠습니까?\n삭제된 일기는 복구할 수 없습니다.`}
        confirmLabel="삭제"
        cancelLabel="취소"
        variant="danger"
        onConfirm={async () => {
          if (diaryDeleteTargetId !== null) {
            await handleDelete(diaryDeleteTargetId);
            setSelectedHistory(null);
            setDiaryDeleteTargetId(null);
          }
        }}
        onCancel={() => setDiaryDeleteTargetId(null)}
      />

      {zoomedPhoto && createPortal(
        <div
          className="fixed inset-0 z-[5000] bg-black/90 backdrop-blur-xl flex items-center justify-center p-4 cursor-zoom-out"
          onClick={() => setZoomedPhoto(null)}
        >
          <img src={zoomedPhoto} className="max-w-full max-h-full object-contain shadow-2xl" alt="Enlarged" />
        </div>,
        document.body
      )}
    </>
  );
};
