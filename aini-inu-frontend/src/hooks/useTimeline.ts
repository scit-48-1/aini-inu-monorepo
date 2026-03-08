'use client';

import { useState, useCallback } from 'react';
import { getTimeline } from '@/api/timeline';
import type { TimelineEventResponse } from '@/api/timeline';
import { ApiError } from '@/api/types';

export function useTimeline(memberId: number) {
  const [events, setEvents] = useState<TimelineEventResponse[]>([]);
  const [hasNext, setHasNext] = useState(false);
  const [page, setPage] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const [isPrivate, setIsPrivate] = useState(false);

  const fetchTimeline = useCallback(async (pageNum = 0) => {
    setIsLoading(true);
    setIsPrivate(false);
    try {
      const res = await getTimeline(memberId, { page: pageNum, size: 20 });
      if (pageNum === 0) {
        setEvents(res.content);
      } else {
        setEvents((prev) => [...prev, ...res.content]);
      }
      setHasNext(res.hasNext);
      setPage(pageNum);
    } catch (e) {
      if (e instanceof ApiError && e.status === 403) {
        setIsPrivate(true);
        setEvents([]);
      } else {
        console.error('Failed to fetch timeline:', e);
      }
    } finally {
      setIsLoading(false);
    }
  }, [memberId]);

  const loadMore = useCallback(() => {
    fetchTimeline(page + 1);
  }, [fetchTimeline, page]);

  return {
    events,
    isLoading,
    hasNext,
    isPrivate,
    fetchTimeline,
    loadMore,
  };
}
