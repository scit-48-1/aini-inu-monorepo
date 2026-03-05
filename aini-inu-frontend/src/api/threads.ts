import { apiClient } from './client';
import type { PaginationParams, SliceResponse } from './types';

// --- Types ---

export interface ThreadCreateRequest {
  title: string;
  description?: string;
  walkDate: string;
  startTime: string;
  endTime: string;
  chatType: string;
  maxParticipants: number;
  allowNonPetOwner?: boolean;
  isVisibleAlways?: boolean;
  location: LocationRequest;
  petIds: number[];
  filters?: ThreadFilterRequest[];
}

export interface ThreadPatchRequest {
  title?: string;
  description?: string;
  walkDate?: string;
  startTime?: string;
  endTime?: string;
  chatType?: string;
  maxParticipants?: number;
  allowNonPetOwner?: boolean;
  isVisibleAlways?: boolean;
  location?: LocationRequest;
  petIds?: number[];
  filters?: ThreadFilterRequest[];
}

export interface LocationRequest {
  placeName: string;
  latitude: number;
  longitude: number;
  address: string;
}

export interface ThreadFilterRequest {
  type: string;
  values: string[];
  isRequired: boolean;
}

export interface ThreadApplyRequest {
  petIds: number[];
}

export interface ThreadResponse {
  id: number;
  authorId: number;
  title: string;
  description: string;
  walkDate: string;
  startTime: string;
  endTime: string;
  chatType: string;
  maxParticipants: number;
  currentParticipants: number;
  allowNonPetOwner: boolean;
  isVisibleAlways: boolean;
  placeName: string;
  latitude: number;
  longitude: number;
  address: string;
  status: string;
  petIds: number[];
  applicants: ApplicantSummary[];
}

export interface ThreadSummaryResponse {
  id: number;
  title: string;
  description: string;
  chatType: string;
  maxParticipants: number;
  currentParticipants: number;
  placeName: string;
  latitude: number;
  longitude: number;
  startTime: string;
  endTime: string;
  status: string;
  applied: boolean;
  isApplied: boolean;
}

export interface ThreadApplyResponse {
  threadId: number;
  chatRoomId: number;
  applicationStatus: string;
  idempotentReplay: boolean;
  isIdempotentReplay: boolean;
}

export interface ThreadMapResponse {
  threadId: number;
  title: string;
  chatType: string;
  currentParticipants: number;
  maxParticipants: number;
  latitude: number;
  longitude: number;
  placeName: string;
}

export interface ThreadHotspotResponse {
  region: string;
  count: number;
}

export interface ApplicantSummary {
  memberId: number;
  status: string;
  chatRoomId: number;
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

export async function getThreads(params?: PaginationParams): Promise<SliceResponse<ThreadSummaryResponse>> {
  const query = buildQuery({ ...params });
  return apiClient.get<SliceResponse<ThreadSummaryResponse>>(`/threads${query}`);
}

export async function createThread(data: ThreadCreateRequest): Promise<ThreadResponse> {
  return apiClient.post<ThreadResponse>('/threads', data);
}

export async function getThread(threadId: number): Promise<ThreadResponse> {
  return apiClient.get<ThreadResponse>(`/threads/${threadId}`);
}

export async function updateThread(threadId: number, data: ThreadPatchRequest): Promise<ThreadResponse> {
  return apiClient.patch<ThreadResponse>(`/threads/${threadId}`, data);
}

export async function deleteThread(threadId: number): Promise<void> {
  return apiClient.delete<void>(`/threads/${threadId}`);
}

export async function applyToThread(threadId: number, data: ThreadApplyRequest): Promise<ThreadApplyResponse> {
  return apiClient.post<ThreadApplyResponse>(`/threads/${threadId}/apply`, data);
}

export async function cancelApplication(threadId: number): Promise<void> {
  return apiClient.delete<void>(`/threads/${threadId}/apply`);
}

export async function getThreadMap(params: {
  latitude: number;
  longitude: number;
  radius?: number;
}): Promise<ThreadMapResponse[]> {
  const query = buildQuery(params);
  return apiClient.get<ThreadMapResponse[]>(`/threads/map${query}`);
}

export async function getHotspots(hours?: number): Promise<ThreadHotspotResponse[]> {
  const query = hours !== undefined ? `?hours=${hours}` : '';
  return apiClient.get<ThreadHotspotResponse[]>(`/threads/hotspot${query}`);
}
