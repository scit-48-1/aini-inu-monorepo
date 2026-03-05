'use client';

import { useState, useCallback } from 'react';
import { postService } from '@/services/api/postService';
import { toast } from 'sonner';

export function usePostForm(onSuccess?: () => void) {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [postForm, setPostForm] = useState({
    caption: '',
    location: '',
    images: [] as string[]
  });

  const handleSubmit = useCallback(async () => {
    if (postForm.images.length === 0) return toast.warning('사진을 최소 1장 이상 업로드해주세요.');
    if (!postForm.caption.trim()) return toast.warning('설명을 입력해주세요.');

    setIsSubmitting(true);
    try {
      await postService.createPost(postForm);
      toast.success('포스팅이 등록되었습니다!');
      setPostForm({ caption: '', location: '', images: [] });
      onSuccess?.();
      return true;
    } catch (e) {
      toast.error('등록 중 오류가 발생했습니다.');
      return false;
    } finally {
      setIsSubmitting(false);
    }
  }, [postForm, onSuccess]);

  return {
    postForm, setPostForm,
    isSubmitting,
    handleSubmit
  };
}
