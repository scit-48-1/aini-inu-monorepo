'use client';

import React, { useState, useEffect } from 'react';
import dynamic from 'next/dynamic';
import { X, Zap, Clock, MapPin, Users, Loader2, Footprints, MessageCircle, Pencil, Trash2 } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { MannerScoreGauge } from '@/components/common/MannerScoreGauge';
import { ThreadType } from '@/types';

const DynamicMap = dynamic(() => import('@/components/common/DynamicMap'), { 
  ssr: false,
  loading: () => (
    <div className="w-full h-full bg-[#f8fafc] flex flex-col items-center justify-center space-y-6">
      <div className="relative">
        <Loader2 className="animate-spin text-amber-500" size={64} />
        <div className="absolute inset-0 flex items-center justify-center">
          <Footprints size={24} className="text-amber-500/50" />
        </div>
      </div>
      <Typography variant="label" className="text-zinc-400 font-black tracking-[0.2em] uppercase text-xs">Aini Inu Radar Syncing...</Typography>
    </div>
  )
});

interface RadarMapSectionProps {
  mapCenter: [number, number];
  visibleMarkers: ThreadType[];
  selectedPin: ThreadType | null;
  setSelectedPin: (pin: ThreadType | null) => void;
  userNickname: string;
  getRemainingTime: (time?: string) => string;
  onJoinThread: (id: string | number) => void;
  onContact: (partnerId: string) => void;
  onDeleteThread: (id: string | number) => void;
  onEditThread: (thread: ThreadType) => void;
  activeTab: string;
}

export const RadarMapSection: React.FC<RadarMapSectionProps> = ({
  mapCenter,
  visibleMarkers,
  selectedPin,
  setSelectedPin,
  userNickname,
  getRemainingTime,
  onJoinThread,
  onContact,
  onDeleteThread,
  onEditThread,
  activeTab
}) => {
  const [isConfirmingDelete, setIsConfirmingDelete] = useState(false);

  useEffect(() => {
    setIsConfirmingDelete(false);
  }, [selectedPin]);

  return (
    <div className={cn(
      "flex-[1.8] relative bg-zinc-100 rounded-[48px] border border-card-border overflow-hidden min-h-[500px] flex flex-col shadow-2xl transition-all duration-700", 
      activeTab === 'EMERGENCY' ? 'lg:flex-[1.2]' : ''
    )}>
      <div className="absolute inset-0 z-0" onClick={() => setSelectedPin(null)}>
        <DynamicMap center={mapCenter} zoom={14} markers={visibleMarkers} onMarkerClick={(m) => setSelectedPin(m as ThreadType)} />
      </div>
      
      {selectedPin && (
        <div className="absolute bottom-10 left-1/2 -translate-x-1/2 z-[1000] w-full max-w-lg px-4 animate-in slide-in-from-bottom-4 duration-500">
          <Card className="p-8 bg-white shadow-2xl border-2 border-amber-500/10 space-y-6 rounded-[48px] overflow-hidden">
            <div className="flex justify-between items-start">
              <div className="flex items-center gap-5">
                <img src={selectedPin.image || selectedPin.thumbnail} className="w-24 h-24 rounded-[32px] object-cover shadow-2xl border-4 border-white" alt="Thumb" />
                <div>
                  <Badge variant={selectedPin.isEmergency ? 'default' : 'amber'} className={selectedPin.isEmergency ? 'bg-red-500 text-white border-none px-3' : 'px-3'}>
                    {selectedPin.isEmergency ? '긴급 제보' : '메이트 모집 중'}
                  </Badge>
                  <Typography variant="h3" className="text-2xl text-navy-900 leading-tight mb-1 mt-2">{selectedPin.title || selectedPin.breed || selectedPin.name}</Typography>
                  <div className="flex items-center gap-2">
                    <Typography variant="label" className="text-zinc-400 font-black">@{selectedPin.owner || selectedPin.author?.nickname}</Typography>
                    <MannerScoreGauge score={selectedPin.author?.mannerScore || 5} />
                  </div>
                </div>
              </div>
              <button onClick={() => { setSelectedPin(null); setIsConfirmingDelete(false); }} className="p-3 bg-zinc-50 rounded-2xl text-zinc-300 hover:text-navy-900"><X size={24} /></button>
            </div>
            
            {!selectedPin.isEmergency && (
              <div className="grid grid-cols-2 gap-4 py-5 border-y border-zinc-50">
                <div className="flex items-center gap-3 text-sm font-black text-zinc-600"><Zap size={16} className="text-amber-500" /> {getRemainingTime(selectedPin.time)}</div>
                <div className="flex items-center gap-3 text-sm font-black text-zinc-600"><Clock size={16} className="text-amber-500" /> {selectedPin.time || '시간 협의'}</div>
                <div className="col-span-2 flex items-center gap-3 text-sm font-black text-zinc-600"><MapPin size={16} className="text-amber-500" /> {selectedPin.place || '상세 장소 협의'}</div>
              </div>
            )}
            
            <div className="bg-zinc-50/50 p-6 rounded-[32px] border border-zinc-100/50 italic text-zinc-600">&quot;{selectedPin.content || selectedPin.description || '내용이 없습니다.'}&quot;</div>
            
            {/* Action Buttons */}
            <div className="flex gap-3">
              {selectedPin.owner === userNickname || selectedPin.author?.nickname === userNickname ? (
                <div className="flex-1 space-y-3">
                  <div className="h-12 flex items-center justify-center gap-3 bg-amber-50 rounded-[20px] border border-amber-100">
                    <Users size={16} className="text-amber-500" />
                    <Typography variant="body" className="text-sm font-black text-amber-700">
                      내가 만든 스레드 • 현재 <span className="text-amber-900">{selectedPin.participatingDogs?.length || 1}명</span> 참여 중
                    </Typography>
                  </div>
                  {isConfirmingDelete ? (
                    <div className="flex gap-2 animate-in fade-in duration-200">
                      <Button
                        variant="outline"
                        fullWidth
                        size="lg"
                        className="h-12 rounded-[20px] border-zinc-200 text-zinc-500"
                        onClick={() => setIsConfirmingDelete(false)}
                      >
                        취소
                      </Button>
                      <Button
                        variant="outline"
                        fullWidth
                        size="lg"
                        className="h-12 rounded-[20px] bg-red-500 border-red-500 text-white hover:bg-red-600"
                        onClick={() => { onDeleteThread(selectedPin.id); setIsConfirmingDelete(false); }}
                      >
                        <Trash2 size={16} className="mr-2" /> 삭제 확인
                      </Button>
                    </div>
                  ) : (
                    <div className="flex gap-2">
                      <Button
                        variant="outline"
                        fullWidth
                        size="lg"
                        className="h-12 rounded-[20px] border-zinc-200 text-zinc-600 hover:border-amber-500 hover:text-amber-600"
                        onClick={() => onEditThread(selectedPin)}
                      >
                        <Pencil size={16} className="mr-2" /> 수정하기
                      </Button>
                      <Button
                        variant="outline"
                        fullWidth
                        size="lg"
                        className="h-12 rounded-[20px] border-zinc-200 text-red-400 hover:border-red-400 hover:text-red-500"
                        onClick={() => setIsConfirmingDelete(true)}
                      >
                        <Trash2 size={16} className="mr-2" /> 삭제하기
                      </Button>
                    </div>
                  )}
                </div>
              ) : (
                <>
                  {selectedPin.isEmergency ? (
                    <Button 
                      variant="primary" 
                      fullWidth 
                      size="lg" 
                      className="h-16 text-lg rounded-[24px] shadow-2xl bg-red-500" 
                      onClick={() => onContact(selectedPin.author?.id || selectedPin.id)}
                    >
                      <MessageCircle className="mr-2" size={20} /> 제보자와 대화하기
                    </Button>
                  ) : (
                    <>
                      {!selectedPin.isJoined ? (
                        <Button 
                          variant="primary" 
                          fullWidth 
                          size="lg" 
                          className="h-16 text-lg rounded-[24px] shadow-2xl bg-navy-900" 
                          onClick={() => onJoinThread(selectedPin.id)}
                        >
                          산책 신청하기
                        </Button>
                      ) : (
                        <Button 
                          variant="primary" 
                          fullWidth 
                          size="lg" 
                          className="h-16 text-lg rounded-[24px] shadow-2xl bg-emerald-500" 
                          onClick={() => onContact(selectedPin.author?.id || selectedPin.id)}
                        >
                          <MessageCircle className="mr-2" size={20} /> 메이트와 대화하기
                        </Button>
                      )}
                    </>
                  )}
                </>
              )}
            </div>
          </Card>
        </div>
      )}
    </div>
  );
};
