import type { FastifyInstance } from 'fastify';
import { z } from 'zod';
import { config } from '../config.js';
import { ApiError } from '../lib/errors.js';
import { verifyToken } from '../lib/sign.js';
import { prepareDownload } from '../services/streamer.js';

const querySchema = z.object({
  token: z.string().min(16),
});

export async function downloadRoutes(app: FastifyInstance): Promise<void> {
  app.get('/api/v1/download', async (request, reply) => {
    const parsed = querySchema.safeParse(request.query);
    if (!parsed.success) throw ApiError.tokenInvalid();

    const ticket = verifyToken(parsed.data.token, config.SIGNING_SECRET);
    const file = await prepareDownload(ticket);

    // ASCII fallback + RFC 5987 UTF-8 name so every browser and download
    // manager (including IDM's sniffer) picks up a sane filename.
    const asciiName = file.fileName.replace(/[^\x20-\x7e]/g, '_').replace(/"/g, "'");
    reply.raw.setHeader('Content-Type', file.contentType);
    reply.raw.setHeader('Content-Length', String(file.sizeBytes));
    reply.raw.setHeader(
      'Content-Disposition',
      `attachment; filename="${asciiName}"; filename*=UTF-8''${encodeURIComponent(file.fileName)}`,
    );
    reply.raw.setHeader('Accept-Ranges', 'none');
    reply.raw.setHeader('Cache-Control', 'no-store');
    reply.raw.setHeader('X-Content-Type-Options', 'nosniff');

    const stream = file.createStream();
    stream.on('close', () => {
      void file.cleanup().catch((err) => request.log.warn({ err }, 'cleanup failed'));
    });
    // Fastify pipes the stream and ends the response for us.
    return reply.send(stream);
  });
}
