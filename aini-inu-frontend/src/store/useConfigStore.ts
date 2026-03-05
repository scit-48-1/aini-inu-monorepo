import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface ConfigState {
  currentLocation: string;
  lastCoordinates: [number, number];
  pushEnabled: boolean;
  security2FA: boolean;

  // Actions
  setLocation: (location: string) => void;
  setCoordinates: (coords: [number, number]) => void;
  togglePush: () => void;
  toggle2FA: () => void;
}

export const useConfigStore = create<ConfigState>()(
  persist(
    (set) => ({
      currentLocation: '서울시 성수동',
      lastCoordinates: [37.5445, 127.0445],
      pushEnabled: true,
      security2FA: false,

      setLocation: (location) => set({ currentLocation: location }),
      setCoordinates: (coords) => set({ lastCoordinates: coords }),
      togglePush: () => set((state) => ({ pushEnabled: !state.pushEnabled })),
      toggle2FA: () => set((state) => ({ security2FA: !state.security2FA })),
    }),
    {
      name: 'aini-inu-config',
    }
  )
);
