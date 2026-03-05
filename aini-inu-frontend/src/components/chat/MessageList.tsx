'use client';

import React, { useEffect, useRef } from 'react';
import { Typography } from '@/components/ui/Typography';
import { cn } from '@/lib/utils';
import { ChatRoomType } from '@/services/api/chatService';

interface MessageListProps {
  messages: any[]; // [TODO] ChatMessage Type update
  currentUserId: string;
  partner: { nickname: string; avatar: string };
}

export const MessageList: React.FC<MessageListProps> = ({ messages, currentUserId, partner }) => {
  const scrollRef = useRef<HTMLDivElement>(null);

  // Auto-scroll to bottom
  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages]);

  return (
    <div 
      ref={scrollRef}
      className="flex-1 overflow-y-auto p-4 md:p-6 space-y-6 no-scrollbar bg-[#fdfbf7]"
    >
      {messages.map((msg, i) => {
        const isMe = msg.senderId === currentUserId;
        const prevMsg = messages[i-1];
        const isSameSender = prevMsg?.senderId === msg.senderId;
        const timeStr = msg.timestamp 
          ? new Date(msg.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
          : '';

        return (
          <div key={msg.id || i} className={cn(
            "flex gap-3 max-w-[85%] md:max-w-[70%] animate-in fade-in slide-in-from-bottom-2 duration-300",
            isMe ? "ml-auto flex-row-reverse" : "mr-auto",
            isSameSender ? "mt-1" : "mt-4"
          )}>
            {/* Avatar (Left side only) */}
            {!isMe && (
              <div className="shrink-0 w-8 h-8 self-end mb-1">
                {!isSameSender && (
                  <img src={partner.avatar} alt="Avatar" className="w-full h-full rounded-full object-cover shadow-sm border border-white" />
                )}
              </div>
            )}

            <div className={cn("flex flex-col", isMe ? "items-end" : "items-start")}>
              {!isMe && !isSameSender && (
                <Typography variant="label" className="text-zinc-400 font-bold text-[10px] ml-1 mb-1">
                  {partner.nickname}
                </Typography>
              )}
              
              <div className={cn("flex items-end gap-1.5", isMe ? "flex-row-reverse" : "")}>
                <div className={cn(
                  "px-4 py-2.5 rounded-[20px] shadow-sm border transition-all text-sm font-medium leading-relaxed break-words whitespace-pre-wrap max-w-full",
                  isMe
                    ? "bg-navy-900 text-white border-navy-900 rounded-br-none"
                    : "bg-white border-zinc-100 text-navy-900 rounded-bl-none"
                )}>
                  {msg.content || msg.text}
                </div>
                <div className={cn("flex flex-col items-end gap-0.5 shrink-0 mb-1", !isMe && "items-start")}>
                  <span className="text-[9px] text-zinc-300 font-bold">{timeStr}</span>
                  {isMe && (
                    <span className="text-[8px] text-zinc-300 font-bold">전송 완료</span>
                  )}
                </div>
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
};
