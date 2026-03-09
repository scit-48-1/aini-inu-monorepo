'use client';

import React, { useEffect, useMemo, useRef, useState } from 'react';
import { MapContainer, TileLayer, Marker, Circle, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { MapMarker } from '@/types';

interface DynamicMapProps {
  center: [number, number];
  zoom: number;
  markers: MapMarker[];
  onMarkerClick: (marker: MapMarker) => void;
  hideCircle?: boolean; // 레이다 원 숨김 옵션 추가
  interactive?: boolean; // 지도 상호작용 여부 추가
  radiusKm?: number; // 반경 (km), 기본 2.5
  onMoveEnd?: (lat: number, lng: number) => void; // 드래그 후 좌표 콜백
  selectedMarkerId?: string | null; // 선택된 마커 강조 표시
  flyTo?: [number, number] | null; // 특정 위치로 부드럽게 이동
  flyToOffsetX?: number; // flyTo 시 마커를 왼쪽으로 치우치게 하는 픽셀 오프셋
}

function MapController({ center, zoom, onVisualCenterChange, onMoveEnd, flyToActive }: { center: [number, number]; zoom: number; onVisualCenterChange?: (c: [number, number]) => void; onMoveEnd?: (lat: number, lng: number) => void; flyToActive?: boolean }) {
  const map = useMap();
  useEffect(() => {
    // flyTo가 활성화되어 있으면 setView를 건너뛴다 (flyTo가 항상 우선)
    if (!flyToActive) {
      map.setView(center, zoom, { animate: true });
    }

    const timer = setTimeout(() => {
      map.invalidateSize();
    }, 800);

    let resizeTimer: ReturnType<typeof setTimeout>;
    const resizeObserver = new ResizeObserver(() => {
      clearTimeout(resizeTimer);
      resizeTimer = setTimeout(() => map.invalidateSize(), 300);
    });

    const container = map.getContainer();
    resizeObserver.observe(container);

    return () => {
      clearTimeout(timer);
      clearTimeout(resizeTimer);
      resizeObserver.unobserve(container);
    };
  }, [center, zoom, map, flyToActive]);

  // Track visual center on move (drag/zoom) for Circle
  useEffect(() => {
    if (!onVisualCenterChange) return;
    const handler = () => {
      const c = map.getCenter();
      onVisualCenterChange([c.lat, c.lng]);
    };
    map.on('move', handler);
    return () => { map.off('move', handler); };
  }, [map, onVisualCenterChange]);

  // Report final center on moveend (drag/zoom complete)
  useEffect(() => {
    if (!onMoveEnd) return;
    const handler = () => {
      const c = map.getCenter();
      onMoveEnd(c.lat, c.lng);
    };
    map.on('moveend', handler);
    return () => { map.off('moveend', handler); };
  }, [map, onMoveEnd]);

  return null;
}

function FlyToController({ flyTo, offsetX }: { flyTo?: [number, number] | null; offsetX?: number }) {
  const map = useMap();
  const prevRef = useRef<[number, number] | null>(null);
  useEffect(() => {
    if (!flyTo) { prevRef.current = null; return; }
    // 좌표 값이 실제로 변경된 경우에만 flyTo 실행
    if (prevRef.current && prevRef.current[0] === flyTo[0] && prevRef.current[1] === flyTo[1]) return;
    prevRef.current = flyTo;
    // 진행 중인 애니메이션을 중단하고 스레드 위치로 이동
    map.stop();
    if (offsetX) {
      const zoom = map.getZoom();
      const targetPoint = map.project(flyTo, zoom);
      const newCenter = map.unproject(
        L.point(targetPoint.x + offsetX, targetPoint.y),
        zoom,
      );
      map.flyTo(newCenter, zoom, { duration: 0.8 });
    } else {
      map.flyTo(flyTo, map.getZoom(), { duration: 0.8 });
    }
  }, [flyTo, offsetX, map]);
  return null;
}

export default function DynamicMap({ center, zoom, markers, onMarkerClick, hideCircle, interactive = true, radiusKm, onMoveEnd, selectedMarkerId, flyTo, flyToOffsetX }: DynamicMapProps) {
  const visualCenterRef = useRef<[number, number]>(center);
  const [visualCenter, setVisualCenter] = useState<[number, number]>(center);

  // Throttle visual center updates to avoid excessive re-renders during drag
  const handleVisualCenterChange = useMemo(() => {
    let rafId: number | null = null;
    return (c: [number, number]) => {
      visualCenterRef.current = c;
      if (rafId === null) {
        rafId = requestAnimationFrame(() => {
          setVisualCenter(visualCenterRef.current);
          rafId = null;
        });
      }
    };
  }, []);

  // Preload marker images into browser cache when markers change
  useEffect(() => {
    const urls = new Set<string>();
    for (const m of markers) {
      const url = m.image || m.thumbnail;
      if (url) urls.add(url);
    }
    urls.forEach(url => {
      const img = new Image();
      img.src = url;
    });
  }, [markers]);

  // Base icons — only recreated when markers array changes (NOT on selection change)
  const baseIcons = useMemo(() => {
    const iconMap = new Map<string, L.DivIcon>();
    for (const m of markers) {
      if (m.lat == null || m.lng == null) continue;
      const imageUrl = m.image || m.thumbnail || '/AINIINU_ROGO_B.png';
      const isEmergency = !!m.isEmergency;
      const icon = L.divIcon({
        className: 'custom-dog-marker',
        html: `
          <div class="relative group" style="transition: transform 0.3s ease, filter 0.3s ease;">
            <div class="w-14 h-14 rounded-[22px] bg-white p-1 shadow-xl border-2 ${isEmergency ? 'border-red-500 animate-pulse' : 'border-amber-500'} transition-transform hover:scale-110 overflow-hidden">
              <img src="${imageUrl}" decoding="async" onerror="this.onerror=null;this.src='/AINIINU_ROGO_B.png';" class="w-full h-full object-cover rounded-[18px]" />
            </div>
            ${isEmergency ? `
              <div class="absolute -top-2 -right-2 bg-red-500 text-white p-1.5 rounded-full shadow-lg">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
              </div>
            ` : ''}
            <div class="absolute -bottom-1 -right-1 w-4 h-4 bg-amber-500 rounded-full border-2 border-white"></div>
          </div>
        `,
        iconSize: [56, 56],
        iconAnchor: [28, 56],
      });
      iconMap.set(m.id, icon);
    }
    return iconMap;
  }, [markers]);

  // Selected icon — only 1 icon recreated when selection changes
  const selectedIcon = useMemo(() => {
    if (!selectedMarkerId) return null;
    const m = markers.find(m => m.id === selectedMarkerId);
    if (!m || m.lat == null || m.lng == null) return null;
    const imageUrl = m.image || m.thumbnail || '/AINIINU_ROGO_B.png';
    return L.divIcon({
      className: 'custom-dog-marker',
      html: `
        <div class="relative group" style="transform: scale(1.3); z-index: 9999; filter: drop-shadow(0 0 12px rgba(245, 158, 11, 0.7)); transition: transform 0.3s ease, filter 0.3s ease;">
          <div class="w-14 h-14 rounded-[22px] bg-white p-1 shadow-xl border-amber-400 transition-transform hover:scale-110 overflow-hidden" style="border-width: 3px; border-style: solid; box-shadow: 0 0 20px rgba(245, 158, 11, 0.5), 0 4px 20px rgba(0,0,0,0.15);">
            <img src="${imageUrl}" decoding="async" onerror="this.onerror=null;this.src='/AINIINU_ROGO_B.png';" class="w-full h-full object-cover rounded-[18px]" />
          </div>
          <div class="absolute -bottom-2 left-1/2 -translate-x-1/2 w-6 h-6 bg-amber-500 rounded-full border-2 border-white shadow-lg flex items-center justify-center" style="animation: selectedPulse 2s ease-in-out infinite;">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="white" stroke="none"><path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/></svg>
          </div>
        </div>
      `,
      iconSize: [56, 56],
      iconAnchor: [28, 56],
    });
  }, [selectedMarkerId, markers]);

  return (
    <div className="w-full h-full relative shadow-inner overflow-hidden rounded-[48px]">
      <style jsx global>{`
        .leaflet-container { background: #f8fafc !important; cursor: default !important; }
        .custom-dog-marker { background: transparent !important; border: none !important; }
        .leaflet-overlay-pane { z-index: 600 !important; }
        @keyframes selectedPulse {
          0%, 100% { transform: translateX(-50%) scale(1); opacity: 1; }
          50% { transform: translateX(-50%) scale(1.3); opacity: 0.7; }
        }
      `}</style>
      
      <MapContainer 
        center={center} 
        zoom={zoom} 
        minZoom={12}
        style={{ height: '100%', width: '100%', position: 'absolute', top: 0, left: 0 }}
        zoomControl={true}
        attributionControl={false}
        dragging={interactive}
        scrollWheelZoom={interactive}
        doubleClickZoom={interactive}
        touchZoom={interactive}
        boxZoom={interactive}
        keyboard={interactive}
        preferCanvas={true}
      >
        <MapController center={center} zoom={zoom} onVisualCenterChange={handleVisualCenterChange} onMoveEnd={onMoveEnd} flyToActive={!!flyTo} />
        <FlyToController flyTo={flyTo} offsetX={flyToOffsetX} />
        
        <TileLayer
          url="https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png"
        />
        
        {!hideCircle && (
          <Circle
            center={visualCenter}
            radius={(radiusKm ?? 2.5) * 1000}
            pathOptions={{ 
              fillColor: '#FF9F0A', 
              fillOpacity: 0.15, 
              color: '#FF9F0A', 
              weight: 8, 
              dashArray: '15, 30' 
            }}
          />
        )}

        {markers.filter((m): m is MapMarker & { lat: number; lng: number } => m.lat != null && m.lng != null).map((m) => (
          <Marker
            key={m.id}
            position={[m.lat, m.lng]}
            icon={m.id === selectedMarkerId && selectedIcon ? selectedIcon : baseIcons.get(m.id)!}
            zIndexOffset={m.id === selectedMarkerId ? 1000 : 0}
            eventHandlers={{ click: () => onMarkerClick(m) }}
          />
        ))}
      </MapContainer>
    </div>
  );
}