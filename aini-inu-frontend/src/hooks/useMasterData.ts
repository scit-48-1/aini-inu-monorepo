'use client';

import { useState, useEffect } from 'react';
import { toast } from 'sonner';
import { getBreeds, getPersonalities, getWalkingStyles } from '@/api/pets';
import type { BreedResponse, PersonalityResponse, WalkingStyleResponse } from '@/api/pets';

interface MasterData {
  breeds: BreedResponse[];
  personalities: PersonalityResponse[];
  walkingStyles: WalkingStyleResponse[];
  isLoading: boolean;
  error: boolean;
}

export function useMasterData(): MasterData {
  const [breeds, setBreeds] = useState<BreedResponse[]>([]);
  const [personalities, setPersonalities] = useState<PersonalityResponse[]>([]);
  const [walkingStyles, setWalkingStyles] = useState<WalkingStyleResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(false);

  useEffect(() => {
    let cancelled = false;

    const fetchAll = async () => {
      setIsLoading(true);
      setError(false);
      try {
        const [breedsRes, personalitiesRes, walkingStylesRes] = await Promise.all([
          getBreeds(),
          getPersonalities(),
          getWalkingStyles(),
        ]);
        if (!cancelled) {
          setBreeds(breedsRes || []);
          setPersonalities(personalitiesRes || []);
          setWalkingStyles(walkingStylesRes || []);
        }
      } catch {
        if (!cancelled) {
          setError(true);
          toast.error('마스터 데이터를 불러오는데 실패했습니다.');
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    };

    fetchAll();

    return () => {
      cancelled = true;
    };
  }, []);

  return { breeds, personalities, walkingStyles, isLoading, error };
}
