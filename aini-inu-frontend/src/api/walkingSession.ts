import { apiClient } from './client';

// --- Types ---

export interface WalkingSessionResponse {
  id: number;
  memberId: number;
  status: 'ACTIVE' | 'ENDED';
  startedAt: string;
  lastHeartbeatAt: string;
}

export interface WalkingUserResponse {
  memberId: number;
  nickname: string;
  profileImageUrl: string | null;
  mannerTemperature: number;
  walkingStartedAt: string;
}

// --- Functions ---

export async function getActiveWalkers(): Promise<WalkingUserResponse[]> {
  return apiClient.get<WalkingUserResponse[]>('/walking-sessions/active');
}

export async function startWalking(): Promise<WalkingSessionResponse> {
  return apiClient.post<WalkingSessionResponse>('/walking-sessions/start');
}

export async function sendHeartbeat(): Promise<void> {
  return apiClient.put<void>('/walking-sessions/heartbeat');
}

export async function stopWalking(): Promise<void> {
  return apiClient.post<void>('/walking-sessions/stop');
}

export async function getMyWalkingSession(): Promise<WalkingSessionResponse | null> {
  return apiClient.get<WalkingSessionResponse | null>('/walking-sessions/my');
}
