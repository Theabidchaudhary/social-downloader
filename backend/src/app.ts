import cors from '@fastify/cors';
import rateLimit from '@fastify/rate-limit';
import Fastify, { type FastifyInstance } from 'fastify';
import { randomUUID } from 'node:crypto';
import { config, type AppConfig } from './config.js';
import { ApiError } from './lib/errors.js';
import { downloadRoutes } from './routes/download.js';
import { healthRoutes } from './routes/health.js';
import { platformRoutes } from './routes/platforms.js';
import { resolveRoutes } from './routes/resolve.js';

export async function buildApp(overrides: Partial<AppConfig> = {}): Promise<FastifyInstance> {
  const cfg = { ...config, ...overrides };

  const app = Fastify({
    logger: {
      level: cfg.LOG_LEVEL,
      redact: ['req.headers.authorization', 'req.headers.cookie'],
    },
    genReqId: () => randomUUID(),
    trustProxy: true,
    bodyLimit: 16 * 1024, // resolve bodies are tiny; anything bigger is abuse
  });

  await app.register(cors, {
    origin: cfg.ALLOWED_ORIGINS === '*' ? true : cfg.ALLOWED_ORIGINS.split(',').map((o) => o.trim()),
    methods: ['GET', 'POST'],
  });

  await app.register(rateLimit, {
    max: cfg.RATE_LIMIT_MAX,
    timeWindow: cfg.RATE_LIMIT_WINDOW_MINUTES * 60 * 1000,
    allowList: (req) => req.url === '/healthz' || req.url === '/readyz',
    errorResponseBuilder: () => ({
      error: {
        code: 'RATE_LIMITED',
        message: 'Too many requests. Slow down and try again shortly.',
      },
    }),
  });

  app.setErrorHandler((error, request, reply) => {
    if (error instanceof ApiError) {
      if (error.statusCode >= 500) {
        request.log.error({ err: error, details: error.details }, error.code);
      } else {
        request.log.info({ code: error.code }, error.message);
      }
      return reply.status(error.statusCode).send(error.toBody());
    }

    // Fastify-internal errors (rate limit, body too large, etc.) carry statusCode.
    const status = 'statusCode' in error && typeof error.statusCode === 'number' ? error.statusCode : 500;
    if (status >= 500) request.log.error({ err: error }, 'unhandled error');
    return reply.status(status).send({
      error: {
        code: status === 429 ? 'RATE_LIMITED' : 'INTERNAL',
        message: status >= 500 ? 'Something went wrong on our side.' : error.message,
      },
    });
  });

  app.setNotFoundHandler((_request, reply) =>
    reply.status(404).send({ error: { code: 'NOT_FOUND', message: 'Route not found.' } }),
  );

  await app.register(healthRoutes);
  await app.register(platformRoutes);
  await app.register(resolveRoutes);
  await app.register(downloadRoutes);

  return app;
}
