'use client';

import React, { useState } from 'react';
import { Heart, PlusCircle } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import Link from 'next/link';
import { MemberSearchModal } from '@/components/dashboard/MemberSearchModal';
import { OptimizedImage } from '@/components/common/OptimizedImage';

interface Friend {
  id: string;
  roomId: string;
  name: string;
  img: string;
  score: number;
}

interface RecentFriendsProps {
  friends: Friend[];
}

export const RecentFriends: React.FC<RecentFriendsProps> = ({ friends }) => {
  const isEmpty = !friends || friends.length === 0;
  const [isSearchOpen, setIsSearchOpen] = useState(false);

  return (
    <section className="space-y-6">
      <div className="flex items-center justify-between px-2">
        <Typography variant="h3" className="font-serif text-navy-900">최근 산책한 <span className="text-amber-500 italic">친구들</span></Typography>
        {!isEmpty && (
          <Typography
            variant="label"
            className="text-zinc-400 hover:text-amber-500 transition-colors cursor-pointer"
            onClick={() => setIsSearchOpen(true)}
          >
            친구 찾기
          </Typography>
        )}
      </div>

      {isEmpty ? (
        <Link href="/around-me">
          <Card interactive className="w-full py-12 flex flex-col items-center justify-center border-2 border-dashed border-zinc-100 bg-zinc-50/30 group">
            <div className="w-16 h-16 bg-white rounded-[24px] shadow-sm flex items-center justify-center text-zinc-300 mb-4 group-hover:scale-110 group-hover:text-amber-500 transition-all">
              <PlusCircle size={32} />
            </div>
            <Typography variant="body" className="font-bold text-zinc-400 group-hover:text-navy-900 transition-colors">아직 함께 산책한 친구가 없어요.</Typography>
            <Typography variant="label" className="text-zinc-300">동네에서 첫 산책 메이트를 찾아볼까요? ✨</Typography>
          </Card>
        </Link>
      ) : (
        <div className="flex gap-6 overflow-x-auto no-scrollbar pb-4 -mx-2 px-2">
          {friends.map((friend) => (
            <Link key={friend.id} href={`/profile/${friend.id}`}>
              <Card interactive className="min-w-[160px] max-w-[200px] flex-shrink-0 p-5">
                <div className="relative mb-4">
                  <div className="w-full aspect-square rounded-[32px] overflow-hidden shadow-lg relative">
                    <OptimizedImage src={friend.img} alt={friend.name} fill sizes="200px" className="object-cover" />
                  </div>
                  <div className="absolute -bottom-2 -right-2 w-8 h-8 bg-white rounded-full flex items-center justify-center text-red-500 shadow-md">
                    <Heart size={14} fill="currentColor" />
                  </div>
                </div>
                <div className="text-center space-y-1">
                  <Typography variant="body" className="font-black text-navy-900">{friend.name}</Typography>
                  <Typography variant="label" className="text-zinc-400">Score {friend.score}/10</Typography>
                </div>
              </Card>
            </Link>
          ))}

          <button
            onClick={() => setIsSearchOpen(true)}
            className="min-w-[160px] max-w-[200px] flex-shrink-0 flex flex-col items-center justify-center border-2 border-dashed border-zinc-200 rounded-[40px] text-zinc-300 hover:border-amber-500 hover:text-amber-500 transition-all"
          >
            <PlusCircle size={32} className="mb-2" />
            <Typography variant="label">친구 추가</Typography>
          </button>
        </div>
      )}

      <MemberSearchModal isOpen={isSearchOpen} onClose={() => setIsSearchOpen(false)} />
    </section>
  );
};
