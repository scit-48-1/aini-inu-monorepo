'use client';

import React from 'react';
import { ChevronRight } from 'lucide-react';
import { Typography } from '@/components/ui/Typography';
import { cn } from '@/lib/utils';
import { ProfileView } from '@/components/profile/ProfileView';

interface ProfileExplorerProps {
  partnerId: string;
  isOpen: boolean;
  onClose: () => void;
}

export const ProfileExplorer: React.FC<ProfileExplorerProps> = ({ partnerId, isOpen, onClose }) => {
  return (
    <aside className={cn(
      "h-full bg-white flex flex-col border-l border-zinc-100",
      "transition-all duration-500 ease-in-out",
      // Mobile/Tablet: fixed 오버레이 — translate로 슬라이드 인/아웃
      "fixed inset-y-0 right-0 z-[100] w-full sm:w-[400px] shadow-2xl",
      isOpen ? "translate-x-0" : "translate-x-full",
      // Desktop xl+: 인라인 패널 — width로 부드럽게 확장/축소
      "xl:relative xl:z-0 xl:shadow-none xl:translate-x-0 xl:overflow-hidden",
      isOpen ? "xl:w-1/2 xl:shrink-0 xl:opacity-100" : "xl:w-0 xl:opacity-0",
    )}>
      {/* 패널 헤더 */}
      <div className="p-4 border-b border-zinc-50 flex items-center justify-between bg-white/80 backdrop-blur-md shrink-0 z-20">
        <button
          onClick={onClose}
          className="p-2 -ml-2 text-zinc-300 hover:text-navy-900 transition-all hover:scale-110 active:scale-90 flex items-center gap-1 group"
        >
          <ChevronRight size={24} strokeWidth={3} className="group-hover:translate-x-1 transition-transform" />
          <Typography variant="label" className="text-[9px] font-black opacity-0 group-hover:opacity-100 transition-opacity">Close</Typography>
        </button>
        <Typography variant="label" className="text-[10px] font-black text-zinc-400 uppercase tracking-[0.3em]">Partner Profile</Typography>
      </div>

      {/* 프로필 컨텐츠 */}
      <div className="flex-1 overflow-y-auto no-scrollbar">
        <ProfileView memberId={partnerId} compact={true} />
      </div>
    </aside>
  );
};
