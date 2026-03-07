'use client';

import React from 'react';
import { Send, Clock } from 'lucide-react';
import { Typography } from '@/components/ui/Typography';
import { cn } from '@/lib/utils';

interface ChatInputProps {
  inputText: string;
  setInputText: (text: string) => void;
  onSend: () => void;
  isArchived?: boolean;
}

export const ChatInput: React.FC<ChatInputProps> = ({
  inputText,
  setInputText,
  onSend,
  isArchived,
}) => {
  if (isArchived) {
    return (
      <footer className="p-10 bg-zinc-100/50 text-center border-t border-zinc-200">
        <div className="flex flex-col items-center gap-2 opacity-30">
          <Clock size={24} className="text-navy-900" />
          <Typography
            variant="label"
            className="text-navy-900 font-black tracking-[0.2em] text-[10px] uppercase"
          >
            This conversation has ended
          </Typography>
        </div>
      </footer>
    );
  }

  return (
    <footer className="p-6 md:p-10 bg-white border-t border-zinc-50 z-20">
      <div className="max-w-5xl mx-auto space-y-4">
        <div className="flex gap-2 overflow-x-auto no-scrollbar pb-2">
          {[
            '곧 도착해요! 🏃',
            '몽이가 좋아해요 ✨',
            '다음에 또 봐요 👋',
            '매너 최고예요 👍',
          ].map((reply) => (
            <button
              key={reply}
              onClick={() => setInputText(reply)}
              className="px-4 py-2 bg-zinc-50 hover:bg-zinc-100 border border-zinc-100 rounded-full text-[10px] font-black text-zinc-500 whitespace-nowrap transition-all active:scale-95"
            >
              {reply}
            </button>
          ))}
        </div>

        <div className="relative group flex items-center gap-4">
          <div className="flex-1 relative">
            <input
              type="text"
              value={inputText}
              onChange={(e) => setInputText(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && !e.nativeEvent.isComposing && onSend()}
              maxLength={500}
              placeholder="따뜻한 메시지를 남겨주세요..."
              className="w-full bg-zinc-50 border-none rounded-[32px] py-6 pl-8 pr-20 font-bold focus:outline-none focus:ring-8 ring-navy-900/5 transition-all text-navy-900 shadow-inner"
            />
            <button
              onClick={onSend}
              className={cn(
                'absolute right-3 top-1/2 -translate-y-1/2 w-12 h-12 rounded-full flex items-center justify-center shadow-lg transition-all active:scale-90',
                inputText.trim()
                  ? 'bg-amber-500 text-navy-900 scale-100'
                  : 'bg-zinc-200 text-white scale-90',
              )}
            >
              <Send size={20} strokeWidth={3} />
            </button>
          </div>
        </div>

        {/* Character counter */}
        <div className="flex justify-end pr-2">
          <span
            className={cn(
              'text-[10px] font-bold',
              inputText.length >= 450 ? 'text-red-500' : 'text-zinc-400',
            )}
          >
            {inputText.length}/500
          </span>
        </div>
      </div>
    </footer>
  );
};
