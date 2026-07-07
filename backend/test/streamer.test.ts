import { describe, expect, it } from 'vitest';
import { sanitizeFileName } from '../src/services/streamer.js';

describe('sanitizeFileName', () => {
  it('strips path separators and reserved characters', () => {
    expect(sanitizeFileName('a/b\\c:d*e?f"g<h>i|j')).toBe('a b c d e f g h i j');
  });

  it('collapses whitespace and trims trailing dots', () => {
    expect(sanitizeFileName('  My   Video... ')).toBe('My Video');
  });

  it('falls back for empty input', () => {
    expect(sanitizeFileName('///')).toBe('siphon-download');
  });

  it('keeps unicode titles intact', () => {
    expect(sanitizeFileName('動画タイトル — テスト')).toBe('動画タイトル — テスト');
  });

  it('caps length at 120 characters', () => {
    expect(sanitizeFileName('x'.repeat(300)).length).toBeLessThanOrEqual(120);
  });
});
