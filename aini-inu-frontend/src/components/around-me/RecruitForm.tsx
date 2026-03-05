'use client';

import React, { useState } from 'react';
import { MapPin, Clock, Users, CheckCircle2, Loader2, PlusCircle, Check, AlertCircle } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import { DogType, ThreadType } from '@/types';

interface RecruitFormProps {
  currentLocation: string;
  onLocationClick: () => void;
  myDogs: DogType[];
  isSubmitting: boolean;
  isSuccess: boolean;
  onSubmit: (data: Partial<ThreadType>, selectedDogs: string[]) => void;
  editingThread?: ThreadType | null;
  myActiveThread?: ThreadType | null;
}

export const RecruitForm: React.FC<RecruitFormProps> = ({
  currentLocation,
  onLocationClick,
  myDogs,
  isSubmitting,
  isSuccess,
  onSubmit,
  editingThread = null,
  myActiveThread = null
}) => {
  const now = new Date();
  const minTime = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;

  const [form, setForm] = useState({
    title: editingThread?.title || '',
    time: editingThread?.time || editingThread?.startTime || '',
    place: editingThread?.place || editingThread?.location || currentLocation,
    description: editingThread?.description || editingThread?.content || ''
  });
  const [selectedDogs, setSelectedDogs] = useState<string[]>(
    editingThread?.participatingDogs?.map((d) => d.id) || []
  );

  const handleSubmit = () => {
    onSubmit(form, selectedDogs);
  };

  // 수정 중이 아닌데 이미 활성 스레드가 있으면 등록 차단
  if (!editingThread && myActiveThread) {
    return (
      <div className="max-w-4xl mx-auto w-full py-6 animate-in fade-in zoom-in-95 duration-700">
        <Card className="p-10 bg-white shadow-2xl rounded-[40px] text-center space-y-6">
          <div className="w-16 h-16 bg-amber-50 text-amber-500 rounded-full flex items-center justify-center mx-auto">
            <AlertCircle size={32} />
          </div>
          <div className="space-y-2">
            <Typography variant="h3" className="text-xl font-black text-navy-900">이미 모집 중인 스레드가 있어요</Typography>
            <Typography variant="body" className="text-zinc-400 text-sm">
              기존 스레드를 삭제하거나 수정한 뒤 새로운 모집글을 올릴 수 있어요.
            </Typography>
          </div>
          <div className="bg-amber-50 rounded-[24px] p-5 border border-amber-100 text-left space-y-1">
            <Typography variant="body" className="font-black text-navy-900 text-sm">{myActiveThread.title}</Typography>
            <Typography variant="body" className="text-zinc-400 text-xs">{myActiveThread.place || myActiveThread.location} · {myActiveThread.time || myActiveThread.startTime}</Typography>
          </div>
        </Card>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto w-full py-6 space-y-6 animate-in fade-in zoom-in-95 duration-700">
      <div className="text-center space-y-1">
        <Typography variant="h2" className="text-2xl">
          {editingThread
            ? <>내 <span className="text-amber-500 italic">스레드</span> 수정하기</>
            : <>함께 산책할 <span className="text-amber-500 italic">메이트</span>를 찾으시나요?</>
          }
        </Typography>
        <Typography variant="body" className="text-zinc-400 text-sm">
          {editingThread ? '수정된 내용이 주변 이웃들에게 반영됩니다.' : '산책 정보를 등록하면 주변 이웃들에게 시각화되어 노출됩니다.'}
        </Typography>
      </div>

      <Card className="p-8 bg-white shadow-2xl rounded-[40px] space-y-8">
        {isSuccess ? (
          <div className="py-10 text-center space-y-6 animate-in zoom-in-90">
            <div className="w-20 h-20 bg-amber-50 text-amber-500 rounded-full flex items-center justify-center mx-auto">
              <Check size={40} strokeWidth={3} />
            </div>
            <Typography variant="h2" className="text-2xl">모집 등록 완료!</Typography>
          </div>
        ) : (
          <>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-3">
                <Typography variant="label" className="text-navy-900 flex items-center gap-2 text-[11px]"><MapPin size={14} className="text-amber-500"/> 산책 장소</Typography>
                <input type="text" value={currentLocation} readOnly onClick={onLocationClick} className="w-full bg-white border border-zinc-200 rounded-2xl py-3 px-5 text-sm font-bold text-navy-900 cursor-pointer hover:border-amber-500 hover:shadow-md transition-all" />
              </div>
              <div className="space-y-3">
                <Typography variant="label" className="text-navy-900 flex items-center gap-2 text-[11px]"><Clock size={14} className="text-amber-500"/> 시작 시간</Typography>
                <input type="time" value={form.time} min={minTime} onChange={e => setForm({...form, time: e.target.value})} className="w-full bg-white border border-zinc-200 rounded-2xl py-3 px-5 text-sm font-bold text-navy-900 focus:outline-none focus:border-amber-500 focus:ring-4 ring-amber-500/10 transition-all" />
              </div>
              <div className="space-y-3 col-span-full">
                <div className="flex items-center justify-between"><Typography variant="label" className="text-navy-900 flex items-center gap-2"><Users size={14} className="text-amber-500"/> 참여 반려견</Typography><span className="text-[10px] font-black text-amber-500">선택됨 {selectedDogs.length}</span></div>
                <div className="flex gap-3 overflow-x-auto pb-2">
                  {myDogs.map(d => (
                    <div 
                      key={d.id} 
                      onClick={() => setSelectedDogs(prev => prev.includes(d.id) ? prev.filter(i => i!==d.id) : [...prev, d.id])} 
                      className={cn("flex-shrink-0 w-20 h-20 rounded-[20px] overflow-hidden border-2 cursor-pointer transition-all relative", selectedDogs.includes(d.id) ? "border-amber-500 ring-2 ring-amber-500/30 scale-105" : "border-transparent opacity-60")}
                    >
                      <img src={d.image} className="w-full h-full object-cover" alt="My Dog" />
                      {selectedDogs.includes(d.id) && <div className="absolute inset-0 bg-amber-500/20 flex items-center justify-center"><CheckCircle2 className="text-white" size={24} /></div>}
                    </div>
                  ))}
                </div>
              </div>
            </div>
            <div className="space-y-3"><Typography variant="label" className="text-navy-900">제목</Typography><input type="text" placeholder="예: 오늘 오후에 공원 한바퀴 도실 분!" value={form.title} onChange={e => setForm({...form, title: e.target.value})} className="w-full bg-white border border-zinc-200 rounded-2xl py-3 px-5 text-sm font-bold text-navy-900 focus:outline-none focus:border-amber-500" /></div>
            <div className="space-y-3"><Typography variant="label" className="text-navy-900">상세 설명</Typography><textarea placeholder="자유롭게 적어주세요." value={form.description} onChange={e => setForm({...form, description: e.target.value})} className="w-full h-28 bg-white border border-zinc-200 rounded-2xl p-5 text-sm font-medium text-navy-900 focus:outline-none focus:border-amber-500 resize-none" /></div>
            <Button variant="secondary" fullWidth size="md" className="py-4 shadow-xl" onClick={handleSubmit} disabled={isSubmitting || !form.title || !form.time || selectedDogs.length === 0}>
              {isSubmitting ? <Loader2 className="animate-spin mr-2" /> : <PlusCircle className="mr-2" size={18} />}
              {isSubmitting ? (editingThread ? '수정 중...' : '등록 중...') : (editingThread ? '수정 완료하기' : '모집 시작하기')}
            </Button>
          </>
        )}
      </Card>
    </div>
  );
};
