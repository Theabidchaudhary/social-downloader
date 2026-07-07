import { createReadStream } from 'node:fs';
import { mkdtemp, readdir, rm, stat } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import path from 'node:path';
import { config } from '../config.js';
import { ApiError } from '../lib/errors.js';
import { Semaphore } from '../lib/semaphore.js';
import type { DownloadTicket } from '../lib/sign.js';
import { runYtDlp, platformArgs, BROWSER_USER_AGENT } from '../lib/ytdlp.js';

const streamGate = new Semaphore(config.MAX_CONCURRENT_STREAMS);
const DOWNLOAD_TIMEOUT_MS = 20 * 60 * 1000;

export interface PreparedFile {
  filePath: string;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  /** Call when the response finishes (success or abort) to reclaim disk. */
  cleanup: () => Promise<void>;
  createStream: () => NodeJS.ReadableStream;
}

const CONTENT_TYPES: Record<string, string> = {
  mp4: 'video/mp4',
  mp3: 'audio/mpeg',
  m4a: 'audio/mp4',
};

/**
 * Fetch the requested media into a per-request temp directory, then hand back
 * a readable stream plus metadata.
 *
 * We deliberately buffer to disk instead of piping yt-dlp's stdout:
 *  - MP4 muxing and MP3 transcoding cannot be produced on a pipe (both need a
 *    seekable output), so a temp file is required for those paths anyway.
 *  - It lets us send an exact Content-Length, which browsers and download
 *    managers (IDM et al.) need for progress bars and segmented fetching.
 */
export async function prepareDownload(ticket: DownloadTicket): Promise<PreparedFile> {
  return streamGate.run(async () => {
    const workDir = await mkdtemp(path.join(tmpdir(), 'siphon-'));
    const cleanup = () => rm(workDir, { recursive: true, force: true });

    try {
      const args = buildArgs(ticket, workDir);
      const result = await runYtDlp(args, DOWNLOAD_TIMEOUT_MS);
      if (result.exitCode !== 0) {
        throw ApiError.extractionFailed({ stderr: result.stderr.slice(0, 500) });
      }

      const files = await readdir(workDir);
      const produced = files.find((f) => !f.endsWith('.part') && !f.endsWith('.ytdl'));
      if (!produced) throw ApiError.extractionFailed({ reason: 'no-output-file' });

      const filePath = path.join(workDir, produced);
      const info = await stat(filePath);

      if (config.MAX_DOWNLOAD_SIZE_MB > 0 && info.size > config.MAX_DOWNLOAD_SIZE_MB * 1024 * 1024) {
        throw ApiError.downloadTooLarge(config.MAX_DOWNLOAD_SIZE_MB);
      }

      const ext = ticket.c;
      return {
        filePath,
        fileName: `${sanitizeFileName(ticket.t || 'siphon-download')}.${ext}`,
        contentType: CONTENT_TYPES[ext] ?? 'application/octet-stream',
        sizeBytes: info.size,
        cleanup,
        createStream: () => createReadStream(filePath),
      };
    } catch (err) {
      await cleanup().catch(() => undefined);
      throw err;
    }
  });
}

function buildArgs(ticket: DownloadTicket, workDir: string): string[] {
  const base = [
    '--user-agent', BROWSER_USER_AGENT,
    '--add-header', 'Accept-Language:en-US,en;q=0.9',
    '--no-warnings',
    '--no-call-home',
    '--no-check-certificates',
    '--no-playlist',
    '--no-mtime',
    ...platformArgs(ticket.u),
    '--max-filesize',
    config.MAX_DOWNLOAD_SIZE_MB > 0 ? `${config.MAX_DOWNLOAD_SIZE_MB}M` : 'infinite',
    '-o',
    path.join(workDir, 'media.%(ext)s'),
  ];

  const mp3Match = /^mp3-(\d{2,3})$/.exec(ticket.f);
  if (ticket.k === 'audio' && mp3Match) {
    return [
      ...base,
      '-f',
      'bestaudio/best',
      '-x',
      '--audio-format',
      'mp3',
      '--audio-quality',
      `${mp3Match[1]}K`,
      '--',
      ticket.u,
    ];
  }

  if (ticket.k === 'video' && ticket.c === 'mp4' && ticket.f.includes('+')) {
    return [...base, '-f', ticket.f, '--merge-output-format', 'mp4', '--', ticket.u];
  }

  return [...base, '-f', ticket.f, '--', ticket.u];
}

export function sanitizeFileName(name: string): string {
  return (
    name
      .replace(/[\\/:*?"<>|\u0000-\u001f]/g, ' ')
      .replace(/\s+/g, ' ')
      .trim()
      .slice(0, 120)
      .replace(/[. ]+$/g, '') || 'siphon-download'
  );
}
