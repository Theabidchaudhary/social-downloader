import type { FastifyInstance } from 'fastify';
import { z } from 'zod';
import { ApiError } from '../lib/errors.js';
import { resolveUrl } from '../services/extractor.js';

const bodySchema = z.object({
  url: z.string().min(1).max(2048),
});

export async function resolveRoutes(app: FastifyInstance): Promise<void> {
  app.post('/api/v1/resolve', async (request, reply) => {
    const parsed = bodySchema.safeParse(request.body);
    if (!parsed.success) throw ApiError.invalidUrl('Body must be { "url": string }.');

    const result = await resolveUrl(parsed.data.url);
    return reply.send(result);
  });
}
