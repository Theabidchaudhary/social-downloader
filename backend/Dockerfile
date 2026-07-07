# ---- build stage ----
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci || npm install
COPY tsconfig.json ./
COPY src ./src
RUN npm run build && npm prune --omit=dev

# ---- runtime stage ----
FROM node:20-alpine
RUN apk add --no-cache ffmpeg python3 py3-pip curl \
  && pip3 install --no-cache-dir --break-system-packages yt-dlp
WORKDIR /app
ENV NODE_ENV=production
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/dist ./dist
COPY package.json ./
EXPOSE 8787
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl -fsS http://127.0.0.1:8787/healthz || exit 1
CMD sh -c "pip3 install -U --break-system-packages -q yt-dlp 2>/dev/null || true && exec node dist/index.js"
