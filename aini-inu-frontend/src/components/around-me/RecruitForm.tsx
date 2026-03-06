'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import {
  MapPin,
  Clock,
  Users,
  Check,
  AlertCircle,
  Loader2,
  PlusCircle,
  CheckCircle2,
  Info,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import { toast } from 'sonner';
import DaumPostcode from 'react-daum-postcode';
import { createThread, updateThread, getThread } from '@/api/threads';
import type { PetResponse } from '@/api/pets';

// ---------------------------------------------------------------------------
// Props
// ---------------------------------------------------------------------------

interface RecruitFormProps {
  myPets: PetResponse[];
  editingThreadId: number | null;
  coordinates: [number, number];
  onSuccess: () => void;
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export const RecruitForm: React.FC<RecruitFormProps> = ({
  myPets,
  editingThreadId,
  coordinates,
  onSuccess,
}) => {
  const today = new Date().toISOString().slice(0, 10);

  const [form, setForm] = useState({
    title: '',
    walkDate: '',
    startTime: '',
    endTime: '',
    description: '',
    chatType: 'INDIVIDUAL' as 'INDIVIDUAL' | 'GROUP',
    maxParticipants: 5,
    placeName: '',
    latitude: coordinates[0],
    longitude: coordinates[1],
    address: '',
  });
  const [selectedPetIds, setSelectedPetIds] = useState<number[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [showAddressSearch, setShowAddressSearch] = useState(false);
  const [isFetchingEdit, setIsFetchingEdit] = useState(false);

  // Chat type tooltip display
  const [showTooltip, setShowTooltip] = useState<'INDIVIDUAL' | 'GROUP' | null>(null);

  // -------------------------------------------------------------------------
  // Edit mode: fetch existing thread and pre-fill
  // -------------------------------------------------------------------------
  useEffect(() => {
    if (editingThreadId === null) return;

    setIsFetchingEdit(true);
    getThread(editingThreadId)
      .then((thread) => {
        // Extract HH:mm from ISO startTime/endTime for time inputs
        const toTimeValue = (iso: string) => {
          if (!iso) return '';
          // ISO: 2024-05-10T09:00:00 or 09:00:00
          const timePart = iso.includes('T') ? iso.split('T')[1] : iso;
          return timePart.slice(0, 5); // HH:mm
        };
        const toDateValue = (iso: string) => {
          if (!iso) return '';
          // Could be a date already or ISO
          return iso.includes('T') ? iso.split('T')[0] : iso.slice(0, 10);
        };

        setForm({
          title: thread.title ?? '',
          walkDate: toDateValue(thread.walkDate ?? thread.startTime ?? ''),
          startTime: toTimeValue(thread.startTime ?? ''),
          endTime: toTimeValue(thread.endTime ?? ''),
          description: thread.description ?? '',
          chatType: (thread.chatType === 'GROUP' ? 'GROUP' : 'INDIVIDUAL') as 'INDIVIDUAL' | 'GROUP',
          maxParticipants: thread.maxParticipants ?? 5,
          placeName: thread.placeName ?? '',
          latitude: thread.latitude ?? coordinates[0],
          longitude: thread.longitude ?? coordinates[1],
          address: thread.address ?? '',
        });
        setSelectedPetIds(thread.petIds ?? []);
      })
      .catch(() => {
        toast.error('모집글 정보를 불러오는 데 실패했습니다.');
      })
      .finally(() => setIsFetchingEdit(false));
  }, [editingThreadId]); // eslint-disable-line react-hooks/exhaustive-deps

  // -------------------------------------------------------------------------
  // Derived state
  // -------------------------------------------------------------------------

  const isEditing = editingThreadId !== null;

  const composedStartTime = form.walkDate && form.startTime
    ? `${form.walkDate}T${form.startTime}:00`
    : '';
  const composedEndTime = form.walkDate && form.endTime
    ? `${form.walkDate}T${form.endTime}:00`
    : '';

  const descriptionOverLimit = form.description.length > 500;

  const isSubmitDisabled =
    isSubmitting ||
    form.title.length === 0 ||
    form.title.length > 30 ||
    form.walkDate === '' ||
    form.startTime === '' ||
    selectedPetIds.length === 0 ||
    descriptionOverLimit;

  // -------------------------------------------------------------------------
  // Non-pet-owner block (DEC-008)
  // -------------------------------------------------------------------------
  if (myPets.length === 0) {
    return (
      <div className="max-w-4xl mx-auto w-full py-6 animate-in fade-in zoom-in-95 duration-700">
        <Card className="p-10 bg-white shadow-2xl rounded-[40px] text-center space-y-6">
          <div className="w-16 h-16 bg-amber-50 text-amber-500 rounded-full flex items-center justify-center mx-auto">
            <AlertCircle size={32} />
          </div>
          <div className="space-y-2">
            <Typography variant="h3" className="text-xl font-black text-navy-900">
              반려견을 등록해야 모집글을 작성할 수 있어요
            </Typography>
            <Typography variant="body" className="text-zinc-400 text-sm">
              프로필에서 반려견을 먼저 등록해 주세요.
            </Typography>
          </div>
          <Link
            href="/profile"
            className="inline-block bg-amber-500 text-white font-black text-sm px-8 py-3 rounded-[20px] hover:bg-amber-600 transition-colors"
          >
            프로필에서 등록하기
          </Link>
        </Card>
      </div>
    );
  }

  // -------------------------------------------------------------------------
  // Loading spinner while fetching edit data
  // -------------------------------------------------------------------------
  if (isFetchingEdit) {
    return (
      <div className="max-w-4xl mx-auto w-full py-6 flex items-center justify-center min-h-[300px]">
        <Loader2 size={40} className="animate-spin text-amber-500" />
      </div>
    );
  }

  // -------------------------------------------------------------------------
  // Submit handler
  // -------------------------------------------------------------------------
  const handleSubmit = async () => {
    setIsSubmitting(true);
    try {
      if (isEditing) {
        const patch: Record<string, unknown> = {};
        if (form.title) patch.title = form.title;
        if (form.description !== undefined) patch.description = form.description;
        if (form.walkDate) patch.walkDate = form.walkDate;
        if (composedStartTime) patch.startTime = composedStartTime;
        if (composedEndTime) patch.endTime = composedEndTime;
        patch.chatType = form.chatType;
        patch.maxParticipants = form.maxParticipants;
        if (form.placeName || form.address) {
          patch.location = {
            placeName: form.placeName || '현재 위치',
            latitude: form.latitude,
            longitude: form.longitude,
            address: form.address || form.placeName,
          };
        }
        patch.petIds = selectedPetIds;

        await updateThread(editingThreadId!, patch);
      } else {
        await createThread({
          title: form.title,
          description: form.description || undefined,
          walkDate: form.walkDate,
          startTime: composedStartTime,
          endTime: composedEndTime,
          chatType: form.chatType,
          maxParticipants: form.maxParticipants,
          location: {
            placeName: form.placeName || '현재 위치',
            latitude: form.latitude,
            longitude: form.longitude,
            address: form.address || form.placeName || '현재 위치',
          },
          petIds: selectedPetIds,
        });
      }

      setIsSuccess(true);
      setTimeout(() => {
        onSuccess();
      }, 2000);
    } catch {
      toast.error(isEditing ? '모집글 수정에 실패했습니다.' : '모집글 등록에 실패했습니다.');
    } finally {
      setIsSubmitting(false);
    }
  };

  // -------------------------------------------------------------------------
  // Pet toggle helper
  // -------------------------------------------------------------------------
  const togglePet = (id: number) => {
    setSelectedPetIds((prev) =>
      prev.includes(id) ? prev.filter((i) => i !== id) : [...prev, id],
    );
  };

  // -------------------------------------------------------------------------
  // DaumPostcode complete handler
  // -------------------------------------------------------------------------
  const handleAddressComplete = (data: { address: string; buildingName?: string }) => {
    setForm((f) => ({
      ...f,
      placeName: data.buildingName || data.address,
      address: data.address,
      // Keep lat/lng from GPS coordinates
    }));
    setShowAddressSearch(false);
  };

  // -------------------------------------------------------------------------
  // Success screen
  // -------------------------------------------------------------------------
  if (isSuccess) {
    return (
      <div className="max-w-4xl mx-auto w-full py-6 animate-in fade-in zoom-in-95 duration-700">
        <Card className="p-10 bg-white shadow-2xl rounded-[40px] text-center space-y-6">
          <div className="w-20 h-20 bg-amber-50 text-amber-500 rounded-full flex items-center justify-center mx-auto">
            <Check size={40} strokeWidth={3} />
          </div>
          <Typography variant="h2" className="text-2xl">
            {isEditing ? '모집글이 수정되었어요!' : '모집 등록 완료!'}
          </Typography>
          <Typography variant="body" className="text-zinc-400 text-sm">
            잠시 후 목록으로 이동합니다.
          </Typography>
        </Card>
      </div>
    );
  }

  // -------------------------------------------------------------------------
  // Main form
  // -------------------------------------------------------------------------
  return (
    <div className="max-w-4xl mx-auto w-full py-6 space-y-6 animate-in fade-in zoom-in-95 duration-700">
      <div className="text-center space-y-1">
        <Typography variant="h2" className="text-2xl">
          {isEditing ? (
            <>
              내 <span className="text-amber-500 italic">스레드</span> 수정하기
            </>
          ) : (
            <>
              함께 산책할 <span className="text-amber-500 italic">메이트</span>를 찾으시나요?
            </>
          )}
        </Typography>
        <Typography variant="body" className="text-zinc-400 text-sm">
          {isEditing
            ? '수정된 내용이 주변 이웃들에게 반영됩니다.'
            : '산책 정보를 등록하면 주변 이웃들에게 시각화되어 노출됩니다.'}
        </Typography>
      </div>

      <Card className="p-8 bg-white shadow-2xl rounded-[40px] space-y-8">
        {/* ---- Title ---- */}
        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <Typography variant="label" className="text-navy-900">제목 *</Typography>
            <span className={cn('text-[11px] font-bold', form.title.length > 30 ? 'text-red-500' : 'text-zinc-400')}>
              {form.title.length}/30
            </span>
          </div>
          <input
            type="text"
            placeholder="예: 오늘 오후에 공원 한바퀴 도실 분!"
            value={form.title}
            maxLength={30}
            onChange={(e) => setForm({ ...form, title: e.target.value })}
            className="w-full bg-white border border-zinc-200 rounded-2xl py-3 px-5 text-sm font-bold text-navy-900 focus:outline-none focus:border-amber-500 focus:ring-4 ring-amber-500/10 transition-all"
          />
        </div>

        {/* ---- Walk Date ---- */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="space-y-2">
            <Typography variant="label" className="text-navy-900 flex items-center gap-2">
              산책 날짜 *
            </Typography>
            <input
              type="date"
              value={form.walkDate}
              min={today}
              onChange={(e) => setForm({ ...form, walkDate: e.target.value })}
              className="w-full bg-white border border-zinc-200 rounded-2xl py-3 px-5 text-sm font-bold text-navy-900 focus:outline-none focus:border-amber-500 focus:ring-4 ring-amber-500/10 transition-all"
            />
          </div>

          {/* ---- Start Time ---- */}
          <div className="space-y-2">
            <Typography variant="label" className="text-navy-900 flex items-center gap-2">
              <Clock size={14} className="text-amber-500" /> 시작 시간 *
            </Typography>
            <input
              type="time"
              value={form.startTime}
              onChange={(e) => setForm({ ...form, startTime: e.target.value })}
              className="w-full bg-white border border-zinc-200 rounded-2xl py-3 px-5 text-sm font-bold text-navy-900 focus:outline-none focus:border-amber-500 focus:ring-4 ring-amber-500/10 transition-all"
            />
          </div>

          {/* ---- End Time ---- */}
          <div className="space-y-2">
            <Typography variant="label" className="text-navy-900 flex items-center gap-2">
              <Clock size={14} className="text-amber-500" /> 종료 시간
            </Typography>
            <input
              type="time"
              value={form.endTime}
              onChange={(e) => setForm({ ...form, endTime: e.target.value })}
              className="w-full bg-white border border-zinc-200 rounded-2xl py-3 px-5 text-sm font-bold text-navy-900 focus:outline-none focus:border-amber-500 focus:ring-4 ring-amber-500/10 transition-all"
            />
          </div>
        </div>

        {/* ---- Description ---- */}
        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <Typography variant="label" className="text-navy-900">상세 설명</Typography>
            <span
              className={cn(
                'text-[11px] font-bold',
                descriptionOverLimit ? 'text-red-500' : 'text-zinc-400',
              )}
            >
              {form.description.length}/500
            </span>
          </div>
          <textarea
            placeholder="자유롭게 적어주세요."
            value={form.description}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
            className={cn(
              'w-full h-28 bg-white border rounded-2xl p-5 text-sm font-medium text-navy-900 focus:outline-none focus:ring-4 ring-amber-500/10 resize-none transition-all',
              descriptionOverLimit
                ? 'border-red-400 focus:border-red-400'
                : 'border-zinc-200 focus:border-amber-500',
            )}
          />
          {descriptionOverLimit && (
            <p className="text-red-500 text-xs font-bold">설명은 500자 이내로 작성해 주세요.</p>
          )}
        </div>

        {/* ---- Chat Type Toggle ---- */}
        <div className="space-y-2">
          <Typography variant="label" className="text-navy-900">채팅 유형</Typography>
          <div className="flex gap-3">
            {(['INDIVIDUAL', 'GROUP'] as const).map((type) => {
              const label = type === 'INDIVIDUAL' ? '1:1 채팅' : '그룹 채팅';
              const tooltip =
                type === 'INDIVIDUAL'
                  ? '참여자와 개별 채팅방이 생성됩니다'
                  : '모든 참여자가 하나의 채팅방에 입장합니다';
              const isActive = form.chatType === type;
              return (
                <div key={type} className="relative">
                  <button
                    type="button"
                    onClick={() => setForm({ ...form, chatType: type })}
                    onMouseEnter={() => setShowTooltip(type)}
                    onMouseLeave={() => setShowTooltip(null)}
                    className={cn(
                      'flex items-center gap-2 px-5 py-2.5 rounded-[16px] text-sm font-black transition-all border-2',
                      isActive
                        ? 'bg-amber-500 text-white border-amber-500 shadow-md'
                        : 'bg-white text-zinc-400 border-zinc-200 hover:border-amber-300',
                    )}
                  >
                    {isActive && <Check size={14} strokeWidth={3} />}
                    {label}
                    <Info size={12} className={isActive ? 'text-white/70' : 'text-zinc-300'} />
                  </button>
                  {showTooltip === type && (
                    <div className="absolute top-full mt-2 left-0 z-10 bg-navy-900 text-white text-xs font-medium px-3 py-2 rounded-xl whitespace-nowrap shadow-lg">
                      {tooltip}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>

        {/* ---- Max Participants ---- */}
        <div className="space-y-2">
          <Typography variant="label" className="text-navy-900 flex items-center gap-2">
            <Users size={14} className="text-amber-500" /> 최대 참여 인원
          </Typography>
          <input
            type="number"
            min={2}
            value={form.maxParticipants}
            onChange={(e) =>
              setForm({ ...form, maxParticipants: Math.max(2, Number(e.target.value)) })
            }
            className="w-28 bg-white border border-zinc-200 rounded-2xl py-3 px-5 text-sm font-bold text-navy-900 focus:outline-none focus:border-amber-500 focus:ring-4 ring-amber-500/10 transition-all"
          />
        </div>

        {/* ---- Location ---- */}
        <div className="space-y-2">
          <Typography variant="label" className="text-navy-900 flex items-center gap-2">
            <MapPin size={14} className="text-amber-500" /> 장소
          </Typography>
          <div className="flex items-center gap-3">
            <div className="flex-1 bg-zinc-50 border border-zinc-200 rounded-2xl py-3 px-5 text-sm font-bold text-navy-900 truncate">
              {form.placeName || form.address || '현재 위치'}
            </div>
            <button
              type="button"
              onClick={() => setShowAddressSearch((v) => !v)}
              className="px-5 py-3 rounded-2xl bg-amber-50 text-amber-600 font-black text-sm hover:bg-amber-100 transition-colors border border-amber-200 shrink-0"
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

        {/* ---- Pet Selection ---- */}
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <Typography variant="label" className="text-navy-900 flex items-center gap-2">
              <Users size={14} className="text-amber-500" /> 참여 반려견 * (최소 1마리)
            </Typography>
            <span className="text-[10px] font-black text-amber-500">선택됨 {selectedPetIds.length}</span>
          </div>
          <div className="flex gap-3 flex-wrap">
            {myPets.map((pet) => {
              const isSelected = selectedPetIds.includes(pet.id);
              return (
                <button
                  type="button"
                  key={pet.id}
                  onClick={() => togglePet(pet.id)}
                  className={cn(
                    'flex flex-col items-center gap-1.5 p-2 rounded-[20px] border-2 cursor-pointer transition-all relative w-20',
                    isSelected
                      ? 'border-amber-500 ring-2 ring-amber-500/30 scale-105 bg-amber-50'
                      : 'border-transparent opacity-60 bg-zinc-50',
                  )}
                >
                  <div className="w-14 h-14 rounded-[14px] overflow-hidden bg-zinc-200 relative">
                    {pet.photoUrl ? (
                      <img
                        src={pet.photoUrl}
                        alt={pet.name}
                        className="w-full h-full object-cover"
                      />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center text-zinc-400 text-xs font-bold">
                        {pet.name.slice(0, 1)}
                      </div>
                    )}
                    {isSelected && (
                      <div className="absolute inset-0 bg-amber-500/20 flex items-center justify-center">
                        <CheckCircle2 className="text-white drop-shadow" size={22} />
                      </div>
                    )}
                  </div>
                  <span className="text-[11px] font-black text-navy-900 truncate w-full text-center">
                    {pet.name}
                  </span>
                </button>
              );
            })}
          </div>
        </div>

        {/* ---- Submit ---- */}
        <Button
          variant="secondary"
          fullWidth
          size="md"
          className="py-4 shadow-xl"
          onClick={handleSubmit}
          disabled={isSubmitDisabled}
        >
          {isSubmitting ? (
            <Loader2 className="animate-spin mr-2" />
          ) : (
            <PlusCircle className="mr-2" size={18} />
          )}
          {isSubmitting
            ? isEditing
              ? '수정 중...'
              : '등록 중...'
            : isEditing
            ? '수정 완료하기'
            : '모집 시작하기'}
        </Button>
      </Card>
    </div>
  );
};
