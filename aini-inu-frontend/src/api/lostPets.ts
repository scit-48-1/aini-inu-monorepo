import { apiClient } from './client';
import { SliceResponse, PaginationParams } from './types';

// --- Types ---

export interface LostPetCreateRequest {
  petName: string;
  breed: string;
  photoUrl: string;
  description: string;
  lastSeenAt: string;
  lastSeenLocation: string;
}

export interface LostPetResponse {
  lostPetId: number;
  status: string;
  createdAt: string;
}

export interface LostPetSummaryResponse {
  lostPetId: number;
  petName: string;
  status: string;
  lastSeenAt: string;
}

export interface LostPetDetailResponse {
  lostPetId: number;
  ownerId: number;
  petName: string;
  photoUrl: string;
  lastSeenAt: string;
  lastSeenLocation: string;
  status: string;
}

export interface LostPetAnalyzeRequest {
  lostPetId: number;
  image?: string;
  imageUrl?: string;
  mode?: string;
  queryText?: string;
  latitude?: number;
  longitude?: number;
  imageProvided?: boolean;
}

export interface LostPetAnalyzeResponse {
  sessionId: number;
  summary: string;
  candidates: LostPetAnalyzeCandidateResponse[];
}

export interface LostPetAnalyzeCandidateResponse {
  sightingId: number;
  finderId: number;
  scoreSimilarity: number;
  scoreDistance: number;
  scoreRecency: number;
  scoreTotal: number;
  rank: number;
  status: string;
}

export interface LostPetMatchApproveRequest {
  sessionId: number;
  sightingId: number;
}

export interface LostPetMatchCandidateResponse {
  sessionId: number;
  sightingId: number;
  finderId: number;
  scoreSimilarity: number;
  scoreDistance: number;
  scoreRecency: number;
  scoreTotal: number;
  rank: number;
  status: string;
}

export interface LostPetMatchResponse {
  matchId: number;
  status: string;
  chatRoomId: number;
}

export interface SightingCreateRequest {
  photoUrl: string;
  foundAt: string;
  foundLocation: string;
  memo?: string;
}

export interface SightingResponse {
  sightingId: number;
  status: string;
  foundAt: string;
}

// --- API Functions ---

export async function getLostPets(
  params?: PaginationParams & { status?: string },
): Promise<SliceResponse<LostPetSummaryResponse>> {
  const query = new URLSearchParams();
  if (params?.status) query.set('status', params.status);
  if (params?.page !== undefined) query.set('page', String(params.page));
  if (params?.size !== undefined) query.set('size', String(params.size));
  if (params?.sort) query.set('sort', params.sort);
  const qs = query.toString();
  return apiClient.get<SliceResponse<LostPetSummaryResponse>>(
    `/lost-pets${qs ? `?${qs}` : ''}`,
  );
}

export async function createLostPet(
  data: LostPetCreateRequest,
): Promise<LostPetResponse> {
  return apiClient.post<LostPetResponse>('/lost-pets', data);
}

export async function getLostPet(
  lostPetId: number,
): Promise<LostPetDetailResponse> {
  return apiClient.get<LostPetDetailResponse>(`/lost-pets/${lostPetId}`);
}

export async function analyzeLostPet(
  data: LostPetAnalyzeRequest,
): Promise<LostPetAnalyzeResponse> {
  return apiClient.post<LostPetAnalyzeResponse>('/lost-pets/analyze', data);
}

export async function getMatches(
  lostPetId: number,
  params: { sessionId: number } & PaginationParams,
): Promise<SliceResponse<LostPetMatchCandidateResponse>> {
  const query = new URLSearchParams();
  query.set('sessionId', String(params.sessionId));
  if (params.page !== undefined) query.set('page', String(params.page));
  if (params.size !== undefined) query.set('size', String(params.size));
  if (params.sort) query.set('sort', params.sort);
  return apiClient.get<SliceResponse<LostPetMatchCandidateResponse>>(
    `/lost-pets/${lostPetId}/match?${query.toString()}`,
  );
}

export async function approveMatch(
  lostPetId: number,
  data: LostPetMatchApproveRequest,
): Promise<LostPetMatchResponse> {
  return apiClient.post<LostPetMatchResponse>(
    `/lost-pets/${lostPetId}/match`,
    data,
  );
}

export async function createSighting(
  data: SightingCreateRequest,
): Promise<SightingResponse> {
  return apiClient.post<SightingResponse>('/sightings', data);
}
