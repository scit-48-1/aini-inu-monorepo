'use client';

import React from 'react';
import Link from 'next/link';
import { ArrowRight } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import type { ThreadHotspotResponse } from '@/api/threads';

interface AIBannerProps {
  hotspots: ThreadHotspotResponse[];
  dogName: string;
}

export const AIBanner: React.FC<AIBannerProps> = ({ hotspots, dogName }) => {
  const topHotspot = hotspots.length > 0
    ? hotspots.reduce((max, h) => h.count > max.count ? h : max, hotspots[0])
    : null;

  return (
    <div className="flex flex-col lg:flex-row gap-6">
      <Card variant="vibrant" className="flex-1 bg-gradient-to-r from-amber-500/10 via-amber-500/5 to-transparent p-8 flex items-center gap-8 border-amber-500/20">
        <div className="w-16 h-16 bg-white rounded-full flex items-center justify-center shadow-xl shadow-amber-500/10 shrink-0 overflow-hidden border border-amber-100">
          <img src="/AINIINU_ROGO_B.png" alt="Logo" className="w-10 h-10 object-contain" />
        </div>
        <div className="flex-1 min-w-0">
          <Typography variant="label" className="text-amber-600 mb-1 font-black tracking-widest uppercase text-[10px]">Aini AI Coaching</Typography>
          <Typography variant="h3" className="text-2xl font-black text-navy-900 leading-tight">
            {topHotspot ? (
              <>&quot;오늘은 <span className="text-amber-500">{topHotspot.region}</span>에 <span className="text-amber-500">강아지 친구들</span>이 가장 많이 모여있어요! {dogName}와 함께 산책 어때요?&quot;</>
            ) : (
              <>&quot;{dogName}와 함께 산책하기 좋은 날이에요!&quot;</>
            )}
          </Typography>
        </div>
        <Link href="/around-me" className="hidden lg:block shrink-0">
          <Button size="lg" className="flex items-center gap-3 px-10">
            지도에서 확인 <ArrowRight size={18} />
          </Button>
        </Link>
      </Card>
    </div>
  );
};
