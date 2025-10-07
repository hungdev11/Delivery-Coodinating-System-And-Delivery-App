/**
 * Health Check Controller
 * Provides health check endpoints for service monitoring
 */

import { Request, Response } from 'express';
import { prisma } from '../database/prisma.client';
import { kafkaService } from '../kafka/kafka.service';
import { logger } from '../logger/logger.service';

class HealthController {
  /**
   * Basic health check
   */
  public static async healthCheck(_req: Request, res: Response): Promise<void> {
    res.status(200).json({
      status: 'UP',
      timestamp: new Date().toISOString(),
      service: 'zone-service',
    });
  }

  /**
   * Detailed health check with dependencies
   */
  public static async detailedHealthCheck(_req: Request, res: Response): Promise<void> {
    const health = {
      status: 'UP',
      timestamp: new Date().toISOString(),
      service: 'zone-service',
      dependencies: {
        database: await HealthController.checkDatabase(),
        kafka: await HealthController.checkKafka(),
        settingsService: await HealthController.checkSettingsService(),
      },
    };

    // Determine overall status
    const allHealthy = Object.values(health.dependencies).every(dep => dep.status === 'UP');
    health.status = allHealthy ? 'UP' : 'DEGRADED';

    const statusCode = allHealthy ? 200 : 503;
    res.status(statusCode).json(health);
  }

  /**
   * Check database connectivity
   */
  private static async checkDatabase(): Promise<{ status: string; message?: string }> {
    try {
      await prisma.$queryRaw`SELECT 1`;
      return { status: 'UP' };
    } catch (error) {
      logger.error('Database health check failed', { error });
      return { status: 'DOWN', message: error instanceof Error ? error.message : 'Unknown error' };
    }
  }

  /**
   * Check Kafka connectivity
   */
  private static async checkKafka(): Promise<{ status: string; message?: string }> {
    try {
      const isConnected = kafkaService.getConnectionStatus();
      return isConnected ? { status: 'UP' } : { status: 'DOWN', message: 'Not connected' };
    } catch (error) {
      logger.error('Kafka health check failed', { error });
      return { status: 'DOWN', message: error instanceof Error ? error.message : 'Unknown error' };
    }
  }

  /**
   * Check Settings Service connectivity
   */
  private static async checkSettingsService(): Promise<{ status: string; message?: string }> {
    try {
      // This will be implemented after settings service is ready
      // For now, return UP if URL is configured
      const url = process.env.SETTINGS_SERVICE_URL;
      return url ? { status: 'UP' } : { status: 'DOWN', message: 'URL not configured' };
    } catch (error) {
      logger.error('Settings service health check failed', { error });
      return { status: 'DOWN', message: error instanceof Error ? error.message : 'Unknown error' };
    }
  }

  /**
   * Readiness check
   */
  public static async readinessCheck(_req: Request, res: Response): Promise<void> {
    try {
      await prisma.$queryRaw`SELECT 1`;
      res.status(200).json({ status: 'READY' });
    } catch (error) {
      res.status(503).json({ status: 'NOT_READY', error: error instanceof Error ? error.message : 'Unknown error' });
    }
  }

  /**
   * Liveness check
   */
  public static async livenessCheck(_req: Request, res: Response): Promise<void> {
    res.status(200).json({ status: 'ALIVE' });
  }
}

export { HealthController };
