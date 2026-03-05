'use client';

import { useState, useCallback } from 'react';
import { authService } from '@/services/authService';
import { DogFormData } from '@/types';
import { toast } from 'sonner';

export function useSignupForm() {
  const [step, setStep] = useState<'ACCOUNT' | 'MANAGER' | 'PET' | 'COMPLETE'>('ACCOUNT');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // 1. Account Info
  const [accountData, setAccountData] = useState({ email: '', password: '', confirmPassword: '' });
  const [isEmailVerified, setIsEmailVerified] = useState(false);

  // 2. Manager & Pet Info (Unified State)
  const [managerData, setManagerData] = useState({
    nickname: '',
    birthDate: '', // 추가: 보호자 생년월일
    age: '',
    gender: '' as '' | 'M' | 'F',
    phone: '',
    location: '',
    about: '',
    mbti: '',
    avatar: '',
    dogs: [{
      name: '',
      breed: '',
      birthDate: '',
      gender: 'M' as const,
      tendencies: [],
      walkStyle: '느긋함' as const,
      image: '',
      isNeutralized: false,
    }] as DogFormData[]
  });

  const handleSignup = useCallback(async () => {
    if (isSubmitting) return; // 이중 제출 방지 가드
    setIsSubmitting(true);
    try {
      // 나이 계산 헬퍼
      const calculateAge = (birthDate: string) => {
        if (!birthDate) return 0;
        const today = new Date();
        const birth = new Date(birthDate);
        let age = today.getFullYear() - birth.getFullYear();
        const m = today.getMonth() - birth.getMonth();
        if (m < 0 || (m === 0 && today.getDate() < birth.getDate())) age--;
        return age;
      };

      const signupPayload = {
        email: accountData.email,
        password: accountData.password,
        nickname: managerData.nickname,
        location: managerData.location,
        about: managerData.about || "반가워요! 함께 산책해요 🐾",
        mbti: managerData.mbti,
        gender: managerData.gender as 'M' | 'F',
        phone: managerData.phone || undefined,
        birthDate: managerData.birthDate,
        age: calculateAge(managerData.birthDate),
        dogs: managerData.dogs.map(d => ({
          ...d,
          age: d.birthDate ? calculateAge(d.birthDate) : 0,
          isMain: true
        }))
      };
      
      const result = await authService.signup(signupPayload);
      if (result) {
        setStep('COMPLETE');
        toast.success('회원가입이 완료되었습니다!');
      }
    } catch (error: any) {
      console.error("Signup error:", error);
      // 서버에서 보낸 구체적인 메시지(예: 중복 이메일)를 우선 표시
      toast.error(error.message || '회원가입 중 오류가 발생했습니다.');
    } finally {
      setIsSubmitting(false);
    }
  }, [accountData, managerData, isSubmitting]);

  return {
    step, setStep,
    isSubmitting,
    accountData, setAccountData,
    isEmailVerified, setIsEmailVerified,
    managerData, setManagerData,
    handleSignup
  };
}
