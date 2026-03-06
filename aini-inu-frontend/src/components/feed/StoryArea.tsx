'use client';

import React from 'react';
import { Typography } from '@/components/ui/Typography';
import { UserAvatar } from '@/components/common/UserAvatar';
import type { StoryGroupResponse } from '@/api/community';

interface StoryAreaProps {
  storyGroups: StoryGroupResponse[];
  onStoryClick: (group: StoryGroupResponse, index: number) => void;
  isLoading?: boolean;
}

export const StoryArea: React.FC<StoryAreaProps> = ({ storyGroups, onStoryClick, isLoading }) => {
  // Loading skeleton
  if (isLoading) {
    return (
      <div className="flex gap-6 overflow-x-auto no-scrollbar py-2 -mx-4 px-4 mb-8">
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="flex flex-col items-center gap-2 shrink-0">
            <div className="w-16 h-16 rounded-full bg-zinc-200 animate-pulse" />
            <div className="w-10 h-2 bg-zinc-200 animate-pulse rounded" />
          </div>
        ))}
      </div>
    );
  }

  return (
    <div className="flex gap-6 overflow-x-auto no-scrollbar py-2 -mx-4 px-4 mb-8">
      {storyGroups.length === 0 ? (
        <div className="flex items-center gap-4 opacity-30 px-2 py-4">
          <div className="w-16 h-16 rounded-full border-2 border-dashed border-zinc-300 flex items-center justify-center">
            <span className="text-[10px] font-black">Empty</span>
          </div>
          <Typography variant="label" className="text-[10px] font-black">지금은 새로운 스토리가 없습니다.</Typography>
        </div>
      ) : (
        storyGroups.map((group, index) => (
          <button
            key={group.memberId}
            className="flex flex-col items-center gap-2 group shrink-0"
            onClick={() => onStoryClick(group, index)}
          >
            <div className="relative">
              <UserAvatar
                src={group.profileImageUrl || '/AINIINU_ROGO_B.png'}
                alt={group.nickname}
                hasRecentDiary={true}
                size="md"
                className="group-hover:scale-110 transition-transform duration-500"
              />
              <div className="absolute -bottom-1 -right-1 w-5 h-5 bg-amber-500 text-white rounded-full flex items-center justify-center border-2 border-white shadow-sm text-[9px] font-bold">
                {group.diaries.length}
              </div>
            </div>
            <Typography variant="label" className="text-[9px] font-black text-navy-900 uppercase tracking-widest group-hover:text-amber-600 transition-colors">
              {group.nickname}
            </Typography>
          </button>
        ))
      )}
    </div>
  );
};
