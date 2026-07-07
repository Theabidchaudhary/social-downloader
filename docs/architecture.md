# Siphon — Architecture

## System overview

```
┌────────────┐        ┌────────────┐
│  Web app    │        │ Android app │
│ (React/Vite)│        │  (Compose)  │
└──────┬─────┘        └──────┬──────┘
       │  POST /api/v1/resolve │
       │  GET  /api/v1/download│        direct CDN URL (Range)
       ▼                      ▼        ┌──────────────────────┐
┌──────────────────────────────┐      │ Platform CDNs         │
│ Siphon API (Fastify, Node 20)│      │ (YouTube, TikTok, …)  │
│  • platform detection        │◄─────┤ progressive files     │
│  • yt-dlp metadata extraction│      └──────────────────────┘
│  • signed download tokens    │
│  • yt-dlp/ffmpeg streaming   │
└──────────────────────────────┘
```

Two request kinds exist:

1. **Resolve** — `POST /api/v1/resolve { url }`. The API detects the platform,
   runs `yt-dlp --dump-single-json`, normalizes formats into a compact option
   list (MP4 per resolution + M4A passthrough + MP3 transcodes) with size
   estimates, and attaches a **signed download URL** per option.
2. **Download** — `GET /api/v1/download?token=…`. The token (HMAC-SHA256,
   expiring) encodes the source URL and the yt-dlp format selector. The server
   fetches/muxes/transcodes into a per-request temp dir, then streams the file
   with exact `Content-Length` and `Content-Disposition` — which is what makes
   browser download UIs and interceptors like IDM work.

### Why signed tokens?

The download endpoint would otherwise be an open proxy ("download any URL as
any format"). Tokens are minted only by resolve, expire (default 30 min), and
pin URL + format + container. Verification is stateless (HMAC), so any replica
can serve any token — no sticky sessions, no shared session store.

### Why buffer-to-disk instead of piping yt-dlp stdout?

- MP4 muxing (1080p+ DASH video + audio) and MP3 transcoding need a seekable
  output; they cannot be produced on a pipe at all.
- A temp file gives an exact `Content-Length`, so clients render real progress
  bars and segmented downloaders behave.
- Disk is reclaimed per request (`siphon-*` temp dirs removed on stream close).

### Direct URLs for the Android client

For progressive sources (most TikTok/Instagram/X/Facebook videos, YouTube's
progressive renditions) resolve also exposes the CDN's `directUrl`. The
Android download manager prefers it because CDNs honor `Range`, enabling true
pause/resume. Server-processed options (`requiresProcessing: true`) have no
direct URL and fall back to the API proxy.

### Backpressure & abuse controls

| Control | Mechanism |
| --- | --- |
| Extraction concurrency | FIFO semaphore (`MAX_CONCURRENT_EXTRACTIONS`) |
| Stream concurrency | FIFO semaphore (`MAX_CONCURRENT_STREAMS`) |
| Per-IP rate limit | `@fastify/rate-limit` (default 30 req/min) |
| Download size cap | `MAX_DOWNLOAD_SIZE_MB` enforced by yt-dlp and post-stat |
| Resolve cache | In-memory TTL cache keyed by canonical URL; tokens re-signed on hit |
| Input hygiene | zod-validated bodies, 16 KB body limit, URL canonicalization |

### Error model

Every client-visible failure is an `ApiError` with a stable `code`
(`UNSUPPORTED_PLATFORM`, `EXTRACTION_FAILED`, `TOKEN_EXPIRED`, …) so clients
localize and branch without string matching. Raw yt-dlp stderr goes to server
logs only.

### Scaling path

The API is stateless (cache is an optimization, not a dependency), so it
scales horizontally behind any load balancer. The natural upgrade points, in
order: move the resolve cache to Redis, move heavy downloads to a worker pool
consuming a queue (BullMQ) with the API returning job status, and put the
proxy behind a CDN-adjacent egress pool. None of these change the public API.

---

## Web app

Single-page Vite + React + TypeScript app. No state library — the app is one
state machine (`idle → loading → ready | error`) plus a localStorage-backed
history list. The design system is plain CSS custom properties
(`styles/tokens.css`), mirrored 1:1 by the Android theme.

Download start is a plain same-origin anchor click on the signed URL: the
browser (or IDM, when installed and intercepting) owns the transfer from
there. Client-side platform detection lights up the platform chips while
typing; the server remains authoritative.

---

## Android app

100 % Kotlin + Jetpack Compose, MVVM, unidirectional data flow. **No WebView
anywhere.**

```
ui/ (Compose screens + ViewModels)
 └── data/repo (MediaRepository, DownloadRepository)
      ├── data/remote (OkHttp + kotlinx.serialization client)
      ├── data/db (Room: downloads table = queue + history)
      └── data/settings (DataStore preferences)
download/ (foreground DownloadService + Downloader + notifications)
util/ (UrlDetector, StorageSink, formatters, intents)
```

### Dependency injection

A hand-rolled composition root (`di/AppContainer`) instead of Hilt: the graph
is ~8 objects, construction stays explicit and debuggable, and the build needs
no annotation processors beyond Room's. ViewModels get the container through
`viewModelFactory` initializers. If the graph grows past a screenful, swapping
in Hilt is mechanical.

### Download manager

- **Queue & history are one Room table.** Status transitions:
  `QUEUED → RUNNING → COMPLETED | FAILED | PAUSED | CANCELED`. The Downloads
  screen is a single reactive query with in-memory filter/search/sort.
- **DownloadService** is a `dataSync` foreground service. It fills up to
  `maxParallelDownloads` slots (user setting, 1–5), picks up queued rows,
  and stops itself when idle. Rows left `RUNNING` by a process death are
  re-queued on service start.
- **Pause vs cancel** both cancel the job coroutine; a flag decides whether
  the row becomes `PAUSED` (partial file kept; resume sends `Range`) or
  `CANCELED` (partial file deleted).
- **Expired links**: rows store the source URL + chosen quality, not just the
  transport URLs. On 403/410/404 the job re-resolves once and retries with
  fresh URLs; retry-after-failure does the same from scratch.
- **Storage** (`util/StorageSink`) writes to MediaStore collections
  (Downloads/Movies/Music, `IS_PENDING` until finalized) or a persisted SAF
  tree for custom folders and SD cards. The chosen location is remembered in
  DataStore.
- **Verification**: a download only completes if it received every byte of the
  server's `Content-Length`; resolve-time size *estimates* never fail a file.
- **Notifications**: one foreground summary + one silent progress notification
  per download (speed, ETA), completion/failure alerts, all behind the user's
  notification toggle and Android 13's runtime permission.

### Share-sheet flow

`ShareActivity` is `exported` for `ACTION_SEND text/plain`, runs with a fully
transparent window (`Theme.Siphon.Share`), `taskAffinity=""`,
`excludeFromRecents` and `noHistory`, so it renders as a bottom sheet floating
over the sharing app. It extracts the first URL from the shared text, resolves
it, and shows the same `MediaOptionsContent` composable used on Home. Picking
a format enqueues into the same repository/service and finishes the activity
with no transition — the user never leaves the original app.

### Performance notes

- Progress writes are throttled to 500 ms and speed is exponentially smoothed;
  the UI recomposes from Room flows only when rows actually change.
- Lists use `LazyColumn` with stable keys; thumbnails load through Coil with
  crossfade-free placeholders to avoid jank.
- The APK stays lean: no Retrofit/Moshi/Hilt, R8 + resource shrinking on.
