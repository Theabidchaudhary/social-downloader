/** Typed client for the Siphon API. Mirrors backend/src/services/extractor.ts. */

export type Platform = 'youtube' | 'instagram' | 'tiktok' | 'twitter' | 'facebook';

export interface ResolvedFormat {
  id: string;
  kind: 'video' | 'audio';
  container: 'mp4' | 'mp3' | 'm4a';
  qualityLabel: string;
  width: number | null;
  height: number | null;
  fps: number | null;
  sizeBytes: number | null;
  sizeIsEstimate: boolean;
  directUrl: string | null;
  requiresProcessing: boolean;
  downloadUrl: string;
}

export interface ResolvedMedia {
  type: 'media';
  platform: Platform;
  kind: string;
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
  platform: Platform;
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

export class ApiRequestError extends Error {
  constructor(
    public readonly code: string,
    message: string,
  ) {
    super(message);
  }
}

/**
 * Base URL for the Siphon API. Empty by default, meaning "same origin,
 * relative /api/... paths" — the setup used when nginx (or the Vite dev
 * proxy) reverse-proxies /api to the backend on one domain.
 *
 * Set VITE_API_BASE_URL at build time (e.g. `VITE_API_BASE_URL=https://api.example.com
 * npm run build`, or as a platform env var) when the web app and API are
 * deployed as separate services with different origins (Render, Netlify,
 * Vercel + a standalone API host, etc). The backend must then set
 * ALLOWED_ORIGINS to the web app's origin so CORS allows the request.
 */
const API_BASE = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '');

function apiUrl(path: string): string {
  return path.startsWith('http') ? path : `${API_BASE}${path}`;
}

export async function resolve(url: string, signal?: AbortSignal): Promise<ResolveResult> {
  let response: Response;
  try {
    response = await fetch(apiUrl('/api/v1/resolve'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ url }),
      signal,
    });
  } catch (err) {
    if (err instanceof DOMException && err.name === 'AbortError') throw err;
    throw new ApiRequestError('NETWORK', 'Could not reach the Siphon API. Check your connection.');
  }

  const body = (await response.json().catch(() => null)) as
    | ResolveResult
    | { error?: { code?: string; message?: string } }
    | null;

  if (!response.ok || !body || !('type' in body)) {
    const error = body && 'error' in body ? body.error : undefined;
    throw new ApiRequestError(
      error?.code ?? 'INTERNAL',
      error?.message ?? 'Something went wrong. Try again.',
    );
  }

  // downloadUrl comes back as a path relative to the API (e.g.
  // "/api/v1/download?token=…"); absolutize it against API_BASE so the
  // browser fetches it from the API's origin, not the web app's.
  if (body.type === 'media') {
    body.video = body.video.map((f) => ({ ...f, downloadUrl: apiUrl(f.downloadUrl) }));
    body.audio = body.audio.map((f) => ({ ...f, downloadUrl: apiUrl(f.downloadUrl) }));
  }

  return body;
}
