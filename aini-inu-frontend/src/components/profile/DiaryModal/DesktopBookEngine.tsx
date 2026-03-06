'use client';

import React from 'react';
import HTMLFlipBook from 'react-pageflip';
import { cn } from '@/lib/utils';
import type { WalkDiaryResponse } from '@/api/diaries';

interface DesktopBookEngineProps {
  dimensions: { width: number; height: number };
  instanceKey: number;
  walkHistoryDetails: WalkDiaryResponse[];
  onFlip: (e: { data: number }) => void;
  renderContent: (data: WalkDiaryResponse, side: 'LEFT' | 'RIGHT') => React.ReactNode;
  bookRef: React.RefObject<unknown>;
  initialPage: number;
  editMode: 'NONE' | 'CONTENT' | 'PHOTOS';
}

// 라이브러리 전용 페이지 래퍼 (하드 재질 + 모서리 라운딩 적용)
const LibPage = React.forwardRef<HTMLDivElement, { children: React.ReactNode; number: number; side: 'LEFT' | 'RIGHT' }>((props, ref) => (
  <div 
    className={cn(
      "w-full h-full bg-[#fdfbf7] overflow-hidden",
      props.side === 'LEFT' ? "rounded-l-[28px]" : "rounded-r-[28px]"
    )} 
    ref={ref} 
    data-density="hard"
  >
    <div className="w-full h-full flex flex-col">{props.children}</div>
  </div>
));
LibPage.displayName = 'LibPage';

export const DesktopBookEngine: React.FC<DesktopBookEngineProps> = ({
  dimensions, instanceKey, walkHistoryDetails, onFlip, renderContent, bookRef, initialPage, editMode
}) => {
  if (dimensions.width === 0) return null;

  return (
    <HTMLFlipBook 
      key={`lib-instance-${instanceKey}`}
      width={dimensions.width} 
      height={dimensions.height} 
      size="stretch"
      minWidth={200} maxWidth={2000} minHeight={200} maxHeight={2000}
      maxShadowOpacity={0.4} 
      showCover={false} 
      mobileScrollSupport={true}
      className="flip-book" 
      ref={bookRef} 
      drawShadow={true} 
      flippingTime={1200}
      usePortrait={false} 
      startZIndex={100} 
      autoSize={false} 
      showPageCorners={editMode === 'NONE'} // [MODIFIED] 수정 중에는 모서리 효과 끔
      clickEventForward={true} 
      useMouseEvents={editMode === 'NONE'} // [MODIFIED] 수정 중에는 마우스 드래그 금지
      swipeDistance={50}
      onFlip={onFlip}
      startPage={initialPage}
      style={{}}
      disableFlipByClick={editMode !== 'NONE'} // [MODIFIED] 클릭으로 넘기기 방지
    >
      {walkHistoryDetails.map((item, i) => [
        <LibPage key={`l-${item.id}`} number={i*2+1} side="LEFT">
          {renderContent(item, 'LEFT')}
        </LibPage>,
        <LibPage key={`r-${item.id}`} number={i*2+2} side="RIGHT">
          {renderContent(item, 'RIGHT')}
        </LibPage>
      ])}
    </HTMLFlipBook>
  );
};