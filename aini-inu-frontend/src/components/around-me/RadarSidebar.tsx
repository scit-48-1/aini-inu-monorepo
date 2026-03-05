'use client';

import React, { useState } from 'react';
import { MapPin, Zap, Footprints, ArrowRight, Users, Loader2, Pencil, Trash2 } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import { Badge } from '@/components/ui/Badge';
import { MannerScoreGauge } from '@/components/common/MannerScoreGauge';
import { ThreadType } from '@/types';

interface RadarSidebarProps {
  isLoading: boolean;
  visibleMarkers: ThreadType[];
  sortBy: 'DISTANCE' | 'TIME';
  setSortBy: (sort: 'DISTANCE' | 'TIME') => void;
  onCardClick: (t: ThreadType) => void;
  getRemainingTime: (time?: string) => string;
  currentUserId?: string;
  onDeleteThread?: (id: string | number) => void;
  onEditThread?: (t: ThreadType) => void;
}

export const RadarSidebar: React.FC<RadarSidebarProps> = ({
  isLoading,
  visibleMarkers,
  sortBy,
  setSortBy,
  onCardClick,
  getRemainingTime,
  currentUserId,
  onDeleteThread,
  onEditThread,
}) => {
  const [deleteConfirmId, setDeleteConfirmId] = useState<string | number | null>(null);

  return (
    <div className="flex-1 flex flex-col gap-8 overflow-y-auto no-scrollbar min-w-[350px]">
      <div className="flex items-center justify-between px-4">
        <div className="space-y-1">
          <Typography variant="h3" className="text-3xl font-black">동네 산책 <span className="text-amber-500 italic font-serif">스레드</span></Typography>
          <Typography variant="body" className="text-xs text-zinc-400 font-bold">지금 우리 동네에서 이웃들이 메이트를 기다리고 있어요.</Typography>
        </div>
        <div className="flex bg-zinc-50 p-1 rounded-xl border border-zinc-100">
          <button 
            onClick={() => setSortBy('DISTANCE')} 
            className={cn("px-3 py-1.5 rounded-lg text-[10px] font-black transition-all", sortBy === 'DISTANCE' ? "bg-white text-navy-900 shadow-sm" : "text-zinc-300")}
          >거리순</button>
          <button 
            onClick={() => setSortBy('TIME')} 
            className={cn("px-3 py-1.5 rounded-lg text-[10px] font-black transition-all", sortBy === 'TIME' ? "bg-white text-navy-900 shadow-sm" : "text-zinc-300")}
          >시간순</button>
        </div>
      </div>
      
      <div className="space-y-6 pb-24 px-1">
        {isLoading ? (
          <div className="p-20 text-center opacity-20"><Loader2 className="animate-spin mx-auto" size={40} /></div>
        ) : (
          visibleMarkers.map((t) => {
            const isMyThread = currentUserId && t.author?.id === currentUserId;
            return (
            <Card key={t.id} interactive className="relative p-8 bg-white border-card-border shadow-xl rounded-[48px]" onClick={() => onCardClick(t)}>
              {/* 삭제 확인 오버레이 */}
              {deleteConfirmId === t.id && (
                <div
                  className="absolute inset-0 bg-white/95 backdrop-blur-sm rounded-[48px] flex flex-col items-center justify-center gap-5 z-10 p-8"
                  onClick={(e) => e.stopPropagation()}
                >
                  <Typography variant="body" className="text-sm font-black text-navy-900 text-center">이 모집글을 삭제할까요?</Typography>
                  <div className="flex gap-3">
                    <button
                      onClick={() => setDeleteConfirmId(null)}
                      className="px-5 py-2.5 rounded-2xl bg-zinc-100 text-zinc-500 text-xs font-black hover:bg-zinc-200 transition-colors"
                    >취소</button>
                    <button
                      onClick={() => { onDeleteThread?.(t.id); setDeleteConfirmId(null); }}
                      className="px-5 py-2.5 rounded-2xl bg-red-500 text-white text-xs font-black hover:bg-red-600 transition-colors"
                    >삭제</button>
                  </div>
                </div>
              )}

              <div className="flex justify-between items-start mb-8">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 bg-zinc-100 rounded-2xl flex items-center justify-center overflow-hidden border border-white">
                    <img src={t.author?.avatar || '/AINIINU_ROGO_B.png'} className="w-full h-full object-cover" alt="Avatar" />
                  </div>
                  <div>
                    <Typography variant="body" className="text-sm font-black text-navy-900 mb-1">{t.owner || t.author?.nickname}</Typography>
                    <div className="flex items-center gap-2 text-[10px] text-zinc-400 font-bold whitespace-nowrap">
                      <div className="flex items-center gap-1 min-w-0 overflow-hidden">
                        <MapPin size={10} className="text-amber-500 shrink-0" />
                        <span className="truncate">{t.place || t.location}</span>
                      </div>
                      <span className="shrink-0 text-amber-500/40">|</span>
                      <span className="shrink-0 text-amber-600 font-black">
                        {(t.distance ?? 0) < 1 ? `${Math.round((t.distance ?? 0) * 1000)}m` : `${(t.distance ?? 0).toFixed(1)}km`}
                      </span>
                    </div>
                  </div>
                </div>
                <div className="flex flex-col items-end gap-2">
                  <Badge variant="amber" className="bg-amber-50 text-amber-600 border-none px-3 py-1 text-[10px] flex items-center gap-1"><Zap size={10} fill="currentColor" /> {getRemainingTime(t.time)}</Badge>
                  {isMyThread && (
                    <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
                      <button
                        onClick={() => onEditThread?.(t)}
                        className="p-2 rounded-xl text-zinc-300 hover:text-amber-500 hover:bg-amber-50 transition-all"
                        title="수정하기"
                      >
                        <Pencil size={13} />
                      </button>
                      <button
                        onClick={() => setDeleteConfirmId(t.id)}
                        className="p-2 rounded-xl text-zinc-300 hover:text-red-500 hover:bg-red-50 transition-all"
                        title="삭제하기"
                      >
                        <Trash2 size={13} />
                      </button>
                    </div>
                  )}
                </div>
              </div>
              <div className="flex gap-6 mb-8">
                <div className="relative">
                  <img src={t.thumbnail || t.image} className="w-28 h-28 rounded-[40px] object-cover border-4 border-white shadow-2xl" alt="Pet" />
                  <div className="absolute -bottom-2 -right-2 w-10 h-10 bg-white rounded-full flex items-center justify-center shadow-lg text-amber-500 border-2 border-amber-50">
                    <Footprints size={18} />
                  </div>
                </div>
                <div className="flex-1 space-y-3 pt-2">
                  <Typography variant="h3" className="text-2xl font-black text-navy-900 line-clamp-1">{t.title || `${t.name}와 산책`}</Typography>
                  <Typography variant="body" className="text-base text-zinc-400 line-clamp-2 leading-relaxed">&quot;{t.content || t.description}&quot;</Typography>
                </div>
              </div>
              <div className="flex items-center justify-between pt-8 border-t border-zinc-50">
                <div className="flex items-center gap-6">
                  <MannerScoreGauge score={t.author?.mannerScore || 5} />
                  <div className="flex items-center gap-2 text-xs font-black text-zinc-300 uppercase tracking-widest">
                    <Users size={14} /> {t.participatingDogs?.length || 1} Partners
                  </div>
                </div>
                <div className="w-12 h-12 bg-navy-900 text-white rounded-2xl flex items-center justify-center shadow-xl"><ArrowRight size={20} /></div>
              </div>
            </Card>
            );
          })
        )}
      </div>
    </div>
  );
};
