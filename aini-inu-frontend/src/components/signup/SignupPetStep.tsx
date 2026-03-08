'use client';

import React, { useState } from 'react';
import { ArrowLeft, SkipForward } from 'lucide-react';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { toast } from 'sonner';
import { createPet } from '@/api/pets';
import { PetForm } from '@/components/profile/PetForm';
import type { PetCreateRequest } from '@/api/pets';

interface SignupPetStepProps {
  onComplete: () => void;
  onSkip: () => void;
  onPrev: () => void;
}

export const SignupPetStep: React.FC<SignupPetStepProps> = ({
  onComplete,
  onSkip,
  onPrev,
}) => {
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (data: PetCreateRequest) => {
    setIsSubmitting(true);
    try {
      await createPet({ ...data, isMain: true });
      onComplete();
    } catch {
      toast.error('반려동물 등록에 실패했습니다.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="space-y-10 animate-in fade-in duration-500">
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

      <PetForm
        mode="create"
        onSubmit={handleSubmit}
        onCancel={onPrev}
        isSubmitting={isSubmitting}
      />

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
    </div>
  );
};
