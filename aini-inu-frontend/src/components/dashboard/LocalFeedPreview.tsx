'use client';

import React from 'react';
import Link from 'next/link';
import { MapPin, ArrowRight, Users, Clock, RefreshCw } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import type { ThreadSummaryResponse } from '@/api/threads';

interface LocalFeedPreviewProps {
  threads: ThreadSummaryResponse[];
  error?: string | null;
  onRetry?: () => void;
}

function formatTimeRange(startTime: string, endTime: string): string {
  const fmt = (iso: string) => {
    try {
      const d = new Date(iso);
      return d.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', hour12: false });
    } catch {
      return iso;
    }
  };
  return `${fmt(startTime)} - ${fmt(endTime)}`;
}

function statusLabel(status: string): { text: string; variant: 'amber' | 'indigo' | 'default' } {
  switch (status) {
    case 'RECRUITING': return { text: '모집중', variant: 'amber' };
    case 'FULL': return { text: '마감', variant: 'indigo' };
    case 'COMPLETED': return { text: '완료', variant: 'default' };
    default: return { text: status, variant: 'default' };
  }
}

export const LocalFeedPreview: React.FC<LocalFeedPreviewProps> = ({ threads, error, onRetry }) => {
  return (
    <section className="space-y-8">
      <div className="flex items-center justify-between px-2">
        <div className="space-y-1">
          <Typography variant="h3" className="font-serif text-navy-900 text-2xl">우리 동네 <span className="text-amber-500 italic">새로운 소식</span></Typography>
          <Typography variant="body" className="text-zinc-400 font-medium text-xs">지금 이웃들이 올린 따끈따끈한 산책 스레드입니다.</Typography>
        </div>
        <Link href="/around-me">
          <Button variant="ghost" size="sm" className="gap-2">
            레이더 전체보기 <ArrowRight size={14} />
          </Button>
        </Link>
      </div>

      {error ? (
        <Card className="p-12 flex flex-col items-center gap-4">
          <Typography variant="body" className="text-zinc-400 text-sm">{error}</Typography>
          {onRetry && (
            <Button variant="ghost" size="sm" onClick={onRetry} className="gap-2">
              <RefreshCw size={14} /> 다시 시도
            </Button>
          )}
        </Card>
      ) : threads.length === 0 ? (
        <Card className="p-12 flex flex-col items-center gap-2">
          <Typography variant="h3" className="text-zinc-300 text-lg">아직 동네 소식이 없어요</Typography>
          <Typography variant="body" className="text-zinc-300 text-xs">첫 번째 산책 스레드를 만들어 보세요!</Typography>
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          {threads.map((thread) => (
            <Link key={thread.id} href={`/around-me?threadId=${thread.id}`}>
              <Card interactive className="p-8 group h-full flex flex-col">
                <div className="flex items-center justify-between mb-4">
                  <Badge variant={statusLabel(thread.status).variant} className="text-[10px]">
                    {statusLabel(thread.status).text}
                  </Badge>
                  <Badge variant="default" className="bg-zinc-100 text-zinc-500 border-none text-[10px]">
                    {thread.chatType === 'GROUP' ? '그룹' : '1:1'}
                  </Badge>
                </div>

                <div className="space-y-2 flex-1">
                  <Typography variant="h3" className="text-lg font-black text-navy-900 group-hover:text-amber-500 transition-colors line-clamp-1">
                    {thread.title}
                  </Typography>
                  <Typography variant="body" className="text-xs text-zinc-400 line-clamp-2 leading-relaxed">
                    {thread.description}
                  </Typography>
                </div>

                <div className="flex flex-col gap-3 pt-6 mt-4 border-t border-zinc-50">
                  <div className="flex items-center gap-1.5 text-zinc-400 text-xs">
                    <MapPin size={12} className="text-amber-500 shrink-0" />
                    <span className="truncate">{thread.placeName}</span>
                  </div>
                  <div className="flex items-center gap-1.5 text-zinc-400 text-xs">
                    <Clock size={12} className="text-amber-500 shrink-0" />
                    <span>{formatTimeRange(thread.startTime, thread.endTime)}</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-1.5 text-zinc-400 text-xs">
                      <Users size={12} className="text-amber-500 shrink-0" />
                      <span>{thread.currentParticipants}/{thread.maxParticipants}명</span>
                    </div>
                    <div className="w-8 h-8 bg-zinc-50 rounded-full flex items-center justify-center text-zinc-300 group-hover:bg-amber-500 group-hover:text-navy-900 transition-all">
                      <ArrowRight size={14} />
                    </div>
                  </div>
                </div>
              </Card>
            </Link>
          ))}
        </div>
      )}
    </section>
  );
};
