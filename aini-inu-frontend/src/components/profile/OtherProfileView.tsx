'use client';

import React, { useState, useEffect, useCallback } from 'react';
import { Loader2, AlertCircle } from 'lucide-react';
import { getMember, getMemberPets, getFollowing } from '@/api/members';
import type { MemberResponse, PetResponse } from '@/api/members';
import { ProfileHeader } from '@/components/profile/ProfileHeader';
import { ProfileTabs } from '@/components/profile/ProfileTabs';
import type { ProfileTab } from '@/components/profile/ProfileTabs';
import { NeighborsModal } from '@/components/profile/NeighborsModal';
import { useFollowToggle } from '@/hooks/useFollowToggle';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { cn } from '@/lib/utils';

interface OtherProfileViewProps {
  memberId: number;
}

export const OtherProfileView: React.FC<OtherProfileViewProps> = ({ memberId }) => {
  const [member, setMember] = useState<MemberResponse | null>(null);
  const [pets, setPets] = useState<PetResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [followerCount, setFollowerCount] = useState(0);
  const [followingCount, setFollowingCount] = useState(0);
  const [initialIsFollowing, setInitialIsFollowing] = useState(false);
  const [followStateLoaded, setFollowStateLoaded] = useState(false);

  const [activeTab, setActiveTab] = useState<ProfileTab>('DOGS');
  const [neighborsModalOpen, setNeighborsModalOpen] = useState(false);
  const [neighborsModalType, setNeighborsModalType] = useState<'FOLLOWERS' | 'FOLLOWING'>('FOLLOWERS');

  const fetchData = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [memberData, petsData] = await Promise.all([
        getMember(memberId),
        getMemberPets(memberId),
      ]);
      setMember(memberData);
      setPets(petsData);
    } catch {
      setError('프로필을 불러오는데 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  }, [memberId]);

  // Determine follow state by checking if memberId is in the current user's following list
  const fetchFollowState = useCallback(async () => {
    try {
      const res = await getFollowing({ size: 100 });
      const isFollowing = res.content.some((f) => f.id === memberId);
      setInitialIsFollowing(isFollowing);
    } catch {
      // Non-critical: default to not following
      setInitialIsFollowing(false);
    } finally {
      setFollowStateLoaded(true);
    }
  }, [memberId]);

  useEffect(() => {
    fetchData();
    fetchFollowState();
  }, [fetchData, fetchFollowState]);

  const { isFollowing, isLoading: isFollowLoading, toggle: toggleFollow } = useFollowToggle(
    memberId,
    initialIsFollowing,
    {
      onFollow: () => setFollowerCount((c) => c + 1),
      onUnfollow: () => setFollowerCount((c) => Math.max(0, c - 1)),
    }
  );

  const handleFollowersClick = () => {
    setNeighborsModalType('FOLLOWERS');
    setNeighborsModalOpen(true);
  };

  const handleFollowingClick = () => {
    setNeighborsModalType('FOLLOWING');
    setNeighborsModalOpen(true);
  };

  if (isLoading) {
    return (
      <div className="h-full min-h-[400px] flex items-center justify-center opacity-20">
        <Loader2 className="animate-spin" size={48} />
      </div>
    );
  }

  if (error || !member) {
    return (
      <div className="h-full min-h-[400px] flex flex-col items-center justify-center gap-4 opacity-60 p-8">
        <AlertCircle size={48} strokeWidth={1} className="text-red-400" />
        <Typography variant="body" className="text-zinc-500 text-center">
          {error || '프로필을 불러올 수 없습니다.'}
        </Typography>
        <Button variant="outline" size="sm" onClick={fetchData}>
          다시 시도
        </Button>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full">
      <ProfileHeader
        member={member}
        postCount={0}
        followerCount={followerCount}
        followingCount={followingCount}
        isAnyDogVerified={pets.some((p) => p.isMain)}
        isMe={false}
        isFollowing={followStateLoaded ? isFollowing : false}
        isFollowLoading={isFollowLoading || !followStateLoaded}
        onFollowToggle={toggleFollow}
        onEditClick={() => {}}
        onSettingsClick={() => {}}
        onFollowersClick={handleFollowersClick}
        onFollowingClick={handleFollowingClick}
      />

      <ProfileTabs activeTab={activeTab} onTabChange={setActiveTab} />

      <div className="flex-1 overflow-y-auto">
        {activeTab === 'DOGS' && (
          <OtherProfilePets pets={pets} />
        )}
        {activeTab === 'FEED' && (
          <EmptyTabState
            message="아직 포스팅이 없습니다."
            subMessage="이 기능은 곧 준비될 예정입니다."
          />
        )}
        {activeTab === 'HISTORY' && (
          <EmptyTabState
            message="산책 일기가 없습니다."
            subMessage="이 기능은 곧 준비될 예정입니다."
          />
        )}
      </div>

      <NeighborsModal
        isOpen={neighborsModalOpen}
        onClose={() => setNeighborsModalOpen(false)}
        initialType={neighborsModalType}
      />
    </div>
  );
};

// --- Sub-components ---

interface OtherProfilePetsProps {
  pets: PetResponse[];
}

const OtherProfilePets: React.FC<OtherProfilePetsProps> = ({ pets }) => {
  if (pets.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center p-16 gap-4 opacity-30">
        <Typography variant="body" className="font-bold text-zinc-500">
          등록된 반려동물이 없습니다.
        </Typography>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 p-6 md:p-8 animate-in slide-in-from-bottom-4 duration-500">
      {pets.map((pet) => (
        <Card key={pet.id} className={cn("group overflow-hidden rounded-[40px] border-zinc-100 shadow-xl")}>
          <div className="h-48 relative overflow-hidden">
            <img
              src={pet.photoUrl || '/default-pet.png'}
              alt={pet.name}
              className="w-full h-full object-cover transition-transform group-hover:scale-110"
            />
            <div className="absolute bottom-0 left-0 right-0 p-6 bg-gradient-to-t from-black/80 to-transparent text-white">
              <Typography variant="h3" className="text-white text-xl">{pet.name}</Typography>
              <Typography variant="label" className="text-white/80 font-bold opacity-80">
                {pet.breed?.name ?? ''}
                {pet.gender ? ` • ${pet.gender}` : ''}
              </Typography>
            </div>
          </div>
          <div className="p-6 space-y-4">
            {pet.isMain && (
              <Badge variant="default" className="bg-amber-50 border-none text-[10px] font-bold text-amber-600 px-3">
                대표
              </Badge>
            )}
          </div>
        </Card>
      ))}
    </div>
  );
};

interface EmptyTabStateProps {
  message: string;
  subMessage?: string;
}

const EmptyTabState: React.FC<EmptyTabStateProps> = ({ message, subMessage }) => (
  <div className="flex flex-col items-center justify-center p-16 gap-2 opacity-30 animate-in fade-in duration-300">
    <Typography variant="body" className="font-bold text-zinc-500">{message}</Typography>
    {subMessage && (
      <Typography variant="label" className="text-zinc-400 text-center">{subMessage}</Typography>
    )}
  </div>
);
