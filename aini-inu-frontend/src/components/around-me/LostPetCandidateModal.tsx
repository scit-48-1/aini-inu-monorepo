'use client';

import React, { useState } from 'react';
import { X } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { toast } from 'sonner';
import { Typography } from '@/components/ui/Typography';
import { AICandidateList } from '@/components/around-me/AICandidateList';
import { approveMatch } from '@/api/lostPets';
import type {
  LostPetAnalyzeCandidateResponse,
  LostPetMatchCandidateResponse,
} from '@/api/lostPets';

interface LostPetCandidateModalProps {
  lostPetId: number;
  sessionId: number;
  candidates: (LostPetAnalyzeCandidateResponse | LostPetMatchCandidateResponse)[];
  onClose: () => void;
}

export const LostPetCandidateModal: React.FC<LostPetCandidateModalProps> = ({
  lostPetId,
  sessionId,
  candidates,
  onClose,
}) => {
  const router = useRouter();
  const [approving, setApproving] = useState(false);

  const handleApprove = async (sightingId: number) => {
    setApproving(true);
    try {
      const result = await approveMatch(lostPetId, { sessionId, sightingId });
      toast.success('채팅방이 생성되었습니다!');
      router.push(`/chat/${result.chatRoomId}`);
    } catch {
      // apiClient handles error toast
    } finally {
      setApproving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-[2000] bg-white overflow-y-auto">
      {/* Header */}
      <div className="sticky top-0 z-10 bg-white/95 backdrop-blur border-b border-zinc-100 px-6 py-4 flex items-center justify-between">
        <Typography variant="h3" className="text-lg font-black text-navy-900">
          AI 매칭 결과
        </Typography>
        <button
          onClick={onClose}
          className="w-10 h-10 rounded-full bg-zinc-100 flex items-center justify-center hover:bg-zinc-200 transition-colors"
        >
          <X size={20} />
        </button>
      </div>

      {/* Body */}
      <div className="px-6 pb-20">
        <AICandidateList
          candidates={candidates}
          onApprove={handleApprove}
          approving={approving}
        />
      </div>
    </div>
  );
};
