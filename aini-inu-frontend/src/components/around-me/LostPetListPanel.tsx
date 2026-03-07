'use client';

import React, { useState, useEffect, useCallback } from 'react';
import { Loader2, ChevronDown, ChevronUp, Search, Sparkles } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Card } from '@/components/ui/Card';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import {
  getLostPets,
  getLostPet,
  getMatches,
} from '@/api/lostPets';
import type {
  LostPetSummaryResponse,
  LostPetDetailResponse,
  LostPetMatchCandidateResponse,
} from '@/api/lostPets';
import { LostPetCandidateModal } from '@/components/around-me/LostPetCandidateModal';
import type { AsyncState } from '@/api/types';

interface LostPetListPanelProps {
  onAnalyzeRequest?: (lostPetId: number, photoUrl: string) => void;
}

export const LostPetListPanel: React.FC<LostPetListPanelProps> = ({
  onAnalyzeRequest,
}) => {
  const [listState, setListState] = useState<AsyncState>('idle');
  const [items, setItems] = useState<LostPetSummaryResponse[]>([]);
  const [page, setPage] = useState(0);
  const [hasNext, setHasNext] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);

  // Expand state
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [detail, setDetail] = useState<LostPetDetailResponse | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);

  // Analysis state tracking
  const [analyzedIds, setAnalyzedIds] = useState<Set<number>>(new Set());
  const [analyzedCandidates, setAnalyzedCandidates] = useState<
    Map<number, { sessionId: number; candidates: LostPetMatchCandidateResponse[] }>
  >(new Map());

  // Modal for re-entry
  const [modalData, setModalData] = useState<{
    lostPetId: number;
    sessionId: number;
    candidates: LostPetMatchCandidateResponse[];
  } | null>(null);

  const fetchList = useCallback(async (pageNum: number, append: boolean) => {
    if (!append) setListState('loading');
    else setLoadingMore(true);

    try {
      const res = await getLostPets({ page: pageNum, size: 10 });
      const newItems = res.content;
      if (!append) {
        setItems(newItems);
        setListState(newItems.length === 0 ? 'empty' : 'success');
      } else {
        setItems((prev) => [...prev, ...newItems]);
      }
      setHasNext(res.hasNext);
      setPage(pageNum);
    } catch {
      if (!append) setListState('error');
    } finally {
      setLoadingMore(false);
    }
  }, []);

  useEffect(() => {
    fetchList(0, false);
  }, [fetchList]);

  const handleExpand = async (lostPetId: number) => {
    if (expandedId === lostPetId) {
      setExpandedId(null);
      setDetail(null);
      return;
    }

    setExpandedId(lostPetId);
    setDetail(null);
    setDetailLoading(true);

    try {
      const [detailRes, matchesRes] = await Promise.all([
        getLostPet(lostPetId),
        getMatches(lostPetId, { page: 0, size: 10 }).catch(() => null),
      ]);
      setDetail(detailRes);

      if (matchesRes && matchesRes.content.length > 0) {
        setAnalyzedIds((prev) => new Set(prev).add(lostPetId));
        const sessionId =
          matchesRes.content[0] && 'sessionId' in matchesRes.content[0]
            ? matchesRes.content[0].sessionId
            : 0;
        setAnalyzedCandidates((prev) => {
          const next = new Map(prev);
          next.set(lostPetId, { sessionId, candidates: matchesRes.content });
          return next;
        });
      }
    } catch {
      // toast handled by apiClient
    } finally {
      setDetailLoading(false);
    }
  };

  const handleReEnter = (lostPetId: number) => {
    const data = analyzedCandidates.get(lostPetId);
    if (data) {
      setModalData({ lostPetId, ...data });
    }
  };

  const formatDate = (dateStr: string) => {
    try {
      return new Date(dateStr).toLocaleString('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return dateStr;
    }
  };

  // Loading state
  if (listState === 'loading') {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="animate-spin text-amber-500" size={32} />
      </div>
    );
  }

  // Error state
  if (listState === 'error') {
    return (
      <div className="flex flex-col items-center justify-center py-20 space-y-4">
        <Typography variant="body" className="text-zinc-400 text-sm">
          목록을 불러올 수 없습니다.
        </Typography>
        <Button
          variant="outline"
          size="sm"
          onClick={() => fetchList(0, false)}
        >
          다시 시도
        </Button>
      </div>
    );
  }

  // Empty state
  if (listState === 'empty') {
    return (
      <div className="flex items-center justify-center py-20">
        <Typography variant="body" className="text-zinc-400 font-bold text-sm">
          등록된 신고가 없습니다.
        </Typography>
      </div>
    );
  }

  return (
    <>
      <div className="space-y-4">
        {items.map((item) => {
          const isExpanded = expandedId === item.lostPetId;
          const isAnalyzed = analyzedIds.has(item.lostPetId);

          return (
            <Card
              key={item.lostPetId}
              className="rounded-3xl overflow-hidden border border-card-border shadow-sm bg-white"
            >
              {/* Card header -- clickable */}
              <button
                onClick={() => handleExpand(item.lostPetId)}
                className="w-full px-5 py-4 flex items-center justify-between text-left"
              >
                <div className="flex items-center gap-3 flex-1 min-w-0">
                  <Typography
                    variant="body"
                    className="font-black text-sm text-navy-900 truncate"
                  >
                    {item.petName}
                  </Typography>
                  <Badge
                    variant={item.status === 'SEARCHING' ? 'amber' : 'default'}
                    className={cn(
                      'text-[10px] font-bold shrink-0',
                      item.status === 'SEARCHING'
                        ? 'bg-amber-100 text-amber-700 border-amber-200'
                        : 'bg-zinc-100 text-zinc-500 border-zinc-200',
                    )}
                  >
                    {item.status === 'SEARCHING' ? '수색 중' : item.status}
                  </Badge>
                  {isAnalyzed && (
                    <Badge
                      variant="default"
                      className="text-[10px] font-bold bg-green-100 text-green-700 border-green-200 shrink-0 cursor-pointer"
                      onClick={(e: React.MouseEvent) => {
                        e.stopPropagation();
                        handleReEnter(item.lostPetId);
                      }}
                    >
                      <Sparkles size={10} className="mr-0.5" />
                      분석 완료
                    </Badge>
                  )}
                </div>
                <div className="flex items-center gap-3 shrink-0">
                  <Typography
                    variant="label"
                    className="text-zinc-400 text-xs"
                  >
                    {formatDate(item.lastSeenAt)}
                  </Typography>
                  {isExpanded ? (
                    <ChevronUp size={16} className="text-zinc-400" />
                  ) : (
                    <ChevronDown size={16} className="text-zinc-400" />
                  )}
                </div>
              </button>

              {/* Expanded detail */}
              {isExpanded && (
                <div className="px-5 pb-5 pt-0 border-t border-zinc-100 animate-in slide-in-from-top-2 duration-300">
                  {detailLoading ? (
                    <div className="flex items-center justify-center py-8">
                      <Loader2 className="animate-spin text-amber-500" size={20} />
                    </div>
                  ) : detail ? (
                    <div className="space-y-4 pt-4">
                      {detail.photoUrl && (
                        <img
                          src={detail.photoUrl}
                          alt={detail.petName}
                          className="w-full max-w-[240px] aspect-square object-cover rounded-2xl shadow-md"
                        />
                      )}
                      <div className="space-y-2">
                        <Typography
                          variant="body"
                          className="font-bold text-sm text-navy-900"
                        >
                          {detail.petName}
                        </Typography>
                        <Typography
                          variant="label"
                          className="text-zinc-400 text-xs"
                        >
                          상태: {detail.status === 'SEARCHING' ? '수색 중' : detail.status}
                        </Typography>
                        <Typography
                          variant="label"
                          className="text-zinc-400 text-xs"
                        >
                          마지막 목격: {formatDate(detail.lastSeenAt)}
                        </Typography>
                        {detail.lastSeenLocation && (
                          <Typography
                            variant="label"
                            className="text-zinc-400 text-xs"
                          >
                            장소: {detail.lastSeenLocation}
                          </Typography>
                        )}
                      </div>

                      {/* Action buttons */}
                      {isAnalyzed ? (
                        <Button
                          variant="outline"
                          size="sm"
                          className="gap-2"
                          onClick={() => handleReEnter(item.lostPetId)}
                        >
                          <Sparkles size={14} />
                          매칭 결과 보기
                        </Button>
                      ) : (
                        <Button
                          variant="primary"
                          size="sm"
                          className="gap-2 bg-amber-500 text-black"
                          onClick={() =>
                            onAnalyzeRequest?.(item.lostPetId, detail.photoUrl)
                          }
                        >
                          <Search size={14} />
                          분석하기
                        </Button>
                      )}
                    </div>
                  ) : null}
                </div>
              )}
            </Card>
          );
        })}

        {/* Load more */}
        {hasNext && (
          <div className="flex justify-center pt-4">
            <Button
              variant="outline"
              size="sm"
              onClick={() => fetchList(page + 1, true)}
              disabled={loadingMore}
              className="rounded-full px-6"
            >
              {loadingMore ? (
                <Loader2 className="animate-spin" size={16} />
              ) : (
                '더 보기'
              )}
            </Button>
          </div>
        )}
      </div>

      {/* Re-entry candidate modal */}
      {modalData && (
        <LostPetCandidateModal
          lostPetId={modalData.lostPetId}
          sessionId={modalData.sessionId}
          candidates={modalData.candidates}
          onClose={() => setModalData(null)}
        />
      )}
    </>
  );
};
