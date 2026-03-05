// ApiResponse envelope — matches backend common/response/ApiResponse<T>
export interface ApiResponse<T> {
  success: boolean;
  status: number;
  data: T | null;
  errorCode: string | null;
  message: string | null;
}

// Custom error class carrying backend error details
export class ApiError extends Error {
  constructor(
    public errorCode: string,
    message: string,
    public status: number,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

// Pagination: offset-based (used by most list endpoints)
export interface SliceResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  first: boolean;
  last: boolean;
  hasNext: boolean;
}

// Pagination: cursor-based (used by chat messages)
export interface CursorResponse<T> {
  content: T[];
  nextCursor: string | null;
  hasMore: boolean;
}

// Common pagination query params for offset-based endpoints
export interface PaginationParams {
  page?: number;
  size?: number;
  sort?: string;
}

// Pagination query params for cursor-based endpoints
export interface CursorPaginationParams {
  cursor?: string;
  size?: number;
  direction?: string;
}

// --- INFRA-07: UI async state types ---

/** UI async state — 5 states per PRD section 10.2. Components built in domain phases. */
export type AsyncState = 'idle' | 'loading' | 'empty' | 'error' | 'success';

export interface AsyncData<T> {
  state: AsyncState;
  data: T | null;
  error: ApiError | null;
}
