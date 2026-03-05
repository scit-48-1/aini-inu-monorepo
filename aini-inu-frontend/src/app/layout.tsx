'use client';

import { Inter } from "next/font/google";
import "./globals.css";
import Sidebar from "@/components/common/Sidebar";
import { cn } from "@/lib/utils";
import { usePathname } from "next/navigation";
import { ThemeProvider } from "@/components/common/ThemeProvider";
import { Toaster } from 'sonner';
import { AuthProvider } from "@/providers/AuthProvider";

const inter = Inter({ subsets: ["latin"] });

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const pathname = usePathname();

  const noSidebarPaths = ['/', '/login', '/signup'];
  const showSidebar = !noSidebarPaths.includes(pathname);

  return (
    <html lang="ko" suppressHydrationWarning>
      <body className={cn(inter.className, "bg-background text-foreground transition-colors duration-300")}>
        <ThemeProvider attribute="data-theme" defaultTheme="light" enableSystem>
          <AuthProvider>
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
          </AuthProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}
