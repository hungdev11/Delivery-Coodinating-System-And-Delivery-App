/**
 * Error Handling Middleware
 * Global error handler for the application
 */

import { Request, Response, NextFunction } from 'express';
import { logger } from '../logger/logger.service';
import { BaseResponse } from '../types/restful';

export interface AppError extends Error {
  statusCode?: number;
  isOperational?: boolean;
}

/**
 * Global error handler middleware
 */
export function errorHandler(
  err: AppError,
  req: Request,
  res: Response,
  _next: NextFunction
): void {
  const statusCode = err.statusCode || 500;
  const message = err.message || 'Internal Server Error';

  // Log error
  logger.error('Error occurred', {
    statusCode,
    message,
    stack: err.stack,
    url: req.url,
    method: req.method,
    body: req.body,
    query: req.query,
    params: req.params,
  });

  // Send error response
  res.status(statusCode).json(BaseResponse.error<null>(message));
}

/**
 * 404 Not Found handler
 */
export function notFoundHandler(req: Request, res: Response): void {
  res.status(404).json(BaseResponse.error(`Route ${req.url} not found`));
}

/**
 * Create operational error
 */
export function createError(message: string, statusCode: number = 500): AppError {
  const error: AppError = new Error(message);
  error.statusCode = statusCode;
  error.isOperational = true;
  return error;
}
