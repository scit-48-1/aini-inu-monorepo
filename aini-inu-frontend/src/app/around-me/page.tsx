'use client';

import React, { useState, useEffect, useRef, useCallback } from 'react';
import { X, Users } from 'lucide-react';
import { AroundMeHeader } from '@/components/around-me/AroundMeHeader';
import { RadarMapSection } from '@/components/around-me/RadarMapSection';
import { RadarSidebar } from '@/components/around-me/RadarSidebar';
import { RecruitForm } from '@/components/around-me/RecruitForm';
import { EmergencySubTabs } from '@/components/around-me/EmergencySubTabs';
import { WalkingStatusSection } from '@/components/around-me/WalkingStatusSection';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import DaumPostcode from 'react-daum-postcode';
import { useRadarLogic } from '@/hooks/useRadarLogic';
import { useSearchParams } from 'next/navigation';
import { useConfigStore } from '@/store/useConfigStore';
import { useUserStore } from '@/store/useUserStore';

const DISTRICT_COORDS: Record<string, [number, number]> = {
  '강남구': [37.5172, 127.0473], '강동구': [37.5301, 127.1238], '강북구': [37.6396, 127.0255],
  '강서구': [37.5510, 126.8495], '관악구': [37.4784, 126.9516], '광진구': [37.5385, 127.0823],
  '구로구': [37.4954, 126.8874], '금천구': [37.4569, 126.8955], '노원구': [37.6542, 127.0568],
  '도봉구': [37.6688, 127.0471], '동대문구': [37.5744, 127.0400], '동작구': [37.5124, 126.9393],
  '마포구': [37.5664, 126.9018], '서대문구': [37.5791, 126.9368], '서초구': [37.4837, 127.0324],
  '성동구': [37.5633, 127.0371], '성북구': [37.5894, 127.0167], '송파구': [37.5145, 127.1050],
  '양천구': [37.5170, 126.8665], '영등포구': [37.5264, 126.8963], '용산구': [37.5324, 126.9906],
  '은평구': [37.6027, 126.9291], '종로구': [37.5735, 126.9790], '중구': [37.5641, 126.9979],
  '중랑구': [37.6063, 127.0928],
};

export default function AroundMePage() {
  const [mounted, setMounted] = useState(false);
  const [isLocationModalOpen, setIsLocationModalOpen] = useState(false);

  const { currentLocation, setLocation } = useConfigStore();
  const profile = useUserStore((s) => s.profile);

  const {
    activeTab, setActiveTab,
    coordinates, gpsLoading,
    threadList, threadListHasNext, loadMore,
    mapMarkers, hotspots,
    selectedThread, selectThread, clearSelection,
    myPets, isLoading, isRefreshing,
    currentTime, isExpired,
    editingThreadId, startEdit,
    handleDeleteThread, handleRefresh,
    myActiveThread,
    myJoinedThreads,
    dateFrom, dateTo, setDateFrom, setDateTo,
    radius, setRadius,
    searchCoordinates, setSearchCoordinates,
  } = useRadarLogic();

  const effectiveCoordinates = searchCoordinates ?? coordinates;

  useEffect(() => {
    const timer = setTimeout(() => setMounted(true), 0);
    return () => clearTimeout(timer);
  }, []);

  const currentUserId = profile ? Number(profile.id) : undefined;

  // Track map visual center (ref to avoid re-renders)
  const mapCenterRef = useRef<[number, number]>(effectiveCoordinates);
  const handleMapMoveEnd = useCallback((lat: number, lng: number) => {
    mapCenterRef.current = [lat, lng];
  }, []);

  // Auto-select thread from URL query param (deep link from dashboard)
  const searchParams = useSearchParams();
  const threadIdHandled = useRef(false);

  useEffect(() => {
    const threadIdParam = searchParams.get('threadId');
    if (!threadIdParam || threadIdHandled.current) return;
    threadIdHandled.current = true;
    // selectThread internally calls getThread and sets selectedThread
    selectThread(Number(threadIdParam));
  }, [searchParams, selectThread]);

  // Refresh using map's current visual center
  const handleRefreshFromMap = useCallback(() => {
    const coords: [number, number] = [mapCenterRef.current[0], mapCenterRef.current[1]];
    setSearchCoordinates(coords);
    setLocation('현재 위치');
    handleRefresh(coords);
  }, [setSearchCoordinates, setLocation, handleRefresh]);

  if (!mounted || gpsLoading || isLoading) {
    return <div className="flex items-center justify-center h-full"><p className="text-zinc-400">Loading...</p></div>;
  }

  return (
    <div className="h-full flex flex-col animate-in slide-in-from-right duration-500 bg-background text-black relative">
      <AroundMeHeader
        currentLocation={currentLocation}
        onLocationClick={() => setIsLocationModalOpen(true)}
        activeTab={activeTab}
        onTabChange={(tab) => { setActiveTab(tab); clearSelection(); }}
        onRefresh={handleRefreshFromMap}
        isRefreshing={isRefreshing}
        dateFrom={dateFrom}
        dateTo={dateTo}
        onDateFromChange={setDateFrom}
        onDateToChange={setDateTo}
        radius={radius}
        onRadiusChange={setRadius}
      />

      <div className="flex-1 flex flex-col lg:flex-row px-4 md:px-8 py-6 gap-8 overflow-hidden">
        {activeTab === 'FIND' && (
          <RadarMapSection
            coordinates={effectiveCoordinates}
            mapMarkers={mapMarkers}
            hotspots={hotspots}
            selectedThread={selectedThread}
            myPets={myPets}
            currentUserId={currentUserId}
            isExpired={isExpired}
            onMarkerClick={selectThread}
            onClearSelection={clearSelection}
            onDeleteThread={handleDeleteThread}
            onEditThread={startEdit}
            onRefreshDetail={handleRefresh}
            radius={radius}
            onMoveEnd={handleMapMoveEnd}
            myActiveThread={myActiveThread}
            myJoinedThreads={myJoinedThreads}
          />
        )}

        {activeTab === 'FIND' && (
          <RadarSidebar
            isLoading={isLoading}
            threads={threadList}
            hasNext={threadListHasNext}
            coordinates={effectiveCoordinates}
            currentTime={currentTime}
            isExpired={isExpired}
            currentUserId={currentUserId}
            onCardClick={selectThread}
            onLoadMore={loadMore}
            onDeleteThread={handleDeleteThread}
            onEditThread={startEdit}
            myActiveThread={myActiveThread}
            myJoinedThreads={myJoinedThreads}
          />
        )}

        {activeTab === 'RECRUIT' && (
          (myActiveThread && !editingThreadId) ? (
            <div className="flex-1 flex flex-col items-center justify-center gap-4 p-8">
              <div className="w-full max-w-md bg-amber-50 border border-amber-200 rounded-[32px] p-6 space-y-3">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 bg-amber-100 rounded-full flex items-center justify-center">
                    <Users size={20} className="text-amber-600" />
                  </div>
                  <div>
                    <p className="text-sm font-black text-amber-800">이미 활성 모집글이 있습니다</p>
                    <p className="text-xs text-amber-600 mt-0.5">기존 모집글을 삭제하거나 만료 후 새 글을 작성할 수 있습니다</p>
                  </div>
                </div>
                <div className="bg-white rounded-[20px] p-4 border border-amber-100">
                  <p className="text-xs font-black text-zinc-700 truncate">{myActiveThread.title}</p>
                  <p className="text-xs text-zinc-400 mt-1">{myActiveThread.placeName}</p>
                </div>
                <button
                  onClick={() => setActiveTab('FIND')}
                  className="w-full text-xs font-black text-amber-600 hover:text-amber-800 py-2"
                >
                  FIND 탭에서 내 모집글 확인하기 →
                </button>
              </div>
            </div>
          ) : (
            <div className="flex-1 overflow-y-auto no-scrollbar">
              <RecruitForm
                myPets={myPets}
                editingThreadId={editingThreadId}
                coordinates={effectiveCoordinates}
                onSuccess={() => {
                  handleRefresh();
                  setActiveTab('FIND');
                }}
              />
            </div>
          )
        )}

        {activeTab === 'EMERGENCY' && (
          <EmergencySubTabs />
        )}

        {activeTab === 'WALKING' && (
          <WalkingStatusSection />
        )}
      </div>

      {isLocationModalOpen && (
        <div
          className="fixed inset-0 z-[2000] bg-navy-900/40 backdrop-blur-md flex items-center justify-center p-6 animate-in fade-in duration-300"
          onClick={e => e.target === e.currentTarget && setIsLocationModalOpen(false)}
        >
          <Card className="w-full max-w-2xl p-0 bg-white shadow-2xl animate-in zoom-in-95 duration-500 border-none overflow-hidden flex flex-col h-[600px] rounded-[48px]">
            <div className="flex items-center justify-between p-8 shrink-0 border-b border-zinc-50">
              <Typography variant="h3" className="text-navy-900 font-serif">동네 설정하기</Typography>
              <button onClick={() => setIsLocationModalOpen(false)} className="p-2 text-zinc-300 hover:text-navy-900 transition-colors">
                <X size={32} />
              </button>
            </div>
            <div className="flex-1 overflow-y-auto">
              <DaumPostcode
                onComplete={(data) => {
                  setLocation(data.address);
                  setIsLocationModalOpen(false);
                  // Try to resolve district coordinates for search
                  const sigungu = (data as unknown as Record<string, string>).sigungu || '';
                  const district = Object.keys(DISTRICT_COORDS).find((d) => sigungu.includes(d));
                  if (district) {
                    setSearchCoordinates(DISTRICT_COORDS[district]);
                  }
                }}
                style={{ height: '100%' }}
              />
            </div>
          </Card>
        </div>
      )}
    </div>
  );
}
