/**
 * Health Heartbeat Publisher
 * Publishes health status to Kafka at regular intervals
 */

import { kafkaService } from '../kafka/kafka.service';
import { logger } from '../logger/logger.service';
import { config } from '@config/config';
import axios from 'axios';
import * as os from 'os';

const TOPIC_HEALTH_STATUS = 'health-status';
const DEFAULT_PING_INTERVAL_SECONDS = 10;

interface HealthStatusDto {
  serviceName: string;
  status: string;
  timestamp: string;
  version: string;
  metadata: {
    hostname: string;
    pid: number;
    timestamp_millis: number;
  };
}

class HealthHeartbeatPublisher {
  private static instance: HealthHeartbeatPublisher | null = null;
  private intervalId: NodeJS.Timeout | null = null;
  private isRunning: boolean = false;
  private pingIntervalSeconds: number = DEFAULT_PING_INTERVAL_SECONDS;
  private hasError: boolean = false;

  private constructor() {}

  /**
   * Get singleton instance
   */
  public static getInstance(): HealthHeartbeatPublisher {
    if (!HealthHeartbeatPublisher.instance) {
      HealthHeartbeatPublisher.instance = new HealthHeartbeatPublisher();
    }
    return HealthHeartbeatPublisher.instance;
  }

  /**
   * Start publishing health heartbeat
   */
  public async start(): Promise<void> {
    if (this.isRunning) {
      logger.warn('[HealthHeartbeatPublisher] Already running');
      return;
    }

    logger.info('[HealthHeartbeatPublisher] Starting health heartbeat publisher');

    // Load ping interval from Settings Service
    try {
      const settingsServiceUrl = config.settings.serviceUrl || process.env.SETTINGS_SERVICE_URL || 'http://localhost:21502';
      const url = `${settingsServiceUrl}/api/v1/settings/HEALTH_PING_INTERVAL_SECONDS/value?defaultValue=${DEFAULT_PING_INTERVAL_SECONDS}`;
      
      const response = await axios.get(url, { timeout: 5000 });
      if (response.data && typeof response.data === 'string') {
        this.pingIntervalSeconds = parseInt(response.data, 10) || DEFAULT_PING_INTERVAL_SECONDS;
      }
      logger.info(`[HealthHeartbeatPublisher] Health ping interval: ${this.pingIntervalSeconds} seconds`);
    } catch (error) {
      logger.warn(`[HealthHeartbeatPublisher] Failed to load HEALTH_PING_INTERVAL_SECONDS from Settings Service, using default: ${DEFAULT_PING_INTERVAL_SECONDS}s`, { error });
    }

    // Wait for Kafka to be ready
    if (!kafkaService.getConnectionStatus()) {
      logger.warn('[HealthHeartbeatPublisher] Kafka not connected, will retry after Kafka is initialized');
      // Retry after 5 seconds
      setTimeout(() => this.start(), 5000);
      return;
    }

    this.isRunning = true;
    this.hasError = false;

    // Publish immediately
    await this.publishHealthStatus();

    // Then publish at regular intervals
    this.intervalId = setInterval(() => {
      this.publishHealthStatus().catch((error) => {
        logger.error('[HealthHeartbeatPublisher] Error in scheduled publish', { error });
      });
    }, this.pingIntervalSeconds * 1000);

    logger.info(`[HealthHeartbeatPublisher] Health heartbeat publisher started (interval: ${this.pingIntervalSeconds}s)`);
  }

  /**
   * Publish health status to Kafka
   */
  private async publishHealthStatus(): Promise<void> {
    // Skip if we've encountered a fatal error
    if (this.hasError) {
      return;
    }

    // Skip if Kafka is not connected
    if (!kafkaService.getConnectionStatus()) {
      logger.debug('[HealthHeartbeatPublisher] Kafka not connected, skipping publish');
      return;
    }

    try {
      const healthStatus: HealthStatusDto = {
        serviceName: 'zone-service',
        status: 'UP',
        timestamp: new Date().toISOString(),
        version: process.env.npm_package_version || '1.2.0',
        metadata: {
          hostname: os.hostname(),
          pid: process.pid,
          timestamp_millis: Date.now(),
        },
      };

      // Send with serviceName as key (required by API Gateway consumer)
      await kafkaService.sendMessage(TOPIC_HEALTH_STATUS, healthStatus, 'zone-service');
      logger.debug('[HealthHeartbeatPublisher] Health status published successfully');
    } catch (error: any) {
      logger.error('[HealthHeartbeatPublisher] Failed to publish health status', { error });

      // Check if error is due to topic not existing
      if (error?.message?.includes('UnknownTopicOrPartition') || error?.message?.includes('Topic does not exist')) {
        logger.error('[HealthHeartbeatPublisher] Topic does not exist. Stopping heartbeat.');
        this.hasError = true;
        this.stop();
      }
    }
  }

  /**
   * Stop publishing health heartbeat
   */
  public stop(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
      this.intervalId = null;
    }
    this.isRunning = false;
    logger.info('[HealthHeartbeatPublisher] Health heartbeat publisher stopped');
  }

  /**
   * Check if publisher is running
   */
  public isActive(): boolean {
    return this.isRunning && !this.hasError;
  }
}

// Export singleton instance
export const healthHeartbeatPublisher = HealthHeartbeatPublisher.getInstance();

// Export class for advanced use cases
export { HealthHeartbeatPublisher };
