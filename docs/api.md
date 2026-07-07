# Siphon API reference

Base path: `/api/v1`. All responses are JSON unless noted. Errors use a
uniform envelope:

```json
{ "error": { "code": "UNSUPPORTED_PLATFORM", "message": "…" } }
```

| Code | HTTP | Meaning |
| --- | --- | --- |
| `INVALID_URL` | 400 | Input is not a URL |
| `UNSUPPORTED_PLATFORM` | 422 | Valid URL, unsupported site |
| `EXTRACTION_FAILED` | 502 | yt-dlp could not read the source |
| `MEDIA_UNAVAILABLE` | 410 | Source removed/private |
| `TOKEN_INVALID` | 403 | Bad/tampered download token |
| `TOKEN_EXPIRED` | 410 | Signed URL past its TTL |
| `DOWNLOAD_TOO_LARGE` | 413 | Exceeds `MAX_DOWNLOAD_SIZE_MB` |
| `RATE_LIMITED` | 429 | Per-IP limit hit |
| `INTERNAL` | 500 | Unexpected server error |

---

## POST /resolve

Detects the platform, extracts metadata and returns downloadable options.

Request:

```json
{ "url": "https://www.youtube.com/watch?v=dQw4w9WgXcQ" }
```

Response — single media (`type: "media"`):

```json
{
  "type": "media",
  "platform": "youtube",
  "kind": "video",
  "sourceUrl": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
  "title": "…",
  "uploader": "…",
  "durationSeconds": 212,
  "thumbnailUrl": "https://…",
  "video": [
    {
      "id": "v1080-137",
      "kind": "video",
      "container": "mp4",
      "qualityLabel": "1080p",
      "width": 1920, "height": 1080, "fps": 30,
      "sizeBytes": 101600000,
      "sizeIsEstimate": true,
      "directUrl": null,
      "requiresProcessing": true,
      "downloadUrl": "/api/v1/download?token=…"
    }
  ],
  "audio": [
    { "id": "a-m4a-140", "container": "m4a", "qualityLabel": "M4A · 128 kbps", "directUrl": "https://…", "downloadUrl": "/api/v1/download?token=…" },
    { "id": "a-mp3-320", "container": "mp3", "qualityLabel": "MP3 · 320 kbps", "directUrl": null, "downloadUrl": "/api/v1/download?token=…" }
  ],
  "expiresAt": "2026-07-04T10:00:00.000Z"
}
```

Response — playlist (`type: "playlist"`): `title`, `entryCount` and up to 200
`entries` (`title`, `url`, `durationSeconds`, `thumbnailUrl`). Resolve an
entry's `url` to get its options.

Notes:

- `sizeBytes` may be exact (`sizeIsEstimate: false`) or derived from bitrate ×
  duration.
- `directUrl` (when present) is the platform CDN's progressive file — it
  supports `Range` and is what the Android app uses for resumable downloads.
  It expires on the platform's schedule; treat it as short-lived.
- `downloadUrl` is relative to the API origin and valid until `expiresAt`.

## GET /download?token=…

Streams the chosen rendition. Success responds `200` with:

- `Content-Type`: `video/mp4`, `audio/mpeg` or `audio/mp4`
- `Content-Length`: exact byte size
- `Content-Disposition: attachment; filename="…"; filename*=UTF-8''…`
- `Cache-Control: no-store`, `X-Content-Type-Options: nosniff`

Server-side processing (mux/transcode) happens before the first byte is sent,
so very large files have a startup delay proportional to their size.

## GET /platforms

```json
{ "platforms": [ { "id": "youtube", "name": "YouTube", "capabilities": ["video","short","playlist","audio","mp3"] }, … ] }
```

## GET /detect?url=…

Lightweight platform detection (same rules the clients embed):

```json
{ "supported": true, "platform": "youtube", "kind": "short" }
```

## GET /healthz · GET /readyz

`/healthz` is a liveness probe. `/readyz` additionally executes
`yt-dlp --version` and returns `503` if the binary is missing — wire this to
your orchestrator's readiness check.

---

## Rate limits

Default `30` requests per minute per client IP across all `/api` routes
(health probes excluded). Responses over the limit use code `RATE_LIMITED`.

## Configuration

All configuration is environment-driven; see [`backend/.env.example`](../backend/.env.example)
for the full annotated list (`PORT`, `ALLOWED_ORIGINS`, `SIGNING_SECRET`,
`TOKEN_TTL_SECONDS`, `YTDLP_PATH`, concurrency and rate-limit knobs,
`MAX_DOWNLOAD_SIZE_MB`, `LOG_LEVEL`).
