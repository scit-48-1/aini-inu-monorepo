'use client';

import React from 'react';
import { Star, Loader2, MessageSquare } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import { cn } from '@/lib/utils';
import type { MemberReviewResponse } from '@/api/members';

interface ReviewSummary {
  averageScore: number;
  totalCount: number;
  scoreDistribution: Record<number, number>;
}

interface ProfileReviewsProps {
  reviews: MemberReviewResponse[];
  summary: ReviewSummary;
  isLoading: boolean;
  hasNext: boolean;
  onLoadMore: () => void;
}

const StarRating: React.FC<{ score: number; size?: number }> = ({ score, size = 14 }) => (
  <div className="flex gap-0.5">
    {[1, 2, 3, 4, 5].map((s) => (
      <Star
        key={s}
        size={size}
        fill={score >= s ? '#F59E0B' : 'transparent'}
        className={cn(
          'transition-colors',
          score >= s ? 'text-amber-500' : 'text-zinc-200'
        )}
        strokeWidth={1.5}
      />
    ))}
  </div>
);

const ScoreDistributionBar: React.FC<{ star: number; count: number; maxCount: number }> = ({ star, count, maxCount }) => {
  const percentage = maxCount > 0 ? (count / maxCount) * 100 : 0;
  return (
    <div className="flex items-center gap-2 text-xs">
      <span className="w-4 text-right font-bold text-zinc-400">{star}</span>
      <Star size={10} fill="#F59E0B" className="text-amber-500 shrink-0" strokeWidth={1.5} />
      <div className="flex-1 h-2 bg-zinc-100 rounded-full overflow-hidden">
        <div
          className="h-full bg-amber-400 rounded-full transition-all duration-500"
          style={{ width: `${percentage}%` }}
        />
      </div>
      <span className="w-6 text-right font-bold text-zinc-400">{count}</span>
    </div>
  );
};

export const ProfileReviews: React.FC<ProfileReviewsProps> = ({
  reviews,
  summary,
  isLoading,
  hasNext,
  onLoadMore,
}) => {
  // Loading state
  if (isLoading && reviews.length === 0) {
    return (
      <div className="p-8 space-y-4 animate-in slide-in-from-bottom-4 duration-500">
        {[0, 1].map((i) => (
          <Card key={i} className="p-6 rounded-[32px] border-zinc-50 shadow-lg">
            <div className="flex gap-4 items-center">
              <div className="w-12 h-12 rounded-full bg-zinc-200 animate-pulse shrink-0" />
              <div className="flex-1 space-y-3">
                <div className="h-4 bg-zinc-200 rounded-lg animate-pulse w-1/3" />
                <div className="h-3 bg-zinc-100 rounded-lg animate-pulse w-2/3" />
              </div>
            </div>
          </Card>
        ))}
      </div>
    );
  }

  // Empty state
  if (!isLoading && reviews.length === 0) {
    return (
      <div className="p-12 text-center flex flex-col items-center gap-6 animate-in fade-in duration-500">
        <div className="w-20 h-20 rounded-full bg-amber-50 flex items-center justify-center">
          <MessageSquare size={32} className="text-amber-300" />
        </div>
        <div className="space-y-2">
          <Typography variant="h3" className="text-lg font-black text-navy-900">
            아직 받은 리뷰가 없습니다.
          </Typography>
          <Typography variant="body" className="text-zinc-400 text-sm">
            산책 후 리뷰를 받으면 여기에 표시됩니다.
          </Typography>
        </div>
      </div>
    );
  }

  const maxDistCount = Math.max(...Object.values(summary.scoreDistribution), 1);

  return (
    <div className="animate-in slide-in-from-bottom-4 duration-500">
      {/* Summary section */}
      <div className="px-6 md:px-8 pt-6 pb-2">
        <Card className="p-6 rounded-[32px] border-zinc-50 shadow-lg">
          <div className="flex gap-8 items-center">
            {/* Average score */}
            <div className="text-center shrink-0">
              <Typography variant="h2" className="text-4xl font-black text-navy-900 leading-none">
                {summary.averageScore.toFixed(1)}
              </Typography>
              <StarRating score={Math.round(summary.averageScore)} size={16} />
              <Typography variant="label" className="text-zinc-400 text-[10px] font-bold mt-1">
                {summary.totalCount}개의 리뷰
              </Typography>
            </div>
            {/* Distribution */}
            <div className="flex-1 space-y-1.5">
              {[5, 4, 3, 2, 1].map((star) => (
                <ScoreDistributionBar
                  key={star}
                  star={star}
                  count={summary.scoreDistribution[star] ?? 0}
                  maxCount={maxDistCount}
                />
              ))}
            </div>
          </div>
        </Card>
      </div>

      {/* Review list */}
      <div className="space-y-4 p-6 md:p-8">
        {reviews.map((review) => {
          const formattedDate = (() => {
            try {
              return new Date(review.createdAt).toLocaleDateString('ko-KR', {
                year: 'numeric',
                month: 'long',
                day: 'numeric',
              });
            } catch {
              return review.createdAt;
            }
          })();

          return (
            <Card
              key={review.id}
              className="p-5 rounded-[28px] border-zinc-50 shadow-md bg-white"
            >
              <div className="flex gap-4">
                {/* Reviewer avatar */}
                <div className="w-11 h-11 rounded-full overflow-hidden shrink-0 bg-zinc-100">
                  {review.reviewerProfileImageUrl ? (
                    <img
                      src={review.reviewerProfileImageUrl}
                      alt={review.reviewerNickname}
                      className="w-full h-full object-cover"
                    />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center text-zinc-300 font-black text-sm">
                      {review.reviewerNickname?.charAt(0) ?? '?'}
                    </div>
                  )}
                </div>

                {/* Content */}
                <div className="flex-1 min-w-0 space-y-1.5">
                  <div className="flex items-center justify-between">
                    <Typography variant="h3" className="text-sm font-black text-navy-900">
                      {review.reviewerNickname}
                    </Typography>
                    <Typography variant="label" className="text-zinc-300 text-[10px] font-bold">
                      {formattedDate}
                    </Typography>
                  </div>
                  <StarRating score={review.score} size={12} />
                  {review.comment && (
                    <Typography variant="body" className="text-zinc-500 text-sm leading-relaxed">
                      {review.comment}
                    </Typography>
                  )}
                </div>
              </div>
            </Card>
          );
        })}
      </div>

      {/* Load more */}
      {hasNext ? (
        <div className="flex justify-center pb-6">
          <Button
            onClick={onLoadMore}
            disabled={isLoading}
            variant="outline"
            className="font-bold text-sm px-8 py-2 rounded-2xl"
          >
            {isLoading ? (
              <Loader2 size={16} className="animate-spin" />
            ) : (
              '더 보기'
            )}
          </Button>
        </div>
      ) : null}
    </div>
  );
};
