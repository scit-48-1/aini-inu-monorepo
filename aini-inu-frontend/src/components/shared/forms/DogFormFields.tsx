'use client';

import React, { Dispatch, SetStateAction } from 'react';
import { Camera, Dog } from 'lucide-react';
import { Typography } from '@/components/ui/Typography';
import { cn } from '@/lib/utils';
import { DOG_PERSONALITIES } from '@/constants/dogPersonalities';
import { DogBreed } from '@/constants/dogBreeds';
import { WalkStyle, DogTendency } from '@/types';

interface DogFormState {
  name: string;
  breed: string;
  age: string;
  birthDate: string;
  gender: 'M' | 'F';
  image: string;
  isNeutralized: boolean;
  tendencies: DogTendency[];
  walkStyle: WalkStyle;
  mbti: string;
}

interface DogFormFieldsProps {
  dogForm: DogFormState;
  setDogForm: Dispatch<SetStateAction<DogFormState>>;
  onBreedChange: (val: string) => void;
  breedSuggestions: DogBreed[];
  showBreedSuggestions: boolean;
  setShowBreedSuggestions: (show: boolean) => void;
  onSelectBreed: (breed: DogBreed) => void;
  onToggleTendency: (t: string) => void;
  onImageUpload: (base64: string) => void;
  fileInputRef: React.RefObject<HTMLInputElement | null>;
}

export const DogFormFields: React.FC<DogFormFieldsProps> = ({
  dogForm, setDogForm,
  onBreedChange, breedSuggestions, showBreedSuggestions, setShowBreedSuggestions, onSelectBreed,
  onToggleTendency, onImageUpload, fileInputRef
}) => {
  return (
    <div className="space-y-12">
      {/* 1. Photo */}
      <div className="flex flex-col items-center space-y-4 pb-4">
        <div className="relative w-32 h-32 rounded-[40px] overflow-hidden border-4 border-zinc-50 shadow-xl group cursor-pointer bg-zinc-100 flex items-center justify-center" onClick={() => fileInputRef.current?.click()}>
          {dogForm.image ? <img src={dogForm.image} className="w-full h-full object-cover" alt="Preview" /> : <Dog size={48} strokeWidth={1.5} className="text-zinc-300" />}
          <div className="absolute inset-0 bg-black/20 group-hover:bg-black/40 transition-all flex items-center justify-center text-white opacity-0 group-hover:opacity-100"><Camera size={24} /></div>
        </div>
        <input name="dogPhoto" type="file" ref={fileInputRef} className="hidden" accept="image/*" onChange={(e) => {
          const file = e.target.files?.[0]; if (!file) return;
          const reader = new FileReader();
          reader.onloadend = () => onImageUpload(reader.result as string);
          reader.readAsDataURL(file);
        }} />
      </div>

      {/* 2. Basic Info */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="space-y-2">
          <Typography variant="label" className="ml-1">이름</Typography>
          <input name="dogName" type="text" placeholder="아이의 이름" value={dogForm.name} onChange={(e) => setDogForm({...dogForm, name: e.target.value})} className="w-full bg-zinc-50 border-none rounded-2xl py-4 px-6 font-bold text-navy-900 focus:ring-4 ring-amber-500/10 transition-all" />
        </div>
        <div className="space-y-2 relative">
          <Typography variant="label" className="ml-1">품종</Typography>
          <input name="breed" type="text" placeholder="예: 시바견" value={dogForm.breed} onChange={(e) => onBreedChange(e.target.value)} onBlur={() => setTimeout(() => setShowBreedSuggestions(false), 200)} className="w-full bg-zinc-50 border-none rounded-2xl py-4 px-6 font-bold text-navy-900 focus:ring-4 ring-amber-500/10 transition-all" />
          {showBreedSuggestions && breedSuggestions.length > 0 && (
            <div className="absolute top-20 left-0 right-0 bg-white rounded-2xl border border-zinc-100 shadow-2xl z-[3000] overflow-hidden">
              {breedSuggestions.map((b, i) => (
                <button key={i} type="button" onClick={() => onSelectBreed(b)} className="w-full px-6 py-4 text-left hover:bg-amber-50 border-b border-zinc-50 last:border-none transition-colors">
                  <Typography variant="body" className="font-bold text-navy-900">{b.ko}</Typography>
                </button>
              ))}
            </div>
          )}
        </div>
        <div className="space-y-2">
          <Typography variant="label" className="ml-1">생년월일</Typography>
          <input name="birthDate" type="date" value={dogForm.birthDate} onChange={(e) => setDogForm({...dogForm, birthDate: e.target.value})} className="w-full bg-zinc-50 border-none rounded-2xl py-4 px-6 font-bold text-navy-900 focus:ring-4 ring-amber-500/10 transition-all" />
        </div>
        <div className="space-y-2">
          <Typography variant="label" className="ml-1">성별</Typography>
          <div className="flex bg-zinc-50 p-1.5 rounded-2xl">
            <button type="button" onClick={() => setDogForm({...dogForm, gender: 'M'})} className={cn("flex-1 py-3 rounded-xl text-sm font-black transition-all", dogForm.gender === 'M' ? "bg-white text-navy-900 shadow-md" : "text-zinc-400")}>남아</button>
            <button type="button" onClick={() => setDogForm({...dogForm, gender: 'F'})} className={cn("flex-1 py-3 rounded-xl text-sm font-black transition-all", dogForm.gender === 'F' ? "bg-white text-navy-900 shadow-md" : "text-zinc-400")}>여아</button>
          </div>
        </div>
      </div>

      {/* 3. Personality (MBTI + Tendencies) */}
      <div className="space-y-10">
        <div className="space-y-4">
          <div className="flex items-center justify-between px-1">
            <Typography variant="label">강아지 MBTI</Typography>
            <span className="text-[9px] font-black text-zinc-300 uppercase">Optional</span>
          </div>
          <div className="flex gap-2 overflow-x-auto no-scrollbar pb-2 -mx-1 px-1">
            {['ENFP', 'ENFJ', 'ENTP', 'ENTJ', 'ESFP', 'ESFJ', 'ESTP', 'ESTJ', 'INFP', 'INFJ', 'INTP', 'INTJ', 'ISFP', 'ISFJ', 'ISTP', 'ISTJ'].map((mbti) => (
              <button
                key={mbti}
                type="button"
                onClick={() => setDogForm({ ...dogForm, mbti: dogForm.mbti === mbti ? '' : mbti })}
                className={cn(
                  "flex-shrink-0 px-5 py-3 rounded-2xl text-xs font-black transition-all border",
                  dogForm.mbti === mbti
                    ? "bg-amber-500 text-white border-amber-500 shadow-lg"
                    : "bg-white text-zinc-400 border-zinc-100 hover:border-amber-200"
                )}
              >
                {mbti}
              </button>
            ))}
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="space-y-4">
            <Typography variant="label" className="ml-1">중성화 여부</Typography>
            <div className="flex bg-zinc-50 p-1.5 rounded-2xl">
              <button type="button" onClick={() => setDogForm({...dogForm, isNeutralized: true})} className={cn("flex-1 py-3 rounded-xl text-sm font-black transition-all", dogForm.isNeutralized ? "bg-white text-navy-900 shadow-md" : "text-zinc-400")}>완료</button>
              <button type="button" onClick={() => setDogForm({...dogForm, isNeutralized: false})} className={cn("flex-1 py-3 rounded-xl text-sm font-black transition-all", !dogForm.isNeutralized ? "bg-white text-navy-900 shadow-md" : "text-zinc-400")}>미완료</button>
            </div>
          </div>
        </div>

        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <Typography variant="label">성향 (최대 6개)</Typography>
            <span className="text-[10px] font-black text-amber-500">{dogForm.tendencies.length} / 6</span>
          </div>
          <div className="flex flex-wrap gap-2">
            {DOG_PERSONALITIES.slice(0, 15).map((p) => (
              <button
                key={p.en}
                type="button"
                onClick={() => onToggleTendency(p.ko)}
                className={cn(
                  "px-4 py-2 rounded-2xl text-xs font-black transition-all border",
                  dogForm.tendencies.includes(p.ko as DogTendency)
                    ? "bg-navy-900 text-white border-navy-900 shadow-md"
                    : "bg-white text-zinc-400 border-zinc-100 hover:border-amber-200"
                )}
              >
                #{p.ko}
              </button>
            ))}
          </div>
        </div>

        <div className="space-y-4">
          <Typography variant="label">산책 스타일</Typography>
          <div className="flex bg-zinc-50 p-1.5 rounded-2xl">
            {['느긋함', '적당함', '에너지넘침', '전력질주'].map((style) => (
              <button
                key={style}
                type="button"
                onClick={() => setDogForm({...dogForm, walkStyle: style as WalkStyle})}
                className={cn(
                  "flex-1 py-3 rounded-xl text-xs font-black transition-all",
                  dogForm.walkStyle === style ? "bg-white text-navy-900 shadow-md" : "text-zinc-400"
                )}
              >
                {style}
              </button>
            ))}
          </div>
        </div>
      </div>

    </div>
  );
};
