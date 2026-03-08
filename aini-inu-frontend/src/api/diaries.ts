import { apiClient } from './client';
import type { PaginationParams, SliceResponse } from './types';

// --- Types ---

export interface AvailableThreadResponse {
  threadId: number;
  title: string;
  walkDate: string;
  placeName: string;
  startTime: string;
}

export interface WalkDiaryCreateRequest {
  threadId: number;
  title: string;
  content: string;
  photoUrls?: string[];
  walkDate: string;
  isPublic?: boolean;
}

export interface WalkDiaryPatchRequest {
  threadId?: number;
  title?: string;
  content?: string;
  photoUrls?: string[];
  walkDate?: string;
  isPublic?: boolean;
}

export interface WalkDiaryResponse {
  id: number;
  memberId: number;
  threadId: number;
  title: string;
  content: string;
  photoUrls: string[];
  walkDate: string;
  linkedThreadStatus: string;
  createdAt: string;
  updatedAt: string;
  public: boolean;
  isPublic: boolean;
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

export async function getDiaries(params?: PaginationParams & { memberId?: number }): Promise<SliceResponse<WalkDiaryResponse>> {
  const query = buildQuery({ ...params });
  return apiClient.get<SliceResponse<WalkDiaryResponse>>(`/walk-diaries${query}`);
}

export async function createDiary(data: WalkDiaryCreateRequest): Promise<WalkDiaryResponse> {
  return apiClient.post<WalkDiaryResponse>('/walk-diaries', data);
}

export async function getDiary(diaryId: number): Promise<WalkDiaryResponse> {
  return apiClient.get<WalkDiaryResponse>(`/walk-diaries/${diaryId}`);
}

export async function updateDiary(diaryId: number, data: WalkDiaryPatchRequest): Promise<WalkDiaryResponse> {
  return apiClient.patch<WalkDiaryResponse>(`/walk-diaries/${diaryId}`, data);
}

export async function deleteDiary(diaryId: number): Promise<void> {
  return apiClient.delete<void>(`/walk-diaries/${diaryId}`);
}

export async function getFollowingDiaries(params?: PaginationParams): Promise<SliceResponse<WalkDiaryResponse>> {
  const query = buildQuery({ ...params });
  return apiClient.get<SliceResponse<WalkDiaryResponse>>(`/walk-diaries/following${query}`);
}

export async function getAvailableThreads(): Promise<AvailableThreadResponse[]> {
  return apiClient.get<AvailableThreadResponse[]>('/walk-diaries/available-threads');
}
