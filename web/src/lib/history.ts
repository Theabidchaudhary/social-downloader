import type { Platform } from './api';

export interface HistoryEntry {
  sourceUrl: string;
  title: string;
  platform: Platform;
  thumbnailUrl: string | null;
  qualityLabel: string;
  container: string;
  savedAt: number;
}

const KEY = 'siphon.history.v1';
const MAX_ENTRIES = 30;

export function loadHistory(): HistoryEntry[] {
  try {
    const raw = localStorage.getItem(KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw) as HistoryEntry[];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export function addHistory(entry: HistoryEntry): HistoryEntry[] {
  const next = [entry, ...loadHistory().filter((e) => !(e.sourceUrl === entry.sourceUrl && e.qualityLabel === entry.qualityLabel))].slice(0, MAX_ENTRIES);
  try {
    localStorage.setItem(KEY, JSON.stringify(next));
  } catch {
    /* storage full or blocked — history is best-effort */
  }
  return next;
}

export function clearHistory(): HistoryEntry[] {
  try {
    localStorage.removeItem(KEY);
  } catch {
    /* ignore */
  }
  return [];
}
