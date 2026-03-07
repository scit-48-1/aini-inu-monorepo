import { toast } from 'sonner';
import { useAuthStore } from '../store/useAuthStore';
import { ApiError, ApiResponse } from './types';

const API_BASE_URL = '/api/v1';
const REQUEST_TIMEOUT_MS = 8000;

// --- Request options ---

export interface ApiRequestOptions extends Omit<RequestInit, 'body' | 'method'> {
  /** Suppress automatic error toast */
  suppressToast?: boolean;
  /** Skip auth header injection (used internally for refresh call) */
  skipAuth?: boolean;
}

// --- 401 refresh queue ---

let isLoggingOut = false;

export function setLoggingOut(value: boolean) {
  isLoggingOut = value;
}

let isRefreshing = false;
let refreshQueue: Array<{
  resolve: (token: string) => void;
  reject: (error: unknown) => void;
}> = [];

function processQueue(error: unknown, token: string | null) {
  refreshQueue.forEach(({ resolve, reject }) => {
    if (error || !token) {
      reject(error);
    } else {
      resolve(token);
    }
  });
  refreshQueue = [];
}

async function refreshAccessToken(): Promise<string> {
  const { getRefreshToken, setTokens, clearTokens } = useAuthStore.getState();
  const refreshToken = getRefreshToken();

  if (!refreshToken) {
    clearTokens();
    throw new Error('No refresh token');
  }

  try {
    const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    });

    if (!response.ok) {
      throw new Error(`Refresh failed: ${response.status}`);
    }

    const result: ApiResponse<{ accessToken: string; refreshToken: string }> =
      await response.json();

    if (!result.success || !result.data) {
      throw new Error(result.message || 'Token refresh failed');
    }

    setTokens(result.data.accessToken, result.data.refreshToken);
    return result.data.accessToken;
  } catch {
    clearTokens();
    throw new Error('Session expired');
  }
}

// --- Core request function ---

async function request<T>(
  method: string,
  endpoint: string,
  body?: unknown,
  options: ApiRequestOptions = {},
): Promise<T> {
  const { suppressToast = false, skipAuth = false, ...fetchOptions } = options;

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

  const cleanEndpoint = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
  const fullUrl = `${API_BASE_URL}${cleanEndpoint}`;

  // Build headers
  const headers: Record<string, string> = {
    ...((fetchOptions.headers as Record<string, string>) || {}),
  };

  if (!skipAuth) {
    const token = useAuthStore.getState().getAccessToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
  }

  // Set Content-Type for JSON bodies (skip for FormData/binary)
  if (body !== undefined && body !== null && !(body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
  }

  const requestInit: RequestInit = {
    ...fetchOptions,
    method,
    headers,
    signal: controller.signal,
    body:
      body !== undefined && body !== null
        ? body instanceof FormData
          ? body
          : JSON.stringify(body)
        : undefined,
  };

  const retryFn = () => request<T>(method, endpoint, body, options);

  try {
    const response = await fetch(fullUrl, requestInit);
    clearTimeout(timeoutId);

    // Handle 204 No Content
    if (response.status === 204) {
      return undefined as T;
    }

    // Handle 401 — attempt token refresh (skip if this IS the refresh call)
    if (response.status === 401 && !skipAuth) {
      return handle401<T>(method, endpoint, body, options);
    }

    // Validate JSON content type
    const contentType = response.headers.get('content-type');
    if (!contentType || !contentType.includes('application/json')) {
      const text = await response.text();
      console.error(`[Non-JSON Response] ${fullUrl}:`, text.substring(0, 100));
      throw new Error('서버 응답 규격이 올바르지 않습니다.');
    }

    // Parse and unwrap envelope
    const result: ApiResponse<T> = await response.json();

    if (!result.success) {
      throw new ApiError(
        result.errorCode || 'UNKNOWN_ERROR',
        result.message || `요청 실패 (${response.status})`,
        result.status || response.status,
      );
    }

    return result.data as T;
  } catch (error: unknown) {
    clearTimeout(timeoutId);

    // Network / timeout errors
    if (error instanceof TypeError || (error instanceof DOMException && error.name === 'AbortError')) {
      const message =
        error instanceof DOMException
          ? '요청 시간이 초과되었습니다.'
          : '네트워크 오류가 발생했습니다.';

      if (!suppressToast) {
        toast.error(message, {
          duration: 3000,
          action: {
            label: '다시 시도',
            onClick: () => { retryFn(); },
          },
        });
      }

      throw new Error(message);
    }

    // ApiError — show toast with backend message
    if (error instanceof ApiError) {
      if (!suppressToast && !isLoggingOut) {
        toast.error(error.message || '요청에 실패했습니다', { duration: 3000 });
      }
      throw error;
    }

    // Other errors — show generic toast
    if (error instanceof Error) {
      if (!suppressToast && !isLoggingOut) {
        toast.error(error.message || '요청에 실패했습니다', { duration: 3000 });
      }
    }

    throw error;
  }
}

// --- 401 handler with queue ---

async function handle401<T>(
  method: string,
  endpoint: string,
  body: unknown,
  options: ApiRequestOptions,
): Promise<T> {
  if (isRefreshing) {
    // Queue this request and wait for the refresh to complete
    return new Promise<T>((resolve, reject) => {
      refreshQueue.push({
        resolve: () => {
          // Retry with new token
          request<T>(method, endpoint, body, options).then(resolve).catch(reject);
        },
        reject,
      });
    });
  }

  isRefreshing = true;

  try {
    await refreshAccessToken();
    isRefreshing = false;
    processQueue(null, 'refreshed');

    // Retry original request with new token
    return request<T>(method, endpoint, body, options);
  } catch (error) {
    isRefreshing = false;
    processQueue(error, null);

    if (!isLoggingOut) {
      toast.error('세션이 만료되었습니다', { duration: 3000 });

      if (typeof window !== 'undefined') {
        window.location.href = '/login';
      }
    }

    throw error;
  }
}

// --- Exported API client ---

export const apiClient = {
  get: <T>(endpoint: string, options?: ApiRequestOptions) =>
    request<T>('GET', endpoint, undefined, options),

  post: <T>(endpoint: string, body?: unknown, options?: ApiRequestOptions) =>
    request<T>('POST', endpoint, body, options),

  put: <T>(endpoint: string, body?: unknown, options?: ApiRequestOptions) =>
    request<T>('PUT', endpoint, body, options),

  patch: <T>(endpoint: string, body?: unknown, options?: ApiRequestOptions) =>
    request<T>('PATCH', endpoint, body, options),

  delete: <T>(endpoint: string, options?: ApiRequestOptions) =>
    request<T>('DELETE', endpoint, undefined, options),
};
