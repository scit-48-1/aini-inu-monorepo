'use client';

import React from 'react';
import { Plus } from 'lucide-react';
import type { PetResponse } from '@/api/pets';
import { cn } from '@/lib/utils';

interface PetHighlightsProps {
  pets: PetResponse[];
  onPetClick: (pet: PetResponse) => void;
  onAddClick?: () => void;
}

export const PetHighlights: React.FC<PetHighlightsProps> = ({ pets, onPetClick, onAddClick }) => {
  if (pets.length === 0 && !onAddClick) {
    return null;
  }

  if (pets.length === 0 && onAddClick) {
    return (
      <div className="px-6 py-3 flex items-center gap-3">
        <button
          onClick={onAddClick}
          className="flex flex-col items-center gap-1.5 group"
        >
          <div className="w-16 h-16 rounded-full border-2 border-dashed border-zinc-300 flex items-center justify-center text-zinc-300 group-hover:border-amber-500 group-hover:text-amber-500 transition-colors">
            <Plus size={20} />
          </div>
          <span className="text-[10px] text-zinc-400 font-medium">등록하기</span>
        </button>
        <span className="text-xs text-zinc-300 ml-1">반려견을 등록해보세요</span>
      </div>
    );
  }

  return (
    <div className="px-6 py-3 overflow-x-auto no-scrollbar">
      <div className="flex gap-4">
        {pets.map((pet) => (
          <button
            key={pet.id}
            onClick={() => onPetClick(pet)}
            className="flex flex-col items-center gap-1.5 shrink-0 group"
          >
            <div
              className={cn(
                'w-16 h-16 rounded-full overflow-hidden',
                pet.isMain ? 'ring-2 ring-amber-500 ring-offset-2' : 'ring-1 ring-zinc-200'
              )}
            >
              <img
                src={pet.photoUrl || '/AINIINU_ROGO_B.png'}
                alt={pet.name}
                className="w-full h-full object-cover group-hover:scale-110 transition-transform"
              />
            </div>
            <span className="text-[10px] text-zinc-600 font-medium max-w-[4em] truncate">
              {pet.name}
            </span>
          </button>
        ))}
        {onAddClick && (
          <button
            onClick={onAddClick}
            className="flex flex-col items-center gap-1.5 shrink-0 group"
          >
            <div className="w-16 h-16 rounded-full border-2 border-dashed border-zinc-300 flex items-center justify-center text-zinc-300 group-hover:border-amber-500 group-hover:text-amber-500 transition-colors">
              <Plus size={20} />
            </div>
            <span className="text-[10px] text-zinc-400 font-medium">추가</span>
          </button>
        )}
      </div>
    </div>
  );
};
