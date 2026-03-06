'use client';

import React, { useState, useRef, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { X, Plus, Loader2, Image as ImageIcon, Eye, EyeOff } from 'lucide-react';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import { cn } from '@/lib/utils';
import { useDiaryForm } from '@/hooks/forms/useDiaryForm';
import { uploadImageFlow } from '@/api/upload';
import type { WalkDiaryCreateRequest, WalkDiaryPatchRequest, WalkDiaryResponse } from '@/api/diaries';
import { toast } from 'sonner';

interface DiaryCreateModalProps {
  isOpen: boolean;
  onClose: () => void;
  editDiary?: WalkDiaryResponse;
  onSubmit: (data: WalkDiaryCreateRequest | WalkDiaryPatchRequest) => Promise<void>;
}

export const DiaryCreateModal: React.FC<DiaryCreateModalProps> = ({
  isOpen,
  onClose,
  editDiary,
  onSubmit,
}) => {
  const {
    form,
    setForm,
    resetForm,
    loadForEdit,
    toCreateRequest,
    toPatchRequest,
    isSubmitting,
    setIsSubmitting,
  } = useDiaryForm();

  const [isUploading, setIsUploading] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const fileInputRef = useRef<HTMLInputElement>(null);

  const isEditMode = !!editDiary;

  useEffect(() => {
    if (!isOpen) return;
    if (editDiary) {
      loadForEdit(editDiary);
    } else {
      resetForm();
    }
    setErrors({});
  }, [isOpen, editDiary, loadForEdit, resetForm]);

  if (!isOpen) return null;

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};
    if (!form.title.trim()) {
      newErrors.title = '제목을 입력해주세요.';
    }
    if (!form.content.trim()) {
      newErrors.content = '내용을 입력해주세요.';
    } else if (form.content.length > 300) {
      newErrors.content = '내용은 300자 이내로 입력해주세요.';
    }
    if (!form.walkDate) {
      newErrors.walkDate = '산책 날짜를 선택해주세요.';
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handlePhotoUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;

    const remainingSlots = 5 - form.photoUrls.length;
    if (remainingSlots <= 0) {
      toast.error('사진은 최대 5장까지 추가할 수 있습니다.');
      return;
    }

    const filesToUpload = Array.from(files).slice(0, remainingSlots);
    setIsUploading(true);

    try {
      const uploadedUrls = await Promise.all(
        filesToUpload.map((file) => uploadImageFlow(file, 'WALK_DIARY')),
      );
      setForm((prev) => ({
        ...prev,
        photoUrls: [...prev.photoUrls, ...uploadedUrls],
      }));
    } catch {
      toast.error('사진 업로드에 실패했습니다.');
    } finally {
      setIsUploading(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  const removePhoto = (index: number) => {
    setForm((prev) => ({
      ...prev,
      photoUrls: prev.photoUrls.filter((_, i) => i !== index),
    }));
  };

  const handleSubmit = async () => {
    if (!validate()) return;

    setIsSubmitting(true);
    try {
      const data = isEditMode ? toPatchRequest() : toCreateRequest();
      await onSubmit(data);
    } catch {
      toast.error(isEditMode ? '수정에 실패했습니다.' : '작성에 실패했습니다.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return createPortal(
    <div
      className="fixed inset-0 z-[2000] bg-navy-900/40 backdrop-blur-md flex items-center justify-center p-6 animate-in fade-in duration-300"
      onClick={(e) => e.target === e.currentTarget && onClose()}
    >
      <div className="bg-white rounded-[32px] shadow-2xl w-full max-w-lg max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between p-6 pb-4 border-b border-zinc-100">
          <Typography variant="h2" className="text-lg font-black text-navy-900">
            {isEditMode ? '산책일기 수정' : '산책일기 작성'}
          </Typography>
          <button
            onClick={onClose}
            className="p-2 rounded-full hover:bg-zinc-100 transition-colors"
          >
            <X size={20} className="text-zinc-400" />
          </button>
        </div>

        {/* Form */}
        <div className="p-6 space-y-5">
          {/* Title */}
          <div>
            <label className="block text-xs font-bold text-zinc-500 uppercase tracking-wider mb-1.5">
              제목
            </label>
            <input
              type="text"
              value={form.title}
              onChange={(e) => setForm((prev) => ({ ...prev, title: e.target.value }))}
              placeholder="산책일기 제목"
              className={cn(
                'w-full px-4 py-3 rounded-2xl border bg-zinc-50 text-sm focus:outline-none focus:ring-2 focus:ring-amber-400/50 transition-all',
                errors.title ? 'border-red-400' : 'border-zinc-200',
              )}
            />
            {errors.title ? (
              <p className="text-red-500 text-xs mt-1">{errors.title}</p>
            ) : null}
          </div>

          {/* Content */}
          <div>
            <label className="block text-xs font-bold text-zinc-500 uppercase tracking-wider mb-1.5">
              내용
            </label>
            <textarea
              value={form.content}
              onChange={(e) => setForm((prev) => ({ ...prev, content: e.target.value }))}
              maxLength={300}
              placeholder="오늘의 산책은 어땠나요?"
              rows={4}
              className={cn(
                'w-full px-4 py-3 rounded-2xl border bg-zinc-50 text-sm resize-none focus:outline-none focus:ring-2 focus:ring-amber-400/50 transition-all',
                errors.content ? 'border-red-400' : 'border-zinc-200',
              )}
            />
            <div className="flex justify-between mt-1">
              {errors.content ? (
                <p className="text-red-500 text-xs">{errors.content}</p>
              ) : (
                <span />
              )}
              <span
                className={cn(
                  'text-xs font-medium',
                  form.content.length > 300 ? 'text-red-500' : 'text-zinc-400',
                )}
              >
                {form.content.length}/300
              </span>
            </div>
          </div>

          {/* Photos */}
          <div>
            <label className="block text-xs font-bold text-zinc-500 uppercase tracking-wider mb-1.5">
              사진 ({form.photoUrls.length}/5)
            </label>
            <div className="flex flex-wrap gap-3">
              {form.photoUrls.map((url, idx) => (
                <div
                  key={idx}
                  className="relative w-20 h-20 rounded-xl overflow-hidden shadow-sm group"
                >
                  <img
                    src={url}
                    alt={`Photo ${idx + 1}`}
                    className="w-full h-full object-cover"
                  />
                  <button
                    type="button"
                    onClick={() => removePhoto(idx)}
                    className="absolute top-1 right-1 w-5 h-5 bg-black/50 rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity"
                  >
                    <X size={12} className="text-white" />
                  </button>
                </div>
              ))}
              {form.photoUrls.length < 5 ? (
                <button
                  type="button"
                  onClick={() => fileInputRef.current?.click()}
                  disabled={isUploading}
                  className="w-20 h-20 rounded-xl border-2 border-dashed border-zinc-300 flex items-center justify-center hover:border-amber-400 hover:bg-amber-50/50 transition-all disabled:opacity-50"
                >
                  {isUploading ? (
                    <Loader2 size={20} className="text-zinc-400 animate-spin" />
                  ) : (
                    <Plus size={20} className="text-zinc-400" />
                  )}
                </button>
              ) : null}
            </div>
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              multiple
              onChange={handlePhotoUpload}
              className="hidden"
            />
          </div>

          {/* Walk Date */}
          <div>
            <label className="block text-xs font-bold text-zinc-500 uppercase tracking-wider mb-1.5">
              산책 날짜
            </label>
            <input
              type="date"
              value={form.walkDate}
              onChange={(e) => setForm((prev) => ({ ...prev, walkDate: e.target.value }))}
              className={cn(
                'w-full px-4 py-3 rounded-2xl border bg-zinc-50 text-sm focus:outline-none focus:ring-2 focus:ring-amber-400/50 transition-all',
                errors.walkDate ? 'border-red-400' : 'border-zinc-200',
              )}
            />
            {errors.walkDate ? (
              <p className="text-red-500 text-xs mt-1">{errors.walkDate}</p>
            ) : null}
          </div>

          {/* Thread ID (optional) */}
          <div>
            <label className="block text-xs font-bold text-zinc-500 uppercase tracking-wider mb-1.5">
              연결된 산책 스레드 (선택사항)
            </label>
            <input
              type="number"
              value={form.threadId ?? ''}
              onChange={(e) =>
                setForm((prev) => ({
                  ...prev,
                  threadId: e.target.value ? Number(e.target.value) : undefined,
                }))
              }
              placeholder="스레드 번호"
              className="w-full px-4 py-3 rounded-2xl border border-zinc-200 bg-zinc-50 text-sm focus:outline-none focus:ring-2 focus:ring-amber-400/50 transition-all"
            />
          </div>

          {/* isPublic toggle */}
          <div className="flex items-center justify-between py-2">
            <div className="flex items-center gap-2">
              {form.isPublic ? (
                <Eye size={16} className="text-amber-500" />
              ) : (
                <EyeOff size={16} className="text-zinc-400" />
              )}
              <span className="text-sm font-bold text-navy-900">
                {form.isPublic ? '공개' : '나만 보기'}
              </span>
            </div>
            <button
              type="button"
              onClick={() => setForm((prev) => ({ ...prev, isPublic: !prev.isPublic }))}
              className={cn(
                'relative w-12 h-6 rounded-full transition-colors',
                form.isPublic ? 'bg-amber-500' : 'bg-zinc-300',
              )}
            >
              <div
                className={cn(
                  'absolute top-0.5 w-5 h-5 bg-white rounded-full shadow-sm transition-transform',
                  form.isPublic ? 'translate-x-6' : 'translate-x-0.5',
                )}
              />
            </button>
          </div>
        </div>

        {/* Footer */}
        <div className="p-6 pt-0">
          <Button
            onClick={handleSubmit}
            disabled={isSubmitting || isUploading}
            className="w-full bg-amber-500 hover:bg-amber-600 text-white font-bold py-3 rounded-2xl transition-colors disabled:opacity-50"
          >
            {isSubmitting ? (
              <Loader2 size={16} className="animate-spin mx-auto" />
            ) : isEditMode ? (
              '수정하기'
            ) : (
              '작성하기'
            )}
          </Button>
        </div>
      </div>
    </div>,
    document.body,
  );
};
