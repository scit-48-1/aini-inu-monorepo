'use client';

import React, { useState } from 'react';
import { Star, X, Check, Heart, Loader2 } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import { cn } from '@/lib/utils';
import { toast } from 'sonner';
import { createReview } from '@/api/chat';

interface WalkReviewModalProps {
  isOpen: boolean;
  onClose: () => void;
  revieweeId: number;
  revieweeName: string;
  chatRoomId: number;
  onReviewSubmitted: () => void;
}

export const WalkReviewModal: React.FC<WalkReviewModalProps> = ({
  isOpen,
  onClose,
  revieweeId,
  revieweeName,
  chatRoomId,
  onReviewSubmitted,
}) => {
  const [rating, setRating] = useState(0);
  const [hoverRating, setHoverRating] = useState(0);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [selectedTags, setSelectedTags] = useState<string[]>([]);
  const [comment, setComment] = useState('');

  const positiveTags = ['시간약속을 잘 지켜요', '친절하고 배려심 넘쳐요', '강아지가 사교적이에요', '매너가 아주 좋아요'];
  const negativeTags = ['시간을 안 지켰어요', '공격성이 조금 있었어요', '소통이 원활하지 않아요'];

  const toggleTag = (tag: string) => {
    setSelectedTags(prev => prev.includes(tag) ? prev.filter(t => t !== tag) : [...prev, tag]);
  };

  const handleSubmit = async () => {
    if (rating === 0) {
      toast.warning('별점을 선택해 주세요!');
      return;
    }
    setIsSubmitting(true);
    try {
      // Concatenate tags into comment string (backend has no tags field)
      const fullComment = [...selectedTags, comment].filter(Boolean).join('; ');
      await createReview(chatRoomId, {
        revieweeId,
        score: rating,
        comment: fullComment,
      });
      setIsSuccess(true);
      onReviewSubmitted();
      setTimeout(() => {
        setIsSuccess(false);
        onClose();
      }, 2000);
    } catch {
      toast.error('리뷰 등록 중 오류가 발생했습니다.');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-[6000] bg-navy-900/40 backdrop-blur-md flex items-center justify-center p-6 animate-in fade-in duration-300">
      <Card className="w-full max-w-xl bg-white shadow-2xl rounded-[48px] border-none overflow-hidden relative animate-in zoom-in-95 duration-500">
        <button onClick={onClose} className="absolute top-8 right-8 p-2 text-zinc-300 hover:text-navy-900 transition-colors z-10"><X size={32} /></button>

        <div className="p-10 md:p-14 space-y-10">
          {isSuccess ? (
            <div className="py-10 text-center space-y-6 animate-in zoom-in-90">
              <div className="w-24 h-24 bg-amber-50 text-amber-500 rounded-full flex items-center justify-center mx-auto shadow-inner">
                <Heart size={48} fill="currentColor" />
              </div>
              <div className="space-y-2">
                <Typography variant="h2" className="text-3xl font-black">리뷰 전송 완료!</Typography>
                <Typography variant="body" className="text-zinc-400">전달해주신 마음이 이웃의 매너 온도에 반영됩니다.</Typography>
              </div>
            </div>
          ) : (
            <>
              <div className="text-center space-y-6">
                <Typography variant="label" className="text-amber-500 font-black tracking-[0.3em] uppercase text-[10px]">Walk Review</Typography>
                <div className="flex flex-col items-center gap-4">
                  <div className="w-24 h-24 rounded-full border-4 border-zinc-50 shadow-xl overflow-hidden">
                    <img
                      src="/AINIINU_ROGO_B.png"
                      className="w-full h-full object-cover"
                      alt={revieweeName}
                    />
                  </div>
                  <div className="space-y-1">
                    <Typography variant="h2" className="text-2xl font-black text-navy-900">@{revieweeName}</Typography>
                  </div>
                </div>
                <Typography variant="serif" className="text-3xl md:text-4xl italic">오늘 산책은 <span className="text-amber-500">어떠셨나요?</span></Typography>
              </div>

              {/* Star Rating */}
              <div className="flex flex-col items-center gap-4 py-4">
                <div className="flex gap-2">
                  {[1, 2, 3, 4, 5].map((s) => (
                    <button
                      key={s}
                      onMouseEnter={() => setHoverRating(s)}
                      onMouseLeave={() => setHoverRating(0)}
                      onClick={() => setRating(s)}
                      className="transition-transform active:scale-90 hover:scale-110"
                    >
                      <Star
                        size={48}
                        fill={(hoverRating || rating) >= s ? '#F59E0B' : 'transparent'}
                        className={cn(
                          "transition-colors duration-300",
                          (hoverRating || rating) >= s ? 'text-amber-500' : 'text-zinc-100'
                        )}
                        strokeWidth={1.5}
                      />
                    </button>
                  ))}
                </div>
                <Typography variant="label" className="text-[10px] font-black text-zinc-300 uppercase tracking-widest">Tap to rate</Typography>
              </div>

              {/* Quick Tags */}
              <div className="space-y-4">
                <Typography variant="label" className="text-navy-900 font-black text-[10px] uppercase tracking-widest ml-1">Recommend Points</Typography>
                <div className="flex flex-wrap gap-2 justify-center">
                  {(rating >= 4 ? positiveTags : [...positiveTags, ...negativeTags]).map((tag) => (
                    <button
                      key={tag}
                      onClick={() => toggleTag(tag)}
                      className={cn(
                        "px-5 py-2.5 rounded-full text-xs font-bold transition-all border-2",
                        selectedTags.includes(tag)
                          ? "bg-navy-900 border-navy-900 text-white shadow-lg"
                          : "bg-white border-zinc-50 text-zinc-400 hover:border-amber-100 hover:text-amber-600"
                      )}
                    >
                      {tag}
                    </button>
                  ))}
                </div>
              </div>

              {/* Comment Area */}
              <div className="space-y-3">
                <Typography variant="label" className="text-navy-900 font-black text-[10px] uppercase tracking-widest ml-1">Special Thanks</Typography>
                <textarea
                  value={comment}
                  onChange={(e) => setComment(e.target.value)}
                  placeholder="이웃에게 남기고 싶은 따뜻한 한마디를 적어주세요."
                  className="w-full h-32 bg-zinc-50 border-none rounded-[32px] p-6 text-sm font-medium focus:outline-none focus:ring-4 ring-amber-500/5 resize-none no-scrollbar"
                />
              </div>

              <Button
                variant="primary"
                size="xl"
                fullWidth
                className="py-8 text-xl shadow-2xl shadow-navy-900/20"
                onClick={handleSubmit}
                disabled={isSubmitting || rating === 0}
              >
                {isSubmitting ? <Loader2 className="animate-spin mr-3" /> : <Check className="mr-3" />}
                {isSubmitting ? '전송 중...' : '리뷰 보내기'}
              </Button>
            </>
          )}
        </div>
      </Card>
    </div>
  );
};
