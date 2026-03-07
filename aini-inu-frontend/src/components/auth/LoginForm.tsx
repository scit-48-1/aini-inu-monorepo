'use client';

import React, { useState } from 'react';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import { Mail, Lock, Loader2, ArrowRight } from 'lucide-react';
import Link from 'next/link';
import { toast } from 'sonner';
import { useAuth } from '@/providers/AuthProvider';
import { ApiError } from '@/api/types';

export function LoginForm() {
  const auth = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [fieldError, setFieldError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !password) return;

    setFieldError(null);
    setIsLoading(true);
    try {
      await auth.login(email, password);
      // Redirect is handled by AuthProvider.login()
    } catch (error: unknown) {
      if (error instanceof ApiError) {
        // Show inline error for auth-specific codes
        setFieldError(error.message || '이메일 또는 비밀번호를 확인해 주세요.');
      } else if (error instanceof Error) {
        toast.error(error.message || '네트워크 에러가 발생했습니다.');
      } else {
        toast.error('네트워크 에러가 발생했습니다.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  const isDisabled = isLoading || !email || !password;

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div className="space-y-3">
        <Typography variant="label" className="flex items-center gap-2 ml-2">
          <Mail size={14} className="text-amber-500" /> 이메일
        </Typography>
        <input
          type="email"
          required
          autoComplete="username"
          className="w-full bg-zinc-50 border border-zinc-100 rounded-[24px] py-5 px-8 font-bold text-navy-900 text-base focus:ring-8 ring-amber-500/5 outline-none transition-all"
          placeholder="example@email.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
      </div>

      <div className="space-y-3">
        <Typography variant="label" className="flex items-center gap-2 ml-2">
          <Lock size={14} className="text-amber-500" /> 비밀번호
        </Typography>
        <input
          type="password"
          required
          autoComplete="current-password"
          className="w-full bg-zinc-50 border border-zinc-100 rounded-[24px] py-5 px-8 font-bold text-navy-900 text-base focus:ring-8 ring-amber-500/5 outline-none transition-all"
          placeholder="••••••••"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
      </div>

      {fieldError && (
        <div className="rounded-[16px] bg-red-50 border border-red-100 px-5 py-3">
          <Typography variant="body" className="text-red-600 text-sm font-bold">
            {fieldError}
          </Typography>
        </div>
      )}

      <div className="pt-4">
        <Button type="submit" disabled={isDisabled} variant="primary" size="xl" fullWidth className="py-8 shadow-2xl">
          {isLoading ? <Loader2 className="animate-spin mr-3" /> : null}
          로그인하기 <ArrowRight className="ml-2" />
        </Button>
      </div>

      <div className="mt-4 text-center">
        <Typography variant="body" className="text-zinc-400 text-sm font-bold">
          아직 계정이 없으신가요?{' '}
          <Link href="/signup" className="text-amber-600 hover:underline decoration-2 underline-offset-4">
            회원가입하기
          </Link>
        </Typography>
      </div>
    </form>
  );
}
