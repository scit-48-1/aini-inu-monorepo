'use client';

import { useState, useEffect, useMemo, useCallback } from 'react';
import { toast } from 'sonner';
import { threadService } from '@/services/api/threadService';
import { memberService } from '@/services/api/memberService';
import { locationService } from '@/services/api/locationService';
import { useConfigStore } from '@/store/useConfigStore';
import { calculateDistance, getRemainingTimeStr } from '@/lib/utils';
import { ThreadType, UserType, DogType } from '@/types';

type SubView = 'FIND' | 'RECRUIT' | 'EMERGENCY';

export function useRadarLogic() {
  // 1. 상태 관리 (State Management)
  const [activeTab, setActiveTab] = useState<SubView>('FIND');
  const [userProfile, setUserProfile] = useState<UserType | null>(null);
  const [myDogs, setMyDogs] = useState<DogType[]>([]);
  const [allThreads, setAllThreads] = useState<ThreadType[]>([]);
  const [selectedPin, setSelectedPin] = useState<ThreadType | null>(null);
  const [editingThread, setEditingThread] = useState<ThreadType | null>(null);
  const [sortBy, setSortBy] = useState<'DISTANCE' | 'TIME'>('DISTANCE');
  
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [currentTime, setCurrentTime] = useState(new Date());

  // Zustand Global State
  const { 
    currentLocation, 
    setLocation, 
    lastCoordinates: mapCenter, 
    setCoordinates: setMapCenter 
  } = useConfigStore();

  // 2. 데이터 페칭 (Data Fetching)
  const fetchData = useCallback(async () => {
    try {
      const [threads, profile, dogs] = await Promise.all([
        threadService.getThreads(mapCenter[0], mapCenter[1]), 
        memberService.getMe(),
        memberService.getMyDogs()
      ]);
      setAllThreads(threads || []);
      setUserProfile(profile);
      setMyDogs(dogs || []);
      
      // 초기 1회 프로필 위치 연동
      if (profile?.location && !currentLocation) {
        setLocation(profile.location);
      }
    } catch (e) {
      console.error('Failed to fetch radar data:', e);
    } finally {
      setIsLoading(false);
    }
  }, [mapCenter, currentLocation, setLocation]);

  // 3. 위치 동기화 (DIP: locationService)
  useEffect(() => {
    if (!currentLocation) return;
    locationService.getCoordinates(currentLocation).then(coords => {
      if (coords?.length === 2) setMapCenter(coords);
    }).catch(console.error);
  }, [currentLocation, setMapCenter]);

  // 4. 주기적 갱신 (Polling)
  useEffect(() => {
    fetchData();
    const timers = [
      setInterval(fetchData, 10000),
      setInterval(() => setCurrentTime(new Date()), 10000)
    ];
    return () => timers.forEach(clearInterval);
  }, [fetchData]);

  // 5. 필터링 로직 (Memoized)
  const checkExpired = (startTime?: string) => {
    if (!startTime) return false;
    const [h, m] = startTime.split(':').map(Number);
    const target = new Date(currentTime);
    target.setHours(h, m, 0, 0);
    return (currentTime.getTime() - target.getTime()) / (1000 * 60) >= 60;
  };

  // 현재 유저의 활성 모집 스레드 (중복 등록 방지용)
  const myActiveThread = useMemo(() => {
    if (!userProfile) return null;
    return allThreads.find(t =>
      !t.isEmergency &&
      t.author?.id === userProfile.id &&
      !checkExpired(t.time || t.startTime)
    ) || null;
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [allThreads, userProfile, currentTime]);

  const visibleMarkers = useMemo(() => {
    const isExpired = (startTime?: string) => {
      if (!startTime) return false;
      const [h, m] = startTime.split(':').map(Number);
      const target = new Date(currentTime);
      target.setHours(h, m, 0, 0);
      // 시작 1시간 후면 만료 처리
      return (currentTime.getTime() - target.getTime()) / (1000 * 60) >= 60;
    };

    let filtered = allThreads.filter(t => {
      if (activeTab === 'FIND') return !t.isEmergency && !isExpired(t.time || t.startTime);
      if (activeTab === 'EMERGENCY') return t.isEmergency;
      return false;
    });

    // 거리 계산 및 정렬
    const withDistance = filtered.map(t => ({
      ...t,
      distance: calculateDistance(mapCenter[0], mapCenter[1], t.lat ?? mapCenter[0], t.lng ?? mapCenter[1])
    })).filter(t => t.distance <= 5); // 5km 이내만 표시

    return withDistance.sort((a, b) => {
      if (sortBy === 'TIME') {
        const [ah, am] = (a.time || a.startTime || "00:00").split(':').map(Number);
        const [bh, bm] = (b.time || b.startTime || "00:00").split(':').map(Number);
        return (ah * 60 + am) - (bh * 60 + bm);
      }
      return a.distance - b.distance;
    });
  }, [activeTab, allThreads, currentTime, mapCenter, sortBy]);

  // 6. 액션 핸들러 (Action Handlers)
  const handleJoinThread = async (id: string | number) => {
    try {
      await threadService.joinThread(id);
      await fetchData();
      if (selectedPin?.id === id) {
        setSelectedPin({ ...selectedPin, isJoined: true });
      }
      toast.success('산책 신청이 완료되었습니다!');
    } catch (e) {
      toast.error('참여 신청에 실패했습니다.');
    }
  };

  const handleDeleteThread = async (id: string | number) => {
    try {
      await threadService.deleteThread(id);
      setSelectedPin(null);
      await fetchData();
      toast.success('모집글이 삭제되었습니다.');
    } catch {
      toast.error('삭제에 실패했습니다.');
    }
  };

  const handleEditThread = (thread: ThreadType) => {
    setEditingThread(thread);
    setSelectedPin(null);
    setActiveTab('RECRUIT');
  };

  const handleRecruitSubmit = async (formData: any, selectedDogsIds: string[]) => {
    setIsSubmitting(true);
    try {
      const dogs = myDogs.filter(d => selectedDogsIds.includes(d.id));

      if (editingThread) {
        // 수정 모드
        const updatedData = {
          ...formData,
          participatingDogs: dogs,
          name: dogs[0]?.name || editingThread.name || '강아지',
          image: dogs[0]?.image || editingThread.image,
        };
        await threadService.updateThread(editingThread.id, updatedData);
        setEditingThread(null);
        toast.success('모집글이 수정되었습니다.');
      } else {
        // 신규 등록 모드
        const newThread = {
          ...formData,
          lat: mapCenter[0],
          lng: mapCenter[1],
          name: dogs[0]?.name || '강아지',
          image: dogs[0]?.image,
          owner: userProfile?.nickname || '익명',
          author: userProfile ?? undefined,
          participatingDogs: dogs
        };
        await threadService.createThread(newThread);
      }

      await fetchData();
      setIsSuccess(true);
      setTimeout(() => {
        setIsSuccess(false);
        setActiveTab('FIND');
      }, 2000);
    } catch {
      toast.error(editingThread ? '모집글 수정에 실패했습니다.' : '모집글 등록에 실패했습니다.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleEmergencySubmit = async (image: string, analysis: any, memo: string, mode: string) => {
    setIsSubmitting(true);
    try {
      const newPin = {
        name: mode === 'FOUND' ? '제보됨' : '실종됨',
        breed: analysis?.breed?.ko || '알 수 없음',
        lat: mapCenter[0] + (Math.random() - 0.5) * 0.005,
        lng: mapCenter[1] + (Math.random() - 0.5) * 0.005,
        image,
        isEmergency: true,
        owner: '나의 제보',
        author: userProfile ?? undefined, // 제보자 정보 추가
        content: memo || '방금 등록된 긴급 정보입니다.'
      };
      
      await threadService.createThread(newPin);
      await fetchData();
      setIsSuccess(true);
      setTimeout(() => {
        setIsSuccess(false);
        setActiveTab('FIND');
      }, 3000);
    } catch (e) {
      toast.error('긴급 제보 등록에 실패했습니다.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const getRemainingTime = useCallback((startTime?: string) => {
    return getRemainingTimeStr(startTime, currentTime);
  }, [currentTime]);

  return {
    activeTab, setActiveTab,
    currentLocation, setCurrentLocation: setLocation,
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
    handleEmergencySubmit,
    fetchData
  };
}
