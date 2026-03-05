import { apiClient } from './apiClient';
import { FeedPostType } from '@/types';

export interface CommentType {
  id: string;
  author: {
    id: string;
    nickname: string;
    avatar: string;
  };
  content: string;
  createdAt: string;
}

export const postService = {
  getPosts: (memberId?: string, location?: string) => {
    let url = '/posts?';
    if (memberId) url += `memberId=${memberId}&`;
    if (location) url += `location=${encodeURIComponent(location)}&`;
    return apiClient.get<FeedPostType[]>(url);
  },
  createPost: (data: Partial<FeedPostType>) => apiClient.post<FeedPostType>('/posts', data),
  updatePost: (id: string | number, data: Partial<FeedPostType>) => apiClient.put<FeedPostType>(`/posts/${id}`, data),
  deletePost: (id: string | number) => apiClient.delete(`/posts/${id}`),

  // --- Interaction ---
  likePost: (id: string | number) => apiClient.post<{ likes: number; isLiked: boolean }>(`/posts/${id}/like`),
  addComment: (id: string | number, content: string) => apiClient.post<CommentType>(`/posts/${id}/comments`, { content }),
  getComments: (id: string | number) => apiClient.get<CommentType[]>(`/posts/${id}/comments`),
  getStories: () => apiClient.get<any[]>('/stories'),
};
