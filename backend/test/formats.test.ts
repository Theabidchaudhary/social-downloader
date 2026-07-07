import { describe, expect, it } from 'vitest';
import { buildFormatOptions } from '../src/services/formats.js';
import type { YtDlpInfo } from '../src/lib/ytdlp.js';

const baseInfo: YtDlpInfo = {
  id: 'abc',
  title: 'Test',
  duration: 100,
  formats: [],
};

describe('buildFormatOptions', () => {
  it('prefers progressive formats and exposes direct URLs for them', () => {
    const info: YtDlpInfo = {
      ...baseInfo,
      formats: [
        {
          format_id: '22',
          ext: 'mp4',
          vcodec: 'avc1',
          acodec: 'mp4a',
          height: 720,
          width: 1280,
          filesize: 50_000_000,
          url: 'https://cdn.example.com/720.mp4',
          protocol: 'https',
        },
        {
          format_id: '136',
          ext: 'mp4',
          vcodec: 'avc1',
          acodec: 'none',
          height: 720,
          width: 1280,
          filesize: 40_000_000,
          protocol: 'https',
        },
      ],
    };
    const { video } = buildFormatOptions(info);
    expect(video).toHaveLength(1);
    expect(video[0]?.directUrl).toBe('https://cdn.example.com/720.mp4');
    expect(video[0]?.requiresProcessing).toBe(false);
    expect(video[0]?.qualityLabel).toBe('720p');
  });

  it('emits server-muxed options for video-only heights and adds audio size to the estimate', () => {
    const info: YtDlpInfo = {
      ...baseInfo,
      formats: [
        {
          format_id: '137',
          ext: 'mp4',
          vcodec: 'avc1',
          acodec: 'none',
          height: 1080,
          filesize: 100_000_000,
          protocol: 'https',
        },
        {
          format_id: '140',
          ext: 'm4a',
          vcodec: 'none',
          acodec: 'mp4a',
          abr: 128,
          filesize: 1_600_000,
          url: 'https://cdn.example.com/audio.m4a',
          protocol: 'https',
        },
      ],
    };
    const { video, audio } = buildFormatOptions(info);
    const v1080 = video.find((v) => v.height === 1080);
    expect(v1080?.requiresProcessing).toBe(true);
    expect(v1080?.directUrl).toBeNull();
    expect(v1080?.sizeBytes).toBe(101_600_000);
    expect(v1080?.sizeIsEstimate).toBe(true);

    const m4a = audio.find((a) => a.container === 'm4a');
    expect(m4a?.directUrl).toBe('https://cdn.example.com/audio.m4a');
    const mp3s = audio.filter((a) => a.container === 'mp3');
    expect(mp3s.map((m) => m.qualityLabel)).toEqual([
      'MP3 · 320 kbps',
      'MP3 · 192 kbps',
      'MP3 · 128 kbps',
    ]);
  });

  it('estimates size from tbr and duration when filesize is missing', () => {
    const info: YtDlpInfo = {
      ...baseInfo,
      duration: 200,
      formats: [
        {
          format_id: '0',
          ext: 'mp4',
          vcodec: 'h264',
          acodec: 'aac',
          height: 540,
          tbr: 1200,
          protocol: 'https',
        },
      ],
    };
    const { video } = buildFormatOptions(info);
    expect(video[0]?.sizeBytes).toBe(Math.round((1200 * 1000 * 200) / 8));
    expect(video[0]?.sizeIsEstimate).toBe(true);
  });

  it('labels high-fps formats', () => {
    const info: YtDlpInfo = {
      ...baseInfo,
      formats: [
        {
          format_id: '299',
          ext: 'mp4',
          vcodec: 'avc1',
          acodec: 'none',
          height: 1080,
          fps: 60,
          filesize: 1,
          protocol: 'https',
        },
      ],
    };
    const { video } = buildFormatOptions(info);
    expect(video[0]?.qualityLabel).toBe('1080p60');
  });

  it('sorts video options from highest to lowest resolution', () => {
    const mk = (id: string, height: number) => ({
      format_id: id,
      ext: 'mp4',
      vcodec: 'avc1',
      acodec: 'mp4a',
      height,
      filesize: 1,
      protocol: 'https',
    });
    const info: YtDlpInfo = { ...baseInfo, formats: [mk('a', 360), mk('b', 1080), mk('c', 720)] };
    const { video } = buildFormatOptions(info);
    expect(video.map((v) => v.height)).toEqual([1080, 720, 360]);
  });
});
