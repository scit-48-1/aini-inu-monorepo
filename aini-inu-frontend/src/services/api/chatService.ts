import { apiClient } from './apiClient';
import { ChatRoom, ChatMessage } from '@/types';

// ChatRoomType: ChatRoom의 alias — 기존 컴포넌트 호환용 re-export
export type ChatRoomType = ChatRoom;

export const chatService = {
  /**
   * 모든 채팅방 목록을 가져옵니다.
   */
  getRooms: () => apiClient.get<ChatRoom[]>('/chat/rooms'),

  /**
   * 특정 이웃과의 채팅방이 이미 존재하는지 확인하고, 없으면 생성합니다.
   * @param partnerId 대화 상대방의 ID
   */
  getOrCreateRoom: (partnerId: string) => 
    apiClient.post<ChatRoom>('/chat/rooms', { partnerId }),

  /**
   * 특정 채팅방의 메시지 내역을 가져옵니다.
   */
  getMessages: (roomId: string) => 
    apiClient.get<ChatMessage[]>(`/chat/rooms/${roomId}/messages`),

  /**
   * 메시지를 전송합니다.
   */
  sendMessage: (roomId: string, content: string) => 
    apiClient.post<ChatMessage>(`/chat/rooms/${roomId}/messages`, { content }),
};
