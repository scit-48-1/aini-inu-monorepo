'use client';

import React from 'react';
import { ShieldCheck, Trophy } from 'lucide-react';
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
      </div>

      {candidates.length === 0 ? (
        <div className="flex items-center justify-center py-20">
          <Typography variant="body" className="text-zinc-400 text-sm">
            일치하는 후보를 찾지 못했습니다.
          </Typography>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {candidates.slice(0, 10).map((candidate) => (
            <Card
              key={`${candidate.sightingId}-${candidate.rank}`}
              className="group overflow-hidden rounded-[40px] border-none shadow-2xl bg-white flex flex-col hover:scale-[1.02] transition-transform duration-500"
            >
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

                {/* Sighting info */}
                <div className="p-4 bg-zinc-50 rounded-2xl border border-zinc-100 space-y-1">
                  <Typography
                    variant="label"
                    className="text-zinc-400 text-[10px] font-black uppercase"
                  >
                    Sighting #{candidate.sightingId}
                  </Typography>
                  <Typography variant="body" className="text-zinc-500 text-xs">
                    상태: {candidate.status}
                  </Typography>
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
