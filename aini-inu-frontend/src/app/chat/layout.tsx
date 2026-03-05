'use client';

import React from 'react';
import { useParams } from 'next/navigation';
import { ChatList } from '@/components/chat/ChatList';
import { cn } from '@/lib/utils';

export default function ChatLayout({ children }: { children: React.ReactNode }) {
  const params = useParams();
  const isRoomActive = params?.id;

  return (
    <div className="flex h-full w-full bg-white min-[672px]:bg-transparent overflow-hidden">
      <div className="flex w-full h-full min-w-0">
        {/* Sidebar: Chat List */}
        <aside className={cn(
          "h-full border-r border-zinc-100 shrink-0 transition-all duration-500 ease-in-out",
          isRoomActive 
            ? "hidden min-[672px]:flex min-[672px]:w-[320px] min-[960px]:w-[380px] min-[672px]:flex-none" 
            : "flex flex-1 w-full min-[672px]:w-[320px] min-[960px]:w-[380px] min-[672px]:flex-none"
        )}>
          <ChatList />
        </aside>

        {/* Main Content: Chat Room or Placeholder */}
        <main className={cn(
          "h-full relative transition-all duration-500 ease-in-out flex-1 min-w-0",
          isRoomActive 
            ? "flex w-full" 
            : "hidden min-[672px]:flex"
        )}>
          {children}
        </main>
      </div>
    </div>
  );
}