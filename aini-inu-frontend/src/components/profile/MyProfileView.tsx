'use client';

import React from 'react';
import { Loader2 } from 'lucide-react';

/**
 * MyProfileView - own profile variant (stub — full implementation in Task 2).
 */
export const MyProfileView: React.FC = () => {
  return (
    <div className="h-full min-h-[200px] flex items-center justify-center opacity-20">
      <Loader2 className="animate-spin" size={48} />
    </div>
  );
};
