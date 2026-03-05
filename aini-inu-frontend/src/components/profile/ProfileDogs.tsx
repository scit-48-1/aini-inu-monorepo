'use client';

import React from 'react';
import { Plus, Target, MoreHorizontal } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import { Badge } from '@/components/ui/Badge';
import { DogType } from '@/types';

interface ProfileDogsProps {
  dogs: DogType[];
  onDogClick: (dog: DogType) => void;
  onAddClick: () => void;
}

export const ProfileDogs: React.FC<ProfileDogsProps> = ({ dogs, onDogClick, onAddClick }) => {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 p-6 md:p-8 animate-in slide-in-from-bottom-4 duration-500">
      {dogs.map(dog => (
        <Card key={dog.id} interactive className="group overflow-hidden rounded-[40px] border-zinc-100 shadow-xl" onClick={() => onDogClick(dog)}>
          <div className="h-48 relative overflow-hidden">
            <img src={dog.image} alt={dog.name} className="w-full h-full object-cover transition-transform group-hover:scale-110" />
            <div className="absolute bottom-0 left-0 right-0 p-6 bg-gradient-to-t from-black/80 to-transparent text-white">
              <Typography variant="h3" className="text-white text-xl">{dog.name}</Typography>
              <Typography variant="label" className="text-white/80 font-bold opacity-80">{dog.breed} • {dog.age}세</Typography>
            </div>
          </div>
          <div className="p-6 space-y-4">
            <div className="flex flex-wrap gap-2">
              {(dog.tendencies || []).slice(0, 3).map((t: any, i: number) => (
                <Badge key={i} variant="default" className="bg-zinc-50 border-none text-[10px] font-bold text-zinc-500 px-3">
                  {typeof t === 'string' ? t : t.ko}
                </Badge>
              ))}
            </div>
            <div className="pt-4 border-t border-zinc-50 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Target size={12} className="text-indigo-500" />
                <Typography variant="label" className="text-indigo-600 font-black">{dog.walkStyle}</Typography>
              </div>
              <MoreHorizontal size={16} className="text-zinc-300" />
            </div>
          </div>
        </Card>
      ))}
      <button 
        onClick={onAddClick} 
        className="bg-zinc-50 border-2 border-dashed border-zinc-200 rounded-[40px] flex flex-col items-center justify-center text-zinc-300 hover:text-amber-500 hover:border-amber-500/50 transition-all p-8 min-h-[300px]"
      >
        <Plus size={32} className="mb-2" />
        <Typography variant="body" className="font-black text-xs uppercase tracking-widest">Add New Partner</Typography>
      </button>
    </div>
  );
};
