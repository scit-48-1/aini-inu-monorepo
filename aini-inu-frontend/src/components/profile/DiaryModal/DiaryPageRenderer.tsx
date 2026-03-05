'use client';

import React, { useRef } from 'react';
import { X, MapPin, Image as ImageIcon, AlignLeft, Plus, Camera } from 'lucide-react';
import { Typography } from '@/components/ui/Typography';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { cn } from '@/lib/utils';
import dynamic from 'next/dynamic';
import { WalkDiaryType, DiaryFormValues } from '@/types';

const DynamicMap = dynamic(() => import('@/components/common/DynamicMap'), { ssr: false });

interface DiaryPageRendererProps {
  data: WalkDiaryType;
  side: 'LEFT' | 'RIGHT';
  isCurrent: boolean;
  isReadOnly: boolean; // 추가된 엔진 권한 플래그
  editMode: 'NONE' | 'CONTENT' | 'PHOTOS';
  diaryForm: DiaryFormValues;
  setDiaryForm: (form: DiaryFormValues) => void;
  onClose: () => void;
  onZoom: (photo: string) => void;
  setEditMode: (mode: 'NONE' | 'CONTENT' | 'PHOTOS') => void;
  onSave: () => void;
  onImageUpload: (base64: string) => void;
}

export const DiaryPageRenderer: React.FC<DiaryPageRendererProps> = ({
  data, side, isCurrent, isReadOnly, editMode, diaryForm, setDiaryForm, onClose, onZoom, setEditMode, onSave, onImageUpload
}) => {
  const fileInputRef = useRef<HTMLInputElement>(null);
  if (!data) return null;

  // 참여 강아지 정렬 로직 (내 강아지 우선)
  const participatingDogs = data.participatingDogs || [];
  
  return (
    <div className="flex-1 overflow-y-auto p-8 md:p-12 no-scrollbar flex flex-col h-full bg-[#fdfbf7] relative border-none">
      <input type="file" ref={fileInputRef} className="hidden" accept="image/*" multiple onChange={(e) => {
        const files = Array.from(e.target.files || []);
        files.forEach(file => {
          const reader = new FileReader();
          reader.onloadend = () => onImageUpload(reader.result as string);
          reader.readAsDataURL(file);
        });
      }} />

      {side === 'LEFT' ? (
        <div className="space-y-8">
          <header className="space-y-2">
            <Typography variant="label" className="text-amber-600 font-black uppercase tracking-[0.3em] text-[10px]">Walk Record</Typography>
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
          
          <div className="space-y-4">
            <Typography variant="label" className="text-zinc-400 font-black uppercase tracking-widest text-[9px]">Location</Typography>
            <div className="h-32 md:h-48 w-full rounded-[24px] md:rounded-[32px] overflow-hidden border-4 border-white shadow-xl bg-zinc-50 relative">
              <DynamicMap 
                center={[data.lat || 37.5445, data.lng || 127.0445]} 
                zoom={15} 
                markers={[{ id: data.id, lat: data.lat || 37.5445, lng: data.lng || 127.0445, image: data.image || data.photos?.[0], isEmergency: false }]} 
                hideCircle={true} interactive={false} 
                onMarkerClick={() => {}}
              />
            </div>
            <div className="flex items-center gap-2 text-zinc-500 font-bold">
              <MapPin size={14} className="text-amber-500" />
              <Typography variant="body" className="text-[10px] md:text-xs">{data.place}</Typography>
            </div>
          </div>
          
          <div className="space-y-6 pt-2">
            <div className="space-y-3">
              <Typography variant="label" className="text-zinc-400 font-black uppercase tracking-widest text-[8px]">Participating Dogs</Typography>
              <div className="flex flex-row flex-wrap gap-3">
                {participatingDogs.map((dog, idx: number) => (
                  <div key={idx} className="flex flex-col items-center gap-1.5 group">
                    <div className="w-12 h-12 md:w-14 md:h-14 rounded-full p-0.5 shadow-md transition-all duration-500 group-hover:scale-110 bg-zinc-200">
                      <div className="w-full h-full rounded-full border-2 border-white overflow-hidden bg-white">
                        <img src={dog.image} className="w-full h-full object-cover" alt={dog.name} />
                      </div>
                    </div>
                    <span className="text-[9px] font-black leading-none text-navy-900">{dog.name}</span>
                  </div>
                ))}
              </div>
            </div>
              
            <div className="space-y-3 pt-1">
              <Typography variant="label" className="text-zinc-400 font-black uppercase tracking-widest text-[8px]">With Neighbors</Typography>
              <div className="flex flex-row flex-wrap gap-3">
                {(isCurrent && editMode !== 'NONE' ? (diaryForm.tags || []) : (data.tags || [])).map((person, idx: number) => (
                  <div key={idx} className="flex items-center gap-2 bg-zinc-50/50 py-1.5 px-3 rounded-full border border-zinc-100 group hover:bg-white transition-all relative">
                    <div className="w-6 h-6 rounded-full overflow-hidden border border-white shadow-sm">
                      <img src={person.avatar} className="w-full h-full object-cover" alt="Avatar" />
                    </div>
                    <span className="text-[10px] font-bold text-zinc-400 group-hover:text-navy-900 transition-colors">@{person.nickname}</span>
                    {isCurrent && !isReadOnly && editMode !== 'NONE' && (
                      <button 
                        onClick={() => setDiaryForm({...diaryForm, tags: diaryForm.tags.filter((t) => t.id !== person.id)})}
                        className="ml-1 text-zinc-300 hover:text-red-500 transition-colors"
                      >
                        <X size={10} />
                      </button>
                    )}
                  </div>
                ))}
                {((isCurrent && editMode !== 'NONE' ? (diaryForm.tags || []) : (data.tags || [])).length === 0) && (
                  <span className="text-[10px] text-zinc-300 italic">함께한 이웃이 없습니다.</span>
                )}
              </div>
            </div>
          </div>
        </div>
      ) : (
        <div className="space-y-8">
          <div className="flex justify-between items-center">
            <header className="space-y-1">
              <Typography variant="label" className="text-amber-600 font-black uppercase tracking-widest text-[10px]">Diary Entry</Typography>
              <div className="flex items-center gap-3">
                <Typography variant="h3" className="text-lg md:text-xl text-navy-900 font-serif">{data.walkDate}</Typography>
                {isCurrent && !isReadOnly && editMode !== 'NONE' ? (
                  <button
                    onClick={() => setDiaryForm({ ...diaryForm, isPublic: !diaryForm.isPublic })}
                    className="transition-opacity hover:opacity-70"
                  >
                    <Badge variant="emerald" className={cn("text-[8px] px-2 py-0 cursor-pointer", diaryForm.isPublic ? "bg-emerald-50 text-emerald-600" : "bg-zinc-100 text-zinc-400")}>
                      {diaryForm.isPublic ? 'PUBLIC' : 'PRIVATE'}
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
              {isCurrent && !isReadOnly && editMode === 'NONE' && (
                <button onClick={() => setEditMode('PHOTOS')} className="text-[10px] font-black text-amber-600 flex items-center gap-1 hover:underline">
                  <ImageIcon size={12} /> 사진 관리
                </button>
              )}
            </div>
            <div className="flex flex-wrap gap-4 py-2">
              {(isCurrent && editMode === 'PHOTOS' ? diaryForm.photos : (data.photos || [])).map((photo: string, i: number) => (
                <div key={i} className="relative group">
                  <div className={cn("w-28 h-28 md:w-36 md:h-36 bg-white p-1 shadow-md border border-zinc-200/50 transition-all cursor-zoom-in", i % 2 === 0 ? "rotate-[-2deg]" : "rotate-[2deg]", "hover:rotate-0")} onClick={() => onZoom(photo)}>
                    <img src={photo} className="w-full h-full object-cover" alt="Moment" />
                    {isCurrent && !isReadOnly && editMode === 'PHOTOS' && (
                      <button onClick={(e) => { e.stopPropagation(); setDiaryForm({...diaryForm, photos: diaryForm.photos.filter((_, idx: number) => idx !== i)}); }} className="absolute -top-2 -right-2 w-6 h-6 bg-red-500 text-white rounded-full flex items-center justify-center shadow-lg"><X size={14} /></button>
                    )}
                  </div>
                </div>
              ))}
              {isCurrent && !isReadOnly && editMode === 'PHOTOS' && (
                <button onClick={() => fileInputRef.current?.click()} className="w-28 h-28 md:w-36 md:h-36 rounded-sm border-2 border-dashed border-zinc-200 bg-zinc-50/50 flex flex-col items-center justify-center text-zinc-300 hover:border-amber-500 hover:text-amber-500 transition-all"><Plus size={24} /><span className="text-[8px] font-black mt-1">ADD PHOTO</span></button>
              )}
            </div>
            {isCurrent && !isReadOnly && editMode === 'PHOTOS' && (
              <div className="flex gap-2 pt-2">
                <Button variant="primary" size="sm" className="bg-navy-900 rounded-xl px-6" onClick={onSave}>사진 저장</Button>
                <Button variant="outline" size="sm" className="rounded-xl" onClick={() => setEditMode('NONE')}>취소</Button>
              </div>
            )}
          </div>

          <div className="space-y-6 pt-6 border-t border-zinc-100">
            <div className="flex items-center justify-between">
              <Typography variant="label" className="text-zinc-400 font-black uppercase tracking-widest text-[9px]">Memories</Typography>
              {isCurrent && !isReadOnly && editMode === 'NONE' && (
                <button onClick={() => setEditMode('CONTENT')} className="text-amber-600 font-black text-[10px] flex items-center hover:underline">
                  <AlignLeft size={12} className="mr-1" /> 글 수정하기
                </button>
              )}
            </div>
            {isCurrent && !isReadOnly && editMode === 'CONTENT' ? (
              <div className="space-y-4 animate-in slide-in-from-top-2">
                <textarea className="w-full bg-white/80 border-2 border-amber-100 rounded-[32px] p-6 text-sm font-serif min-h-[200px]" value={diaryForm.content} onChange={(e) => setDiaryForm({...diaryForm, content: e.target.value})} />
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
