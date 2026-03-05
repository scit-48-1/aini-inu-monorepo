'use client';

import { useState, useEffect, useCallback } from 'react';
import { memberService } from '@/services/api/memberService';
import { postService } from '@/services/api/postService';
import { threadService } from '@/services/api/threadService';
import { UserType, DogType, FeedPostType, WalkDiaryType } from '@/types';

export function useMemberProfile(memberId: string) {
  const [profile, setProfile] = useState<UserType | null>(null);
  const [dogs, setDogs] = useState<DogType[]>([]);
  const [posts, setPosts] = useState<FeedPostType[]>([]);
  const [diaries, setDiaries] = useState<Record<string, WalkDiaryType>>({});
  const [isLoading, setIsLoading] = useState(true);

  const fetchMemberData = useCallback(async () => {
    if (!memberId) return;
    setIsLoading(true);
    try {
      const [profileData, dogsData, postsData, diariesData] = await Promise.all([
        memberService.getMemberProfile(memberId),
        memberService.getMemberDogs(memberId),
        postService.getPosts(memberId),
        threadService.getWalkDiaries(memberId)
      ]);

      setProfile(profileData);
      setDogs(dogsData || []);
      setPosts(postsData || []);
      setDiaries(diariesData || {});
    } catch (error) {
      console.error('Failed to fetch member profile:', error);
    } finally {
      setIsLoading(false);
    }
  }, [memberId]);

  useEffect(() => {
    fetchMemberData();
  }, [fetchMemberData]);

  return {
    profile,
    dogs,
    posts,
    diaries,
    isLoading,
    refresh: fetchMemberData
  };
}
