import { create } from 'zustand';
import { UserType } from '@/types';
import { memberService } from '@/services/api/memberService';

interface UserState {
  profile: UserType | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  
  // Actions
  setProfile: (profile: UserType | null) => void;
  fetchProfile: () => Promise<void>;
  updateProfile: (data: Partial<UserType>) => Promise<boolean>;
  logout: () => void;
}

export const useUserStore = create<UserState>((set, get) => ({
  profile: null,
  isAuthenticated: false,
  isLoading: false,

  setProfile: (profile) => set({ profile, isAuthenticated: !!profile }),

  fetchProfile: async () => {
    set({ isLoading: true });
    try {
      const data = await memberService.getMe();
      set({ profile: data, isAuthenticated: true });
    } catch (error) {
      console.error('Failed to fetch user profile:', error);
      set({ profile: null, isAuthenticated: false });
    } finally {
      set({ isLoading: false });
    }
  },

  updateProfile: async (data) => {
    try {
      const updated = await memberService.updateMe(data);
      set({ profile: updated });
      return true;
    } catch (error) {
      console.error('Failed to update profile:', error);
      return false;
    }
  },

  logout: () => {
    try {
      const DB_KEY = 'aini_inu_v6_db';
      const raw = localStorage.getItem(DB_KEY);
      if (raw) {
        const db = JSON.parse(raw);
        db.currentUserId = null;
        localStorage.setItem(DB_KEY, JSON.stringify(db));
      }
    } catch { /* localStorage 접근 실패 시 무시 */ }
    set({ profile: null, isAuthenticated: false });
  },
}));
