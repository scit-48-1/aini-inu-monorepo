import { apiClient } from './client';
import { ApiResponse } from './types';

// --- Types ---

export interface PresignedImageRequest {
  purpose: string;
  fileName: string;
  contentType: string;
}

export interface PresignedImageResponse {
  uploadUrl: string;
  imageUrl: string;
  expiresIn: number;
  maxFileSizeBytes: number;
}

// --- API Functions ---

const API_BASE_URL = '/api/v1';

export async function getPresignedUrl(
  data: PresignedImageRequest,
): Promise<PresignedImageResponse> {
  return apiClient.post<PresignedImageResponse>(
    '/images/presigned-url',
    data,
  );
}

export async function uploadToPresignedUrl(
  token: string,
  file: File | Blob,
): Promise<void> {
  const response = await fetch(
    `${API_BASE_URL}/images/presigned-upload/${token}`,
    {
      method: 'PUT',
      headers: {
        'Content-Type': file.type || 'application/octet-stream',
      },
      body: file,
    },
  );

  if (!response.ok) {
    // Try to parse as ApiResponse envelope for error details
    const contentType = response.headers.get('content-type');
    if (contentType?.includes('application/json')) {
      const result: ApiResponse<void> = await response.json();
      throw new Error(result.message || `Upload failed: ${response.status}`);
    }
    throw new Error(`Upload failed: ${response.status}`);
  }
}

export function getImageUrl(key: string): string {
  return `${API_BASE_URL}/images/local?key=${encodeURIComponent(key)}`;
}

export async function uploadImageFlow(
  file: File,
  purpose: string,
): Promise<string> {
  // Step 1: Get presigned URL
  const presigned = await getPresignedUrl({
    purpose,
    fileName: file.name,
    contentType: file.type || 'application/octet-stream',
  });

  // Step 2: Extract token from uploadUrl
  const url = new URL(presigned.uploadUrl, window.location.origin);
  const pathSegments = url.pathname.split('/');
  const token = pathSegments[pathSegments.length - 1];

  if (!token) {
    throw new Error('Failed to extract upload token from presigned URL');
  }

  // Step 3: Upload file to presigned URL
  await uploadToPresignedUrl(token, file);

  // Step 4: Return the image URL for storage
  return presigned.imageUrl;
}
