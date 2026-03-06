'use client';

import React, { useRef } from 'react';
import { X, Image as ImageIcon, AlignLeft, Plus, Trash2 } from 'lucide-react';
import { Typography } from '@/components/ui/Typography';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { cn } from '@/lib/utils';
import { UserAvatar } from '@/components/common/UserAvatar';
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
  setDiaryForm: (form: DiaryFormValues) => void;
  onClose: () => void;
  onZoom: (photo: string) => void;
  setEditMode: (mode: 'NONE' | 'CONTENT' | 'PHOTOS') => void;
  onSave: () => void;
  onImageUpload: (imageUrl: string) => void;
  onDelete?: () => void;
  storyHeader?: StoryHeader;
  onToggleVisibility?: (diaryId: number, newIsPublic: boolean) => void;
}

export const DiaryPageRenderer: React.FC<DiaryPageRendererProps> = ({
  data, side, isCurrent, isReadOnly, editMode, diaryForm, setDiaryForm, onClose, onZoom, setEditMode, onSave, onImageUpload, onDelete, storyHeader, onToggleVisibility
}) => {
  const fileInputRef = useRef<HTMLInputElement>(null);
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
        <div className="space-y-8">
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
            {isCurrent && !isReadOnly && editMode === 'CONTENT' ? (
              <input
                type="text"
                value={diaryForm.title}
                onChange={(e) => setDiaryForm({ ...diaryForm, title: e.target.value })}
                className="w-full bg-white border-2 border-amber-100 rounded-2xl px-4 py-2 text-lg text-navy-900 font-serif italic focus:outline-none"
              />
            ) : (
              <Typography variant="h2" className="text-navy-900 font-serif lowercase italic leading-tight text-2xl md:text-3xl">
                {isCurrent && editMode !== 'NONE' ? diaryForm.title : (data.title || '즐거운 산책')}
              </Typography>
            )}
            <div className="h-1 w-16 bg-amber-500 rounded-full" />
          </header>

          {/* Walk date */}
          <div className="space-y-2">
            <Typography variant="label" className="text-zinc-400 font-black uppercase tracking-widest text-[9px]">Walk Date</Typography>
            <Typography variant="body" className="text-sm text-zinc-600 font-serif">{data.walkDate}</Typography>
          </div>

          {/* Content preview */}
          <div className="space-y-2">
            <Typography variant="label" className="text-zinc-400 font-black uppercase tracking-widest text-[9px]">Story</Typography>
            <Typography variant="body" className="text-zinc-700 font-serif text-sm leading-relaxed italic line-clamp-4">
              {data.content || '아직 작성된 이야기가 없습니다.'}
            </Typography>
          </div>

          {/* Photo gallery */}
          {data.photoUrls && data.photoUrls.length > 0 ? (
            <div className="space-y-3">
              <Typography variant="label" className="text-zinc-400 font-black uppercase tracking-widest text-[9px]">Photos</Typography>
              <div className="flex flex-wrap gap-3">
                {data.photoUrls.map((photo: string, i: number) => (
                  <div
                    key={i}
                    className={cn(
                      "w-20 h-20 md:w-24 md:h-24 bg-white p-0.5 shadow-md border border-zinc-200/50 cursor-zoom-in transition-all hover:scale-105",
                      i % 2 === 0 ? "rotate-[-1deg]" : "rotate-[1deg]"
                    )}
                    onClick={() => onZoom(photo)}
                  >
                    <img src={photo} className="w-full h-full object-cover" alt="Photo" />
                  </div>
                ))}
              </div>
            </div>
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
                        setDiaryForm({ ...diaryForm, isPublic: !diaryForm.isPublic });
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
                      <button onClick={(e) => { e.stopPropagation(); setDiaryForm({...diaryForm, photoUrls: diaryForm.photoUrls.filter((_, idx: number) => idx !== i)}); }} className="absolute -top-2 -right-2 w-6 h-6 bg-red-500 text-white rounded-full flex items-center justify-center shadow-lg"><X size={14} /></button>
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
                <Button variant="primary" size="sm" className="bg-navy-900 rounded-xl px-6" onClick={onSave}>사진 저장</Button>
                <Button variant="outline" size="sm" className="rounded-xl" onClick={() => setEditMode('NONE')}>취소</Button>
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
                <div className="relative">
                  <textarea className="w-full bg-white/80 border-2 border-amber-100 rounded-[32px] p-6 text-sm font-serif min-h-[200px]" value={diaryForm.content} maxLength={300} onChange={(e) => setDiaryForm({...diaryForm, content: e.target.value})} />
                  <span className="absolute bottom-3 right-6 text-[10px] text-zinc-400">{diaryForm.content.length}/300</span>
                </div>
                <div className="flex justify-end gap-2">
                  <Button variant="primary" size="sm" className="rounded-xl px-6 bg-navy-900" onClick={onSave}>저장하기</Button>
                  <Button variant="outline" size="sm" className="rounded-xl" onClick={() => setEditMode('NONE')}>취소</Button>
                </div>
              </div>
            ) : (
              <div className="relative min-h-[150px]">
                <div className="absolute inset-0 bg-[linear-gradient(transparent_31px,#eee_32px)] bg-[length:100%_32px] pointer-events-none opacity-50" />
                <Typography variant="body" className="text-zinc-700 font-serif text-base leading-[32px] pt-1 italic relative z-10 px-2">
                  {(isCurrent && editMode === 'CONTENT' ? diaryForm.content : (data.content || '')) || '아직 작성된 이야기가 없습니다.'}
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
