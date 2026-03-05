'use client';

import React from 'react';
import { MessageSquare, Heart } from 'lucide-react';
import { Typography } from '@/components/ui/Typography';
import { useRouter } from 'next/navigation';

export default function ChatPlaceholderPage() {
  const router = useRouter();

  return (
    <div className="flex-1 flex flex-col items-center justify-center p-10 bg-[#fdfbf7] text-center space-y-6">
      <div className="relative">
        <div className="w-32 h-32 bg-white rounded-[48px] shadow-2xl flex items-center justify-center text-zinc-100 animate-in zoom-in duration-700">
          <MessageSquare size={64} strokeWidth={1} />
        </div>
        <div className="absolute -bottom-2 -right-2 w-12 h-12 bg-amber-500 rounded-full flex items-center justify-center text-white shadow-xl animate-bounce">
          <Heart size={20} fill="currentColor" />
        </div>
      </div>

      <div className="space-y-2 max-w-xs animate-in slide-in-from-bottom-4 duration-1000">
        <Typography variant="h3" className="text-2xl font-black text-navy-900">대화를 시작해보세요!</Typography>
        <Typography variant="body" className="text-zinc-400 text-sm leading-relaxed">
          왼쪽 목록에서 이웃을 선택하거나,<br />
          새로운 산책 메이트를 찾아 인사를 건네보세요.
        </Typography>
      </div>

      <button
        onClick={() => router.push('/around-me')}
        className="px-8 py-4 bg-navy-900 text-white rounded-2xl font-black text-xs uppercase tracking-widest hover:bg-amber-500 hover:text-navy-900 transition-all active:scale-95 shadow-xl"
      >
        동네 산책 탐색하기
      </button>
    </div>
  );
}
