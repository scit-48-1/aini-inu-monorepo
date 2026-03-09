'use client';

import React, { useState, useRef, useEffect } from 'react';
import { ChevronLeft, Zap, UserCircle, MoreVertical, CheckCircle, LogOut, ExternalLink } from 'lucide-react';
import { Typography } from '@/components/ui/Typography';
import { Badge } from '@/components/ui/Badge';
import { cn } from '@/lib/utils';

import type { ChatRoomDetailResponse, WalkConfirmResponse } from '@/api/chat';
import { UserAvatar } from '@/components/common/UserAvatar';
import { GroupAvatar } from '@/components/common/GroupAvatar';

interface ChatHeaderProps {
  room: ChatRoomDetailResponse;
  currentMemberId: number;
  onBack: () => void;
  onShowInfoToggle: () => void;
  showInfo: boolean;
  connectionMode: 'ws' | 'polling' | 'disconnected';
  walkConfirmState: WalkConfirmResponse | null;
  onConfirmWalk: () => void;
  onCancelConfirm: () => void;
  onLeave: () => void;
  onTitleClick?: () => void;
}

function getPartnerDisplay(
  room: ChatRoomDetailResponse,
  currentMemberId: number,
): { label: string; petNames: string[]; profileImages: (string | null)[]; isGroup: boolean } {
  const others = room.participants.filter(
    (p) => p.memberId !== currentMemberId && !p.left,
  );

  if (room.chatType === 'INDIVIDUAL' || others.length === 1) {
    const partner = others[0];
    const petNames = partner?.pets?.map((p) => p.name) ?? [];
    const nickname = partner?.nickname;
    return {
      label: nickname || `Member ${partner?.memberId ?? ''}`,
      petNames,
      profileImages: [partner?.profileImageUrl ?? null],
      isGroup: false,
    };
  }

  // GROUP rooms: show nicknames of other participants
  const nicknames = others
    .map((p) => p.nickname || `Member ${p.memberId}`)
    .slice(0, 3);
  const allPets = others.flatMap((p) => p.pets.map((pet) => pet.name));
  return {
    label: nicknames.join(', ') || `Group (${others.length})`,
    petNames: allPets,
    profileImages: others.slice(0, 4).map((p) => p.profileImageUrl),
    isGroup: true,
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
  walkConfirmState,
  onConfirmWalk,
  onCancelConfirm,
  onLeave,
  onTitleClick,
}) => {
  const { label, petNames, profileImages, isGroup } = getPartnerDisplay(room, currentMemberId);
  const isWalkRoom = room.origin === 'WALK' && !!room.roomTitle;
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  // Close menu on outside click
  useEffect(() => {
    if (!menuOpen) return;
    function handleClickOutside(e: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setMenuOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [menuOpen]);

  const isConfirmed = walkConfirmState?.myState === 'CONFIRMED';
  const allConfirmed = walkConfirmState?.allConfirmed === true;
  const confirmedCount = walkConfirmState?.confirmedMemberIds?.length ?? 0;
  const totalCount = room.participants.filter((p) => !p.left).length;

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
          onClick={isWalkRoom ? onTitleClick : onShowInfoToggle}
        >
          <div className="relative">
            <div className="w-10 h-10 md:w-12 md:h-12 shadow-lg group-hover:scale-105 transition-transform duration-500">
              {isGroup ? (
                <GroupAvatar images={profileImages} size="md" className="w-full h-full" />
              ) : (
                <img
                  src={profileImages[0] || '/AINIINU_ROGO_B.png'}
                  alt={label}
                  className="w-full h-full object-cover rounded-full"
                />
              )}
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
                {isWalkRoom ? room.roomTitle : label}
              </Typography>
              {isWalkRoom && (
                <ExternalLink size={14} className="text-zinc-400 group-hover:text-amber-500 transition-colors shrink-0" />
              )}
              {room.origin === 'WALK' && allConfirmed ? (
                <Badge
                  variant="emerald"
                  className="bg-emerald-50 text-emerald-600 border-none text-[7px] px-1 py-0 shrink-0"
                >
                  산책 확정!
                </Badge>
              ) : room.origin === 'WALK' && room.walkConfirmed ? (
                <Badge
                  variant="emerald"
                  className="bg-emerald-50 text-emerald-600 border-none text-[7px] px-1 py-0 shrink-0"
                >
                  MATCHED
                </Badge>
              ) : null}
            </div>
            <div className="flex items-center gap-1.5 text-zinc-400 font-bold text-[9px] uppercase tracking-widest truncate">
              {isWalkRoom ? (
                <span className="truncate group-hover:text-amber-500 transition-colors">
                  산책 스레드 보기
                </span>
              ) : (
                <>
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
                </>
              )}
            </div>
          </div>
        </div>
      </div>

      <div className="flex items-center gap-2">
        {/* Walk confirm button -- only for WALK origin rooms */}
        {room.origin === 'WALK' && (
          <button
            onClick={() => {
              if (allConfirmed) return;
              if (isConfirmed) {
                onCancelConfirm();
              } else {
                onConfirmWalk();
              }
            }}
            disabled={allConfirmed}
            className={cn(
              'p-2.5 rounded-xl transition-all flex items-center gap-1.5',
              allConfirmed
                ? 'bg-emerald-100 text-emerald-600 cursor-not-allowed opacity-80'
                : isConfirmed
                  ? 'bg-emerald-50 text-emerald-600 hover:bg-emerald-100'
                  : 'bg-zinc-50 text-zinc-400 hover:bg-zinc-100 hover:text-emerald-600',
            )}
            title={allConfirmed ? '산책 확정 완료' : isConfirmed ? '산책 확인 취소' : '산책 확인'}
          >
            <CheckCircle size={20} />
            <span className="text-[10px] font-black">
              {allConfirmed ? '확정' : isConfirmed ? '확인됨' : '산책완료'} {confirmedCount}/{totalCount}
            </span>
          </button>
        )}

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

        {/* More menu with leave action */}
        <div className="relative" ref={menuRef}>
          <button
            onClick={() => setMenuOpen(!menuOpen)}
            className="p-2.5 bg-zinc-50 rounded-xl text-zinc-400 hover:bg-zinc-100 hover:text-navy-900 transition-all"
          >
            <MoreVertical size={20} />
          </button>

          {menuOpen ? (
            <div className="absolute right-0 top-full mt-2 w-44 bg-white rounded-2xl shadow-2xl border border-zinc-100 py-2 z-50 animate-in fade-in zoom-in-95 duration-200">
              <button
                onClick={() => {
                  setMenuOpen(false);
                  onLeave();
                }}
                className="flex items-center gap-3 w-full px-4 py-3 text-sm font-bold text-red-500 hover:bg-red-50 transition-colors"
              >
                <LogOut size={16} />
                채팅방 나가기
              </button>
            </div>
          ) : null}
        </div>
      </div>
    </header>
  );
};
