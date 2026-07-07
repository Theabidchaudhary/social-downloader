import { useState } from 'react';
import type { ResolvedFormat, ResolvedMedia } from '../lib/api';
import { formatBytes, formatDuration } from '../lib/format';
import { PLATFORM_META } from '../lib/platform';

interface MediaResultProps {
  media: ResolvedMedia;
  onDownload: (format: ResolvedFormat) => void;
}

export function MediaResult({ media, onDownload }: MediaResultProps) {
  const [started, setStarted] = useState<Set<string>>(new Set());

  const start = (format: ResolvedFormat) => {
    onDownload(format);
    setStarted((prev) => new Set(prev).add(format.id));
  };

  return (
    <section className="result" aria-label="Download options">
      <div className="result-media">
        <div className="thumb">
          {media.thumbnailUrl && (
            <img src={media.thumbnailUrl} alt="" loading="lazy" referrerPolicy="no-referrer" />
          )}
          {media.durationSeconds !== null && (
            <span className="duration">{formatDuration(media.durationSeconds)}</span>
          )}
        </div>
        <div className="result-meta">
          <span className="title">{media.title}</span>
          {media.uploader && <span className="byline">{media.uploader}</span>}
          <span className="badge">
            {PLATFORM_META[media.platform].name} · {media.kind}
          </span>
        </div>
      </div>

      <div className="format-groups">
        {media.video.length > 0 && (
          <div className="format-group">
            <h3>Video · MP4</h3>
            <div className="format-grid">
              {media.video.map((f) => (
                <FormatButton key={f.id} format={f} started={started.has(f.id)} onClick={() => start(f)} />
              ))}
            </div>
          </div>
        )}
        {media.audio.length > 0 && (
          <div className="format-group">
            <h3>Audio</h3>
            <div className="format-grid">
              {media.audio.map((f) => (
                <FormatButton key={f.id} format={f} started={started.has(f.id)} onClick={() => start(f)} />
              ))}
            </div>
          </div>
        )}
      </div>
    </section>
  );
}

function FormatButton({
  format,
  started,
  onClick,
}: {
  format: ResolvedFormat;
  started: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      className={`format-btn${started ? ' started' : ''}`}
      onClick={onClick}
      aria-label={`Download ${format.qualityLabel}, ${formatBytes(format.sizeBytes, format.sizeIsEstimate)}`}
    >
      <span>
        <span className="q">{format.qualityLabel}</span>
        <span className="sub">
          {format.container.toUpperCase()}
          {format.requiresProcessing ? ' · processed' : ''}
        </span>
      </span>
      <span className="size">{formatBytes(format.sizeBytes, format.sizeIsEstimate)}</span>
    </button>
  );
}
