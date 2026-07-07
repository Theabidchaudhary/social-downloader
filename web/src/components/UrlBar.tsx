import { useCallback, useRef } from 'react';

interface UrlBarProps {
  value: string;
  busy: boolean;
  onChange: (value: string) => void;
  onSubmit: () => void;
}

export function UrlBar({ value, busy, onChange, onSubmit }: UrlBarProps) {
  const inputRef = useRef<HTMLInputElement>(null);

  const handlePaste = useCallback(async () => {
    try {
      const text = await navigator.clipboard.readText();
      if (text) {
        onChange(text.trim());
        inputRef.current?.focus();
      }
    } catch {
      // Clipboard permission denied — just focus so the user can Ctrl+V.
      inputRef.current?.focus();
    }
  }, [onChange]);

  return (
    <form
      className="urlbar"
      onSubmit={(e) => {
        e.preventDefault();
        onSubmit();
      }}
    >
      <input
        ref={inputRef}
        type="url"
        inputMode="url"
        autoComplete="off"
        spellCheck={false}
        placeholder="Paste a video link — YouTube, Instagram, TikTok, X, Facebook"
        aria-label="Video URL"
        value={value}
        onChange={(e) => onChange(e.target.value)}
      />
      <button type="button" className="btn btn-ghost" onClick={handlePaste}>
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <rect x="8" y="3" width="8" height="4" rx="1.5" stroke="currentColor" strokeWidth="2" />
          <path
            d="M8 5H6.5A1.5 1.5 0 0 0 5 6.5v13A1.5 1.5 0 0 0 6.5 21h11a1.5 1.5 0 0 0 1.5-1.5v-13A1.5 1.5 0 0 0 17.5 5H16"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
          />
        </svg>
        Paste
      </button>
      <button type="submit" className="btn btn-primary" disabled={busy || value.trim() === ''}>
        {busy ? 'Fetching…' : 'Fetch'}
      </button>
    </form>
  );
}
