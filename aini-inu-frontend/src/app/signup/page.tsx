'use client';

import React, { useState, useEffect } from 'react';
import { Mail, User, Dog, CheckCircle2 } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Card } from '@/components/ui/Card';
import { SignupAccountStep } from '@/components/signup/SignupAccountStep';
import { SignupProfileStep } from '@/components/signup/SignupProfileStep';
import { SignupPetStep } from '@/components/signup/SignupPetStep';
import { SignupComplete } from '@/components/signup/SignupComplete';
import { useAuthStore } from '@/store/useAuthStore';
import type { LoginResponse } from '@/api/auth';

type SignupStep = 'ACCOUNT' | 'PROFILE' | 'PET' | 'COMPLETE';

const STEPS = [
  { id: 'ACCOUNT' as SignupStep, icon: Mail, label: 'Account' },
  { id: 'PROFILE' as SignupStep, icon: User, label: 'Profile' },
  { id: 'PET' as SignupStep, icon: Dog, label: 'Pet' },
  { id: 'COMPLETE' as SignupStep, icon: CheckCircle2, label: 'Done' },
];

const STEP_ORDER: SignupStep[] = ['ACCOUNT', 'PROFILE', 'PET', 'COMPLETE'];

export default function SignupPage() {
  const [mounted, setMounted] = useState(false);
  const [step, setStep] = useState<SignupStep>('ACCOUNT');
  const [nickname, setNickname] = useState('');

  useEffect(() => setMounted(true), []);

  if (!mounted) return null;

  const handleAccountComplete = (tokens: LoginResponse, accountNickname: string) => {
    useAuthStore.getState().setTokens(tokens.accessToken, tokens.refreshToken);
    setNickname(accountNickname);
    setStep('PROFILE');
  };

  const handleProfileComplete = () => {
    setStep('PET');
  };

  const handlePetComplete = () => {
    setStep('COMPLETE');
  };

  const handlePetSkip = () => {
    setStep('COMPLETE');
  };

  return (
    <div className="min-h-screen bg-[#FDFCF8] flex flex-col items-center justify-center p-6 lg:p-12 overflow-y-auto no-scrollbar relative pb-32">
      <div className="fixed top-0 right-0 w-[500px] h-[500px] bg-amber-500/5 rounded-full -mr-64 -mt-64 blur-[100px] pointer-events-none" />
      <div className="fixed bottom-0 left-0 w-[500px] h-[500px] bg-navy-900/5 rounded-full -ml-64 -mb-64 blur-[100px] pointer-events-none" />

      {/* Progress Indicator */}
      <div className="max-w-xl w-full mb-16 flex justify-between relative mt-10">
        <div className="absolute top-1/2 left-0 right-0 h-0.5 bg-zinc-100 -translate-y-1/2 z-0" />
        {STEPS.map((s, i) => {
          const isActive = step === s.id;
          const isPast = STEP_ORDER.indexOf(step) > i;
          return (
            <div key={s.id} className="flex flex-col items-center gap-3 relative z-10">
              <div className={cn(
                "w-12 h-12 md:w-14 md:h-14 rounded-2xl flex items-center justify-center border-4 transition-all duration-500 shadow-sm",
                isActive
                  ? 'bg-navy-900 border-amber-500 text-white scale-110'
                  : isPast
                    ? 'bg-amber-500 border-amber-500 text-white'
                    : 'bg-white border-zinc-100 text-zinc-300'
              )}>
                <s.icon size={20} className="md:size-6" />
              </div>
              <span className={cn(
                "text-[10px] font-black uppercase tracking-widest hidden md:block",
                isActive ? "text-navy-900" : "text-zinc-300"
              )}>
                {s.label}
              </span>
            </div>
          );
        })}
      </div>

      <Card className="max-w-3xl w-full bg-white rounded-[64px] border-none shadow-2xl p-8 md:p-20 space-y-12 animate-in slide-in-from-bottom-8 duration-700 relative">
        {step === 'ACCOUNT' && (
          <SignupAccountStep onComplete={handleAccountComplete} />
        )}
        {step === 'PROFILE' && (
          <SignupProfileStep
            initialNickname={nickname}
            onComplete={handleProfileComplete}
            onPrev={() => setStep('ACCOUNT')}
          />
        )}
        {step === 'PET' && (
          <SignupPetStep
            onComplete={handlePetComplete}
            onSkip={handlePetSkip}
            onPrev={() => setStep('PROFILE')}
          />
        )}
        {step === 'COMPLETE' && (
          <SignupComplete nickname={nickname} />
        )}
      </Card>
    </div>
  );
}
