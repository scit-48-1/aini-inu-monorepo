'use client';

import React from 'react';
import { createPortal } from 'react-dom';
import { AlertTriangle, Loader2 } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';

interface DeleteConfirmDialogProps {
  isOpen: boolean;
  petName: string;
  onConfirm: () => void;
  onCancel: () => void;
  isDeleting: boolean;
}

export const DeleteConfirmDialog: React.FC<DeleteConfirmDialogProps> = ({
  isOpen,
  petName,
  onConfirm,
  onCancel,
  isDeleting,
}) => {
  if (!isOpen) return null;

  return createPortal(
    <div
      className="fixed inset-0 z-[4000] bg-black/60 backdrop-blur-sm flex items-center justify-center p-6 animate-in fade-in duration-200"
      onClick={(e) => e.target === e.currentTarget && !isDeleting && onCancel()}
    >
      <Card className="w-full max-w-sm p-8 bg-white shadow-2xl animate-in zoom-in-95 duration-300 border-none rounded-[40px] space-y-6">
        <div className="flex flex-col items-center gap-4 text-center">
          <div className="w-16 h-16 bg-red-50 rounded-full flex items-center justify-center">
            <AlertTriangle size={32} className="text-red-500" />
          </div>
          <div className="space-y-2">
            <Typography variant="h3" className="text-navy-900">
              &ldquo;{petName}&rdquo;을(를) 정말 삭제하시겠습니까?
            </Typography>
            <Typography variant="body" className="text-zinc-400 text-sm">
              삭제된 정보는 복구할 수 없습니다.
            </Typography>
          </div>
        </div>
        <div className="flex gap-3">
          <Button
            type="button"
            variant="outline"
            size="lg"
            className="flex-1"
            onClick={onCancel}
            disabled={isDeleting}
          >
            취소
          </Button>
          <Button
            type="button"
            variant="danger"
            size="lg"
            className="flex-1"
            onClick={onConfirm}
            disabled={isDeleting}
          >
            {isDeleting ? (
              <Loader2 className="animate-spin" size={18} />
            ) : (
              '삭제'
            )}
          </Button>
        </div>
      </Card>
    </div>,
    document.body
  );
};
