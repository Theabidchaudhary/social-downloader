# Siphon — Hosting quickstart (test → live domain)

A concrete path from zero to a working, TLS-secured deployment. Two things get
hosted: the **API** (does the actual downloading; needs a server that can run
Docker or Node + yt-dlp + ffmpeg) and the **web app** (static files). Static-only
hosts (GitHub Pages, plain Netlify) are **not enough by themselves** — the web
app is a frontend for the API.

**Important if an AI assistant is running these commands for you in a cloud
session:** `http://localhost:...` only works in a browser on the *same
computer* that ran the command. A command run inside an isolated cloud
container (like a Claude Code web session) is not reachable from your own
browser — there is no clickable link that will work. Either run section 0
yourself on your own machine, or skip straight to section 0.5 for a real
public URL with no local setup at all.

---

## 0.5. Fastest way to a real public URL (~10 min, free, no server rental)

This gets you an actual `https://…` link you can open on any device, using
[Render](https://render.com)'s free tier — no VPS, no credit card required to
start, deploy via their dashboard from your GitHub repo.

**A. Deploy the API**
1. render.com → sign in with GitHub → **New +** → **Web Service**.
2. Connect the `pixxelpulse` repo.
3. **Root Directory**: `siphon/backend`. **Runtime**: Docker (it will detect
   the `Dockerfile` automatically). **Instance type**: Free.
4. Environment variables: add `SIGNING_SECRET` (any long random string —
   or click "Generate") and `ALLOWED_ORIGINS` = `*` for now (tighten in step C).
5. Create the service. First build takes a few minutes (installs yt-dlp +
   ffmpeg). Note its URL, e.g. `https://siphon-api-xxxx.onrender.com`.
6. Confirm it's alive: open `https://siphon-api-xxxx.onrender.com/healthz` —
   should show `{"status":"ok"}`.

**B. Deploy the web app**
1. render.com → **New +** → **Static Site** → same repo.
2. **Root Directory**: `siphon/web`. **Build Command**: `npm ci && npm run build`.
   **Publish Directory**: `dist`.
3. Environment variables: add `VITE_API_BASE_URL` = the API URL from step A
   (e.g. `https://siphon-api-xxxx.onrender.com`) — this tells the web app
   where to send requests since it's on a different domain than the API.
4. Create the site. When it finishes you get a URL like
   `https://siphon-web-xxxx.onrender.com` — **open it, paste a video link,
   test it for real.**

**C. Lock down CORS** (optional but recommended once it works): go back to the
API service's environment variables and set `ALLOWED_ORIGINS` to your exact
web URL from step B instead of `*`, then save (triggers a redeploy).

Free-tier caveats: the API service spins down after 15 minutes idle (first
request after that takes ~30s to wake up) and has limited RAM, so very large
1080p+ muxes may be slow or fail — fine for testing, not for real traffic.
[Railway](https://railway.app) works the same way (Docker-aware "Deploy from
GitHub", root directory setting) if you'd rather try that instead.

Once you're happy and want a permanent setup with your own domain, follow
section 3+ below for a VPS + nginx + free TLS, which has no idle spin-down and
no resource ceiling.

### YouTube / X saying "private, age-restricted, or asking the server to sign in"

This isn't a bug in Siphon — YouTube and X actively fingerprint and
rate-limit requests coming from cloud/datacenter IP ranges (Render, Railway,
AWS, etc.) far more aggressively than a home internet connection, regardless
of what headers the request sends. It usually shows up as "Sign in to
confirm you're not a bot" in server logs. Two ways to fix it:

1. **Just retry.** The block is often temporary/per-video and clears after a
   few minutes.
2. **Give the server login cookies** (the permanent fix): export a
   `cookies.txt` from a browser where you're logged into YouTube/X (e.g. the
   "Get cookies.txt LOCALLY" extension for Chrome/Firefox), then:
   - Render: service → **Environment** → **Secret Files** → add a file named
     `cookies.txt` with the exported contents.
   - Set an environment variable `COOKIES_FILE` = `/etc/secrets/cookies.txt`
     (Render mounts secret files at that path) and redeploy.
   - Cookies expire — re-export and re-upload every few weeks if extraction
     quality drops again.

   Use an account you're comfortable using for this; don't share the
   resulting cookies.txt, it grants login access to that account.

---

## 0. Test locally on your own machine

```bash
# Terminal 1 — API (or use Docker, see below)
cd siphon/backend
cp .env.example .env                  # set SIGNING_SECRET to anything long
npm install && npm run dev            # → http://localhost:8787

# Terminal 2 — web app
cd siphon/web
npm install && npm run dev            # → http://localhost:5173
```

Open http://localhost:5173, paste a YouTube link, pick a quality — the file
should download. (`yt-dlp` and `ffmpeg` must be installed and on PATH for the
non-Docker path: `pip install yt-dlp` / `apt install ffmpeg`.)

**Android testing:**

- Emulator: install the **debug APK** — it already points at
  `http://10.0.2.2:8787` (your machine's localhost).
- Physical phone on the same Wi-Fi: build with your PC's LAN IP —
  `./gradlew :app:assembleDebug -PsiphonApiBaseUrl=http://192.168.1.20:8787`
  (debug builds allow plain HTTP; release builds require HTTPS).
- Install: `adb install app/build/outputs/apk/debug/app-debug.apk`, or copy
  the APK to the phone and open it (allow "install unknown apps").
- Test the flagship flow: open YouTube → Share → **Save with Siphon**.

## 1. Get a server + domain

- Any small VPS works to start: 2 vCPU / 2–4 GB RAM / 40 GB disk
  (Hetzner CX22, DigitalOcean, Lightsail…). Ubuntu 22.04+.
- Buy a domain anywhere (Namecheap, Cloudflare, …).

**DNS**: in your domain's DNS panel add two **A records** pointing to the
VPS's public IP:

| Type | Name | Value |
| --- | --- | --- |
| A | `@` (or `siphon`) | `<VPS IP>` — serves the web app |
| A | `api` | `<VPS IP>` — serves the API |

Propagation is usually minutes. Verify with `dig +short yourdomain.com`.

## 2. Run the API on the VPS

```bash
ssh root@<VPS-IP>
apt update && apt install -y docker.io docker-compose-v2 nginx certbot python3-certbot-nginx git
git clone <your repo> && cd pixxelpulse/siphon/backend

SIGNING_SECRET=$(openssl rand -hex 32) \
ALLOWED_ORIGINS=https://yourdomain.com \
docker compose up --build -d

curl http://127.0.0.1:8787/readyz     # → {"status":"ready",...}
```

## 3. Put the web build on the VPS

Build locally (`cd siphon/web && npm run build`) or grab the
`siphon-web-dist` artifact from GitHub Actions, then:

```bash
scp -r dist/* root@<VPS-IP>:/var/www/siphon/
```

## 4. nginx: one site for web, one for API, then TLS

`/etc/nginx/sites-available/siphon`:

```nginx
server {
    listen 80;
    server_name yourdomain.com;
    root /var/www/siphon;
    location / { try_files $uri /index.html; }
    # Same-origin API proxy — the web app calls /api/... relatively
    location /api/ {
        proxy_pass http://127.0.0.1:8787;
        proxy_buffering off;
        proxy_read_timeout 1200s;
    }
}

server {
    listen 80;
    server_name api.yourdomain.com;   # used by the Android app
    location / {
        proxy_pass http://127.0.0.1:8787;
        proxy_buffering off;
        proxy_read_timeout 1200s;
    }
}
```

```bash
ln -s /etc/nginx/sites-available/siphon /etc/nginx/sites-enabled/
nginx -t && systemctl reload nginx
certbot --nginx -d yourdomain.com -d api.yourdomain.com   # free TLS, auto-renew
```

Now `https://yourdomain.com` is the product and
`https://api.yourdomain.com/healthz` returns `{"status":"ok"}`.

## 5. Point the Android app at your domain

Build the release APK against your API and sign it:

```bash
cd siphon/android
./gradlew :app:assembleRelease -PsiphonApiBaseUrl=https://api.yourdomain.com
```

The CI workflow (`.github/workflows/siphon-build.yml`) produces an unsigned
release APK; sign it with `apksigner` and your keystore, or configure
`signingConfigs` / Play App Signing for Play Store distribution
(`:app:bundleRelease` for the AAB).

## Maintenance

- **yt-dlp goes stale** as platforms change — rebuild the API image weekly:
  `docker compose build --pull && docker compose up -d`.
- Watch disk: proxied downloads buffer in the container's `/tmp` (tmpfs).
- Logs: `docker compose logs -f api` (JSON, pino).
