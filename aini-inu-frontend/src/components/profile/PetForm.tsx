'use client';

import React, { useState, useRef } from 'react';
import { Loader2, Dog, Camera } from 'lucide-react';
import { toast } from 'sonner';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { cn } from '@/lib/utils';
import { useMasterData } from '@/hooks/useMasterData';
import { uploadImageFlow } from '@/api/upload';
import type { PetResponse, PetCreateRequest } from '@/api/pets';

interface PetFormProps {
  mode: 'create' | 'edit';
  initialData?: PetResponse;
  onSubmit: (data: PetCreateRequest) => Promise<void>;
  onCancel: () => void;
  isSubmitting: boolean;
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

const TODAY = new Date().toISOString().split('T')[0];

export const PetForm: React.FC<PetFormProps> = ({
  mode,
  initialData,
  onSubmit,
  onCancel,
  isSubmitting,
}) => {
  const { breeds, personalities, walkingStyles, isLoading: masterDataLoading } = useMasterData();

  // Form fields
  const [name, setName] = useState(initialData?.name || '');
  const [breedId, setBreedId] = useState<number | ''>(
    mode === 'edit' ? (initialData?.breed?.id ?? '') : ''
  );
  const [birthDate, setBirthDate] = useState('');
  const [gender, setGender] = useState(initialData?.gender || '');
  const [size, setSize] = useState(initialData?.size || '');
  const [isNeutered, setIsNeutered] = useState(initialData?.isNeutered ?? false);
  const [mbti, setMbti] = useState(initialData?.mbti || '');
  const [photoUrl, setPhotoUrl] = useState(initialData?.photoUrl || '');
  const [certificationNumber, setCertificationNumber] = useState('');
  const [selectedWalkingStyles, setSelectedWalkingStyles] = useState<string[]>(
    initialData?.walkingStyles || []
  );
  const [selectedPersonalityIds, setSelectedPersonalityIds] = useState<number[]>(
    (initialData?.personalities || []).map(p => p.id)
  );
  const [isUploadingPhoto, setIsUploadingPhoto] = useState(false);

  const fileInputRef = useRef<HTMLInputElement>(null);

  const isEdit = mode === 'edit';

  // Photo upload handler
  const handlePhotoChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setIsUploadingPhoto(true);
    try {
      const url = await uploadImageFlow(file, 'PET_PROFILE');
      setPhotoUrl(url);
    } catch {
      toast.error('사진 업로드에 실패했습니다.');
    } finally {
      setIsUploadingPhoto(false);
    }
  };

  // Walking style toggle
  const toggleWalkingStyle = (code: string) => {
    setSelectedWalkingStyles(prev =>
      prev.includes(code) ? prev.filter(c => c !== code) : [...prev, code]
    );
  };

  // Personality toggle
  const togglePersonality = (id: number) => {
    setSelectedPersonalityIds(prev =>
      prev.includes(id) ? prev.filter(i => i !== id) : [...prev, id]
    );
  };

  // Validate and submit
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!name.trim() || name.length < 1 || name.length > 10) {
      toast.warning('반려견 이름은 1~10자 사이로 입력해주세요.');
      return;
    }
    if (!isEdit && !breedId) {
      toast.warning('견종을 선택해주세요.');
      return;
    }
    if (!birthDate) {
      toast.warning('생년월일을 입력해주세요.');
      return;
    }
    if (!isEdit && !gender) {
      toast.warning('성별을 선택해주세요.');
      return;
    }
    if (!isEdit && !size) {
      toast.warning('크기를 선택해주세요.');
      return;
    }

    const payload: PetCreateRequest = {
      name: name.trim(),
      breedId: isEdit ? (initialData?.breed?.id ?? 0) : (breedId as number),
      birthDate,
      gender: isEdit ? (initialData?.gender ?? '') : gender,
      size: isEdit ? (initialData?.size ?? '') : size,
      isNeutered,
    };

    if (mbti.trim()) payload.mbti = mbti.trim();
    if (photoUrl) payload.photoUrl = photoUrl;
    if (certificationNumber.trim()) payload.certificationNumber = certificationNumber.trim();
    if (selectedWalkingStyles.length > 0) payload.walkingStyles = selectedWalkingStyles;
    if (selectedPersonalityIds.length > 0) payload.personalityIds = selectedPersonalityIds;

    await onSubmit(payload);
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-8">
      {/* Photo */}
      <div className="flex flex-col items-center gap-4">
        <div
          className="relative w-28 h-28 rounded-full overflow-hidden bg-zinc-50 border-2 border-dashed border-zinc-200 cursor-pointer hover:border-amber-400 transition-all flex items-center justify-center"
          onClick={() => fileInputRef.current?.click()}
        >
          {photoUrl ? (
            <img src={photoUrl} alt="반려견 사진" className="w-full h-full object-cover" />
          ) : (
            <div className="flex flex-col items-center gap-1 text-zinc-300">
              <Dog size={28} />
              <span className="text-[10px] font-bold">사진 추가</span>
            </div>
          )}
          {isUploadingPhoto && (
            <div className="absolute inset-0 bg-black/40 flex items-center justify-center rounded-full">
              <Loader2 className="animate-spin text-white" size={20} />
            </div>
          )}
          <div className="absolute bottom-1 right-1 bg-amber-500 rounded-full p-1">
            <Camera size={12} className="text-white" />
          </div>
        </div>
        <input
          ref={fileInputRef}
          type="file"
          accept="image/*"
          className="hidden"
          onChange={handlePhotoChange}
        />
      </div>

      {/* Name */}
      <div className="space-y-3">
        <Typography variant="label">이름 <span className="text-red-400">*</span></Typography>
        <input
          type="text"
          placeholder="반려견 이름 (1~10자)"
          maxLength={10}
          required
          className="w-full bg-zinc-50 border border-zinc-100 rounded-[24px] py-5 px-8 font-bold text-navy-900 outline-none focus:ring-4 ring-amber-500/5 transition-all"
          value={name}
          onChange={e => setName(e.target.value)}
        />
        <Typography variant="label" className="text-zinc-400 text-xs px-2">
          {name.length}/10자
        </Typography>
      </div>

      {/* Breed */}
      <div className="space-y-3">
        <Typography variant="label">견종 <span className="text-red-400">*</span></Typography>
        {isEdit ? (
          <div className="w-full bg-zinc-100 border border-zinc-200 rounded-[24px] py-5 px-8 font-bold text-zinc-400 cursor-not-allowed">
            {initialData?.breed?.name || '알 수 없음'}
            <span className="text-xs ml-2 font-normal text-zinc-400">(변경 불가)</span>
          </div>
        ) : (
          <select
            className="w-full bg-zinc-50 border border-zinc-100 rounded-[24px] py-5 px-8 font-bold text-navy-900 outline-none focus:ring-4 ring-amber-500/5 transition-all appearance-none"
            value={breedId}
            onChange={e => setBreedId(e.target.value === '' ? '' : Number(e.target.value))}
            disabled={masterDataLoading}
            required
          >
            <option value="">
              {masterDataLoading ? '불러오는 중...' : '견종을 선택해주세요'}
            </option>
            {breeds.map(breed => (
              <option key={breed.id} value={breed.id}>
                {breed.name}
              </option>
            ))}
          </select>
        )}
      </div>

      {/* Birth Date */}
      <div className="space-y-3">
        <Typography variant="label">생년월일 <span className="text-red-400">*</span></Typography>
        <input
          type="date"
          max={TODAY}
          required
          className="w-full bg-zinc-50 border border-zinc-100 rounded-[24px] py-5 px-8 font-bold text-navy-900 outline-none focus:ring-4 ring-amber-500/5 transition-all"
          value={birthDate}
          onChange={e => setBirthDate(e.target.value)}
        />
      </div>

      {/* Gender */}
      <div className="space-y-3">
        <Typography variant="label">성별 <span className="text-red-400">*</span></Typography>
        {isEdit ? (
          <div className="w-full bg-zinc-100 border border-zinc-200 rounded-[24px] py-5 px-8 font-bold text-zinc-400 cursor-not-allowed">
            {initialData?.gender === 'MALE' ? '수컷' : initialData?.gender === 'FEMALE' ? '암컷' : '알 수 없음'}
            <span className="text-xs ml-2 font-normal text-zinc-400">(변경 불가)</span>
          </div>
        ) : (
          <div className="flex gap-4">
            {GENDER_OPTIONS.map(opt => (
              <button
                key={opt.value}
                type="button"
                className={cn(
                  'flex-1 py-5 rounded-[24px] font-bold text-sm border-2 transition-all',
                  gender === opt.value
                    ? 'bg-navy-900 border-navy-900 text-white'
                    : 'bg-zinc-50 border-zinc-100 text-zinc-500 hover:border-amber-300'
                )}
                onClick={() => setGender(opt.value)}
              >
                {opt.label}
              </button>
            ))}
          </div>
        )}
      </div>

      {/* Size */}
      <div className="space-y-3">
        <Typography variant="label">크기 <span className="text-red-400">*</span></Typography>
        {isEdit ? (
          <div className="w-full bg-zinc-100 border border-zinc-200 rounded-[24px] py-5 px-8 font-bold text-zinc-400 cursor-not-allowed">
            {SIZE_OPTIONS.find(o => o.value === initialData?.size)?.label || '알 수 없음'}
            <span className="text-xs ml-2 font-normal text-zinc-400">(변경 불가)</span>
          </div>
        ) : (
          <div className="flex flex-col gap-2">
            {SIZE_OPTIONS.map(opt => (
              <button
                key={opt.value}
                type="button"
                className={cn(
                  'w-full py-4 px-8 rounded-[20px] font-bold text-sm border-2 text-left transition-all',
                  size === opt.value
                    ? 'bg-navy-900 border-navy-900 text-white'
                    : 'bg-zinc-50 border-zinc-100 text-zinc-500 hover:border-amber-300'
                )}
                onClick={() => setSize(opt.value)}
              >
                {opt.label}
              </button>
            ))}
          </div>
        )}
      </div>

      {/* Neutered */}
      <div className="flex items-center justify-between bg-zinc-50 rounded-[24px] px-8 py-5">
        <Typography variant="label" className="mb-0">중성화 여부</Typography>
        <button
          type="button"
          className={cn(
            'w-14 h-7 rounded-full transition-all relative',
            isNeutered ? 'bg-amber-500' : 'bg-zinc-200'
          )}
          onClick={() => setIsNeutered(!isNeutered)}
        >
          <span
            className={cn(
              'absolute top-0.5 w-6 h-6 bg-white rounded-full shadow transition-all',
              isNeutered ? 'left-7' : 'left-0.5'
            )}
          />
        </button>
      </div>

      {/* MBTI */}
      <div className="space-y-3">
        <Typography variant="label">MBTI <span className="text-zinc-400 text-xs font-normal">(선택)</span></Typography>
        <input
          type="text"
          placeholder="예: ENFP"
          maxLength={4}
          className="w-full bg-zinc-50 border border-zinc-100 rounded-[24px] py-5 px-8 font-bold text-navy-900 outline-none focus:ring-4 ring-amber-500/5 transition-all uppercase"
          value={mbti}
          onChange={e => setMbti(e.target.value.toUpperCase())}
        />
      </div>

      {/* Certification Number */}
      <div className="space-y-3">
        <Typography variant="label">등록번호 <span className="text-zinc-400 text-xs font-normal">(선택)</span></Typography>
        <input
          type="text"
          placeholder="동물등록번호 (최대 15자)"
          maxLength={15}
          className="w-full bg-zinc-50 border border-zinc-100 rounded-[24px] py-5 px-8 font-bold text-navy-900 outline-none focus:ring-4 ring-amber-500/5 transition-all"
          value={certificationNumber}
          onChange={e => setCertificationNumber(e.target.value)}
        />
      </div>

      {/* Walking Styles */}
      <div className="space-y-3">
        <Typography variant="label">산책 스타일 <span className="text-zinc-400 text-xs font-normal">(선택, 중복 가능)</span></Typography>
        {masterDataLoading ? (
          <div className="flex items-center gap-2 text-zinc-300 py-2">
            <Loader2 size={14} className="animate-spin" />
            <span className="text-sm">불러오는 중...</span>
          </div>
        ) : (
          <div className="flex flex-wrap gap-2">
            {walkingStyles.map(ws => (
              <button
                key={ws.code}
                type="button"
                className={cn(
                  'px-4 py-2 rounded-full text-sm font-bold border-2 transition-all',
                  selectedWalkingStyles.includes(ws.code)
                    ? 'bg-amber-500 border-amber-500 text-white'
                    : 'bg-zinc-50 border-zinc-100 text-zinc-500 hover:border-amber-300'
                )}
                onClick={() => toggleWalkingStyle(ws.code)}
              >
                {ws.name}
              </button>
            ))}
          </div>
        )}
      </div>

      {/* Personalities */}
      <div className="space-y-3">
        <Typography variant="label">성격 <span className="text-zinc-400 text-xs font-normal">(선택, 중복 가능)</span></Typography>
        {masterDataLoading ? (
          <div className="flex items-center gap-2 text-zinc-300 py-2">
            <Loader2 size={14} className="animate-spin" />
            <span className="text-sm">불러오는 중...</span>
          </div>
        ) : (
          <div className="flex flex-wrap gap-2">
            {personalities.map(p => (
              <button
                key={p.id}
                type="button"
                className={cn(
                  'px-4 py-2 rounded-full text-sm font-bold border-2 transition-all',
                  selectedPersonalityIds.includes(p.id)
                    ? 'bg-navy-900 border-navy-900 text-white'
                    : 'bg-zinc-50 border-zinc-100 text-zinc-500 hover:border-amber-300'
                )}
                onClick={() => togglePersonality(p.id)}
              >
                {p.name}
              </button>
            ))}
          </div>
        )}
      </div>

      {/* Actions */}
      <div className="flex gap-4 pt-4">
        <Button
          type="button"
          variant="outline"
          size="xl"
          className="flex-1 py-6 rounded-[24px]"
          onClick={onCancel}
          disabled={isSubmitting}
        >
          취소
        </Button>
        <Button
          type="submit"
          variant="primary"
          size="xl"
          className="flex-1 py-6 shadow-xl"
          disabled={isSubmitting || isUploadingPhoto}
        >
          {isSubmitting ? (
            <Loader2 className="animate-spin" size={20} />
          ) : (
            <>{isEdit ? '수정하기' : '등록하기'}</>
          )}
        </Button>
      </div>
    </form>
  );
};
