'use client';

import React, { useState, useEffect, useCallback } from 'react';
import { Loader2 } from 'lucide-react';
import { Typography } from '@/components/ui/Typography';
import { FeedPostType } from '@/types';
import { CreatePostModal } from '@/components/common/CreatePostModal';
import { postService } from '@/services/api/postService';
import { memberService } from '@/services/api/memberService';
import { threadService } from '@/services/api/threadService';
import { FeedItem } from '@/components/feed/FeedItem';
import { FeedHeader } from '@/components/feed/FeedHeader';
import { StoryArea } from '@/components/feed/StoryArea';
import { DiaryBookModal } from '@/components/profile/DiaryBookModal';
import { useConfigStore } from '@/store/useConfigStore';
import { toast } from 'sonner';

export default function FeedPage() {
  const { currentLocation } = useConfigStore();
  const [posts, setPosts] = useState<FeedPostType[]>([]);
  const [stories, setStories] = useState<any[]>([]);
  const [userProfile, setUserProfile] = useState<any>(null);
  const [isLoading, setIsLoading] = useState(true);
  
  // Modals
  const [isPostModalOpen, setIsPostModalOpen] = useState(false);
  const [isDiaryModalOpen, setIsDiaryModalOpen] = useState(false);
  const [followingDiaries, setFollowingDiaries] = useState<Record<string, any>>({});
  const [selectedDiaryId, setSelectedDiaryId] = useState<string>('');

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
    try {
      const [storiesData, diaryRes] = await Promise.all([
        postService.getStories(),
        threadService.getFollowingDiaries()
      ]);
      
      if (storiesData) setStories(storiesData);
      if (diaryRes) setFollowingDiaries(diaryRes);
    } catch (e) { console.error(e); }
  };

  const fetchUserProfile = async () => {
    try {
      const data = await memberService.getMe();
      setUserProfile(data);
    } catch (e) { console.error(e); }
  };

  const handleStoryClick = (story: any) => {
    const diaryEntries = Object.entries(followingDiaries || {});
    if (diaryEntries.length === 0) {
      toast.info('불러올 수 있는 산책 일기가 없습니다.');
      return;
    }
    // story 작성자의 다이어리가 있으면 그것부터, 없으면 첫 번째 다이어리로 시작
    const authorMatch = diaryEntries.find(([, diary]: [string, any]) => diary.authorId === story?.user?.id);
    const startId = authorMatch ? authorMatch[0] : diaryEntries[0][0];
    setSelectedDiaryId(startId);
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
            stories={stories || []} 
            onStoryClick={handleStoryClick} 
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

      {/* Walk Diaries = Flipbook UI */}
      <DiaryBookModal
        isOpen={isDiaryModalOpen}
        onClose={() => setIsDiaryModalOpen(false)}
        selectedDiaryId={selectedDiaryId}
        diaries={followingDiaries}
      />
    </div>
  );
}
