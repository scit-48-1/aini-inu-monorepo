'use client';

import React from 'react';
import { Clock, MapPin, Edit3, Lock } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import { Badge } from '@/components/ui/Badge';
import { cn } from '@/lib/utils';
import { WalkDiaryType } from '@/types';

interface ProfileHistoryProps {
  walkHistory: WalkDiaryType[];
  allDiaries: Record<string, WalkDiaryType>;
  onHistoryClick: (thread: WalkDiaryType) => void;
}

export const ProfileHistory: React.FC<ProfileHistoryProps> = ({ walkHistory, allDiaries, onHistoryClick }) => {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-6 p-6 md:p-8 animate-in slide-in-from-bottom-4 duration-500">
      {walkHistory.map((thread) => {
        const diary = allDiaries[thread.id];
        const isDraft = diary?.isDraft;
        const thumbnail = diary?.photos?.[0] || thread.image || thread.thumbnail;

        return (
          <Card 
            key={thread.id} 
            interactive 
            className={cn(
              "p-6 rounded-[40px] border-zinc-50 shadow-lg flex gap-6 items-center group relative overflow-hidden transition-all",
              isDraft ? "bg-zinc-50/80 opacity-70 border-dashed border-zinc-200" : "bg-white"
            )} 
            onClick={() => onHistoryClick(thread)}
          >
            <div className="w-20 h-20 rounded-2xl relative shrink-0 overflow-hidden shadow-md group-hover:scale-105 transition-transform duration-500">
              <img src={thumbnail} alt="Walk" className={cn("w-full h-full object-cover", isDraft && "brightness-[0.4] grayscale-[0.2]")} />
              {isDraft && (
                <div className="absolute inset-0 flex items-center justify-center text-white/80">
                  <Edit3 size={24} strokeWidth={2.5} className="animate-pulse" />
                </div>
              )}
            </div>
            
            <div className="flex-1 space-y-3 min-w-0 relative z-10">
              <div className="flex items-center gap-2">
                <Typography variant="h3" className="text-lg font-black text-navy-900 truncate">{thread.diaryTitle}</Typography>
                {isDraft && <Badge variant="amber" className="bg-amber-500 text-white border-none text-[9px] px-2.5 py-0.5 shrink-0 uppercase font-black tracking-widest shadow-sm animate-pulse">Pending</Badge>}
                {!diary?.isPublic && !isDraft && <Lock size={12} className="text-zinc-300 shrink-0" />}
              </div>
              <div className="flex flex-col gap-1">
                <div className="flex items-center gap-2 text-zinc-400 text-[10px] font-black uppercase">
                  <Clock size={10} className="text-amber-500" /> {thread.walkDate}
                </div>
                <div className="flex items-center gap-2 text-zinc-400 text-[10px] font-black uppercase">
                  <MapPin size={10} className="text-amber-500" /> 
                  <span className="truncate">{thread.place || thread.location}</span>
                </div>
              </div>
            </div>

            {isDraft && (
              <div className="absolute -right-4 -bottom-4 opacity-[0.07] select-none pointer-events-none rotate-[-15deg]">
                <span className="text-7xl font-black text-navy-900 whitespace-nowrap italic">PENDING</span>
              </div>
            )}
          </Card>
        );
      })}
    </div>
  );
};
