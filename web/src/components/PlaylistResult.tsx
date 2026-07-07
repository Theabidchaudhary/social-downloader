import type { ResolvedPlaylist } from '../lib/api';
import { formatDuration } from '../lib/format';
import { PLATFORM_META } from '../lib/platform';

interface PlaylistResultProps {
  playlist: ResolvedPlaylist;
  onPickEntry: (url: string) => void;
}

export function PlaylistResult({ playlist, onPickEntry }: PlaylistResultProps) {
  return (
    <section className="result" aria-label="Playlist contents">
      <div className="result-media">
        <div className="result-meta">
          <span className="title">{playlist.title}</span>
          <span className="byline">
            {playlist.entryCount} video{playlist.entryCount === 1 ? '' : 's'} — pick one to see its
            download options
          </span>
          <span className="badge">{PLATFORM_META[playlist.platform].name} · playlist</span>
        </div>
      </div>
      <div className="playlist-entries">
        {playlist.entries.map((entry, index) => (
          <button
            key={`${entry.url}-${index}`}
            type="button"
            className="playlist-entry"
            onClick={() => onPickEntry(entry.url)}
          >
            {entry.thumbnailUrl ? (
              <img src={entry.thumbnailUrl} alt="" loading="lazy" referrerPolicy="no-referrer" />
            ) : (
              <span style={{ width: 84 }} aria-hidden="true" />
            )}
            <span className="pe-title">{entry.title}</span>
            <span className="pe-dur">{formatDuration(entry.durationSeconds)}</span>
          </button>
        ))}
      </div>
    </section>
  );
}
