'use client';

import React from 'react';
import { createPortal } from 'react-dom';
import { X, ShieldCheck, Edit2, Zap, Target, Check } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import { DogType } from '@/types';

interface DogDetailModalProps {
  isOpen: boolean;
  onClose: () => void;
  dog: DogType | null;
  onEdit: () => void;
  onZoom: (img: string) => void;
}

export const DogDetailModal: React.FC<DogDetailModalProps> = ({ isOpen, onClose, dog, onEdit, onZoom }) => {
  if (!isOpen || !dog) return null;

  return createPortal(
    <div className="fixed inset-0 z-[3000] bg-black/60 backdrop-blur-sm flex items-center justify-center p-6 animate-in fade-in duration-300" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <Card className="w-full max-w-lg overflow-hidden bg-white shadow-2xl animate-in zoom-in-95 duration-500 border-none rounded-[56px] flex flex-col max-h-[90vh]">
        <div className="h-80 relative shrink-0 group cursor-zoom-in" onClick={() => onZoom(dog.image)}>
          <img src={dog.image} className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105" alt={dog.name} />
          <button onClick={(e) => { e.stopPropagation(); onClose(); }} className="absolute top-6 right-6 w-12 h-12 bg-black/20 hover:bg-black/40 backdrop-blur-md text-white rounded-full flex items-center justify-center transition-all active:scale-95"><X size={24} /></button>
          <button onClick={(e) => { e.stopPropagation(); onEdit(); }} className="absolute top-6 left-6 w-8 h-8 bg-black/15 hover:bg-black/35 backdrop-blur-md text-white/60 hover:text-white rounded-full flex items-center justify-center transition-all active:scale-95"><Edit2 size={14} /></button>
          <div className="absolute bottom-0 left-0 right-0 p-10 bg-gradient-to-t from-black/90 via-black/40 to-transparent text-white pointer-events-none">
            <div className="flex items-center gap-3 mb-2">
              <Typography variant="h2" className="text-white text-4xl leading-none">{dog.name}</Typography>
              {dog.registrationNumber && <div className="bg-blue-500 text-white p-1.5 rounded-full shadow-lg border-2 border-white/20"><ShieldCheck size={16} fill="currentColor" strokeWidth={3} /></div>}
            </div>
            <Typography variant="body" className="text-white/80 font-black text-lg">{dog.breed} • {dog.age}세 • {dog.gender === 'M' ? '남아' : '여아'}</Typography>
          </div>
        </div>
        <div className="flex-1 overflow-y-auto p-10 space-y-10 no-scrollbar">
          <div className="space-y-4">
            <Typography variant="label" className="text-amber-500 flex items-center gap-2 font-black uppercase tracking-[0.2em] text-[10px]"><Zap size={14} /> Character & Tendencies</Typography>
            <div className="flex flex-wrap gap-2.5">
              {(dog.tendencies || []).map((t: any, i: number) => (
                <div key={i} className="bg-zinc-50 border border-zinc-100 text-zinc-600 px-4 py-2 rounded-2xl text-xs font-black shadow-sm">
                  #{typeof t === 'string' ? t : t.ko}
                </div>
              ))}
            </div>
          </div>
          <div className="grid grid-cols-2 gap-6 pt-2">
            <div className="space-y-4 p-6 bg-zinc-50 rounded-[32px] border border-zinc-100">
              <Typography variant="label" className="text-indigo-500 flex items-center gap-2 font-black uppercase tracking-widest text-[9px]"><Target size={14} /> Walk Style</Typography>
              <Typography variant="body" className="text-navy-900 font-black text-xl">{dog.walkStyle}</Typography>
            </div>
            <div className="space-y-4 p-6 bg-zinc-50 rounded-[32px] border border-zinc-100 flex flex-col justify-center">
              <Typography variant="label" className="text-emerald-500 flex items-center gap-2 font-black uppercase tracking-widest text-[9px]"><Check size={14} /> Neutralized</Typography>
              <Typography variant="body" className="text-navy-900 font-black text-xl">{dog.isNeutralized ? '완료' : '미완료'}</Typography>
            </div>
          </div>
        </div>
      </Card>
    </div>,
    document.body
  );
};
