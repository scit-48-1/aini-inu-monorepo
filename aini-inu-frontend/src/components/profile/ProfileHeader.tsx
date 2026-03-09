'use client';

import React from 'react';
import { ShieldCheck, Settings, UserPlus, UserCheck } from 'lucide-react';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import { NotificationBell } from '@/components/notification/NotificationBell';
import { MannerScoreGauge } from '@/components/common/MannerScoreGauge';
import { UserAvatar } from '@/components/common/UserAvatar';
import { cn } from '@/lib/utils';
import type { MemberResponse } from '@/api/members';

interface ProfileHeaderProps {
  member: MemberResponse | null;
  postCount: number;
  followerCount: number;
  followingCount: number;
  isAnyDogVerified: boolean;
  isMe: boolean;
  isFollowing?: boolean;
  hasRecentDiary?: boolean;
  /** true: 채팅 패널 등 좁은 컨테이너용 고정 레이아웃 */
  compact?: boolean;
  onEditClick: () => void;
  onSettingsClick: () => void;
  onFollowersClick: () => void;
  onFollowingClick: () => void;
  onFollowToggle?: () => void;
  isFollowLoading?: boolean;
  /** Optional slot for injecting walk stats heatmap below the header (used by MyProfileView) */
  walkStatsSlot?: React.ReactNode;
}

export const ProfileHeader: React.FC<ProfileHeaderProps> = ({
  member,
  postCount,
  followerCount,
  followingCount,
  isAnyDogVerified,
  isMe,
  isFollowing = false,
  hasRecentDiary = false,
  compact = false,
  onEditClick,
  onSettingsClick,
  onFollowersClick,
  onFollowingClick,
  onFollowToggle,
  isFollowLoading = false,
  walkStatsSlot,
}) => {
  if (!member) return null;

  const displayHandle = member.linkedNickname || member.nickname;

  return (
    <section className={cn(compact ? "p-5 space-y-4" : "p-6 lg:p-12 space-y-8")}>
      <div className={cn("flex items-start", compact ? "gap-4" : "gap-8 lg:gap-16")}>
        <div className="relative shrink-0">
          <UserAvatar
            src={member.profileImageUrl}
            alt={member.nickname}
            hasRecentDiary={hasRecentDiary}
            size={compact ? "lg" : "xl"}
          />
        </div>
        <div className={cn("flex-1 pt-2", compact ? "space-y-3" : "space-y-6 lg:space-y-8")}>
          {/* 닉네임 + 버튼 */}
          <div className={cn(
            "flex gap-3",
            compact ? "flex-col" : "flex-col lg:flex-row lg:items-center gap-4 lg:gap-6"
          )}>
            <div className="flex items-center gap-2">
              <Typography variant="h2" className={cn(
                "text-navy-900 lowercase tracking-tighter",
                compact ? "text-xl" : "text-2xl lg:text-3xl"
              )}>
                {member.nickname}
              </Typography>
              {isAnyDogVerified && (
                <div className="bg-blue-500 text-white p-1 rounded-full shadow-sm">
                  <ShieldCheck size={compact ? 10 : 14} fill="currentColor" strokeWidth={3} />
                </div>
              )}
            </div>
            <div className="flex gap-2 items-center">
              {isMe ? (
                <>
                  <NotificationBell />
                  <Button variant="outline" size="sm" className="h-9 px-6 rounded-lg font-bold text-xs border-zinc-200" onClick={onEditClick}>
                    프로필 편집
                  </Button>
                  <Button variant="outline" size="sm" className="h-9 w-9 p-0 rounded-lg border-zinc-200" onClick={onSettingsClick}>
                    <Settings size={16} />
                  </Button>
                </>
              ) : (
                <Button
                  variant={isFollowing ? 'outline' : 'primary'}
                  size="sm"
                  className="h-9 px-5 rounded-lg font-bold text-xs flex items-center gap-1.5"
                  onClick={onFollowToggle}
                  disabled={isFollowLoading}
                >
                  {isFollowing ? (
                    <><UserCheck size={14} /> 팔로잉</>
                  ) : (
                    <><UserPlus size={14} /> 팔로우</>
                  )}
                </Button>
              )}
            </div>
          </div>

          {/* 통계 (compact: 항상 표시 / 기본: lg 이상에서만 표시) */}
          <div className={cn(
            "flex items-center pt-1",
            compact ? "flex gap-5" : "hidden lg:flex gap-12 pt-2"
          )}>
            <div className="flex items-center gap-1.5">
              <span className={cn("font-black text-navy-900", compact ? "text-sm" : "")}>{postCount}</span>
              <span className={cn("text-zinc-400 font-bold", compact ? "text-[10px]" : "text-sm")}>포스팅</span>
            </div>
            <button onClick={onFollowersClick} className="flex items-center gap-1.5 hover:opacity-60 transition-opacity">
              <span className={cn("font-black text-navy-900", compact ? "text-sm" : "")}>{followerCount}</span>
              <span className={cn("text-zinc-400 font-bold", compact ? "text-[10px]" : "text-sm")}>팔로워</span>
            </button>
            <button onClick={onFollowingClick} className="flex items-center gap-1.5 hover:opacity-60 transition-opacity">
              <span className={cn("font-black text-navy-900", compact ? "text-sm" : "")}>{followingCount}</span>
              <span className={cn("text-zinc-400 font-bold", compact ? "text-[10px]" : "text-sm")}>팔로잉</span>
            </button>
            {!compact && (
              <div className="pl-4 border-l border-zinc-100">
                <MannerScoreGauge score={member.mannerTemperature || 0} />
              </div>
            )}
          </div>

          {/* compact 전용: MannerScore 별도 행 */}
          {compact && (
            <MannerScoreGauge score={member.mannerTemperature || 0} />
          )}

          {/* 소개 (compact: 항상 표시 / 기본: lg 이상에서만 표시) */}
          <div className={cn(compact ? "block space-y-1" : "hidden lg:block space-y-1")}>
            <Typography variant="body" className={cn("font-black text-navy-900", compact ? "text-xs" : "")}>{displayHandle}</Typography>
            <Typography variant="body" className={cn("text-zinc-500 leading-relaxed whitespace-pre-line", compact ? "text-[11px]" : "text-sm max-w-md")}>{member.selfIntroduction}</Typography>
          </div>
        </div>
      </div>

      {/* Walk stats heatmap slot (optional, injected by MyProfileView) */}
      {walkStatsSlot && (
        <div className="mt-4">
          {walkStatsSlot}
        </div>
      )}
    </section>
  );
};
