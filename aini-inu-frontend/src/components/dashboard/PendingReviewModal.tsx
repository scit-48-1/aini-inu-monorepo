'use client';

import React, { useState } from 'react';
import { X, ChevronRight } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import { WalkReviewModal } from '@/components/shared/modals/WalkReviewModal';

export interface PendingReview {
  chatRoomId: number;
  displayName: string;
  partnerId: number;
  partnerNickname: string;
  profileImageUrl: string | null;
}

interface PendingReviewModalProps {
  isOpen: boolean;
  onClose: () => void;
  pendingReviews: PendingReview[];
  onReviewSubmitted: () => void;
}

export const PendingReviewModal: React.FC<PendingReviewModalProps> = ({
  isOpen,
  onClose,
  pendingReviews,
  onReviewSubmitted,
}) => {
  const [selectedReview, setSelectedReview] = useState<PendingReview | null>(null);
  const [localReviews, setLocalReviews] = useState<PendingReview[]>(pendingReviews);

  // Sync when pendingReviews prop changes
  React.useEffect(() => {
    setLocalReviews(pendingReviews);
  }, [pendingReviews]);

  if (!isOpen) return null;

  const handleReviewSubmitted = () => {
    const remaining = localReviews.filter((r) => r.chatRoomId !== selectedReview?.chatRoomId);
    setLocalReviews(remaining);
    setSelectedReview(null);
    onReviewSubmitted();

    if (remaining.length === 0) {
      onClose();
    }
  };

  const handleCloseReviewForm = () => {
    setSelectedReview(null);
  };

  // When a specific review form is open, delegate to WalkReviewModal
  if (selectedReview) {
    return (
      <WalkReviewModal
        isOpen={true}
        onClose={handleCloseReviewForm}
        revieweeId={selectedReview.partnerId}
        revieweeName={selectedReview.partnerNickname}
        chatRoomId={selectedReview.chatRoomId}
        onReviewSubmitted={handleReviewSubmitted}
      />
    );
  }

  // List selection state
  return (
    <div
      className="fixed inset-0 z-[6000] bg-navy-900/40 backdrop-blur-md flex items-center justify-center p-6 animate-in fade-in duration-300"
      onClick={onClose}
    >
      <Card
        className="w-full max-w-lg bg-white shadow-2xl rounded-[48px] border-none overflow-hidden relative animate-in zoom-in-95 duration-500"
        onClick={(e: React.MouseEvent) => e.stopPropagation()}
      >
        <button
          onClick={onClose}
          className="absolute top-8 right-8 p-2 text-zinc-300 hover:text-navy-900 transition-colors z-10"
        >
          <X size={28} />
        </button>

        <div className="p-10 md:p-14 space-y-8">
          <div className="text-center space-y-2">
            <Typography variant="label" className="text-amber-500 font-black tracking-[0.3em] uppercase text-[10px]">
              Pending Reviews
            </Typography>
            <Typography variant="h2" className="text-2xl font-black text-navy-900">
              리뷰를 작성해주세요
            </Typography>
            <Typography variant="body" className="text-zinc-400 text-sm">
              {localReviews.length}건의 리뷰가 대기 중입니다
            </Typography>
          </div>

          <div className="space-y-3 max-h-[400px] overflow-y-auto no-scrollbar">
            {localReviews.map((review) => (
              <button
                key={review.chatRoomId}
                onClick={() => setSelectedReview(review)}
                className="w-full text-left p-5 rounded-[24px] bg-zinc-50 hover:bg-amber-50 border-2 border-transparent hover:border-amber-200 transition-all group flex items-center gap-4"
              >
                <div className="w-12 h-12 bg-white rounded-full flex items-center justify-center shadow-sm shrink-0 overflow-hidden">
                  <img
                    src={review.profileImageUrl || '/AINIINU_ROGO_B.png'}
                    alt={review.partnerNickname}
                    className={review.profileImageUrl ? 'w-12 h-12 object-cover' : 'w-8 h-8 object-contain'}
                  />
                </div>
                <div className="flex-1 min-w-0">
                  <Typography variant="body" className="font-bold text-navy-900 truncate">
                    {review.displayName}
                  </Typography>
                  <Typography variant="label" className="text-zinc-400 text-xs">
                    @{review.partnerNickname}
                  </Typography>
                </div>
                <ChevronRight
                  size={20}
                  className="text-zinc-300 group-hover:text-amber-500 transition-colors shrink-0"
                />
              </button>
            ))}
          </div>
        </div>
      </Card>
    </div>
  );
};
