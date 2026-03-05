'use client';

import React, { useState, useRef } from 'react';
import { createPortal } from 'react-dom';
import { X, Dog, Check } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import { useDogForm } from '@/hooks/forms/useDogForm';
import { DogFormFields } from '@/components/shared/forms/DogFormFields';
import { toast } from 'sonner';

interface DogRegisterModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (data: any) => Promise<boolean>;
  editingDog?: any;
  optimizeImage: (base64: string) => Promise<string>;
}

export const DogRegisterModal: React.FC<DogRegisterModalProps> = ({ 
  isOpen, onClose, onSave, editingDog, optimizeImage
}) => {
  const {
    dogForm, setDogForm, isVerified, breedSuggestions, showBreedSuggestions, 
    setShowBreedSuggestions, handleBreedChange, selectBreed, toggleTendency, handleVerify
  } = useDogForm(editingDog);

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [isVerifying, setIsVerifying] = useState(false);
  const [ownerName, setOwnerName] = useState('');
  const fileInputRef = useRef<HTMLInputElement>(null);

  if (!isOpen) return null;

  const handleVerifyWrapper = async (name: string) => {
    setIsVerifying(true);
    const result = await handleVerify(name);
    setIsVerifying(false);
    return result;
  };

  const handleSubmit = async () => {
    if (!dogForm.name) return toast.warning('강아지 이름을 입력해주세요.');
    if (!dogForm.breed) return toast.warning('강아지 품종을 선택해주세요.');
    if (!dogForm.age) return toast.warning('강아지 나이를 입력해주세요.');
    if (dogForm.tendencies.length === 0) return toast.warning('아이의 성향을 최소 1개 이상 선택해주세요!');
    
    setIsSubmitting(true);
    const success = await onSave({ ...dogForm, isVerified });
    if (success) {
      setIsSuccess(true);
      setTimeout(() => { setIsSuccess(false); onClose(); }, 1000);
    }
    setIsSubmitting(false);
  };

  return createPortal(
    <div className="fixed inset-0 z-[2000] bg-navy-900/40 backdrop-blur-md flex items-center justify-center p-6 animate-in fade-in duration-300" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <Card className="w-full max-w-2xl p-0 bg-white shadow-2xl animate-in zoom-in-95 duration-500 border-none overflow-hidden flex flex-col max-h-[90vh] rounded-[48px]">
        <div className="flex items-center justify-between p-8 shrink-0 border-b border-zinc-50">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-amber-50 rounded-xl flex items-center justify-center text-amber-500">
              <Dog size={24} />
            </div>
            <Typography variant="h3" className="text-navy-900">{editingDog ? '반려견 정보 수정' : '새로운 반려견 등록'}</Typography>
          </div>
          <button onClick={onClose} className="p-2 text-zinc-300 hover:text-navy-900 transition-colors"><X size={32} /></button>
        </div>
        <div className="flex-1 overflow-y-auto p-8 space-y-10 no-scrollbar">
          {isSuccess ? (
            <div className="py-20 text-center space-y-6 animate-in zoom-in-90">
              <div className="w-24 h-24 bg-emerald-50 text-emerald-500 rounded-full flex items-center justify-center mx-auto shadow-inner"><Check size={48} strokeWidth={3} /></div>
              <Typography variant="h2" className="text-3xl font-black">완료!</Typography>
            </div>
          ) : (
            <>
              <DogFormFields 
                dogForm={dogForm} setDogForm={setDogForm} isVerified={isVerified}
                onVerify={handleVerifyWrapper} isVerifying={isVerifying}
                ownerName={ownerName} setOwnerName={setOwnerName}
                onBreedChange={handleBreedChange} breedSuggestions={breedSuggestions}
                showBreedSuggestions={showBreedSuggestions} setShowBreedSuggestions={setShowBreedSuggestions}
                onSelectBreed={selectBreed} onToggleTendency={toggleTendency}
                onImageUpload={async (base64) => {
                  const optimized = await optimizeImage(base64);
                  setDogForm({...dogForm, image: optimized});
                }}
                fileInputRef={fileInputRef}
              />
              <div className="pt-4 pb-8">
                <Button 
                  variant="secondary" fullWidth size="lg" className="py-6 shadow-xl" 
                  onClick={handleSubmit} disabled={isSubmitting}
                >
                  {isSubmitting ? '처리 중...' : (editingDog ? '수정 완료' : '반려견 등록 완료')}
                </Button>
              </div>
            </>
          )}
        </div>
      </Card>
    </div>,
    document.body
  );
};