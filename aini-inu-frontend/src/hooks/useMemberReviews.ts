'use client';

import { useState, useCallback } from 'react';
import { getMemberReviews } from '@/api/members';
import type { MemberReviewResponse, MemberReviewSummaryResponse } from '@/api/members';

interface MemberReviewSummary {
  averageScore: number;
  totalCount: number;
  scoreDistribution: Record<number, number>;
}

export function useMemberReviews(memberId?: number) {
  const [reviews, setReviews] = useState<MemberReviewResponse[]>([]);
  const [summary, setSummary] = useState<MemberReviewSummary>({
    averageScore: 0,
    totalCount: 0,
    scoreDistribution: { 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 },
  });
  const [hasNext, setHasNext] = useState(false);
  const [page, setPage] = useState(0);
  const [isLoading, setIsLoading] = useState(false);

  const fetchReviews = useCallback(async (pageNum = 0) => {
    setIsLoading(true);
    try {
      const res = await getMemberReviews({ memberId, page: pageNum, size: 20 });
      if (pageNum === 0) {
        setReviews(res.reviews.content);
      } else {
        setReviews((prev) => [...prev, ...res.reviews.content]);
      }
      setHasNext(res.reviews.hasNext);
      setPage(pageNum);
      setSummary({
        averageScore: res.averageScore,
        totalCount: res.totalCount,
        scoreDistribution: res.scoreDistribution,
      });
    } catch (e) {
      console.error('Failed to fetch member reviews:', e);
    } finally {
      setIsLoading(false);
    }
  }, [memberId]);

  const loadMore = useCallback(() => {
    fetchReviews(page + 1);
  }, [fetchReviews, page]);

  return { reviews, summary, isLoading, hasNext, fetchReviews, loadMore };
}
