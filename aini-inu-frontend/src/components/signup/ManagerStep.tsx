'use client';

import React, { useEffect } from 'react';
import { User, MapPin, Sparkles, Loader2, Check, Camera, Calendar, Search, Phone } from 'lucide-react';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { cn } from '@/lib/utils';
import Script from 'next/script';

// window.daum 타입은 react-daum-postcode 패키지에서 이미 선언됨 (재선언 불필요)

interface ManagerStepProps {
  managerData: any;
  setManagerData: (data: any) => void;
  isSubmitting: boolean;
  onSubmit: () => void;
  onPrev: () => void;
}

export const ManagerStep: React.FC<ManagerStepProps> = ({ managerData, setManagerData, isSubmitting, onSubmit, onPrev }) => {
  // 닉네임, 주활동 지역, 생년월일이 필수값
  const isFormValid = Boolean(
    managerData.nickname?.trim() &&
    managerData.location?.trim() &&
    managerData.birthDate &&
    managerData.gender
  );

  useEffect(() => {
    console.log('[Signup Debug] ManagerStep Status:', {
      nickname: managerData.nickname,
      location: managerData.location,
      birthDate: managerData.birthDate,
      isFormValid
    });
  }, [managerData, isFormValid]);

  const fileInputRef = React.useRef<HTMLInputElement>(null);

  const handleAddressSearch = () => {
    if (!window.daum) return;
    new window.daum.Postcode({
      oncomplete: (data: any) => {
        let fullAddress = data.address;
        let extraAddress = '';

        if (data.addressType === 'R') {
          if (data.bname !== '') extraAddress += data.bname;
          if (data.buildingName !== '') extraAddress += extraAddress !== '' ? `, ${data.buildingName}` : data.buildingName;
          fullAddress += extraAddress !== '' ? ` (${extraAddress})` : '';
        }

        setManagerData({ ...managerData, location: fullAddress });
      },
    }).open();
  };

  const handleImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onloadend = () => {
      setManagerData({ ...managerData, avatar: reader.result as string });
    };
    reader.readAsDataURL(file);
  };

  return (
    <form 
      onSubmit={(e) => { e.preventDefault(); if(isFormValid) onSubmit(); }}
      className="space-y-10 animate-in fade-in duration-500"
    >
      <Script src="//t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js" strategy="lazyOnload" />
      
      <div className="text-center space-y-4">
        <Badge variant="amber" className="px-4 py-1.5 bg-amber-50 text-amber-600 border-none">Step 03. Manager Profile</Badge>
        <Typography variant="h2" className="text-4xl md:text-5xl font-serif font-black text-navy-900 tracking-tight">당신을 <span className="text-amber-500 italic">소개</span>해주세요.</Typography>
      </div>

      <div className="space-y-8 bg-zinc-50/50 p-8 rounded-[48px] border border-zinc-100">
        <div className="flex flex-col items-center gap-6">
          <div 
            onClick={() => fileInputRef.current?.click()}
            className="w-32 h-32 rounded-[40px] bg-white shadow-2xl border-4 border-white overflow-hidden relative group cursor-pointer"
          >
            {managerData.avatar ? (
              <img src={managerData.avatar} className="w-full h-full object-cover" alt="Manager" />
            ) : (
              <div className="w-full h-full bg-zinc-100 flex items-center justify-center text-zinc-300 group-hover:bg-zinc-200 transition-colors">
                <User size={48} strokeWidth={1.5} />
              </div>
            )}
            <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center text-white">
              <Camera size={24} />
            </div>
          </div>
          <input 
            type="file" 
            ref={fileInputRef} 
            className="hidden" 
            accept="image/*" 
            onChange={handleImageUpload} 
          />
          
          <div className="w-full space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-3">
                <Typography variant="label" className="flex items-center gap-2 ml-2"><User size={14} className="text-amber-500" /> 활동 닉네임 (필수)</Typography>
                <input 
                  name="nickname"
                  type="text" 
                  placeholder="동네에서 불릴 이름" 
                  className="w-full bg-white border border-zinc-100 rounded-[24px] py-5 px-8 font-bold text-navy-900 shadow-sm outline-none focus:ring-4 ring-amber-500/5 transition-all" 
                  value={managerData.nickname} 
                  onChange={(e) => setManagerData({...managerData, nickname: e.target.value})} 
                />
              </div>

              <div className="space-y-3">
                <Typography variant="label" className="flex items-center gap-2 ml-2"><Calendar size={14} className="text-amber-500" /> 생년월일 (필수)</Typography>
                <input 
                  name="userBirthDate"
                  type="date" 
                  className="w-full bg-white border border-zinc-100 rounded-[24px] py-5 px-8 font-bold text-navy-900 shadow-sm focus:ring-4 ring-amber-500/10 transition-all outline-none" 
                  value={managerData.birthDate} 
                  onChange={(e) => setManagerData({...managerData, birthDate: e.target.value})} 
                />
              </div>
            </div>

            <div className="space-y-3">
              <Typography variant="label" className="flex items-center gap-2 ml-2"><User size={14} className="text-amber-500" /> 성별 (필수)</Typography>
              <div className="flex gap-3">
                {[{ value: 'M', label: '남성' }, { value: 'F', label: '여성' }].map(({ value, label }) => (
                  <button
                    key={value}
                    type="button"
                    onClick={() => setManagerData({ ...managerData, gender: value as 'M' | 'F' })}
                    className={cn(
                      "flex-1 py-5 rounded-[24px] text-sm font-black transition-all border",
                      managerData.gender === value
                        ? "bg-amber-500 text-white border-amber-500 shadow-lg"
                        : "bg-white text-zinc-400 border-zinc-100 hover:border-amber-200"
                    )}
                  >
                    {label}
                  </button>
                ))}
              </div>
            </div>

            <div className="space-y-3">
              <div className="flex items-center justify-between px-2">
                <Typography variant="label" className="flex items-center gap-2"><Phone size={14} className="text-amber-500" /> 휴대폰 번호</Typography>
                <span className="text-[10px] font-black text-zinc-300 uppercase">Optional</span>
              </div>
              <input
                name="phone"
                type="tel"
                placeholder="010-0000-0000"
                className="w-full bg-white border border-zinc-100 rounded-[24px] py-5 px-8 font-bold text-navy-900 shadow-sm outline-none focus:ring-4 ring-amber-500/5 transition-all"
                value={managerData.phone}
                onChange={(e) => setManagerData({ ...managerData, phone: e.target.value })}
              />
            </div>

            <div className="space-y-3">
              <Typography variant="label" className="flex items-center gap-2 ml-2"><MapPin size={14} className="text-amber-500" /> 주활동 지역 (필수)</Typography>
              <div className="flex gap-3">
                <input 
                  name="location"
                  type="text" 
                  readOnly
                  placeholder="주소 검색을 눌러주세요" 
                  className="flex-1 bg-zinc-50 border border-zinc-100 rounded-[24px] py-5 px-8 font-bold text-navy-900 shadow-sm cursor-pointer" 
                  value={managerData.location} 
                  onClick={handleAddressSearch}
                />
                <Button 
                  type="button" 
                  onClick={handleAddressSearch} 
                  variant="outline" 
                  className="rounded-[24px] px-8 h-[68px] border-zinc-200 hover:border-amber-500 hover:text-amber-600 transition-all"
                >
                  <Search size={20} className="mr-2" /> 주소 검색
                </Button>
              </div>
            </div>

            <div className="space-y-4">
              <div className="flex items-center justify-between px-2">
                <Typography variant="label" className="flex items-center gap-2"><Sparkles size={14} className="text-amber-500" /> 나의 MBTI</Typography>
                <span className="text-[10px] font-black text-zinc-300 uppercase">Optional</span>
              </div>
              <div className="flex flex-wrap gap-2">
                {['ENFP', 'ENFJ', 'ENTP', 'ENTJ', 'ESFP', 'ESFJ', 'ESTP', 'ESTJ', 'INFP', 'INFJ', 'INTP', 'INTJ', 'ISFP', 'ISFJ', 'ISTP', 'ISTJ'].map((mbti) => (
                  <button
                    key={mbti}
                    type="button"
                    onClick={() => setManagerData({ ...managerData, mbti: managerData.mbti === mbti ? '' : mbti })}
                    className={cn(
                      "px-4 py-2 rounded-full text-sm font-bold border-2 transition-all",
                      managerData.mbti === mbti
                        ? "bg-amber-500 border-amber-500 text-white"
                        : "bg-zinc-50 border-zinc-100 text-zinc-500 hover:border-amber-300"
                    )}
                  >
                    {mbti}
                  </button>
                ))}
              </div>
            </div>

            <div className="space-y-3">
              <div className="flex items-center justify-between px-2">
                <Typography variant="label" className="flex items-center gap-2"><Sparkles size={14} className="text-amber-500" /> 한 줄 소개</Typography>
                <span className="text-[10px] font-black text-zinc-300 uppercase">Optional</span>
              </div>
              <textarea 
                name="about"
                placeholder="이웃들에게 전할 따뜻한 인사말 (미입력 시 기본 문구가 설정됩니다)" 
                className="w-full h-32 bg-white border border-zinc-100 rounded-[32px] py-6 px-8 font-bold text-navy-900 shadow-sm resize-none focus:ring-4 ring-amber-500/10 transition-all outline-none" 
                value={managerData.about} 
                onChange={(e) => setManagerData({...managerData, about: e.target.value})} 
              />
            </div>
          </div>
        </div>
      </div>

      <div className="flex gap-4">
        <Button type="button" onClick={onPrev} variant="outline" size="xl" className="flex-1 py-8">이전으로</Button>
        <Button type="submit" disabled={!isFormValid || isSubmitting} variant="primary" size="xl" className="flex-[2] py-8 shadow-2xl">
          {isSubmitting ? <Loader2 className="animate-spin mr-3" /> : <Check className="mr-3" />}
          가입 완료하기
        </Button>
      </div>
    </form>
  );
};
