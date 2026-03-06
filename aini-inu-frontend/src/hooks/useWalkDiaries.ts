'use client';

import { useState, useCallback } from 'react';
import {
  getDiaries,
  createDiary,
  updateDiary,
  deleteDiary,
} from '@/api/diaries';
import type {
  WalkDiaryCreateRequest,
  WalkDiaryPatchRequest,
  WalkDiaryResponse,
} from '@/api/diaries';
import { toast } from 'sonner';

export function useWalkDiaries() {
  const [diaries, setDiaries] = useState<WalkDiaryResponse[]>([]);
  const [hasNext, setHasNext] = useState(false);
  const [page, setPage] = useState(0);
  const [isLoading, setIsLoading] = useState(false);

  const fetchDiaries = useCallback(async (pageNum = 0) => {
    setIsLoading(true);
    try {
      const res = await getDiaries({ page: pageNum, size: 20 });
      if (pageNum === 0) {
        setDiaries(res.content);
      } else {
        setDiaries((prev) => [...prev, ...res.content]);
      }
      setHasNext(res.hasNext);
      setPage(pageNum);
    } catch (e) {
      console.error('Failed to fetch diaries:', e);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const loadMore = useCallback(() => {
    fetchDiaries(page + 1);
  }, [fetchDiaries, page]);

  const handleCreate = useCallback(
    async (data: WalkDiaryCreateRequest) => {
      await createDiary(data);
      toast.success('산책일기가 작성되었습니다!');
      await fetchDiaries(0);
    },
    [fetchDiaries],
  );

  const handleUpdate = useCallback(
    async (diaryId: number, data: WalkDiaryPatchRequest) => {
      await updateDiary(diaryId, data);
      toast.success('산책일기가 수정되었습니다!');
      await fetchDiaries(0);
    },
    [fetchDiaries],
  );

  const handleDelete = useCallback(
    async (diaryId: number) => {
      await deleteDiary(diaryId);
      toast.success('산책일기가 삭제되었습니다!');
      await fetchDiaries(0);
    },
    [fetchDiaries],
  );

  return {
    diaries,
    isLoading,
    hasNext,
    fetchDiaries,
    loadMore,
    handleCreate,
    handleUpdate,
    handleDelete,
  };
}
