'use client';

import React, { createContext, use, useEffect, useState } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import * as authApi from '@/api/auth';
import { setLoggingOut } from '@/api/client';
import { getMe } from '@/api/members';
import { useAuthStore } from '@/store/useAuthStore';
import { useUserStore } from '@/store/useUserStore';

// --- Auth context interface ---

interface AuthContextValue {
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

// --- Protected paths that require authentication ---

const PROTECTED_PATHS = ['/dashboard', '/feed', '/chat', '/around-me', '/settings', '/profile'];

// --- AuthProvider ---

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const [isLoading, setIsLoading] = useState(true);

  const { setTokens, clearTokens, getRefreshToken, getAccessToken } = useAuthStore.getState();
  const { setProfile, clearProfile } = useUserStore.getState();

  // Derive isAuthenticated from auth store (reactive)
  const accessToken = useAuthStore((s) => s.accessToken);
  const isAuthenticated = !!accessToken;

  // Bootstrap: on mount, restore session from persisted refreshToken
  useEffect(() => {
    async function bootstrap() {
      await useAuthStore.persist.rehydrate();
      const storedRefreshToken = getRefreshToken();
      const storedAccessToken = getAccessToken();

      if (!storedRefreshToken) {
        setIsLoading(false);
        const isProtected = PROTECTED_PATHS.some((p) => pathname.startsWith(p));
        if (isProtected) {
          router.replace('/login');
        }
        return;
      }

      if (storedAccessToken) {
        // Access token already in memory — fetch profile silently
        try {
          const profile = await getMe();
          setProfile(profile as unknown as Parameters<typeof setProfile>[0]);
        } catch {
          // Profile fetch failed — try refresh
          try {
            const refreshed = await authApi.refreshToken({ refreshToken: storedRefreshToken });
            setTokens(refreshed.accessToken, refreshed.refreshToken);
            const profile = await getMe();
            setProfile(profile as unknown as Parameters<typeof setProfile>[0]);
          } catch {
            // Refresh also failed — clear everything
            clearTokens();
            clearProfile();
            const isProtected = PROTECTED_PATHS.some((p) => pathname.startsWith(p));
            if (isProtected) {
              router.replace('/login');
            }
          }
        }
      } else {
        // Have refresh token but no access token — attempt silent refresh
        try {
          const refreshed = await authApi.refreshToken({ refreshToken: storedRefreshToken });
          setTokens(refreshed.accessToken, refreshed.refreshToken);
          const profile = await getMe();
          setProfile(profile as unknown as Parameters<typeof setProfile>[0]);
        } catch {
          clearTokens();
          clearProfile();
          const isProtected = PROTECTED_PATHS.some((p) => pathname.startsWith(p));
          if (isProtected) {
            router.replace('/login');
          }
        }
      }

      setIsLoading(false);
    }

    bootstrap();
    // Run only once on mount
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // --- Login action ---

  const login = async (email: string, password: string): Promise<void> => {
    setLoggingOut(false);
    const response = await authApi.login({ email, password });
    setTokens(response.accessToken, response.refreshToken);
    const profile = await getMe();
    setProfile(profile as unknown as Parameters<typeof setProfile>[0]);
    router.push('/dashboard');
  };

  // --- Logout action ---

  const logout = async (): Promise<void> => {
    setLoggingOut(true);
    const storedRefreshToken = getRefreshToken();
    if (storedRefreshToken) {
      try {
        await authApi.logout({ refreshToken: storedRefreshToken });
      } catch {
        // Ignore logout errors — still clear local state
      }
    }
    clearTokens();
    clearProfile();
    router.push('/login');
  };

  const value: AuthContextValue = {
    isAuthenticated,
    isLoading,
    login,
    logout,
  };

  // Block children until auth bootstrap completes — prevents race condition
  // where child components fire API calls (→ 401 → handle401 refresh) before
  // bootstrap's own refresh, causing RTR to invalidate the bootstrap's token.
  if (isLoading) {
    return null;
  }

  return <AuthContext value={value}>{children}</AuthContext>;
}

// --- useAuth hook ---

export function useAuth(): AuthContextValue {
  const ctx = use(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return ctx;
}
