import React from 'react';
import { cn } from '@/lib/utils';

interface MannerScoreGaugeProps {
  score: number;
}

export const MannerScoreGauge: React.FC<MannerScoreGaugeProps> = ({ score }) => {
  const percentage = Math.min(Math.max((score / 10) * 100, 0), 100);
  
  // 등급 명칭 결정
  const getGrade = (s: number) => {
    if (s >= 9) return 'BEST';
    if (s >= 7) return 'GOOD';
    if (s >= 4) return 'NORMAL';
    return 'BAD';
  };

  const grade = getGrade(score);
  const colorClass = 
    grade === 'BEST' ? 'bg-emerald-500' : 
    grade === 'GOOD' ? 'bg-amber-500' : 
    grade === 'NORMAL' ? 'bg-blue-500' : 'bg-zinc-400';

  return (
    <div className="flex flex-col gap-1.5 w-full max-w-[120px]">
      <div className="flex justify-between items-center px-1">
        <span className="text-[9px] font-black text-navy-900 uppercase tracking-tighter">Manner Score</span>
        <span className={cn("text-[10px] font-black px-1.5 rounded-md text-white", colorClass)}>{score}/10</span>
      </div>
      <div className="h-2.5 bg-zinc-100 rounded-full overflow-hidden border border-zinc-200 p-0.5">
        <div 
          className={cn("h-full rounded-full transition-all duration-1000 ease-out shadow-sm", colorClass)}
          style={{ width: `${percentage}%` }}
        />
      </div>
    </div>
  );
};