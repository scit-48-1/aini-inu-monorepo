'use client';

import React, { useEffect, useState } from 'react';
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
}

function MapController({ center, zoom, onVisualCenterChange, onMoveEnd }: { center: [number, number]; zoom: number; onVisualCenterChange?: (c: [number, number]) => void; onMoveEnd?: (lat: number, lng: number) => void }) {
  const map = useMap();
  useEffect(() => {
    map.setView(center, zoom, { animate: true });

    const timer = setTimeout(() => {
      map.invalidateSize();
    }, 800);

    const resizeObserver = new ResizeObserver(() => {
      map.invalidateSize();
    });

    const container = map.getContainer();
    resizeObserver.observe(container);

    return () => {
      clearTimeout(timer);
      resizeObserver.unobserve(container);
    };
  }, [center, zoom, map]);

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

export default function DynamicMap({ center, zoom, markers, onMarkerClick, hideCircle, interactive = true, radiusKm, onMoveEnd }: DynamicMapProps) {
  const [visualCenter, setVisualCenter] = useState<[number, number]>(center);

  const createCustomIcon = (imageUrl: string, isEmergency: boolean) => {
    return L.divIcon({
      className: 'custom-dog-marker',
      html: `
        <div class="relative group">
          <div class="w-14 h-14 rounded-[22px] bg-white p-1 shadow-xl border-2 ${isEmergency ? 'border-red-500 animate-pulse' : 'border-amber-500'} transition-transform hover:scale-110 overflow-hidden">
            <img src="${imageUrl}" class="w-full h-full object-cover rounded-[18px]" />
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
  };

  return (
    <div className="w-full h-full relative shadow-inner overflow-hidden rounded-[48px]">
      <style jsx global>{`
        .leaflet-container { background: #f8fafc !important; cursor: default !important; }
        .custom-dog-marker { background: transparent !important; border: none !important; }
        .leaflet-overlay-pane { z-index: 600 !important; }
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
        <MapController center={center} zoom={zoom} onVisualCenterChange={setVisualCenter} onMoveEnd={onMoveEnd} />
        
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
            icon={createCustomIcon(m.image || m.thumbnail || '', !!m.isEmergency)}
            eventHandlers={{ click: () => onMarkerClick(m) }}
          />
        ))}
      </MapContainer>
    </div>
  );
}