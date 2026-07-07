import type { HistoryEntry } from '../lib/history';
import { timeAgo } from '../lib/format';
import { PLATFORM_META } from '../lib/platform';

interface HistoryListProps {
  entries: HistoryEntry[];
  onSelect: (url: string) => void;
  onClear: () => void;
}

export function HistoryList({ entries, onSelect, onClear }: HistoryListProps) {
  if (entries.length === 0) return null;

  return (
    <section className="history" aria-label="Recent downloads">
      <div className="history-head">
        <h2>Recent</h2>
        <button type="button" className="btn btn-ghost" onClick={onClear} style={{ padding: '6px 12px', fontSize: 13 }}>
          Clear
        </button>
      </div>
      {entries.map((entry, index) => (
        <button
          key={`${entry.sourceUrl}-${entry.savedAt}-${index}`}
          type="button"
          className="history-item"
          onClick={() => onSelect(entry.sourceUrl)}
          title="Fetch this link again"
        >
          {entry.thumbnailUrl ? (
            <img src={entry.thumbnailUrl} alt="" loading="lazy" referrerPolicy="no-referrer" />
          ) : (
            <span style={{ width: 64 }} aria-hidden="true" />
          )}
          <span className="hi-main">
            <span className="hi-title">{entry.title}</span>
            <span className="hi-sub">
              {PLATFORM_META[entry.platform].name} · {entry.qualityLabel} ·{' '}
              {entry.container.toUpperCase()} · {timeAgo(entry.savedAt)}
            </span>
          </span>
        </button>
      ))}
    </section>
  );
}
