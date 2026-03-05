import { apiClient } from './apiClient';
import { UserType, DogType } from '@/types';

export const memberService = {
  getMe: () => apiClient.get<UserType>('/members/me'),
  getMemberProfile: (id: string) => apiClient.get<UserType>(`/members/${id}`),
  updateMe: (data: Partial<UserType>) => apiClient.put<UserType>('/members/me', data),
  
  getMyDogs: () => apiClient.get<DogType[]>('/members/me/dogs'),
  getMemberDogs: (id: string) => apiClient.get<DogType[]>(`/members/${id}/dogs`),
  registerDog: (data: Partial<DogType>) => apiClient.post<DogType>('/members/me/dogs', data),
  updateDog: (id: string | number, data: Partial<DogType>) => apiClient.put<DogType>(`/members/me/dogs/${id}`, data),
  deleteDog: (id: string | number) => apiClient.delete(`/members/me/dogs/${id}`),

  searchMembers: (q: string) => apiClient.get<UserType[]>(`/members?q=${encodeURIComponent(q)}`),
  getFollowers: () => apiClient.get<UserType[]>('/members/me/followers'),
  getFollowing: () => apiClient.get<UserType[]>('/members/me/following'),
  follow: (targetId: string) => apiClient.post<{ isFollowing: boolean }>(`/members/me/follow/${targetId}`),
  unfollow: (targetId: string) => apiClient.delete<{ isFollowing: boolean }>(`/members/me/follow/${targetId}`),
  submitReview: (partnerId: string, review: any) => apiClient.post(`/members/${partnerId}/reviews`, review),
  getWalkStats: () => apiClient.get<number[]>('/members/me/stats/walk'),
};
