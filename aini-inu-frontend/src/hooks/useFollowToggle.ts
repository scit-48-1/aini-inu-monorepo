'use client';

import { useState, useEffect } from 'react';
import { memberService } from '@/services/api/memberService';
import { toast } from 'sonner';

/**
 * 팔로우/언팔로우 토글 로직을 캡슐화한 훅.
 * ProfileView, NeighborsModal 등 팔로우 기능이 필요한 모든 컴포넌트에서 재사용합니다.
 */
export function useFollowToggle(
  targetId: string,
  initialIsFollowing: boolean,
  options?: {
    onFollow?: () => void;
    onUnfollow?: () => void;
  }
) {
  const [isFollowing, setIsFollowing] = useState(initialIsFollowing);
  const [isLoading, setIsLoading] = useState(false);

  // 비동기 데이터 로드 후 외부에서 초기값이 바뀌면 동기화
  useEffect(() => {
    setIsFollowing(initialIsFollowing);
  }, [initialIsFollowing]);

  const toggle = async (targetNickname?: string) => {
    if (isLoading) return;
    setIsLoading(true);
    try {
      if (isFollowing) {
        await memberService.unfollow(targetId);
        setIsFollowing(false);
        toast.success(targetNickname ? `${targetNickname}님 팔로우를 취소했습니다.` : '팔로우를 취소했습니다.');
        options?.onUnfollow?.();
      } else {
        await memberService.follow(targetId);
        setIsFollowing(true);
        toast.success(targetNickname ? `${targetNickname}님을 팔로우했습니다.` : '팔로우했습니다.');
        options?.onFollow?.();
      }
    } catch {
      toast.error('팔로우 처리에 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  return { isFollowing, isLoading, toggle };
}
