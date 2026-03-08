'use client';

import React from 'react';
import { ClipboardList } from 'lucide-react';
import { Typography } from '@/components/ui/Typography';

interface PendingReviewCardProps {
  pendingCount: number;
  onClick: () => void;
}

export const PendingReviewCard: React.FC<PendingReviewCardProps> = ({ pendingCount, onClick }) => {
  if (pendingCount === 0) return null;

  return (
    <button
      onClick={onClick}
      className="fixed bottom-6 right-6 z-50 flex items-center gap-3 bg-white border border-amber-200 shadow-lg shadow-amber-100/50 rounded-full pl-4 pr-5 py-3 hover:shadow-xl hover:scale-105 transition-all animate-in slide-in-from-bottom-4 duration-500"
    >
      <div className="w-10 h-10 bg-amber-500 rounded-full flex items-center justify-center shrink-0">
        <ClipboardList size={18} className="text-white" />
      </div>
      <div className="text-left">
        <Typography variant="body" className="text-navy-900 font-bold text-sm leading-tight">
          리뷰 {pendingCount}건
        </Typography>
        <Typography variant="label" className="text-zinc-400 text-[10px]">
          작성 대기중
        </Typography>
      </div>
    </button>
  );
};
