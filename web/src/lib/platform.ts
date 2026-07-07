/**
 * Client-side platform detection used for instant UI feedback while typing.
 * The backend (lib/platform.ts) remains authoritative at resolve time.
 */
import type { Platform } from './api';

const RULES: Array<{ id: Platform; test: RegExp }> = [
  { id: 'youtube', test: /(^|\.)((youtube\.com)|(youtu\.be)|(youtube-nocookie\.com))$/i },
  { id: 'instagram', test: /(^|\.)instagram\.com$/i },
  { id: 'tiktok', test: /(^|\.)tiktok\.com$/i },
  { id: 'twitter', test: /(^|\.)((twitter\.com)|(x\.com)|(t\.co))$/i },
  { id: 'facebook', test: /(^|\.)((facebook\.com)|(fb\.watch)|(fb\.com))$/i },
];

export function detectPlatformClient(raw: string): Platform | null {
  const trimmed = raw.trim();
  if (!trimmed) return null;
  const candidate = /^[a-z][a-z0-9+.-]*:\/\//i.test(trimmed) ? trimmed : `https://${trimmed}`;
  try {
    const url = new URL(candidate);
    if (url.protocol !== 'http:' && url.protocol !== 'https:') return null;
    return RULES.find((r) => r.test.test(url.hostname))?.id ?? null;
  } catch {
    return null;
  }
}

export const PLATFORM_META: Record<Platform, { name: string; color: string }> = {
  youtube: { name: 'YouTube', color: '#ff5252' },
  instagram: { name: 'Instagram', color: '#e15fed' },
  tiktok: { name: 'TikTok', color: '#4cd9e8' },
  twitter: { name: 'X', color: '#e7e9ea' },
  facebook: { name: 'Facebook', color: '#5b8def' },
};
