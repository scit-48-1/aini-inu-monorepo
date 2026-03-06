import { apiClient } from './client';
import type { ApiRequestOptions } from './client';
import type { LoginResponse } from './auth';
import type { PaginationParams, SliceResponse } from './types';
import type { PetResponse, BreedResponse, PersonalityResponse, WalkingStyleResponse } from './pets';

// Re-export for consumers that need PetResponse via members
export type { PetResponse, BreedResponse, PersonalityResponse, WalkingStyleResponse };

// --- Types ---

export interface MemberResponse {
  id: number;
  email: string;
  nickname: string;
  memberType: string;
  profileImageUrl: string;
  linkedNickname: string;
  phone: string;
  age: number;
  gender: string;
  mbti: string;
  personality: string;
  selfIntroduction: string;
  personalityTypes: MemberPersonalityTypeResponse[];
  mannerTemperature: number;
  status: string;
  createdAt: string;
  nicknameChangedAt: string;
  verified: boolean;
  isVerified: boolean;
}

export interface MemberSignupRequest {
  email: string;
  password: string;
  nickname: string;
  memberType: string;
}

export interface MemberCreateRequest {
  nickname: string;
  profileImageUrl?: string;
  linkedNickname?: string;
  phone?: string;
  age?: number;
  gender?: string;
  mbti?: string;
  personality?: string;
  selfIntroduction?: string;
  personalityTypeIds?: number[];
}

export interface MemberProfilePatchRequest {
  nickname?: string;
  profileImageUrl?: string;
  linkedNickname?: string;
  phone?: string;
  age?: number;
  gender?: string;
  mbti?: string;
  personality?: string;
  selfIntroduction?: string;
  personalityTypeIds?: number[];
}

export interface MemberFollowResponse {
  id: number;
  nickname: string;
  profileImageUrl: string;
  mannerTemperature: number;
  followedAt: string;
}

export interface FollowStatusResponse {
  following: boolean;
  isFollowing: boolean;
}

export interface WalkStatsResponse {
  windowDays: number;
  startDate: string;
  endDate: string;
  timezone: string;
  totalWalks: number;
  points: WalkStatsPointResponse[];
}

export interface WalkStatsPointResponse {
  date: string;
  count: number;
}

export interface MemberPersonalityTypeResponse {
  id: number;
  name: string;
  code: string;
}

// --- Helpers ---

function buildQuery(params: Record<string, unknown>): string {
  const entries = Object.entries(params).filter(
    ([, v]) => v !== undefined && v !== null,
  );
  if (entries.length === 0) return '';
  return '?' + entries.map(([k, v]) => `${k}=${encodeURIComponent(String(v))}`).join('&');
}

// --- Functions ---

export async function signup(data: MemberSignupRequest, options?: ApiRequestOptions): Promise<LoginResponse> {
  return apiClient.post<LoginResponse>('/members/signup', data, options);
}

export async function createProfile(data: MemberCreateRequest, options?: ApiRequestOptions): Promise<MemberResponse> {
  return apiClient.post<MemberResponse>('/members/profile', data, options);
}

export async function getMe(): Promise<MemberResponse> {
  return apiClient.get<MemberResponse>('/members/me');
}

export async function updateMe(data: MemberProfilePatchRequest): Promise<MemberResponse> {
  return apiClient.patch<MemberResponse>('/members/me', data);
}

export async function getMember(memberId: number): Promise<MemberResponse> {
  return apiClient.get<MemberResponse>(`/members/${memberId}`);
}

export async function getMemberPets(memberId: number): Promise<PetResponse[]> {
  return apiClient.get<PetResponse[]>(`/members/${memberId}/pets`);
}

export async function getFollowers(params?: PaginationParams & { memberId?: number }): Promise<SliceResponse<MemberFollowResponse>> {
  const { memberId, ...rest } = params ?? {};
  const query = buildQuery({ ...rest });
  const base = memberId != null ? `/members/${memberId}/followers` : '/members/me/followers';
  return apiClient.get<SliceResponse<MemberFollowResponse>>(`${base}${query}`);
}

export async function getFollowing(params?: PaginationParams & { memberId?: number }): Promise<SliceResponse<MemberFollowResponse>> {
  const { memberId, ...rest } = params ?? {};
  const query = buildQuery({ ...rest });
  const base = memberId != null ? `/members/${memberId}/following` : '/members/me/following';
  return apiClient.get<SliceResponse<MemberFollowResponse>>(`${base}${query}`);
}

export async function follow(targetId: number): Promise<FollowStatusResponse> {
  return apiClient.post<FollowStatusResponse>(`/members/me/follows/${targetId}`);
}

export async function unfollow(targetId: number): Promise<FollowStatusResponse> {
  return apiClient.delete<FollowStatusResponse>(`/members/me/follows/${targetId}`);
}

export async function getFollowStatus(targetId: number): Promise<FollowStatusResponse> {
  return apiClient.get<FollowStatusResponse>(`/members/me/follows/${targetId}`);
}

export async function getWalkStats(): Promise<WalkStatsResponse> {
  return apiClient.get<WalkStatsResponse>('/members/me/stats/walk');
}

export async function searchMembers(q: string, params?: PaginationParams): Promise<SliceResponse<MemberResponse>> {
  const query = buildQuery({ q, ...params });
  return apiClient.get<SliceResponse<MemberResponse>>(`/members/search${query}`);
}

export async function getPersonalityTypes(): Promise<MemberPersonalityTypeResponse[]> {
  return apiClient.get<MemberPersonalityTypeResponse[]>('/member-personality-types');
}
