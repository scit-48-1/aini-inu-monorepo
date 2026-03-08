'use client';

import React, { useEffect } from 'react';
import {
  Loader2,
  Lock,
  MapPin,
  Footprints,
  CheckCircle2,
  FileText,
  AlertTriangle,
  Eye,
  LucideIcon,
} from 'lucide-react';
import { useTimeline } from '@/hooks/useTimeline';
import type { TimelineEventType } from '@/api/timeline';
import type { TimelineEventResponse } from '@/api/timeline';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import { cn } from '@/lib/utils';

interface ProfileTimelineProps {
  memberId: number;
}

const EVENT_CONFIG: Record<TimelineEventType, { icon: LucideIcon; color: string; label: string }> = {
  WALK_THREAD_CREATED: { icon: MapPin, color: 'bg-blue-500', label: '산책 스레드' },
  WALKING_SESSION_STARTED: { icon: Footprints, color: 'bg-green-500', label: '산책 시작' },
  WALKING_SESSION_COMPLETED: { icon: CheckCircle2, color: 'bg-emerald-600', label: '산책 완료' },
  POST_CREATED: { icon: FileText, color: 'bg-purple-500', label: '게시글' },
  LOST_PET_REPORT_CREATED: { icon: AlertTriangle, color: 'bg-red-500', label: '실종 신고' },
  SIGHTING_CREATED: { icon: Eye, color: 'bg-amber-500', label: '목격 제보' },
};

function formatAbsoluteTime(dateString: string): string {
  const date = new Date(dateString);
  const now = new Date();

  const isToday =
    date.getFullYear() === now.getFullYear() &&
    date.getMonth() === now.getMonth() &&
    date.getDate() === now.getDate();

  if (isToday) {
    return date.toLocaleTimeString('ko-KR', {
      hour: 'numeric',
      minute: '2-digit',
      hour12: true,
    });
  }

  const isThisYear = date.getFullYear() === now.getFullYear();

  if (isThisYear) {
    return date.toLocaleString('ko-KR', {
      month: 'long',
      day: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
      hour12: true,
    });
  }

  return date.toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
}

function groupByDate(events: TimelineEventResponse[]): Map<string, TimelineEventResponse[]> {
  const groups = new Map<string, TimelineEventResponse[]>();
  for (const event of events) {
    const dateKey = new Date(event.occurredAt).toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
    const group = groups.get(dateKey) || [];
    group.push(event);
    groups.set(dateKey, group);
  }
  return groups;
}

export const ProfileTimeline: React.FC<ProfileTimelineProps> = ({ memberId }) => {
  const { events, isLoading, hasNext, isPrivate, fetchTimeline, loadMore } = useTimeline(memberId);

  useEffect(() => {
    fetchTimeline(0);
  }, [fetchTimeline]);

  if (isPrivate) {
    return (
      <div className="flex flex-col items-center justify-center p-16 gap-4 opacity-40 animate-in fade-in duration-300">
        <Lock size={40} strokeWidth={1.5} className="text-zinc-400" />
        <Typography variant="body" className="font-bold text-zinc-500">
          비공개 타임라인입니다
        </Typography>
        <Typography variant="label" className="text-zinc-400 text-center">
          이 사용자의 타임라인은 비공개로 설정되어 있습니다.
        </Typography>
      </div>
    );
  }

  if (isLoading && events.length === 0) {
    return (
      <div className="flex items-center justify-center p-16 opacity-20">
        <Loader2 className="animate-spin" size={32} />
      </div>
    );
  }

  if (events.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center p-16 gap-2 opacity-30 animate-in fade-in duration-300">
        <Typography variant="body" className="font-bold text-zinc-500">
          아직 활동 기록이 없습니다
        </Typography>
        <Typography variant="label" className="text-zinc-400 text-center">
          산책, 게시글, 실종 신고 등의 활동이 여기에 표시됩니다.
        </Typography>
      </div>
    );
  }

  const grouped = groupByDate(events);

  return (
    <div className="p-6 md:p-8 animate-in slide-in-from-bottom-4 duration-500">
      <div className="relative">
        {/* Vertical line */}
        <div className="absolute left-5 top-0 bottom-0 w-0.5 bg-zinc-100" />

        {Array.from(grouped.entries()).map(([dateLabel, dateEvents]) => (
          <div key={dateLabel} className="mb-6">
            {/* Date header */}
            <div className="relative flex items-center mb-4 pl-12">
              <div className="absolute left-3.5 w-3 h-3 rounded-full bg-zinc-200 border-2 border-white z-10" />
              <Typography variant="label" className="text-zinc-400 font-black text-[10px] uppercase tracking-widest">
                {dateLabel}
              </Typography>
            </div>

            {/* Events for this date */}
            {dateEvents.map((event) => {
              const config = EVENT_CONFIG[event.eventType];
              const Icon = config.icon;

              return (
                <div key={event.id} className="relative flex gap-4 mb-4 pl-12 group">
                  {/* Event dot */}
                  <div className={cn(
                    "absolute left-2.5 w-5 h-5 rounded-full flex items-center justify-center z-10 shadow-sm transition-transform group-hover:scale-110",
                    config.color
                  )}>
                    <Icon size={11} className="text-white" strokeWidth={2.5} />
                  </div>

                  {/* Event card */}
                  <div className="flex-1 bg-zinc-50/50 rounded-2xl border border-zinc-100/50 p-4 hover:bg-zinc-50 transition-colors">
                    <div className="flex items-start justify-between gap-3">
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2 mb-1">
                          <span className={cn(
                            "text-[9px] font-black uppercase tracking-widest px-2 py-0.5 rounded-full text-white",
                            config.color
                          )}>
                            {config.label}
                          </span>
                          <span className="text-[10px] text-zinc-400 font-medium">
                            {formatAbsoluteTime(event.occurredAt)}
                          </span>
                        </div>
                        {event.title && (
                          <Typography variant="body" className="text-navy-900 font-bold text-sm leading-snug">
                            {event.title}
                          </Typography>
                        )}
                        {event.summary && (
                          <Typography variant="label" className="text-zinc-500 text-xs mt-1 line-clamp-2">
                            {event.summary}
                          </Typography>
                        )}
                      </div>
                      {event.thumbnailUrl && (
                        <img
                          src={event.thumbnailUrl}
                          alt=""
                          className="w-12 h-12 rounded-xl object-cover shrink-0 shadow-sm"
                        />
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        ))}

        {/* Load more */}
        {hasNext && (
          <div className="flex justify-center pt-4 pl-12">
            <Button
              variant="outline"
              size="sm"
              onClick={loadMore}
              disabled={isLoading}
              className="text-xs"
            >
              {isLoading ? (
                <Loader2 className="animate-spin mr-2" size={14} />
              ) : null}
              더보기
            </Button>
          </div>
        )}
      </div>
    </div>
  );
};
