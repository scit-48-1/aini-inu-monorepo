'use client';

import React, { useState, useMemo } from 'react';
import { Mail, Lock, Check, Loader2, ArrowRight } from 'lucide-react';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { authService } from '@/services/authService';
import { toast } from 'sonner';
import { cn } from '@/lib/utils';

interface AccountStepProps {
  accountData: any;
  setAccountData: (data: any) => void;
  isEmailVerified: boolean;
  setIsEmailVerified: (v: boolean) => void;
  onNext: () => void;
}

export const AccountStep: React.FC<AccountStepProps> = ({
  accountData, setAccountData, isEmailVerified, setIsEmailVerified, onNext
}) => {
  const [verificationCode, setVerificationCode] = useState('');
  const [isCodeSent, setIsCodeSent] = useState(false);
  const [isSendingCode, setIsSendingCode] = useState(false);
  const [isVerifyingCode, setIsVerifyingCode] = useState(false);

  const criteria = useMemo(() => ({
    length: accountData.password.length >= 8,
    upper: /[A-Z]/.test(accountData.password),
    lower: /[a-z]/.test(accountData.password),
    number: /[0-9]/.test(accountData.password),
    special: /[@$!%*?&]/.test(accountData.password),
  }), [accountData.password]);

  const isPasswordValid = Object.values(criteria).every(Boolean);
  const canGoNext = isEmailVerified && isPasswordValid && accountData.password === accountData.confirmPassword;

  const handleSendCode = async () => {
    setIsSendingCode(true);
    try {
      await authService.sendVerificationCode(accountData.email);
      setIsCodeSent(true);
      toast.success('인증 코드가 전송되었습니다. (1234)');
    } catch (e: any) { toast.error(e.message); } finally { setIsSendingCode(false); }
  };

  const handleVerifyCode = async () => {
    setIsVerifyingCode(true);
    try {
      await authService.verifyCode(accountData.email, verificationCode);
      setIsEmailVerified(true);
      toast.success('인증 완료!');
    } catch (e: any) { toast.error(e.message); } finally { setIsVerifyingCode(false); }
  };

  // 인증코드 입력 영역 표시 조건
  const showCodeInput = isCodeSent && !isEmailVerified;
  // 비밀번호 입력 영역 표시 조건
  const showPassword = isEmailVerified;

  return (
    <form
      onSubmit={(e) => { e.preventDefault(); if (canGoNext) onNext(); }}
      className="space-y-10 animate-in fade-in duration-500"
    >
      <div className="text-center space-y-4">
        <Badge variant="amber" className="px-4 py-1.5 bg-amber-50 text-amber-600 border-none">Step 01. Account Setting</Badge>
        <Typography variant="h2" className="text-4xl md:text-5xl font-serif font-black text-navy-900 tracking-tight">이메일로 <span className="text-amber-500 italic">가입</span>하기</Typography>
      </div>

      <div className="space-y-4">
        {/* 이메일 입력 */}
        <div className="space-y-3">
          <Typography variant="label" className="flex items-center gap-2"><Mail size={14} className="text-amber-500" /> 이메일 주소</Typography>
          <div className="flex gap-3">
            <div className="relative flex-1">
              <input
                name="email"
                type="email"
                autoComplete="email"
                placeholder="example@email.com"
                disabled={isEmailVerified}
                className={cn(
                  "w-full bg-zinc-50 border border-zinc-100 rounded-[24px] py-5 px-8 font-bold text-navy-900 transition-all outline-none focus:ring-4 ring-amber-500/5",
                  isEmailVerified && "bg-emerald-50 border-emerald-100 text-emerald-700"
                )}
                value={accountData.email}
                onChange={(e) => setAccountData({ ...accountData, email: e.target.value })}
              />
              {isEmailVerified && <Check className="absolute right-6 top-1/2 -translate-y-1/2 text-emerald-500" size={20} />}
            </div>
            {!isEmailVerified && (
              <Button type="button" onClick={handleSendCode} disabled={!accountData.email || isSendingCode} variant="outline" className="rounded-[24px] px-8 h-[68px]">
                {isSendingCode ? <Loader2 className="animate-spin" /> : (isCodeSent ? '재전송' : '인증 요청')}
              </Button>
            )}
          </div>
        </div>

        {/* 인증번호 입력 — grid-rows 트릭으로 부드러운 높이 전환 */}
        <div
          className={cn(
            "grid transition-all duration-500 ease-in-out",
            showCodeInput ? "grid-rows-[1fr] opacity-100" : "grid-rows-[0fr] opacity-0"
          )}
        >
          <div className="overflow-hidden">
            <div className="flex gap-3 pt-2 pb-1">
              <input
                name="code"
                type="text"
                placeholder="인증번호 1234 입력"
                autoComplete="one-time-code"
                tabIndex={showCodeInput ? 0 : -1}
                className="flex-1 bg-zinc-50 border border-zinc-100 rounded-[24px] py-5 px-8 font-bold text-navy-900 outline-none focus:ring-4 ring-amber-500/5 transition-all"
                value={verificationCode}
                onChange={(e) => setVerificationCode(e.target.value)}
              />
              <Button type="button" onClick={handleVerifyCode} disabled={!verificationCode || isVerifyingCode} variant="primary" className="rounded-[24px] px-8 h-[68px]">
                {isVerifyingCode ? <Loader2 className="animate-spin" /> : '인증 확인'}
              </Button>
            </div>
          </div>
        </div>

        {/* 비밀번호 입력 — grid-rows 트릭으로 부드러운 높이 전환 */}
        <div
          className={cn(
            "grid transition-all duration-700 ease-in-out",
            showPassword ? "grid-rows-[1fr] opacity-100" : "grid-rows-[0fr] opacity-0"
          )}
        >
          <div className="overflow-hidden">
            <div className="space-y-6 pt-4">
              <div className="space-y-3">
                <Typography variant="label" className="flex items-center gap-2"><Lock size={14} className="text-amber-500" /> 비밀번호 설정</Typography>
                <input
                  name="password"
                  type="password"
                  autoComplete="new-password"
                  placeholder="8자 이상, 대/소문자, 숫자, 특수문자"
                  tabIndex={showPassword ? 0 : -1}
                  className="w-full bg-zinc-50 border border-zinc-100 rounded-[24px] py-5 px-8 font-bold text-navy-900 outline-none focus:ring-4 ring-amber-500/5 transition-all"
                  value={accountData.password}
                  onChange={(e) => setAccountData({ ...accountData, password: e.target.value })}
                />
                <div className="flex flex-wrap gap-x-4 gap-y-2 px-4 pt-1">
                  {Object.entries(criteria).map(([key, val]) => (
                    <div key={key} className={cn("flex items-center gap-1.5 text-[10px] font-black", val ? "text-emerald-500" : "text-zinc-300")}>
                      {val ? <Check size={10} /> : <div className="w-2.5 h-2.5 rounded-full border border-zinc-200" />}
                      {key === 'length' ? '8자이상' : key === 'upper' ? '대문자' : key === 'lower' ? '소문자' : key === 'number' ? '숫자' : '특수문자'}
                    </div>
                  ))}
                </div>
              </div>

              <div className="space-y-3">
                <Typography variant="label" className="flex items-center gap-2"><Check size={14} className="text-amber-500" /> 비밀번호 확인</Typography>
                <input
                  name="confirmPassword"
                  type="password"
                  autoComplete="new-password"
                  placeholder="한 번 더 입력해주세요"
                  tabIndex={showPassword ? 0 : -1}
                  className="w-full bg-zinc-50 border border-zinc-100 rounded-[24px] py-5 px-8 font-bold text-navy-900 outline-none focus:ring-4 ring-amber-500/5 transition-all"
                  value={accountData.confirmPassword}
                  onChange={(e) => setAccountData({ ...accountData, confirmPassword: e.target.value })}
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      <Button type="submit" disabled={!canGoNext} variant="primary" size="xl" fullWidth className="py-8 shadow-2xl">
        다음 단계로 <ArrowRight className="ml-2" />
      </Button>
    </form>
  );
};
