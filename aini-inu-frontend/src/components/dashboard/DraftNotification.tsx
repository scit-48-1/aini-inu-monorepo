'use client';

import React from 'react';
import { History, ArrowRight } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import Link from 'next/link';

interface DraftNotificationProps {
  draftCount: number;
}

export const DraftNotification: React.FC<DraftNotificationProps> = ({ draftCount }) => {
  if (draftCount === 0) return null;

  return (
    <div className="animate-in slide-in-from-top-4 duration-700">
      <Card className="bg-navy-900 border-none p-6 flex flex-col md:flex-row items-center justify-between gap-6 shadow-2xl">
        <div className="flex items-center gap-5">
          <div className="w-14 h-14 rounded-2xl bg-amber-500 text-navy-900 flex items-center justify-center shrink-0 shadow-lg shadow-amber-500/20">
            <History size={28} strokeWidth={2.5} />
          </div>
          <div>
            <Typography variant="h3" className="text-white text-lg font-black leading-tight">
              오늘의 산책일기가 기다리고 있어요!
            </Typography>
            <Typography variant="body" className="text-white/50 text-xs font-bold">
              산책을 마친 지 1시간이 지났습니다. 소중한 추억을 기록해 보세요.
            </Typography>
          </div>
        </div>
        <Link href="/profile?tab=HISTORY" className="w-full md:w-auto">
          <Button variant="primary" className="w-full bg-white text-navy-900 hover:bg-amber-500 hover:text-white border-none px-8 font-black text-sm gap-2 h-12 rounded-xl shadow-xl">
            일기 쓰러 가기 <ArrowRight size={16} />
          </Button>
        </Link>
      </Card>
    </div>
  );
};
