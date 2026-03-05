import React from 'react';
import { cn } from '@/lib/utils';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger';
  size?: 'sm' | 'md' | 'lg' | 'xl';
  fullWidth?: boolean;
}

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant = 'primary', size = 'md', fullWidth, ...props }, ref) => {
    const variants = {
      // Primary: 신뢰의 네이비
      primary: 'bg-navy-900 text-white hover:bg-navy-800 shadow-lg shadow-navy-900/10',
      // Secondary: 따뜻한 앰버 (포인트)
      secondary: 'bg-amber-500 text-navy-900 hover:bg-amber-600 shadow-lg shadow-amber-500/20',
      // Outline: 깔끔한 테두리
      outline: 'bg-transparent border-2 border-zinc-200 text-navy-900 hover:border-amber-500 hover:text-amber-600',
      // Ghost: 투명 버튼
      ghost: 'bg-transparent text-zinc-400 hover:text-navy-900 hover:bg-zinc-100',
      // Danger: 경고 (SOS 등)
      danger: 'bg-error text-white hover:opacity-90 shadow-lg shadow-error/20',
    };

    const sizes = {
      sm: 'px-2.5 py-1 text-[9px]',
      md: 'px-4 py-2 text-[11px]',
      lg: 'px-6 py-3 text-xs',
      xl: 'px-8 py-4 text-sm',
    };

    return (
      <button
        ref={ref}
        className={cn(
          'inline-flex items-center justify-center rounded-full font-black uppercase tracking-widest transition-all active:scale-95 disabled:opacity-50 disabled:pointer-events-none',
          variants[variant],
          sizes[size],
          fullWidth && 'w-full',
          className
        )}
        {...props}
      />
    );
  }
);

Button.displayName = 'Button';