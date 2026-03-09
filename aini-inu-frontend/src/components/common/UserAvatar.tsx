'use client';

import React from 'react';
import { cn } from '@/lib/utils';
import { OptimizedImage } from '@/components/common/OptimizedImage';

interface UserAvatarProps {
  src: string;
  alt?: string;
  /** 24시간 이내 다이어리 작성 여부 — true일 때만 amber ring 표시 */
  hasRecentDiary?: boolean;
  size?: 'sm' | 'md' | 'lg' | 'xl';
  className?: string;
}

const sizeConfig = {
  sm: { wrapper: 'w-8 h-8', imgBorder: 'border', sizes: '32px' },
  md: { wrapper: 'w-16 h-16', imgBorder: 'border-2', sizes: '64px' },
  lg: { wrapper: 'w-24 h-24', imgBorder: 'border-[3px]', sizes: '96px' },
  xl: { wrapper: 'w-24 h-24 lg:w-44 lg:h-44', imgBorder: 'border-4', sizes: '(min-width: 1024px) 176px, 96px' },
};

export const UserAvatar: React.FC<UserAvatarProps> = ({
  src,
  alt = '프로필',
  hasRecentDiary = false,
  size = 'md',
  className,
}) => {
  const { wrapper, imgBorder, sizes } = sizeConfig[size];

  return (
    <div className={cn(
      wrapper, 'rounded-full shadow-xl shrink-0 relative',
      hasRecentDiary ? 'p-1 bg-gradient-to-tr from-amber-400 to-amber-600' : 'p-0',
      className
    )}>
      <div className="relative w-full h-full">
        <OptimizedImage
          src={src}
          alt={alt}
          fill
          sizes={sizes}
          className={cn(
            'rounded-full object-cover',
            hasRecentDiary && `${imgBorder} border-white`
          )}
        />
      </div>
    </div>
  );
};
