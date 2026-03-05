'use client';

import React from 'react';
import Link from 'next/link';
import { MapPin, ArrowRight } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { MannerScoreGauge } from '@/components/common/MannerScoreGauge';
import { ThreadType } from '@/types';

interface LocalFeedPreviewProps {
  threads: ThreadType[];
}

export const LocalFeedPreview: React.FC<LocalFeedPreviewProps> = ({ threads }) => {
  return (
    <section className="space-y-8">
      <div className="flex items-center justify-between px-2">
        <div className="space-y-1">
          <Typography variant="h3" className="font-serif text-navy-900 text-2xl">우리 동네 <span className="text-amber-500 italic">새로운 소식</span></Typography>
          <Typography variant="body" className="text-zinc-400 font-medium text-xs">지금 성수동 이웃들이 올린 따끈따끈한 산책 스레드입니다.</Typography>
        </div>
        <Link href="/around-me">
          <Button variant="ghost" size="sm" className="gap-2">
            레이더 전체보기 <ArrowRight size={14} />
          </Button>
        </Link>
      </div>
      
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
        {threads.map((thread, i) => (
          <Card key={i} interactive className="p-8 group">
            <div className="flex items-center justify-between mb-8">
              <div className="flex items-center gap-4">
                <img src={thread.author?.avatar || '/AINIINU_ROGO_B.png'} alt="Avatar" className="w-12 h-12 rounded-full border-2 border-white shadow-md" />
                <div>
                  <Typography variant="body" className="font-black text-navy-900">{thread.author?.nickname || thread.owner}</Typography>
                  <div className="flex items-center gap-1.5 text-zinc-400 text-[10px] font-bold">
                    <MapPin size={10} className="text-amber-500" /> {thread.location || thread.place}
                  </div>
                </div>
              </div>
              <Badge variant="amber" className="bg-amber-50 text-amber-600 border-none text-[10px]">TODAY</Badge>
            </div>
            
            <div className="relative h-56 rounded-[40px] overflow-hidden mb-8">
              <img src={thread.thumbnail || thread.image} alt={thread.title} className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-1000" />
              <div className="absolute top-6 right-6 bg-navy-900/80 backdrop-blur-md px-4 py-2 rounded-2xl text-[10px] font-black text-white">
                {thread.startTime || thread.time}
              </div>
            </div>
            
            <div className="space-y-2">
              <Typography variant="h3" className="text-lg font-black text-navy-900 group-hover:text-amber-500 transition-colors line-clamp-1">{thread.title || `${thread.name}와 산책`}</Typography>
              <Typography variant="body" className="text-xs text-zinc-400 line-clamp-2 leading-relaxed">{thread.description || thread.content}</Typography>
            </div>
            
            <div className="flex items-center justify-between pt-8 mt-2 border-t border-zinc-50">
              <MannerScoreGauge score={thread.author?.mannerScore || 5} />
              <div className="w-10 h-10 bg-zinc-50 rounded-full flex items-center justify-center text-zinc-300 group-hover:bg-amber-500 group-hover:text-navy-900 transition-all">
                <ArrowRight size={18} />
              </div>
            </div>
          </Card>
        ))}
      </div>
    </section>
  );
};
