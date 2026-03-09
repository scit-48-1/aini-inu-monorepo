'use client';

import { useState, useCallback, useEffect } from 'react';
import { DOG_BREEDS, DogBreed } from '@/constants/dogBreeds';
import { DogType, WalkStyle, DogTendency } from '@/types';
import { toast } from 'sonner';

export function useDogForm(initialDog?: Partial<DogType>) {
  const [dogForm, setDogForm] = useState({
    name: initialDog?.name || '',
    breed: initialDog?.breed || '',
    age: initialDog?.age?.toString() || '',
    birthDate: initialDog?.birthDate || '', // 추가: 초기값 설정
    gender: (initialDog?.gender || 'M') as 'M' | 'F',
    tendencies: (initialDog?.tendencies || []) as DogTendency[],
    walkStyle: (initialDog?.walkStyle || '느긋함') as WalkStyle,
    image: initialDog?.image || '',
    mbti: initialDog?.mbti || '',
    isNeutralized: initialDog?.isNeutralized ?? true,
  });

  const [breedSuggestions, setBreedSuggestions] = useState<DogBreed[]>([]);
  const [showBreedSuggestions, setShowBreedSuggestions] = useState(false);

  useEffect(() => {
    setDogForm({
      name: initialDog?.name || '',
      breed: initialDog?.breed || '',
      age: initialDog?.age?.toString() || '',
      birthDate: initialDog?.birthDate || '',
      gender: (initialDog?.gender || 'M') as 'M' | 'F',
      tendencies: (initialDog?.tendencies || []) as DogTendency[],
      walkStyle: (initialDog?.walkStyle || '느긋함') as WalkStyle,
      image: initialDog?.image || '',
      mbti: initialDog?.mbti || '',
      isNeutralized: initialDog?.isNeutralized ?? true,
    });
  }, [initialDog]);

  const handleBreedChange = useCallback((val: string) => {
    setDogForm(prev => ({ ...prev, breed: val }));
    if (val.trim().length > 0) {
      const filtered = DOG_BREEDS.filter(b => 
        (b.ko && b.ko.includes(val)) || 
        (b.en && b.en.toLowerCase().includes(val.toLowerCase()))
      ).slice(0, 5);
      setBreedSuggestions(filtered);
      setShowBreedSuggestions(true);
    } else {
      setShowBreedSuggestions(false);
    }
  }, []);

  const selectBreed = useCallback((breed: DogBreed) => {
    setDogForm(prev => ({ ...prev, breed: breed.ko }));
    setShowBreedSuggestions(false);
  }, []);

  const toggleTendency = useCallback((personality: string) => {
    setDogForm(prev => {
      const exists = prev.tendencies.includes(personality as DogTendency);
      if (exists) return { ...prev, tendencies: prev.tendencies.filter(t => t !== personality) };
      if (prev.tendencies.length >= 6) {
        toast.warning('성향은 최대 6개까지 선택 가능합니다.');
        return prev;
      }
      return { ...prev, tendencies: [...prev.tendencies, personality as DogTendency] };
    });
  }, []);

  return {
    dogForm, setDogForm,
    breedSuggestions, showBreedSuggestions, setShowBreedSuggestions,
    handleBreedChange, selectBreed, toggleTendency
  };
}
