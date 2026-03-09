'use client';

import React from 'react';
import { ShieldCheck, Trophy, MapPin, Clock, MessageSquare, User } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import type {
  LostPetAnalyzeCandidateResponse,
  LostPetMatchCandidateResponse,
} from '@/api/lostPets';

type CandidateType =
  | LostPetAnalyzeCandidateResponse
  | LostPetMatchCandidateResponse;

interface AICandidateListProps {
  candidates: CandidateType[];
  onApprove: (sightingId: number) => void;
  approving?: boolean;
}

function formatDateTime(dateStr: string): string {
  try {
    const date = new Date(dateStr);
    return date.toLocaleString('ko-KR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return dateStr;
  }
}

export const AICandidateList: React.FC<AICandidateListProps> = ({
  candidates,
  onApprove,
  approving = false,
}) => {
  return (
    <div className="space-y-10 animate-in fade-in zoom-in-95 duration-700 max-w-5xl mx-auto w-full py-6">
      <div className="text-center space-y-4">
        <div className="inline-flex items-center gap-2 bg-amber-50 px-4 py-2 rounded-full border border-amber-100 text-amber-600 mb-2">
          <ShieldCheck size={16} />
          <Typography
            variant="label"
            className="font-black text-[10px] uppercase tracking-widest"
          >
            AI Matching Analysis
          </Typography>
        </div>
        {candidates.length > 0 ? (
          <>
            <Typography
              variant="h2"
              className="text-3xl md:text-4xl text-navy-900 font-serif"
            >
              실종된 아이와{' '}
              <span className="text-amber-500 italic">유사한 제보</span>들을
              찾았습니다.
            </Typography>
            <Typography variant="body" className="text-zinc-400 text-lg">
              Gemini AI가 이미지와 특징을 분석하여 가장 높은 확률의 후보들을
              선별했습니다.
            </Typography>
          </>
        ) : (
          <>
            <Typography
              variant="h2"
              className="text-3xl md:text-4xl text-navy-900 font-serif"
            >
              아직 <span className="text-zinc-400 italic">일치하는 제보</span>가
              없습니다.
            </Typography>
            <Typography variant="body" className="text-zinc-400 text-lg">
              새로운 제보가 등록되면 AI가 자동으로 매칭을 시도합니다.
            </Typography>
          </>
        )}
      </div>

      {candidates.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {candidates.slice(0, 10).map((candidate) => (
            <Card
              key={`${candidate.sightingId}-${candidate.rank}`}
              className="group overflow-hidden rounded-[40px] border-none shadow-2xl bg-white flex flex-col hover:scale-[1.02] transition-transform duration-500"
            >
              {/* Photo */}
              <div className="relative w-full aspect-[4/3] bg-zinc-100 overflow-hidden">
                {candidate.photoUrl ? (
                  <img
                    src={candidate.photoUrl}
                    alt={`제보 #${candidate.sightingId}`}
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <div className="w-full h-full flex items-center justify-center text-zinc-300">
                    <Typography variant="body" className="text-sm">
                      사진 없음
                    </Typography>
                  </div>
                )}
              </div>

              {/* Score header */}
              <div className="p-6 pb-0">
                <div className="flex items-center justify-between mb-4">
                  <div className="flex items-center gap-2">
                    <Trophy
                      size={18}
                      className="text-amber-500"
                    />
                    <Typography
                      variant="label"
                      className="font-black text-lg text-navy-900"
                    >
                      #{candidate.rank}
                    </Typography>
                  </div>
                  <Badge
                    variant="amber"
                    className="bg-amber-500/90 text-navy-900 border-none font-black text-sm px-4 py-1.5"
                  >
                    {candidate.scoreTotal.toFixed(1)}점
                  </Badge>
                </div>
              </div>

              <div className="p-6 pt-2 flex-1 flex flex-col justify-between space-y-5">
                {/* Sighting details */}
                <div className="space-y-2.5">
                  <div className="flex items-start gap-2 text-sm">
                    <MapPin size={14} className="text-zinc-400 mt-0.5 shrink-0" />
                    <Typography variant="body" className="text-zinc-600 text-sm">
                      {candidate.foundLocation}
                    </Typography>
                  </div>
                  <div className="flex items-start gap-2 text-sm">
                    <Clock size={14} className="text-zinc-400 mt-0.5 shrink-0" />
                    <Typography variant="body" className="text-zinc-600 text-sm">
                      {formatDateTime(candidate.foundAt)}
                    </Typography>
                  </div>
                  {candidate.memo && (
                    <div className="flex items-start gap-2 text-sm">
                      <MessageSquare size={14} className="text-zinc-400 mt-0.5 shrink-0" />
                      <Typography variant="body" className="text-zinc-600 text-sm line-clamp-2">
                        {candidate.memo}
                      </Typography>
                    </div>
                  )}
                  <div className="flex items-start gap-2 text-sm">
                    <User size={14} className="text-zinc-400 mt-0.5 shrink-0" />
                    <Typography variant="body" className="text-zinc-600 text-sm">
                      {candidate.finderNickname}
                    </Typography>
                  </div>
                </div>

                {/* Score breakdown */}
                <div className="space-y-3">
                  <div className="flex items-center justify-between text-sm">
                    <Typography
                      variant="label"
                      className="text-zinc-400 text-xs font-bold"
                    >
                      유사도
                    </Typography>
                    <Typography
                      variant="body"
                      className="font-black text-zinc-700"
                    >
                      {candidate.scoreSimilarity.toFixed(1)}
                    </Typography>
                  </div>
                  <div className="flex items-center justify-between text-sm">
                    <Typography
                      variant="label"
                      className="text-zinc-400 text-xs font-bold"
                    >
                      거리
                    </Typography>
                    <Typography
                      variant="body"
                      className="font-black text-zinc-700"
                    >
                      {candidate.scoreDistance.toFixed(1)}
                    </Typography>
                  </div>
                  <div className="flex items-center justify-between text-sm">
                    <Typography
                      variant="label"
                      className="text-zinc-400 text-xs font-bold"
                    >
                      최근성
                    </Typography>
                    <Typography
                      variant="body"
                      className="font-black text-zinc-700"
                    >
                      {candidate.scoreRecency.toFixed(1)}
                    </Typography>
                  </div>
                </div>

                {/* Approve button */}
                <Button
                  variant="primary"
                  fullWidth
                  className="bg-navy-900 rounded-2xl h-14 font-black gap-2 shadow-xl shadow-navy-900/20"
                  onClick={() => onApprove(candidate.sightingId)}
                  disabled={approving}
                >
                  승인
                </Button>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
};
