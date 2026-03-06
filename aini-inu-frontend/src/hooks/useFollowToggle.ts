'use client';

import { useState, useEffect } from 'react';
import { follow, unfollow } from '@/api/members';
import { toast } from 'sonner';

/**
 * 팔로우/언팔로우 토글 로직을 캡슐화한 훅.
 * Optimistic update pattern: toggles state immediately, reverts on failure.
 */
export function useFollowToggle(
  targetId: number,
  initialIsFollowing: boolean,
  options?: {
    onFollow?: () => void;
    onUnfollow?: () => void;
    onError?: () => void;
  }
) {
  const [isFollowing, setIsFollowing] = useState(initialIsFollowing);
  const [isLoading, setIsLoading] = useState(false);

  // 비동기 데이터 로드 후 외부에서 초기값이 바뀌면 동기화
  useEffect(() => {
    setIsFollowing(initialIsFollowing);
  }, [initialIsFollowing]);

  const toggle = async () => {
    if (isLoading) return;

    // Optimistic update: toggle immediately before API call
    const wasFollowing = isFollowing;
    setIsFollowing(!wasFollowing);
    setIsLoading(true);

    if (wasFollowing) {
      options?.onUnfollow?.();
    } else {
      options?.onFollow?.();
    }

    try {
      if (wasFollowing) {
        await unfollow(targetId);
      } else {
        await follow(targetId);
      }
    } catch {
      // Rollback on failure
      setIsFollowing(wasFollowing);
      if (wasFollowing) {
        options?.onFollow?.(); // revert the unfollow count change
      } else {
        options?.onUnfollow?.(); // revert the follow count change
      }
      options?.onError?.();
      toast.error('팔로우 처리에 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  return { isFollowing, isLoading, toggle };
}
