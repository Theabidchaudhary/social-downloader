# Siphon — Testing strategy

## Layers

| Layer | Tooling | What it covers | Command |
| --- | --- | --- | --- |
| Backend unit | Vitest | platform detection, token signing/verification, format mapping & size estimation, filename sanitization | `cd backend && npm test` |
| Backend types | tsc strict | whole service | `npm run typecheck` |
| Web build/types | tsc strict + Vite | whole app compiles; bundle builds | `cd web && npm run build` |
| Android unit | JUnit 4 | `UrlDetector` (platform/URL extraction), `Formatters` (bytes/duration/ETA/progress) | `cd android && ./gradlew testDebugUnitTest` |
| Integration (manual/CI with network) | curl / instrumented device | end-to-end resolve → download per platform | checklist below |

Unit tests deliberately concentrate on the **pure decision logic** (what is
supported, what tokens are valid, which formats surface, what the user sees) —
the parts that break silently. Transport code paths are covered by the
integration checklist because they depend on live platforms.

## Backend integration checklist (needs network + yt-dlp)

For each platform — YouTube video, YouTube Short, YouTube playlist, Instagram
reel, TikTok video, X video, Facebook video/reel:

1. `POST /resolve` returns thumbnail, title, ≥1 MP4 option, MP3 options.
2. Progressive option downloads via `GET /download` and plays.
3. A muxed 1080p option downloads as a valid MP4 (ffmpeg present).
4. An MP3 option downloads and plays at the chosen bitrate.
5. Expired token replays return `TOKEN_EXPIRED` (set `TOKEN_TTL_SECONDS=5`).
6. Unsupported host returns `UNSUPPORTED_PLATFORM`; garbage returns `INVALID_URL`.
7. Hammering resolve trips `RATE_LIMITED`.

## Android QA checklist

**Share sheet (flagship):** share from each supported app → sheet appears over
the app → options within ~2 s on Wi-Fi → pick 720p → toast + background
download → original app still foregrounded. Repeat with MP3 and with a
playlist link (entry picker).

**Download manager:** parallel limit respected (set 2, enqueue 4 → 2 running,
2 queued); pause mid-transfer → resume continues from offset on direct URLs;
kill the app mid-download → reopen → job re-queued and completes; airplane
mode mid-download → FAILED with readable error → retry succeeds; cancel
removes partial file; completed files open from the list and appear in the
chosen folder (Downloads/Movies/Music/custom SAF tree incl. SD card);
notification progress matches in-app numbers; Android 13 notification
permission denial doesn't crash anything.

**Settings:** theme switch is instant; download location persists across
restarts; clipboard detection only fires when enabled; per-app language
switch applies.

**Accessibility pass:** TalkBack announces format options with size, all
touch targets ≥ 48 dp, contrast on `ink` surfaces ≥ 4.5:1 for body text.

## CI recommendation

- Backend: `npm ci && npm run typecheck && npm test` on every PR (no network
  needed).
- Web: `npm ci && npm run build`.
- Android: `./gradlew testDebugUnitTest lintDebug` on a runner with the
  Android SDK; assemble a debug APK as an artifact.
- Nightly (networked runner): the backend integration checklist against a
  small pinned URL set, so extractor breakage from platform changes is caught
  within a day.
