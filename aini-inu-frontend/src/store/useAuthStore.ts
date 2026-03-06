import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;

  // Actions
  setTokens: (accessToken: string, refreshToken: string) => void;
  clearTokens: () => void;
  getAccessToken: () => string | null;
  getRefreshToken: () => string | null;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken: null,
      refreshToken: null,

      setTokens: (accessToken: string, refreshToken: string) =>
        set({ accessToken, refreshToken }),

      clearTokens: () => set({ accessToken: null, refreshToken: null }),

      getAccessToken: () => get().accessToken,

      getRefreshToken: () => get().refreshToken,
    }),
    {
      name: 'aini-inu-auth',
      partialize: (state) => ({ refreshToken: state.refreshToken }),
      skipHydration: true,
      // Custom storage: typeof window checked at call time (not at module init/SSR time)
      storage: {
        getItem: (name: string) => {
          if (typeof window === 'undefined') return null;
          const str = window.localStorage.getItem(name);
          if (!str) return null;
          try { return JSON.parse(str); } catch { return null; }
        },
        setItem: (name: string, value: unknown) => {
          if (typeof window === 'undefined') return;
          window.localStorage.setItem(name, JSON.stringify(value));
        },
        removeItem: (name: string) => {
          if (typeof window === 'undefined') return;
          window.localStorage.removeItem(name);
        },
      },
    }
  )
);
