'use client';

import React from 'react';
import { Grid, History, Star, Clock, LucideIcon } from 'lucide-react';
import { cn } from '@/lib/utils';

export type ProfileTab = 'FEED' | 'HISTORY' | 'REVIEWS' | 'TIMELINE';

interface TabItem {
  id: ProfileTab;
  label: string;
  icon: LucideIcon;
}

interface ProfileTabsProps {
  activeTab: ProfileTab;
  onTabChange: (tab: ProfileTab) => void;
}

const tabs: TabItem[] = [
  { id: 'TIMELINE', label: '타임라인', icon: Clock },
  { id: 'FEED', label: '포스팅', icon: Grid },
  { id: 'HISTORY', label: '산책일기', icon: History },
  { id: 'REVIEWS', label: '리뷰', icon: Star },
];

export const ProfileTabs: React.FC<ProfileTabsProps> = ({ activeTab, onTabChange }) => {
  return (
    <div className="flex justify-center border-t border-zinc-100 mt-4">
      {tabs.map(tab => (
        <button 
          key={tab.id} 
          onClick={() => onTabChange(tab.id)} 
          className={cn(
            "flex-1 lg:flex-none lg:px-16 py-4 flex items-center justify-center gap-2 font-black text-[10px] lg:text-xs transition-all border-t-2 -mt-[2px] uppercase tracking-widest", 
            activeTab === tab.id ? 'border-navy-900 text-navy-900 scale-105' : 'border-transparent text-zinc-300'
          )}
        >
          <tab.icon size={16} /> 
          <span className="hidden lg:inline">{tab.label}</span>
        </button>
      ))}
    </div>
  );
};
