import { apiClient } from './client';
import type { PaginationParams, SliceResponse } from './types';

// --- Types ---

export type TimelineEventType =
  | 'WALK_THREAD_CREATED'
  | 'WALKING_SESSION_STARTED'
  | 'WALKING_SESSION_COMPLETED'
  | 'POST_CREATED'
  | 'LOST_PET_REPORT_CREATED'
  | 'SIGHTING_CREATED'
  | 'WALK_DIARY_CREATED';

export interface TimelineEventResponse {
  id: number;
  eventType: TimelineEventType;
  referenceId: number;
  title: string | null;
  summary: string | null;
  thumbnailUrl: string | null;
  occurredAt: string;
}

export interface TimelineSettingsResponse {
  isTimelinePublic: boolean;
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

export async function getTimeline(
  memberId: number,
  params?: PaginationParams,
): Promise<SliceResponse<TimelineEventResponse>> {
  const query = buildQuery({ ...params });
  return apiClient.get<SliceResponse<TimelineEventResponse>>(
    `/members/${memberId}/timeline${query}`,
  );
}

export async function updateTimelineSettings(
  isTimelinePublic: boolean,
): Promise<TimelineSettingsResponse> {
  return apiClient.patch<TimelineSettingsResponse>(
    '/members/me/timeline/settings',
    { isTimelinePublic },
  );
}
