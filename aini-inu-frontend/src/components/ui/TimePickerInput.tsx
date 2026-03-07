'use client';

import React, { useState, useRef, useEffect, useCallback } from 'react';
import { cn } from '@/lib/utils';
import { Clock } from 'lucide-react';

interface TimePickerInputProps {
  value: string; // "HH:mm" 24h format
  onChange: (value: string) => void;
  className?: string;
  placeholder?: string;
}

const HOURS_12 = Array.from({ length: 12 }, (_, i) => i + 1); // 1..12
const MINUTES = Array.from({ length: 12 }, (_, i) => i * 5); // 0,5,10,...55

// Reversed: larger values on top
const HOURS_DESC = [...HOURS_12].reverse(); // 12,11,10,...1
const MINUTES_DESC = [...MINUTES].reverse(); // 55,50,...0

function to12Hour(h24: number): { hour12: number; isPM: boolean } {
  if (h24 === 0) return { hour12: 12, isPM: false };
  if (h24 === 12) return { hour12: 12, isPM: true };
  if (h24 > 12) return { hour12: h24 - 12, isPM: true };
  return { hour12: h24, isPM: false };
}

function to24Hour(hour12: number, isPM: boolean): number {
  if (hour12 === 12) return isPM ? 12 : 0;
  return isPM ? hour12 + 12 : hour12;
}

function pad(n: number): string {
  return n.toString().padStart(2, '0');
}

function ScrollColumn({
  items,
  selected,
  onSelect,
  renderItem,
}: {
  items: (string | number)[];
  selected: string | number;
  onSelect: (item: string | number) => void;
  renderItem: (item: string | number) => string;
}) {
  const containerRef = useRef<HTMLDivElement>(null);
  const itemRefs = useRef<Map<string | number, HTMLButtonElement>>(new Map());

  useEffect(() => {
    const el = itemRefs.current.get(selected);
    if (el && containerRef.current) {
      const container = containerRef.current;
      const scrollTop = el.offsetTop - container.clientHeight / 2 + el.clientHeight / 2;
      container.scrollTo({ top: scrollTop, behavior: 'smooth' });
    }
  }, [selected]);

  return (
    <div
      ref={containerRef}
      className="h-[200px] overflow-y-auto scrollbar-thin flex flex-col"
    >
      {items.map((item) => {
        const isActive = item === selected;
        return (
          <button
            key={String(item)}
            type="button"
            ref={(el) => {
              if (el) itemRefs.current.set(item, el);
            }}
            onClick={() => onSelect(item)}
            className={cn(
              'px-3 py-2 text-sm font-bold text-center shrink-0 rounded-lg mx-1 transition-all',
              isActive
                ? 'bg-blue-500 text-white'
                : 'text-zinc-600 hover:bg-zinc-100',
            )}
          >
            {renderItem(item)}
          </button>
        );
      })}
    </div>
  );
}

export const TimePickerInput: React.FC<TimePickerInputProps> = ({
  value,
  onChange,
  className,
  placeholder = '시간 선택',
}) => {
  const [open, setOpen] = useState(false);
  const wrapperRef = useRef<HTMLDivElement>(null);

  // Parse current value
  const parsed = value
    ? (() => {
        const [hStr, mStr] = value.split(':');
        const h24 = parseInt(hStr, 10);
        const m = parseInt(mStr, 10);
        const { hour12, isPM } = to12Hour(h24);
        return { hour12, minute: m, isPM };
      })()
    : null;

  const selectedPeriod = parsed ? (parsed.isPM ? '오후' : '오전') : '오전';
  const selectedHour = parsed?.hour12 ?? 12;
  const selectedMinute = parsed ? parsed.minute : 0;

  // Snap minute to nearest 5
  const snappedMinute = Math.round(selectedMinute / 5) * 5 === 60 ? 55 : Math.round(selectedMinute / 5) * 5;

  const updateTime = useCallback(
    (period: string, hour12: number, minute: number) => {
      const isPM = period === '오후';
      const h24 = to24Hour(hour12, isPM);
      onChange(`${pad(h24)}:${pad(minute)}`);
    },
    [onChange],
  );

  // Close on outside click
  useEffect(() => {
    const handleClick = (e: MouseEvent) => {
      if (wrapperRef.current && !wrapperRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    if (open) document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, [open]);

  // Display text
  const displayText = parsed
    ? `${selectedPeriod} ${pad(selectedHour)}:${pad(snappedMinute)}`
    : placeholder;

  return (
    <div ref={wrapperRef} className="relative">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className={cn(
          'w-full bg-white border border-zinc-200 rounded-2xl py-3 px-5 text-sm font-bold text-navy-900 focus:outline-none focus:border-amber-500 focus:ring-4 ring-amber-500/10 transition-all text-left flex items-center gap-2',
          !parsed && 'text-zinc-400',
          className,
        )}
      >
        <Clock size={14} className="text-zinc-400 shrink-0" />
        {displayText}
      </button>

      {open && (
        <div className="absolute top-full mt-2 left-0 z-50 bg-white border border-zinc-200 rounded-2xl shadow-xl p-2 flex gap-1 animate-in fade-in zoom-in-95 duration-150">
          {/* Period column: 오후 on top, 오전 on bottom */}
          <ScrollColumn
            items={['오후', '오전']}
            selected={selectedPeriod}
            onSelect={(item) => updateTime(item as string, selectedHour, snappedMinute)}
            renderItem={(item) => item as string}
          />

          <div className="w-px bg-zinc-100" />

          {/* Hours: 12,11,10,...1 (larger on top) */}
          <ScrollColumn
            items={HOURS_DESC}
            selected={selectedHour}
            onSelect={(item) => updateTime(selectedPeriod, item as number, snappedMinute)}
            renderItem={(item) => pad(item as number)}
          />

          <div className="w-px bg-zinc-100" />

          {/* Minutes: 55,50,...0 (larger on top) */}
          <ScrollColumn
            items={MINUTES_DESC}
            selected={snappedMinute}
            onSelect={(item) => updateTime(selectedPeriod, selectedHour, item as number)}
            renderItem={(item) => pad(item as number)}
          />
        </div>
      )}
    </div>
  );
};
