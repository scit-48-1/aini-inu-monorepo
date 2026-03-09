import { apiClient, type ApiRequestOptions } from './client';
import { SliceResponse, CursorResponse, PaginationParams, CursorPaginationParams } from './types';

// --- Types ---

export interface ChatRoomSummaryResponse {
  chatRoomId: number;
  chatType: string;
  status: string;
  origin: string;
  roomTitle: string | null;
  displayName: string;
  participantProfileImages: string[];
  lastMessage: ChatMessageResponse | null;
  unreadCount: number;
  updatedAt: string;
}

export interface ChatRoomDetailResponse {
  chatRoomId: number;
  chatType: string;
  status: string;
  origin: string;
  threadId: number | null;
  roomTitle: string | null;
  walkConfirmed: boolean;
  participants: ChatParticipantResponse[];
  lastMessage: ChatMessageResponse | null;
}

export interface ChatParticipantResponse {
  memberId: number;
  nickname: string | null;
  profileImageUrl: string | null;
  walkConfirmState: string;
  left: boolean;
  pets: ChatParticipantPetResponse[];
}

export interface ChatParticipantPetResponse {
  petId: number;
  name: string;
}

export interface ChatRoomDirectCreateRequest {
  partnerId: number;
  origin?: string;
  roomTitle?: string;
}

export interface ChatMessageCreateRequest {
  content: string;
  messageType: string;
  clientMessageId: string;
}

export interface ChatMessageResponse {
  id: number;
  roomId: number;
  sender: ChatSenderResponse;
  content: string;
  messageType: string;
  status: string;
  clientMessageId: string;
  sentAt: string;
}

export interface ChatSenderResponse {
  memberId: number;
}

export interface MessageReadRequest {
  messageId: number;
  readAt: string;
}

export interface MessageReadResponse {
  roomId: number;
  memberId: number;
  lastReadMessageId: number;
  updatedAt: string;
}

export interface ChatReviewCreateRequest {
  revieweeId: number;
  score: number;
  comment: string;
}

export interface ChatReviewResponse {
  id: number;
  chatRoomId: number;
  reviewerId: number;
  revieweeId: number;
  score: number;
  comment: string;
  createdAt: string;
}

export interface MyChatReviewResponse {
  exists: boolean;
  review: ChatReviewResponse | null;
}

export interface LeaveRoomResponse {
  roomId: number;
  left: boolean;
  roomStatus: string;
}

export interface WalkConfirmRequest {
  action: string;
}

export interface WalkConfirmResponse {
  roomId: number;
  memberId: number;
  myState: string;
  allConfirmed: boolean;
  confirmedMemberIds: number[];
}

// --- API Functions ---

export async function getRooms(
  params?: PaginationParams & { status?: string; origin?: string },
): Promise<SliceResponse<ChatRoomSummaryResponse>> {
  const query = new URLSearchParams();
  if (params?.status) query.set('status', params.status);
  if (params?.origin) query.set('origin', params.origin);
  if (params?.page !== undefined) query.set('page', String(params.page));
  if (params?.size !== undefined) query.set('size', String(params.size));
  if (params?.sort) query.set('sort', params.sort);
  const qs = query.toString();
  return apiClient.get<SliceResponse<ChatRoomSummaryResponse>>(
    `/chat-rooms${qs ? `?${qs}` : ''}`,
  );
}

export async function createDirectRoom(
  data: ChatRoomDirectCreateRequest,
  options?: ApiRequestOptions,
): Promise<ChatRoomDetailResponse> {
  return apiClient.post<ChatRoomDetailResponse>('/chat-rooms/direct', data, options);
}

export async function getRoom(
  chatRoomId: number,
): Promise<ChatRoomDetailResponse> {
  return apiClient.get<ChatRoomDetailResponse>(`/chat-rooms/${chatRoomId}`);
}

export async function leaveRoom(
  chatRoomId: number,
): Promise<LeaveRoomResponse> {
  return apiClient.post<LeaveRoomResponse>(`/chat-rooms/${chatRoomId}/leave`);
}

export async function getMessages(
  chatRoomId: number,
  params?: CursorPaginationParams,
): Promise<CursorResponse<ChatMessageResponse>> {
  const query = new URLSearchParams();
  if (params?.cursor) query.set('cursor', params.cursor);
  if (params?.size !== undefined) query.set('size', String(params.size));
  if (params?.direction) query.set('direction', params.direction);
  const qs = query.toString();
  return apiClient.get<CursorResponse<ChatMessageResponse>>(
    `/chat-rooms/${chatRoomId}/messages${qs ? `?${qs}` : ''}`,
  );
}

export async function sendMessage(
  chatRoomId: number,
  data: ChatMessageCreateRequest,
): Promise<ChatMessageResponse> {
  return apiClient.post<ChatMessageResponse>(
    `/chat-rooms/${chatRoomId}/messages`,
    data,
  );
}

export async function markMessagesRead(
  chatRoomId: number,
  data: MessageReadRequest,
): Promise<MessageReadResponse> {
  return apiClient.post<MessageReadResponse>(
    `/chat-rooms/${chatRoomId}/messages/read`,
    data,
  );
}

export async function getReviews(
  chatRoomId: number,
  params?: PaginationParams,
): Promise<SliceResponse<ChatReviewResponse>> {
  const query = new URLSearchParams();
  if (params?.page !== undefined) query.set('page', String(params.page));
  if (params?.size !== undefined) query.set('size', String(params.size));
  if (params?.sort) query.set('sort', params.sort);
  const qs = query.toString();
  return apiClient.get<SliceResponse<ChatReviewResponse>>(
    `/chat-rooms/${chatRoomId}/reviews${qs ? `?${qs}` : ''}`,
  );
}

export async function createReview(
  chatRoomId: number,
  data: ChatReviewCreateRequest,
): Promise<ChatReviewResponse> {
  return apiClient.post<ChatReviewResponse>(
    `/chat-rooms/${chatRoomId}/reviews`,
    data,
  );
}

export async function getMyReview(
  chatRoomId: number,
): Promise<MyChatReviewResponse> {
  return apiClient.get<MyChatReviewResponse>(
    `/chat-rooms/${chatRoomId}/reviews/me`,
  );
}

export async function getWalkConfirm(
  chatRoomId: number,
): Promise<WalkConfirmResponse> {
  return apiClient.get<WalkConfirmResponse>(
    `/chat-rooms/${chatRoomId}/walk-confirm`,
  );
}

export async function confirmWalk(
  chatRoomId: number,
  data: WalkConfirmRequest,
): Promise<WalkConfirmResponse> {
  return apiClient.post<WalkConfirmResponse>(
    `/chat-rooms/${chatRoomId}/walk-confirm`,
    data,
  );
}

export async function cancelWalkConfirm(
  chatRoomId: number,
): Promise<void> {
  return apiClient.delete<void>(`/chat-rooms/${chatRoomId}/walk-confirm`);
}
