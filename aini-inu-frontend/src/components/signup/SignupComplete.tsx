'use client';

import React, { useEffect, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { CheckCircle2, Star, ArrowRight, Sparkles } from 'lucide-react';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import confetti from 'canvas-confetti';

interface SignupCompleteProps {
  nickname: string;
  petName: string;
}

export const SignupComplete: React.FC<SignupCompleteProps> = ({ nickname, petName }) => {
  const router = useRouter();

  const fireConfetti = useCallback(() => {
    const colors = ['#FF69B4', '#00BFFF', '#32CD32', '#FFD700', '#9370DB', '#FF4500', '#FFFFFF'];
    const settings = { particleCount: 200, spread: 70, colors, startVelocity: 110, gravity: 3.5, ticks: 200 };
    confetti({ ...settings, angle: 45, origin: { x: 0, y: 1.0 } });
    confetti({ ...settings, angle: 135, origin: { x: 1, y: 1.0 } });
  }, []);

  useEffect(() => {
    const timer = setTimeout(() => {
      fireConfetti();
    }, 300);
    return () => clearTimeout(timer);
  }, [fireConfetti]);

  return (
    <div className="text-center space-y-12 animate-in zoom-in-95 duration-1000 py-10">
      <div className="relative w-48 h-48 mx-auto">
        <div className="absolute inset-0 bg-amber-500 rounded-[56px] rotate-6 opacity-10"></div>
        <div className="absolute inset-0 bg-amber-500 rounded-[56px] -rotate-3 animate-pulse"></div>
        <div className="relative z-10 w-full h-full bg-amber-500 rounded-[56px] flex items-center justify-center text-navy-900 shadow-2xl shadow-amber-500/30">
          <CheckCircle2 size={96} strokeWidth={1.5} />
        </div>
        <div className="absolute -top-4 -right-4 w-12 h-12 bg-white rounded-2xl shadow-xl flex items-center justify-center text-amber-500 animate-bounce">
          <Star size={24} fill="currentColor" />
        </div>
      </div>
      
      <div className="space-y-6">
        <h2 className="text-5xl md:text-6xl font-serif font-black text-navy-900 italic tracking-tighter">Ready to Walk!</h2>
        <Typography variant="body" className="text-2xl text-zinc-400 font-medium leading-relaxed">
          환영합니다! <br /> <span className="text-navy-900 font-bold">{nickname}</span>님과 <span className="text-amber-500 font-bold">{petName}</span>의 <br /> 즐거운 산책 라이프가 시작됩니다.
        </Typography>
      </div>

      <div className="pt-10 flex flex-col gap-4">
        <Button onClick={() => router.push('/dashboard')} variant="primary" size="xl" fullWidth className="py-8 text-2xl shadow-2xl group border-none">
          대시보드로 입장 <ArrowRight size={32} className="group-hover:translate-x-3 transition-transform ml-4" />
        </Button>
        <button onClick={fireConfetti} className="text-zinc-300 hover:text-amber-500 transition-colors flex items-center justify-center gap-2 text-xs font-black">
          <Sparkles size={14} /> 한 번 더 축하하기
        </button>
      </div>
    </div>
  );
};
