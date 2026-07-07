import { config } from '../config.js';
import { ApiError } from '../lib/errors.js';
import { detectPlatform, type DetectedLink } from '../lib/platform.js';
import { Semaphore } from '../lib/semaphore.js';
import { signTicket, type DownloadTicket } from '../lib/sign.js';
import { fetchInfo, type YtDlpInfo } from '../lib/ytdlp.js';
import { buildFormatOptions, type MediaFormatOption } from './formats.js';

export interface ResolvedFormat extends Omit<MediaFormatOption, 'selector'> {
  /** Signed, expiring URL on this API that streams the file. */
  downloadUrl: string;
}

export interface ResolvedMedia {
  type: 'media';
  platform: DetectedLink['platform'];
  kind: DetectedLink['kind'];
  sourceUrl: string;
  title: string;
  uploader: string | null;
  durationSeconds: number | null;
  thumbnailUrl: string | null;
  video: ResolvedFormat[];
  audio: ResolvedFormat[];
  expiresAt: string;
}

export interface ResolvedPlaylist {
  type: 'playlist';
  platform: DetectedLink['platform'];
  sourceUrl: string;
  title: string;
  entryCount: number;
  entries: Array<{
    title: string;
    url: string;
    durationSeconds: number | null;
    thumbnailUrl: string | null;
  }>;
}

export type ResolveResult = ResolvedMedia | ResolvedPlaylist;

const extractionGate = new Semaphore(config.MAX_CONCURRENT_EXTRACTIONS);

interface CacheEntry {
  value: ResolveResult;
  expiresAtMs: number;
}

const cache = new Map<string, CacheEntry>();
const CACHE_MAX_ENTRIES = 500;

function cacheGet(key: string): ResolveResult | null {
  const hit = cache.get(key);
  if (!hit) return null;
  if (Date.now() > hit.expiresAtMs) {
    cache.delete(key);
    return null;
  }
  return hit.value;
}

function cacheSet(key: string, value: ResolveResult): void {
  if (config.RESOLVE_CACHE_TTL_SECONDS === 0) return;
  if (cache.size >= CACHE_MAX_ENTRIES) {
    const oldest = cache.keys().next().value;
    if (oldest !== undefined) cache.delete(oldest);
  }
  cache.set(key, { value, expiresAtMs: Date.now() + config.RESOLVE_CACHE_TTL_SECONDS * 1000 });
}

export async function resolveUrl(rawUrl: string): Promise<ResolveResult> {
  const detected = detectPlatform(rawUrl);
  if (!detected) {
    // Distinguish "not a URL at all" from "URL we don't support".
    throw rawUrl.trim().match(/^\S+\.\S+/) || rawUrl.includes('://')
      ? ApiError.unsupportedPlatform()
      : ApiError.invalidUrl();
  }

  const cached = cacheGet(detected.canonicalUrl);
  if (cached) return refreshTokens(cached);

  const info = await extractionGate.run(() =>
    fetchInfo(detected.canonicalUrl, { playlist: detected.kind === 'playlist' }),
  );

  const result =
    info._type === 'playlist' && info.entries
      ? mapPlaylist(detected, info)
      : mapMedia(detected, info);

  cacheSet(detected.canonicalUrl, result);
  return result;
}

/**
 * Cached results keep their format lists but tokens inside may be close to
 * expiry; re-sign on every cache hit so clients always receive full-TTL links.
 */
function refreshTokens(result: ResolveResult): ResolveResult {
  if (result.type !== 'media') return result;
  const resign = (f: ResolvedFormat): ResolvedFormat => ({
    ...f,
    downloadUrl: buildDownloadUrl({
      sourceUrl: result.sourceUrl,
      title: result.title,
      option: f,
    }),
  });
  return {
    ...result,
    expiresAt: tokenExpiryIso(),
    video: result.video.map(resign),
    audio: result.audio.map(resign),
  };
}

function tokenExpirySeconds(): number {
  return Math.floor(Date.now() / 1000) + config.TOKEN_TTL_SECONDS;
}

function tokenExpiryIso(): string {
  return new Date(tokenExpirySeconds() * 1000).toISOString();
}

function buildDownloadUrl(input: {
  sourceUrl: string;
  title: string;
  option: Pick<MediaFormatOption, 'kind' | 'container'> & { selector?: string; id: string };
  selector?: string;
}): string {
  const ticket: DownloadTicket = {
    u: input.sourceUrl,
    f: input.selector ?? input.option.selector ?? input.option.id,
    k: input.option.kind,
    c: input.option.container,
    t: input.title.slice(0, 120),
    e: tokenExpirySeconds(),
  };
  const token = signTicket(ticket, config.SIGNING_SECRET);
  return `/api/v1/download?token=${encodeURIComponent(token)}`;
}

function mapMedia(detected: DetectedLink, info: YtDlpInfo): ResolvedMedia {
  const { video, audio } = buildFormatOptions(info);
  if (video.length === 0 && audio.length === 0) {
    throw ApiError.extractionFailed({ reason: 'no-downloadable-formats' });
  }

  const title = info.title ?? 'Untitled media';
  const sourceUrl = info.webpage_url ?? detected.canonicalUrl;

  const attach = (options: MediaFormatOption[]): ResolvedFormat[] =>
    options.map(({ selector, ...rest }) => ({
      ...rest,
      downloadUrl: buildDownloadUrl({ sourceUrl, title, option: rest, selector }),
    }));

  return {
    type: 'media',
    platform: detected.platform,
    kind: detected.kind === 'playlist' ? 'video' : detected.kind,
    sourceUrl,
    title,
    uploader: info.uploader ?? info.channel ?? null,
    durationSeconds: info.duration ?? null,
    thumbnailUrl: pickThumbnail(info),
    video: attach(video),
    audio: attach(audio),
    expiresAt: tokenExpiryIso(),
  };
}

function mapPlaylist(detected: DetectedLink, info: YtDlpInfo): ResolvedPlaylist {
  const entries = (info.entries ?? [])
    .filter((e) => !!e.url || !!e.id)
    .slice(0, 200)
    .map((e) => ({
      title: e.title ?? 'Untitled',
      url: e.url ?? `https://www.youtube.com/watch?v=${e.id}`,
      durationSeconds: e.duration ?? null,
      thumbnailUrl: e.thumbnails?.[0]?.url ?? null,
    }));

  return {
    type: 'playlist',
    platform: detected.platform,
    sourceUrl: info.webpage_url ?? detected.canonicalUrl,
    title: info.title ?? 'Playlist',
    entryCount: entries.length,
    entries,
  };
}

function pickThumbnail(info: YtDlpInfo): string | null {
  if (info.thumbnail) return info.thumbnail;
  const list = info.thumbnails ?? [];
  if (list.length === 0) return null;
  const best = [...list].sort((a, b) => (b.width ?? 0) - (a.width ?? 0))[0];
  return best?.url ?? null;
}
