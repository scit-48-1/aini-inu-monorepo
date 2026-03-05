'use client';

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Mail, Lock, Loader2, ArrowRight } from 'lucide-react';
import Link from 'next/link';
import { toast } from 'sonner';
import { useUserStore } from '@/store/useUserStore';
import { authService } from '@/services/authService';

export default function LoginPage() {
  const router = useRouter();
  const { fetchProfile } = useUserStore();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !password) return;

    setIsLoading(true);
    try {
      const result = await authService.login(email, password);

      if (result) {
        toast.success(`${result.nickname}님, 환영합니다!`);
        // 스토어 프로필 갱신 — 실패해도 로그인 자체는 성공으로 처리
        await fetchProfile().catch(() => {});
        router.push('/dashboard');
      }
    } catch (e: any) {
      toast.error(e.message || '네트워크 에러가 발생했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-full bg-[#fdfbf7] flex items-center justify-center p-6">
      <Card className="w-full max-w-lg bg-white shadow-2xl rounded-[48px] p-10 md:p-16 border-none animate-in zoom-in-95 duration-700">
        <div className="text-center space-y-4 mb-12">
          <div className="w-20 h-20 bg-amber-50 rounded-[32px] flex items-center justify-center mx-auto mb-6 shadow-inner">
            <img src="/AINIINU_ROGO_B.png" className="w-12 h-12 object-contain" alt="Logo" />
          </div>
          <Typography variant="h2" className="text-4xl font-black text-navy-900 tracking-tighter italic">Welcome Back!</Typography>
          <Typography variant="body" className="text-zinc-400 font-bold">아이니 이누에 다시 오신 것을 환영해요.</Typography>
        </div>

        <form onSubmit={handleLogin} className="space-y-6">
          <div className="space-y-3">
            <Typography variant="label" className="flex items-center gap-2 ml-2"><Mail size={14} className="text-amber-500" /> 이메일</Typography>
            <input 
              type="email" 
              required
              autoComplete="username"
              className="w-full bg-zinc-50 border border-zinc-100 rounded-[24px] py-5 px-8 font-bold text-navy-900 focus:ring-8 ring-amber-500/5 outline-none transition-all" 
              placeholder="example@email.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>

          <div className="space-y-3">
            <Typography variant="label" className="flex items-center gap-2 ml-2"><Lock size={14} className="text-amber-500" /> 비밀번호</Typography>
            <input 
              type="password" 
              required
              autoComplete="current-password"
              className="w-full bg-zinc-50 border border-zinc-100 rounded-[24px] py-5 px-8 font-bold text-navy-900 focus:ring-8 ring-amber-500/5 outline-none transition-all" 
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>

          <div className="pt-4">
            <Button type="submit" disabled={isLoading} variant="primary" size="xl" fullWidth className="py-8 shadow-2xl">
              {isLoading ? <Loader2 className="animate-spin mr-3" /> : null}
              로그인하기 <ArrowRight className="ml-2" />
            </Button>
          </div>
        </form>

        <div className="mt-10 text-center">
          <Typography variant="body" className="text-zinc-400 text-sm font-bold">
            아직 계정이 없으신가요? {' '}
            <Link href="/signup" className="text-amber-600 hover:underline decoration-2 underline-offset-4">회원가입하기</Link>
          </Typography>
        </div>
      </Card>
    </div>
  );
}
