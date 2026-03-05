'use client';

import { useEffect, useCallback } from 'react';
import { useUserStore } from '@/store/useUserStore';
import { UserType } from '@/types';
import { toast } from 'sonner';

/**
 * useProfile - useUserStore의 전역 상태를 사용하는 Hook
 * 개별 페이지에서 호출해도 동일한 메모리(Profile)를 공유합니다.
 */
export function useProfile() {
  const { profile, isLoading, fetchProfile, updateProfile: updateStoreProfile } = useUserStore();

  const updateProfile = useCallback(async (data: Partial<UserType>) => {
    const success = await updateStoreProfile(data);
    if (success) {
      toast.success('프로필이 수정되었습니다!');
    } else {
      toast.error('프로필 수정 중 오류가 발생했습니다.');
    }
    return success;
  }, [updateStoreProfile]);

  useEffect(() => {
    fetchProfile();
  }, [fetchProfile]);

  return { 
    profile, 
    isLoading, 
    fetchProfile, 
    updateProfile 
  };
}