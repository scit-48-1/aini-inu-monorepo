'use client';

import React, { useState, useEffect, useCallback } from 'react';
import { X } from 'lucide-react';
import { AroundMeHeader } from '@/components/around-me/AroundMeHeader';
import { RadarMapSection } from '@/components/around-me/RadarMapSection';
import { RadarSidebar } from '@/components/around-me/RadarSidebar';
import { RecruitForm } from '@/components/around-me/RecruitForm';
import { EmergencyReportForm } from '@/components/around-me/EmergencyReportForm';
import { AICandidateList, AICandidate } from '@/components/around-me/AICandidateList';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import DaumPostcode from 'react-daum-postcode';
import { useRadarLogic } from '@/hooks/useRadarLogic';
import { chatService } from '@/services/api/chatService';
import { memberService } from '@/services/api/memberService';
import { toast } from 'sonner';
import { useRouter } from 'next/navigation';

export default function AroundMePage() {
  const router = useRouter();
  const [mounted, setMounted] = useState(false);
  const [isLocationModalOpen, setIsLocationModalOpen] = useState(false);
  const [matchingCandidates, setMatchingCandidates] = useState<AICandidate[]>([]);
  const [emergencyMode, setEmergencyMode] = useState<'LOST' | 'FOUND'>('LOST');

  const {
    activeTab, setActiveTab,
    currentLocation, setCurrentLocation,
    userProfile, myDogs,
    mapCenter, setMapCenter,
    isSubmitting, isSuccess,
    selectedPin, setSelectedPin,
    editingThread,
    myActiveThread,
    visibleMarkers,
    isLoading,
    sortBy, setSortBy,
    getRemainingTime,
    handleJoinThread,
    handleDeleteThread,
    handleEditThread,
    handleRecruitSubmit,
    handleEmergencySubmit: originalEmergencySubmit
  } = useRadarLogic();

  useEffect(() => {
    // 하이드레이션 이후에만 렌더링되도록 보장
    const timer = setTimeout(() => setMounted(true), 0);
    return () => clearTimeout(timer);
  }, []);

  /**
   * 이웃과 채팅방을 생성하고 해당 방으로 이동합니다. (Real Backend Logic)
   */
  const handleStartChat = useCallback(async (partnerId: string, partnerNickname: string) => {
    try {
      toast.loading(`${partnerNickname}님과 연결 중...`);
      const res = await chatService.getOrCreateRoom(partnerId);
      if (res) {
        toast.dismiss();
        toast.success('채팅방이 준비되었습니다.');
        router.push(`/chat/${res.id}`);
      }
    } catch (e) {
      toast.dismiss();
      toast.error('채팅방을 생성할 수 없습니다.');
      console.error(e);
    }
  }, [router]);

  const handleEmergencySubmit = async (image: string, result: any, memo: string, mode: 'LOST' | 'FOUND') => {
    setEmergencyMode(mode);
    await originalEmergencySubmit(image, result, memo, mode);
    
    // 주변 스레드 작성자 기반 AI 매칭 (반경 내 실제 이웃 우선)
    try {
      const threadAuthors = visibleMarkers
        .filter((m: any) => m.author?.id !== userProfile?.id && m.author?.dogs?.length > 0)
        .map((m: any) => m.author)
        .filter((a: any, idx: number, arr: any[]) => arr.findIndex((x: any) => x.id === a.id) === idx); // 중복 제거

      const neighbors = threadAuthors.length > 0
        ? threadAuthors.slice(0, 2)
        : (await memberService.getFollowers()).slice(0, 2);

      const dynamicCandidates = neighbors.map((n, i) => ({
        id: n.id,
        name: mode === 'LOST' ? `${n.dogs[0]?.name || '댕댕이'}` : `실종된 ${n.dogs[0]?.name || '아이'}`,
        breed: result.breed?.ko || n.dogs[0]?.breed || '포메라니안',
        age: n.dogs[0]?.age || 3,
        gender: n.dogs[0]?.gender || 'M',
        matchRate: 90 - (i * 10) + Math.floor(Math.random() * 5),
        location: n.location || '성수동 인근',
        ownerNickname: n.nickname,
        lastSeen: mode === 'LOST' ? '방금 전 인근에서 비슷한 아이를 목격했다는 제보가 있었습니다.' : '실종된 아이와 인상착의가 매우 비슷합니다.',
        image: n.dogs[0]?.image || '/AINIINU_ROGO_B.png',
        type: mode
      }));

      setMatchingCandidates(dynamicCandidates);
    } catch (e) {
      // Fallback if API fails
      setMatchingCandidates([]);
    }
  };

  const optimizeImage = (base64: string): Promise<string> => {
    return new Promise((resolve) => {
      const img = new Image();
      img.onload = () => {
        const canvas = document.createElement('canvas');
        const MAX = 800; let w = img.width; let h = img.height;
        if (w > h) { if (w > MAX) { h *= MAX / w; w = MAX; } }
        else { if (h > MAX) { w *= MAX / h; h = MAX; } }
        canvas.width = w; canvas.height = h;
        const ctx = canvas.getContext('2d'); ctx?.drawImage(img, 0, 0, w, h);
        resolve(canvas.toDataURL('image/jpeg', 0.5));
      };
      img.src = base64;
    });
  };

  if (!mounted || isLoading) return <div className="flex items-center justify-center h-full"><p className="text-zinc-400">Loading...</p></div>;
  if (!userProfile) return <div className="flex items-center justify-center h-full"><p className="text-zinc-400">Loading...</p></div>;

  return (
    <div className="h-full flex flex-col animate-in slide-in-from-right duration-500 bg-background text-black relative">
      <AroundMeHeader
        currentLocation={currentLocation}
        onLocationClick={() => setIsLocationModalOpen(true)}
        activeTab={activeTab}
        onTabChange={(tab) => { setActiveTab(tab); setSelectedPin(null); }}
      />

      <div className="flex-1 flex flex-col lg:flex-row px-4 md:px-8 py-6 gap-8 overflow-hidden">
        {(activeTab === 'FIND' || activeTab === 'EMERGENCY') && (
          <RadarMapSection
            mapCenter={mapCenter}
            visibleMarkers={visibleMarkers}
            selectedPin={selectedPin}
            setSelectedPin={setSelectedPin}
            userNickname={userProfile.nickname}
            getRemainingTime={getRemainingTime}
            onJoinThread={handleJoinThread}
            onContact={(id) => handleStartChat(id, '이웃')}
            onDeleteThread={handleDeleteThread}
            onEditThread={handleEditThread}
            activeTab={activeTab}
          />
        )}

        {activeTab === 'FIND' && (
          <RadarSidebar
            isLoading={isLoading}
            visibleMarkers={visibleMarkers}
            sortBy={sortBy}
            setSortBy={setSortBy}
            onCardClick={(t) => { if (t.lat != null && t.lng != null) setMapCenter([t.lat, t.lng]); setSelectedPin(t); }}
            getRemainingTime={getRemainingTime}
            currentUserId={userProfile?.id}
            onDeleteThread={handleDeleteThread}
            onEditThread={handleEditThread}
          />
        )}
        
        {activeTab === 'RECRUIT' && (
          <div className="flex-1 overflow-y-auto no-scrollbar">
            <RecruitForm
              currentLocation={currentLocation}
              onLocationClick={() => setIsLocationModalOpen(true)}
              myDogs={myDogs}
              isSubmitting={isSubmitting}
              isSuccess={isSuccess}
              onSubmit={handleRecruitSubmit}
              editingThread={editingThread}
              myActiveThread={myActiveThread}
            />
          </div>
        )}

        {activeTab === 'EMERGENCY' && (
          <div className="flex-1 overflow-y-auto no-scrollbar">
            {matchingCandidates.length > 0 ? (
              <AICandidateList 
                candidates={matchingCandidates} 
                mode={emergencyMode}
                onClose={() => setMatchingCandidates([])}
                onContact={(c) => handleStartChat(c.id, c.ownerNickname)}
              />
            ) : (
              <EmergencyReportForm 
                isSubmitting={isSubmitting} 
                isSuccess={isSuccess} 
                onSubmit={handleEmergencySubmit} 
                optimizeImage={optimizeImage} 
              />
            )}
          </div>
        )}
      </div>

      {isLocationModalOpen && (
        <div className="fixed inset-0 z-[2000] bg-navy-900/40 backdrop-blur-md flex items-center justify-center p-6 animate-in fade-in duration-300" onClick={e => e.target === e.currentTarget && setIsLocationModalOpen(false)}>
           <Card className="w-full max-w-2xl p-0 bg-white shadow-2xl animate-in zoom-in-95 duration-500 border-none overflow-hidden flex flex-col h-[600px] rounded-[48px]">
              <div className="flex items-center justify-between p-8 shrink-0 border-b border-zinc-50">
                <Typography variant="h3" className="text-navy-900 font-serif">동네 설정하기</Typography>
                <button onClick={() => setIsLocationModalOpen(false)} className="p-2 text-zinc-300 hover:text-navy-900 transition-colors">
                  <X size={32} />
                </button>
              </div>
              <div className="flex-1 overflow-y-auto">
                <DaumPostcode onComplete={(data) => { setCurrentLocation(data.address); setIsLocationModalOpen(false); }} style={{ height: '100%' }} />
              </div>
           </Card>
        </div>
      )}
    </div>
  );
}
