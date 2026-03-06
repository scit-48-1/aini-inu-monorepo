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

import { ProfileHeader } from '@/components/profile/ProfileHeader';
import { ProfileTabs, ProfileTab } from '@/components/profile/ProfileTabs';
import { ProfileFeed } from '@/components/profile/ProfileFeed';
import { ProfileDogs } from '@/components/profile/ProfileDogs';
import { ProfileHistory } from '@/components/profile/ProfileHistory';
import { ProfileEditModal } from '@/components/profile/ProfileEditModal';
import { DogRegisterModal } from '@/components/profile/DogRegisterModal';
import { DogDetailModal } from '@/components/profile/DogDetailModal';
import { PostDetailModal } from '@/components/profile/PostDetailModal';
import { DiaryBookModal } from '@/components/profile/DiaryBookModal';
import { NeighborsModal } from '@/components/profile/NeighborsModal';

import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import { cn } from '@/lib/utils';

import { FeedPostType, DogType, WalkDiaryType } from '@/types';

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

function mapPetResponseToDogType(pet: PetResponse): DogType {
  return {
    id: String(pet.id),
    name: pet.name,
    breed: pet.breed?.name || '',
    age: pet.age || 0,
    birthDate: '',
    gender: (pet.gender === 'MALE' ? 'M' : 'F') as 'M' | 'F',
    image: pet.photoUrl || '/AINIINU_ROGO_B.png',
    tendencies: (pet.personalities || []).map(p => p.name) as DogType['tendencies'],
    walkStyle: (pet.walkingStyles?.[0] || '') as DogType['walkStyle'],
    mbti: pet.mbti || undefined,
    isNeutralized: pet.isNeutered,
    isMain: pet.isMain,
    isVerified: pet.isCertified,
    registrationNumber: undefined,
  };
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
  const [dogs, setDogs] = useState<DogType[]>([]);
  const [posts, setPosts] = useState<FeedPostType[]>([]);
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
  const [editingDog, setEditingDog] = useState<DogType | null>(null);
  const [isNeighborsModalOpen, setIsNeighborsModalOpen] = useState(false);
  const [neighborsModalType, setNeighborsModalType] = useState<'FOLLOWERS' | 'FOLLOWING'>('FOLLOWERS');
  const [selectedDog, setSelectedDog] = useState<DogType | null>(null);
  const [selectedHistory, setSelectedHistory] = useState<WalkDiaryType | null>(null);
  const [selectedPost, setSelectedPost] = useState<FeedPostType | null>(null);
  const [isEditingPost, setIsEditingPost] = useState(false);
  const [editCaption, setEditCaption] = useState('');
  const [zoomedPhoto, setZoomedPhoto] = useState<string | null>(null);

  const {
    diaries: allDiaries,
    processedDiaries: walkHistoryDetails,
    fetchDiaries,
  } = useWalkDiaries('me');

  const hasRecentDiary = useMemo(() => {
    if (!allDiaries || typeof allDiaries !== 'object') return false;
    const now = Date.now();
    const oneDayMs = 24 * 60 * 60 * 1000;
    return Object.values(allDiaries).some((d) => {
      const ts = d?.walkDate
        ? new Date(d.walkDate.replace(/\./g, '-')).getTime()
        : 0;
      return ts > 0 && now - ts <= oneDayMs;
    });
  }, [allDiaries]);

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
      setDogs((petsRes || []).map(mapPetResponseToDogType));
      setFollowerCount(followersRes?.content?.length ?? 0);
      setFollowingCount(followingRes?.content?.length ?? 0);
      await fetchDiaries(undefined);
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
  }, [fetchDiaries]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

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
        isAnyDogVerified={dogs.some(d => d.isVerified)}
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
            onPostClick={(p) => { setSelectedPost(p); setEditCaption(p.caption); setIsEditingPost(false); }}
          />
        )}
        {activeTab === 'DOGS' && (
          <ProfileDogs
            dogs={dogs}
            onDogClick={setSelectedDog}
            onAddClick={() => setIsRegisterDogOpen(true)}
          />
        )}
        {activeTab === 'HISTORY' && (
          <ProfileHistory
            walkHistory={walkHistoryDetails}
            allDiaries={allDiaries}
            onHistoryClick={(h) => setSelectedHistory(h)}
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
        optimizeImage={optimizeImage}
      />

      <DogRegisterModal
        isOpen={isRegisterDogOpen}
        onClose={() => { setIsRegisterDogOpen(false); setEditingDog(null); }}
        editingDog={editingDog}
        onSave={async () => { await fetchData(); return true; }}
        optimizeImage={optimizeImage}
      />

      <DogDetailModal
        isOpen={!!selectedDog}
        onClose={() => setSelectedDog(null)}
        dog={selectedDog}
        onEdit={() => { setEditingDog(selectedDog); setIsRegisterDogOpen(true); setSelectedDog(null); }}
        onZoom={setZoomedPhoto}
      />

      <PostDetailModal
        isOpen={!!selectedPost}
        onClose={() => setSelectedPost(null)}
        post={selectedPost}
        user={member ? {
          id: String(member.id),
          email: member.email,
          nickname: member.nickname,
          handle: member.linkedNickname || member.nickname,
          avatar: member.profileImageUrl || '',
          mannerScore: member.mannerTemperature || 0,
          isOwner: member.memberType === 'OWNER',
          birthDate: '',
          age: member.age || 0,
          gender: (member.gender as 'M' | 'F') || 'M',
          mbti: member.mbti,
          phone: member.phone,
          nicknameChangedAt: member.nicknameChangedAt,
          about: member.selfIntroduction || '',
          location: '',
          dogs: [],
          followerCount,
          followingCount,
          tendencies: member.personalityTypes?.map(pt => pt.name) || [],
        } : null}
        isEditing={isEditingPost}
        setIsEditing={setIsEditingPost}
        editCaption={editCaption}
        setEditCaption={setEditCaption}
        onUpdate={async () => { await fetchData(); setIsEditingPost(false); }}
        onDelete={async () => { await fetchData(); setSelectedPost(null); }}
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
