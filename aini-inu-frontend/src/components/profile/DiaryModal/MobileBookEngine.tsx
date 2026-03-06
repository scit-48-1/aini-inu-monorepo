'use client';

import React from 'react';
import { BookFlipContainer } from '@/components/common/BookFlip/BookFlipContainer';
import { ChevronUp, ChevronDown } from 'lucide-react';
import type { WalkDiaryResponse } from '@/api/diaries';

interface MobileBookEngineProps {
  pageDirection: 'next' | 'prev' | null;
  renderContent: (data: WalkDiaryResponse, side: 'LEFT' | 'RIGHT') => React.ReactNode;
  currentData: WalkDiaryResponse;
  tempNextData: WalkDiaryResponse | null;
  onNavigate: (dir: 'next' | 'prev') => void;
  editMode: 'NONE' | 'CONTENT' | 'PHOTOS';
  isSingle?: boolean;
}

export const MobileBookEngine: React.FC<MobileBookEngineProps> = ({
  pageDirection, renderContent, currentData, tempNextData, onNavigate, editMode, isSingle = false
}) => {
  return (
    <BookFlipContainer
      pageDirection={pageDirection}
      renderPageSide={renderContent}
      currentData={currentData}
      tempNextData={tempNextData}
    >
      {!isSingle && (
        <div className="absolute inset-0 z-[1000] pointer-events-none flex flex-col justify-between p-4">
          <button
            onClick={() => onNavigate('prev')}
            className="w-full h-20 pointer-events-auto flex items-center justify-center text-white/10 active:text-white/40 disabled:opacity-0"
            disabled={editMode !== 'NONE'}
          >
            <ChevronUp size={48} />
          </button>
          <button
            onClick={() => onNavigate('next')}
            className="w-full h-20 pointer-events-auto flex items-center justify-center text-white/10 active:text-white/40 disabled:opacity-0"
            disabled={editMode !== 'NONE'}
          >
            <ChevronDown size={48} />
          </button>
        </div>
      )}
    </BookFlipContainer>
  );
};
