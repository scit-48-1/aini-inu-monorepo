'use client';

import { useEffect } from 'react';

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error('[Route Error: login]', error);
  }, [error]);

  return (
    <div className="flex flex-col items-center justify-center h-full gap-4 p-8">
      <h2 className="text-xl font-semibold text-[#1A1A2E]">This page encountered an error</h2>
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
  );
}
