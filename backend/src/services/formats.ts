import type { YtDlpFormat, YtDlpInfo } from '../lib/ytdlp.js';

/**
 * Public format model shared with the web and Android clients.
 * `selector` is the yt-dlp -f expression baked into the signed token; it is
 * never chosen by the client at download time, which keeps /download from
 * being a generic yt-dlp proxy.
 */
export interface MediaFormatOption {
  id: string;
  kind: 'video' | 'audio';
  container: 'mp4' | 'mp3' | 'm4a';
  /** e.g. "1080p", "720p60", "320 kbps" */
  qualityLabel: string;
  width: number | null;
  height: number | null;
  fps: number | null;
  sizeBytes: number | null;
  sizeIsEstimate: boolean;
  /**
   * Direct CDN URL when the source serves a single progressive file. Supports
   * HTTP Range on most platforms, so the Android app prefers it for
   * pause/resume. Absent when the option needs server-side muxing (1080p+
   * DASH) or transcoding (MP3).
   */
  directUrl: string | null;
  /** yt-dlp selector — consumed server-side only. */
  selector: string;
  /** True when ffmpeg must merge or transcode on the server. */
  requiresProcessing: boolean;
}

const AUDIO_BITRATES = [320, 192, 128] as const;

function isHttpProtocol(f: YtDlpFormat): boolean {
  return !f.protocol || f.protocol === 'https' || f.protocol === 'http';
}

function hasVideo(f: YtDlpFormat): boolean {
  return !!f.vcodec && f.vcodec !== 'none';
}

function hasAudio(f: YtDlpFormat): boolean {
  return !!f.acodec && f.acodec !== 'none';
}

function estimateSize(f: YtDlpFormat, duration: number | null): { size: number | null; estimate: boolean } {
  if (f.filesize && f.filesize > 0) return { size: f.filesize, estimate: false };
  if (f.filesize_approx && f.filesize_approx > 0) return { size: Math.round(f.filesize_approx), estimate: true };
  if (f.tbr && duration && duration > 0) {
    return { size: Math.round((f.tbr * 1000 * duration) / 8), estimate: true };
  }
  return { size: null, estimate: true };
}

function qualityLabel(height: number | null, fps: number | null): string {
  if (!height) return 'Video';
  const fpsSuffix = fps && fps > 30 ? String(Math.round(fps)) : '';
  return `${height}p${fpsSuffix}`;
}

/**
 * Build the user-facing option list from a raw yt-dlp format array.
 *
 * Strategy:
 *  - Progressive MP4s (video+audio in one file) become direct options.
 *  - For heights only available as video-only DASH streams (YouTube 1080p+),
 *    emit a server-muxed option (`<id>+bestaudio`).
 *  - One option per height, preferring progressive over muxed, higher tbr on ties.
 *  - Audio: the best audio-only stream as M4A passthrough plus MP3 transcodes
 *    at 320/192/128 kbps.
 */
export function buildFormatOptions(info: YtDlpInfo): {
  video: MediaFormatOption[];
  audio: MediaFormatOption[];
} {
  const duration = info.duration ?? null;
  const formats = (info.formats ?? []).filter(isHttpProtocol);

  const byHeight = new Map<number, { format: YtDlpFormat; progressive: boolean }>();

  for (const f of formats) {
    if (!hasVideo(f) || !f.height) continue;
    const progressive = hasAudio(f);
    const existing = byHeight.get(f.height);
    if (
      !existing ||
      (progressive && !existing.progressive) ||
      (progressive === existing.progressive && (f.tbr ?? 0) > (existing.format.tbr ?? 0))
    ) {
      // Prefer mp4 container on ties for maximum device compatibility.
      if (existing && existing.format.ext === 'mp4' && f.ext !== 'mp4' && progressive === existing.progressive) {
        continue;
      }
      byHeight.set(f.height, { format: f, progressive });
    }
  }

  const video: MediaFormatOption[] = [...byHeight.entries()]
    .sort(([a], [b]) => b - a)
    .map(([height, { format: f, progressive }]) => {
      const { size, estimate } = estimateSize(f, duration);
      const audioSize = progressive ? 0 : estimateBestAudioSize(formats, duration);
      return {
        id: `v${height}-${f.format_id}`,
        kind: 'video' as const,
        container: 'mp4' as const,
        qualityLabel: qualityLabel(height, f.fps ?? null),
        width: f.width ?? null,
        height,
        fps: f.fps ?? null,
        sizeBytes: size !== null ? size + audioSize : null,
        sizeIsEstimate: estimate || !progressive,
        directUrl: progressive && f.url ? f.url : null,
        selector: progressive ? f.format_id : `${f.format_id}+bestaudio/best`,
        requiresProcessing: !progressive,
      };
    });

  const audio = buildAudioOptions(formats, duration);
  return { video, audio };
}

function bestAudioOnly(formats: YtDlpFormat[]): YtDlpFormat | null {
  const audioOnly = formats.filter((f) => hasAudio(f) && !hasVideo(f));
  if (audioOnly.length === 0) return null;
  return audioOnly.reduce((best, f) => ((f.abr ?? f.tbr ?? 0) > (best.abr ?? best.tbr ?? 0) ? f : best));
}

function estimateBestAudioSize(formats: YtDlpFormat[], duration: number | null): number {
  const best = bestAudioOnly(formats);
  if (!best) return 0;
  const { size } = estimateSize(best, duration);
  return size ?? 0;
}

function buildAudioOptions(formats: YtDlpFormat[], duration: number | null): MediaFormatOption[] {
  const options: MediaFormatOption[] = [];
  const best = bestAudioOnly(formats);

  if (best) {
    const { size, estimate } = estimateSize(best, duration);
    if (best.ext === 'm4a' || best.ext === 'mp4') {
      options.push({
        id: `a-m4a-${best.format_id}`,
        kind: 'audio',
        container: 'm4a',
        qualityLabel: best.abr ? `M4A · ${Math.round(best.abr)} kbps` : 'M4A · original',
        width: null,
        height: null,
        fps: null,
        sizeBytes: size,
        sizeIsEstimate: estimate,
        directUrl: best.url ?? null,
        selector: best.format_id,
        requiresProcessing: false,
      });
    }
  }

  const sourceAvailable = best !== null || formats.some(hasAudio);
  if (sourceAvailable) {
    for (const bitrate of AUDIO_BITRATES) {
      const size = duration && duration > 0 ? Math.round((bitrate * 1000 * duration) / 8) : null;
      options.push({
        id: `a-mp3-${bitrate}`,
        kind: 'audio',
        container: 'mp3',
        qualityLabel: `MP3 · ${bitrate} kbps`,
        width: null,
        height: null,
        fps: null,
        sizeBytes: size,
        sizeIsEstimate: true,
        directUrl: null,
        selector: `mp3-${bitrate}`,
        requiresProcessing: true,
      });
    }
  }

  return options;
}
