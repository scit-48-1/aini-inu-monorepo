'use client';

import React, { useState, useEffect } from 'react';
import dynamic from 'next/dynamic';
import { useRouter } from 'next/navigation';
import { X, Clock, MapPin, Users, Loader2, Footprints, Pencil, Trash2, Flame, MessageCircle } from 'lucide-react';
import { toast } from 'sonner';
import { cn, formatRemainingTime } from '@/lib/utils';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import type { MapMarker } from '@/types';
import type {
  ThreadMapResponse,
  ThreadHotspotResponse,
  ThreadResponse,
  ThreadSummaryResponse,
} from '@/api/threads';
import type { PetResponse } from '@/api/pets';
import { applyToThread, cancelApplication } from '@/api/threads';

const DynamicMap = dynamic(() => import('@/components/common/DynamicMap'), {
  ssr: false,
  loading: () => (
    <div className="w-full h-full bg-[#f8fafc] flex flex-col items-center justify-center space-y-6">
      <div className="relative">
        <Loader2 className="animate-spin text-amber-500" size={64} />
        <div className="absolute inset-0 flex items-center justify-center">
          <Footprints size={24} className="text-amber-500/50" />
        </div>
      </div>
      <Typography variant="label" className="text-zinc-400 font-black tracking-[0.2em] uppercase text-xs">
        Aini Inu Radar Syncing...
      </Typography>
    </div>
  ),
});

// Seoul district coordinate lookup table for hotspot map markers
const SEOUL_DISTRICT_COORDS: Record<string, [number, number]> = {
  '강남구': [37.5172, 127.0473],
  '강동구': [37.5301, 127.1238],
  '강북구': [37.6396, 127.0255],
  '강서구': [37.5510, 126.8495],
  '관악구': [37.4784, 126.9516],
  '광진구': [37.5385, 127.0823],
  '구로구': [37.4954, 126.8874],
  '금천구': [37.4569, 126.8955],
  '노원구': [37.6542, 127.0568],
  '도봉구': [37.6688, 127.0471],
  '동대문구': [37.5744, 127.0400],
  '동작구': [37.5124, 126.9393],
  '마포구': [37.5664, 126.9018],
  '서대문구': [37.5791, 126.9368],
  '서초구': [37.4837, 127.0324],
  '성동구': [37.5633, 127.0371],
  '성북구': [37.5894, 127.0167],
  '송파구': [37.5145, 127.1050],
  '양천구': [37.5170, 126.8665],
  '영등포구': [37.5264, 126.8963],
  '용산구': [37.5324, 126.9906],
  '은평구': [37.6027, 126.9291],
  '종로구': [37.5735, 126.9790],
  '중구': [37.5641, 126.9979],
  '중랑구': [37.6063, 127.0928],
};

interface HotspotPopup {
  region: string;
  count: number;
}

interface RadarMapSectionProps {
  coordinates: [number, number];
  mapMarkers: ThreadMapResponse[];
  hotspots: ThreadHotspotResponse[];
  selectedThread: ThreadResponse | null;
  myPets: PetResponse[];
  currentUserId: number | undefined;
  isExpired: (startTime: string) => boolean;
  onMarkerClick: (threadId: number) => void;
  onClearSelection: () => void;
  onDeleteThread: (threadId: number) => void;
  onEditThread: (threadId: number) => void;
  onRefreshDetail: () => void;
  radius?: number;
  onMoveEnd?: (lat: number, lng: number) => void;
  myActiveThread?: ThreadSummaryResponse | null;
  myJoinedThreads?: ThreadSummaryResponse[];
}

export const RadarMapSection: React.FC<RadarMapSectionProps> = ({
  coordinates,
  mapMarkers,
  hotspots,
  selectedThread,
  myPets,
  currentUserId,
  isExpired,
  onMarkerClick,
  onClearSelection,
  onDeleteThread,
  onEditThread,
  onRefreshDetail,
  radius,
  onMoveEnd,
  myActiveThread,
  myJoinedThreads,
}) => {
  const router = useRouter();
  const [isConfirmingDelete, setIsConfirmingDelete] = useState(false);
  const [selectedPetIds, setSelectedPetIds] = useState<number[]>([]);
  const [isApplying, setIsApplying] = useState(false);
  const [isCancelling, setIsCancelling] = useState(false);
  const [showPetSelect, setShowPetSelect] = useState(false);
  const [hotspotPopup, setHotspotPopup] = useState<HotspotPopup | null>(null);
  const [optimisticApplied, setOptimisticApplied] = useState<boolean | null>(null);

  useEffect(() => {
    setIsConfirmingDelete(false);
    setSelectedPetIds([]);
    setShowPetSelect(false);
    setHotspotPopup(null);
    setOptimisticApplied(null);
  }, [selectedThread]);

  // Convert ThreadMapResponse[] to MapMarker[] for DynamicMap
  const threadMarkers: MapMarker[] = mapMarkers.map((m) => ({
    id: String(m.threadId),
    lat: m.latitude,
    lng: m.longitude,
    image: m.petImageUrl,
  }));

  // Always include my active thread marker on the map (even if outside search area)
  if (myActiveThread && !threadMarkers.some((m) => m.id === String(myActiveThread.id))) {
    threadMarkers.push({
      id: String(myActiveThread.id),
      lat: myActiveThread.latitude,
      lng: myActiveThread.longitude,
    });
  }

  // Always include my joined thread markers on the map (even if outside search area)
  for (const jt of (myJoinedThreads ?? [])) {
    if (!threadMarkers.some((m) => m.id === String(jt.id))) {
      threadMarkers.push({ id: String(jt.id), lat: jt.latitude, lng: jt.longitude });
    }
  }

  // Convert hotspots to map markers using Seoul district coordinate lookup
  const mappedHotspots = hotspots.filter((h) => SEOUL_DISTRICT_COORDS[h.region]);
  const unmappedHotspots = hotspots.filter((h) => !SEOUL_DISTRICT_COORDS[h.region]);

  const hotspotMarkers: MapMarker[] = mappedHotspots.map((h) => ({
    id: `hotspot-${h.region}`,
    lat: SEOUL_DISTRICT_COORDS[h.region][0],
    lng: SEOUL_DISTRICT_COORDS[h.region][1],
    isEmergency: false,
  }));

  const allMarkers: MapMarker[] = [...threadMarkers, ...hotspotMarkers];

  const handleMapMarkerClick = (marker: MapMarker) => {
    if (marker.id.startsWith('hotspot-')) {
      const region = marker.id.replace('hotspot-', '');
      const hotspot = hotspots.find((h) => h.region === region);
      if (hotspot) {
        setHotspotPopup({ region: hotspot.region, count: hotspot.count });
      }
      return;
    }
    const threadId = Number(marker.id);
    if (!isNaN(threadId)) {
      onMarkerClick(threadId);
    }
  };

  const handleApply = async () => {
    if (!selectedThread) return;
    setIsApplying(true);
    try {
      const result = await applyToThread(selectedThread.id, { petIds: selectedPetIds });
      toast.success('참여 완료!', {
        action: {
          label: '채팅방 가기',
          onClick: () => router.push(`/chat/${result.chatRoomId}`),
        },
      });
      setShowPetSelect(false);
      setSelectedPetIds([]);
      setOptimisticApplied(true);
      onMarkerClick(selectedThread.id);
      onRefreshDetail();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : '';
      if (msg.includes('capacity') || msg.includes('정원')) {
        toast.error('정원이 초과되었습니다');
      } else {
        toast.error('신청에 실패했습니다');
      }
    } finally {
      setIsApplying(false);
    }
  };

  const handleCancelApplication = async () => {
    if (!selectedThread) return;
    setIsCancelling(true);
    try {
      await cancelApplication(selectedThread.id);
      toast.success('신청이 취소되었습니다');
      setOptimisticApplied(false);
      onMarkerClick(selectedThread.id);
      onRefreshDetail();
    } catch {
      toast.error('취소에 실패했습니다');
    } finally {
      setIsCancelling(false);
    }
  };

  const togglePet = (petId: number) => {
    setSelectedPetIds((prev) =>
      prev.includes(petId) ? prev.filter((id) => id !== petId) : [...prev, petId],
    );
  };

  const formatTime = (isoString: string) => {
    try {
      return new Date(isoString).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', hour12: false });
    } catch {
      return isoString;
    }
  };

  const getRemainingMinutes = (startTime: string): string => {
    try {
      const start = new Date(startTime);
      const expiry = new Date(start.getTime() + 60 * 60 * 1000);
      const diff = expiry.getTime() - new Date().getTime();
      return formatRemainingTime(diff);
    } catch {
      return '알 수 없음';
    }
  };

  const isApplied = optimisticApplied ?? selectedThread?.applied ?? false;

  const isOwner = selectedThread ? selectedThread.authorId === currentUserId : false;

  const isFull = selectedThread ? selectedThread.currentParticipants >= selectedThread.maxParticipants : false;

  return (
    <div
      className={cn(
        'flex-[1.8] relative bg-zinc-100 rounded-[48px] border border-card-border overflow-hidden min-h-[500px] flex flex-col shadow-2xl transition-all duration-700',
      )}
    >
      {/* Map */}
      <div className="absolute inset-0 z-0" onClick={(e) => { if (e.target === e.currentTarget) { onClearSelection(); setHotspotPopup(null); } }}>
        <DynamicMap
          center={coordinates}
          zoom={14}
          markers={allMarkers}
          onMarkerClick={handleMapMarkerClick}
          radiusKm={radius}
          onMoveEnd={onMoveEnd}
          selectedMarkerId={selectedThread ? String(selectedThread.id) : null}
        />
      </div>

      {/* Hotspot popup for clicked hotspot marker */}
      {hotspotPopup && (
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 z-[600] bg-white/95 rounded-2xl p-4 shadow-xl border border-amber-100 min-w-[140px]">
          <div className="flex items-center gap-2 mb-1">
            <Flame size={14} className="text-orange-500" />
            <span className="text-xs font-black text-zinc-700">{hotspotPopup.region}</span>
          </div>
          <p className="text-sm font-black text-amber-600">{hotspotPopup.count}건 활동 중</p>
          <button
            onClick={(e) => { e.stopPropagation(); setHotspotPopup(null); }}
            className="absolute top-2 right-2 p-1 text-zinc-300 hover:text-zinc-600"
          >
            <X size={12} />
          </button>
        </div>
      )}

      {/* Unmapped hotspots overlay panel */}
      {unmappedHotspots.length > 0 && (
        <div className="absolute top-4 right-4 z-[500] bg-white/90 backdrop-blur-sm rounded-2xl p-3 shadow-lg border border-zinc-100 max-w-[200px]">
          <p className="text-[10px] font-black text-zinc-400 mb-2 flex items-center gap-1">
            <Flame size={12} className="text-orange-500" /> hotspot
          </p>
          {unmappedHotspots.map((h) => (
            <div key={h.region} className="flex justify-between text-xs py-1">
              <span className="text-zinc-600 truncate">{h.region}</span>
              <span className="text-amber-600 font-black ml-2">{h.count}</span>
            </div>
          ))}
        </div>
      )}

      {/* Thread detail popup */}
      {selectedThread && (
        <div className="absolute bottom-10 left-1/2 -translate-x-1/2 z-[1000] w-full max-w-lg px-4 animate-in slide-in-from-bottom-4 duration-500">
          <Card className="p-8 bg-white shadow-2xl border-2 border-amber-500/10 space-y-6 rounded-[48px] overflow-hidden">
            {/* Header */}
            <div className="flex justify-between items-start">
              <div className="flex-1 pr-4">
                <div className="flex items-center gap-2 mb-2">
                  <Badge variant="amber" className="px-3 text-[10px]">
                    {selectedThread.chatType === 'INDIVIDUAL' ? '1:1 채팅' : '그룹 채팅'}
                  </Badge>
                  {isFull ? (
                    <Badge variant="default" className="bg-blue-100 text-blue-600 border-none px-3 text-[10px]">
                      모집 완료
                    </Badge>
                  ) : isExpired(selectedThread.startTime) ? (
                    <Badge variant="default" className="bg-red-100 text-red-500 border-none px-3 text-[10px]">
                      만료됨
                    </Badge>
                  ) : null}
                </div>
                <Typography variant="h3" className="text-xl text-navy-900 leading-tight mb-1">
                  {selectedThread.title}
                </Typography>
                <div className="flex items-center gap-1 text-xs text-zinc-400">
                  <MapPin size={11} className="text-amber-500" />
                  <span>{selectedThread.placeName}</span>
                  {selectedThread.address && (
                    <span className="text-zinc-300">· {selectedThread.address}</span>
                  )}
                </div>
              </div>
              <button
                onClick={() => { onClearSelection(); setIsConfirmingDelete(false); }}
                className="p-3 bg-zinc-50 rounded-2xl text-zinc-300 hover:text-navy-900 shrink-0"
              >
                <X size={24} />
              </button>
            </div>

            {/* Time & Participants info */}
            <div className="grid grid-cols-2 gap-4 py-5 border-y border-zinc-50">
              <div className="flex items-center gap-3 text-sm font-black text-zinc-600">
                <Clock size={16} className="text-amber-500" />
                {selectedThread.walkDate && (
                  <span className="text-zinc-400 font-bold mr-1">{selectedThread.walkDate}</span>
                )}
                {formatTime(selectedThread.startTime)} - {formatTime(selectedThread.endTime)}
              </div>
              <div className="flex items-center gap-3 text-sm font-black text-zinc-600">
                <Users size={16} className="text-amber-500" />
                {selectedThread.currentParticipants} / {selectedThread.maxParticipants}명
              </div>
              <div className="col-span-2 text-xs font-bold text-amber-600">
                {getRemainingMinutes(selectedThread.startTime)}
              </div>
            </div>

            {/* Description */}
            {selectedThread.description && (
              <div className="bg-zinc-50/50 p-6 rounded-[32px] border border-zinc-100/50 italic text-zinc-600 text-sm">
                &quot;{selectedThread.description}&quot;
              </div>
            )}

            {/* Action Buttons */}
            <div className="flex gap-3">
              {isOwner ? (
                <div className="flex-1 space-y-3">
                  <div className="h-12 flex items-center justify-center gap-3 bg-amber-50 rounded-[20px] border border-amber-100">
                    <Users size={16} className="text-amber-500" />
                    <Typography variant="body" className="text-sm font-black text-amber-700">
                      내가 만든 스레드 · <span className="text-amber-900">{selectedThread.currentParticipants}명</span> 참여 중
                    </Typography>
                  </div>
                  {isConfirmingDelete ? (
                    <div className="space-y-3 animate-in fade-in duration-200">
                      <div className="bg-red-50 border border-red-200 rounded-[20px] px-4 py-3 flex items-start gap-2">
                        <span className="text-red-500 text-lg leading-none mt-0.5">&#9888;</span>
                        <div>
                          <p className="text-sm font-black text-red-700">정말 삭제하시겠습니까?</p>
                          <p className="text-xs text-red-400 mt-0.5">삭제하면 되돌릴 수 없습니다.</p>
                        </div>
                      </div>
                      <div className="flex gap-2">
                        <Button
                          variant="outline"
                          fullWidth
                          size="lg"
                          className="h-12 rounded-[20px] border-zinc-200 text-zinc-500"
                          onClick={() => setIsConfirmingDelete(false)}
                        >
                          취소
                        </Button>
                        <Button
                          variant="outline"
                          fullWidth
                          size="lg"
                          className="h-12 rounded-[20px] bg-red-500 border-red-500 text-white hover:bg-red-600"
                          onClick={() => { onDeleteThread(selectedThread.id); setIsConfirmingDelete(false); }}
                        >
                          <Trash2 size={16} className="mr-2" /> 삭제 확인
                        </Button>
                      </div>
                    </div>
                  ) : (
                    <div className="flex gap-2">
                      <Button
                        variant="outline"
                        fullWidth
                        size="lg"
                        className="h-12 rounded-[20px] border-zinc-200 text-zinc-600 hover:border-amber-500 hover:text-amber-600"
                        onClick={() => onEditThread(selectedThread.id)}
                      >
                        <Pencil size={16} className="mr-2" /> 수정하기
                      </Button>
                      <Button
                        variant="outline"
                        fullWidth
                        size="lg"
                        className="h-12 rounded-[20px] border-zinc-200 text-red-400 hover:border-red-400 hover:text-red-500"
                        onClick={() => setIsConfirmingDelete(true)}
                      >
                        <Trash2 size={16} className="mr-2" /> 삭제하기
                      </Button>
                    </div>
                  )}
                </div>
              ) : isApplied ? (
                <Button
                  variant="outline"
                  fullWidth
                  size="lg"
                  className="h-16 text-lg rounded-[24px] border-zinc-200 text-zinc-500"
                  disabled={isCancelling}
                  onClick={handleCancelApplication}
                >
                  <MessageCircle className="mr-2" size={20} />
                  {isCancelling ? '취소 중...' : '신청 취소'}
                </Button>
              ) : showPetSelect ? (
                <div className="flex-1 space-y-4">
                  <Typography variant="body" className="text-sm font-black text-zinc-700">
                    함께할 반려견을 선택하세요
                  </Typography>
                  <div className="grid grid-cols-2 gap-2">
                    {myPets.map((pet) => (
                      <button
                        key={pet.id}
                        onClick={() => togglePet(pet.id)}
                        className={cn(
                          'p-3 rounded-2xl border-2 text-left transition-all',
                          selectedPetIds.includes(pet.id)
                            ? 'border-amber-500 bg-amber-50'
                            : 'border-zinc-100 bg-white',
                        )}
                      >
                        <div className="flex items-center gap-2">
                          {pet.photoUrl && (
                            <img
                              src={pet.photoUrl}
                              alt={pet.name}
                              className="w-8 h-8 rounded-xl object-cover"
                            />
                          )}
                          <span className="text-xs font-black text-zinc-700">{pet.name}</span>
                        </div>
                      </button>
                    ))}
                  </div>
                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      fullWidth
                      size="lg"
                      className="h-12 rounded-[20px] border-zinc-200 text-zinc-500"
                      onClick={() => { setShowPetSelect(false); setSelectedPetIds([]); }}
                    >
                      취소
                    </Button>
                    <Button
                      variant="primary"
                      fullWidth
                      size="lg"
                      className="h-12 rounded-[20px] bg-navy-900"
                      disabled={selectedPetIds.length === 0 || isApplying}
                      onClick={handleApply}
                    >
                      {isApplying ? '신청 중...' : '신청 확인'}
                    </Button>
                  </div>
                </div>
              ) : (
                <Button
                  variant="primary"
                  fullWidth
                  size="lg"
                  className={cn("h-16 text-lg rounded-[24px] shadow-2xl bg-navy-900", isFull && "opacity-50 cursor-not-allowed")}
                  disabled={isFull}
                  onClick={() => !isFull && setShowPetSelect(true)}
                >
                  {isFull ? '정원이 찼습니다' : '산책 신청하기'}
                </Button>
              )}
            </div>
          </Card>
        </div>
      )}
    </div>
  );
};
