'use client';

import React from 'react';
import { Loader2 } from 'lucide-react';

interface OtherProfileViewProps {
  memberId: number;
}

/**
 * OtherProfileView - placeholder for Plan 02.
 * Will be replaced with full other-member profile implementation.
 */
// eslint-disable-next-line @typescript-eslint/no-unused-vars
export const OtherProfileView: React.FC<OtherProfileViewProps> = ({ memberId }) => {
  return (
    <div className="h-full min-h-[200px] flex items-center justify-center opacity-20">
      <Loader2 className="animate-spin" size={48} />
    </div>
  );
};
