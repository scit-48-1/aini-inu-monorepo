'use client';

import { Inter } from "next/font/google";
import "./globals.css";
import Sidebar from "@/components/common/Sidebar";
import { cn } from "@/lib/utils";
import { usePathname, useRouter } from "next/navigation";
import { ThemeProvider } from "@/components/common/ThemeProvider";
import { MSWProvider } from "@/mocks/MSWProvider";
import { Toaster } from 'sonner';
import { useEffect } from "react";

const inter = Inter({ subsets: ["latin"] });

const PROTECTED_PATHS = ['/dashboard', '/feed', '/chat', '/around-me', '/settings', '/profile'];
const DB_KEY = 'aini_inu_v6_db';

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const pathname = usePathname();
  const router = useRouter();

  const noSidebarPaths = ['/', '/login', '/signup'];
  const showSidebar = !noSidebarPaths.includes(pathname);

  useEffect(() => {
    const isProtected = PROTECTED_PATHS.some(p => pathname.startsWith(p));
    if (!isProtected) return;

    try {
      const raw = localStorage.getItem(DB_KEY);
      if (!raw) { router.replace('/login'); return; }
      const db = JSON.parse(raw);
      if (!db.currentUserId) router.replace('/login');
    } catch {
      router.replace('/login');
    }
  }, [pathname, router]);

  return (
    <html lang="ko" suppressHydrationWarning>
      <body className={cn(inter.className, "bg-background text-foreground transition-colors duration-300")}>
        <MSWProvider>
          <ThemeProvider attribute="data-theme" defaultTheme="light" enableSystem>
            <div className="flex h-screen w-screen overflow-hidden bg-background">
              {showSidebar && <Sidebar />}
              <main className={cn(
                "flex-1 h-full relative overflow-y-auto no-scrollbar",
                !showSidebar && "w-full"
              )}>
                {children}
              </main>
            </div>
            <Toaster 
              position="top-center" 
              richColors 
              closeButton 
              toastOptions={{
                style: {
                  borderRadius: '24px',
                  padding: '16px 24px',
                  fontSize: '14px',
                  fontWeight: 'bold',
                  fontFamily: 'inherit',
                  boxShadow: '0 20px 40px rgba(0,0,0,0.1)',
                  border: '1px solid rgba(0,0,0,0.05)'
                }
              }}
            />
          </ThemeProvider>
        </MSWProvider>
      </body>
    </html>
  );
}