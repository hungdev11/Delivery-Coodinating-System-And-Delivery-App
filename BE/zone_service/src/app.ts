/**
 * Express Application Configuration
 * Main application setup with middleware, routes, and error handling
 */

import 'reflect-metadata';
import express, { Express } from 'express';
import cors from 'cors';
import { logger } from './common/logger/logger.service';
import { requestLogger } from './common/middleware/logger.middleware';
import { errorHandler, notFoundHandler } from './common/middleware/error.middleware';
import { HealthController } from './common/health/health.controller';
import { routes } from './modules/routes';

/**
 * Create and configure Express application
 */
export function createApp(): Express {
  const app = express();

  // CORS configuration
  const corsOrigins = process.env.CORS_ORIGINS?.split(',').map(origin => origin.trim()) || ['*'];
  
  app.use(
    cors({
      origin: corsOrigins.includes('*') ? '*' : corsOrigins,
      credentials: true,
      methods: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'OPTIONS'],
      allowedHeaders: ['Content-Type', 'Authorization', 'X-Requested-With'],
    })
  );

  // Body parsing middleware
  app.use(express.json({ limit: '10mb' }));
  app.use(express.urlencoded({ extended: true, limit: '10mb' }));

  // Request logging middleware
  app.use(requestLogger);

  // Health check endpoints (before other routes)
  app.get('/health', HealthController.healthCheck);
  app.get('/health/detailed', HealthController.detailedHealthCheck);
  app.get('/health/readiness', HealthController.readinessCheck);
  app.get('/health/liveness', HealthController.livenessCheck);

  // API routes
  app.use('/api/v1', routes);

  // 404 handler
  app.use(notFoundHandler);

  // Error handling middleware (must be last)
  app.use(errorHandler);

  logger.info('Express application configured');

  return app;
}
