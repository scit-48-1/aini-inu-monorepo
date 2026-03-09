'use client';

import React, { useState, useRef } from 'react';
import { ArrowRight } from 'lucide-react';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { DogFormFields } from '@/components/shared/forms/DogFormFields';
import { DOG_BREEDS } from '@/constants/dogBreeds';

interface PetStepProps {
  dogData: any;
  setDogData: (data: any) => void;
  onNext: () => void;
  onPrev: () => void;
}

export const PetStep: React.FC<PetStepProps> = ({ dogData, setDogData, onNext, onPrev }) => {
  // Local state for UI logic
  const [showBreedSuggestions, setShowBreedSuggestions] = useState(false);
  const [breedSuggestions, setBreedSuggestions] = useState<any[]>([]);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const isFormValid = dogData.name && dogData.breed && dogData.birthDate;

  // Handlers
  const handleBreedChange = (val: string) => {
    setDogData({ ...dogData, breed: val });
    if (val.length > 0) {
      const filtered = DOG_BREEDS.filter(b => b.ko.includes(val) || b.en.toLowerCase().includes(val.toLowerCase()));
      setBreedSuggestions(filtered);
      setShowBreedSuggestions(true);
    } else {
      setShowBreedSuggestions(false);
    }
  };

  const handleSelectBreed = (breed: any) => {
    setDogData({ ...dogData, breed: breed.ko });
    setShowBreedSuggestions(false);
  };

  const handleToggleTendency = (tendency: string) => {
    const current = dogData.tendencies || [];
    if (current.includes(tendency)) {
      setDogData({ ...dogData, tendencies: current.filter((t: string) => t !== tendency) });
    } else {
      if (current.length < 6) {
        setDogData({ ...dogData, tendencies: [...current, tendency] });
      }
    }
  };

  return (
    <form 
      onSubmit={(e) => { e.preventDefault(); if(isFormValid) onNext(); }}
      className="space-y-10 animate-in fade-in duration-500"
    >
      <div className="text-center space-y-4">
        <Badge variant="amber" className="px-4 py-1.5 bg-indigo-50 text-indigo-600 border-none">Step 02. Pet Registration</Badge>
        <Typography variant="h2" className="text-4xl md:text-5xl font-serif font-black text-navy-900 tracking-tight">아이를 <span className="text-indigo-500 italic">등록</span>할까요?</Typography>
      </div>

      <DogFormFields 
        dogForm={dogData}
        setDogForm={setDogData}
        onBreedChange={handleBreedChange}
        breedSuggestions={breedSuggestions}
        showBreedSuggestions={showBreedSuggestions}
        setShowBreedSuggestions={setShowBreedSuggestions}
        onSelectBreed={handleSelectBreed}
        onToggleTendency={handleToggleTendency}
        onImageUpload={(base64) => setDogData({ ...dogData, image: base64 })}
        fileInputRef={fileInputRef}
      />

      <div className="flex gap-4">
        <Button type="button" onClick={onPrev} variant="outline" size="xl" className="flex-1 py-8">이전으로</Button>
        <Button type="submit" disabled={!isFormValid} variant="primary" size="xl" className="flex-[2] py-8 shadow-2xl bg-indigo-600 hover:bg-indigo-700">
          다음 단계로 <ArrowRight className="ml-2" />
        </Button>
      </div>
    </form>
  );
};
