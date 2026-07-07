import type { FastifyInstance } from 'fastify';
import { detectPlatform, SUPPORTED_PLATFORMS } from '../lib/platform.js';

export async function platformRoutes(app: FastifyInstance): Promise<void> {
  app.get('/api/v1/platforms', async () => ({ platforms: SUPPORTED_PLATFORMS }));

  /**
   * Cheap, non-rate-limited-in-spirit detection endpoint. Clients also embed
   * the same rules locally; this exists so third parties and tests can verify
   * server behaviour matches.
   */
  app.get<{ Querystring: { url?: string } }>('/api/v1/detect', async (request) => {
    const url = request.query.url ?? '';
    const detected = detectPlatform(url);
    return detected
      ? { supported: true, platform: detected.platform, kind: detected.kind }
      : { supported: false, platform: null, kind: null };
  });
}
