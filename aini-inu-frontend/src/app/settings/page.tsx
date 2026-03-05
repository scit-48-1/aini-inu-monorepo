'use client';

import React, { useEffect, useState } from 'react';
import { Moon, Sun, Bell, Shield, LogOut } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Typography } from '@/components/ui/Typography';
import { useTheme } from 'next-themes';
import { useConfigStore } from '@/store/useConfigStore';
import { useUserStore } from '@/store/useUserStore';
import { useRouter } from 'next/navigation';
import { toast } from 'sonner';

const SettingRow = ({ icon: Icon, title, description, children }: { icon: any, title: string, description: string, children: React.ReactNode }) => (
  <div className="flex items-center justify-between p-6 transition-all border bg-card border-card-border shadow-sm rounded-[32px] hover:border-amber-500/20">
    <div className="flex items-center gap-4">
      <div className="w-12 h-12 rounded-2xl flex items-center justify-center bg-background text-foreground shadow-inner">
        <Icon size={24} />
      </div>
      <div>
        <Typography variant="body" className="font-bold text-navy-900">{title}</Typography>
        <Typography variant="label" className="text-zinc-400 normal-case text-[11px] leading-tight">{description}</Typography>
      </div>
    </div>
    <div className="relative">{children}</div>
  </div>
);

const Toggle = ({ enabled, onToggle, color = "bg-amber-500" }: { enabled: boolean, onToggle: () => void, color?: string }) => (
  <button 
    onClick={onToggle}
    className={cn(
      "w-12 h-6 rounded-full relative transition-colors duration-300",
      enabled ? color : 'bg-zinc-200'
    )}
  >
    <div className={cn(
      "absolute top-1 w-4 h-4 bg-white rounded-full transition-all duration-300 shadow-sm",
      enabled ? 'left-7' : 'left-1'
    )}></div>
  </button>
);

export default function SettingsPage() {
  const { theme, setTheme } = useTheme();
  const { pushEnabled, togglePush, security2FA, toggle2FA } = useConfigStore();
  const logout = useUserStore((s) => s.logout);
  const router = useRouter();
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  if (!mounted) return null;

  const isDarkMode = theme === 'dark';

  const handleLogout = () => {
    logout();
    toast.success('로그아웃되었습니다.');
    router.replace('/login');
  };

  return (
    <div className="p-8 max-w-4xl mx-auto space-y-10 animate-in fade-in duration-500 h-full overflow-y-auto no-scrollbar">
      <header className="space-y-2">
        <Typography variant="h2" className="text-5xl font-black tracking-tighter text-navy-900">
          Settings
        </Typography>
        <Typography variant="body" className="text-zinc-400 font-medium">서비스 환경 및 보안 설정</Typography>
      </header>

      {/* 1. Interface Theme */}
      <div className="space-y-4">
        <Typography variant="label" className="font-black text-zinc-400 uppercase tracking-widest px-2">Interface Theme</Typography>
        <SettingRow
          icon={isDarkMode ? Moon : Sun}
          title="다크 모드"
          description="주변 환경에 맞춰 화면 테마를 전환합니다."
        >
          <Toggle enabled={isDarkMode} onToggle={() => setTheme(isDarkMode ? 'light' : 'dark')} />
        </SettingRow>
      </div>

      {/* 2. Notifications */}
      <div className="space-y-4">
        <Typography variant="label" className="font-black text-zinc-400 uppercase tracking-widest px-2">System Notifications</Typography>
        <SettingRow
          icon={Bell}
          title="푸시 알림"
          description="채팅, 산책 제안, 소셜 활동 알림을 받습니다."
        >
          <Toggle
            enabled={pushEnabled}
            onToggle={() => {
              togglePush();
              toast.success(pushEnabled ? '알림이 꺼졌습니다.' : '알림을 활성화했습니다.');
            }}
          />
        </SettingRow>
      </div>

      {/* 3. Security */}
      <div className="space-y-4">
        <Typography variant="label" className="font-black text-zinc-400 uppercase tracking-widest px-2">Security</Typography>
        <SettingRow
          icon={Shield}
          title="2단계 인증 (2FA)"
          description="로그인 시 추가 인증 단계로 계정을 보호합니다."
        >
          <Toggle
            enabled={security2FA}
            onToggle={() => {
              toggle2FA();
              toast.success(security2FA ? '2단계 인증이 꺼졌습니다.' : '2단계 인증이 활성화되었습니다.');
            }}
            color="bg-emerald-500"
          />
        </SettingRow>
      </div>

      {/* 4. Account */}
      <div className="space-y-4">
        <Typography variant="label" className="font-black text-zinc-400 uppercase tracking-widest px-2">Account</Typography>
        <button
          onClick={handleLogout}
          className="w-full flex items-center justify-between p-6 transition-all border bg-card border-card-border shadow-sm rounded-[32px] hover:border-red-400/40 group"
        >
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-2xl flex items-center justify-center bg-red-50 text-red-400 shadow-inner group-hover:bg-red-100 transition-colors">
              <LogOut size={24} />
            </div>
            <div className="text-left">
              <Typography variant="body" className="font-bold text-red-400">로그아웃</Typography>
              <Typography variant="label" className="text-zinc-400 normal-case text-[11px] leading-tight">현재 기기에서 로그아웃합니다.</Typography>
            </div>
          </div>
        </button>
      </div>
    </div>
  );
}
