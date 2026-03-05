'use client';

import React from 'react';
import { X, AlertCircle } from 'lucide-react';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { cn } from '@/lib/utils';

interface ConfirmModalProps {
  isOpen: boolean;
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  onConfirm: () => void;
  onCancel: () => void;
  variant?: 'danger' | 'primary';
}

export const ConfirmModal = ({
  isOpen,
  title,
  message,
  confirmLabel = '확인',
  cancelLabel = '취소',
  onConfirm,
  onCancel,
  variant = 'primary'
}: ConfirmModalProps) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-[9999] flex items-center justify-center p-6 animate-in fade-in duration-300">
      {/* Backdrop */}
      <div 
        className="absolute inset-0 bg-navy-900/40 backdrop-blur-md" 
        onClick={onCancel}
      />
      
      {/* Modal Content */}
      <Card className="relative w-full max-w-md p-8 bg-white shadow-2xl rounded-[40px] animate-in zoom-in-95 duration-300 border-none space-y-8 text-center">
        <div className="flex flex-col items-center space-y-4">
          <div className={cn(
            "w-16 h-16 rounded-3xl flex items-center justify-center mb-2",
            variant === 'danger' ? "bg-red-50 text-red-500" : "bg-amber-50 text-amber-500"
          )}>
            <AlertCircle size={32} />
          </div>
          <Typography variant="h3" className="text-2xl text-navy-900 font-serif lowercase italic">
            {title}
          </Typography>
          <Typography variant="body" className="text-zinc-500 font-medium leading-relaxed whitespace-pre-line">
            {message}
          </Typography>
        </div>

        <div className="flex gap-4 pt-2">
          <Button 
            variant="outline" 
            fullWidth 
            className="h-14 rounded-2xl border-zinc-100 font-black text-zinc-400"
            onClick={onCancel}
          >
            {cancelLabel}
          </Button>
          <Button 
            variant={variant === 'danger' ? 'danger' : 'primary'} 
            fullWidth 
            className={cn(
              "h-14 rounded-2xl font-black shadow-lg",
              variant === 'primary' && "bg-navy-900 text-white"
            )}
            onClick={onConfirm}
          >
            {confirmLabel}
          </Button>
        </div>
      </Card>
    </div>
  );
};
