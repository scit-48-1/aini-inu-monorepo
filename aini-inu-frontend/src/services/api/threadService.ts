import { apiClient } from './apiClient';
import { ThreadType, WalkDiaryType } from '@/types';

export const threadService = {
  /**
   * 주변 탐색 스레드 목록을 가져옵니다.
   */
  getThreads: (lat?: number, lng?: number) => {
    let url = '/threads';
    if (lat !== undefined && lng !== undefined) {
      url += `?lat=${lat}&lng=${lng}`;
    }
    return apiClient.get<ThreadType[]>(url);
  },

  /**
   * 내가 팔로우하는 사람들의 공개 산책 일기 목록을 가져옵니다.
   */
  getFollowingDiaries: () =>
    apiClient.get<Record<string, WalkDiaryType>>('/walk-diaries/following'),

  /**
   * 특정 멤버의 산책 일기 목록을 가져옵니다.
   */
  getWalkDiaries: (memberId?: string) =>
    apiClient.get<Record<string, WalkDiaryType>>(`/walk-diaries${memberId ? `?memberId=${memberId}` : ''}`),

  /**
   * 산책 일기를 저장합니다.
   */
  saveWalkDiary: (id: string | number, data: Partial<WalkDiaryType>) =>
    apiClient.post(`/walk-diaries/${id}`, data),

  /**
   * 새로운 산책 모집글을 생성합니다.
   */
  createThread: (data: Partial<ThreadType>) =>
    apiClient.post<ThreadType>('/threads', data),

  /**
   * 산책 모집글에 참여합니다.
   */
  joinThread: (id: string | number) =>
    apiClient.post(`/threads/${id}/join`),

  /**
   * 산책 모집글 참여를 취소합니다.
   */
  unjoinThread: (id: string | number) =>
    apiClient.delete(`/threads/${id}/join`),

  /**
   * 핫스팟 정보를 가져옵니다.
   */
  getHotspots: (hours: number = 3) =>
    apiClient.get<{ region: string; count: number }>(`/threads/hotspot?hours=${hours}`),

  /**
   * 산책 모집글을 수정합니다. (작성자 본인만 가능)
   */
  updateThread: (id: string | number, data: Partial<ThreadType>) =>
    apiClient.put<ThreadType>(`/threads/${id}`, data),

  /**
   * 산책 모집글을 삭제합니다. (작성자 본인만 가능)
   */
  deleteThread: (id: string | number) =>
    apiClient.delete(`/threads/${id}`),
};
