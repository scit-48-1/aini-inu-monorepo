import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

/**
 * 하버사인(Haversine) 공식을 이용한 두 지점 간의 거리 계산 (km)
 */
export function calculateDistance(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6371; 
  const dLat = (lat2 - lat1) * (Math.PI / 180);
  const dLon = (lon2 - lon1) * (Math.PI / 180);
  const a = 
    Math.sin(dLat / 2) * Math.sin(dLat / 2) + 
    Math.cos(lat1 * (Math.PI / 180)) * Math.cos(lat2 * (Math.PI / 180)) * 
    Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

/**
 * 시작 시간으로부터 현재 시간과의 차이를 한글 문자열로 반환
 */
export function getRemainingTimeStr(startTime?: string, currentTime: Date = new Date()): string {
  if (!startTime) return '시간 협의';
  const [hours, minutes] = startTime.split(':').map(Number);
  const target = new Date(currentTime);
  target.setHours(hours, minutes, 0, 0);
  
  const diff = target.getTime() - currentTime.getTime();
  
  if (diff > 0) {
    const h = Math.floor(diff / (1000 * 60 * 60));
    const m = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
    return h > 0 ? `${h}시간 ${m}분 후 시작` : `${m}분 후 시작`;
  }
  
  const passedMins = Math.abs(Math.floor(diff / (1000 * 60)));
  if (passedMins < 5) return '방금 시작';
  if (passedMins < 60) return `${passedMins}분 지남`;
  return '만료됨';
}

