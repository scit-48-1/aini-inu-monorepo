'use client';

import React, { useRef, useState, useEffect } from 'react';
import { X, Image as ImageIcon, AlignLeft, Plus, Trash2, MapPin, Calendar, Clock, PawPrint } from 'lucide-react';
import { Typography } from '@/components/ui/Typography';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { cn } from '@/lib/utils';
import { UserAvatar } from '@/components/common/UserAvatar';
import { KakaoStaticMap } from '@/components/common/KakaoStaticMap';
import type { WalkDiaryResponse } from '@/api/diaries';
import { uploadImageFlow } from '@/api/upload';

interface DiaryFormValues {
  title: string;
  content: string;
  photoUrls: string[];
  walkDate: string;
  isPublic: boolean;
  threadId?: number;
}

interface StoryHeader {
  nickname: string;
  profileImageUrl: string;
  createdAt: string;
}

interface DiaryPageRendererProps {
  data: WalkDiaryResponse;
  side: 'LEFT' | 'RIGHT';
  isCurrent: boolean;
  isReadOnly: boolean;
  editMode: 'NONE' | 'CONTENT' | 'PHOTOS';
  diaryForm: DiaryFormValues;
  setDiaryForm: React.Dispatch<React.SetStateAction<DiaryFormValues>>;
  onClose: () => void;
  onZoom: (photo: string) => void;
  setEditMode: (mode: 'NONE' | 'CONTENT' | 'PHOTOS') => void;
  onSave: (localValues: { title: string; content: string }) => void;
  onImageUpload: (imageUrl: string) => void;
  onDelete?: () => void;
  storyHeader?: StoryHeader;
  onToggleVisibility?: (diaryId: number, newIsPublic: boolean) => void;
}

export const DiaryPageRenderer: React.FC<DiaryPageRendererProps> = ({
  data, side, isCurrent, isReadOnly, editMode, diaryForm, setDiaryForm, onClose, onZoom, setEditMode, onSave, onImageUpload, onDelete, storyHeader, onToggleVisibility
}) => {
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Local state for edit inputs to avoid react-pageflip DOM management interference.
  // react-pageflip manages child DOM internally, preventing React from updating
  // controlled input values properly. Local state keeps inputs responsive.
  const [localTitle, setLocalTitle] = useState(diaryForm.title);
  const [localContent, setLocalContent] = useState(diaryForm.content);

  // Sync local state only when entering edit mode or switching diary (data.id change)
  useEffect(() => {
    setLocalTitle(diaryForm.title);
    setLocalContent(diaryForm.content);
  }, [editMode, data.id]); // eslint-disable-line react-hooks/exhaustive-deps

  if (!data) return null;

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files || []);
    for (const file of files) {
      try {
        const imageUrl = await uploadImageFlow(file, 'WALK_DIARY');
        onImageUpload(imageUrl);
      } catch (err) {
        console.error('Failed to upload image:', err);
      }
    }
    // Reset input so the same file can be re-selected
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  return (
    <div className="flex-1 overflow-y-auto p-8 md:p-12 no-scrollbar flex flex-col h-full bg-[#fdfbf7] relative border-none">
      <input type="file" ref={fileInputRef} className="hidden" accept="image/*" multiple onChange={handleFileUpload} />

      {side === 'LEFT' ? (
        <div className="space-y-6">
          {/* Story header (when viewing from feed story mode) */}
          {storyHeader ? (
            <div className="flex items-center gap-3 pb-2">
              <UserAvatar
                src={storyHeader.profileImageUrl || '/AINIINU_ROGO_B.png'}
                alt={storyHeader.nickname}
                hasRecentDiary={true}
                size="md"
              />
              <div className="flex flex-col">
                <span className="text-sm font-bold text-navy-900">{storyHeader.nickname}</span>
                <span className="text-[10px] text-zinc-400">{storyHeader.createdAt}</span>
              </div>
            </div>
          ) : null}

          <header className="space-y-2">
            <div className="flex items-center justify-between">
              <Typography variant="label" className="text-amber-600 font-black uppercase tracking-[0.3em] text-[10px]">Walk Record</Typography>
              {isCurrent && !isReadOnly && onDelete ? (
                <button onClick={onDelete} className="p-1.5 text-zinc-300 hover:text-red-500 transition-colors" title="삭제">
                  <Trash2 size={16} />
                </button>
              ) : null}
            </div>
            <Typography variant="h2" className="text-navy-900 font-serif lowercase italic leading-tight text-2xl md:text-3xl">
              {data.title || '즐거운 산책'}
            </Typography>
            <div className="h-1 w-16 bg-amber-500 rounded-full" />
          </header>

          {/* Walk date & time */}
          <div className="space-y-2">
            <Typography variant="label" className="text-zinc-400 font-black uppercase tracking-widest text-[9px]">Walk Date</Typography>
            <div className="flex items-center gap-2 text-sm text-zinc-600 font-serif">
              <Calendar size={14} className="text-amber-500" />
              <span>{data.thread?.walkDate || data.walkDate}</span>
            </div>
            {data.thread?.startTime ? (
              <div className="flex items-center gap-2 text-xs text-zinc-500 font-serif">
                <Clock size={12} className="text-amber-400" />
                <span>
                  {data.thread.startTime.slice(11, 16)}
                  {data.thread.endTime ? ` ~ ${data.thread.endTime.slice(11, 16)}` : ''}
                </span>
              </div>
            ) : null}
          </div>

          {/* Walking Buddies */}
          {data.thread?.pets && data.thread.pets.length > 0 ? (
            <div className="space-y-2">
              <Typography variant="label" className="text-zinc-400 font-black uppercase tracking-widest text-[9px]">
                <span className="flex items-center gap-1"><PawPrint size={10} /> Walking Buddies</span>
              </Typography>
              <div className="flex gap-3 overflow-x-auto no-scrollbar py-1">
                {data.thread.pets.map((pet) => (
                  <div key={pet.id} className="flex flex-col items-center gap-1 shrink-0">
                    <div className="w-12 h-12 rounded-full overflow-hidden border-2 border-amber-200 shadow-sm bg-zinc-100">
                      {pet.photoUrl ? (
                        <img src={pet.photoUrl} alt={pet.name} className="w-full h-full object-cover" />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center text-zinc-300">
                          <PawPrint size={20} />
                        </div>
                      )}
                    </div>
                    <span className="text-[10px] font-bold text-navy-900 text-center leading-tight">{pet.name}</span>
                    {pet.breedName ? (
                      <span className="text-[8px] text-zinc-400 text-center leading-tight">{pet.breedName}</span>
                    ) : null}
                  </div>
                ))}
              </div>
            </div>
          ) : null}

          {/* Location */}
          {data.thread?.placeName ? (
            <div className="space-y-2">
              <Typography variant="label" className="text-zinc-400 font-black uppercase tracking-widest text-[9px]">
                <span className="flex items-center gap-1"><MapPin size={10} /> Location</span>
              </Typography>
              <Typography variant="body" className="text-sm text-navy-900 font-serif font-semibold">
                {data.thread.placeName}
              </Typography>
              {data.thread.address ? (
                <Typography variant="body" className="text-xs text-zinc-400 font-serif">
                  {data.thread.address}
                </Typography>
              ) : null}
            </div>
          ) : null}

          {/* Kakao Static Map */}
          {data.thread?.latitude && data.thread?.longitude ? (
            <KakaoStaticMap
              latitude={data.thread.latitude}
              longitude={data.thread.longitude}
              height={180}
            />
          ) : null}
        </div>
      ) : (
        <div className="space-y-8">
          <div className="flex justify-between items-center">
            <header className="space-y-1">
              <Typography variant="label" className="text-amber-600 font-black uppercase tracking-widest text-[10px]">Diary Entry</Typography>
              <div className="flex items-center gap-3">
                <Typography variant="h3" className="text-lg md:text-xl text-navy-900 font-serif">{data.walkDate}</Typography>
                {isCurrent && !isReadOnly ? (
                  <button
                    onClick={() => {
                      if (editMode !== 'NONE') {
                        setDiaryForm(prev => ({ ...prev, isPublic: !prev.isPublic }));
                      } else if (onToggleVisibility) {
                        onToggleVisibility(data.id, !data.isPublic);
                      }
                    }}
                    className="transition-opacity hover:opacity-70"
                  >
                    <Badge variant="emerald" className={cn("text-[8px] px-2 py-0 cursor-pointer", (editMode !== 'NONE' ? diaryForm.isPublic : !!data.isPublic) ? "bg-emerald-50 text-emerald-600" : "bg-zinc-100 text-zinc-400")}>
                      {(editMode !== 'NONE' ? diaryForm.isPublic : !!data.isPublic) ? 'PUBLIC' : 'PRIVATE'}
                    </Badge>
                  </button>
                ) : (
                  <Badge variant="emerald" className={cn("text-[8px] px-2 py-0", !!data.isPublic ? "bg-emerald-50 text-emerald-600" : "bg-zinc-100 text-zinc-400")}>
                    {!!data.isPublic ? 'PUBLIC' : 'PRIVATE'}
                  </Badge>
                )}
              </div>
            </header>
            {isCurrent && <button onClick={onClose} className="p-2 text-zinc-300 hover:text-navy-900 transition-all hover:rotate-90"><X size={28} /></button>}
          </div>

          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <Typography variant="label" className="text-zinc-400 font-black uppercase tracking-widest text-[9px]">Captured Moments</Typography>
              {isCurrent && !isReadOnly && editMode === 'NONE' ? (
                <button onClick={() => setEditMode('PHOTOS')} className="text-[10px] font-black text-amber-600 flex items-center gap-1 hover:underline">
                  <ImageIcon size={12} /> 사진 관리
                </button>
              ) : null}
            </div>
            <div className="flex flex-wrap gap-4 py-2">
              {(isCurrent && editMode === 'PHOTOS' ? diaryForm.photoUrls : (data.photoUrls || [])).map((photo: string, i: number) => (
                <div key={i} className="relative group">
                  <div className={cn("w-28 h-28 md:w-36 md:h-36 bg-white p-1 shadow-md border border-zinc-200/50 transition-all cursor-zoom-in", i % 2 === 0 ? "rotate-[-2deg]" : "rotate-[2deg]", "hover:rotate-0")} onClick={() => onZoom(photo)}>
                    <img src={photo} className="w-full h-full object-cover" alt="Moment" />
                    {isCurrent && !isReadOnly && editMode === 'PHOTOS' ? (
                      <button onClick={(e) => { e.stopPropagation(); setDiaryForm(prev => ({...prev, photoUrls: prev.photoUrls.filter((_, idx: number) => idx !== i)})); }} className="absolute -top-2 -right-2 w-6 h-6 bg-red-500 text-white rounded-full flex items-center justify-center shadow-lg"><X size={14} /></button>
                    ) : null}
                  </div>
                </div>
              ))}
              {isCurrent && !isReadOnly && editMode === 'PHOTOS' ? (
                <button onClick={() => fileInputRef.current?.click()} className="w-28 h-28 md:w-36 md:h-36 rounded-sm border-2 border-dashed border-zinc-200 bg-zinc-50/50 flex flex-col items-center justify-center text-zinc-300 hover:border-amber-500 hover:text-amber-500 transition-all"><Plus size={24} /><span className="text-[8px] font-black mt-1">ADD PHOTO</span></button>
              ) : null}
            </div>
            {isCurrent && !isReadOnly && editMode === 'PHOTOS' ? (
              <div className="flex gap-2 pt-2">
                <Button variant="primary" size="sm" className="bg-navy-900 rounded-xl px-6" onClick={() => onSave({ title: localTitle, content: localContent })}>사진 저장</Button>
                <Button variant="outline" size="sm" className="rounded-xl" onClick={() => { setDiaryForm(prev => ({ ...prev, photoUrls: data.photoUrls || [] })); setEditMode('NONE'); }}>취소</Button>
              </div>
            ) : null}
          </div>

          <div className="space-y-6 pt-6 border-t border-zinc-100">
            <div className="flex items-center justify-between">
              <Typography variant="label" className="text-zinc-400 font-black uppercase tracking-widest text-[9px]">Memories</Typography>
              {isCurrent && !isReadOnly && editMode === 'NONE' ? (
                <button onClick={() => setEditMode('CONTENT')} className="text-amber-600 font-black text-[10px] flex items-center hover:underline">
                  <AlignLeft size={12} className="mr-1" /> 글 수정하기
                </button>
              ) : null}
            </div>
            {isCurrent && !isReadOnly && editMode === 'CONTENT' ? (
              <div className="space-y-4 animate-in slide-in-from-top-2">
                <div>
                  <Typography variant="label" className="text-zinc-400 font-black uppercase tracking-widest text-[9px] mb-1">Title</Typography>
                  <input
                    type="text"
                    value={localTitle}
                    onChange={(e) => setLocalTitle(e.target.value)}
                    className="w-full bg-white/80 border-2 border-amber-100 rounded-2xl px-4 py-2 text-sm text-navy-900 font-serif italic focus:outline-none"
                  />
                </div>
                <div className="relative">
                  <textarea className="w-full bg-white/80 border-2 border-amber-100 rounded-[32px] p-6 text-sm font-serif min-h-[200px]" value={localContent} maxLength={300} onChange={(e) => setLocalContent(e.target.value)} />
                  <span className="absolute bottom-3 right-6 text-[10px] text-zinc-400">{localContent.length}/300</span>
                </div>
                <div className="flex justify-end gap-2">
                  <Button variant="primary" size="sm" className="rounded-xl px-6 bg-navy-900" onClick={() => onSave({ title: localTitle, content: localContent })}>저장하기</Button>
                  <Button variant="outline" size="sm" className="rounded-xl" onClick={() => { setLocalTitle(data.title || ''); setLocalContent(data.content || ''); setDiaryForm(prev => ({ ...prev, title: data.title || '', content: data.content || '', isPublic: !!data.isPublic })); setEditMode('NONE'); }}>취소</Button>
                </div>
              </div>
            ) : (
              <div className="relative min-h-[150px]">
                <div className="absolute inset-0 bg-[linear-gradient(transparent_31px,#eee_32px)] bg-[length:100%_32px] pointer-events-none opacity-50" />
                <Typography variant="body" className="text-zinc-700 font-serif text-base leading-[32px] pt-1 italic relative z-10 px-2">
                  {(isCurrent && editMode === 'CONTENT' ? localContent : (data.content || '')) || '아직 작성된 이야기가 없습니다.'}
                </Typography>
              </div>
            )}
          </div>
        </div>
      )}
      <div className={cn("absolute bottom-6 text-[10px] font-black text-zinc-300 pointer-events-none uppercase tracking-widest", side === 'LEFT' ? "left-10" : "right-10")}>
        Page {side === 'LEFT' ? '01' : '02'}
      </div>
    </div>
  );
};
