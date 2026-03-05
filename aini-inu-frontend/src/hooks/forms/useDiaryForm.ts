'use client';

import { useState, useCallback } from 'react';
import { threadService } from '@/services/api/threadService';
import { toast } from 'sonner';

export function useDiaryForm(onSuccess?: () => void) {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [diaryForm, setDiaryForm] = useState({
    title: '',
    content: '',
    photos: [] as string[],
    isPublic: false,
    tags: [] as { id: string; nickname: string; avatar: string }[]
  });

  const handleSave = useCallback(async (id: string | number) => {
    setIsSubmitting(true);
    try {
      await threadService.saveWalkDiary(id, diaryForm);
      toast.success('일기가 저장되었습니다!');
      onSuccess?.();
      return true;
    } catch (e) {
      toast.error('저장 중 오류가 발생했습니다.');
      return false;
    } finally {
      setIsSubmitting(false);
    }
  }, [diaryForm, onSuccess]);

  return {
    diaryForm, setDiaryForm,
    isSubmitting,
    handleSave
  };
}
