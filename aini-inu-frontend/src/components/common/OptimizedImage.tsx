'use client';

import React, { useState } from 'react';
import Image from 'next/image';
import { cn } from '@/lib/utils';

const DEFAULT_FALLBACK = '/AINIINU_ROGO_B.png';

interface OptimizedImageProps {
  src: string | undefined | null;
  alt: string;
  width?: number;
  height?: number;
  fill?: boolean;
  className?: string;
  priority?: boolean;
  sizes?: string;
  fallbackSrc?: string;
}

export const OptimizedImage: React.FC<OptimizedImageProps> = ({
  src,
  alt,
  width,
  height,
  fill,
  className,
  priority = false,
  sizes,
  fallbackSrc = DEFAULT_FALLBACK,
}) => {
  const [imgSrc, setImgSrc] = useState(src || fallbackSrc);
  const [isLoading, setIsLoading] = useState(true);

  const handleError = () => {
    setImgSrc(fallbackSrc);
  };

  // Update src when prop changes
  React.useEffect(() => {
    setImgSrc(src || fallbackSrc);
  }, [src, fallbackSrc]);

  return (
    <Image
      src={imgSrc}
      alt={alt}
      width={fill ? undefined : width}
      height={fill ? undefined : height}
      fill={fill}
      className={cn(
        isLoading && 'bg-zinc-200 animate-pulse',
        className,
      )}
      priority={priority}
      sizes={sizes}
      onLoad={() => setIsLoading(false)}
      onError={handleError}
      unoptimized={imgSrc.startsWith('/api/')}
    />
  );
};
