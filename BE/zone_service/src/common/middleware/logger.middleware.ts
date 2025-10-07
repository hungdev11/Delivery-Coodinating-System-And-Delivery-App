/**
 * Request Logger Middleware
 * Logs incoming HTTP requests
 */

import { Request, Response, NextFunction } from 'express';
import { logger } from '../logger/logger.service';

/**
 * Log incoming requests
 */
export function requestLogger(req: Request, res: Response, next: NextFunction): void {
  const start = Date.now();

  // Log request
  logger.info('Incoming request', {
    method: req.method,
    url: req.url,
    ip: req.ip,
    userAgent: req.get('user-agent'),
  });

  // Log response on finish
  res.on('finish', () => {
    const duration = Date.now() - start;
    logger.info('Request completed', {
      method: req.method,
      url: req.url,
      statusCode: res.statusCode,
      duration: `${duration}ms`,
    });
  });

  next();
}

