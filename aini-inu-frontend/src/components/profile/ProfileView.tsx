'use client';

import React, { useState, useEffect, useMemo } from 'react';
import { createPortal } from 'react-dom';
import { useRouter } from 'next/navigation';
import { useProfile } from '@/hooks/useProfile';
import { postService } from '@/services/api/postService';
import { memberService } from '@/services/api/memberService';
import { FeedPostType, DogType, UserType, WalkDiaryType } from '@/types';
import { ProfileHeader } from '@/components/profile/ProfileHeader';
import { ProfileTabs, ProfileTab } from '@/components/profile/ProfileTabs';
import { Typography } from '@/components/ui/Typography';
import { ProfileFeed } from '@/components/profile/ProfileFeed';
import { ProfileDogs } from '@/components/profile/ProfileDogs';
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
    diaries: allDiaries,
    processedDiaries: walkHistoryDetails,
    fetchDiaries,
  } = useWalkDiaries(memberId);

  const [profile, setProfile] = useState<UserType | null>(null);
  const [dogs, setDogs] = useState<DogType[]>([]);
  const [posts, setPosts] = useState<FeedPostType[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isFollowingInit, setIsFollowingInit] = useState(false);
  const [activeTab, setActiveTab] = useState<ProfileTab>('FEED');

  const [isEditProfileOpen, setIsEditProfileOpen] = useState(false);
  const [isRegisterDogOpen, setIsRegisterDogOpen] = useState(false);
  const [editingDog, setEditingDog] = useState<DogType | null>(null);
  const [isNeighborsModalOpen, setIsNeighborsModalOpen] = useState(false);
  const [neighborsModalType, setNeighborsModalType] = useState<'FOLLOWERS' | 'FOLLOWING'>('FOLLOWERS');
  const [selectedDog, setSelectedDog] = useState<DogType | null>(null);
  const [selectedHistory, setSelectedHistory] = useState<WalkDiaryType | null>(null);
  const [editMode, setEditMode] = useState<EditMode>('NONE');
  const [selectedPost, setSelectedPost] = useState<FeedPostType | null>(null);
  const [isEditingPost, setIsEditingPost] = useState(false);
  const [editCaption, setEditCaption] = useState('');
  const [zoomedPhoto, setZoomedPhoto] = useState<string | null>(null);

  const isMe = useMemo(() => {
    if (!memberId || !myProfile) return memberId === 'me';
    return memberId === 'me' || memberId === myProfile.id;
  }, [memberId, myProfile]);

  const hasRecentDiary = useMemo(() => {
    if (!allDiaries || typeof allDiaries !== 'object') return false;
    const now = Date.now();
    const oneDayMs = 24 * 60 * 60 * 1000;
    return Object.values(allDiaries).some((d: any) => {
      const ts = d?.createdAt
        ? new Date(d.createdAt).getTime()
        : d?.walkDate
          ? new Date(d.walkDate.replace(/\./g, '-')).getTime()
          : 0;
      return ts > 0 && now - ts <= oneDayMs;
    });
  }, [allDiaries]);

  const fetchData = async () => {
    if (!memberId || memberId === 'undefined' || memberId === '[memberId]') {
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    try {
      const isTargetMe = memberId === 'me';
      const targetId = isTargetMe ? undefined : memberId;

      const [userRes, dogsRes, postsRes] = await Promise.all([
        isTargetMe ? memberService.getMe() : memberService.getMemberProfile(memberId),
        isTargetMe ? memberService.getMyDogs() : memberService.getMemberDogs(memberId),
        postService.getPosts(targetId),
      ]);

      if (!userRes) throw new Error('User not found');

      setProfile(userRes);
      setDogs(dogsRes || []);
      setPosts(postsRes || []);
      await fetchDiaries(targetId);

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

  const optimizeImage = (base64: string): Promise<string> => {
    return new Promise((resolve) => {
      const img = new Image();
      img.onload = () => {
        const canvas = document.createElement('canvas');
        const MAX = 800;
        let w = img.width;
        let h = img.height;
        if (w > h) { if (w > MAX) { h *= MAX / w; w = MAX; } }
        else { if (h > MAX) { w *= MAX / h; h = MAX; } }
        canvas.width = w; canvas.height = h;
        const ctx = canvas.getContext('2d');
        ctx?.drawImage(img, 0, 0, w, h);
        resolve(canvas.toDataURL('image/jpeg', 0.5));
      };
      img.src = base64;
    });
  };

  const {
    isFollowing,
    isLoading: isFollowLoading,
    toggle: handleFollowToggle,
  } = useFollowToggle(profile?.id ?? '', isFollowingInit, {
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
        user={profile}
        postCount={posts.length}
        isAnyDogVerified={dogs.some(d => !!d.registrationNumber)}
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

      <ProfileTabs activeTab={activeTab} onTabChange={setActiveTab} />

      <div className="pb-32 px-1 lg:px-0">
        {activeTab === 'FEED' && (
          <ProfileFeed
            posts={posts}
            onPostClick={(p) => { setSelectedPost(p); setEditCaption(p.caption); setIsEditingPost(false); }}
          />
        )}
        {activeTab === 'DOGS' && (
          <ProfileDogs
            dogs={dogs}
            onDogClick={setSelectedDog}
            onAddClick={() => isMe && setIsRegisterDogOpen(true)}
          />
        )}
        {activeTab === 'HISTORY' && (
          <ProfileHistory
            walkHistory={walkHistoryDetails}
            allDiaries={allDiaries}
            onHistoryClick={(h) => { setSelectedHistory(h); setEditMode('NONE'); }}
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
            user={profile}
            onSave={async (d) => {
              try {
                await memberService.updateMe(d);
                await fetchData();
                fetchMyProfile();
                toast.success('프로필이 수정되었습니다.');
                return true;
              } catch {
                toast.error('프로필 수정에 실패했습니다.');
                return false;
              }
            }}
            optimizeImage={optimizeImage}
          />
          <DogRegisterModal
            isOpen={isRegisterDogOpen}
            onClose={() => { setIsRegisterDogOpen(false); setEditingDog(null); }}
            editingDog={editingDog}
            onSave={async () => { await fetchData(); return true; }}
            optimizeImage={optimizeImage}
          />
        </>
      )}

      <DogDetailModal
        isOpen={!!selectedDog}
        onClose={() => setSelectedDog(null)}
        dog={selectedDog}
        onEdit={() => { if (isMe) { setEditingDog(selectedDog); setIsRegisterDogOpen(true); setSelectedDog(null); } }}
        onZoom={setZoomedPhoto}
      />

      <PostDetailModal
        isOpen={!!selectedPost}
        onClose={() => setSelectedPost(null)}
        post={selectedPost}
        user={profile}
        isEditing={isMe && isEditingPost}
        setIsEditing={setIsEditingPost}
        editCaption={editCaption}
        setEditCaption={setEditCaption}
        onUpdate={async () => { await fetchData(); setIsEditingPost(false); }}
        onDelete={async () => { if (isMe) { await fetchData(); setSelectedPost(null); } }}
      />

      <DiaryBookModal
        isOpen={!!selectedHistory}
        onClose={() => setSelectedHistory(null)}
        selectedDiaryId={selectedHistory?.id ?? ''}
        diaries={allDiaries}
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
