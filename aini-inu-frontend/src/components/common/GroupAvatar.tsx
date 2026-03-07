'use client';

import React from 'react';
import { cn } from '@/lib/utils';

const FALLBACK = '/AINIINU_ROGO_B.png';

interface GroupAvatarProps {
  images: (string | null)[];
  size?: 'sm' | 'md';
  className?: string;
}

const sizeMap = {
  sm: 'w-8 h-8',
  md: 'w-12 h-12',
};

function AvatarCell({ src, className }: { src: string | null; className?: string }) {
  return (
    <img
      src={src || FALLBACK}
      alt="프로필"
      className={cn('object-cover', className)}
    />
  );
}

export const GroupAvatar: React.FC<GroupAvatarProps> = ({
  images,
  size = 'md',
  className,
}) => {
  const count = Math.min(images.length, 4);
  const items = images.slice(0, 4);

  if (count <= 1) {
    return (
      <div className={cn(sizeMap[size], 'rounded-full overflow-hidden shrink-0', className)}>
        <AvatarCell src={items[0] ?? null} className="w-full h-full" />
      </div>
    );
  }

  if (count === 2) {
    return (
      <div className={cn(sizeMap[size], 'rounded-full overflow-hidden shrink-0 flex', className)}>
        <AvatarCell src={items[0] ?? null} className="w-1/2 h-full" />
        <AvatarCell src={items[1] ?? null} className="w-1/2 h-full" />
      </div>
    );
  }

  if (count === 3) {
    return (
      <div className={cn(sizeMap[size], 'rounded-full overflow-hidden shrink-0 grid grid-cols-2 grid-rows-2', className)}>
        <AvatarCell src={items[0] ?? null} className="col-span-2 w-full h-full" />
        <AvatarCell src={items[1] ?? null} className="w-full h-full" />
        <AvatarCell src={items[2] ?? null} className="w-full h-full" />
      </div>
    );
  }

  // 4
  return (
    <div className={cn(sizeMap[size], 'rounded-full overflow-hidden shrink-0 grid grid-cols-2 grid-rows-2', className)}>
      <AvatarCell src={items[0] ?? null} className="w-full h-full" />
      <AvatarCell src={items[1] ?? null} className="w-full h-full" />
      <AvatarCell src={items[2] ?? null} className="w-full h-full" />
      <AvatarCell src={items[3] ?? null} className="w-full h-full" />
    </div>
  );
};
