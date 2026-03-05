'use client';

import { useEffect, useState } from 'react';

export function MSWProvider({ children }: { children: React.ReactNode }) {
  const [mswReady, setMswReady] = useState(false);

  useEffect(() => {
    const initMsw = async () => {
      // 브라우저 환경에서만 실행 (서버 환경 제외), NEXT_PUBLIC_ENABLE_MSW=false 이면 MSW 비활성화
      if (typeof window !== 'undefined' && process.env.NODE_ENV === 'development' && process.env.NEXT_PUBLIC_ENABLE_MSW !== 'false') {
        const { worker } = await import('./browser');
        await worker.start({
          onUnhandledRequest: 'bypass',
        });
        // Service Worker가 등록(registered)을 넘어 실제로 현재 페이지를
        // controlling하는 상태가 될 때까지 기다림. 이 시점 전에 fetch가
        // 실행되면 MSW가 인터셉트하지 못하고 Next.js 서버가 HTML을 반환함.
        await new Promise<void>(resolve => {
          if (navigator.serviceWorker.controller) {
            resolve();
          } else {
            navigator.serviceWorker.addEventListener('controllerchange', () => resolve(), { once: true });
          }
        });
        setMswReady(true);
      } else {
        setMswReady(true);
      }
    };

    if (!mswReady) {
      initMsw();
    }
  }, [mswReady]);

  if (!mswReady) {
    return null; // MSW가 준비될 때까지 렌더링 방지
  }

  return <>{children}</>;
}
