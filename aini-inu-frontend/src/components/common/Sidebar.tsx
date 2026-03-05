'use client';

import React, { useEffect, useState } from 'react';
import { MapPin, Rss, Settings, Plus, LogOut, MessageSquare, User } from 'lucide-react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { cn } from '@/lib/utils';
import { Typography } from '@/components/ui/Typography';
import { useTheme } from 'next-themes';
import { CreatePostModal } from './CreatePostModal';
import { useProfile } from '@/hooks/useProfile';

const Sidebar: React.FC = () => {
  const pathname = usePathname();
  const { theme } = useTheme();
  const { profile: userProfile } = useProfile();
  const [mounted, setMounted] = useState(false);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  const menuItems = [
    { icon: MapPin, path: '/around-me', label: '동네 탐색' },
    { icon: MessageSquare, path: '/chat', label: '채팅' },
    { icon: Rss, path: '/feed', label: '피드' },
    { icon: User, path: '/profile', label: '프로필' },
  ];

  const isDarkMode = theme === 'dark';
  const logoSrc = isDarkMode ? '/AINIINU_ROGO_W.png' : '/AINIINU_ROGO_B.png';

  if (!mounted) return <div className="hidden lg:flex w-24 h-screen" />;

  return (
    <>
      {/* Desktop Sidebar */}
      <aside className="hidden lg:flex w-24 h-screen flex-col items-center py-10 gap-10 z-50 transition-all duration-500 bg-sidebar border-r border-card-border shrink-0 m-4 h-[calc(100vh-32px)] rounded-[40px] shadow-2xl border-transparent">
        <div className="mb-6">
          <Link href="/dashboard">
            <div className="flex flex-col items-center justify-center transition-transform hover:rotate-12 group">
               <div className="w-12 h-12 bg-card rounded-full shadow-sm overflow-hidden mb-1 border border-card-border flex items-center justify-center group-hover:shadow-md transition-all">
                  <img src={logoSrc} alt="Logo" className="w-full h-full object-cover" />
               </div>
               <Typography variant="body" className="text-[10px] font-black tracking-tighter lowercase leading-none text-navy-900">
                 aini<span className="text-amber-500">inu</span>
               </Typography>
            </div>
          </Link>
        </div>

        <nav className="flex-1 flex flex-col gap-8">
          {menuItems.map((item) => {
            const isActive = pathname === item.path || pathname.startsWith(`${item.path}/`);
            return (
              <Link
                key={item.path}
                href={item.path}
                className={cn(
                  "p-4 transition-all duration-300 group relative",
                  isActive 
                    ? "bg-amber-50 text-amber-600 rounded-2xl scale-110 shadow-sm" 
                    : "text-zinc-300 hover:text-navy-900"
                )}
              >
                <item.icon size={24} strokeWidth={isActive ? 3 : 2} />
                <span className="absolute left-full ml-6 px-3 py-1.5 bg-navy-900 text-white text-[10px] font-black rounded-lg opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap pointer-events-none z-50 uppercase tracking-widest">
                  {item.label}
                </span>
              </Link>
            );
          })}
        </nav>

        <div className="flex flex-col gap-6">
          <button 
            onClick={() => setIsCreateModalOpen(true)}
            className="p-4 rounded-2xl border-2 border-dashed bg-background border-card-border text-amber-500 hover:bg-amber-50 transition-all active:scale-95"
          >
            <Plus size={20} />
          </button>
          <Link href="/" className="p-4 text-zinc-300 hover:text-error transition-colors">
            <LogOut size={20} />
          </Link>
        </div>
      </aside>

      {userProfile && (
        <CreatePostModal
          isOpen={isCreateModalOpen}
          onClose={() => setIsCreateModalOpen(false)}
          onSuccess={() => {
            if (pathname === '/profile' || pathname === '/feed') {
              window.location.reload();
            }
          }}
          userProfile={userProfile}
        />
      )}

      {/* Mobile Bottom Navigation Bar */}
      <nav className="lg:hidden fixed bottom-0 left-0 right-0 h-20 pb-safe px-6 flex items-center justify-around z-[100] border-t border-card-border backdrop-blur-2xl bg-sidebar/90 text-amber-600 shadow-[0_-10px_40px_rgba(0,0,0,0.05)]">
        {menuItems.map((item) => {
          const isActive = pathname === item.path || pathname.startsWith(`${item.path}/`);
          return (
            <Link
              key={item.path}
              href={item.path}
              className={cn(
                "flex flex-col items-center gap-1.5 p-2 transition-all relative",
                isActive ? "text-amber-600 scale-110 font-bold" : "text-zinc-400"
              )}
            >
              <item.icon size={22} strokeWidth={isActive ? 3 : 2} />
              <span className="text-[9px] font-black uppercase tracking-tighter">{item.label}</span>
              {isActive && (
                <div className="absolute -top-3 left-1/2 -translate-x-1/2 w-1 h-1 bg-amber-600 rounded-full"></div>
              )}
            </Link>
          );
        })}
      </nav>
    </>
  );
};

export default Sidebar;