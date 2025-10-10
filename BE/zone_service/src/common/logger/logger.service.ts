/**
 * Logger Service
 * Winston-based logging service with different log levels
 *
 * Usage:
 * - Use logger.info() for developer logs (business logic, important events)
 * - Use logger.error() for errors and exceptions
 * - Use logger.warn() for warnings
 * - Use logger.debug() for debug information (only shown when LOG_LEVEL=debug)
 *
 * Configuration:
 * - Set LOG_LEVEL=info (default) to show info, warn, and error logs
 * - Set LOG_LEVEL=error to only show errors (suppress info/warn from application)
 * - Third-party libraries typically don't log through Winston
 */

import winston from 'winston';

class LoggerService {
  private static instance: winston.Logger | null = null;

  private constructor() {}

  /**
   * Get the singleton instance of Logger
   */
  public static getInstance(): winston.Logger {
    if (!LoggerService.instance) {
      const logFormat = winston.format.combine(
        winston.format.timestamp({ format: 'YYYY-MM-DD HH:mm:ss' }),
        winston.format.errors({ stack: true }),
        winston.format.splat(),
        winston.format.json()
      );

      const consoleFormat = winston.format.combine(
        winston.format.colorize(),
        winston.format.timestamp({ format: 'YYYY-MM-DD HH:mm:ss' }),
        winston.format.printf(({ timestamp, level, message, ...meta }) => {
          const metaStr = Object.keys(meta).length ? JSON.stringify(meta, null, 2) : '';
          return `${timestamp} [${level}]: ${message} ${metaStr}`;
        })
      );

      LoggerService.instance = winston.createLogger({
        level: process.env.LOG_LEVEL || 'info',
        format: logFormat,
        transports: [
          // Console transport
          new winston.transports.Console({
            format: consoleFormat,
          }),
          // File transport for errors
          new winston.transports.File({
            filename: 'logs/error.log',
            level: 'error',
          }),
          // File transport for all logs
          new winston.transports.File({
            filename: 'logs/combined.log',
          }),
        ],
      });

      // Handle production logging
      if (process.env.NODE_ENV === 'production') {
        LoggerService.instance.add(
          new winston.transports.File({
            filename: 'logs/production.log',
            format: logFormat,
          })
        );
      }
    }

    return LoggerService.instance;
  }

  /**
   * Log info message
   */
  public static info(message: string, meta?: any): void {
    this.getInstance().info(message, meta);
  }

  /**
   * Log error message
   */
  public static error(message: string, meta?: any): void {
    this.getInstance().error(message, meta);
  }

  /**
   * Log warning message
   */
  public static warn(message: string, meta?: any): void {
    this.getInstance().warn(message, meta);
  }

  /**
   * Log debug message
   */
  public static debug(message: string, meta?: any): void {
    this.getInstance().debug(message, meta);
  }
}

// Export the singleton instance
export const logger = LoggerService.getInstance();

// Export the class for advanced use cases
export { LoggerService };
