'use client';

import React from 'react';
import { Typography } from '@/components/ui/Typography';
import { Card } from '@/components/ui/Card';
import { LoginForm } from '@/components/auth/LoginForm';

export default function LoginPage() {
  return (
    <div className="min-h-full bg-[#fdfbf7] flex items-center justify-center p-6">
      <Card className="w-full max-w-lg bg-white shadow-2xl rounded-[48px] p-10 md:p-16 border-none animate-in zoom-in-95 duration-700">
        <div className="text-center space-y-4 mb-12">
          <div className="w-20 h-20 bg-amber-50 rounded-[32px] flex items-center justify-center mx-auto mb-6 shadow-inner">
            <img src="/AINIINU_ROGO_B.png" className="w-12 h-12 object-contain" alt="Logo" />
          </div>
          <Typography variant="h2" className="text-4xl font-black text-navy-900 tracking-tighter italic">
            Welcome Back!
          </Typography>
          <Typography variant="body" className="text-zinc-400 font-bold">
            아이니 이누에 다시 오신 것을 환영해요.
          </Typography>
        </div>

        <LoginForm />
      </Card>
    </div>
  );
}
