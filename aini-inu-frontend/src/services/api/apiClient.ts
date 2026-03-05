// apiClient.ts: 현업 표준 방식의 API 통신 모듈
export const API_BASE_URL = '/api/v1';

async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 8000);

  // endpoint가 '/'로 시작하지 않으면 자동으로 붙여줌, 베이스와 결합
  const cleanEndpoint = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
  const fullUrl = `${API_BASE_URL}${cleanEndpoint}`;

  try {
    const response = await fetch(fullUrl, {
      ...options,
      signal: controller.signal,
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
    });

    clearTimeout(timeoutId);

    if (response.status === 204) return {} as T;

    const contentType = response.headers.get('content-type');
    if (!contentType || !contentType.includes('application/json')) {
      const text = await response.text();
      console.error(`[Non-JSON Response] ${fullUrl}:`, text.substring(0, 100));
      throw new Error('서버 응답 규격이 올바르지 않습니다.');
    }

    const result = await response.json();

    if (!response.ok || !result.success) {
      throw new Error(result.message || `요청 실패 (${response.status})`);
    }

    return result.data as T;
  } catch (error: any) {
    if (error.name === 'AbortError') throw new Error('요청 시간이 초과되었습니다.');
    console.error(`[API Error] ${options.method || 'GET'} ${fullUrl}:`, error);
    throw error;
  }
}

export const apiClient = {
  get: <T>(endpoint: string, options?: RequestInit) => request<T>(endpoint, { ...options, method: 'GET' }),
  post: <T>(endpoint: string, body?: unknown, options?: RequestInit) => 
    request<T>(endpoint, { ...options, method: 'POST', body: body ? JSON.stringify(body) : undefined }),
  put: <T>(endpoint: string, body?: unknown, options?: RequestInit) => 
    request<T>(endpoint, { ...options, method: 'PUT', body: body ? JSON.stringify(body) : undefined }),
  delete: <T>(endpoint: string, options?: RequestInit) => request<T>(endpoint, { ...options, method: 'DELETE' }),
};
