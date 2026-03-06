'use client';

import React from 'react';
import { Clock, Lock, Plus, Loader2 } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import type { WalkDiaryResponse } from '@/api/diaries';

interface ProfileHistoryProps {
  diaries: WalkDiaryResponse[];
  isLoading: boolean;
  hasNext: boolean;
  onLoadMore: () => void;
  onDiaryClick: (diary: WalkDiaryResponse) => void;
  onCreateClick: () => void;
}

export const ProfileHistory: React.FC<ProfileHistoryProps> = ({
  diaries,
  isLoading,
  hasNext,
  onLoadMore,
  onDiaryClick,
  onCreateClick,
}) => {
  // Loading state
  if (isLoading && diaries.length === 0) {
    return (
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 p-6 md:p-8 animate-in slide-in-from-bottom-4 duration-500">
        {[0, 1].map((i) => (
          <Card key={i} className="p-6 rounded-[40px] border-zinc-50 shadow-lg flex gap-6 items-center">
            <div className="w-20 h-20 rounded-2xl bg-zinc-200 animate-pulse shrink-0" />
            <div className="flex-1 space-y-3">
              <div className="h-5 bg-zinc-200 rounded-lg animate-pulse w-3/4" />
              <div className="h-3 bg-zinc-100 rounded-lg animate-pulse w-1/2" />
            </div>
          </Card>
        ))}
      </div>
    );
  }

  // Empty state
  if (!isLoading && diaries.length === 0) {
    return (
      <div className="p-12 text-center flex flex-col items-center gap-6 animate-in fade-in duration-500">
        <div className="w-20 h-20 rounded-full bg-amber-50 flex items-center justify-center">
          <Clock size={32} className="text-amber-300" />
        </div>
        <div className="space-y-2">
          <Typography variant="h3" className="text-lg font-black text-navy-900">
            아직 작성한 산책일기가 없습니다.
          </Typography>
          <Typography variant="body" className="text-zinc-400 text-sm">
            산책 후 일기를 작성해 추억을 기록해보세요!
          </Typography>
        </div>
        <Button
          onClick={onCreateClick}
          className="bg-amber-500 hover:bg-amber-600 text-white font-bold px-6 py-3 rounded-2xl transition-colors"
        >
          첫 산책일기 작성하기
        </Button>
      </div>
    );
  }

  // Default/Success state
  return (
    <div className="relative">
      {/* Create button */}
      <div className="flex justify-end px-6 md:px-8 pt-4">
        <button
          onClick={onCreateClick}
          className="w-10 h-10 rounded-full bg-amber-500 hover:bg-amber-600 text-white flex items-center justify-center shadow-lg transition-all hover:scale-110 active:scale-95"
        >
          <Plus size={20} />
        </button>
      </div>

      {/* Diary grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 p-6 md:p-8 animate-in slide-in-from-bottom-4 duration-500">
        {diaries.map((diary) => {
          const thumbnail = diary.photoUrls?.[0];
          const formattedDate = (() => {
            try {
              return new Date(diary.walkDate).toLocaleDateString('ko-KR', {
                year: 'numeric',
                month: 'long',
                day: 'numeric',
              });
            } catch {
              return diary.walkDate;
            }
          })();

          return (
            <Card
              key={diary.id}
              interactive
              className="p-6 rounded-[40px] border-zinc-50 shadow-lg flex gap-6 items-center group relative overflow-hidden transition-all bg-white"
              onClick={() => onDiaryClick(diary)}
            >
              <div className="w-20 h-20 rounded-2xl relative shrink-0 overflow-hidden shadow-md group-hover:scale-105 transition-transform duration-500">
                {thumbnail ? (
                  <img
                    src={thumbnail}
                    alt="Walk"
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <div className="w-full h-full bg-amber-50 flex items-center justify-center">
                    <Clock size={24} className="text-amber-300" />
                  </div>
                )}
              </div>

              <div className="flex-1 space-y-3 min-w-0 relative z-10">
                <div className="flex items-center gap-2">
                  <Typography
                    variant="h3"
                    className="text-lg font-black text-navy-900 truncate"
                  >
                    {diary.title}
                  </Typography>
                  {!diary.isPublic ? (
                    <Lock size={12} className="text-zinc-300 shrink-0" />
                  ) : null}
                </div>
                <div className="flex items-center gap-2 text-zinc-400 text-[10px] font-black uppercase">
                  <Clock size={10} className="text-amber-500" />
                  {formattedDate}
                </div>
              </div>
            </Card>
          );
        })}
      </div>

      {/* Load more */}
      {hasNext ? (
        <div className="flex justify-center pb-6">
          <Button
            onClick={onLoadMore}
            disabled={isLoading}
            variant="outline"
            className="font-bold text-sm px-8 py-2 rounded-2xl"
          >
            {isLoading ? (
              <Loader2 size={16} className="animate-spin" />
            ) : (
              '더 보기'
            )}
          </Button>
        </div>
      ) : null}
    </div>
  );
};
