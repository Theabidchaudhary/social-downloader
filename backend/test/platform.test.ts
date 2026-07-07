import { describe, expect, it } from 'vitest';
import { detectPlatform } from '../src/lib/platform.js';

describe('detectPlatform', () => {
  it('detects standard YouTube watch URLs', () => {
    const d = detectPlatform('https://www.youtube.com/watch?v=dQw4w9WgXcQ');
    expect(d).toMatchObject({ platform: 'youtube', kind: 'video' });
  });

  it('detects youtu.be short links and schemeless input', () => {
    expect(detectPlatform('youtu.be/dQw4w9WgXcQ')).toMatchObject({ platform: 'youtube', kind: 'video' });
  });

  it('detects YouTube Shorts', () => {
    expect(detectPlatform('https://youtube.com/shorts/abc123XYZ_-')).toMatchObject({
      platform: 'youtube',
      kind: 'short',
    });
  });

  it('detects YouTube playlists', () => {
    expect(detectPlatform('https://www.youtube.com/playlist?list=PL123')).toMatchObject({
      platform: 'youtube',
      kind: 'playlist',
    });
  });

  it('treats watch URLs that carry a list param as single videos', () => {
    expect(detectPlatform('https://www.youtube.com/watch?v=abc&list=PL123')).toMatchObject({
      platform: 'youtube',
      kind: 'video',
    });
  });

  it('detects Instagram reels and posts', () => {
    expect(detectPlatform('https://www.instagram.com/reel/Cxyz123/')).toMatchObject({
      platform: 'instagram',
      kind: 'reel',
    });
    expect(detectPlatform('https://www.instagram.com/p/Cxyz123/')).toMatchObject({
      platform: 'instagram',
      kind: 'video',
    });
  });

  it('detects TikTok videos and short links', () => {
    expect(detectPlatform('https://www.tiktok.com/@user/video/7123456789012345678')).toMatchObject({
      platform: 'tiktok',
      kind: 'video',
    });
    expect(detectPlatform('https://vm.tiktok.com/ZM8abcdef/')).toMatchObject({
      platform: 'tiktok',
      kind: 'video',
    });
    expect(detectPlatform('https://www.tiktok.com/@someuser')).toMatchObject({
      platform: 'tiktok',
      kind: 'unknown',
    });
  });

  it('detects X / Twitter status URLs on both domains', () => {
    expect(detectPlatform('https://x.com/user/status/1234567890')).toMatchObject({
      platform: 'twitter',
      kind: 'video',
    });
    expect(detectPlatform('https://twitter.com/user/status/1234567890')).toMatchObject({
      platform: 'twitter',
      kind: 'video',
    });
  });

  it('detects Facebook videos, reels and fb.watch', () => {
    expect(detectPlatform('https://www.facebook.com/user/videos/123456/')).toMatchObject({
      platform: 'facebook',
      kind: 'video',
    });
    expect(detectPlatform('https://fb.watch/abc123/')).toMatchObject({ platform: 'facebook', kind: 'video' });
    expect(detectPlatform('https://www.facebook.com/reel/123456')).toMatchObject({
      platform: 'facebook',
      kind: 'reel',
    });
  });

  it('rejects unsupported hosts and junk', () => {
    expect(detectPlatform('https://vimeo.com/12345')).toBeNull();
    expect(detectPlatform('not a url')).toBeNull();
    expect(detectPlatform('')).toBeNull();
    expect(detectPlatform('ftp://youtube.com/watch?v=x')).toBeNull();
    expect(detectPlatform('https://notyoutube.com/watch?v=x')).toBeNull();
    expect(detectPlatform('https://evilyoutu.be.example.com/x')).toBeNull();
  });

  it('strips tracking params but keeps identity params in the canonical URL', () => {
    const d = detectPlatform('https://www.youtube.com/watch?v=abc&utm_source=share&si=xyz');
    expect(d?.canonicalUrl).toBe('https://www.youtube.com/watch?v=abc');
  });
});
