'use client';

import React from 'react';
import { ClipboardList } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';

interface PendingReviewCardProps {
  pendingCount: number;
  onClick: () => void;
}

export const PendingReviewCard: React.FC<PendingReviewCardProps> = ({ pendingCount, onClick }) => {
  if (pendingCount === 0) return null;

  return (
    <div className="animate-in slide-in-from-top-4 duration-500">
      <Card className="bg-navy-900 border-none p-6 md:p-8 flex items-center gap-6">
        <div className="w-14 h-14 bg-amber-500/20 rounded-[20px] flex items-center justify-center shrink-0">
          <ClipboardList size={28} className="text-amber-500" />
        </div>
        <div className="flex-1 min-w-0 space-y-1">
          <Typography variant="h3" className="text-white text-lg font-black">
            작성하지 않은 리뷰가 {pendingCount}건 있어요!
          </Typography>
          <Typography variant="body" className="text-zinc-400 text-xs">
            산책 후기를 남겨주시면 매너 점수에 반영됩니다.
          </Typography>
        </div>
        <Button
          variant="primary"
          size="sm"
          className="shrink-0 px-6"
          onClick={onClick}
        >
          리뷰 작성하기
        </Button>
      </Card>
    </div>
  );
};
