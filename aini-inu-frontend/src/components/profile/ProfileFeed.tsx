'use client';

import React from 'react';
import { Heart, MessageCircle, Camera, Loader2 } from 'lucide-react';
import { Typography } from '@/components/ui/Typography';
import type { PostResponse } from '@/api/community';

interface ProfileFeedProps {
  posts: PostResponse[];
  isLoading?: boolean;
  hasNext?: boolean;
  onLoadMore?: () => void;
  onPostClick: (post: PostResponse) => void;
}

export const ProfileFeed: React.FC<ProfileFeedProps> = ({ posts, isLoading, hasNext, onLoadMore, onPostClick }) => {
  if (posts.length === 0 && !isLoading) {
    return (
      <div className="flex flex-col items-center justify-center py-20 gap-4">
        <div className="w-16 h-16 rounded-full bg-zinc-100 flex items-center justify-center">
          <Camera size={28} className="text-zinc-300" />
        </div>
        <Typography variant="body" className="text-zinc-400 font-bold text-sm">
          아직 게시물이 없습니다
        </Typography>
      </div>
    );
  }

  return (
    <div className="space-y-1 lg:space-y-8">
      <div className="grid grid-cols-3 gap-1 lg:gap-8 pt-1 lg:pt-8 animate-in fade-in duration-500">
        {posts.map(post => (
          <div
            key={post.id}
            className="relative aspect-square group cursor-pointer overflow-hidden lg:rounded-3xl bg-zinc-100"
            onClick={() => onPostClick(post)}
          >
            <img src={post.imageUrls[0]} className="w-full h-full object-cover transition-transform duration-700 group-hover:scale-110" alt="Post" />
            <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-6 text-white font-black">
              <div className="flex items-center gap-2"><Heart size={20} fill="currentColor"/> {post.likeCount}</div>
              <div className="flex items-center gap-2"><MessageCircle size={20} fill="currentColor"/> {post.commentCount}</div>
            </div>
          </div>
        ))}
      </div>

      {isLoading && (
        <div className="flex justify-center py-8">
          <Loader2 className="animate-spin text-zinc-300" size={24} />
        </div>
      )}

      {hasNext && !isLoading && (
        <div className="flex justify-center py-6">
          <button
            onClick={onLoadMore}
            className="px-6 py-2.5 rounded-full bg-zinc-100 text-zinc-600 text-sm font-bold hover:bg-zinc-200 transition-colors"
          >
            더 보기
          </button>
        </div>
      )}
    </div>
  );
};
