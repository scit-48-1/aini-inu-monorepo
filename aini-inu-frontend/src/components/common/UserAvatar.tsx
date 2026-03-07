'use client';

import React from 'react';
import { cn } from '@/lib/utils';

interface UserAvatarProps {
  src: string;
  alt?: string;
  /** 24시간 이내 다이어리 작성 여부 — true일 때만 amber ring 표시 */
  hasRecentDiary?: boolean;
  size?: 'sm' | 'md' | 'lg' | 'xl';
  className?: string;
}

const sizeConfig = {
  sm: { wrapper: 'w-8 h-8', imgBorder: 'border' },
  md: { wrapper: 'w-16 h-16', imgBorder: 'border-2' },
  lg: { wrapper: 'w-24 h-24', imgBorder: 'border-[3px]' },
  xl: { wrapper: 'w-24 h-24 lg:w-44 lg:h-44', imgBorder: 'border-4' },
};

export const UserAvatar: React.FC<UserAvatarProps> = ({
  src,
  alt = '프로필',
  hasRecentDiary = false,
  size = 'md',
  className,
}) => {
  const { wrapper, imgBorder } = sizeConfig[size];

  return (
    <div className={cn(
      wrapper, 'rounded-full shadow-xl shrink-0',
      hasRecentDiary ? 'p-1 bg-gradient-to-tr from-amber-400 to-amber-600' : 'p-0',
      className
    )}>
      <img
        src={src || '/AINIINU_ROGO_B.png'}
        alt={alt}
        className={cn(
          'w-full h-full rounded-full object-cover',
          hasRecentDiary && `${imgBorder} border-white`
        )}
      />
    </div>
  );
};
