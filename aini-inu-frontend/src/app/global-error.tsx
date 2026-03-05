'use client';

import { useEffect } from 'react';

export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error('[Global Error]', error);
  }, [error]);

  return (
    <html lang="ko">
      <body style={{ margin: 0 }}>
        <div
          className="flex flex-col items-center justify-center min-h-screen gap-6 p-8 bg-[#FDFCF8]"
        >
          <h1 className="text-2xl font-bold text-[#1A1A2E]">Something went wrong</h1>
          <p className="text-sm text-zinc-500 text-center max-w-md">
            {error.message || 'An unexpected error occurred'}
          </p>
          <button
            onClick={reset}
            className="px-6 py-3 bg-amber-400 text-[#1A1A2E] font-semibold rounded-2xl hover:bg-amber-300 transition-colors"
          >
            Try again
          </button>
        </div>
      </body>
    </html>
  );
}
