'use client';

import React from 'react';
import { Plus, Crown, Dog } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import { Badge } from '@/components/ui/Badge';
import type { PetResponse } from '@/api/pets';

interface ProfileDogsProps {
  pets: PetResponse[];
  onPetClick: (pet: PetResponse) => void;
  onAddClick: () => void;
}

export const ProfileDogs: React.FC<ProfileDogsProps> = ({ pets, onPetClick, onAddClick }) => {
  const isAtLimit = pets.length >= 10;

  if (pets.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center p-12 min-h-[300px]">
        <Dog size={48} className="text-zinc-300 mb-4" />
        <Typography variant="body" className="text-zinc-400 mb-6">
          아직 등록된 반려견이 없습니다
        </Typography>
        <button
          onClick={onAddClick}
          className="bg-amber-500 text-white px-6 py-3 rounded-full font-bold hover:bg-amber-600 transition-colors"
        >
          반려견 등록하기
        </button>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 p-6 md:p-8 animate-in slide-in-from-bottom-4 duration-500">
      {pets.map(pet => (
        <Card key={pet.id} interactive className="group overflow-hidden rounded-[40px] border-zinc-100 shadow-xl" onClick={() => onPetClick(pet)}>
          <div className="h-48 relative overflow-hidden">
            <img
              src={pet.photoUrl || '/AINIINU_ROGO_B.png'}
              alt={pet.name}
              className="w-full h-full object-cover transition-transform group-hover:scale-110"
            />
            {pet.isMain && (
              <div className="absolute top-3 right-3 bg-amber-500 rounded-full p-1.5 shadow-lg">
                <Crown size={14} className="text-white" />
              </div>
            )}
            <div className="absolute bottom-0 left-0 right-0 p-6 bg-gradient-to-t from-black/80 to-transparent text-white">
              <Typography variant="h3" className="text-white text-xl">{pet.name}</Typography>
              <Typography variant="label" className="text-white/80 font-bold opacity-80">
                {pet.breed?.name} {pet.age != null ? `• ${pet.age}세` : ''}
              </Typography>
            </div>
          </div>
          <div className="p-6 space-y-4">
            <div className="flex flex-wrap gap-2">
              {(pet.personalities || []).slice(0, 3).map(p => (
                <Badge key={p.id} variant="default" className="bg-zinc-50 border-none text-[10px] font-bold text-zinc-500 px-3">
                  {p.name}
                </Badge>
              ))}
            </div>
            {(pet.walkingStyles || []).length > 0 && (
              <div className="pt-4 border-t border-zinc-50 flex items-center gap-2 flex-wrap">
                {pet.walkingStyles.map(code => (
                  <Badge key={code} variant="default" className="bg-amber-50 border-none text-[10px] font-bold text-amber-600 px-3">
                    {code}
                  </Badge>
                ))}
              </div>
            )}
          </div>
        </Card>
      ))}

      <div className="flex flex-col items-center gap-3">
        <button
          onClick={onAddClick}
          disabled={isAtLimit}
          className="w-full bg-zinc-50 border-2 border-dashed border-zinc-200 rounded-[40px] flex flex-col items-center justify-center text-zinc-300 hover:text-amber-500 hover:border-amber-500/50 transition-all p-8 min-h-[300px] disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:text-zinc-300 disabled:hover:border-zinc-200"
        >
          <Plus size={32} className="mb-2" />
          <Typography variant="body" className="font-black text-xs uppercase tracking-widest">Add New Partner</Typography>
          <Typography variant="label" className="text-zinc-400 text-xs mt-2 normal-case tracking-normal">
            {pets.length}/10마리
          </Typography>
        </button>
        {isAtLimit && (
          <Typography variant="label" className="text-zinc-400 text-xs text-center">
            최대 10마리까지 등록할 수 있습니다
          </Typography>
        )}
      </div>
    </div>
  );
};
