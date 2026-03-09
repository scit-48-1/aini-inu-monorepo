'use client';

import React, { useState, useCallback } from 'react';
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

export const OptimizedImage: React.FC<OptimizedImageProps> = React.memo(({
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
  const resolvedSrc = src || fallbackSrc;
  const [imgSrc, setImgSrc] = useState(resolvedSrc);
  const [loaded, setLoaded] = useState(false);

  // If the parent passes a new src, update (but keep loaded state if same src)
  if (resolvedSrc !== imgSrc && resolvedSrc !== fallbackSrc) {
    setImgSrc(resolvedSrc);
    setLoaded(false);
  }

  const handleLoad = useCallback(() => {
    setLoaded(true);
  }, []);

  const handleError = useCallback(() => {
    setImgSrc(fallbackSrc);
  }, [fallbackSrc]);

  return (
    <Image
      src={imgSrc}
      alt={alt}
      width={fill ? undefined : width}
      height={fill ? undefined : height}
      fill={fill}
      className={cn(
        'transition-opacity duration-300',
        loaded ? 'opacity-100' : 'opacity-0',
        className,
      )}
      priority={priority}
      sizes={sizes}
      onLoad={handleLoad}
      onError={handleError}
      unoptimized={imgSrc.startsWith('/api/')}
    />
  );
});

OptimizedImage.displayName = 'OptimizedImage';
