import React from 'react';
import { cn } from '@/lib/utils';

interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
  variant?: 'default' | 'vibrant' | 'glass';
  interactive?: boolean;
}

export const Card = ({ 
  className, 
  variant = 'default', 
  interactive = false, 
  children, 
  ...props 
}: CardProps) => {
  const variants = {
    // 다크모드 변수(bg-card, border-card-border)를 적용합니다.
    default: 'bg-card border border-card-border shadow-sm',
    vibrant: 'bg-card border-transparent shadow-xl',
    glass: 'bg-card/90 backdrop-blur-xl border border-white/10 shadow-2xl',
  };

  return (
    <div
      className={cn(
        'rounded-[32px] overflow-hidden transition-all duration-500',
        variants[variant],
        interactive && 'hover:shadow-2xl hover:-translate-y-1 cursor-pointer',
        className
      )}
      {...props}
    >
      {children}
    </div>
  );
};