'use client';

import React from 'react';
import { ShieldCheck, MapPin, MessageCircle, UserCircle } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';

export interface AICandidate {
  id: string;
  name: string;
  breed: string;
  age: number;
  gender: 'M' | 'F';
  image: string;
  matchRate: number;
  location: string;
  ownerNickname: string;
  lastSeen: string;
  type: 'LOST' | 'FOUND';
}

interface AICandidateListProps {
  candidates: AICandidate[];
  onContact: (candidate: AICandidate) => void;
  onClose: () => void;
  mode: 'LOST' | 'FOUND';
}

export const AICandidateList: React.FC<AICandidateListProps> = ({
  candidates,
  onContact,
  onClose,
  mode
}) => {
  return (
    <div className="space-y-10 animate-in fade-in zoom-in-95 duration-700 max-w-5xl mx-auto w-full py-6">
      <div className="text-center space-y-4">
        <div className="inline-flex items-center gap-2 bg-amber-50 px-4 py-2 rounded-full border border-amber-100 text-amber-600 mb-2">
          <ShieldCheck size={16} />
          <Typography variant="label" className="font-black text-[10px] uppercase tracking-widest">AI Matching Analysis</Typography>
        </div>
        <Typography variant="h2" className="text-3xl md:text-4xl text-navy-900 font-serif">
          {mode === 'LOST' 
            ? <>실종된 아이와 <span className="text-amber-500 italic">유사한 제보</span>들을 찾았습니다.</>
            : <>제보해주신 아이와 <span className="text-amber-500 italic">일치하는 실종 신고</span>입니다.</>
          }
        </Typography>
        <Typography variant="body" className="text-zinc-400 text-lg">
          Gemini AI가 이미지와 특징을 분석하여 가장 높은 확률의 후보들을 선별했습니다.
        </Typography>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {candidates.map((candidate) => (
          <Card key={candidate.id} className="group overflow-hidden rounded-[40px] border-none shadow-2xl bg-white flex flex-col hover:scale-[1.02] transition-transform duration-500">
            <div className="h-64 relative overflow-hidden">
              <img src={candidate.image} alt="Candidate" className="w-full h-full object-cover transition-transform duration-700 group-hover:scale-110" />
              <div className="absolute top-4 left-4">
                <Badge variant="amber" className="bg-amber-500/90 text-navy-900 border-none font-black text-[10px] px-3 py-1 backdrop-blur-sm">
                  {candidate.matchRate}% MATCH
                </Badge>
              </div>
              <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-transparent to-transparent opacity-60" />
              <div className="absolute bottom-6 left-6 right-6 text-white space-y-1">
                <Typography variant="h3" className="text-2xl text-white leading-none">{candidate.name || '이름 미상'}</Typography>
                <Typography variant="label" className="text-white/80 font-bold text-xs">{candidate.breed} • {candidate.gender === 'M' ? '남아' : '여아'}</Typography>
              </div>
            </div>

            <div className="p-8 flex-1 flex flex-col justify-between space-y-6">
              <div className="space-y-4">
                <div className="flex items-center gap-3 text-zinc-500">
                  <MapPin size={16} className="text-amber-500 shrink-0" />
                  <Typography variant="body" className="text-sm font-bold truncate">{candidate.location}</Typography>
                </div>
                <div className="flex items-center gap-3 text-zinc-400">
                  <UserCircle size={16} className="text-zinc-300 shrink-0" />
                  <Typography variant="label" className="text-xs font-black uppercase">Reported by {candidate.ownerNickname}</Typography>
                </div>
                <div className="p-4 bg-zinc-50 rounded-2xl border border-zinc-100 italic text-zinc-500 text-xs leading-relaxed">
                  &quot;{candidate.lastSeen}&quot;
                </div>
              </div>

              <Button
                variant="primary"
                fullWidth
                className="bg-navy-900 rounded-2xl h-14 font-black gap-2 shadow-xl shadow-navy-900/20"
                onClick={() => onContact(candidate)}
              >
                <MessageCircle size={18} /> 채팅으로 확인
              </Button>
            </div>
          </Card>
        ))}
      </div>

      <div className="flex justify-center pt-10">
        <Button variant="outline" size="lg" onClick={onClose} className="rounded-full px-10 border-2 border-zinc-100 text-zinc-400 hover:border-amber-500 hover:text-amber-500 font-black">
          다른 정보 더 보기
        </Button>
      </div>
    </div>
  );
};
