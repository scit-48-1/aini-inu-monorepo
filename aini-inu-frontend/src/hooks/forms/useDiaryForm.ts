'use client';

import { useState, useCallback } from 'react';
import type {
  WalkDiaryCreateRequest,
  WalkDiaryPatchRequest,
  WalkDiaryResponse,
} from '@/api/diaries';

interface DiaryFormState {
  title: string;
  content: string;
  photoUrls: string[];
  walkDate: string;
  isPublic: boolean;
  threadId: number | undefined;
}

function getDefaultForm(): DiaryFormState {
  const today = new Date().toISOString().slice(0, 10);
  return {
    title: '',
    content: '',
    photoUrls: [],
    walkDate: today,
    isPublic: true,
    threadId: undefined,
  };
}

export function useDiaryForm() {
  const [form, setForm] = useState<DiaryFormState>(getDefaultForm);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const resetForm = useCallback(() => {
    setForm(getDefaultForm());
  }, []);

  const loadForEdit = useCallback((diary: WalkDiaryResponse) => {
    setForm({
      title: diary.title,
      content: diary.content,
      photoUrls: diary.photoUrls || [],
      walkDate: diary.walkDate,
      isPublic: diary.isPublic,
      threadId: diary.threadId || undefined,
    });
  }, []);

  const toCreateRequest = useCallback((): WalkDiaryCreateRequest => {
    const req: WalkDiaryCreateRequest = {
      title: form.title,
      content: form.content,
      walkDate: form.walkDate,
      isPublic: form.isPublic,
    };
    if (form.photoUrls.length > 0) {
      req.photoUrls = form.photoUrls;
    }
    if (form.threadId) {
      req.threadId = form.threadId;
    }
    return req;
  }, [form]);

  const toPatchRequest = useCallback((): WalkDiaryPatchRequest => {
    const req: WalkDiaryPatchRequest = {
      title: form.title,
      content: form.content,
      walkDate: form.walkDate,
      isPublic: form.isPublic,
    };
    if (form.photoUrls.length > 0) {
      req.photoUrls = form.photoUrls;
    }
    if (form.threadId) {
      req.threadId = form.threadId;
    }
    return req;
  }, [form]);

  return {
    form,
    setForm,
    resetForm,
    loadForEdit,
    toCreateRequest,
    toPatchRequest,
    isSubmitting,
    setIsSubmitting,
  };
}
