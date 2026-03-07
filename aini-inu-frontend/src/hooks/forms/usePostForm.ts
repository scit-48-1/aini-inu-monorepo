'use client';

import { useState, useCallback } from 'react';
import { createPost } from '@/api/community';
import { uploadImageFlow } from '@/api/upload';
import { toast } from 'sonner';

export function usePostForm(onSuccess?: () => void) {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [content, setContent] = useState('');
  const [imageFiles, setImageFiles] = useState<File[]>([]);
  const [previewUrls, setPreviewUrls] = useState<string[]>([]);

  const handleAddImage = useCallback((file: File) => {
    setImageFiles((prev) => [...prev, file]);
    const url = URL.createObjectURL(file);
    setPreviewUrls((prev) => [...prev, url]);
  }, []);

  const handleRemoveImage = useCallback((index: number) => {
    setPreviewUrls((prev) => {
      const url = prev[index];
      if (url) URL.revokeObjectURL(url);
      return prev.filter((_, i) => i !== index);
    });
    setImageFiles((prev) => prev.filter((_, i) => i !== index));
  }, []);

  const handleSubmit = useCallback(async () => {
    if (imageFiles.length === 0) {
      toast.warning('사진을 최소 1장 이상 업로드해주세요.');
      return;
    }
    if (!content.trim()) {
      toast.warning('설명을 입력해주세요.');
      return;
    }

    setIsSubmitting(true);
    try {
      const imageUrls = await Promise.all(
        imageFiles.map((f) => uploadImageFlow(f, 'COMMUNITY_POST')),
      );
      await createPost({ content, imageUrls });
      toast.success('게시글이 등록되었습니다!');
      // Reset state
      setContent('');
      setImageFiles([]);
      setPreviewUrls((prev) => {
        prev.forEach((url) => URL.revokeObjectURL(url));
        return [];
      });
      onSuccess?.();
    } catch {
      toast.error('등록 중 오류가 발생했습니다.');
    } finally {
      setIsSubmitting(false);
    }
  }, [imageFiles, content, onSuccess]);

  return {
    content,
    setContent,
    imageFiles,
    previewUrls,
    handleAddImage,
    handleRemoveImage,
    isSubmitting,
    handleSubmit,
  };
}
