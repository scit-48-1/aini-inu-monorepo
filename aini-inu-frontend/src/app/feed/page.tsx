'use client';

import React, { useState, useEffect, useCallback, useRef } from 'react';
import { Loader2, RefreshCw } from 'lucide-react';
import { Typography } from '@/components/ui/Typography';
import { getPosts, getStories } from '@/api/community';
import type { PostResponse, StoryGroupResponse } from '@/api/community';
import { CreatePostModal } from '@/components/common/CreatePostModal';
import { FeedItem } from '@/components/feed/FeedItem';
import { FeedHeader } from '@/components/feed/FeedHeader';
import { StoryArea } from '@/components/feed/StoryArea';
import { DiaryBookModal } from '@/components/profile/DiaryBookModal';
import { useUserStore } from '@/store/useUserStore';
import { toast } from 'sonner';

export default function FeedPage() {
  const profile = useUserStore(s => s.profile);
  const [posts, setPosts] = useState<PostResponse[]>([]);
  const [stories, setStories] = useState<StoryGroupResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isStoriesLoading, setIsStoriesLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [hasNext, setHasNext] = useState(false);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [hasError, setHasError] = useState(false);

  // Modals
  const [isPostModalOpen, setIsPostModalOpen] = useState(false);
  const [isDiaryModalOpen, setIsDiaryModalOpen] = useState(false);
  const [selectedStoryIndex, setSelectedStoryIndex] = useState(0);

  // Infinite scroll sentinel
  const sentinelRef = useRef<HTMLDivElement>(null);

  const fetchPosts = useCallback(async (pageNum: number) => {
    if (pageNum === 0) {
      setIsLoading(true);
      setHasError(false);
    } else {
      setIsLoadingMore(true);
    }

    try {
      const res = await getPosts({ page: pageNum, size: 10 });
      setPosts(prev => pageNum === 0 ? res.content : [...prev, ...res.content]);
      setHasNext(res.hasNext);
      setPage(pageNum);
    } catch {
      if (pageNum === 0) {
        setHasError(true);
      }
      toast.error('피드를 불러오는데 실패했습니다.');
    } finally {
      setIsLoading(false);
      setIsLoadingMore(false);
    }
  }, []);

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

  const handleStoryClick = (group: StoryGroupResponse, index: number) => {
    if (group.diaries.length === 0) {
      toast.info('불러올 수 있는 산책 일기가 없습니다.');
      return;
    }
    setSelectedStoryIndex(index);
    setIsDiaryModalOpen(true);
  };

  const handleDeletePost = useCallback((postId: number) => {
    setPosts(prev => prev.filter(p => p.id !== postId));
  }, []);

  const handleEditUpdate = useCallback((updatedPost: PostResponse) => {
    setPosts(prev => prev.map(p => p.id === updatedPost.id ? { ...p, content: updatedPost.content } : p));
  }, []);

  const handleLikeUpdate = useCallback((postId: number, liked: boolean, likeCount: number) => {
    setPosts(prev => prev.map(p => p.id === postId ? { ...p, liked, likeCount } : p));
  }, []);

  useEffect(() => {
    fetchPosts(0);
    fetchStories();
  }, [fetchPosts]);

  // Infinite scroll observer
  useEffect(() => {
    const sentinel = sentinelRef.current;
    if (!sentinel) return;

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0]?.isIntersecting && hasNext && !isLoadingMore) {
          fetchPosts(page + 1);
        }
      },
      { threshold: 0.1 },
    );

    observer.observe(sentinel);
    return () => observer.disconnect();
  }, [hasNext, isLoadingMore, page, fetchPosts]);

  const currentUserId = profile ? Number(profile.id) : undefined;

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
          {/* Loading state */}
          {isLoading ? (
            <div className="flex flex-col items-center py-20 opacity-20">
              <Loader2 className="animate-spin mb-4" size={40} />
              <Typography variant="label">피드를 불러오는 중...</Typography>
            </div>
          ) : hasError ? (
            /* Error state */
            <div className="flex flex-col items-center py-20 text-zinc-400 space-y-4">
              <Typography variant="body" className="text-zinc-500">피드를 불러오는데 실패했습니다.</Typography>
              <button
                onClick={() => fetchPosts(0)}
                className="flex items-center gap-2 px-4 py-2 rounded-full bg-amber-50 text-amber-600 text-sm font-bold hover:bg-amber-100 transition-colors"
              >
                <RefreshCw size={14} /> 다시 시도
              </button>
            </div>
          ) : posts.length > 0 ? (
            /* Success state with posts */
            <>
              {posts.map((post) => (
                <FeedItem
                  key={post.id}
                  post={post}
                  currentUserId={currentUserId}
                  onDelete={handleDeletePost}
                  onEditUpdate={handleEditUpdate}
                  onLikeUpdate={handleLikeUpdate}
                />
              ))}

              {/* Infinite scroll sentinel */}
              <div ref={sentinelRef} className="h-1" />

              {/* Loading more state */}
              {isLoadingMore && (
                <div className="flex justify-center py-4">
                  <Loader2 className="animate-spin text-zinc-300" size={24} />
                </div>
              )}
            </>
          ) : (
            /* Empty state */
            <div className="text-center py-20 text-zinc-300">표시할 피드가 없습니다.</div>
          )}
        </main>
      </div>

      <CreatePostModal
        isOpen={isPostModalOpen}
        onClose={() => setIsPostModalOpen(false)}
        onSuccess={() => fetchPosts(0)}
        userProfile={profile as any}
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
