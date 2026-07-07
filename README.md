<p align="center">
  <img src="assets/brand/logo.svg" alt="Siphon" width="360" />
</p>

<p align="center"><strong>Save any video. In seconds.</strong></p>

<p align="center">
Siphon is a premium video downloader ecosystem: a SaaS-quality web app, a fully
native Android app, and a shared API — one brand, one design system, three surfaces.
</p>

---

## What it does

Paste (or share) a link from **YouTube, Instagram, TikTok, X or Facebook** and
Siphon shows the thumbnail, title and every available rendition — MP4 in each
resolution with estimated file sizes, plus MP3/M4A audio extraction. One click
or tap starts the download. Playlists resolve into a browsable entry list.

| Surface | Stack | Highlights |
| --- | --- | --- |
| `backend/` | Node 20 · TypeScript · Fastify · yt-dlp · ffmpeg | Signed expiring download URLs, rate limiting, structured logs, concurrency gates, Docker |
| `web/` | Vite · React 18 · TypeScript | The actual app (not a landing page): paste → preview → pick quality → instant download, IDM-compatible, local history |
| `android/` | Kotlin · Jetpack Compose · Room · DataStore · OkHttp | Native share-sheet flow, foreground download manager (queue/pause/resume/retry), MediaStore + SAF storage |

## The flagship interaction (Android)

Tap **Share** in YouTube/Instagram/TikTok/X/Facebook → pick **Siphon** → a
translucent native bottom sheet appears *over the current app* with thumbnail,
title, qualities and sizes → tap one → the download starts in the background,
the sheet closes, and you're still exactly where you were.

## Quick start

### 1. Backend

Requires Node ≥ 20, plus [`yt-dlp`](https://github.com/yt-dlp/yt-dlp) and
`ffmpeg` on `PATH`.

```bash
cd backend
cp .env.example .env          # set SIGNING_SECRET
npm install
npm run dev                   # http://localhost:8787
npm test                      # 26 unit tests
```

Or with Docker (bundles yt-dlp + ffmpeg):

```bash
cd backend
SIGNING_SECRET=$(openssl rand -hex 32) docker compose up --build
```

### 2. Web app

```bash
cd web
npm install
npm run dev                   # http://localhost:5173, proxies /api to :8787
```

### 3. Android app

Open `android/` in Android Studio (Koala+), or:

```bash
cd android
./gradlew :app:assembleDebug  # debug builds point at http://10.0.2.2:8787
./gradlew :app:testDebugUnitTest
```

Release builds read the API base URL from the `siphonApiBaseUrl` Gradle
property: `./gradlew :app:assembleRelease -PsiphonApiBaseUrl=https://api.example.com`.

## Repository layout

```
siphon/
├── assets/brand/      # logo, app icon (SVG sources of truth)
├── backend/           # Fastify API — resolve + signed download streaming
├── web/               # React web application
├── android/           # Native Android app (Compose)
└── docs/
    ├── architecture.md    # system design, data flow, decisions
    ├── api.md             # REST API reference
    ├── branding.md        # name, palette, typography, design language
    ├── deployment.md      # Docker, reverse proxy, Play packaging
    └── testing.md         # test strategy & QA checklists
```

## Documentation

- [Architecture](docs/architecture.md)
- [API reference](docs/api.md)
- [Branding & design system](docs/branding.md)
- [Deployment](docs/deployment.md)
- [Testing strategy](docs/testing.md)

## Legal

Siphon is a tool for saving content you own or are permitted to download.
Downloading may violate a platform's terms of service or the rights of content
owners; users are responsible for how they use it. The project ships with
rate limiting, signed URLs and size caps specifically so operators can run it
responsibly.
