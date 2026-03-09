'use client';

import React, { useState, useEffect, useCallback } from 'react';
import { createPortal } from 'react-dom';
import { Loader2, AlertCircle } from 'lucide-react';
import { getMember, getMemberPets, getFollowStatus, getFollowers } from '@/api/members';
import type { MemberResponse, PetResponse } from '@/api/members';
import { ProfileHeader } from '@/components/profile/ProfileHeader';
import { ProfileTabs } from '@/components/profile/ProfileTabs';
import type { ProfileTab } from '@/components/profile/ProfileTabs';
import { PetHighlights } from '@/components/profile/PetHighlights';
import { ProfileReviews } from '@/components/profile/ProfileReviews';
import { ProfileTimeline } from '@/components/profile/ProfileTimeline';
import { NeighborsModal } from '@/components/profile/NeighborsModal';
import { DogDetailModal } from '@/components/profile/DogDetailModal';
import { useFollowToggle } from '@/hooks/useFollowToggle';
import { useMemberReviews } from '@/hooks/useMemberReviews';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';

interface OtherProfileViewProps {
  memberId: number;
}

export const OtherProfileView: React.FC<OtherProfileViewProps> = ({ memberId }) => {
  const [member, setMember] = useState<MemberResponse | null>(null);
  const [pets, setPets] = useState<PetResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [followerCount, setFollowerCount] = useState(0);
  const [followingCount] = useState(0);
  const [initialIsFollowing, setInitialIsFollowing] = useState(false);
  const [followStateLoaded, setFollowStateLoaded] = useState(false);

  const [activeTab, setActiveTab] = useState<ProfileTab>('TIMELINE');
  const [neighborsModalOpen, setNeighborsModalOpen] = useState(false);
  const [neighborsModalType, setNeighborsModalType] = useState<'FOLLOWERS' | 'FOLLOWING'>('FOLLOWERS');
  const [selectedPet, setSelectedPet] = useState<PetResponse | null>(null);
  const [zoomedPhoto, setZoomedPhoto] = useState<string | null>(null);

  const {
    reviews,
    summary: reviewSummary,
    isLoading: reviewsLoading,
    hasNext: reviewsHasNext,
    fetchReviews,
    loadMore: loadMoreReviews,
  } = useMemberReviews(memberId);

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
      // Get accurate follower count for this member
      getFollowers({ memberId, size: 1000 }).then(res => {
        setFollowerCount(res.content.length);
      }).catch(() => {});
    } catch {
      setError('프로필을 불러오는데 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  }, [memberId]);

  // Determine follow state using dedicated follow-status endpoint
  const fetchFollowState = useCallback(async () => {
    try {
      const res = await getFollowStatus(memberId);
      setInitialIsFollowing(res.isFollowing);
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
    fetchReviews(0);
  }, [fetchData, fetchFollowState, fetchReviews]);

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

      <PetHighlights
        pets={pets}
        onPetClick={(pet) => setSelectedPet(pet)}
      />

      <ProfileTabs activeTab={activeTab} onTabChange={setActiveTab} />

      <div className="flex-1 overflow-y-auto">
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
        {activeTab === 'TIMELINE' && (
          <ProfileTimeline memberId={memberId} />
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
        isOpen={neighborsModalOpen}
        onClose={() => setNeighborsModalOpen(false)}
        initialType={neighborsModalType}
        memberId={memberId}
      />

      <DogDetailModal
        isOpen={!!selectedPet}
        onClose={() => setSelectedPet(null)}
        pet={selectedPet}
        onZoom={setZoomedPhoto}
        readOnly
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
    </div>
  );
};

// --- Sub-components ---

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
