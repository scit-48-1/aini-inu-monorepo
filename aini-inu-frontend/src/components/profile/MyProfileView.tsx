'use client';

import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { createPortal } from 'react-dom';
import { useRouter } from 'next/navigation';
import { Loader2, RefreshCw } from 'lucide-react';

import { getMe, getActivityStats, getFollowers, getFollowing } from '@/api/members';
import type { MemberResponse, ActivityStatsResponse } from '@/api/members';
import { getMyPets } from '@/api/pets';
import type { PetResponse } from '@/api/pets';
import { getPostsByAuthor } from '@/api/community';

import { useUserStore } from '@/store/useUserStore';
import { useWalkDiaries } from '@/hooks/useWalkDiaries';
import { useMemberReviews } from '@/hooks/useMemberReviews';

import { ActivityHeatmap } from '@/components/profile/ActivityHeatmap';
import { ProfileHeader } from '@/components/profile/ProfileHeader';
import { ProfileTabs, ProfileTab } from '@/components/profile/ProfileTabs';
import { PetHighlights } from '@/components/profile/PetHighlights';
import { ProfileFeed } from '@/components/profile/ProfileFeed';
import { ProfileHistory } from '@/components/profile/ProfileHistory';
import { ProfileReviews } from '@/components/profile/ProfileReviews';
import { ProfileTimeline } from '@/components/profile/ProfileTimeline';
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
import type { WalkDiaryResponse, WalkDiaryCreateRequest } from '@/api/diaries';
import type { PostResponse } from '@/api/community';

// --- Activity Stats helper ---

function transformActivityStats(stats: ActivityStatsResponse): number[] {
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
  const [activityStats, setActivityStats] = useState<ActivityStatsResponse | null>(null);

  // Posts pagination state
  const [postsPage, setPostsPage] = useState(0);
  const [postsHasNext, setPostsHasNext] = useState(false);
  const [postsLoading, setPostsLoading] = useState(false);

  // UI state
  const [isLoading, setIsLoading] = useState(true);
  const [hasError, setHasError] = useState(false);
  const [activeTab, setActiveTab] = useState<ProfileTab>('TIMELINE');

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
    () => (activityStats ? transformActivityStats(activityStats) : []),
    [activityStats],
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

      // Fetch my posts
      if (memberRes?.id) {
        try {
          const postsRes = await getPostsByAuthor(memberRes.id, { page: 0, size: 20 });
          setPosts(postsRes.content || []);
          setPostsHasNext(postsRes.hasNext ?? false);
          setPostsPage(0);
        } catch {
          // Posts failure is non-fatal
        }
      }
    } catch (e) {
      console.error('MyProfileView fetchData error:', e);
      setHasError(true);
    } finally {
      setIsLoading(false);
    }
    // Fetch activity stats separately (non-blocking)
    try {
      const stats = await getActivityStats();
      setActivityStats(stats);
    } catch {
      // Activity stats failure is non-fatal
    }
  }, [fetchDiaries, fetchReviews]);

  const loadMorePosts = useCallback(async () => {
    if (!member || postsLoading || !postsHasNext) return;
    setPostsLoading(true);
    try {
      const nextPage = postsPage + 1;
      const res = await getPostsByAuthor(member.id, { page: nextPage, size: 20 });
      setPosts(prev => [...prev, ...(res.content || [])]);
      setPostsHasNext(res.hasNext ?? false);
      setPostsPage(nextPage);
    } catch {
      // ignore
    } finally {
      setPostsLoading(false);
    }
  }, [member, postsLoading, postsHasNext, postsPage]);

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

  return (
    <>
      <ProfileHeader
        member={member}
        postCount={posts.length}
        followerCount={followerCount}
        followingCount={followingCount}
        isMe={true}
        hasRecentDiary={hasRecentDiary}
        onEditClick={() => setIsEditProfileOpen(true)}
        onSettingsClick={() => router.push('/settings')}
        onFollowersClick={() => { setNeighborsModalType('FOLLOWERS'); setIsNeighborsModalOpen(true); }}
        onFollowingClick={() => { setNeighborsModalType('FOLLOWING'); setIsNeighborsModalOpen(true); }}
      />

      <PetHighlights
        pets={pets}
        onPetClick={setSelectedPet}
        onAddClick={() => setIsRegisterDogOpen(true)}
      />

      {grassData.length > 0 && (
        <ActivityHeatmap grassData={grassData} totalActivities={activityStats?.totalActivities || 0} />
      )}

      <ProfileTabs activeTab={activeTab} onTabChange={setActiveTab} />

      <div className="pb-32 px-1 lg:px-0">
        {activeTab === 'FEED' && (
          <ProfileFeed
            posts={posts}
            isLoading={postsLoading}
            hasNext={postsHasNext}
            onLoadMore={loadMorePosts}
            onPostClick={(p) => setSelectedPost(p)}
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
        {activeTab === 'TIMELINE' && member && (
          <ProfileTimeline memberId={member.id} />
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
