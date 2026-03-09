'use client';

import React, { useState, useEffect, useRef, useMemo } from 'react';
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

/**
 * Inner component that handles loading state.
 * Keyed by `resolvedSrc` so state resets when the image source changes.
 */
function OptimizedImageInner({
  resolvedSrc,
  alt,
  width,
  height,
  fill,
  className,
  priority,
  sizes,
  fallbackSrc,
}: {
  resolvedSrc: string;
  alt: string;
  width?: number;
  height?: number;
  fill?: boolean;
  className?: string;
  priority: boolean;
  sizes?: string;
  fallbackSrc: string;
}) {
  const [useFallback, setUseFallback] = useState(false);
  const [hasLoaded, setHasLoaded] = useState(false);
  const [showPlaceholder, setShowPlaceholder] = useState(false);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const imgSrc = useFallback ? fallbackSrc : resolvedSrc;
  const isLocal = useMemo(() => typeof imgSrc === 'string' && imgSrc.startsWith('/'), [imgSrc]);

  const handleError = () => {
    setUseFallback(true);
  };

  const handleLoad = () => {
    setHasLoaded(true);
    setShowPlaceholder(false);
    if (timerRef.current) {
      clearTimeout(timerRef.current);
      timerRef.current = null;
    }
  };

  // For remote images, delay showing placeholder by 150ms
  useEffect(() => {
    if (hasLoaded || isLocal) return;

    timerRef.current = setTimeout(() => {
      setShowPlaceholder(true);
    }, 150);

    return () => {
      if (timerRef.current) {
        clearTimeout(timerRef.current);
        timerRef.current = null;
      }
    };
  }, [hasLoaded, isLocal]);

  return (
    <Image
      src={imgSrc}
      alt={alt}
      width={fill ? undefined : width}
      height={fill ? undefined : height}
      fill={fill}
      className={cn(
        !hasLoaded && showPlaceholder && 'bg-zinc-200 animate-pulse',
        className,
      )}
      priority={priority}
      sizes={sizes}
      onLoad={handleLoad}
      onError={handleError}
      unoptimized={imgSrc.startsWith('/api/')}
    />
  );
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
  const resolvedSrc = src || fallbackSrc;

  return (
    <OptimizedImageInner
      key={resolvedSrc}
      resolvedSrc={resolvedSrc}
      alt={alt}
      width={width}
      height={height}
      fill={fill}
      className={className}
      priority={priority}
      sizes={sizes}
      fallbackSrc={fallbackSrc}
    />
  );
};
