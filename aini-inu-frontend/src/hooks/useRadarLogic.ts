'use client';

import { useState, useEffect, useCallback } from 'react';
import { toast } from 'sonner';
import {
  getThreads,
  getThreadMap,
  getHotspots,
  getThread,
  deleteThread,
  getMyActiveThread,
} from '@/api/threads';
import type {
  ThreadSummaryResponse,
  ThreadMapResponse,
  ThreadHotspotResponse,
  ThreadResponse,
} from '@/api/threads';
import { getMyPets } from '@/api/pets';
import type { PetResponse } from '@/api/pets';
import { useConfigStore } from '@/store/useConfigStore';

type SubView = 'FIND' | 'RECRUIT' | 'EMERGENCY';

const SEOUL_CITY_HALL: [number, number] = [37.566295, 126.977945];

export function isExpired(startTime: string): boolean {
  return Date.now() - Date.parse(startTime) >= 60 * 60 * 1000;
}

export function useRadarLogic() {
  const { setCoordinates, setLocation } = useConfigStore();

  // Tab state
  const [activeTab, setActiveTab] = useState<SubView>('FIND');

  // GPS / coordinates
  const [coordinates, setCoordinates_] = useState<[number, number]>(SEOUL_CITY_HALL);
  const [gpsLoading, setGpsLoading] = useState(true);

  // Thread list (sidebar, paginated)
  const [threadList, setThreadList] = useState<ThreadSummaryResponse[]>([]);
  const [threadListPage, setThreadListPage] = useState(0);
  const [threadListHasNext, setThreadListHasNext] = useState(false);

  // Map markers
  const [mapMarkers, setMapMarkers] = useState<ThreadMapResponse[]>([]);

  // Hotspots
  const [hotspots, setHotspots] = useState<ThreadHotspotResponse[]>([]);

  // Selected thread detail
  const [selectedThread, setSelectedThread] = useState<ThreadResponse | null>(null);

  // My pets
  const [myPets, setMyPets] = useState<PetResponse[]>([]);

  // Loading states
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);

  // Local clock for expiry display (NOT a data fetch trigger)
  const [currentTime, setCurrentTime] = useState(new Date());

  // Edit state
  const [editingThreadId, setEditingThreadId] = useState<number | null>(null);

  // My active thread
  const [myActiveThread, setMyActiveThread] = useState<ThreadSummaryResponse | null>(null);

  // Date filter
  const [dateFrom, setDateFrom] = useState<string>('');
  const [dateTo, setDateTo] = useState<string>('');

  // Radius (km)
  const [radius, setRadius] = useState<number>(5);

  // Search coordinates (may differ from GPS after 동네 설정)
  const [searchCoordinates, setSearchCoordinates] = useState<[number, number] | null>(null);

  // ---------------------------------------------------------------
  // Fetch thread data (list + map + hotspots)
  // ---------------------------------------------------------------
  const fetchThreadData = useCallback(async (coords: [number, number], filterDateFrom?: string, filterDateTo?: string, radiusKm?: number) => {
    const [latitude, longitude] = coords;
    try {
      const [listResult, markerResult, hotspotResult] = await Promise.all([
        getThreads({ page: 0, size: 20, startDate: filterDateFrom || undefined, endDate: filterDateTo || undefined, latitude, longitude, radius: radiusKm ?? 5 }),
        getThreadMap({ latitude, longitude, radius: radiusKm ?? 5, startDate: filterDateFrom || undefined, endDate: filterDateTo || undefined }),
        getHotspots(),
      ]);
      setThreadList(listResult.content);
      setThreadListPage(0);
      setThreadListHasNext(listResult.hasNext);
      setMapMarkers(markerResult);
      setHotspots(hotspotResult);
    } catch {
      toast.error('스레드를 불러오는데 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  // ---------------------------------------------------------------
  // GPS acquisition on mount (once)
  // ---------------------------------------------------------------
  useEffect(() => {
    const applyCoords = (coords: [number, number]) => {
      setCoordinates_(coords);
      setCoordinates(coords); // sync to config store
      setLocation('현재 위치');
      setGpsLoading(false);
    };

    if (typeof navigator !== 'undefined' && navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          const lat = Number(pos.coords.latitude.toFixed(6));
          const lng = Number(pos.coords.longitude.toFixed(6));
          applyCoords([lat, lng]);
        },
        () => {
          // Permission denied, timeout, or unavailable — use Seoul City Hall fallback
          applyCoords(SEOUL_CITY_HALL);
        },
        { timeout: 10000, enableHighAccuracy: false },
      );
    } else {
      applyCoords(SEOUL_CITY_HALL);
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // ---------------------------------------------------------------
  // Fetch my pets on mount
  // ---------------------------------------------------------------
  useEffect(() => {
    getMyPets()
      .then(setMyPets)
      .catch(() => {
        // Non-critical — silently ignore
      });
    getMyActiveThread()
      .then(list => setMyActiveThread(list.length > 0 ? list[0] : null))
      .catch(() => {});
  }, []);

  // ---------------------------------------------------------------
  // Fetch thread data once GPS is ready
  // ---------------------------------------------------------------
  useEffect(() => {
    if (!gpsLoading) {
      fetchThreadData(coordinates);
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [gpsLoading]);

  // ---------------------------------------------------------------
  // Local expiry timer — updates display clock every 60 seconds
  // NO data fetching here
  // ---------------------------------------------------------------
  useEffect(() => {
    const clockInterval = setInterval(() => setCurrentTime(new Date()), 60000);
    return () => clearInterval(clockInterval);
  }, []);

  // ---------------------------------------------------------------
  // Load more (sidebar pagination)
  // ---------------------------------------------------------------
  const loadMore = useCallback(async () => {
    if (!threadListHasNext) return;
    try {
      const nextPage = threadListPage + 1;
      const coords = searchCoordinates ?? coordinates;
      const result = await getThreads({
        page: nextPage,
        size: 20,
        startDate: dateFrom || undefined,
        endDate: dateTo || undefined,
        latitude: coords[0],
        longitude: coords[1],
        radius,
      });
      setThreadList((prev) => [...prev, ...result.content]);
      setThreadListPage(nextPage);
      setThreadListHasNext(result.hasNext);
    } catch {
      toast.error('더 불러오는데 실패했습니다.');
    }
  }, [threadListPage, threadListHasNext, dateFrom, dateTo, coordinates, searchCoordinates, radius]);

  // ---------------------------------------------------------------
  // Manual re-search (DEC-029: only way to refetch)
  // ---------------------------------------------------------------
  const handleRefresh = useCallback(async () => {
    setIsRefreshing(true);
    const coords = searchCoordinates ?? coordinates;
    await fetchThreadData(coords, dateFrom, dateTo, radius);
    getMyActiveThread()
      .then(list => setMyActiveThread(list.length > 0 ? list[0] : null))
      .catch(() => {});
    setIsRefreshing(false);
  }, [fetchThreadData, coordinates, searchCoordinates, dateFrom, dateTo, radius]);

  // ---------------------------------------------------------------
  // Thread selection
  // ---------------------------------------------------------------
  const selectThread = useCallback(async (threadId: number) => {
    try {
      const detail = await getThread(threadId);
      setSelectedThread(detail);
    } catch {
      toast.error('스레드 상세를 불러오는데 실패했습니다.');
    }
  }, []);

  const clearSelection = useCallback(() => {
    setSelectedThread(null);
  }, []);

  // ---------------------------------------------------------------
  // Action handlers
  // ---------------------------------------------------------------
  const handleDeleteThread = useCallback(async (threadId: number) => {
    try {
      await deleteThread(threadId);
      toast.success('모집글이 삭제되었습니다.');
      setEditingThreadId(null);
      setMyActiveThread(null);
      clearSelection();
      const coords = searchCoordinates ?? coordinates;
      await fetchThreadData(coords, dateFrom, dateTo, radius);
    } catch {
      toast.error('삭제에 실패했습니다.');
    }
  }, [clearSelection, fetchThreadData, coordinates, searchCoordinates, dateFrom, dateTo, radius]);

  const startEdit = useCallback((threadId: number) => {
    setEditingThreadId(threadId);
    setActiveTab('RECRUIT');
  }, []);

  return {
    // Tab
    activeTab,
    setActiveTab,
    // GPS
    coordinates,
    gpsLoading,
    // Thread list
    threadList,
    threadListHasNext,
    loadMore,
    // Map & hotspots
    mapMarkers,
    hotspots,
    // Thread detail
    selectedThread,
    selectThread,
    clearSelection,
    // Pets
    myPets,
    // Loading
    isLoading,
    isRefreshing,
    // Display clock (no data fetch)
    currentTime,
    isExpired,
    // Edit
    editingThreadId,
    startEdit,
    // My active thread
    myActiveThread,
    // Date filter
    dateFrom,
    dateTo,
    setDateFrom,
    setDateTo,
    // Radius
    radius,
    setRadius,
    // Search coordinates
    searchCoordinates,
    setSearchCoordinates,
    // Actions
    handleDeleteThread,
    handleRefresh,
  };
}
