import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  ApiRequestError,
  resolve,
  type ResolvedFormat,
  type ResolveResult,
} from './lib/api';
import { detectPlatformClient } from './lib/platform';
import { addHistory, clearHistory, loadHistory, type HistoryEntry } from './lib/history';
import { Footer } from './components/Footer';
import { Header } from './components/Header';
import { HistoryList } from './components/HistoryList';
import { MediaResult } from './components/MediaResult';
import { PlatformChips } from './components/PlatformChips';
import { UrlBar } from './components/UrlBar';
import { PlaylistResult } from './components/PlaylistResult';

type Phase =
  | { name: 'idle' }
  | { name: 'loading' }
  | { name: 'ready'; result: ResolveResult }
  | { name: 'error'; message: string };

export default function App() {
  const [url, setUrl] = useState('');
  const [phase, setPhase] = useState<Phase>({ name: 'idle' });
  const [history, setHistory] = useState<HistoryEntry[]>(() => loadHistory());
  const abortRef = useRef<AbortController | null>(null);

  const detected = useMemo(() => detectPlatformClient(url), [url]);

  const fetchUrl = useCallback(async (target: string) => {
    const trimmed = target.trim();
    if (!trimmed) return;

    abortRef.current?.abort();
    const controller = new AbortController();
    abortRef.current = controller;

    setPhase({ name: 'loading' });
    try {
      const result = await resolve(trimmed, controller.signal);
      setPhase({ name: 'ready', result });
    } catch (err) {
      if (err instanceof DOMException && err.name === 'AbortError') return;
      setPhase({
        name: 'error',
        message:
          err instanceof ApiRequestError ? err.message : 'Something went wrong. Try again.',
      });
    }
  }, []);

  const handleSubmit = useCallback(() => void fetchUrl(url), [fetchUrl, url]);

  const handlePickEntry = useCallback(
    (entryUrl: string) => {
      setUrl(entryUrl);
      void fetchUrl(entryUrl);
    },
    [fetchUrl],
  );

  const handleHistorySelect = useCallback(
    (sourceUrl: string) => {
      setUrl(sourceUrl);
      void fetchUrl(sourceUrl);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    },
    [fetchUrl],
  );

  const handleDownload = useCallback(
    (format: ResolvedFormat) => {
      if (phase.name !== 'ready' || phase.result.type !== 'media') return;
      const media = phase.result;

      // A plain same-origin GET with Content-Disposition: attachment — the
      // browser (or IDM, if installed and intercepting) takes it from here.
      const anchor = document.createElement('a');
      anchor.href = format.downloadUrl;
      anchor.rel = 'noopener';
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();

      setHistory(
        addHistory({
          sourceUrl: media.sourceUrl,
          title: media.title,
          platform: media.platform,
          thumbnailUrl: media.thumbnailUrl,
          qualityLabel: format.qualityLabel,
          container: format.container,
          savedAt: Date.now(),
        }),
      );
    },
    [phase],
  );

  useEffect(() => () => abortRef.current?.abort(), []);

  return (
    <div className="page">
      <Header />

      <main>
        <section className="shell hero">
          <h1>
            Save any video.
            <br />
            <span className="accent">In seconds.</span>
          </h1>
          <p>
            Paste a link from YouTube, Instagram, TikTok, X or Facebook. Pick a quality — MP4 or
            MP3 — and your download starts instantly.
          </p>

          <UrlBar value={url} busy={phase.name === 'loading'} onChange={setUrl} onSubmit={handleSubmit} />
          <PlatformChips active={detected} />

          {phase.name === 'loading' && (
            <div className="status" role="status">
              <span className="spinner" aria-hidden="true" />
              Reading the link and listing available qualities…
            </div>
          )}

          {phase.name === 'error' && (
            <div className="error-banner" role="alert">
              {phase.message}
            </div>
          )}

          {phase.name === 'ready' && phase.result.type === 'media' && (
            <MediaResult media={phase.result} onDownload={handleDownload} />
          )}

          {phase.name === 'ready' && phase.result.type === 'playlist' && (
            <PlaylistResult playlist={phase.result} onPickEntry={handlePickEntry} />
          )}

          <HistoryList
            entries={history}
            onSelect={handleHistorySelect}
            onClear={() => setHistory(clearHistory())}
          />
        </section>

        <section className="shell section" id="how-it-works">
          <h2>How it works</h2>
          <div className="steps">
            <div className="step">
              <span className="num">1</span>
              <h3>Paste a link</h3>
              <p>
                Copy any video URL and paste it above. Siphon detects the platform automatically —
                videos, Shorts, Reels and playlists.
              </p>
            </div>
            <div className="step">
              <span className="num">2</span>
              <h3>Pick a quality</h3>
              <p>
                See the thumbnail, title and every available resolution with estimated file sizes —
                plus MP3 audio extraction.
              </p>
            </div>
            <div className="step">
              <span className="num">3</span>
              <h3>Download instantly</h3>
              <p>
                One click starts the download in your browser. Works seamlessly with download
                managers like IDM.
              </p>
            </div>
          </div>
        </section>
      </main>

      <Footer />
    </div>
  );
}
