'use client';

import { useState, useCallback, useMemo } from 'react';
import { threadService } from '@/services/api/threadService';
import { WalkDiaryType } from '@/types';
import { toast } from 'sonner';

export function useWalkDiaries(memberId?: string) {
  const [diaries, setDiaries] = useState<Record<string, WalkDiaryType>>({});
  const [isLoading, setIsLoading] = useState(false);

  const fetchDiaries = useCallback(async (targetId?: string) => {
    setIsLoading(true);
    try {
      const data = await threadService.getWalkDiaries(targetId || memberId);
      setDiaries(data || {});
    } catch (e) {
      console.error('Failed to fetch diaries:', e);
    } finally {
      setIsLoading(false);
    }
  }, [memberId]);

  const saveDiary = useCallback(async (id: string | number, data: Partial<WalkDiaryType>) => {
    try {
      await threadService.saveWalkDiary(id, data);
      toast.success('일기가 저장되었습니다!');
      await fetchDiaries(); // 최신 데이터로 갱신
      return true;
    } catch (e) {
      toast.error('저장 중 오류가 발생했습니다.');
      return false;
    }
  }, [fetchDiaries]);

  // UI에서 사용하기 좋게 가공된 데이터 리스트
  const processedDiaries = useMemo(() => {
    return Object.entries(diaries).map(([, diary]) => {
      const dateObj = diary.walkDate ? new Date(diary.walkDate) : new Date();
      return {
        ...diary,
        diaryTitle: diary.title || '산책일기',
        walkDate: isNaN(dateObj.getTime()) 
          ? (diary.walkDate || '2026.02.10') 
          : dateObj.toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' }),
        // 상세 데이터가 없는 경우를 위한 기본 파트너 정보
        partner: diary.partner || { nickname: '이웃', avatar: '/AINIINU_ROGO_B.png' }
      };
    });
  }, [diaries]);

  return {
    diaries,
    processedDiaries,
    isLoading,
    fetchDiaries,
    saveDiary
  };
}
