'use client';

import React from 'react';
import { Heart, MessageCircle } from 'lucide-react';
import { FeedPostType } from '@/types';

interface ProfileFeedProps {
  posts: FeedPostType[];
  onPostClick: (post: FeedPostType) => void;
}

export const ProfileFeed: React.FC<ProfileFeedProps> = ({ posts, onPostClick }) => {
  return (
    <div className="grid grid-cols-3 gap-1 lg:gap-8 pt-1 lg:pt-8 animate-in fade-in duration-500">
      {posts.map(post => (
        <div 
          key={post.id} 
          className="relative aspect-square group cursor-pointer overflow-hidden lg:rounded-3xl bg-zinc-100" 
          onClick={() => onPostClick(post)}
        >
          <img src={post.images[0]} className="w-full h-full object-cover transition-transform duration-700 group-hover:scale-110" alt="Post" />
          <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-6 text-white font-black">
            <div className="flex items-center gap-2"><Heart size={20} fill="currentColor"/> {post.likes}</div>
            <div className="flex items-center gap-2"><MessageCircle size={20} fill="currentColor"/> {post.comments}</div>
          </div>
        </div>
      ))}
    </div>
  );
};
