'use client';

import React from 'react';
import { PenLine } from 'lucide-react';
import { Typography } from '@/components/ui/Typography';

interface FeedHeaderProps {
  onAddClick: () => void;
}

export const FeedHeader: React.FC<FeedHeaderProps> = ({ onAddClick }) => {
  return (
    <header className="flex flex-col items-center gap-4 mb-10">
      <div className="flex items-center justify-between w-full px-4">
        <div className="w-10" />
        <Typography variant="h2" className="text-3xl text-navy-900 uppercase tracking-tighter flex items-center gap-2">
          <span className="flex items-center"><svg width="32" height="32" viewBox="0 0 24 24" fill="#EF4444" xmlns="http://www.w3.org/2000/svg"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg></span>
          Inu <span className="text-amber-500 italic">Feed</span>
        </Typography>
        <div className="flex items-center gap-2">
          <button
            onClick={onAddClick}
            className="group flex items-center gap-0 h-10 pl-3 pr-3 rounded-full bg-gradient-to-r from-amber-400 to-amber-500 text-white shadow-md shadow-amber-200/50 hover:shadow-lg hover:shadow-amber-300/50 hover:pl-4 hover:pr-5 hover:gap-2 transition-all duration-300 ease-out cursor-pointer"
          >
            <PenLine size={18} className="shrink-0 group-hover:scale-110 transition-transform duration-300" />
            <span className="max-w-0 overflow-hidden opacity-0 group-hover:max-w-[4rem] group-hover:opacity-100 transition-all duration-300 ease-out text-sm font-bold whitespace-nowrap">
              글쓰기
            </span>
          </button>
        </div>
      </div>
    </header>
  );
};
