'use client';

import React from 'react';
import { MapPin, Edit2, Footprints, PlusCircle, Siren, Activity, RefreshCw, CalendarDays, Radar } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Typography } from '@/components/ui/Typography';

type SubView = 'FIND' | 'RECRUIT' | 'EMERGENCY' | 'WALKING';

interface AroundMeHeaderProps {
  currentLocation: string;
  onLocationClick: () => void;
  activeTab: SubView;
  onTabChange: (tab: SubView) => void;
  onRefresh?: () => void;
  isRefreshing?: boolean;
  dateFrom?: string;
  dateTo?: string;
  onDateFromChange?: (val: string) => void;
  onDateToChange?: (val: string) => void;
  radius?: number;
  onRadiusChange?: (val: number) => void;
}

export const AroundMeHeader: React.FC<AroundMeHeaderProps> = ({
  currentLocation,
  onLocationClick,
  activeTab,
  onTabChange,
  onRefresh,
  isRefreshing = false,
  dateFrom,
  dateTo,
  onDateFromChange,
  onDateToChange,
  radius = 5,
  onRadiusChange,
}) => {
  return (
    <div className="p-6 md:px-10 md:py-6 flex flex-col lg:flex-row items-start lg:items-center justify-between gap-6 border-b border-card-border bg-white sticky top-0 z-20 shadow-sm transition-all">
      <div className="flex flex-col sm:flex-row items-start sm:items-center gap-4 w-full lg:w-auto">
        <div className="flex items-center gap-3">
          <h2 className="text-xl md:text-2xl font-black text-black font-serif flex items-center gap-2">
            <span className="flex items-center"><svg width="28" height="28" viewBox="0 0 24 24" fill="#EF4444" xmlns="http://www.w3.org/2000/svg"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg></span>
            동네 탐색
          </h2>
        </div>
        <div className="h-4 w-px bg-zinc-100 hidden sm:block mx-2" />
        <div className="flex items-center gap-2">
          <button onClick={onLocationClick} className="text-black font-black text-xs flex items-center gap-2 bg-zinc-50/50 px-4 py-2 rounded-full border border-zinc-100 hover:bg-white hover:border-amber-500 hover:shadow-sm transition-all active:scale-95 group">
            <MapPin size={14} className="text-amber-500" /> {currentLocation} <Edit2 size={10} className="text-zinc-300 group-hover:text-amber-500 ml-1" />
          </button>
          {onDateFromChange && (
            <div className="flex items-center gap-1.5 bg-zinc-50/50 px-3 py-1.5 rounded-full border border-zinc-100 text-xs font-black text-zinc-500">
              <CalendarDays size={12} className="text-amber-500" />
              <input
                type="date"
                value={dateFrom ?? ''}
                onChange={(e) => onDateFromChange(e.target.value)}
                className="bg-transparent border-none outline-none text-xs font-black text-zinc-600 w-[110px]"
              />
              <span className="text-zinc-300">~</span>
              <input
                type="date"
                value={dateTo ?? ''}
                onChange={(e) => onDateToChange?.(e.target.value)}
                className="bg-transparent border-none outline-none text-xs font-black text-zinc-600 w-[110px]"
              />
            </div>
          )}
          {onRadiusChange && (
            <div className="flex items-center gap-1.5 bg-zinc-50/50 px-3 py-1.5 rounded-full border border-zinc-100 text-xs font-black text-zinc-500">
              <Radar size={12} className="text-amber-500" />
              <select
                value={radius}
                onChange={(e) => onRadiusChange(Number(e.target.value))}
                className="bg-transparent border-none outline-none text-xs font-black text-zinc-600 cursor-pointer"
              >
                {[1, 2, 3, 5, 10, 20, 50, 100].map((r) => (
                  <option key={r} value={r}>{r}km</option>
                ))}
              </select>
            </div>
          )}
          {onRefresh && (
            <button
              onClick={onRefresh}
              disabled={isRefreshing}
              title="재탐색"
              className="flex items-center gap-1.5 text-black font-black text-xs bg-zinc-50/50 px-3 py-2 rounded-full border border-zinc-100 hover:bg-white hover:border-amber-500 hover:shadow-sm transition-all active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <RefreshCw size={14} className={cn('text-amber-500', isRefreshing && 'animate-spin')} />
              <span>재탐색</span>
            </button>
          )}
        </div>
      </div>
      <div className="flex items-center gap-3">
        <nav className="flex bg-zinc-50/50 p-1 rounded-xl border border-zinc-100">
        {[
          { id: 'FIND', label: '산책', icon: Footprints },
          { id: 'RECRUIT', label: '모집', icon: PlusCircle },
          { id: 'EMERGENCY', label: '제보', icon: Siren },
          { id: 'WALKING', label: '산책중', icon: Activity },
        ].map(tab => (
          <button
            key={tab.id}
            onClick={() => onTabChange(tab.id as SubView)}
            className={cn(
              'px-6 py-2 rounded-lg text-xs font-black transition-all flex items-center gap-2',
              activeTab === tab.id ? 'bg-amber-500 text-black shadow-md' : 'text-black hover:bg-zinc-50',
            )}
          >
            <tab.icon size={16} /> {tab.label}
          </button>
        ))}
      </nav>
      </div>
    </div>
  );
};
