'use client';

import React, { useState } from 'react';
import { createPortal } from 'react-dom';
import { X, Dog } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import { toast } from 'sonner';
import { PetForm } from '@/components/profile/PetForm';
import { createPet, updatePet } from '@/api/pets';
import type { PetResponse, PetCreateRequest } from '@/api/pets';

interface DogRegisterModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSaved: () => void;
  editingPet?: PetResponse;
}

export const DogRegisterModal: React.FC<DogRegisterModalProps> = ({
  isOpen,
  onClose,
  onSaved,
  editingPet,
}) => {
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (!isOpen) return null;

  const handleSubmit = async (data: PetCreateRequest) => {
    setIsSubmitting(true);
    try {
      if (editingPet) {
        // Edit mode: build PetUpdateRequest from form data
        const updateData = {
          name: data.name,
          birthDate: data.birthDate,
          isNeutered: data.isNeutered,
          mbti: data.mbti,
          photoUrl: data.photoUrl,
          personalityIds: data.personalityIds,
          walkingStyleCodes: data.walkingStyles,
        };
        await updatePet(editingPet.id, updateData);
        toast.success('반려견 정보가 수정되었습니다.');
      } else {
        // Create mode
        await createPet(data);
        toast.success('반려견이 등록되었습니다.');
      }
      onSaved();
      onClose();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : '처리 중 오류가 발생했습니다.';
      toast.error(message);
    } finally {
      setIsSubmitting(false);
    }
  };

  return createPortal(
    <div
      className="fixed inset-0 z-[2000] bg-navy-900/40 backdrop-blur-md flex items-center justify-center p-6 animate-in fade-in duration-300"
      onClick={(e) => e.target === e.currentTarget && onClose()}
    >
      <Card className="w-full max-w-2xl p-0 bg-white shadow-2xl animate-in zoom-in-95 duration-500 border-none overflow-hidden flex flex-col max-h-[90vh] rounded-[48px]">
        <div className="flex items-center justify-between p-8 shrink-0 border-b border-zinc-50">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-amber-50 rounded-xl flex items-center justify-center text-amber-500">
              <Dog size={24} />
            </div>
            <Typography variant="h3" className="text-navy-900">
              {editingPet ? '반려견 정보 수정' : '새로운 반려견 등록'}
            </Typography>
          </div>
          <button onClick={onClose} className="p-2 text-zinc-300 hover:text-navy-900 transition-colors">
            <X size={32} />
          </button>
        </div>
        <div className="flex-1 overflow-y-auto p-8 no-scrollbar">
          <PetForm
            mode={editingPet ? 'edit' : 'create'}
            initialData={editingPet}
            onSubmit={handleSubmit}
            onCancel={onClose}
            isSubmitting={isSubmitting}
          />
        </div>
      </Card>
    </div>,
    document.body
  );
};
