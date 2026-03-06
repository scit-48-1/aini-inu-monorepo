'use client';

import React, { use } from 'react';
import { useUserStore } from '@/store/useUserStore';
import { MyProfileView } from '@/components/profile/MyProfileView';
import { OtherProfileView } from '@/components/profile/OtherProfileView';

export default function ProfilePage({ params }: { params: Promise<{ memberId: string }> }) {
  const { memberId } = use(params);
  const myId = useUserStore((s) => s.profile?.id);

  const isMe = memberId === 'me' || memberId === String(myId);

  return (
    <div className="max-w-4xl mx-auto h-full overflow-y-auto no-scrollbar bg-white lg:bg-transparent">
      {isMe ? (
        <MyProfileView />
      ) : (
        <OtherProfileView memberId={Number(memberId)} />
      )}
    </div>
  );
}
