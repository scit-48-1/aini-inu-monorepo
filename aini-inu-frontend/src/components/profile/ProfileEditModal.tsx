'use client';

import React, { useState, useRef, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { X, User, Camera, Check, Loader2, Lock, Phone, Sparkles, Link2 } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import { cn } from '@/lib/utils';
import { updateMe, getPersonalityTypes } from '@/api/members';
import type { MemberResponse, MemberPersonalityTypeResponse, MemberProfilePatchRequest } from '@/api/members';
import { toast } from 'sonner';

const MBTI_LIST = ['ENFP', 'ENFJ', 'ENTP', 'ENTJ', 'ESFP', 'ESFJ', 'ESTP', 'ESTJ', 'INFP', 'INFJ', 'INTP', 'INTJ', 'ISFP', 'ISFJ', 'ISTP', 'ISTJ'];

type GenderValue = 'MALE' | 'FEMALE' | 'UNKNOWN';

interface ProfileEditModalProps {
  isOpen: boolean;
  onClose: () => void;
  member: MemberResponse;
  onSaved: () => Promise<void>;
  optimizeImage: (base64: string) => Promise<string>;
}

export const ProfileEditModal: React.FC<ProfileEditModalProps> = ({
  isOpen,
  onClose,
  member,
  onSaved,
  optimizeImage,
}) => {
  // Nickname 30-day cooldown
  const daysSinceNicknameChange = member?.nicknameChangedAt
    ? Math.floor((Date.now() - new Date(member.nicknameChangedAt).getTime()) / (1000 * 60 * 60 * 24))
    : 999;
  const isNicknameLocked = daysSinceNicknameChange < 30;
  const daysUntilChange = 30 - daysSinceNicknameChange;

  const [form, setForm] = useState({
    nickname: member?.nickname || '',
    profileImageUrl: member?.profileImageUrl || '',
    linkedNickname: member?.linkedNickname || '',
    phone: member?.phone || '',
    age: member?.age ? String(member.age) : '',
    gender: (member?.gender || 'UNKNOWN') as GenderValue,
    mbti: member?.mbti || '',
    personality: member?.personality || '',
    selfIntroduction: member?.selfIntroduction || '',
    personalityTypeIds: (member?.personalityTypes || []).map(pt => pt.id),
  });

  const [personalityTypes, setPersonalityTypes] = useState<MemberPersonalityTypeResponse[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Fetch personality types when modal opens
  useEffect(() => {
    if (!isOpen) return;
    getPersonalityTypes()
      .then(setPersonalityTypes)
      .catch(() => {
        // Non-fatal: personality types may just not show
      });
  }, [isOpen]);

  if (!isOpen) return null;

  const handleSave = async () => {
    setIsSubmitting(true);
    try {
      const payload: MemberProfilePatchRequest = {
        nickname: form.nickname || undefined,
        profileImageUrl: form.profileImageUrl || undefined,
        linkedNickname: form.linkedNickname || undefined,
        phone: form.phone || undefined,
        age: form.age ? Number(form.age) : undefined,
        gender: form.gender || undefined,
        mbti: form.mbti || undefined,
        personality: form.personality || undefined,
        selfIntroduction: form.selfIntroduction || undefined,
        personalityTypeIds: form.personalityTypeIds.length > 0 ? form.personalityTypeIds : undefined,
      };
      await updateMe(payload);
      toast.success('프로필이 수정되었습니다.');
      onClose();
      await onSaved();
    } catch {
      toast.error('프로필 수정에 실패했습니다.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const togglePersonalityType = (id: number) => {
    setForm(prev => ({
      ...prev,
      personalityTypeIds: prev.personalityTypeIds.includes(id)
        ? prev.personalityTypeIds.filter(x => x !== id)
        : [...prev.personalityTypeIds, id],
    }));
  };

  return createPortal(
    <div
      className="fixed inset-0 z-[2000] bg-navy-900/40 backdrop-blur-md flex items-center justify-center p-6 animate-in fade-in duration-300"
      onClick={(e) => e.target === e.currentTarget && onClose()}
    >
      <Card className="w-full max-w-2xl p-0 bg-white shadow-2xl animate-in zoom-in-95 duration-500 border-none overflow-hidden flex flex-col max-h-[90vh] rounded-[48px]">
        {/* Header */}
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
          {/* Profile image upload */}
          <div className="flex flex-col items-center space-y-4">
            <div className="relative group cursor-pointer" onClick={() => fileInputRef.current?.click()}>
              <div className="w-32 h-32 rounded-[40px] overflow-hidden border-4 border-zinc-50 shadow-xl bg-zinc-100 flex items-center justify-center group-hover:scale-105 transition-transform">
                {form.profileImageUrl ? (
                  <img src={form.profileImageUrl} className="w-full h-full object-cover" alt="Profile Preview" />
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
                      setForm(prev => ({ ...prev, profileImageUrl: optimized }));
                    };
                    reader.readAsDataURL(file);
                  }
                }}
              />
            </div>
            <Typography variant="label" className="text-zinc-400 text-[10px] uppercase font-black tracking-widest">Update Profile Photo</Typography>
          </div>

          <div className="space-y-6">
            {/* Email (read-only) */}
            <div className="space-y-2">
              <Typography variant="label" className="text-zinc-300 ml-1 flex items-center gap-1.5">
                <Lock size={11} /> 이메일 (수정 불가)
              </Typography>
              <input
                type="email"
                disabled
                value={member?.email || ''}
                className="w-full bg-zinc-50 border-none rounded-2xl py-4 px-6 font-bold text-zinc-300 cursor-not-allowed"
              />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {/* Nickname (30-day cooldown) */}
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

              {/* Linked nickname */}
              <div className="space-y-2">
                <Typography variant="label" className="text-navy-900 ml-1 flex items-center gap-1.5">
                  <Link2 size={11} className="text-amber-500" /> 링크드 닉네임 (@)
                </Typography>
                <input
                  type="text"
                  placeholder="@handle"
                  value={form.linkedNickname}
                  onChange={(e) => setForm({ ...form, linkedNickname: e.target.value })}
                  className="w-full bg-zinc-50 border-none rounded-2xl py-4 px-6 font-bold text-navy-900 focus:ring-4 ring-amber-500/10 transition-all"
                />
              </div>
            </div>

            {/* Gender */}
            <div className="space-y-2">
              <Typography variant="label" className="text-navy-900 ml-1">성별</Typography>
              <div className="flex gap-3">
                {([{ value: 'MALE', label: '남성' }, { value: 'FEMALE', label: '여성' }, { value: 'UNKNOWN', label: '비공개' }] as const).map(({ value, label }) => (
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

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {/* Age */}
              <div className="space-y-2">
                <Typography variant="label" className="text-navy-900 ml-1">나이</Typography>
                <input
                  type="number"
                  min={1}
                  max={120}
                  placeholder="나이를 입력하세요"
                  value={form.age}
                  onChange={(e) => setForm({ ...form, age: e.target.value })}
                  className="w-full bg-zinc-50 border-none rounded-2xl py-4 px-6 font-bold text-navy-900 focus:ring-4 ring-amber-500/10 transition-all"
                />
              </div>

              {/* Phone */}
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

            {/* Personality (free text) */}
            <div className="space-y-2">
              <Typography variant="label" className="text-navy-900 ml-1">성향 (자유 텍스트)</Typography>
              <input
                type="text"
                placeholder="ex) 활발한, 친근한..."
                value={form.personality}
                onChange={(e) => setForm({ ...form, personality: e.target.value })}
                className="w-full bg-zinc-50 border-none rounded-2xl py-4 px-6 font-bold text-navy-900 focus:ring-4 ring-amber-500/10 transition-all"
              />
            </div>

            {/* Personality types (multi-select chips) */}
            {personalityTypes.length > 0 && (
              <div className="space-y-2">
                <Typography variant="label" className="text-navy-900 ml-1 flex items-center gap-1.5">
                  <Sparkles size={11} className="text-amber-500" /> 성향 태그
                </Typography>
                <div className="flex flex-wrap gap-2">
                  {personalityTypes.map((pt) => {
                    const isSelected = form.personalityTypeIds.includes(pt.id);
                    return (
                      <button
                        key={pt.id}
                        type="button"
                        onClick={() => togglePersonalityType(pt.id)}
                        className={cn(
                          "px-4 py-2 rounded-2xl text-xs font-black transition-all border",
                          isSelected
                            ? "bg-amber-500 text-white border-amber-500 shadow-lg"
                            : "bg-zinc-50 text-zinc-400 border-transparent hover:border-amber-200"
                        )}
                      >
                        {pt.name}
                      </button>
                    );
                  })}
                </div>
              </div>
            )}

            {/* Self introduction */}
            <div className="space-y-2">
              <Typography variant="label" className="text-navy-900 ml-1">자기소개</Typography>
              <textarea
                value={form.selfIntroduction}
                onChange={(e) => setForm({ ...form, selfIntroduction: e.target.value })}
                placeholder="자신을 소개해 보세요..."
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
    </div>,
    document.body
  );
};
