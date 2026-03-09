'use client';

import React, { useState } from 'react';
import { createPortal } from 'react-dom';
import { X, ShieldCheck, Edit2, Zap, Target, Check, Crown, Loader2, Trash2, Brain } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import { toast } from 'sonner';
import { deletePet, setMainPet } from '@/api/pets';
import type { PetResponse } from '@/api/pets';
import { DeleteConfirmDialog } from '@/components/profile/DeleteConfirmDialog';
import { OptimizedImage } from '@/components/common/OptimizedImage';

interface DogDetailModalProps {
  isOpen: boolean;
  onClose: () => void;
  pet: PetResponse | null;
  onEdit?: () => void;
  onDeleted?: () => void;
  onMainChanged?: () => void;
  onZoom: (img: string) => void;
  readOnly?: boolean;
}

export const DogDetailModal: React.FC<DogDetailModalProps> = ({
  isOpen,
  onClose,
  pet,
  onEdit,
  onDeleted,
  onMainChanged,
  onZoom,
  readOnly = false,
}) => {
  const [isSettingMain, setIsSettingMain] = useState(false);
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);

  if (!isOpen || !pet) return null;

  const photoUrl = pet.photoUrl || '/AINIINU_ROGO_B.png';

  const handleSetMain = async () => {
    setIsSettingMain(true);
    try {
      await setMainPet(pet.id);
      toast.success('대표 반려견이 변경되었습니다.');
      onMainChanged?.();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : '대표 반려견 변경에 실패했습니다.';
      toast.error(message);
      // Refetch to restore correct state on failure
      onMainChanged?.();
    } finally {
      setIsSettingMain(false);
    }
  };

  const handleDeleteConfirm = async () => {
    setIsDeleting(true);
    try {
      await deletePet(pet.id);
      toast.success('반려견이 삭제되었습니다.');
      setIsDeleteDialogOpen(false);
      onDeleted?.();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : '삭제 중 오류가 발생했습니다.';
      toast.error(message);
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <>
      {createPortal(
        <div
          className="fixed inset-0 z-[3000] bg-black/60 backdrop-blur-sm flex items-center justify-center p-6 animate-in fade-in duration-300"
          onClick={(e) => e.target === e.currentTarget && onClose()}
        >
          <Card className="w-full max-w-lg overflow-hidden bg-white shadow-2xl animate-in zoom-in-95 duration-500 border-none rounded-[56px] flex flex-col max-h-[90vh]">
            {/* Hero Image */}
            <div
              className="h-80 relative shrink-0 group cursor-zoom-in"
              onClick={() => onZoom(photoUrl)}
            >
              <OptimizedImage
                src={photoUrl}
                alt={pet.name}
                fill
                sizes="(max-width: 512px) 100vw, 512px"
                priority
                className="object-cover transition-transform duration-500 group-hover:scale-105"
              />
              <button
                onClick={(e) => { e.stopPropagation(); onClose(); }}
                className="absolute top-6 right-6 w-12 h-12 bg-black/20 hover:bg-black/40 backdrop-blur-md text-white rounded-full flex items-center justify-center transition-all active:scale-95"
              >
                <X size={24} />
              </button>
              {!readOnly && onEdit && (
                <button
                  onClick={(e) => { e.stopPropagation(); onEdit(); }}
                  className="absolute top-6 left-6 w-8 h-8 bg-black/15 hover:bg-black/35 backdrop-blur-md text-white/60 hover:text-white rounded-full flex items-center justify-center transition-all active:scale-95"
                >
                  <Edit2 size={14} />
                </button>
              )}
              {pet.isMain && (
                <div className="absolute top-[72px] left-6 w-8 h-8 bg-amber-500 rounded-full flex items-center justify-center shadow-lg">
                  <Crown size={14} className="text-white" />
                </div>
              )}
              <div className="absolute bottom-0 left-0 right-0 p-10 bg-gradient-to-t from-black/90 via-black/40 to-transparent text-white pointer-events-none">
                <div className="flex items-center gap-3 mb-2">
                  <Typography variant="h2" className="text-white text-4xl leading-none">
                    {pet.name}
                  </Typography>
                  {pet.isCertified && (
                    <div className="bg-blue-500 text-white p-1.5 rounded-full shadow-lg border-2 border-white/20">
                      <ShieldCheck size={16} fill="currentColor" strokeWidth={3} />
                    </div>
                  )}
                </div>
                <Typography variant="body" className="text-white/80 font-black text-lg">
                  {pet.breed?.name} &bull; {pet.age != null ? `${pet.age}세` : ''} &bull; {pet.gender === 'MALE' ? '남아' : '여아'}
                </Typography>
              </div>
            </div>

            {/* Detail Content */}
            <div className="flex-1 overflow-y-auto p-10 space-y-8 no-scrollbar">
              {/* Personalities */}
              {(pet.personalities || []).length > 0 && (
                <div className="space-y-4">
                  <Typography variant="label" className="text-amber-500 flex items-center gap-2 font-black uppercase tracking-[0.2em] text-[10px]">
                    <Zap size={14} /> Character &amp; Tendencies
                  </Typography>
                  <div className="flex flex-wrap gap-2.5">
                    {pet.personalities.map((p) => (
                      <div key={p.id} className="bg-zinc-50 border border-zinc-100 text-zinc-600 px-4 py-2 rounded-2xl text-xs font-black shadow-sm">
                        #{p.name}
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Walk Style + Neutered */}
              <div className="grid grid-cols-2 gap-6">
                <div className="space-y-4 p-6 bg-zinc-50 rounded-[32px] border border-zinc-100">
                  <Typography variant="label" className="text-indigo-500 flex items-center gap-2 font-black uppercase tracking-widest text-[9px]">
                    <Target size={14} /> Walk Style
                  </Typography>
                  <Typography variant="body" className="text-navy-900 font-black text-base">
                    {(pet.walkingStyles || []).length > 0
                      ? pet.walkingStyles.join(', ')
                      : '—'}
                  </Typography>
                </div>
                <div className="space-y-4 p-6 bg-zinc-50 rounded-[32px] border border-zinc-100 flex flex-col justify-center">
                  <Typography variant="label" className="text-emerald-500 flex items-center gap-2 font-black uppercase tracking-widest text-[9px]">
                    <Check size={14} /> Neutered
                  </Typography>
                  <Typography variant="body" className="text-navy-900 font-black text-xl">
                    {pet.isNeutered ? '완료' : '미완료'}
                  </Typography>
                </div>
              </div>

              {/* MBTI */}
              {pet.mbti && (
                <div className="space-y-4 p-6 bg-zinc-50 rounded-[32px] border border-zinc-100">
                  <Typography variant="label" className="text-purple-500 flex items-center gap-2 font-black uppercase tracking-widest text-[9px]">
                    <Brain size={14} /> MBTI
                  </Typography>
                  <Typography variant="body" className="text-navy-900 font-black text-xl">
                    {pet.mbti}
                  </Typography>
                </div>
              )}

              {/* Action Buttons */}
              {!readOnly && (
                <div className="flex gap-3 pt-2">
                  {!pet.isMain && (
                    <Button
                      type="button"
                      variant="secondary"
                      size="lg"
                      className="flex-1"
                      onClick={handleSetMain}
                      disabled={isSettingMain}
                    >
                      {isSettingMain ? (
                        <Loader2 className="animate-spin" size={16} />
                      ) : (
                        <>
                          <Crown size={14} className="mr-1" />
                          대표 반려견 설정
                        </>
                      )}
                    </Button>
                  )}
                  <Button
                    type="button"
                    variant="danger"
                    size="lg"
                    className={pet.isMain ? 'flex-1' : ''}
                    onClick={() => setIsDeleteDialogOpen(true)}
                    disabled={isSettingMain}
                  >
                    <Trash2 size={14} className="mr-1" />
                    삭제
                  </Button>
                </div>
              )}
            </div>
          </Card>
        </div>,
        document.body
      )}

      {!readOnly && (
        <DeleteConfirmDialog
          isOpen={isDeleteDialogOpen}
          petName={pet.name}
          onConfirm={handleDeleteConfirm}
          onCancel={() => setIsDeleteDialogOpen(false)}
          isDeleting={isDeleting}
        />
      )}
    </>
  );
};
