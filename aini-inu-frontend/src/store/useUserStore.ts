import { create } from 'zustand';
import { UserType } from '@/types';
import { getMe, updateMe } from '@/api/members';
import type { MemberResponse, MemberProfilePatchRequest } from '@/api/members';
import { useDashboardStore } from '@/store/useDashboardStore';

// Map MemberResponse to UserType for store compatibility
function mapMemberToUser(member: MemberResponse): UserType {
  return {
    id: String(member.id),
    email: member.email,
    nickname: member.nickname,
    handle: member.nickname,
    avatar: member.profileImageUrl || '',
    mannerScore: member.mannerTemperature || 0,
    isOwner: member.memberType === 'OWNER',
    birthDate: '',
    age: member.age || 0,
    gender: (member.gender as 'M' | 'F') || 'M',
    mbti: member.mbti,
    phone: member.phone,
    nicknameChangedAt: member.nicknameChangedAt,
    about: member.selfIntroduction || '',
    location: '',
    dogs: [],
    followerCount: 0,
    followingCount: 0,
    tendencies: member.personalityTypes?.map((pt) => pt.name) || [],
  };
}

interface UserState {
  profile: UserType | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  hasFetched: boolean;

  // Actions
  setProfile: (profile: UserType | MemberResponse | null) => void;
  fetchProfile: (force?: boolean) => Promise<void>;
  updateProfile: (data: Partial<UserType> | MemberProfilePatchRequest) => Promise<boolean>;
  clearProfile: () => void;
  logout: () => void;
}

export const useUserStore = create<UserState>((set, get) => ({
  profile: null,
  isAuthenticated: false,
  isLoading: false,
  hasFetched: false,

  setProfile: (profile) => {
    if (!profile) {
      set({ profile: null, isAuthenticated: false });
      return;
    }
    // Handle MemberResponse (from API) or UserType (legacy)
    if ('email' in profile && 'mannerTemperature' in profile) {
      // It's a MemberResponse — map it
      set({ profile: mapMemberToUser(profile as MemberResponse), isAuthenticated: true });
    } else {
      set({ profile: profile as UserType, isAuthenticated: !!profile });
    }
  },

  fetchProfile: async (force?: boolean) => {
    if (!force && (get().hasFetched || get().isLoading)) return;
    set({ isLoading: true });
    try {
      const data = await getMe();
      set({ profile: mapMemberToUser(data), isAuthenticated: true, hasFetched: true });
    } catch (error) {
      console.error('Failed to fetch user profile:', error);
      set({ profile: null, isAuthenticated: false, hasFetched: true });
    } finally {
      set({ isLoading: false });
    }
  },

  updateProfile: async (data) => {
    try {
      const updated = await updateMe(data as MemberProfilePatchRequest);
      set({ profile: mapMemberToUser(updated) });
      return true;
    } catch (error) {
      console.error('Failed to update profile:', error);
      return false;
    }
  },

  clearProfile: () => {
    set({ profile: null, isAuthenticated: false, hasFetched: false });
  },

  logout: () => {
    set({ profile: null, isAuthenticated: false, hasFetched: false });
    useDashboardStore.getState().clear();
  },
}));
