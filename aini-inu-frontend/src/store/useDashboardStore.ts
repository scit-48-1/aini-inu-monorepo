import { create } from 'zustand';
import type { ActivityStatsResponse } from '@/api/members';
import type { ThreadHotspotResponse, ThreadSummaryResponse } from '@/api/threads';
import type { PetResponse } from '@/api/pets';

export type SectionStatus = 'idle' | 'loading' | 'refreshing' | 'success' | 'empty' | 'error';

export type SectionState<T> =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'refreshing'; data: T }
  | { status: 'error'; message: string }
  | { status: 'empty' }
  | { status: 'success'; data: T };

export interface RecentFriend {
  id: string;
  roomId: string;
  name: string;
  img: string;
  score: number;
}

interface DashboardState {
  activityStats: SectionState<ActivityStatsResponse>;
  hotspots: SectionState<ThreadHotspotResponse[]>;
  threads: SectionState<ThreadSummaryResponse[]>;
  myPets: PetResponse[];
  myPetsLoaded: boolean;
  recentFriends: RecentFriend[];
  recentFriendsLoaded: boolean;
  lastFetchedAt: number | null;

  // Actions
  setActivityStats: (state: SectionState<ActivityStatsResponse>) => void;
  setHotspots: (state: SectionState<ThreadHotspotResponse[]>) => void;
  setThreads: (state: SectionState<ThreadSummaryResponse[]>) => void;
  setMyPets: (pets: PetResponse[]) => void;
  setRecentFriends: (friends: RecentFriend[]) => void;
  markFetched: () => void;
  shouldFetch: () => boolean;
  clear: () => void;
}

const STALE_MS = 30_000; // 30 seconds

const initialState = {
  activityStats: { status: 'idle' as const },
  hotspots: { status: 'idle' as const },
  threads: { status: 'idle' as const },
  myPets: [] as PetResponse[],
  myPetsLoaded: false,
  recentFriends: [] as RecentFriend[],
  recentFriendsLoaded: false,
  lastFetchedAt: null as number | null,
};

export const useDashboardStore = create<DashboardState>((set, get) => ({
  ...initialState,

  setActivityStats: (state) => set({ activityStats: state }),
  setHotspots: (state) => set({ hotspots: state }),
  setThreads: (state) => set({ threads: state }),
  setMyPets: (pets) => set({ myPets: pets, myPetsLoaded: true }),
  setRecentFriends: (friends) => set({ recentFriends: friends, recentFriendsLoaded: true }),

  markFetched: () => set({ lastFetchedAt: Date.now() }),

  shouldFetch: () => {
    const { lastFetchedAt } = get();
    if (!lastFetchedAt) return true;
    return Date.now() - lastFetchedAt > STALE_MS;
  },

  clear: () => set(initialState),
}));
