'use client';

import React, { useState } from 'react';
import { cn } from '@/lib/utils';
import { EmergencyReportForm } from '@/components/around-me/EmergencyReportForm';
import type { LostPetResponse } from '@/api/lostPets';

type EmergencySubTab = 'REPORT' | 'MY_LIST';

export const EmergencySubTabs: React.FC = () => {
  const [activeSubTab, setActiveSubTab] = useState<EmergencySubTab>('REPORT');

  return (
    <div className="flex-1 overflow-y-auto no-scrollbar space-y-6">
      {/* Sub-tab toggle */}
      <div className="flex bg-white p-1.5 rounded-2xl border border-card-border shadow-sm">
        <button
          onClick={() => setActiveSubTab('REPORT')}
          className={cn(
            'flex-1 py-3 rounded-xl text-xs font-black transition-all',
            activeSubTab === 'REPORT'
              ? 'bg-amber-500 text-black shadow-md'
              : 'text-black hover:bg-zinc-50',
          )}
        >
          신고/제보 작성
        </button>
        <button
          onClick={() => setActiveSubTab('MY_LIST')}
          className={cn(
            'flex-1 py-3 rounded-xl text-xs font-black transition-all',
            activeSubTab === 'MY_LIST'
              ? 'bg-amber-500 text-black shadow-md'
              : 'text-black hover:bg-zinc-50',
          )}
        >
          내 신고 목록
        </button>
      </div>

      {/* Sub-tab content */}
      {activeSubTab === 'REPORT' ? (
        <EmergencyReportForm />
      ) : (
        <div className="flex-1 flex items-center justify-center py-20">
          <p className="text-zinc-400 font-bold text-sm">내 신고 목록</p>
        </div>
      )}
    </div>
  );
};
