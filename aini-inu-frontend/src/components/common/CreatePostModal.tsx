'use client';

import React, { useRef, useEffect } from 'react';
import { X, Loader2, Check } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import { usePostForm } from '@/hooks/forms/usePostForm';
import { PostFormFields } from '@/components/shared/forms/PostFormFields';
import { UserType } from '@/types';

interface CreatePostModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
  userProfile: UserType;
}

export const CreatePostModal: React.FC<CreatePostModalProps> = ({ isOpen, onClose, onSuccess, userProfile }) => {
  const { postForm, setPostForm, isSubmitting, handleSubmit } = usePostForm(() => {
    onSuccess();
    onClose();
  });

  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (isOpen && userProfile) {
      setPostForm(prev => ({ ...prev, location: userProfile.location || '서울시 성수동' }));
    }
  }, [isOpen, userProfile, setPostForm]);

  if (!isOpen) return null;

  return (
    <div 
      className="fixed inset-0 z-[5000] bg-black/60 backdrop-blur-md flex items-center justify-center p-6 animate-in fade-in duration-300"
      onClick={(e) => e.target === e.currentTarget && onClose()}
    >
      <Card className="w-full max-w-4xl bg-white shadow-2xl animate-in zoom-in-95 duration-500 border-none rounded-[48px] overflow-hidden flex flex-col max-h-[90vh]">
        <div className="flex items-center justify-between p-8 border-b border-zinc-50 shrink-0">
          <Typography variant="h3" className="text-navy-900 font-serif">새 게시물 작성</Typography>
          <button onClick={onClose} className="p-2 text-zinc-300 hover:text-navy-900 transition-colors"><X size={32} /></button>
        </div>

        <div className="flex-1 overflow-y-auto no-scrollbar">
          <PostFormFields 
            postForm={postForm} setPostForm={setPostForm} userProfile={userProfile}
            onImageUpload={(base64) => setPostForm({...postForm, images: [base64]})}
            fileInputRef={fileInputRef}
          />
        </div>

        <div className="p-8 border-t border-zinc-50">
          <Button 
            variant="primary" fullWidth size="xl" className="py-8 text-xl shadow-2xl"
            onClick={handleSubmit} disabled={isSubmitting}
          >
            {isSubmitting ? (
              <><Loader2 className="animate-spin mr-3" /> 게시 중...</>
            ) : (
              <><Check className="mr-3" /> 공유하기</>
            )}
          </Button>
        </div>
      </Card>
    </div>
  );
};