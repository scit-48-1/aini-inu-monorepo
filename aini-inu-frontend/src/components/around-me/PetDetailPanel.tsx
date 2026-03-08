'use client';

import React from 'react';
import { X, Zap, Target, Check, Brain } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Typography } from '@/components/ui/Typography';
import type { PetSummary } from '@/api/threads';

interface PetDetailPanelProps {
  isOpen: boolean;
  onClose: () => void;
  pet: PetSummary | null;
}

export const PetDetailPanel: React.FC<PetDetailPanelProps> = ({
  isOpen,
  onClose,
  pet,
}) => {
  if (!pet) return null;

  const photoUrl = pet.photoUrl || '/AINIINU_ROGO_B.png';

  return (
    <div
      className={cn(
        'absolute top-0 right-0 h-full w-[340px] z-[1100] transition-transform duration-500 ease-out',
        isOpen ? 'translate-x-0' : 'translate-x-full',
      )}
    >
      {/* Backdrop click area */}
      <div
        className="absolute inset-0 -left-[200%] z-0"
        onClick={onClose}
      />

      {/* Panel */}
      <div className="relative h-full bg-white rounded-l-[48px] shadow-2xl border-l border-zinc-100 flex flex-col overflow-hidden z-10">
        {/* Hero Image */}
        <div className="h-56 relative shrink-0">
          <img
            src={photoUrl}
            className="w-full h-full object-cover"
            alt={pet.name}
          />
          <button
            onClick={onClose}
            className="absolute top-4 right-4 w-10 h-10 bg-black/20 hover:bg-black/40 backdrop-blur-md text-white rounded-full flex items-center justify-center transition-all active:scale-95"
          >
            <X size={20} />
          </button>
          <div className="absolute bottom-0 left-0 right-0 p-6 bg-gradient-to-t from-black/90 via-black/40 to-transparent text-white pointer-events-none">
            <Typography variant="h3" className="text-white text-2xl leading-none mb-1">
              {pet.name}
            </Typography>
            <Typography variant="body" className="text-white/80 font-black text-sm">
              {pet.breedName || '견종 미등록'} &bull; {pet.age}세 &bull; {pet.gender === 'MALE' ? '남아' : '여아'}
            </Typography>
          </div>
        </div>

        {/* Detail Content */}
        <div className="flex-1 overflow-y-auto p-6 space-y-5 no-scrollbar">
          {/* Personalities */}
          {pet.personalities.length > 0 && (
            <div className="space-y-3">
              <Typography variant="label" className="text-amber-500 flex items-center gap-2 font-black uppercase tracking-[0.2em] text-[10px]">
                <Zap size={12} /> Character
              </Typography>
              <div className="flex flex-wrap gap-2">
                {pet.personalities.map((p) => (
                  <div key={p} className="bg-zinc-50 border border-zinc-100 text-zinc-600 px-3 py-1.5 rounded-xl text-[11px] font-black">
                    #{p}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Walk Style */}
          {pet.walkingStyles.length > 0 && (
            <div className="space-y-3 p-4 bg-zinc-50 rounded-[24px] border border-zinc-100">
              <Typography variant="label" className="text-indigo-500 flex items-center gap-2 font-black uppercase tracking-widest text-[9px]">
                <Target size={12} /> Walk Style
              </Typography>
              <Typography variant="body" className="text-navy-900 font-black text-sm">
                {pet.walkingStyles.join(', ')}
              </Typography>
            </div>
          )}

          {/* Neutered + Size */}
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-2 p-4 bg-zinc-50 rounded-[24px] border border-zinc-100">
              <Typography variant="label" className="text-emerald-500 flex items-center gap-2 font-black uppercase tracking-widest text-[9px]">
                <Check size={12} /> Neutered
              </Typography>
              <Typography variant="body" className="text-navy-900 font-black text-base">
                {pet.isNeutered ? '완료' : '미완료'}
              </Typography>
            </div>
            <div className="space-y-2 p-4 bg-zinc-50 rounded-[24px] border border-zinc-100">
              <Typography variant="label" className="text-zinc-400 font-black uppercase tracking-widest text-[9px]">
                Size
              </Typography>
              <Typography variant="body" className="text-navy-900 font-black text-base">
                {pet.size === 'SMALL' ? '소형' : pet.size === 'MEDIUM' ? '중형' : pet.size === 'LARGE' ? '대형' : pet.size}
              </Typography>
            </div>
          </div>

          {/* MBTI */}
          {pet.mbti && (
            <div className="space-y-2 p-4 bg-zinc-50 rounded-[24px] border border-zinc-100">
              <Typography variant="label" className="text-purple-500 flex items-center gap-2 font-black uppercase tracking-widest text-[9px]">
                <Brain size={12} /> MBTI
              </Typography>
              <Typography variant="body" className="text-navy-900 font-black text-lg">
                {pet.mbti}
              </Typography>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
