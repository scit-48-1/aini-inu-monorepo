'use client';

import React, { ReactNode } from 'react';
import { cn } from '@/lib/utils';
import type { WalkDiaryResponse } from '@/api/diaries';

interface BookFlipContainerProps {
  children: ReactNode;
  pageDirection: 'next' | 'prev' | null;
  renderPageSide: (data: WalkDiaryResponse, side: 'LEFT' | 'RIGHT') => ReactNode;
  currentData: WalkDiaryResponse;
  tempNextData: WalkDiaryResponse | null;
  className?: string;
}

export const BookFlipContainer: React.FC<BookFlipContainerProps> = ({
  children,
  pageDirection,
  renderPageSide,
  currentData,
  tempNextData,
  className
}) => {
  // Floor 레이어는 항상 currentData를 보여줌 — 애니메이션 완료 후 onPageChange로 갱신
  const floorData = currentData;

  return (
    <div className={cn(
      "w-full max-w-6xl bg-[#fdfbf7] rounded-[40px] flex relative border-[12px] border-[#2c3e50] shadow-2xl transition-all duration-700",
      "h-[80vh] flex-col md:flex-row",
      "perspective-[3000px] [perspective-origin:center]", 
      pageDirection && "pointer-events-none",
      className
    )}>
      {children}
      <style dangerouslySetInnerHTML={{ __html: `
        .flipping-sheet {
          position: absolute;
          z-index: 500;
          transform-style: preserve-3d;
          will-change: transform;
        }

        /* 가로 플립 (데스크톱) */
        @keyframes sheetFlipNext { 0% { transform: rotateY(0deg); } 100% { transform: rotateY(-180deg); } }
        @keyframes sheetFlipPrev { 0% { transform: rotateY(0deg); } 100% { transform: rotateY(180deg); } }

        /* 세로 플립 (모바일) */
        @keyframes sheetFlipNextV { 0% { transform: rotateX(0deg); } 100% { transform: rotateX(180deg); } }
        @keyframes sheetFlipPrevV { 0% { transform: rotateX(0deg); } 100% { transform: rotateX(-180deg); } }

        @keyframes shadowFade { 0% { opacity: 0; } 50% { opacity: 0.6; } 100% { opacity: 0; } }
      ` }} />

      {/* --- 상단/왼쪽 페이지 (Floor) --- */}
      <div className="flex-1 h-full relative overflow-hidden md:rounded-l-[28px] rounded-t-[28px]">
        {renderPageSide(floorData, 'LEFT')}
        <div className="absolute bottom-6 left-10 text-[10px] font-black text-zinc-300 pointer-events-none uppercase tracking-widest">Page 01</div>
      </div>

      {/* --- 하단/오른쪽 페이지 (Floor) --- */}
      <div className="flex-1 h-full relative overflow-hidden md:rounded-r-[28px] rounded-b-[28px] md:border-l border-t md:border-t-0 border-zinc-100">
        {renderPageSide(floorData, 'RIGHT')}
        <div className="absolute bottom-6 right-10 text-[10px] font-black text-zinc-300 pointer-events-none uppercase tracking-widest">Page 02</div>
      </div>

      {/* --- [DESKTOP] 가로 플립 시트 --- */}
      <div className="hidden md:block">
        {pageDirection && tempNextData && (
          <div className="flipping-sheet inset-y-0 w-1/2" 
               style={{ 
                 right: pageDirection === 'next' ? 0 : 'auto',
                 left: pageDirection === 'prev' ? 0 : 'auto',
                 transformOrigin: pageDirection === 'next' ? 'left center' : 'right center',
                 animation: `${pageDirection === 'next' ? 'sheetFlipNext' : 'sheetFlipPrev'} 1s cubic-bezier(0.645, 0.045, 0.355, 1) forwards`
               }}>
            <div className={cn("absolute inset-0 backface-hidden bg-[#fdfbf7] overflow-hidden z-[2]", pageDirection === 'next' ? "rounded-tr-[28px] rounded-br-[28px]" : "rounded-tl-[28px] rounded-bl-[28px]")}>
              {renderPageSide(currentData, pageDirection === 'next' ? 'RIGHT' : 'LEFT')}
              <div className="absolute inset-0 z-10 bg-black/5" />
            </div>
            <div className={cn("absolute inset-0 backface-hidden bg-[#fdfbf7] overflow-hidden [transform:rotateY(180deg)] z-[1]", pageDirection === 'next' ? "rounded-tl-[28px] rounded-bl-[28px]" : "rounded-tr-[28px] rounded-br-[28px]")}>
              {renderPageSide(tempNextData, pageDirection === 'next' ? 'LEFT' : 'RIGHT')}
              <div className="absolute inset-0 bg-black/5" />
            </div>
          </div>
        )}
      </div>

      {/* --- [MOBILE] 세로 플립 시트 --- */}
      <div className="md:hidden block">
        {pageDirection && tempNextData && (
          <div className="flipping-sheet inset-x-0 h-1/2" 
               style={{ 
                 bottom: pageDirection === 'next' ? 0 : 'auto',
                 top: pageDirection === 'prev' ? 0 : 'auto',
                 transformOrigin: pageDirection === 'next' ? 'center top' : 'center bottom',
                 animation: `${pageDirection === 'next' ? 'sheetFlipNextV' : 'sheetFlipPrevV'} 1s cubic-bezier(0.645, 0.045, 0.355, 1) forwards`
               }}>
            <div className={cn("absolute inset-0 backface-hidden bg-[#fdfbf7] overflow-hidden z-[2]", pageDirection === 'next' ? "rounded-bl-[28px] rounded-br-[28px]" : "rounded-tl-[28px] rounded-tr-[28px]")}>
              {renderPageSide(currentData, pageDirection === 'next' ? 'RIGHT' : 'LEFT')}
              <div className="absolute inset-0 z-10 bg-black/5" />
            </div>
            <div className={cn("absolute inset-0 backface-hidden bg-[#fdfbf7] overflow-hidden [transform:rotateX(180deg)] z-[1]", pageDirection === 'next' ? "rounded-tl-[28px] rounded-tr-[28px]" : "rounded-bl-[28px] rounded-br-[28px]")}>
              {renderPageSide(tempNextData, pageDirection === 'next' ? 'LEFT' : 'RIGHT')}
              <div className="absolute inset-0 bg-black/5" />
            </div>
          </div>
        )}
      </div>

      <div className="hidden md:block absolute left-1/2 top-0 bottom-0 w-px bg-black/10 z-[600] shadow-[0_0_15px_rgba(0,0,0,0.2)]" />
      <div className="md:hidden absolute top-1/2 left-0 right-0 h-px bg-black/10 z-[600] shadow-[0_0_15px_rgba(0,0,0,0.2)]" />
    </div>
  );
};
