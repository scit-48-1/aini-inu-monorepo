'use client';

import React from 'react';
import { ChevronLeft, Zap, UserCircle, MoreVertical } from 'lucide-react';
import { Typography } from '@/components/ui/Typography';
import { Badge } from '@/components/ui/Badge';
import { cn } from '@/lib/utils';
import type { ChatRoomDetailResponse } from '@/api/chat';

interface ChatHeaderProps {
  room: ChatRoomDetailResponse;
  currentMemberId: number;
  onBack: () => void;
  onShowInfoToggle: () => void;
  showInfo: boolean;
  connectionMode: 'ws' | 'polling' | 'disconnected';
  // Walk confirm and leave actions added in Plan 03
  onLeave?: () => void;
  onWalkConfirm?: () => void;
}

function getPartnerDisplay(
  room: ChatRoomDetailResponse,
  currentMemberId: number,
): { label: string; petNames: string[] } {
  const others = room.participants.filter(
    (p) => p.memberId !== currentMemberId && !p.left,
  );

  if (room.chatType === 'INDIVIDUAL' || others.length === 1) {
    const partner = others[0];
    const petNames = partner?.pets?.map((p) => p.name) ?? [];
    return {
      label: petNames.length > 0 ? petNames.join(', ') : `Member ${partner?.memberId ?? ''}`,
      petNames,
    };
  }

  // GROUP rooms: show first 2-3 pet names across all other participants
  const allPets = others.flatMap((p) => p.pets.map((pet) => pet.name));
  const display = allPets.slice(0, 3).join(', ');
  return {
    label: display || `Group (${others.length})`,
    petNames: allPets,
  };
}

const CONNECTION_DOT: Record<string, string> = {
  ws: 'bg-emerald-500',
  polling: 'bg-amber-500',
  disconnected: 'bg-red-400',
};

export const ChatHeader: React.FC<ChatHeaderProps> = ({
  room,
  currentMemberId,
  onBack,
  onShowInfoToggle,
  showInfo,
  connectionMode,
}) => {
  const { label, petNames } = getPartnerDisplay(room, currentMemberId);

  return (
    <header className="p-4 md:p-6 bg-white/80 backdrop-blur-xl border-b border-zinc-100 flex items-center justify-between z-20 sticky top-0">
      <div className="flex items-center gap-3 md:gap-4">
        <button
          onClick={onBack}
          className="p-2 -ml-2 text-zinc-300 hover:text-navy-900 transition-all hover:scale-110 active:scale-90 min-[672px]:hidden"
        >
          <ChevronLeft size={24} strokeWidth={3} />
        </button>

        <div
          className="flex items-center gap-3 cursor-pointer group"
          onClick={onShowInfoToggle}
        >
          <div className="relative">
            <div className="w-10 h-10 md:w-12 md:h-12 rounded-[16px] shadow-lg group-hover:scale-105 transition-transform duration-500 p-0">
              <img
                src="/AINIINU_ROGO_B.png"
                alt={label}
                className="w-full h-full object-cover rounded-[16px]"
              />
            </div>
            {/* Connection mode indicator */}
            <div
              className={cn(
                'absolute -bottom-0.5 -right-0.5 w-4 h-4 border-3 border-white rounded-full shadow-sm',
                CONNECTION_DOT[connectionMode],
              )}
            />
          </div>

          <div className="space-y-0.5 min-w-0">
            <div className="flex items-center gap-2">
              <Typography
                variant="h3"
                className="text-base md:text-lg font-black text-navy-900 leading-tight truncate"
              >
                {label}
              </Typography>
              {room.walkConfirmed && (
                <Badge
                  variant="emerald"
                  className="bg-emerald-50 text-emerald-600 border-none text-[7px] px-1 py-0 shrink-0"
                >
                  MATCHED
                </Badge>
              )}
            </div>
            <div className="flex items-center gap-1.5 text-zinc-400 font-bold text-[9px] uppercase tracking-widest truncate">
              {petNames[0] ? (
                <>
                  <span className="flex items-center gap-1 shrink-0">
                    <Zap size={8} className="text-amber-500" /> {petNames[0]}
                  </span>
                  <span className="w-0.5 h-0.5 rounded-full bg-zinc-200 shrink-0" />
                </>
              ) : null}
              <span className="truncate group-hover:text-amber-500 transition-colors">
                상세 정보
              </span>
            </div>
          </div>
        </div>
      </div>

      <div className="flex items-center gap-2">
        <button
          onClick={onShowInfoToggle}
          className={cn(
            'p-2.5 rounded-xl transition-all flex',
            showInfo
              ? 'bg-navy-900 text-white shadow-xl'
              : 'bg-zinc-50 text-zinc-400 hover:bg-zinc-100',
          )}
          title="정보 패널 토글"
        >
          <UserCircle size={20} />
        </button>

        <button className="p-2.5 bg-zinc-50 rounded-xl text-zinc-400 hover:bg-zinc-100 hover:text-navy-900 transition-all">
          <MoreVertical size={20} />
        </button>
      </div>
    </header>
  );
};
