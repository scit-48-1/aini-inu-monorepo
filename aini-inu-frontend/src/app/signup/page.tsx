'use client';

import React, { useState, useEffect } from 'react';
import { Mail, User, Dog, CheckCircle2, X } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import { AccountStep } from '@/components/signup/AccountStep';
import { ManagerStep } from '@/components/signup/ManagerStep';
import { PetStep } from '@/components/signup/PetStep';
import { SignupComplete } from '@/components/signup/SignupComplete';
import { useSignupForm } from '@/hooks/forms/useSignupForm';
import DaumPostcode from 'react-daum-postcode';

export default function SignupPage() {
  const [mounted, setMounted] = useState(false);
  const [isLocationModalOpen, setIsLocationModalOpen] = useState(false);
  
  const {
    step, setStep,
    isSubmitting,
    accountData, setAccountData,
    isEmailVerified, setIsEmailVerified,
    managerData, setManagerData,
    handleSignup
  } = useSignupForm();

  useEffect(() => setMounted(true), []);

  if (!mounted) return null;

  return (
    <div className="min-h-screen bg-[#FDFCF8] flex flex-col items-center justify-center p-6 lg:p-12 overflow-y-auto no-scrollbar relative pb-32">
      <div className="fixed top-0 right-0 w-[500px] h-[500px] bg-amber-500/5 rounded-full -mr-64 -mt-64 blur-[100px] pointer-events-none" />
      <div className="fixed bottom-0 left-0 w-[500px] h-[500px] bg-navy-900/5 rounded-full -ml-64 -mb-64 blur-[100px] pointer-events-none" />

      {/* Progress Indicator */}
      <div className="max-w-xl w-full mb-16 flex justify-between relative mt-10">
        <div className="absolute top-1/2 left-0 right-0 h-0.5 bg-zinc-100 -translate-y-1/2 z-0" />
        {[
          { id: 'ACCOUNT', icon: Mail, label: 'Account' },
          { id: 'PET', icon: Dog, label: 'Pet' },
          { id: 'MANAGER', icon: User, label: 'Profile' },
          { id: 'COMPLETE', icon: CheckCircle2, label: 'Done' }
        ].map((s, i) => {
          const stepOrder = ['ACCOUNT', 'PET', 'MANAGER', 'COMPLETE'];
          const isActive = step === s.id;
          const isPast = stepOrder.indexOf(step) > i;
          return (
            <div key={s.id} className="flex flex-col items-center gap-3 relative z-10">
              <div className={cn(
                "w-12 h-12 md:w-14 md:h-14 rounded-2xl flex items-center justify-center border-4 transition-all duration-500 shadow-sm",
                isActive ? 'bg-navy-900 border-amber-500 text-white scale-110' : isPast ? 'bg-amber-500 border-amber-500 text-white' : 'bg-white border-zinc-100 text-zinc-300'
              )}>
                <s.icon size={20} className="md:size-6" />
              </div>
              <span className={cn("text-[10px] font-black uppercase tracking-widest hidden md:block", isActive ? "text-navy-900" : "text-zinc-300")}>{s.label}</span>
            </div>
          );
        })}
      </div>

      <Card className="max-w-3xl w-full bg-white rounded-[64px] border-none shadow-2xl p-8 md:p-20 space-y-12 animate-in slide-in-from-bottom-8 duration-700 relative">
        {step === 'ACCOUNT' && (
          <AccountStep 
            accountData={accountData} 
            setAccountData={setAccountData} 
            isEmailVerified={isEmailVerified} 
            setIsEmailVerified={setIsEmailVerified} 
            onNext={() => setStep('PET')} 
          />
        )}
        {step === 'PET' && (
          <PetStep 
            dogData={managerData.dogs?.[0] || { name: '', breed: '', age: '', tendencies: [] }} 
            setDogData={(d) => setManagerData({...managerData, dogs: [d]})}
            onPrev={() => setStep('ACCOUNT')} 
            onNext={() => setStep('MANAGER')} 
          />
        )}
        {step === 'MANAGER' && (
          <ManagerStep 
            managerData={managerData} 
            setManagerData={setManagerData} 
            isSubmitting={isSubmitting} 
            onPrev={() => setStep('PET')}
            onSubmit={handleSignup} 
          />
        )}
        {step === 'COMPLETE' && (
          <SignupComplete nickname={managerData.nickname} petName={managerData.dogs?.[0]?.name || "반려견"} />
        )}
      </Card>

      {isLocationModalOpen && (
        <div className="fixed inset-0 z-[2000] bg-navy-900/40 backdrop-blur-md flex items-center justify-center p-6">
           <Card className="w-full max-w-2xl bg-white shadow-2xl overflow-hidden flex flex-col h-[600px] rounded-[48px]">
              <div className="flex items-center justify-between p-8 border-b border-zinc-50">
                <Typography variant="h3" className="text-navy-900 font-serif">동네 설정하기</Typography>
                <button onClick={() => setIsLocationModalOpen(false)} className="p-2 text-zinc-300 hover:text-navy-900 transition-colors"><X size={32} /></button>
              </div>
              <div className="flex-1 overflow-y-auto">
                <DaumPostcode onComplete={(data) => { setManagerData({...managerData, location: data.address}); setIsLocationModalOpen(false); }} style={{ height: '100%' }} />
              </div>
           </Card>
        </div>
      )}
    </div>
  );
}
