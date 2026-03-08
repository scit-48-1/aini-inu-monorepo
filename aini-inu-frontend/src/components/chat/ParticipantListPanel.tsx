'use client';

import React from 'react';
import { ChevronRight, Zap } from 'lucide-react';
import { Typography } from '@/components/ui/Typography';
import { cn } from '@/lib/utils';
import type { ChatParticipantResponse } from '@/api/chat';
import { useRouter } from 'next/navigation';

interface ParticipantListPanelProps {
  participants: ChatParticipantResponse[];
  currentMemberId: number;
  isOpen: boolean;
  onClose: () => void;
}

const WALK_CONFIRM_LABELS: Record<string, { text: string; className: string }> = {
  CONFIRMED: { text: '확인됨', className: 'bg-emerald-50 text-emerald-600' },
  PENDING: { text: '대기중', className: 'bg-zinc-50 text-zinc-400' },
};

export const ParticipantListPanel: React.FC<ParticipantListPanelProps> = ({
  participants,
  currentMemberId,
  isOpen,
  onClose,
}) => {
  const router = useRouter();
  const activeParticipants = participants.filter((p) => !p.left);
  const leftParticipants = participants.filter((p) => p.left);

  return (
    <aside
      className={cn(
        'h-full bg-white flex flex-col border-l border-zinc-100',
        'transition-all duration-500 ease-in-out',
        'fixed inset-y-0 right-0 z-[100] w-full sm:w-[400px] shadow-2xl',
        isOpen ? 'translate-x-0' : 'translate-x-full',
        'xl:relative xl:z-0 xl:shadow-none xl:translate-x-0 xl:overflow-hidden',
        isOpen ? 'xl:w-1/2 xl:shrink-0 xl:opacity-100' : 'xl:w-0 xl:opacity-0',
      )}
    >
      {/* Panel header */}
      <div className="p-4 border-b border-zinc-50 flex items-center justify-between bg-white/80 backdrop-blur-md shrink-0 z-20">
        <button
          onClick={onClose}
          className="p-2 -ml-2 text-zinc-300 hover:text-navy-900 transition-all hover:scale-110 active:scale-90 flex items-center gap-1 group"
        >
          <ChevronRight
            size={24}
            strokeWidth={3}
            className="group-hover:translate-x-1 transition-transform"
          />
          <Typography
            variant="label"
            className="text-[9px] font-black opacity-0 group-hover:opacity-100 transition-opacity"
          >
            Close
          </Typography>
        </button>
        <Typography
          variant="label"
          className="text-[10px] font-black text-zinc-400 uppercase tracking-[0.3em]"
        >
          Participants ({activeParticipants.length})
        </Typography>
      </div>

      {/* Participant list */}
      <div className="flex-1 overflow-y-auto no-scrollbar">
        <div className="p-4 space-y-2">
          {activeParticipants.map((p) => (
            <button
              key={p.memberId}
              onClick={() => router.push(`/profile/${p.memberId}`)}
              className="w-full flex items-center gap-3 p-3 rounded-2xl hover:bg-zinc-50 transition-colors text-left group"
            >
              <img
                src={p.profileImageUrl || '/AINIINU_ROGO_B.png'}
                alt={p.nickname || ''}
                className="w-11 h-11 rounded-full object-cover shadow-sm group-hover:scale-105 transition-transform"
              />
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2">
                  <Typography
                    variant="body"
                    className="font-bold text-navy-900 truncate text-sm"
                  >
                    {p.nickname || `Member ${p.memberId}`}
                    {p.memberId === currentMemberId && (
                      <span className="text-zinc-400 font-normal ml-1">(나)</span>
                    )}
                  </Typography>
                  {p.walkConfirmState && (
                    <span
                      className={cn(
                        'text-[9px] font-black px-1.5 py-0.5 rounded-md shrink-0',
                        WALK_CONFIRM_LABELS[p.walkConfirmState]?.className ??
                          'bg-zinc-50 text-zinc-400',
                      )}
                    >
                      {WALK_CONFIRM_LABELS[p.walkConfirmState]?.text ?? p.walkConfirmState}
                    </span>
                  )}
                </div>
                {p.pets.length > 0 && (
                  <div className="flex items-center gap-1 text-zinc-400 text-[10px] font-bold mt-0.5">
                    <Zap size={8} className="text-amber-500" />
                    <span className="truncate">
                      {p.pets.map((pet) => pet.name).join(', ')}
                    </span>
                  </div>
                )}
              </div>
              <ChevronRight
                size={16}
                className="text-zinc-200 group-hover:text-zinc-400 transition-colors shrink-0"
              />
            </button>
          ))}
        </div>

        {/* Left participants */}
        {leftParticipants.length > 0 && (
          <div className="px-4 pb-4">
            <Typography
              variant="label"
              className="text-[9px] font-black text-zinc-300 uppercase tracking-[0.2em] mb-2 block"
            >
              나간 참여자
            </Typography>
            <div className="space-y-1">
              {leftParticipants.map((p) => (
                <div
                  key={p.memberId}
                  className="flex items-center gap-3 p-3 rounded-2xl opacity-50"
                >
                  <img
                    src={p.profileImageUrl || '/AINIINU_ROGO_B.png'}
                    alt={p.nickname || ''}
                    className="w-9 h-9 rounded-full object-cover grayscale"
                  />
                  <Typography
                    variant="body"
                    className="text-zinc-400 text-sm truncate"
                  >
                    {p.nickname || `Member ${p.memberId}`}
                  </Typography>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </aside>
  );
};
