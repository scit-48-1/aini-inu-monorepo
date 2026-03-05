'use client';

import React, { useState, useRef } from 'react';
import { User, ImageIcon, FileText, ArrowLeft, ArrowRight, Loader2 } from 'lucide-react';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { cn } from '@/lib/utils';
import { toast } from 'sonner';
import { createProfile } from '@/api/members';
import { uploadImageFlow } from '@/api/upload';
import { ApiError } from '@/api/types';

interface SignupProfileStepProps {
  initialNickname: string;
  onComplete: () => void;
  onPrev: () => void;
}

const NICKNAME_REGEX = /^[가-힣a-zA-Z0-9]+$/;
const MAX_INTRO_LENGTH = 200;

export const SignupProfileStep: React.FC<SignupProfileStepProps> = ({
  initialNickname,
  onComplete,
  onPrev,
}) => {
  const [nickname, setNickname] = useState(initialNickname);
  const [profileImageUrl, setProfileImageUrl] = useState<string>('');
  const [imagePreview, setImagePreview] = useState<string>('');
  const [selfIntroduction, setSelfIntroduction] = useState('');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const nicknameValid =
    nickname.length >= 2 &&
    nickname.length <= 10 &&
    NICKNAME_REGEX.test(nickname);

  const canSubmit = nicknameValid && !isSubmitting && !isUploading;

  const handleFileSelect = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Show local preview immediately
    const objectUrl = URL.createObjectURL(file);
    setImagePreview(objectUrl);

    setIsUploading(true);
    try {
      const url = await uploadImageFlow(file, 'profile');
      setProfileImageUrl(url);
    } catch {
      toast.error('이미지 업로드에 실패했습니다.');
      setImagePreview('');
      setProfileImageUrl('');
    } finally {
      setIsUploading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!canSubmit) return;

    setIsSubmitting(true);
    setFieldErrors({});

    try {
      await createProfile(
        {
          nickname,
          profileImageUrl: profileImageUrl || undefined,
          selfIntroduction: selfIntroduction || undefined,
        },
        { suppressToast: true },
      );
      onComplete();
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.errorCode === 'M003') {
          setFieldErrors({ nickname: '이미 사용 중인 닉네임입니다.' });
        } else {
          toast.error(err.message || '프로필 설정에 실패했습니다.');
        }
      } else {
        toast.error('프로필 설정에 실패했습니다.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-10 animate-in fade-in duration-500">
      <div className="text-center space-y-4">
        <Badge variant="amber" className="px-4 py-1.5 bg-amber-50 text-amber-600 border-none">
          Step 02. Profile Setup
        </Badge>
        <Typography variant="h2" className="text-4xl md:text-5xl font-serif font-black text-navy-900 tracking-tight">
          프로필을 <span className="text-amber-500 italic">설정</span>하세요
        </Typography>
      </div>

      <div className="space-y-6">
        {/* Profile Image */}
        <div className="space-y-3">
          <Typography variant="label" className="flex items-center gap-2">
            <ImageIcon size={14} className="text-amber-500" /> 프로필 이미지 (선택)
          </Typography>
          <div className="flex items-center gap-6">
            <div
              className="w-24 h-24 rounded-[28px] bg-zinc-50 border-2 border-zinc-100 flex items-center justify-center overflow-hidden cursor-pointer hover:border-amber-300 transition-colors"
              onClick={() => fileInputRef.current?.click()}
            >
              {imagePreview ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={imagePreview} alt="프로필 미리보기" className="w-full h-full object-cover" />
              ) : (
                <User size={36} className="text-zinc-300" />
              )}
            </div>
            <div className="flex flex-col gap-2">
              <Button
                type="button"
                variant="outline"
                className="rounded-[20px] px-6"
                onClick={() => fileInputRef.current?.click()}
                disabled={isUploading}
              >
                {isUploading ? (
                  <><Loader2 className="animate-spin mr-2" size={16} /> 업로드 중</>
                ) : (
                  '이미지 선택'
                )}
              </Button>
              <p className="text-xs text-zinc-400">JPG, PNG, GIF (최대 10MB)</p>
            </div>
          </div>
          <input
            ref={fileInputRef}
            type="file"
            accept="image/*"
            className="hidden"
            onChange={handleFileSelect}
          />
        </div>

        {/* Nickname */}
        <div className="space-y-3">
          <Typography variant="label" className="flex items-center gap-2">
            <User size={14} className="text-amber-500" /> 닉네임
          </Typography>
          <input
            name="nickname"
            type="text"
            autoComplete="username"
            placeholder="한글/영문/숫자 2~10자"
            className={cn(
              "w-full bg-zinc-50 border border-zinc-100 rounded-[24px] py-5 px-8 font-bold text-navy-900 transition-all outline-none focus:ring-4 ring-amber-500/5",
              fieldErrors.nickname && "border-red-300 bg-red-50"
            )}
            value={nickname}
            onChange={(e) => {
              setNickname(e.target.value);
              if (fieldErrors.nickname) setFieldErrors((prev) => ({ ...prev, nickname: '' }));
            }}
          />
          {fieldErrors.nickname && (
            <p className="text-red-500 text-xs font-semibold px-4">{fieldErrors.nickname}</p>
          )}
          {!fieldErrors.nickname && nickname.length > 0 && !nicknameValid && (
            <p className="text-zinc-400 text-xs px-4">한글/영문/숫자로 2~10자 입력해주세요.</p>
          )}
        </div>

        {/* Self Introduction */}
        <div className="space-y-3">
          <Typography variant="label" className="flex items-center gap-2">
            <FileText size={14} className="text-amber-500" /> 자기소개 (선택)
          </Typography>
          <div className="relative">
            <textarea
              name="selfIntroduction"
              placeholder="자신을 간단히 소개해보세요 (최대 200자)"
              rows={4}
              maxLength={MAX_INTRO_LENGTH}
              className="w-full bg-zinc-50 border border-zinc-100 rounded-[24px] py-5 px-8 font-medium text-navy-900 outline-none focus:ring-4 ring-amber-500/5 transition-all resize-none"
              value={selfIntroduction}
              onChange={(e) => setSelfIntroduction(e.target.value)}
            />
            <span className="absolute bottom-4 right-6 text-xs text-zinc-300 font-semibold">
              {selfIntroduction.length}/{MAX_INTRO_LENGTH}
            </span>
          </div>
        </div>
      </div>

      <div className="flex gap-4">
        <Button
          type="button"
          variant="outline"
          size="xl"
          className="py-8 rounded-[24px]"
          onClick={onPrev}
          disabled={isSubmitting}
        >
          <ArrowLeft size={24} />
        </Button>
        <Button
          type="submit"
          disabled={!canSubmit}
          variant="primary"
          size="xl"
          fullWidth
          className="py-8 shadow-2xl"
        >
          {isSubmitting ? (
            <Loader2 className="animate-spin" size={24} />
          ) : (
            <>다음 단계로 <ArrowRight className="ml-2" /></>
          )}
        </Button>
      </div>
    </form>
  );
};
