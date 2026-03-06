'use client';

import React, { useState, useEffect } from 'react';
import { X, Siren } from 'lucide-react';
import { AroundMeHeader } from '@/components/around-me/AroundMeHeader';
import { RadarMapSection } from '@/components/around-me/RadarMapSection';
import { RadarSidebar } from '@/components/around-me/RadarSidebar';
import { RecruitForm } from '@/components/around-me/RecruitForm';
import { EmergencyReportForm } from '@/components/around-me/EmergencyReportForm';
import { AICandidateList } from '@/components/around-me/AICandidateList';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import DaumPostcode from 'react-daum-postcode';
import { useRadarLogic } from '@/hooks/useRadarLogic';
import { useConfigStore } from '@/store/useConfigStore';
import { useUserStore } from '@/store/useUserStore';

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
  } = useRadarLogic();

  useEffect(() => {
    const timer = setTimeout(() => setMounted(true), 0);
    return () => clearTimeout(timer);
  }, []);

  const currentUserId = profile ? Number(profile.id) : undefined;

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
        onRefresh={handleRefresh}
        isRefreshing={isRefreshing}
      />

      <div className="flex-1 flex flex-col lg:flex-row px-4 md:px-8 py-6 gap-8 overflow-hidden">
        {(activeTab === 'FIND' || activeTab === 'EMERGENCY') && (
          <RadarMapSection
            coordinates={coordinates}
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
          />
        )}

        {activeTab === 'FIND' && (
          <RadarSidebar
            isLoading={isLoading}
            threads={threadList}
            hasNext={threadListHasNext}
            coordinates={coordinates}
            currentTime={currentTime}
            isExpired={isExpired}
            currentUserId={currentUserId}
            onCardClick={selectThread}
            onLoadMore={loadMore}
            onDeleteThread={handleDeleteThread}
            onEditThread={startEdit}
          />
        )}

        {activeTab === 'RECRUIT' && (
          <div className="flex-1 overflow-y-auto no-scrollbar">
            <RecruitForm
              myPets={myPets}
              editingThreadId={editingThreadId}
              coordinates={coordinates}
              onSuccess={() => {
                handleRefresh();
                setActiveTab('FIND');
              }}
            />
          </div>
        )}

        {activeTab === 'EMERGENCY' && (
          <div className="flex-1 relative">
            <div className="absolute inset-0 z-10 bg-white/80 backdrop-blur-sm flex items-center justify-center rounded-3xl">
              <div className="text-center space-y-2">
                <Siren size={40} className="mx-auto text-zinc-300" />
                <p className="text-zinc-400 font-bold text-sm">준비 중</p>
                <p className="text-zinc-300 text-xs">긴급 제보 기능은 곧 제공됩니다</p>
              </div>
            </div>
            {/* Keep existing emergency components underneath for Phase 10 */}
            <div className="flex-1 overflow-y-auto no-scrollbar opacity-0 pointer-events-none">
              <EmergencyReportForm
                isSubmitting={false}
                isSuccess={false}
                onSubmit={() => {}}
                optimizeImage={(b64) => Promise.resolve(b64)}
              />
            </div>
          </div>
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
                onComplete={(data) => { setLocation(data.address); setIsLocationModalOpen(false); }}
                style={{ height: '100%' }}
              />
            </div>
          </Card>
        </div>
      )}
    </div>
  );
}
