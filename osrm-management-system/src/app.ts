/**
 * Express Application Setup
 */

import express, { Express, Request, Response, NextFunction } from 'express';
import cors from 'cors';
import { createRoutes } from './routes';
import { logger } from './common/logger';
import { PrismaClient } from '@prisma/client';

export function createApp(prisma: PrismaClient): Express {
  const app = express();

  // Middleware
  app.use(cors());
  app.use(express.json());
  app.use(express.urlencoded({ extended: true }));

  // Request logging
  app.use((req: Request, res: Response, next: NextFunction) => {
    logger.info(`${req.method} ${req.path}`, {
      ip: req.ip,
      userAgent: req.get('user-agent'),
    });
    next();
  });

  // Routes
  app.use(createRoutes(prisma));

  // Error handler
  app.use((err: Error, req: Request, res: Response, next: NextFunction) => {
    logger.error('Unhandled error', {
      error: err.message,
      stack: err.stack,
      path: req.path,
    });

    res.status(500).json({
      message: err.message || 'Internal server error',
    });
  });

  // 404 handler
  app.use((req: Request, res: Response) => {
    res.status(404).json({
      message: 'Not found',
    });
  });

  return app;
}
