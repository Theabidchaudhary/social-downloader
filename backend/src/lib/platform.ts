/**
 * Platform detection. Mirrored by the web client (web/src/lib/platform.ts)
 * and the Android client (util/UrlDetector.kt) so every surface agrees on
 * what Siphon supports before a request ever reaches the API.
 */
export type Platform = 'youtube' | 'instagram' | 'tiktok' | 'twitter' | 'facebook';

export type ContentKind = 'video' | 'short' | 'reel' | 'playlist' | 'unknown';

export interface DetectedLink {
  platform: Platform;
  kind: ContentKind;
  /** URL with tracking noise stripped; used as the cache key. */
  canonicalUrl: string;
}

const HOST_RULES: Array<{ platform: Platform; hosts: RegExp }> = [
  { platform: 'youtube', hosts: /(^|\.)((youtube\.com)|(youtu\.be)|(youtube-nocookie\.com)|(music\.youtube\.com))$/i },
  { platform: 'instagram', hosts: /(^|\.)instagram\.com$/i },
  { platform: 'tiktok', hosts: /(^|\.)tiktok\.com$/i },
  { platform: 'twitter', hosts: /(^|\.)((twitter\.com)|(x\.com)|(t\.co))$/i },
  { platform: 'facebook', hosts: /(^|\.)((facebook\.com)|(fb\.watch)|(fb\.com))$/i },
];

/** Query params that identify content and must survive canonicalization. */
const KEEP_PARAMS = new Set(['v', 'list', 'video_id', 'story_fbid', 'fbid', 'id']);

export function parseUrl(raw: string): URL | null {
  const trimmed = raw.trim();
  if (!trimmed) return null;
  const candidate = /^[a-z][a-z0-9+.-]*:\/\//i.test(trimmed) ? trimmed : `https://${trimmed}`;
  try {
    const url = new URL(candidate);
    if (url.protocol !== 'http:' && url.protocol !== 'https:') return null;
    if (!url.hostname.includes('.')) return null;
    return url;
  } catch {
    return null;
  }
}

export function detectPlatform(raw: string): DetectedLink | null {
  const url = parseUrl(raw);
  if (!url) return null;

  const rule = HOST_RULES.find((r) => r.hosts.test(url.hostname));
  if (!rule) return null;

  const kind = classify(rule.platform, url);
  return { platform: rule.platform, kind, canonicalUrl: canonicalize(url) };
}

function classify(platform: Platform, url: URL): ContentKind {
  const path = url.pathname.toLowerCase();
  switch (platform) {
    case 'youtube': {
      if (path.startsWith('/playlist') || (url.searchParams.has('list') && !url.searchParams.has('v'))) {
        return 'playlist';
      }
      if (path.startsWith('/shorts/')) return 'short';
      if (url.hostname.endsWith('youtu.be') || url.searchParams.has('v') || path.startsWith('/live/') || path.startsWith('/embed/')) {
        return 'video';
      }
      return 'unknown';
    }
    case 'instagram': {
      if (path.startsWith('/reel/') || path.startsWith('/reels/')) return 'reel';
      if (path.startsWith('/p/') || path.startsWith('/tv/')) return 'video';
      if (/^\/[^/]+\/(reel|p)\//.test(path)) return path.includes('/reel/') ? 'reel' : 'video';
      return 'unknown';
    }
    case 'tiktok': {
      if (/\/video\/\d+/.test(path)) return 'video';
      if (/^\/@[^/]+\/?$/.test(path) || path === '/' || path === '') return 'unknown'; // profile / home
      // Short links: vm.tiktok.com/<code>, tiktok.com/t/<code>, /v/<id>
      return 'video';
    }
    case 'twitter': {
      if (/\/status\/\d+/.test(path)) return 'video';
      if (url.hostname.endsWith('t.co')) return 'video';
      return 'unknown';
    }
    case 'facebook': {
      if (url.hostname.endsWith('fb.watch')) return 'video';
      if (path.includes('/videos/') || path.startsWith('/watch') || path.startsWith('/reel/') || path.startsWith('/share/')) {
        return path.startsWith('/reel/') ? 'reel' : 'video';
      }
      return 'unknown';
    }
  }
}

function canonicalize(url: URL): string {
  const out = new URL(url.href);
  out.hash = '';
  const toDelete: string[] = [];
  out.searchParams.forEach((_value, key) => {
    if (!KEEP_PARAMS.has(key)) toDelete.push(key);
  });
  for (const key of toDelete) out.searchParams.delete(key);
  // Normalize host casing and drop default ports; URL does most of this.
  return out.href;
}

export const SUPPORTED_PLATFORMS: Array<{
  id: Platform;
  name: string;
  capabilities: string[];
}> = [
  { id: 'youtube', name: 'YouTube', capabilities: ['video', 'short', 'playlist', 'audio', 'mp3'] },
  { id: 'instagram', name: 'Instagram', capabilities: ['video', 'reel', 'audio', 'mp3'] },
  { id: 'tiktok', name: 'TikTok', capabilities: ['video', 'audio', 'mp3'] },
  { id: 'twitter', name: 'X (Twitter)', capabilities: ['video', 'audio', 'mp3'] },
  { id: 'facebook', name: 'Facebook', capabilities: ['video', 'reel', 'audio', 'mp3'] },
];
