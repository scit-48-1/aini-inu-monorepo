'use client';

import React, { useRef, useEffect, useState, useCallback, useMemo } from 'react';
import { createPortal } from 'react-dom';
import { ChevronLeft, ChevronRight, X } from 'lucide-react';
import { useBookAnimation } from '@/components/common/BookFlip/useBookAnimation';
import { DiaryPageRenderer } from './DiaryModal/DiaryPageRenderer';
import { DesktopBookEngine } from './DiaryModal/DesktopBookEngine';
import { MobileBookEngine } from './DiaryModal/MobileBookEngine';
import { useUserStore } from '@/store/useUserStore';
import type { WalkDiaryResponse } from '@/api/diaries';
import { updateDiary } from '@/api/diaries';
import type { StoryGroupResponse, StoryDiaryItemResponse } from '@/api/community';
import { toast } from 'sonner';

type EditMode = 'NONE' | 'CONTENT' | 'PHOTOS';

interface DiaryFormValues {
  title: string;
  content: string;
  photoUrls: string[];
  walkDate: string;
  isPublic: boolean;
  threadId?: number;
}

interface DiaryBookModalProps {
  isOpen: boolean;
  onClose: () => void;
  mode: 'profile' | 'story';
  // Profile mode props
  selectedDiaryId?: number;
  diaries?: WalkDiaryResponse[];
  onSaveSuccess?: () => void;
  onDelete?: (diaryId: number) => void;
  // Story mode props
  storyGroups?: StoryGroupResponse[];
  initialMemberIndex?: number;
}

/** Adapt a StoryDiaryItemResponse to WalkDiaryResponse shape for unified rendering */
function adaptStoryDiary(diary: StoryDiaryItemResponse, group: StoryGroupResponse): WalkDiaryResponse {
  return {
    id: diary.diaryId,
    memberId: group.memberId,
    threadId: 0,
    title: diary.title,
    content: diary.content,
    photoUrls: diary.photoUrls,
    walkDate: diary.walkDate,
    linkedThreadStatus: '',
    createdAt: diary.createdAt,
    updatedAt: diary.createdAt,
    public: true,
    isPublic: true,
  };
}

export const DiaryBookModal: React.FC<DiaryBookModalProps> = ({
  isOpen, onClose, mode,
  selectedDiaryId, diaries, onSaveSuccess, onDelete,
  storyGroups, initialMemberIndex,
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

  // Track which story group (member) is currently viewed in story mode
  const [currentMemberIdx, setCurrentMemberIdx] = useState(0);

  // Build the flattened diary list and member boundary tracking for story mode
  const { diaryList, memberBoundaries, storyHeaderMap } = useMemo(() => {
    if (mode === 'profile') {
      return {
        diaryList: diaries || [],
        memberBoundaries: [] as number[],
        storyHeaderMap: new Map<number, { nickname: string; profileImageUrl: string; createdAt: string }>(),
      };
    }

    // Story mode: flatten all groups into sequential diary list
    const groups = storyGroups || [];
    const list: WalkDiaryResponse[] = [];
    const boundaries: number[] = []; // indices where a new member starts
    const headerMap = new Map<number, { nickname: string; profileImageUrl: string; createdAt: string }>();

    for (const group of groups) {
      boundaries.push(list.length);
      for (const diary of group.diaries) {
        const adapted = adaptStoryDiary(diary, group);
        headerMap.set(adapted.id, {
          nickname: group.nickname,
          profileImageUrl: group.profileImageUrl,
          createdAt: diary.createdAt,
        });
        list.push(adapted);
      }
    }

    return { diaryList: list, memberBoundaries: boundaries, storyHeaderMap: headerMap };
  }, [mode, diaries, storyGroups]);

  // Determine which diary to start on
  const startDiaryIndex = useMemo(() => {
    if (mode === 'profile') {
      if (selectedDiaryId == null) return 0;
      const idx = diaryList.findIndex(d => d.id === selectedDiaryId);
      return idx >= 0 ? idx : 0;
    }
    // Story mode: start at initialMemberIndex
    const memberIdx = initialMemberIndex ?? 0;
    const boundaries = storyGroups
      ? storyGroups.slice(0, memberIdx).reduce((sum, g) => sum + g.diaries.length, 0)
      : 0;
    return boundaries;
  }, [mode, selectedDiaryId, diaryList, initialMemberIndex, storyGroups]);

  const selectedHistory = useMemo(() =>
    diaryList[startDiaryIndex] || diaryList[0]
  , [diaryList, startDiaryIndex]);

  // Owner check
  const isReadOnly = useMemo(() => {
    if (mode === 'story') return true; // Always readOnly in story mode
    if (!selectedHistory || !myProfile) return true;
    return selectedHistory.memberId !== Number(myProfile.id);
  }, [mode, selectedHistory, myProfile]);

  // Form state for editing (profile mode only)
  const [diaryForm, setDiaryForm] = useState<DiaryFormValues>({
    title: '',
    content: '',
    photoUrls: [],
    walkDate: '',
    isPublic: false,
  });

  const handleSave = useCallback(async (id: number) => {
    try {
      await updateDiary(id, {
        title: diaryForm.title,
        content: diaryForm.content,
        photoUrls: diaryForm.photoUrls,
        walkDate: diaryForm.walkDate,
        isPublic: diaryForm.isPublic,
      });
      setEditMode('NONE');
      toast.success('저장되었습니다.');
      onSaveSuccess?.();
    } catch (err) {
      console.error('Failed to save diary:', err);
      toast.error('저장에 실패했습니다.');
    }
  }, [diaryForm, onSaveSuccess]);

  // Sync form state when selected diary changes
  useEffect(() => {
    if (selectedHistory) {
      setDiaryForm({
        title: selectedHistory.title || '',
        content: selectedHistory.content || '',
        photoUrls: selectedHistory.photoUrls || [],
        walkDate: selectedHistory.walkDate || '',
        isPublic: !!selectedHistory.isPublic,
        threadId: selectedHistory.threadId || undefined,
      });
    }
  }, [selectedHistory]);

  const initialPageIndex = useMemo(() => {
    return startDiaryIndex >= 0 ? startDiaryIndex * 2 : 0;
  }, [startDiaryIndex]);

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

  // Reset state on modal open or diary selection change
  useEffect(() => {
    if (isOpen) {
      setCurrentLibIndex(initialPageIndex);
      setEditMode('NONE');
      setZoomedPhoto(null);
      setCurrentMemberIdx(initialMemberIndex ?? 0);
    }
  }, [isOpen, selectedDiaryId, initialPageIndex, initialMemberIndex]);

  useEffect(() => {
    if (isOpen) {
      updateDimensions();
      window.addEventListener('resize', updateDimensions);
      return () => window.removeEventListener('resize', updateDimensions);
    }
  }, [isOpen, updateDimensions]);

  const { pageDirection, tempNextData, navigate: customNavigate } = useBookAnimation({
    dataList: diaryList,
    onPageChange: (nextDiary: WalkDiaryResponse) => {
      setDiaryForm({
        title: nextDiary.title,
        content: nextDiary.content,
        photoUrls: nextDiary.photoUrls || [],
        walkDate: nextDiary.walkDate || '',
        isPublic: !!nextDiary.isPublic,
        threadId: nextDiary.threadId || undefined,
      });

      // In story mode, track member transitions
      if (mode === 'story' && storyGroups) {
        const diaryIdx = diaryList.findIndex(d => d.id === nextDiary.id);
        if (diaryIdx >= 0) {
          // Find which member this diary belongs to
          let memberIdx = 0;
          for (let i = memberBoundaries.length - 1; i >= 0; i--) {
            if (diaryIdx >= memberBoundaries[i]) {
              memberIdx = i;
              break;
            }
          }
          setCurrentMemberIdx(memberIdx);
        }
      }
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

  const renderContent = (data: WalkDiaryResponse, side: 'LEFT' | 'RIGHT') => {
    const dataIndex = diaryList.findIndex(h => h.id === data.id);
    const isVisibleInLib = !isMobile && Math.floor(currentLibIndex / 2) === dataIndex;
    const isCurrent = isMobile ? (data.id === (tempNextData || selectedHistory)?.id) : isVisibleInLib;

    // Story header for story mode
    const storyHeader = mode === 'story' ? storyHeaderMap.get(data.id) : undefined;

    const handleToggleVisibility = async (diaryId: number, newIsPublic: boolean) => {
      try {
        await updateDiary(diaryId, { isPublic: newIsPublic });
        toast.success(newIsPublic ? '공개로 변경되었습니다.' : '비공개로 변경되었습니다.');
        onSaveSuccess?.();
      } catch (err) {
        console.error('Failed to toggle visibility:', err);
        toast.error('변경에 실패했습니다.');
      }
    };

    return (
      <DiaryPageRenderer
        data={data} side={side} isCurrent={isCurrent} isReadOnly={isReadOnly} editMode={editMode}
        diaryForm={diaryForm} setDiaryForm={setDiaryForm} onClose={onClose}
        onSave={() => handleSave(data.id)} setEditMode={setEditMode}
        onZoom={(photo) => setZoomedPhoto(photo)}
        onImageUpload={async (imageUrl: string) => setDiaryForm((prev: any) => ({ ...prev, photoUrls: [...prev.photoUrls, imageUrl] }))}
        onDelete={mode === 'profile' && onDelete && !isReadOnly ? (() => onDelete(data.id)) : undefined}
        storyHeader={storyHeader}
        onToggleVisibility={!isReadOnly ? handleToggleVisibility : undefined}
      />
    );
  };

  if (!isOpen || diaryList.length === 0) return null;

  return createPortal(
    <div className="fixed inset-0 z-[6000] bg-navy-900/80 backdrop-blur-xl flex items-center justify-center p-4 md:p-10 animate-in fade-in duration-500">
      {/* Close Button Overlay */}
      <button onClick={onClose} className="absolute top-10 right-10 p-4 text-white/40 hover:text-white transition-all z-[7000]"><X size={40} /></button>

      {/* Photo Zoom Overlay */}
      {zoomedPhoto ? (
        <div
          className="fixed inset-0 z-[8000] bg-black/90 flex items-center justify-center animate-in fade-in duration-200"
          onClick={() => setZoomedPhoto(null)}
        >
          <button className="absolute top-8 right-8 p-3 text-white/50 hover:text-white transition-all z-[8001]"><X size={36} /></button>
          <img src={zoomedPhoto} className="max-w-[90vw] max-h-[90vh] object-contain shadow-2xl" alt="Zoomed" />
        </div>
      ) : null}

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
          {diaryList.length > 1 ? (
            <>
              <button onClick={() => handleNavigate('prev')} className="absolute -left-16 md:-left-20 top-1/2 -translate-y-1/2 z-[3100] p-4 text-white/20 hover:text-white transition-all active:scale-95 disabled:opacity-0" disabled={editMode !== 'NONE'}><ChevronLeft size={80} strokeWidth={1} /></button>
              <button onClick={() => handleNavigate('next')} className="absolute -right-16 md:-right-20 top-1/2 -translate-y-1/2 z-[3100] p-4 text-white/20 hover:text-white transition-all active:scale-95 disabled:opacity-0" disabled={editMode !== 'NONE'}><ChevronRight size={80} strokeWidth={1} /></button>
            </>
          ) : null}
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
