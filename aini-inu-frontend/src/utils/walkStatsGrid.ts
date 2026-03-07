import type { WalkStatsResponse } from '@/api/members';

/**
 * Transforms WalkStatsResponse.points into a flat number[] for CSS grid rendering.
 * Iterates from startDate for windowDays days, filling missing dates with 0.
 * The resulting array works with CSS grid-flow-col + grid-rows-7 layout.
 */
export function pointsToGridCounts(stats: WalkStatsResponse): number[] {
  const lookup = new Map<string, number>();
  for (const p of stats.points) {
    lookup.set(p.date, p.count);
  }

  const counts: number[] = [];
  const start = new Date(stats.startDate + 'T00:00:00');

  for (let i = 0; i < stats.windowDays; i++) {
    const d = new Date(start);
    d.setDate(d.getDate() + i);
    const key = d.toISOString().slice(0, 10); // YYYY-MM-DD
    counts.push(lookup.get(key) ?? 0);
  }

  return counts;
}
