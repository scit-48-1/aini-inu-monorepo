'use client';

import React, { useState, useEffect } from 'react';
import { Camera, X, Loader2, Megaphone, PlusCircle } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import { toast } from 'sonner';
import DaumPostcode from 'react-daum-postcode';
import { createLostPet, createSighting } from '@/api/lostPets';
import type { LostPetResponse } from '@/api/lostPets';
import { uploadImageFlow } from '@/api/upload';

type EmergencyMode = 'LOST' | 'FOUND';

interface EmergencyReportFormProps {
  onReportCreated?: (report: LostPetResponse, photoUrl: string) => void;
}

export const EmergencyReportForm: React.FC<EmergencyReportFormProps> = ({
  onReportCreated,
}) => {
  const [mode, setMode] = useState<EmergencyMode>('LOST');
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [imagePreview, setImagePreview] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showAddressSearch, setShowAddressSearch] = useState(false);

  // LOST mode fields
  const [petName, setPetName] = useState('');
  const [breed, setBreed] = useState('');
  const [lastSeenAt, setLastSeenAt] = useState('');
  const [lastSeenLocation, setLastSeenLocation] = useState('');
  const [description, setDescription] = useState('');

  // FOUND mode fields
  const [foundAt, setFoundAt] = useState('');
  const [foundLocation, setFoundLocation] = useState('');
  const [memo, setMemo] = useState('');

  // Cleanup object URL on unmount or change
  useEffect(() => {
    return () => {
      if (imagePreview) {
        URL.revokeObjectURL(imagePreview);
      }
    };
  }, [imagePreview]);

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (imagePreview) {
      URL.revokeObjectURL(imagePreview);
    }
    setImageFile(file);
    setImagePreview(URL.createObjectURL(file));
  };

  const clearImage = () => {
    if (imagePreview) {
      URL.revokeObjectURL(imagePreview);
    }
    setImageFile(null);
    setImagePreview(null);
  };

  const resetForm = () => {
    clearImage();
    setPetName('');
    setBreed('');
    setLastSeenAt('');
    setLastSeenLocation('');
    setDescription('');
    setFoundAt('');
    setFoundLocation('');
    setMemo('');
    setShowAddressSearch(false);
  };

  const switchMode = (newMode: EmergencyMode) => {
    setMode(newMode);
    resetForm();
  };

  const handleAddressComplete = (data: { address: string }) => {
    if (mode === 'LOST') {
      setLastSeenLocation(data.address);
    } else {
      setFoundLocation(data.address);
    }
    setShowAddressSearch(false);
  };

  const handleLostSubmit = async () => {
    if (!imageFile) {
      toast.error('사진을 등록해 주세요');
      return;
    }
    if (!petName.trim()) {
      toast.error('반려견 이름을 입력해 주세요');
      return;
    }
    if (!lastSeenLocation) {
      toast.error('마지막 목격 장소를 입력해 주세요');
      return;
    }

    setIsSubmitting(true);
    try {
      const photoUrl = await uploadImageFlow(imageFile, 'LOST_PET');
      const response = await createLostPet({
        petName: petName.trim(),
        breed: breed.trim(),
        photoUrl,
        description: description.trim(),
        lastSeenAt: lastSeenAt ? new Date(lastSeenAt).toISOString() : new Date().toISOString(),
        lastSeenLocation,
      });
      toast.success('실종 신고가 등록되었습니다!');
      onReportCreated?.(response, photoUrl);
      resetForm();
    } catch {
      // apiClient handles error toast
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleFoundSubmit = async () => {
    if (!imageFile) {
      toast.error('사진을 등록해 주세요');
      return;
    }
    if (!foundLocation) {
      toast.error('발견 장소를 입력해 주세요');
      return;
    }

    setIsSubmitting(true);
    try {
      const photoUrl = await uploadImageFlow(imageFile, 'SIGHTING');
      await createSighting({
        photoUrl,
        foundAt: foundAt ? new Date(foundAt).toISOString() : new Date().toISOString(),
        foundLocation,
        memo: memo.trim() || undefined,
      });
      toast.success('제보가 등록되었습니다!');
      resetForm();
    } catch {
      // apiClient handles error toast
    } finally {
      setIsSubmitting(false);
    }
  };

  const currentLocation = mode === 'LOST' ? lastSeenLocation : foundLocation;

  return (
    <div className="space-y-6 pb-20 animate-in slide-in-from-right-10 duration-700">
      <Card className="p-10 bg-white shadow-2xl rounded-[48px] space-y-10 relative overflow-hidden min-h-[500px] justify-center flex flex-col items-center text-center">
        {/* Mode toggle */}
        <div className="flex bg-white p-2 rounded-3xl border border-card-border shadow-sm w-full">
          <button
            onClick={() => switchMode('LOST')}
            className={cn(
              'flex-1 py-4 rounded-2xl text-sm font-black transition-all',
              mode === 'LOST' ? 'bg-amber-500 text-black shadow-md' : 'text-black',
            )}
          >
            내 아이 실종
          </button>
          <button
            onClick={() => switchMode('FOUND')}
            className={cn(
              'flex-1 py-4 rounded-2xl text-sm font-black transition-all',
              mode === 'FOUND' ? 'bg-amber-500 text-black shadow-md' : 'text-black',
            )}
          >
            유기견 제보
          </button>
        </div>

        {/* Image upload area */}
        {!imagePreview ? (
          <div className="py-10 flex flex-col items-center space-y-8">
            <label className="w-32 h-32 rounded-[40px] flex items-center justify-center shadow-xl bg-white border border-card-border hover:bg-zinc-50 transition-all relative group cursor-pointer active:scale-95">
              <div className="absolute inset-0 rounded-[40px] animate-ping opacity-10 bg-amber-500" />
              <Camera size={40} className="text-black relative group-hover:scale-110 transition-transform" />
              <input type="file" className="hidden" accept="image/*" onChange={handleFileUpload} />
            </label>
            <div className="space-y-3">
              <Typography variant="h2" className="text-2xl text-black">
                {mode === 'LOST' ? '실종 강아지 등록' : '유기견 제보'}
              </Typography>
              <Typography variant="body" className="text-zinc-400 text-base leading-relaxed">
                {mode === 'LOST'
                  ? '아이의 특징이 잘 드러난 사진을 올려주세요.'
                  : '발견한 아이의 사진을 올려주세요.'}
              </Typography>
            </div>
          </div>
        ) : (
          <div className="space-y-8 animate-in fade-in duration-500 w-full text-left">
            {/* Image preview */}
            <div className="relative mx-auto max-w-[280px]">
              <img
                src={imagePreview}
                className="w-full aspect-square object-cover rounded-[40px] shadow-xl border-4 border-white"
                alt={mode === 'LOST' ? '실종 반려견' : '제보 사진'}
              />
              {!isSubmitting && (
                <button
                  onClick={clearImage}
                  className="absolute -top-4 -right-4 w-10 h-10 bg-black text-white rounded-full flex items-center justify-center shadow-lg"
                >
                  <X size={20} />
                </button>
              )}
            </div>

            {/* LOST mode form fields */}
            {mode === 'LOST' && (
              <div className="space-y-6">
                <div className="space-y-2">
                  <Typography variant="label" className="text-zinc-400 text-[10px]">반려견 이름 *</Typography>
                  <input
                    type="text"
                    value={petName}
                    onChange={(e) => setPetName(e.target.value)}
                    placeholder="이름을 입력하세요"
                    className="w-full bg-background border border-card-border rounded-2xl px-4 py-3 text-sm font-medium focus:outline-none focus:ring-2 ring-amber-500/20"
                  />
                </div>

                <div className="space-y-2">
                  <Typography variant="label" className="text-zinc-400 text-[10px]">품종</Typography>
                  <input
                    type="text"
                    value={breed}
                    onChange={(e) => setBreed(e.target.value)}
                    placeholder="품종을 입력하세요 (예: 골든리트리버)"
                    className="w-full bg-background border border-card-border rounded-2xl px-4 py-3 text-sm font-medium focus:outline-none focus:ring-2 ring-amber-500/20"
                  />
                </div>

                <div className="space-y-2">
                  <Typography variant="label" className="text-zinc-400 text-[10px]">마지막 목격 시간</Typography>
                  <input
                    type="datetime-local"
                    value={lastSeenAt}
                    onChange={(e) => setLastSeenAt(e.target.value)}
                    className="w-full bg-background border border-card-border rounded-2xl px-4 py-3 text-sm font-medium focus:outline-none focus:ring-2 ring-amber-500/20"
                  />
                </div>

                <div className="space-y-2">
                  <Typography variant="label" className="text-zinc-400 text-[10px]">마지막 목격 장소 *</Typography>
                  <div className="flex items-center gap-2">
                    <div className="flex-1 bg-background border border-card-border rounded-2xl px-4 py-3 text-sm font-medium text-zinc-600 min-h-[44px]">
                      {lastSeenLocation || '주소를 검색해 주세요'}
                    </div>
                    <button
                      type="button"
                      onClick={() => setShowAddressSearch(!showAddressSearch)}
                      className="shrink-0 px-4 py-3 rounded-2xl text-xs font-black bg-zinc-100 hover:bg-zinc-200 transition-colors"
                    >
                      {showAddressSearch ? '닫기' : '주소 검색'}
                    </button>
                  </div>
                  {showAddressSearch && (
                    <div className="border border-zinc-200 rounded-2xl overflow-hidden mt-2" style={{ height: 400 }}>
                      <DaumPostcode
                        onComplete={handleAddressComplete}
                        style={{ height: '100%' }}
                      />
                    </div>
                  )}
                </div>

                <div className="space-y-2">
                  <Typography variant="label" className="text-zinc-400 text-[10px]">상세 설명</Typography>
                  <textarea
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    placeholder="아이의 특징, 상황 등을 자세히 적어주세요."
                    className="w-full h-28 bg-background border border-card-border rounded-2xl p-4 text-sm font-medium focus:outline-none focus:ring-2 ring-amber-500/20 resize-none no-scrollbar"
                  />
                </div>

                <Button
                  variant="secondary"
                  fullWidth
                  size="lg"
                  className="shadow-lg gap-3 text-base py-5"
                  onClick={handleLostSubmit}
                  disabled={isSubmitting}
                >
                  {isSubmitting ? (
                    <Loader2 className="animate-spin" size={20} />
                  ) : (
                    <PlusCircle size={20} />
                  )}
                  <Typography variant="body" className="font-black">
                    {isSubmitting ? '등록 중...' : '실종 등록'}
                  </Typography>
                </Button>
              </div>
            )}

            {/* FOUND mode form fields */}
            {mode === 'FOUND' && (
              <div className="space-y-6">
                <div className="space-y-2">
                  <Typography variant="label" className="text-zinc-400 text-[10px]">발견 장소 *</Typography>
                  <div className="flex items-center gap-2">
                    <div className="flex-1 bg-background border border-card-border rounded-2xl px-4 py-3 text-sm font-medium text-zinc-600 min-h-[44px]">
                      {foundLocation || '주소를 검색해 주세요'}
                    </div>
                    <button
                      type="button"
                      onClick={() => setShowAddressSearch(!showAddressSearch)}
                      className="shrink-0 px-4 py-3 rounded-2xl text-xs font-black bg-zinc-100 hover:bg-zinc-200 transition-colors"
                    >
                      {showAddressSearch ? '닫기' : '주소 검색'}
                    </button>
                  </div>
                  {showAddressSearch && (
                    <div className="border border-zinc-200 rounded-2xl overflow-hidden mt-2" style={{ height: 400 }}>
                      <DaumPostcode
                        onComplete={handleAddressComplete}
                        style={{ height: '100%' }}
                      />
                    </div>
                  )}
                </div>

                <div className="space-y-2">
                  <Typography variant="label" className="text-zinc-400 text-[10px]">발견 시간</Typography>
                  <input
                    type="datetime-local"
                    value={foundAt}
                    onChange={(e) => setFoundAt(e.target.value)}
                    className="w-full bg-background border border-card-border rounded-2xl px-4 py-3 text-sm font-medium focus:outline-none focus:ring-2 ring-amber-500/20"
                  />
                </div>

                <div className="space-y-2">
                  <Typography variant="label" className="text-zinc-400 text-[10px]">메모 (선택)</Typography>
                  <textarea
                    value={memo}
                    onChange={(e) => setMemo(e.target.value)}
                    placeholder="아이를 마주친 상황이나 특징을 적어주세요."
                    className="w-full h-28 bg-background border border-card-border rounded-2xl p-4 text-sm font-medium focus:outline-none focus:ring-2 ring-amber-500/20 resize-none no-scrollbar"
                  />
                </div>

                <Button
                  variant="secondary"
                  fullWidth
                  size="lg"
                  className="shadow-lg gap-3 text-base py-5"
                  onClick={handleFoundSubmit}
                  disabled={isSubmitting}
                >
                  {isSubmitting ? (
                    <Loader2 className="animate-spin" size={20} />
                  ) : (
                    <Megaphone size={20} />
                  )}
                  <Typography variant="body" className="font-black">
                    {isSubmitting ? '등록 중...' : '제보 등록'}
                  </Typography>
                </Button>
              </div>
            )}
          </div>
        )}
      </Card>
    </div>
  );
};
