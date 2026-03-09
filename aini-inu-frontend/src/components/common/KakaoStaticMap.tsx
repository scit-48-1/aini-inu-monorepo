'use client';

import React, { useState } from 'react';
import { MapPin } from 'lucide-react';

interface KakaoStaticMapProps {
  latitude: number;
  longitude: number;
  width?: number;
  height?: number;
  className?: string;
}

export const KakaoStaticMap: React.FC<KakaoStaticMapProps> = ({
  latitude,
  longitude,
  width = 400,
  height = 180,
  className = '',
}) => {
  const [hasError, setHasError] = useState(false);
  const apiKey = process.env.NEXT_PUBLIC_KAKAO_MAP_API_KEY;

  if (!apiKey || hasError) {
    return (
      <div className={`flex flex-col items-center justify-center bg-zinc-100 text-zinc-400 rounded-xl ${className}`} style={{ width: '100%', height }}>
        <MapPin size={28} className="mb-1" />
        <span className="text-[10px] font-medium">지도를 불러올 수 없습니다</span>
      </div>
    );
  }

  const src = `https://dapi.kakao.com/v2/maps/staticmap?appkey=${apiKey}&center=${longitude},${latitude}&level=3&w=${width}&h=${height}&marker=pos%20${longitude}%20${latitude}`;

  return (
    <img
      src={src}
      alt="산책 위치 지도"
      className={`w-full rounded-xl shadow-sm border border-zinc-200/50 ${className}`}
      style={{ height }}
      onError={() => setHasError(true)}
    />
  );
};
