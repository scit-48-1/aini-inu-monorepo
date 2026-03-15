import { apiClient } from './client';
import type { ActivityStatsResponse } from './members';
import type { PetResponse } from './pets';
import type { ThreadHotspotResponse, ThreadSummaryResponse } from './threads';

export interface DashboardRecentFriendResponse {
  memberId: number;
  chatRoomId: number;
  displayName: string;
  profileImageUrl: string | null;
  score: number;
}

export interface DashboardPendingReviewResponse {
  chatRoomId: number;
  displayName: string;
  partnerId: number;
  partnerNickname: string;
  profileImageUrl: string | null;
}

export interface DashboardSummaryResponse {
  activityStats: ActivityStatsResponse;
  hotspots: ThreadHotspotResponse[];
  threads: ThreadSummaryResponse[];
  myPets: PetResponse[];
  recentFriends: DashboardRecentFriendResponse[];
  pendingReviews: DashboardPendingReviewResponse[];
}

export interface DashboardSummaryParams {
  latitude?: number;
  longitude?: number;
  radius?: number;
}

function buildQuery(params: DashboardSummaryParams): string {
  const query = new URLSearchParams();
  if (params.latitude !== undefined) query.set('latitude', String(params.latitude));
  if (params.longitude !== undefined) query.set('longitude', String(params.longitude));
  if (params.radius !== undefined) query.set('radius', String(params.radius));
  const qs = query.toString();
  return qs ? `?${qs}` : '';
}

export async function getDashboardSummary(params: DashboardSummaryParams): Promise<DashboardSummaryResponse> {
  return apiClient.get<DashboardSummaryResponse>(`/dashboard/summary${buildQuery(params)}`);
}
