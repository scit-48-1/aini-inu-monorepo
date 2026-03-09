'use client';

import React, { useEffect, useRef, useState } from 'react';
import { MapPin } from 'lucide-react';

declare global {
  interface Window {
    kakao: any;
  }
}

interface KakaoStaticMapProps {
  latitude: number;
  longitude: number;
  width?: number;
  height?: number;
  className?: string;
}

const KAKAO_SDK_URL = '//dapi.kakao.com/v2/maps/sdk.js?autoload=false&appkey=';

function loadKakaoSDK(apiKey: string): Promise<void> {
  return new Promise((resolve, reject) => {
    if (window.kakao?.maps) {
      if (window.kakao.maps.LatLng) {
        resolve();
      } else {
        window.kakao.maps.load(() => resolve());
      }
      return;
    }

    const script = document.createElement('script');
    script.src = `${KAKAO_SDK_URL}${apiKey}`;
    script.onload = () => {
      window.kakao.maps.load(() => resolve());
    };
    script.onerror = () => reject(new Error('Kakao Maps SDK 로드 실패'));
    document.head.appendChild(script);
  });
}

export const KakaoStaticMap: React.FC<KakaoStaticMapProps> = ({
  latitude,
  longitude,
  width = 400,
  height = 180,
  className = '',
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const [hasError, setHasError] = useState(false);
  const apiKey = process.env.NEXT_PUBLIC_KAKAO_MAP_API_KEY;

  useEffect(() => {
    if (!apiKey || !containerRef.current) return;

    let cancelled = false;

    loadKakaoSDK(apiKey)
      .then(() => {
        if (cancelled || !containerRef.current) return;

        const markerPosition = new window.kakao.maps.LatLng(latitude, longitude);

        new window.kakao.maps.StaticMap(containerRef.current, {
          center: new window.kakao.maps.LatLng(latitude, longitude),
          level: 3,
          marker: {
            position: markerPosition,
          },
        });
      })
      .catch(() => {
        if (!cancelled) setHasError(true);
      });

    return () => {
      cancelled = true;
    };
  }, [apiKey, latitude, longitude]);

  if (!apiKey || hasError) {
    return (
      <div
        className={`flex flex-col items-center justify-center bg-zinc-100 text-zinc-400 rounded-xl ${className}`}
        style={{ width: '100%', height }}
      >
        <MapPin size={28} className="mb-1" />
        <span className="text-[10px] font-medium">지도를 불러올 수 없습니다</span>
      </div>
    );
  }

  return (
    <div
      ref={containerRef}
      className={`w-full rounded-xl shadow-sm border border-zinc-200/50 overflow-hidden ${className}`}
      style={{ width: '100%', height }}
    />
  );
};
