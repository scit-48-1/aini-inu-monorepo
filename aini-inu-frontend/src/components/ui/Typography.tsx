import React from 'react';
import { cn } from '@/lib/utils';

interface TypographyProps extends React.HTMLAttributes<HTMLHeadingElement | HTMLParagraphElement> {
  variant?: 'h1' | 'h2' | 'h3' | 'body' | 'label' | 'serif';
  italic?: boolean;
}

export const Typography = ({ 
  className, 
  variant = 'body', 
  italic = false, 
  children, 
  ...props 
}: TypographyProps) => {
  const tags = {
    h1: 'h1',
    h2: 'h2',
    h3: 'h3',
    body: 'p',
    label: 'span',
    serif: 'h2',
  };

  const styles = {
    h1: 'text-4xl md:text-6xl font-black tracking-tighter leading-none',
    h2: 'text-xl md:text-3xl font-black tracking-tighter leading-tight',
    h3: 'text-lg font-black tracking-tight',
    // 본문을 표준 크기로 유지 (16px)
    body: 'text-base font-medium leading-relaxed',
    // 라벨 크기 (11px)
    label: 'text-[11px] font-black uppercase tracking-[0.1em]',
    serif: 'text-2xl md:text-4xl font-serif font-medium tracking-tighter italic',
  };

  const Tag = tags[variant] as any;

  return (
    <Tag
      className={cn(
        styles[variant],
        italic && 'italic',
        className
      )}
      {...props}
    >
      {children}
    </Tag>
  );
};