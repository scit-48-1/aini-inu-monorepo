import { apiClient } from './client';

// --- Request / Response types ---

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  memberId: number;
  newMember: boolean;
  isNewMember: boolean;
}

export interface TokenRefreshRequest {
  refreshToken: string;
}

export interface TokenRevokeRequest {
  refreshToken: string;
}

// --- Functions ---

export async function login(data: LoginRequest): Promise<LoginResponse> {
  return apiClient.post<LoginResponse>('/auth/login', data, { skipAuth: true });
}

export async function logout(data: TokenRevokeRequest): Promise<void> {
  return apiClient.post<void>('/auth/logout', data, { skipAuth: true });
}

export async function refreshToken(data: TokenRefreshRequest): Promise<LoginResponse> {
  return apiClient.post<LoginResponse>('/auth/refresh', data, {
    skipAuth: true,
    suppressToast: true,
  });
}

export async function getTestToken(memberId: number): Promise<Record<string, string>> {
  return apiClient.post<Record<string, string>>(`/test/auth/token?memberId=${memberId}`);
}
