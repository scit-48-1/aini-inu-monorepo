'use client';

import React from 'react';
import { Plus } from 'lucide-react';
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
        <button 
          onClick={onAddClick}
          className="w-10 h-10 rounded-full bg-white border border-zinc-100 flex items-center justify-center text-navy-900 shadow-sm hover:bg-amber-500 hover:text-white transition-all group"
        >
          <Plus size={24} className="group-hover:rotate-90 transition-transform duration-300" />
        </button>
      </div>
    </header>
  );
};
