'use client';

import React from 'react';
import { Typography } from '@/components/ui/Typography';
import { UserAvatar } from '@/components/common/UserAvatar';

interface Story {
  id: string;
  user: {
    nickname: string;
    avatar: string;
  };
  image: string;
  type: string;
}

interface StoryAreaProps {
  stories: Story[];
  onStoryClick: (story: Story) => void;
}

export const StoryArea: React.FC<StoryAreaProps> = ({ stories, onStoryClick }) => {
  return (
    <div className="flex gap-6 overflow-x-auto no-scrollbar py-2 -mx-4 px-4 mb-8">
      {stories.length === 0 ? (
        <div className="flex items-center gap-4 opacity-30 px-2 py-4">
          <div className="w-16 h-16 rounded-full border-2 border-dashed border-zinc-300 flex items-center justify-center">
            <span className="text-[10px] font-black">Empty</span>
          </div>
          <Typography variant="label" className="text-[10px] font-black">지금은 새로운 스토리가 없습니다.</Typography>
        </div>
      ) : (
        stories.map((story) => (
          <button 
            key={story.id} 
            className="flex flex-col items-center gap-2 group shrink-0"
            onClick={() => onStoryClick(story)}
          >
            <div className="relative">
              <UserAvatar
                src={story?.user?.avatar || '/AINIINU_ROGO_B.png'}
                alt={story?.user?.nickname || '이웃'}
                hasRecentDiary={true}
                size="md"
                className="group-hover:scale-110 transition-transform duration-500"
              />
              <div className="absolute -bottom-1 -right-1 w-6 h-6 bg-navy-900 text-white rounded-full flex items-center justify-center border-2 border-white shadow-sm">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor" xmlns="http://www.w3.org/2000/svg">
                  <path d="M19 21l-7-5-7 5V5a2 2 0 012-2h10a2 2 0 012 2v16z"/>
                </svg>
              </div>
            </div>
            <Typography variant="label" className="text-[9px] font-black text-navy-900 uppercase tracking-widest group-hover:text-amber-600 transition-colors">
              {story?.user?.nickname || '이웃'}
            </Typography>
          </button>
        ))
      )}
    </div>
  );
};
