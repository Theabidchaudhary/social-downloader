/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Absolute origin of the Siphon API, e.g. "https://api.example.com". Empty = same-origin. */
  readonly VITE_API_BASE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
