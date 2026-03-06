'use client';

import React, { useRef, useEffect, useState, useCallback, useMemo } from 'react';
import { createPortal } from 'react-dom';
import { ChevronLeft, ChevronRight, X } from 'lucide-react';
import { useBookAnimation } from '@/components/common/BookFlip/useBookAnimation';
import { DiaryPageRenderer } from './DiaryModal/DiaryPageRenderer';
import { DesktopBookEngine } from './DiaryModal/DesktopBookEngine';
import { MobileBookEngine } from './DiaryModal/MobileBookEngine';
import { useUserStore } from '@/store/useUserStore';
import { WalkDiaryType } from '@/types';

type EditMode = 'NONE' | 'CONTENT' | 'PHOTOS';

interface DiaryBookModalProps {
  isOpen: boolean;
  onClose: () => void;
  selectedDiaryId: string;
  diaries: Record<string, WalkDiaryType> | WalkDiaryType[]; // 유연한 입력 지원
  onSaveSuccess?: () => void;
}

export const DiaryBookModal: React.FC<DiaryBookModalProps> = ({
  isOpen, onClose, selectedDiaryId, diaries, onSaveSuccess
}) => {
  const { profile: myProfile } = useUserStore();
  const bookRef = useRef<any>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  
  const [isMobile, setIsMobile] = useState(false);
  const [dimensions, setDimensions] = useState({ width: 550, height: 733 });
  const [instanceKey, setInstanceKey] = useState(0);
  const [currentLibIndex, setCurrentLibIndex] = useState(0);
  const [editMode, setEditMode] = useState<EditMode>('NONE');
  const [zoomedPhoto, setZoomedPhoto] = useState<string | null>(null);

  // 1. 데이터를 균일한 배열 구조로 변환 (엔진 최적화)
  const diaryList = useMemo(() => {
    const safeDiaries = diaries || {};
    if (Array.isArray(safeDiaries)) return safeDiaries;
    // Record 형태일 경우 키를 id로 보장
    return Object.entries(safeDiaries).map(([key, val]) => ({
      ...(val as WalkDiaryType),
      id: key,
    })) as WalkDiaryType[];
  }, [diaries]);

  const selectedHistory = useMemo(() => 
    diaryList.find(h => h.id === selectedDiaryId) || diaryList[0]
  , [diaryList, selectedDiaryId]);

  // 2. 권한 판단 (Master vs Read-Only)
  const isReadOnly = useMemo(() => {
    if (!selectedHistory || !myProfile) return true;
    return selectedHistory.authorId !== myProfile.id;
  }, [selectedHistory, myProfile]);

  // TODO: DiaryBookModal will be fully rewired in Plan 02 — using inline state for now
  const [diaryForm, setDiaryForm] = useState({
    title: '',
    content: '',
    photos: [] as string[],
    isPublic: false,
    tags: [] as { id: string; nickname: string; avatar: string }[],
  });

  const handleSave = useCallback(async (_id: string) => {
    // Placeholder — Plan 02 rewires with proper API calls
    setEditMode('NONE');
    onSaveSuccess?.();
  }, [onSaveSuccess]);

  useEffect(() => {
    if (selectedHistory) {
      setDiaryForm({
        title: selectedHistory.title || '',
        content: selectedHistory.content || '',
        photos: selectedHistory.photos || [],
        isPublic: !!selectedHistory.isPublic,
        tags: selectedHistory.tags || []
      });
    }
  }, [selectedHistory]);

  const initialPageIndex = useMemo(() => {
    const idx = diaryList.findIndex(h => h.id === selectedDiaryId);
    return idx >= 0 ? idx * 2 : 0;
  }, [diaryList, selectedDiaryId]);

  const updateDimensions = useCallback(() => {
    if (typeof window === 'undefined') return;
    const mobile = window.innerWidth < 1024;
    const prevMobile = isMobile;
    setIsMobile(mobile);
    
    if (!mobile && containerRef.current) {
      const { clientWidth, clientHeight } = containerRef.current;
      setDimensions({ 
        width: Math.floor(clientWidth / 2), 
        height: Math.floor(clientHeight) 
      });
      if (prevMobile) setInstanceKey(k => k + 1);
    }
  }, [isMobile]);

  // 모달 오픈 또는 선택 일기 변경 시 상태 초기화
  useEffect(() => {
    if (isOpen) {
      setCurrentLibIndex(initialPageIndex);
      setEditMode('NONE');
      setZoomedPhoto(null);
    }
  }, [isOpen, selectedDiaryId, initialPageIndex]);

  useEffect(() => {
    if (isOpen) {
      updateDimensions();
      window.addEventListener('resize', updateDimensions);
      return () => window.removeEventListener('resize', updateDimensions);
    }
  }, [isOpen, updateDimensions]);

  const { pageDirection, tempNextData, navigate: customNavigate } = useBookAnimation({
    dataList: diaryList,
    onPageChange: (nextDiary: any) => {
      setDiaryForm({
        title: nextDiary.title,
        content: nextDiary.content,
        photos: nextDiary.photos || [],
        isPublic: !!nextDiary.isPublic,
        tags: nextDiary.tags || []
      });
    }
  });

  const handleNavigate = useCallback((direction: 'next' | 'prev') => {
    if (editMode !== 'NONE') return;
    if (isMobile) {
      customNavigate(direction, selectedHistory?.id);
    } else if (bookRef.current) {
      const pageFlip = bookRef.current.pageFlip();
      if (pageFlip) {
        direction === 'next' ? pageFlip.flipNext() : pageFlip.flipPrev();
      }
    }
  }, [isMobile, customNavigate, selectedHistory, editMode]);

  const renderContent = (data: WalkDiaryType, side: 'LEFT' | 'RIGHT') => {
    const dataIndex = diaryList.findIndex(h => h.id === data.id);
    const isVisibleInLib = !isMobile && Math.floor(currentLibIndex / 2) === dataIndex;
    const isCurrent = isMobile ? (data.id === (tempNextData || selectedHistory)?.id) : isVisibleInLib;

    return (
      <DiaryPageRenderer
        data={data} side={side} isCurrent={isCurrent} isReadOnly={isReadOnly} editMode={editMode}
        diaryForm={diaryForm} setDiaryForm={setDiaryForm} onClose={onClose}
        onSave={() => handleSave(data.id)} setEditMode={setEditMode}
        onZoom={(photo) => setZoomedPhoto(photo)}
        onImageUpload={async (base64: string) => setDiaryForm((prev: any) => ({ ...prev, photos: [...prev.photos, base64] }))}
      />
    );
  };

  if (!isOpen || diaryList.length === 0) return null;

  return createPortal(
    <div className="fixed inset-0 z-[6000] bg-navy-900/80 backdrop-blur-xl flex items-center justify-center p-4 md:p-10 animate-in fade-in duration-500">
      {/* Close Button Overlay */}
      <button onClick={onClose} className="absolute top-10 right-10 p-4 text-white/40 hover:text-white transition-all z-[7000]"><X size={40} /></button>

      {/* Photo Zoom Overlay */}
      {zoomedPhoto && (
        <div
          className="fixed inset-0 z-[8000] bg-black/90 flex items-center justify-center animate-in fade-in duration-200"
          onClick={() => setZoomedPhoto(null)}
        >
          <button className="absolute top-8 right-8 p-3 text-white/50 hover:text-white transition-all z-[8001]"><X size={36} /></button>
          <img src={zoomedPhoto} className="max-w-[90vw] max-h-[90vh] object-contain shadow-2xl" alt="Zoomed" />
        </div>
      )}

      <style dangerouslySetInnerHTML={{ __html: `
        .flip-book { width: 100% !important; height: 100% !important; overflow: visible !important; }
        .stf__parent, .stf__wrapper { width: 100% !important; height: 100% !important; overflow: visible !important; }
      ` }} />

      {!isMobile ? (
        <div className="relative w-full max-w-6xl aspect-[1.4/1] max-h-[85vh] bg-[#2c3e50] rounded-[48px] p-[12px] shadow-2xl flex items-center justify-center overflow-visible [perspective:2000px]">
          <div ref={containerRef} className="w-full h-full rounded-[36px] relative bg-[#fdfbf7] overflow-visible flex justify-center">
            <DesktopBookEngine 
              dimensions={dimensions} instanceKey={instanceKey} walkHistoryDetails={diaryList}
              onFlip={(e) => setCurrentLibIndex(e.data)} renderContent={renderContent}
              bookRef={bookRef} initialPage={initialPageIndex}
              editMode={editMode}
            />
            <div className="absolute left-1/2 top-0 bottom-0 w-px bg-black/5 z-[1000] shadow-[0_0_20px_rgba(0,0,0,0.1)] pointer-events-none" />
          </div>
          {/* Navigation Controls */}
          {diaryList.length > 1 && (
            <>
              <button onClick={() => handleNavigate('prev')} className="fixed left-4 md:left-10 top-1/2 -translate-y-1/2 z-[3100] p-4 text-white/20 hover:text-white transition-all active:scale-95 disabled:opacity-0" disabled={editMode !== 'NONE'}><ChevronLeft size={80} strokeWidth={1} /></button>
              <button onClick={() => handleNavigate('next')} className="fixed right-4 md:right-10 top-1/2 -translate-y-1/2 z-[3100] p-4 text-white/20 hover:text-white transition-all active:scale-95 disabled:opacity-0" disabled={editMode !== 'NONE'}><ChevronRight size={80} strokeWidth={1} /></button>
            </>
          )}
        </div>
      ) : (
        <MobileBookEngine
          pageDirection={pageDirection} renderContent={renderContent}
          currentData={selectedHistory} tempNextData={tempNextData}
          onNavigate={handleNavigate}
          editMode={editMode}
          isSingle={diaryList.length <= 1}
        />
      )}
    </div>,
    document.body
  );
};
