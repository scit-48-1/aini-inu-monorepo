'use client';

import React, { useState, useEffect, useMemo } from 'react';
import { createPortal } from 'react-dom';
import { useRouter } from 'next/navigation';
import { useProfile } from '@/hooks/useProfile';
import { memberService } from '@/services/api/memberService';
import { DogType, UserType } from '@/types';
import type { PostResponse } from '@/api/community';
import type { WalkDiaryResponse } from '@/api/diaries';
import type { MemberResponse } from '@/api/members';
import { ProfileHeader } from '@/components/profile/ProfileHeader';
import { ProfileTabs, ProfileTab } from '@/components/profile/ProfileTabs';
import { Typography } from '@/components/ui/Typography';
import { ProfileFeed } from '@/components/profile/ProfileFeed';
import { PetHighlights } from '@/components/profile/PetHighlights';
import { ProfileHistory } from '@/components/profile/ProfileHistory';
import { ProfileEditModal } from '@/components/profile/ProfileEditModal';
import { DogRegisterModal } from '@/components/profile/DogRegisterModal';
import { DogDetailModal } from '@/components/profile/DogDetailModal';
import { PostDetailModal } from '@/components/profile/PostDetailModal';
import { DiaryBookModal } from '@/components/profile/DiaryBookModal';
import { NeighborsModal } from '@/components/profile/NeighborsModal';
import { useWalkDiaries } from '@/hooks/useWalkDiaries';
import { useFollowToggle } from '@/hooks/useFollowToggle';
import { Loader2 } from 'lucide-react';
import { toast } from 'sonner';

type EditMode = 'NONE' | 'CONTENT' | 'PHOTOS';

interface ProfileViewProps {
  memberId: string;
  /** true: 좁은 컨테이너(채팅 패널 등)에 최적화된 레이아웃 */
  compact?: boolean;
}

export const ProfileView: React.FC<ProfileViewProps> = ({ memberId, compact = false }) => {
  const router = useRouter();
  const { profile: myProfile, fetchProfile: fetchMyProfile } = useProfile();

  const {
    diaries,
    isLoading: diariesLoading,
    hasNext,
    fetchDiaries,
    loadMore,
  } = useWalkDiaries();

  const [profile, setProfile] = useState<UserType | null>(null);
  const [dogs, setDogs] = useState<DogType[]>([]);
  const [posts, setPosts] = useState<PostResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isFollowingInit, setIsFollowingInit] = useState(false);
  const [activeTab, setActiveTab] = useState<ProfileTab>('TIMELINE');

  const [isEditProfileOpen, setIsEditProfileOpen] = useState(false);
  const [isRegisterDogOpen, setIsRegisterDogOpen] = useState(false);
  const [editingDog, setEditingDog] = useState<DogType | null>(null);
  const [isNeighborsModalOpen, setIsNeighborsModalOpen] = useState(false);
  const [neighborsModalType, setNeighborsModalType] = useState<'FOLLOWERS' | 'FOLLOWING'>('FOLLOWERS');
  const [selectedDog, setSelectedDog] = useState<DogType | null>(null);
  const [selectedHistory, setSelectedHistory] = useState<WalkDiaryResponse | null>(null);
  const [editMode, setEditMode] = useState<EditMode>('NONE');
  const [selectedPost, setSelectedPost] = useState<PostResponse | null>(null);
  const [zoomedPhoto, setZoomedPhoto] = useState<string | null>(null);

  const isMe = useMemo(() => {
    if (!memberId || !myProfile) return memberId === 'me';
    return memberId === 'me' || memberId === myProfile.id;
  }, [memberId, myProfile]);

  const hasRecentDiary = useMemo(() => {
    if (!diaries || diaries.length === 0) return false;
    const now = Date.now();
    const oneDayMs = 24 * 60 * 60 * 1000;
    return diaries.some((d) => {
      const ts = d.createdAt ? new Date(d.createdAt).getTime() : 0;
      return ts > 0 && now - ts <= oneDayMs;
    });
  }, [diaries]);

  const fetchData = async () => {
    if (!memberId || memberId === 'undefined' || memberId === '[memberId]') {
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    try {
      const isTargetMe = memberId === 'me';

      const [userRes, dogsRes] = await Promise.all([
        isTargetMe ? memberService.getMe() : memberService.getMemberProfile(memberId),
        isTargetMe ? memberService.getMyDogs() : memberService.getMemberDogs(memberId),
      ]);

      if (!userRes) throw new Error('User not found');

      setProfile(userRes);
      setDogs(dogsRes || []);
      await fetchDiaries(0);

      if (!isTargetMe) {
        try {
          const following = await memberService.getFollowing();
          setIsFollowingInit((following || []).some((u: UserType) => u.id === memberId));
        } catch { /* 무시 */ }
      }
    } catch (e: any) {
      console.error('Profile Fetch Error:', e);
      setProfile(null);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (memberId) fetchData();
  }, [memberId]);

  const {
    isFollowing,
    isLoading: isFollowLoading,
    toggle: handleFollowToggle,
  } = useFollowToggle(profile?.id ? Number(profile.id) : 0, isFollowingInit, {
    onFollow: () => setProfile(prev => prev ? { ...prev, followerCount: (prev.followerCount ?? 0) + 1 } : prev),
    onUnfollow: () => setProfile(prev => prev ? { ...prev, followerCount: Math.max(0, (prev.followerCount ?? 0) - 1) } : prev),
  });

  if (isLoading && !profile) {
    return (
      <div className="h-full min-h-[200px] flex items-center justify-center opacity-20">
        <Loader2 className="animate-spin" size={48} />
      </div>
    );
  }

  if (!profile) {
    return (
      <div className="p-20 text-center flex flex-col items-center gap-4">
        <Typography variant="body" className="text-zinc-400 font-bold">유저 정보를 찾을 수 없습니다.</Typography>
        <button onClick={() => router.push('/dashboard')} className="text-amber-600 font-black text-xs hover:underline">대시보드로 돌아가기</button>
      </div>
    );
  }

  return (
    <>
      <ProfileHeader
        member={profile ? {
          id: Number(profile.id) || 0,
          email: profile.email || '',
          nickname: profile.nickname || '',
          memberType: profile.isOwner ? 'OWNER' : 'NON_OWNER',
          profileImageUrl: profile.avatar || '',
          linkedNickname: profile.handle || '',
          phone: profile.phone || '',
          age: profile.age || 0,
          gender: profile.gender || '',
          mbti: profile.mbti || '',
          personality: '',
          selfIntroduction: profile.about || '',
          personalityTypes: (profile.tendencies || []).map((name, i) => ({ id: i, name, code: name })),
          mannerTemperature: profile.mannerScore || 0,
          status: 'ACTIVE',
          createdAt: '',
          nicknameChangedAt: profile.nicknameChangedAt || '',
          verified: false,
          isVerified: false,
        } as MemberResponse : null}
        postCount={posts.length}
        followerCount={profile?.followerCount || 0}
        followingCount={profile?.followingCount || 0}
        isMe={isMe}
        isFollowing={isFollowing}
        hasRecentDiary={hasRecentDiary}
        compact={compact}
        onEditClick={() => setIsEditProfileOpen(true)}
        onSettingsClick={() => router.push('/settings')}
        onFollowersClick={() => { setNeighborsModalType('FOLLOWERS'); setIsNeighborsModalOpen(true); }}
        onFollowingClick={() => { setNeighborsModalType('FOLLOWING'); setIsNeighborsModalOpen(true); }}
        onFollowToggle={handleFollowToggle}
        isFollowLoading={isFollowLoading}
      />

      <PetHighlights
        pets={dogs as any}
        onPetClick={setSelectedDog as any}
        onAddClick={isMe ? () => setIsRegisterDogOpen(true) : undefined}
      />

      <ProfileTabs activeTab={activeTab} onTabChange={setActiveTab} />

      <div className="pb-32 px-1 lg:px-0">
        {activeTab === 'FEED' && (
          <ProfileFeed
            posts={posts}
            onPostClick={(p) => setSelectedPost(p)}
          />
        )}
        {activeTab === 'HISTORY' && (
          <ProfileHistory
            diaries={diaries}
            isLoading={diariesLoading}
            hasNext={hasNext}
            onLoadMore={loadMore}
            onDiaryClick={(d) => { setSelectedHistory(d); setEditMode('NONE'); }}
            onCreateClick={() => {/* Create handled by MyProfileView only */}}
          />
        )}
      </div>

      <NeighborsModal
        isOpen={isNeighborsModalOpen}
        onClose={() => { setIsNeighborsModalOpen(false); if (isMe) fetchData(); }}
        initialType={neighborsModalType}
      />

      {isMe && (
        <>
          <ProfileEditModal
            isOpen={isEditProfileOpen}
            onClose={() => setIsEditProfileOpen(false)}
            member={profile ? {
              id: Number(profile.id) || 0,
              email: profile.email || '',
              nickname: profile.nickname || '',
              memberType: profile.isOwner ? 'OWNER' : 'NON_OWNER',
              profileImageUrl: profile.avatar || '',
              linkedNickname: profile.handle || '',
              phone: profile.phone || '',
              age: profile.age || 0,
              gender: profile.gender || '',
              mbti: profile.mbti || '',
              personality: '',
              selfIntroduction: profile.about || '',
              personalityTypes: (profile.tendencies || []).map((name, i) => ({ id: i, name, code: name })),
              mannerTemperature: profile.mannerScore || 0,
              status: 'ACTIVE',
              createdAt: '',
              nicknameChangedAt: profile.nicknameChangedAt || '',
              verified: false,
            } as MemberResponse : ({} as MemberResponse)}
            onSaved={async () => {
              await fetchData();
              fetchMyProfile();
            }}
          />
          {/* TODO: ProfileView uses legacy DogType — rewire in future phase */}
          <DogRegisterModal
            isOpen={isRegisterDogOpen}
            onClose={() => { setIsRegisterDogOpen(false); setEditingDog(null); }}
            editingPet={editingDog as any}
            onSaved={async () => { await fetchData(); }}
          />
        </>
      )}

      {/* TODO: ProfileView uses legacy DogType — rewire in future phase */}
      <DogDetailModal
        isOpen={!!selectedDog}
        onClose={() => setSelectedDog(null)}
        pet={selectedDog as any}
        onEdit={() => { if (isMe) { setEditingDog(selectedDog); setIsRegisterDogOpen(true); setSelectedDog(null); } }}
        onDeleted={() => { setSelectedDog(null); fetchData(); }}
        onMainChanged={() => { setSelectedDog(null); fetchData(); }}
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
