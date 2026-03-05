import { useState, useEffect, useCallback } from 'react';
import { memberService } from '@/services/api/memberService';
import { DogType } from '@/types';
import { toast } from 'sonner';

export function useMyDogs() {
  const [dogs, setDogs] = useState<DogType[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  const fetchDogs = useCallback(async () => {
    setIsLoading(true);
    try {
      const data = await memberService.getMyDogs();
      setDogs(data);
    } catch (e) {
      console.error('Failed to fetch dogs:', e);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const registerDog = async (dogData: Partial<DogType>) => {
    try {
      await memberService.registerDog(dogData);
      await fetchDogs();
      return true;
    } catch (e) {
      toast.error('반려견 등록 중 오류가 발생했습니다.');
      return false;
    }
  };

  const deleteDog = async (id: string | number) => {
    try {
      await memberService.deleteDog(id);
      await fetchDogs();
      toast.success('정상적으로 삭제되었습니다.');
      return true;
    } catch (e) {
      toast.error('삭제 중 오류가 발생했습니다.');
      return false;
    }
  };

  useEffect(() => {
    fetchDogs();
  }, [fetchDogs]);

  return { dogs, isLoading, fetchDogs, registerDog, deleteDog };
}
