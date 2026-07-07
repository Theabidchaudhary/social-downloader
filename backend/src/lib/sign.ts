import { createHmac, timingSafeEqual } from 'node:crypto';
import { ApiError } from './errors.js';

/**
 * Stateless signed download tokens.
 *
 * The /resolve endpoint hands out download URLs of the form
 *   /api/v1/download?token=<payload>.<signature>
 * where payload is base64url(JSON) and signature is HMAC-SHA256 over the
 * payload. This keeps the download endpoint from being an open proxy while
 * requiring no server-side session state — any replica can verify a token.
 */
export interface DownloadTicket {
  /** Canonical source page URL. */
  u: string;
  /** yt-dlp format selector (e.g. "137+bestaudio", "22", "mp3-320"). */
  f: string;
  /** 'video' | 'audio' */
  k: 'video' | 'audio';
  /** Container the client should receive: 'mp4' | 'mp3' | 'm4a'. */
  c: string;
  /** Human title used for the Content-Disposition filename. */
  t: string;
  /** Unix seconds expiry. */
  e: number;
}

function b64url(buf: Buffer): string {
  return buf.toString('base64url');
}

function hmac(payload: string, secret: string): string {
  return b64url(createHmac('sha256', secret).update(payload).digest());
}

export function signTicket(ticket: DownloadTicket, secret: string): string {
  const payload = b64url(Buffer.from(JSON.stringify(ticket), 'utf8'));
  return `${payload}.${hmac(payload, secret)}`;
}

export function verifyToken(token: string, secret: string, now = Date.now()): DownloadTicket {
  const dot = token.lastIndexOf('.');
  if (dot <= 0) throw ApiError.tokenInvalid();

  const payload = token.slice(0, dot);
  const signature = token.slice(dot + 1);
  const expected = hmac(payload, secret);

  const a = Buffer.from(signature);
  const b = Buffer.from(expected);
  if (a.length !== b.length || !timingSafeEqual(a, b)) throw ApiError.tokenInvalid();

  let ticket: DownloadTicket;
  try {
    ticket = JSON.parse(Buffer.from(payload, 'base64url').toString('utf8')) as DownloadTicket;
  } catch {
    throw ApiError.tokenInvalid();
  }

  if (
    typeof ticket.u !== 'string' ||
    typeof ticket.f !== 'string' ||
    (ticket.k !== 'video' && ticket.k !== 'audio') ||
    typeof ticket.c !== 'string' ||
    typeof ticket.e !== 'number'
  ) {
    throw ApiError.tokenInvalid();
  }

  if (now / 1000 > ticket.e) throw ApiError.tokenExpired();
  return ticket;
}
