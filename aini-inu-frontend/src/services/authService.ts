import { apiClient } from './api/apiClient';

/**
 * 소셜 로그인 및 인증 관련 서비스
 * apiClient를 사용하여 중앙 집중형 통신 및 에러 처리를 수행합니다.
 */

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  isNewMember: boolean;
  memberId: number;
}

export const authService = {
  /**
   * 이메일/비밀번호 로그인을 처리합니다.
   */
  login: async (email: string, password: string) => {
    return await apiClient.post<any>('/auth/login', { email, password });
  },

  /**
   * 신규 회원의 가입 정보를 백엔드에 전달합니다.
   */
  signup: async (signupData: any) => {
    return await apiClient.post('/members/signup', signupData);
  },

  /**
   * 이메일로 인증 코드를 전송합니다.
   */
  sendVerificationCode: async (email: string) => {
    return await apiClient.post('/auth/email/send', { email });
  },

  /**
   * 이메일 인증 코드 확인
   */
  verifyCode: async (email: string, code: string) => {
    return await apiClient.post('/auth/email/verify', { email, code });
  },
};
