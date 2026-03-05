'use client';

import React, { useState, useMemo } from 'react';
import { Mail, Lock, User, Check, Loader2, ArrowRight } from 'lucide-react';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { cn } from '@/lib/utils';
import { toast } from 'sonner';
import { signup } from '@/api/members';
import { ApiError } from '@/api/types';
import type { LoginResponse } from '@/api/auth';

interface SignupAccountStepProps {
  onComplete: (tokens: LoginResponse, nickname: string) => void;
}

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const NICKNAME_REGEX = /^[가-힣a-zA-Z0-9]+$/;

export const SignupAccountStep: React.FC<SignupAccountStepProps> = ({ onComplete }) => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [nickname, setNickname] = useState('');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  const emailFormatValid = EMAIL_REGEX.test(email);
  const nicknameValid = nickname.length >= 2 && nickname.length <= 10 && NICKNAME_REGEX.test(nickname);

  const criteria = useMemo(() => ({
    length: password.length >= 8,
    upper: /[A-Z]/.test(password),
    lower: /[a-z]/.test(password),
    number: /[0-9]/.test(password),
    special: /[@$!%*?&#^()_\-+=\[\]{};:'",.<>|`~\\]/.test(password),
  }), [password]);

  const isPasswordValid = Object.values(criteria).every(Boolean);
  const passwordsMatch = password === confirmPassword && confirmPassword.length > 0;

  const canGoNext =
    emailFormatValid &&
    isPasswordValid &&
    passwordsMatch &&
    nicknameValid &&
    !isSubmitting;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!canGoNext) return;

    setIsSubmitting(true);
    setFieldErrors({});

    try {
      const result = await signup(
        { email, password, nickname, memberType: 'PET_OWNER' },
        { suppressToast: true },
      );
      onComplete(result, nickname);
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.errorCode === 'M007') {
          setFieldErrors({ email: '이미 사용 중인 이메일입니다.' });
        } else if (err.errorCode === 'M003') {
          setFieldErrors({ nickname: '이미 사용 중인 닉네임입니다.' });
        } else if (err.errorCode === 'C002') {
          // validation error — try to show inline, fallback to toast
          toast.error(err.message || '입력 정보를 확인해주세요.');
        } else {
          toast.error(err.message || '회원가입에 실패했습니다.');
        }
      } else {
        toast.error('회원가입에 실패했습니다.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-10 animate-in fade-in duration-500">
      <div className="text-center space-y-4">
        <Badge variant="amber" className="px-4 py-1.5 bg-amber-50 text-amber-600 border-none">
          Step 01. Account Setting
        </Badge>
        <Typography variant="h2" className="text-4xl md:text-5xl font-serif font-black text-navy-900 tracking-tight">
          이메일로 <span className="text-amber-500 italic">가입</span>하기
        </Typography>
      </div>

      <div className="space-y-6">
        {/* Email */}
        <div className="space-y-3">
          <Typography variant="label" className="flex items-center gap-2">
            <Mail size={14} className="text-amber-500" /> 이메일 주소
          </Typography>
          <input
            name="email"
            type="email"
            autoComplete="email"
            placeholder="example@email.com"
            className={cn(
              "w-full bg-zinc-50 border border-zinc-100 rounded-[24px] py-5 px-8 font-bold text-navy-900 transition-all outline-none focus:ring-4 ring-amber-500/5",
              fieldErrors.email && "border-red-300 bg-red-50"
            )}
            value={email}
            onChange={(e) => {
              setEmail(e.target.value);
              if (fieldErrors.email) setFieldErrors((prev) => ({ ...prev, email: '' }));
            }}
          />
          {fieldErrors.email && (
            <p className="text-red-500 text-xs font-semibold px-4">{fieldErrors.email}</p>
          )}
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

        {/* Password */}
        <div className="space-y-3">
          <Typography variant="label" className="flex items-center gap-2">
            <Lock size={14} className="text-amber-500" /> 비밀번호 설정
          </Typography>
          <input
            name="password"
            type="password"
            autoComplete="new-password"
            placeholder="8자 이상, 대/소문자, 숫자, 특수문자"
            className="w-full bg-zinc-50 border border-zinc-100 rounded-[24px] py-5 px-8 font-bold text-navy-900 outline-none focus:ring-4 ring-amber-500/5 transition-all"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          {/* Password criteria badges */}
          <div className="flex flex-wrap gap-x-4 gap-y-2 px-4 pt-1">
            {(
              [
                { key: 'length', label: '8자이상' },
                { key: 'upper', label: '대문자' },
                { key: 'lower', label: '소문자' },
                { key: 'number', label: '숫자' },
                { key: 'special', label: '특수문자' },
              ] as const
            ).map(({ key, label }) => (
              <div
                key={key}
                className={cn(
                  "flex items-center gap-1.5 text-[10px] font-black",
                  criteria[key] ? "text-emerald-500" : "text-zinc-300"
                )}
              >
                {criteria[key] ? (
                  <Check size={10} />
                ) : (
                  <div className="w-2.5 h-2.5 rounded-full border border-zinc-200" />
                )}
                {label}
              </div>
            ))}
          </div>
        </div>

        {/* Confirm Password */}
        <div className="space-y-3">
          <Typography variant="label" className="flex items-center gap-2">
            <Check size={14} className="text-amber-500" /> 비밀번호 확인
          </Typography>
          <input
            name="confirmPassword"
            type="password"
            autoComplete="new-password"
            placeholder="한 번 더 입력해주세요"
            className={cn(
              "w-full bg-zinc-50 border border-zinc-100 rounded-[24px] py-5 px-8 font-bold text-navy-900 outline-none focus:ring-4 ring-amber-500/5 transition-all",
              confirmPassword.length > 0 && !passwordsMatch && "border-red-300 bg-red-50"
            )}
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
          />
          {confirmPassword.length > 0 && !passwordsMatch && (
            <p className="text-red-500 text-xs font-semibold px-4">비밀번호가 일치하지 않습니다.</p>
          )}
        </div>
      </div>

      <Button
        type="submit"
        disabled={!canGoNext}
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
    </form>
  );
};
