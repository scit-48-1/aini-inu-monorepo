'use client';

import React, { use } from 'react';
import { ProfileView } from '@/components/profile/ProfileView';

export default function ProfilePage({ params }: { params: Promise<{ memberId: string }> }) {
  const resolvedParams = use(params);
  const memberId = resolvedParams?.memberId;

  return (
    <div className="max-w-4xl mx-auto h-full overflow-y-auto no-scrollbar bg-white lg:bg-transparent">
      <ProfileView memberId={memberId} />
    </div>
  );
}
