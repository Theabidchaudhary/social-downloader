import type { FastifyInstance } from 'fastify';
import { runYtDlp } from '../lib/ytdlp.js';

export async function healthRoutes(app: FastifyInstance): Promise<void> {
  app.get('/healthz', async () => ({ status: 'ok', uptimeSeconds: Math.round(process.uptime()) }));

  /** Deep check used by orchestrators before routing traffic to a replica. */
  app.get('/readyz', async (_request, reply) => {
    try {
      const result = await runYtDlp(['--version'], 10_000);
      if (result.exitCode !== 0) throw new Error(result.stderr);
      return { status: 'ready', ytdlpVersion: result.stdout.trim() };
    } catch {
      return reply.status(503).send({ status: 'unavailable', reason: 'yt-dlp not runnable' });
    }
  });
}
