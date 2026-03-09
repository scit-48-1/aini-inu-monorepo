'use client';

import React, { useMemo } from 'react';
import { Activity } from 'lucide-react';
import { Typography } from '@/components/ui/Typography';
import { cn } from '@/lib/utils';

interface ActivityHeatmapProps {
  grassData: number[];
  totalActivities: number;
}

export const ActivityHeatmap: React.FC<ActivityHeatmapProps> = ({ grassData, totalActivities }) => {
  const streak = useMemo(() => {
    let count = 0;
    for (let i = grassData.length - 1; i >= 0; i--) {
      if (grassData[i] > 0) count++;
      else if (count > 0) break;
    }
    return count;
  }, [grassData]);

  const successRate = useMemo(() => {
    if (grassData.length === 0) return 0;
    return Math.round((totalActivities / grassData.length) * 100);
  }, [grassData, totalActivities]);

  return (
    <div className="px-6 lg:px-12 pb-4">
      <div className="bg-zinc-50/50 rounded-[32px] border border-zinc-100/50 p-6 space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Activity size={14} className="text-amber-500" />
            <Typography variant="label" className="text-amber-600 font-black uppercase tracking-widest text-[9px]">Activity</Typography>
          </div>
          <Typography variant="h3" className="text-xl font-black text-navy-900 leading-none">
            {totalActivities} <span className="text-sm text-zinc-400 font-medium">Activities</span>
          </Typography>
        </div>

        <div className="flex gap-3 items-start">
          <div className="flex flex-col justify-between py-4 text-zinc-300 font-black text-[7px] uppercase tracking-widest h-[120px] shrink-0">
            <span className="leading-none">Sun</span>
            <span className="leading-none opacity-0">Mon</span>
            <span className="leading-none">Tue</span>
            <span className="leading-none opacity-0">Wed</span>
            <span className="leading-none">Thu</span>
            <span className="leading-none opacity-0">Fri</span>
            <span className="leading-none">Sat</span>
          </div>
          <div className="flex-1 overflow-x-auto no-scrollbar">
            <div className="grid grid-flow-col grid-rows-7 gap-1 p-2 min-w-max">
              {grassData.map((val, i) => (
                <div
                  key={i}
                  className={cn(
                    "w-3 h-3 rounded-[2px] transition-all hover:scale-150 hover:z-10 cursor-help shadow-sm",
                    val === 0 ? "bg-zinc-200/50" :
                    val === 1 ? "bg-amber-200" :
                    val === 2 ? "bg-amber-400" :
                    val === 3 ? "bg-amber-500" : "bg-amber-600"
                  )}
                  title={`${val} activity(ies)`}
                />
              ))}
            </div>
          </div>
        </div>

        <div className="flex items-center justify-between px-1">
          <div className="flex gap-6">
            <div>
              <Typography variant="label" className="text-zinc-400 text-[8px] font-black tracking-widest">STREAK</Typography>
              <Typography variant="body" className="text-navy-900 font-black text-base leading-none mt-0.5">{streak} Days</Typography>
            </div>
            <div>
              <Typography variant="label" className="text-zinc-400 text-[8px] font-black tracking-widest">SUCCESS</Typography>
              <Typography variant="body" className="text-navy-900 font-black text-base leading-none mt-0.5">{successRate}%</Typography>
            </div>
          </div>
          <div className="flex items-center gap-2 text-[9px] font-black text-zinc-300 uppercase tracking-widest">
            <span>Less</span>
            <div className="flex gap-1">
              <div className="w-2.5 h-2.5 rounded-sm bg-zinc-200/50" />
              <div className="w-2.5 h-2.5 rounded-sm bg-amber-200" />
              <div className="w-2.5 h-2.5 rounded-sm bg-amber-400" />
              <div className="w-2.5 h-2.5 rounded-sm bg-amber-600" />
            </div>
            <span>More</span>
          </div>
        </div>
      </div>
    </div>
  );
};
