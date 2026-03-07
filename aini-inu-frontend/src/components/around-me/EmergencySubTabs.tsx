'use client';

import React, { useState } from 'react';
import { Loader2, AlertTriangle, X } from 'lucide-react';
import { cn } from '@/lib/utils';
import { EmergencyReportForm } from '@/components/around-me/EmergencyReportForm';
import { LostPetListPanel } from '@/components/around-me/LostPetListPanel';
import { LostPetCandidateModal } from '@/components/around-me/LostPetCandidateModal';
import { analyzeLostPet } from '@/api/lostPets';
import type { LostPetAnalyzeResponse } from '@/api/lostPets';
import { ApiError } from '@/api/types';
import { Typography } from '@/components/ui/Typography';

type EmergencySubTab = 'REPORT' | 'MY_LIST';
type AnalysisState = 'idle' | 'loading' | 'success' | 'error';

export const EmergencySubTabs: React.FC = () => {
  const [activeSubTab, setActiveSubTab] = useState<EmergencySubTab>('REPORT');

  // Analysis orchestration state
  const [analysisState, setAnalysisState] = useState<AnalysisState>('idle');
  const [analyzeResult, setAnalyzeResult] = useState<LostPetAnalyzeResponse | null>(null);
  const [analysisError, setAnalysisError] = useState<string | null>(null);
  const [currentLostPetId, setCurrentLostPetId] = useState<number | null>(null);

  const triggerAnalysis = async (lostPetId: number, photoUrl: string) => {
    setAnalysisState('loading');
    setCurrentLostPetId(lostPetId);
    setAnalyzeResult(null);
    setAnalysisError(null);

    try {
      const result = await analyzeLostPet({
        lostPetId,
        imageUrl: photoUrl,
      });
      setAnalyzeResult(result);
      setAnalysisState('success');
    } catch (err) {
      if (err instanceof ApiError && err.errorCode === 'L500_AI_ANALYZE_FAILED') {
        setAnalysisState('error');
        setAnalysisError('AI 분석에 실패했습니다. 잠시 후 다시 시도해주세요.');
      } else {
        // Other errors: apiClient toast handles, reset to idle
        setAnalysisState('idle');
      }
    }
  };

  const dismissOverlay = () => {
    setAnalysisState('idle');
    setAnalyzeResult(null);
    setAnalysisError(null);
    setCurrentLostPetId(null);
  };

  return (
    <div className="flex-1 overflow-y-auto no-scrollbar space-y-6">
      {/* Sub-tab toggle */}
      <div className="flex bg-white p-1.5 rounded-2xl border border-card-border shadow-sm">
        <button
          onClick={() => setActiveSubTab('REPORT')}
          className={cn(
            'flex-1 py-3 rounded-xl text-xs font-black transition-all',
            activeSubTab === 'REPORT'
              ? 'bg-amber-500 text-black shadow-md'
              : 'text-black hover:bg-zinc-50',
          )}
        >
          신고/제보 작성
        </button>
        <button
          onClick={() => setActiveSubTab('MY_LIST')}
          className={cn(
            'flex-1 py-3 rounded-xl text-xs font-black transition-all',
            activeSubTab === 'MY_LIST'
              ? 'bg-amber-500 text-black shadow-md'
              : 'text-black hover:bg-zinc-50',
          )}
        >
          내 신고 목록
        </button>
      </div>

      {/* Sub-tab content */}
      {activeSubTab === 'REPORT' ? (
        <EmergencyReportForm
          onReportCreated={(report, photoUrl) => {
            triggerAnalysis(report.lostPetId, photoUrl);
          }}
        />
      ) : (
        <LostPetListPanel
          onAnalyzeRequest={(lostPetId, photoUrl) => {
            triggerAnalysis(lostPetId, photoUrl);
          }}
        />
      )}

      {/* Full-screen loading overlay */}
      {analysisState === 'loading' && (
        <div className="fixed inset-0 z-[3000] bg-white/95 backdrop-blur flex items-center justify-center">
          <div className="flex flex-col items-center space-y-6 animate-in fade-in duration-500">
            <div className="relative">
              <Loader2 className="animate-spin text-amber-500" size={48} />
              <div className="absolute inset-0 animate-ping opacity-20">
                <Loader2 className="text-amber-500" size={48} />
              </div>
            </div>
            <Typography variant="h3" className="text-navy-900 font-black text-lg animate-pulse">
              실종 동물 AI 분석 중...
            </Typography>
            <Typography variant="body" className="text-zinc-400 text-sm">
              Gemini AI가 유사한 제보를 검색하고 있습니다
            </Typography>
          </div>
        </div>
      )}

      {/* Error overlay */}
      {analysisState === 'error' && (
        <div className="fixed inset-0 z-[3000] bg-white/95 backdrop-blur flex items-center justify-center">
          <div className="flex flex-col items-center space-y-6 animate-in fade-in duration-500">
            <div className="w-16 h-16 rounded-full bg-red-50 flex items-center justify-center">
              <AlertTriangle className="text-red-500" size={32} />
            </div>
            <Typography variant="h3" className="text-navy-900 font-black text-lg">
              분석 실패
            </Typography>
            <Typography variant="body" className="text-zinc-400 text-sm text-center max-w-xs">
              {analysisError}
            </Typography>
            <button
              onClick={dismissOverlay}
              className="w-10 h-10 rounded-full bg-zinc-100 flex items-center justify-center hover:bg-zinc-200 transition-colors mt-4"
            >
              <X size={20} />
            </button>
          </div>
        </div>
      )}

      {/* Candidate modal on analysis success */}
      {analysisState === 'success' && analyzeResult && currentLostPetId !== null && (
        <LostPetCandidateModal
          lostPetId={currentLostPetId}
          sessionId={analyzeResult.sessionId}
          candidates={analyzeResult.candidates}
          onClose={dismissOverlay}
        />
      )}
    </div>
  );
};
