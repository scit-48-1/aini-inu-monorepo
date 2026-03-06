'use client';

import React, { useState, useEffect, useCallback } from 'react';
import { Loader2 } from 'lucide-react';
import { Typography } from '@/components/ui/Typography';
import { FeedPostType } from '@/types';
import { CreatePostModal } from '@/components/common/CreatePostModal';
import { postService } from '@/services/api/postService';
import { memberService } from '@/services/api/memberService';
import { getStories } from '@/api/community';
import type { StoryGroupResponse } from '@/api/community';
import { FeedItem } from '@/components/feed/FeedItem';
import { FeedHeader } from '@/components/feed/FeedHeader';
import { StoryArea } from '@/components/feed/StoryArea';
import { DiaryBookModal } from '@/components/profile/DiaryBookModal';
import { useConfigStore } from '@/store/useConfigStore';
import { toast } from 'sonner';

export default function FeedPage() {
  const { currentLocation } = useConfigStore();
  const [posts, setPosts] = useState<FeedPostType[]>([]);
  const [stories, setStories] = useState<StoryGroupResponse[]>([]);
  const [userProfile, setUserProfile] = useState<any>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isStoriesLoading, setIsStoriesLoading] = useState(true);

  // Modals
  const [isPostModalOpen, setIsPostModalOpen] = useState(false);
  const [isDiaryModalOpen, setIsDiaryModalOpen] = useState(false);
  const [selectedStoryIndex, setSelectedStoryIndex] = useState(0);

  const fetchPosts = useCallback(async () => {
    setIsLoading(true);
    try {
      const data = await postService.getPosts(undefined, currentLocation || undefined);
      setPosts(Array.isArray(data) ? data : []);
    } catch (e) {
      console.error("Failed to fetch feeds", e);
      toast.error('피드를 불러오는데 실패했습니다.');
      setPosts([]);
    } finally {
      setIsLoading(false);
    }
  }, [currentLocation]);

  const fetchStories = async () => {
    setIsStoriesLoading(true);
    try {
      const res = await getStories({ page: 0, size: 20 });
      setStories(res.content);
    } catch (e) {
      console.error('Failed to fetch stories:', e);
    } finally {
      setIsStoriesLoading(false);
    }
  };

  const fetchUserProfile = async () => {
    try {
      const data = await memberService.getMe();
      setUserProfile(data);
    } catch (e) { console.error(e); }
  };

  const handleStoryClick = (group: StoryGroupResponse, index: number) => {
    if (group.diaries.length === 0) {
      toast.info('불러올 수 있는 산책 일기가 없습니다.');
      return;
    }
    setSelectedStoryIndex(index);
    setIsDiaryModalOpen(true);
  };

  useEffect(() => {
    fetchPosts();
    fetchStories();
    fetchUserProfile();
  }, [fetchPosts]);

  return (
    <div className="min-h-full bg-background animate-in fade-in duration-700 h-full overflow-y-auto no-scrollbar">
      <div className="max-w-[700px] mx-auto py-12 px-4 space-y-12">
        <div className="space-y-8">
          <FeedHeader onAddClick={() => setIsPostModalOpen(true)} />
          <StoryArea
            storyGroups={stories}
            onStoryClick={handleStoryClick}
            isLoading={isStoriesLoading}
          />
        </div>

        <main className="flex flex-col gap-10 pb-40">
          {isLoading ? (
            <div className="flex flex-col items-center py-20 opacity-20">
              <Loader2 className="animate-spin mb-4" size={40} />
              <Typography variant="label">피드를 불러오는 중...</Typography>
            </div>
          ) : Array.isArray(posts) && posts.length > 0 ? (
            posts.map((post) => (
              <FeedItem
                key={post.id}
                post={post}
                currentUserId={userProfile?.id}
                onDelete={(id) => setPosts(prev => prev.filter(p => p.id !== id))}
              />
            ))
          ) : (
            <div className="text-center py-20 text-zinc-300">표시할 피드가 없습니다.</div>
          )}
        </main>
      </div>

      <CreatePostModal
        isOpen={isPostModalOpen}
        onClose={() => setIsPostModalOpen(false)}
        onSuccess={fetchPosts}
        userProfile={userProfile}
      />

      {/* Walk Diaries = Flipbook UI (Story mode) */}
      <DiaryBookModal
        isOpen={isDiaryModalOpen}
        onClose={() => setIsDiaryModalOpen(false)}
        mode="story"
        storyGroups={stories}
        initialMemberIndex={selectedStoryIndex}
      />
    </div>
  );
}
