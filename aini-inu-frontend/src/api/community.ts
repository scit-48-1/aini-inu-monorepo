import { apiClient } from './client';
import { SliceResponse, PaginationParams } from './types';

// --- Types ---

export interface Author {
  id: number;
  nickname: string;
  profileImageUrl: string;
}

export interface PostCreateRequest {
  content: string;
  caption?: string;
  imageUrls: string[];
}

export interface PostUpdateRequest {
  content: string;
  caption?: string;
  imageUrls?: string[];
}

export interface PostResponse {
  id: number;
  author: Author;
  content: string;
  imageUrls: string[];
  likeCount: number;
  commentCount: number;
  createdAt: string;
  liked: boolean;
}

export interface PostDetailResponse {
  id: number;
  author: Author;
  content: string;
  imageUrls: string[];
  likeCount: number;
  commentCount: number;
  createdAt: string;
  comments: CommentResponse[];
  liked: boolean;
}

export interface PostLikeResponse {
  likeCount: number;
  liked: boolean;
}

export interface CommentCreateRequest {
  content: string;
}

export interface CommentResponse {
  id: number;
  author: Author;
  content: string;
  createdAt: string;
}

export interface StoryDiaryItemResponse {
  diaryId: number;
  threadId: number;
  title: string;
  content: string;
  photoUrls: string[];
  walkDate: string;
  createdAt: string;
  thread?: import('./diaries').DiaryThreadSummary;
}

export interface StoryGroupResponse {
  memberId: number;
  nickname: string;
  profileImageUrl: string;
  coverImageUrl: string;
  latestCreatedAt: string;
  diaries: StoryDiaryItemResponse[];
}

// --- API Functions ---

export async function getPosts(
  params?: PaginationParams,
): Promise<SliceResponse<PostResponse>> {
  const query = new URLSearchParams();
  if (params?.page !== undefined) query.set('page', String(params.page));
  if (params?.size !== undefined) query.set('size', String(params.size));
  if (params?.sort) query.set('sort', params.sort);
  const qs = query.toString();
  return apiClient.get<SliceResponse<PostResponse>>(
    `/posts${qs ? `?${qs}` : ''}`,
  );
}

export async function getPostsByAuthor(
  authorId: number,
  params?: PaginationParams,
): Promise<SliceResponse<PostResponse>> {
  const query = new URLSearchParams();
  query.set('authorId', String(authorId));
  if (params?.page !== undefined) query.set('page', String(params.page));
  if (params?.size !== undefined) query.set('size', String(params.size));
  if (params?.sort) query.set('sort', params.sort);
  return apiClient.get<SliceResponse<PostResponse>>(`/posts?${query.toString()}`);
}

export async function createPost(
  data: PostCreateRequest,
): Promise<PostResponse> {
  return apiClient.post<PostResponse>('/posts', data);
}

export async function getPost(postId: number): Promise<PostDetailResponse> {
  return apiClient.get<PostDetailResponse>(`/posts/${postId}`);
}

export async function updatePost(
  postId: number,
  data: PostUpdateRequest,
): Promise<PostResponse> {
  return apiClient.patch<PostResponse>(`/posts/${postId}`, data);
}

export async function deletePost(postId: number): Promise<void> {
  return apiClient.delete<void>(`/posts/${postId}`);
}

export async function getComments(
  postId: number,
  params?: PaginationParams,
): Promise<SliceResponse<CommentResponse>> {
  const query = new URLSearchParams();
  if (params?.page !== undefined) query.set('page', String(params.page));
  if (params?.size !== undefined) query.set('size', String(params.size));
  if (params?.sort) query.set('sort', params.sort);
  const qs = query.toString();
  return apiClient.get<SliceResponse<CommentResponse>>(
    `/posts/${postId}/comments${qs ? `?${qs}` : ''}`,
  );
}

export async function createComment(
  postId: number,
  data: CommentCreateRequest,
): Promise<CommentResponse> {
  return apiClient.post<CommentResponse>(`/posts/${postId}/comments`, data);
}

export async function deleteComment(
  postId: number,
  commentId: number,
): Promise<void> {
  return apiClient.delete<void>(`/posts/${postId}/comments/${commentId}`);
}

export async function likePost(postId: number): Promise<PostLikeResponse> {
  return apiClient.post<PostLikeResponse>(`/posts/${postId}/like`);
}

export async function getStories(
  params?: PaginationParams,
): Promise<SliceResponse<StoryGroupResponse>> {
  const query = new URLSearchParams();
  if (params?.page !== undefined) query.set('page', String(params.page));
  if (params?.size !== undefined) query.set('size', String(params.size));
  if (params?.sort) query.set('sort', params.sort);
  const qs = query.toString();
  return apiClient.get<SliceResponse<StoryGroupResponse>>(
    `/stories${qs ? `?${qs}` : ''}`,
  );
}
