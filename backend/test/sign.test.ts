import { describe, expect, it } from 'vitest';
import { ApiError } from '../src/lib/errors.js';
import { signTicket, verifyToken, type DownloadTicket } from '../src/lib/sign.js';

const SECRET = 'test-secret-of-decent-length';

function ticket(overrides: Partial<DownloadTicket> = {}): DownloadTicket {
  return {
    u: 'https://www.youtube.com/watch?v=abc',
    f: '137+bestaudio/best',
    k: 'video',
    c: 'mp4',
    t: 'A test video',
    e: Math.floor(Date.now() / 1000) + 600,
    ...overrides,
  };
}

describe('download token signing', () => {
  it('round-trips a valid ticket', () => {
    const token = signTicket(ticket(), SECRET);
    const verified = verifyToken(token, SECRET);
    expect(verified.u).toBe('https://www.youtube.com/watch?v=abc');
    expect(verified.k).toBe('video');
  });

  it('rejects tampered payloads', () => {
    const token = signTicket(ticket(), SECRET);
    const [payload = '', sig = ''] = token.split('.');
    const forged = Buffer.from(
      JSON.stringify({ ...ticket(), u: 'https://evil.example.com/x' }),
      'utf8',
    ).toString('base64url');
    expect(() => verifyToken(`${forged}.${sig}`, SECRET)).toThrowError(ApiError);
    expect(() => verifyToken(`${payload}x.${sig}`, SECRET)).toThrowError(ApiError);
  });

  it('rejects tokens signed with a different secret', () => {
    const token = signTicket(ticket(), 'some-other-secret-xxxxxx');
    expect(() => verifyToken(token, SECRET)).toThrowError(ApiError);
  });

  it('rejects expired tokens with TOKEN_EXPIRED', () => {
    const token = signTicket(ticket({ e: Math.floor(Date.now() / 1000) - 10 }), SECRET);
    try {
      verifyToken(token, SECRET);
      expect.unreachable();
    } catch (err) {
      expect((err as ApiError).code).toBe('TOKEN_EXPIRED');
    }
  });

  it('rejects structurally invalid tokens', () => {
    expect(() => verifyToken('garbage', SECRET)).toThrowError(ApiError);
    expect(() => verifyToken('a.b.c', SECRET)).toThrowError(ApiError);
    expect(() => verifyToken('', SECRET)).toThrowError(ApiError);
  });
});
