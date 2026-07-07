import { z } from 'zod';

const envSchema = z.object({
  PORT: z.coerce.number().int().positive().default(8787),
  HOST: z.string().default('0.0.0.0'),
  ALLOWED_ORIGINS: z.string().default('*'),
  SIGNING_SECRET: z
    .string()
    .min(16, 'SIGNING_SECRET must be at least 16 characters')
    .default('siphon-dev-secret-do-not-use-in-prod'),
  TOKEN_TTL_SECONDS: z.coerce.number().int().positive().default(1800),
  YTDLP_PATH: z.string().default('yt-dlp'),
  /**
   * Path to a Netscape-format cookies.txt exported from a logged-in browser.
   * YouTube and X aggressively bot-block cloud/datacenter IPs (Render,
   * Railway, AWS, ...) regardless of headers; supplying cookies is the only
   * reliable workaround for "Sign in to confirm you're not a bot" and
   * similar login-gated errors. Optional — everything still works for
   * platforms/videos that don't require it.
   */
  COOKIES_FILE: z.string().optional(),
  MAX_CONCURRENT_EXTRACTIONS: z.coerce.number().int().positive().default(4),
  MAX_CONCURRENT_STREAMS: z.coerce.number().int().positive().default(6),
  RESOLVE_CACHE_TTL_SECONDS: z.coerce.number().int().nonnegative().default(300),
  RATE_LIMIT_MAX: z.coerce.number().int().positive().default(30),
  RATE_LIMIT_WINDOW_MINUTES: z.coerce.number().int().positive().default(1),
  MAX_DOWNLOAD_SIZE_MB: z.coerce.number().int().nonnegative().default(4096),
  LOG_LEVEL: z.enum(['fatal', 'error', 'warn', 'info', 'debug', 'trace']).default('info'),
  NODE_ENV: z.enum(['development', 'test', 'production']).default('development'),
});

export type AppConfig = z.infer<typeof envSchema>;

export function loadConfig(env: NodeJS.ProcessEnv = process.env): AppConfig {
  const parsed = envSchema.safeParse(env);
  if (!parsed.success) {
    const issues = parsed.error.issues
      .map((issue) => `  ${issue.path.join('.')}: ${issue.message}`)
      .join('\n');
    throw new Error(`Invalid environment configuration:\n${issues}`);
  }
  if (
    parsed.data.NODE_ENV === 'production' &&
    parsed.data.SIGNING_SECRET === 'siphon-dev-secret-do-not-use-in-prod'
  ) {
    throw new Error('SIGNING_SECRET must be set explicitly in production');
  }
  return parsed.data;
}

export const config = loadConfig();
