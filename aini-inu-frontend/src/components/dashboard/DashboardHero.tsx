'use client';

import React from 'react';
import Link from 'next/link';
import { Activity, MapPin, Heart } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import { cn } from '@/lib/utils';
import type { WalkStatsResponse } from '@/api/members';
import { pointsToGridCounts } from '@/utils/walkStatsGrid';

interface DashboardHeroProps {
  userProfile: {
    nickname?: string;
    mannerScore?: number;
    location?: string;
    dogs?: { name: string; image: string }[];
  };
  mainDog: { name: string; image: string };
  walkStats: WalkStatsResponse | null;
}

export const DashboardHero: React.FC<DashboardHeroProps> = ({ userProfile, mainDog, walkStats }) => {
  const grassData = walkStats ? pointsToGridCounts(walkStats) : [];
  const totalWalks = walkStats?.totalWalks ?? 0;

  // Real Streak Calculation
  const streak = React.useMemo(() => {
    let count = 0;
    for (let i = grassData.length - 1; i >= 0; i--) {
      if (grassData[i] > 0) count++;
      else if (count > 0) break;
    }
    return count;
  }, [grassData]);

  // Real Success Rate Calculation
  const successRate = React.useMemo(() => {
    if (grassData.length === 0) return 0;
    return Math.round((totalWalks / grassData.length) * 100);
  }, [grassData, totalWalks]);

  return (
    <section className="w-full">
      <Card className="p-8 md:p-12 flex flex-col lg:flex-row gap-12 items-center">
        {/* Left Part: Profile & Greeting */}
        <div className="flex flex-col md:flex-row items-center gap-10 flex-1 w-full">
          <div className="relative shrink-0">
            <div className="absolute inset-0 bg-amber-500 rounded-[48px] blur-2xl opacity-20 animate-pulse"></div>
            <img src={mainDog.image} alt={mainDog.name} className="w-32 h-32 md:w-40 md:h-40 rounded-[48px] object-cover shadow-2xl border-4 border-white relative z-10" />
            <div className="absolute -bottom-2 -right-2 bg-navy-900 text-amber-500 px-4 py-2.5 rounded-[16px] font-black text-[10px] shadow-xl z-20 flex items-center gap-1.5">
              <Activity size={14} /> 산책 준비 완료
            </div>
          </div>
          <div className="flex-1 space-y-6 text-center md:text-left min-w-0">
            <div className="space-y-2">
              <Typography variant="h1" className="text-3xl md:text-5xl leading-tight text-navy-900 tracking-tighter">
                Hello, <span className="text-amber-500 italic">{mainDog.name}</span> <br /> 매니저님!
              </Typography>
              <div className="flex items-center justify-center md:justify-start gap-2 text-zinc-400 font-black text-sm">
                <MapPin size={16} className="text-amber-500" /> {userProfile.location || '서울시 성수동'}
              </div>
            </div>
            <div className="flex flex-wrap justify-center md:justify-start gap-4">
              <div className="px-6 py-4 bg-amber-50/50 rounded-[24px] border border-amber-100 flex items-center gap-3">
                <Heart size={20} className="text-red-500" />
                <div>
                  <Typography variant="label" className="text-zinc-400 font-black block text-[9px]">매너 점수</Typography>
                  <Typography variant="h3" className="text-navy-900 text-xl">{userProfile.mannerScore ?? 0}/10</Typography>
                </div>
              </div>
              <Link href="/around-me" className="flex-1 md:flex-none">
                <Button variant="primary" size="lg" className="h-full rounded-[24px] px-10">
                  오늘의 메이트 찾기
                </Button>
              </Link>
            </div>
          </div>
        </div>

        {/* Divider */}
        <div className="hidden lg:block w-px h-[350px] self-stretch bg-gradient-to-b from-transparent via-zinc-300 to-transparent shrink-0 mx-4" />
        <div className="lg:hidden w-full h-px bg-gradient-to-r from-transparent via-zinc-200 to-transparent my-4" />

        {/* Right Part: Walk Activity (Walk Grass) */}
        <div className="flex flex-col items-center lg:items-end gap-6 lg:pl-8 w-full lg:w-[40%] shrink-0">
          <div className="w-full flex justify-center lg:justify-end items-end mb-2">
            <div className="text-center lg:text-right space-y-0.5">
              <Typography variant="label" className="text-amber-600 font-black uppercase tracking-widest text-[9px]">Walk Activity</Typography>
              <Typography variant="h3" className="text-3xl font-black text-navy-900 leading-none">{totalWalks} <span className="text-sm text-zinc-400 font-medium">Diaries</span></Typography>
            </div>
          </div>

          {grassData.length > 0 ? (
            <>
              <div className="w-full flex justify-center gap-3">
                <div className="flex flex-col justify-between py-6 text-zinc-300 font-black text-[7px] uppercase tracking-widest h-[160px] shrink-0">
                  <span className="leading-none">Sun</span>
                  <span className="leading-none opacity-0">Mon</span>
                  <span className="leading-none">Tue</span>
                  <span className="leading-none opacity-0">Wed</span>
                  <span className="leading-none">Thu</span>
                  <span className="leading-none opacity-0">Fri</span>
                  <span className="leading-none">Sat</span>
                </div>

                <div className="overflow-x-auto no-scrollbar max-w-full">
                  <div className="grid grid-flow-col grid-rows-7 gap-1.5 p-4 bg-zinc-50/50 rounded-[32px] border border-zinc-100/50 shadow-inner min-w-max">
                    {grassData.map((val, i) => (
                      <div
                        key={i}
                        className={cn(
                          "w-3.5 h-3.5 rounded-[3px] transition-all hover:scale-150 hover:z-10 cursor-help shadow-sm",
                          val === 0 ? "bg-zinc-200/50" :
                          val === 1 ? "bg-amber-200" :
                          val === 2 ? "bg-amber-400" :
                          val === 3 ? "bg-amber-500" : "bg-amber-600"
                        )}
                        title={`${val} level walk recorded`}
                      />
                    ))}
                  </div>
                </div>
              </div>

              <div className="w-full flex flex-col sm:flex-row justify-between items-center gap-4 px-2">
                <div className="flex gap-8">
                  <div className="flex flex-col items-center sm:items-start">
                    <Typography variant="label" className="text-zinc-400 text-[8px] font-black tracking-widest">STREAK</Typography>
                    <Typography variant="body" className="text-navy-900 font-black text-lg leading-none mt-1">{streak} Days</Typography>
                  </div>
                  <div className="flex flex-col items-center sm:items-start">
                    <Typography variant="label" className="text-zinc-400 text-[8px] font-black tracking-widest">SUCCESS</Typography>
                    <Typography variant="body" className="text-navy-900 font-black text-lg leading-none mt-1">{successRate}%</Typography>
                  </div>
                </div>
                <div className="flex items-center gap-3 text-[9px] font-black text-zinc-300 uppercase tracking-widest">
                  <span>Less</span>
                  <div className="flex gap-1">
                    <div className="w-3 h-3 rounded-sm bg-zinc-200/50" />
                    <div className="w-3 h-3 rounded-sm bg-amber-200" />
                    <div className="w-3 h-3 rounded-sm bg-amber-400" />
                    <div className="w-3 h-3 rounded-sm bg-amber-600" />
                  </div>
                  <span>More</span>
                </div>
              </div>
            </>
          ) : (
            <div className="w-full flex items-center justify-center h-[200px] bg-zinc-50/50 rounded-[32px] border border-zinc-100/50">
              <Typography variant="body" className="text-zinc-300 text-sm">산책 기록을 불러오는 중...</Typography>
            </div>
          )}
        </div>
      </Card>
    </section>
  );
};
