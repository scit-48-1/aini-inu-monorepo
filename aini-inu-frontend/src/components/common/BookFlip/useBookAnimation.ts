'use client';

import { useState, useCallback } from 'react';

interface UseBookAnimationProps<T> {
  dataList: T[];
  onPageChange: (newData: T) => void;
  animationDuration?: number;
}

export function useBookAnimation<T extends { id: string | number }>({
  dataList,
  onPageChange,
  animationDuration = 1000
}: UseBookAnimationProps<T>) {
  const [pageDirection, setPageDirection] = useState<'next' | 'prev' | null>(null);
  const [tempNextData, setTempNextData] = useState<T | null>(null);

  const navigate = useCallback((direction: 'next' | 'prev', currentId: string | number) => {
    if (pageDirection) return;
    if (dataList.length <= 1) return;

    const currentIndex = dataList.findIndex(item => String(item.id) === String(currentId));
    if (currentIndex === -1) return;

    const nextIndex = direction === 'next'
      ? (currentIndex + 1) % dataList.length
      : (currentIndex - 1 + dataList.length) % dataList.length;

    const nextData = dataList[nextIndex];
    if (!nextData) return;

    setTempNextData(nextData);
    setPageDirection(direction);

    setTimeout(() => {
      onPageChange(nextData);
      setPageDirection(null);
      setTempNextData(null);
    }, animationDuration);
  }, [dataList, onPageChange, pageDirection, animationDuration]);

  return {
    pageDirection,
    tempNextData,
    navigate,
    isAnimating: !!pageDirection
  };
}
