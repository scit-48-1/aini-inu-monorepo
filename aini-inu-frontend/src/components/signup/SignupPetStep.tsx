'use client';

import React, { useState, useEffect } from 'react';
import { Dog, ArrowLeft, ArrowRight, Loader2, SkipForward } from 'lucide-react';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { cn } from '@/lib/utils';
import { toast } from 'sonner';
import { createPet, getBreeds } from '@/api/pets';
import type { BreedResponse } from '@/api/pets';

interface SignupPetStepProps {
  onComplete: () => void;
  onSkip: () => void;
  onPrev: () => void;
}

const GENDER_OPTIONS = [
  { value: 'MALE', label: '수컷' },
  { value: 'FEMALE', label: '암컷' },
];

const SIZE_OPTIONS = [
  { value: 'SMALL', label: '소형 (10kg 미만)' },
  { value: 'MEDIUM', label: '중형 (10~25kg)' },
  { value: 'LARGE', label: '대형 (25kg 이상)' },
];

export const SignupPetStep: React.FC<SignupPetStepProps> = ({
  onComplete,
  onSkip,
  onPrev,
}) => {
  const [name, setName] = useState('');
  const [breedId, setBreedId] = useState<number | ''>('');
  const [birthDate, setBirthDate] = useState('');
  const [gender, setGender] = useState('');
  const [size, setSize] = useState('');
  const [isNeutered, setIsNeutered] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [breeds, setBreeds] = useState<BreedResponse[]>([]);
  const [isBreedsLoading, setIsBreedsLoading] = useState(true);

  useEffect(() => {
    getBreeds()
      .then(setBreeds)
      .catch(() => toast.error('견종 목록을 불러오는데 실패했습니다.'))
      .finally(() => setIsBreedsLoading(false));
  }, []);

  const nameValid = name.length >= 1 && name.length <= 10;
  const birthDateValid = birthDate.length === 10; // YYYY-MM-DD
  const canSubmit =
    nameValid &&
    breedId !== '' &&
    birthDateValid &&
    gender !== '' &&
    size !== '' &&
    !isSubmitting;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!canSubmit) return;

    setIsSubmitting(true);
    try {
      await createPet({
        name,
        breedId: breedId as number,
        birthDate,
        gender,
        size,
        isNeutered,
        isMain: true,
      });
      onComplete();
    } catch {
      toast.error('반려동물 등록에 실패했습니다.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-10 animate-in fade-in duration-500">
      <div className="text-center space-y-4">
        <Badge variant="amber" className="px-4 py-1.5 bg-amber-50 text-amber-600 border-none">
          Step 03. Pet Registration
        </Badge>
        <Typography variant="h2" className="text-4xl md:text-5xl font-serif font-black text-navy-900 tracking-tight">
          반려견을 <span className="text-amber-500 italic">등록</span>하세요
        </Typography>
        <Typography variant="body" className="text-zinc-400">
          나중에 등록하셔도 괜찮아요!
        </Typography>
      </div>

      <div className="space-y-6">
        {/* Pet Name */}
        <div className="space-y-3">
          <Typography variant="label" className="flex items-center gap-2">
            <Dog size={14} className="text-amber-500" /> 이름
          </Typography>
          <input
            name="petName"
            type="text"
            placeholder="반려견 이름 (1~10자)"
            maxLength={10}
            className="w-full bg-zinc-50 border border-zinc-100 rounded-[24px] py-5 px-8 font-bold text-navy-900 outline-none focus:ring-4 ring-amber-500/5 transition-all"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
        </div>

        {/* Breed */}
        <div className="space-y-3">
          <Typography variant="label">견종</Typography>
          <select
            name="breedId"
            className="w-full bg-zinc-50 border border-zinc-100 rounded-[24px] py-5 px-8 font-bold text-navy-900 outline-none focus:ring-4 ring-amber-500/5 transition-all appearance-none"
            value={breedId}
            onChange={(e) => setBreedId(e.target.value === '' ? '' : Number(e.target.value))}
            disabled={isBreedsLoading}
          >
            <option value="">
              {isBreedsLoading ? '불러오는 중...' : '견종을 선택해주세요'}
            </option>
            {breeds.map((breed) => (
              <option key={breed.id} value={breed.id}>
                {breed.name}
              </option>
            ))}
          </select>
        </div>

        {/* Birth Date */}
        <div className="space-y-3">
          <Typography variant="label">생년월일</Typography>
          <input
            name="birthDate"
            type="date"
            max={new Date().toISOString().split('T')[0]}
            className="w-full bg-zinc-50 border border-zinc-100 rounded-[24px] py-5 px-8 font-bold text-navy-900 outline-none focus:ring-4 ring-amber-500/5 transition-all"
            value={birthDate}
            onChange={(e) => setBirthDate(e.target.value)}
          />
        </div>

        {/* Gender */}
        <div className="space-y-3">
          <Typography variant="label">성별</Typography>
          <div className="flex gap-4">
            {GENDER_OPTIONS.map((opt) => (
              <button
                key={opt.value}
                type="button"
                className={cn(
                  "flex-1 py-5 rounded-[24px] font-bold text-sm border-2 transition-all",
                  gender === opt.value
                    ? "bg-navy-900 border-navy-900 text-white"
                    : "bg-zinc-50 border-zinc-100 text-zinc-500 hover:border-amber-300"
                )}
                onClick={() => setGender(opt.value)}
              >
                {opt.label}
              </button>
            ))}
          </div>
        </div>

        {/* Size */}
        <div className="space-y-3">
          <Typography variant="label">크기</Typography>
          <div className="flex flex-col gap-2">
            {SIZE_OPTIONS.map((opt) => (
              <button
                key={opt.value}
                type="button"
                className={cn(
                  "w-full py-4 px-8 rounded-[20px] font-bold text-sm border-2 text-left transition-all",
                  size === opt.value
                    ? "bg-navy-900 border-navy-900 text-white"
                    : "bg-zinc-50 border-zinc-100 text-zinc-500 hover:border-amber-300"
                )}
                onClick={() => setSize(opt.value)}
              >
                {opt.label}
              </button>
            ))}
          </div>
        </div>

        {/* Neutered */}
        <div className="flex items-center justify-between bg-zinc-50 rounded-[24px] px-8 py-5">
          <Typography variant="label" className="mb-0">중성화 여부</Typography>
          <button
            type="button"
            className={cn(
              "w-14 h-7 rounded-full transition-all relative",
              isNeutered ? "bg-amber-500" : "bg-zinc-200"
            )}
            onClick={() => setIsNeutered(!isNeutered)}
          >
            <span
              className={cn(
                "absolute top-0.5 w-6 h-6 bg-white rounded-full shadow transition-all",
                isNeutered ? "left-7" : "left-0.5"
              )}
            />
          </button>
        </div>
      </div>

      <div className="flex gap-4">
        <Button
          type="button"
          variant="outline"
          size="xl"
          className="py-8 rounded-[24px]"
          onClick={onPrev}
          disabled={isSubmitting}
        >
          <ArrowLeft size={24} />
        </Button>
        <Button
          type="submit"
          disabled={!canSubmit}
          variant="primary"
          size="xl"
          fullWidth
          className="py-8 shadow-2xl"
        >
          {isSubmitting ? (
            <Loader2 className="animate-spin" size={24} />
          ) : (
            <>등록하기 <ArrowRight className="ml-2" /></>
          )}
        </Button>
      </div>

      {/* Skip button — always visible */}
      <div className="text-center">
        <button
          type="button"
          onClick={onSkip}
          disabled={isSubmitting}
          className="text-zinc-400 hover:text-amber-500 transition-colors flex items-center justify-center gap-2 text-sm font-semibold mx-auto"
        >
          <SkipForward size={16} /> 나중에 등록하기
        </button>
      </div>
    </form>
  );
};
