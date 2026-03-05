'use client';

import React, { useState, useRef } from 'react';
import { createPortal } from 'react-dom';
import { X, User, Camera, Edit2, Check, Loader2, Lock, Phone, Sparkles } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import { cn } from '@/lib/utils';
import DaumPostcode from 'react-daum-postcode';

const MBTI_LIST = ['ENFP', 'ENFJ', 'ENTP', 'ENTJ', 'ESFP', 'ESFJ', 'ESTP', 'ESTJ', 'INFP', 'INFJ', 'INTP', 'INTJ', 'ISFP', 'ISFJ', 'ISTP', 'ISTJ'];

interface ProfileEditModalProps {
  isOpen: boolean;
  onClose: () => void;
  user: any;
  onSave: (data: any) => Promise<boolean>;
  optimizeImage: (base64: string) => Promise<string>;
}

export const ProfileEditModal: React.FC<ProfileEditModalProps> = ({
  isOpen,
  onClose,
  user,
  onSave,
  optimizeImage
}) => {
  // 닉네임 30일 쿨다운 계산
  const daysSinceNicknameChange = user?.nicknameChangedAt
    ? Math.floor((Date.now() - new Date(user.nicknameChangedAt).getTime()) / (1000 * 60 * 60 * 24))
    : 999;
  const isNicknameLocked = daysSinceNicknameChange < 30;
  const daysUntilChange = 30 - daysSinceNicknameChange;

  const [form, setForm] = useState({
    nickname: user?.nickname || '',
    handle: user?.handle || '',
    avatar: user?.avatar || '',
    about: user?.about || '',
    location: user?.location || '서울시 성수동',
    gender: (user?.gender || 'M') as 'M' | 'F',
    mbti: user?.mbti || '',
    phone: user?.phone || '',
  });
  const [isLocationModalOpen, setIsLocationModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  if (!isOpen) return null;

  const handleAddressComplete = (data: any) => {
    setForm(prev => ({ ...prev, location: data.address }));
    setIsLocationModalOpen(false);
  };

  const handleSave = async () => {
    setIsSubmitting(true);
    const nicknameChanged = form.nickname !== user?.nickname;
    const payload = {
      ...form,
      // 닉네임이 변경된 경우에만 변경 시각 갱신
      ...(nicknameChanged && { nicknameChangedAt: new Date().toISOString() }),
    };
    const success = await onSave(payload);
    if (success) onClose();
    setIsSubmitting(false);
  };

  return createPortal(
    <div
      className="fixed inset-0 z-[2000] bg-navy-900/40 backdrop-blur-md flex items-center justify-center p-6 animate-in fade-in duration-300"
      onClick={(e) => e.target === e.currentTarget && onClose()}
    >
      <Card className="w-full max-w-2xl p-0 bg-white shadow-2xl animate-in zoom-in-95 duration-500 border-none overflow-hidden flex flex-col max-h-[90vh] rounded-[48px]">
        {/* 헤더 */}
        <div className="flex items-center justify-between p-8 shrink-0 border-b border-zinc-50">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-amber-50 rounded-xl flex items-center justify-center text-amber-500">
              <User size={24} />
            </div>
            <Typography variant="h3" className="text-navy-900">프로필 편집</Typography>
          </div>
          <button onClick={onClose} className="p-2 text-zinc-300 hover:text-navy-900 transition-colors">
            <X size={32} />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-8 space-y-8 no-scrollbar">
          {/* 아바타 업로드 */}
          <div className="flex flex-col items-center space-y-4">
            <div className="relative group cursor-pointer" onClick={() => fileInputRef.current?.click()}>
              <div className="w-32 h-32 rounded-[40px] overflow-hidden border-4 border-zinc-50 shadow-xl bg-zinc-100 flex items-center justify-center group-hover:scale-105 transition-transform">
                {form.avatar ? (
                  <img src={form.avatar} className="w-full h-full object-cover" alt="Avatar Preview" />
                ) : (
                  <User size={48} className="text-zinc-300" />
                )}
              </div>
              <div className="absolute inset-0 bg-black/20 group-hover:bg-black/40 transition-all flex items-center justify-center text-white opacity-0 group-hover:opacity-100 rounded-[40px]">
                <Camera size={24} />
              </div>
              <input
                type="file"
                ref={fileInputRef}
                className="hidden"
                accept="image/*"
                onChange={async (e) => {
                  const file = e.target.files?.[0];
                  if (file) {
                    const reader = new FileReader();
                    reader.onloadend = async () => {
                      const optimized = await optimizeImage(reader.result as string);
                      setForm(prev => ({ ...prev, avatar: optimized }));
                    };
                    reader.readAsDataURL(file);
                  }
                }}
              />
            </div>
            <Typography variant="label" className="text-zinc-400 text-[10px] uppercase font-black tracking-widest">Update Profile Photo</Typography>
          </div>

          <div className="space-y-6">
            {/* 이메일 (수정 불가) */}
            <div className="space-y-2">
              <Typography variant="label" className="text-zinc-300 ml-1 flex items-center gap-1.5">
                <Lock size={11} /> 이메일 (수정 불가)
              </Typography>
              <input
                type="email"
                disabled
                value={user?.email || ''}
                className="w-full bg-zinc-50 border-none rounded-2xl py-4 px-6 font-bold text-zinc-300 cursor-not-allowed"
              />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {/* 닉네임 (30일 쿨다운) */}
              <div className="space-y-2">
                <div className="flex items-center justify-between ml-1">
                  <Typography variant="label" className={cn("flex items-center gap-1.5", isNicknameLocked ? "text-amber-400" : "text-navy-900")}>
                    {isNicknameLocked && <Lock size={11} />}
                    닉네임
                  </Typography>
                  {isNicknameLocked && (
                    <span className="text-[10px] font-black text-amber-400">{daysUntilChange}일 후 변경 가능</span>
                  )}
                </div>
                <input
                  type="text"
                  disabled={isNicknameLocked}
                  value={form.nickname}
                  onChange={(e) => setForm({ ...form, nickname: e.target.value })}
                  className={cn(
                    "w-full border-none rounded-2xl py-4 px-6 font-bold transition-all focus:ring-4 ring-amber-500/10",
                    isNicknameLocked
                      ? "bg-amber-50 text-amber-300 cursor-not-allowed"
                      : "bg-zinc-50 text-navy-900"
                  )}
                />
              </div>

              {/* 핸들 (@) */}
              <div className="space-y-2">
                <Typography variant="label" className="text-navy-900 ml-1">핸들 (@)</Typography>
                <input
                  type="text"
                  value={form.handle}
                  onChange={(e) => setForm({ ...form, handle: e.target.value })}
                  className="w-full bg-zinc-50 border-none rounded-2xl py-4 px-6 font-bold text-navy-900 focus:ring-4 ring-amber-500/10 transition-all"
                />
              </div>
            </div>

            {/* 성별 */}
            <div className="space-y-2">
              <Typography variant="label" className="text-navy-900 ml-1">성별</Typography>
              <div className="flex gap-3">
                {([{ value: 'M', label: '남성' }, { value: 'F', label: '여성' }] as const).map(({ value, label }) => (
                  <button
                    key={value}
                    type="button"
                    onClick={() => setForm({ ...form, gender: value })}
                    className={cn(
                      "flex-1 py-4 rounded-2xl text-sm font-black transition-all border",
                      form.gender === value
                        ? "bg-amber-500 text-white border-amber-500 shadow-lg"
                        : "bg-zinc-50 text-zinc-400 border-transparent hover:border-amber-200"
                    )}
                  >
                    {label}
                  </button>
                ))}
              </div>
            </div>

            {/* 휴대폰 번호 */}
            <div className="space-y-2">
              <Typography variant="label" className="text-navy-900 ml-1 flex items-center gap-1.5">
                <Phone size={11} className="text-amber-500" /> 휴대폰 번호
              </Typography>
              <input
                type="tel"
                placeholder="010-0000-0000"
                value={form.phone}
                onChange={(e) => setForm({ ...form, phone: e.target.value })}
                className="w-full bg-zinc-50 border-none rounded-2xl py-4 px-6 font-bold text-navy-900 focus:ring-4 ring-amber-500/10 transition-all"
              />
            </div>

            {/* MBTI */}
            <div className="space-y-2">
              <div className="flex items-center justify-between ml-1">
                <Typography variant="label" className="text-navy-900 flex items-center gap-1.5">
                  <Sparkles size={11} className="text-amber-500" /> MBTI
                </Typography>
                {form.mbti && (
                  <button
                    type="button"
                    onClick={() => setForm({ ...form, mbti: '' })}
                    className="text-[10px] font-black text-zinc-300 hover:text-red-400 transition-colors"
                  >
                    초기화
                  </button>
                )}
              </div>
              <div className="flex gap-2 overflow-x-auto no-scrollbar pb-1">
                {MBTI_LIST.map((mbti) => (
                  <button
                    key={mbti}
                    type="button"
                    onClick={() => setForm({ ...form, mbti: form.mbti === mbti ? '' : mbti })}
                    className={cn(
                      "flex-shrink-0 px-4 py-3 rounded-2xl text-xs font-black transition-all border",
                      form.mbti === mbti
                        ? "bg-amber-500 text-white border-amber-500 shadow-lg"
                        : "bg-zinc-50 text-zinc-400 border-transparent hover:border-amber-200"
                    )}
                  >
                    {mbti}
                  </button>
                ))}
              </div>
            </div>

            {/* 주활동 지역 */}
            <div className="space-y-2">
              <Typography variant="label" className="text-navy-900 ml-1">주활동 지역</Typography>
              <button
                onClick={() => setIsLocationModalOpen(true)}
                className="w-full bg-zinc-50 border-none rounded-2xl py-4 px-6 font-bold text-left text-navy-900 shadow-inner hover:bg-zinc-100 transition-all flex justify-between items-center"
              >
                {form.location}
                <Edit2 size={16} className="text-zinc-300" />
              </button>
            </div>

            {/* 자기소개 */}
            <div className="space-y-2">
              <Typography variant="label" className="text-navy-900 ml-1">자기소개</Typography>
              <textarea
                value={form.about}
                onChange={(e) => setForm({ ...form, about: e.target.value })}
                className="w-full h-32 bg-zinc-50 border-none rounded-[32px] p-6 font-medium text-navy-900 focus:ring-4 ring-amber-500/10 transition-all resize-none no-scrollbar shadow-inner"
              />
            </div>
          </div>

          <Button
            variant="primary"
            fullWidth
            size="lg"
            className="py-6 shadow-xl bg-navy-900"
            onClick={handleSave}
            disabled={isSubmitting || !form.nickname}
          >
            {isSubmitting ? <Loader2 className="animate-spin mr-2" /> : <Check className="mr-2" size={20} />}
            수정 완료
          </Button>
        </div>
      </Card>

      {/* 주소 검색 중첩 모달 */}
      {isLocationModalOpen && (
        <div
          className="fixed inset-0 z-[3000] bg-navy-900/40 backdrop-blur-md flex items-center justify-center p-6 animate-in fade-in duration-300"
          onClick={(e) => e.target === e.currentTarget && setIsLocationModalOpen(false)}
        >
          <Card className="w-full max-w-2xl p-0 bg-white shadow-2xl animate-in zoom-in-95 duration-500 border-none overflow-hidden flex flex-col h-[600px] rounded-[48px]">
            <div className="flex items-center justify-between p-8 shrink-0 border-b border-zinc-50">
              <Typography variant="h3" className="text-navy-900 font-serif">동네 찾기</Typography>
              <button onClick={() => setIsLocationModalOpen(false)} className="p-2 text-zinc-300 hover:text-navy-900 transition-colors">
                <X size={32} />
              </button>
            </div>
            <div className="flex-1 overflow-y-auto">
              <DaumPostcode onComplete={handleAddressComplete} style={{ height: '100%' }} />
            </div>
          </Card>
        </div>
      )}
    </div>,
    document.body
  );
};
