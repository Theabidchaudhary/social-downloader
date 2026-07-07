# Siphon — Deployment

## Backend

### Docker (recommended)

The image bundles Node 20, yt-dlp and ffmpeg:

```bash
cd backend
SIGNING_SECRET=$(openssl rand -hex 32) \
ALLOWED_ORIGINS=https://siphon.example.com \
docker compose up --build -d
```

Operational notes:

- **/tmp sizing** — proxied downloads buffer to `/tmp`; the compose file
  mounts an 8 GB tmpfs. Size it ≥ `MAX_CONCURRENT_STREAMS ×` your typical
  largest file:
- **Keep yt-dlp fresh** — platforms break extractors regularly. Rebuild the
  image (or `pip install -U yt-dlp` in a derived image) on a weekly schedule;
  `/readyz` verifies the binary still runs.
- **Health probes** — liveness `GET /healthz`, readiness `GET /readyz`.
- **Scaling** — the service is stateless; run N replicas behind a load
  balancer. All replicas must share the same `SIGNING_SECRET`.

### Bare metal / VM

```bash
apt install ffmpeg && pip install yt-dlp
cd backend && npm ci && npm run build
NODE_ENV=production SIGNING_SECRET=… node dist/index.js
```

Run under systemd/PM2; give the process user a dedicated tmp dir if you want
quotas.

## Web

```bash
cd web && npm ci && npm run build   # emits web/dist
```

Serve `dist/` from any static host and reverse-proxy `/api` to the backend so
both share one origin (keeps CORS off and signed URLs relative). nginx sketch:

```nginx
server {
  listen 443 ssl http2;
  server_name siphon.example.com;

  root /srv/siphon/web/dist;
  location / { try_files $uri /index.html; }

  location /api/ {
    proxy_pass http://127.0.0.1:8787;
    proxy_http_version 1.1;
    proxy_buffering off;          # stream downloads as they're produced
    proxy_read_timeout 1200s;     # large muxes take time
    client_max_body_size 32k;
  }
}
```

If you host web and API on different origins instead (e.g. Render, Netlify +
a standalone API host — see `docs/hosting-quickstart.md` §0.5 for a concrete
walkthrough), set two things:

- API: `ALLOWED_ORIGINS` to the web app's exact origin.
- Web: `VITE_API_BASE_URL` at build time to the API's absolute origin
  (`VITE_API_BASE_URL=https://api.example.com npm run build`, or as a build
  env var on your hosting platform). See `web/.env.example`.

## Android

- **Debug** builds target `http://10.0.2.2:8787` (emulator loopback).
- **Release**: `./gradlew :app:assembleRelease -PsiphonApiBaseUrl=https://api.siphon.example.com`
  — the URL is baked in via `BuildConfig`. Sign with your keystore
  (`signingConfigs` or Play App Signing) and ship the `aab` from
  `:app:bundleRelease` to Play.
- The app needs no Google services; it runs on any Android 8.0+ device.
- First build downloads the Gradle 8.9 distribution via the checked-in
  wrapper (`./gradlew`).

## Environment matrix

| Variable | Dev default | Production guidance |
| --- | --- | --- |
| `SIGNING_SECRET` | dev fallback (refused in prod) | 32+ random bytes, same on all replicas |
| `ALLOWED_ORIGINS` | `*` | exact web origin(s), comma-separated |
| `TOKEN_TTL_SECONDS` | 1800 | shorten if links are shared publicly |
| `MAX_DOWNLOAD_SIZE_MB` | 4096 | match your tmpfs budget |
| `RATE_LIMIT_MAX` / window | 30/min | tune per audience; put a CDN/WAF in front for real abuse |
| `LOG_LEVEL` | info | `info`; logs are JSON (pino) — ship to your aggregator |
