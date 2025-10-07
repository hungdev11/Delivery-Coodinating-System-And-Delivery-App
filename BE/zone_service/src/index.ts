/**
 * Zone Service - Main Entry Point
 * Microservice for zone and location management
 */

import 'reflect-metadata';
import 'dotenv/config';
import { createApp } from './app';
import { logger } from './common/logger/logger.service';
import { prisma, PrismaClientSingleton } from './common/database/prisma.client';
import { kafkaService } from './common/kafka/kafka.service';
import { checkSettingsService } from './common/startup/settings-check';
import { initializeSettings } from './common/startup/settings-init';

// Export all common modules
export * from './common';

// Export all business modules
export * from './modules';

/**
 * Service initialization and startup
 */
async function bootstrap() {
  try {
    logger.info('🚀 Zone Service Starting...');
    logger.info(`📍 Environment: ${process.env.NODE_ENV || 'development'}`);
    logger.info(`🔌 Database: ${process.env.DB_CONNECTION ? '✓ Configured' : '✗ Not configured'}`);

    // Step 1: Check Settings Service availability
    logger.info('Step 1: Checking Settings Service...');
    const settingsAvailable = await checkSettingsService();
    if (!settingsAvailable && process.env.SETTINGS_SERVICE_URL) {
      logger.error('Settings Service is required but not available');
      process.exit(1);
    }

    // Step 2: Initialize default settings
    if (settingsAvailable) {
      logger.info('Step 2: Initializing settings...');
      await initializeSettings();
    } else {
      logger.warn('Step 2: Skipping settings initialization (service not configured)');
    }

    // Step 3: Test database connection
    logger.info('Step 3: Testing database connection...');
    await prisma.$queryRaw`SELECT 1`;
    logger.info('✓ Database connection successful');

    // Step 4: Initialize Kafka (optional, non-blocking)
    if (process.env.KAFKA_BROKERS) {
      logger.info('Step 4: Initializing Kafka...');
      try {
        await kafkaService.initialize({
          brokers: process.env.KAFKA_BROKERS.split(','),
          clientId: 'zone-service',
          groupId: process.env.KAFKA_GROUP_ID || 'zone-service-group',
        });
        logger.info('✓ Kafka initialized');
      } catch (error) {
        logger.warn('⚠ Kafka initialization failed (service will continue)', { error });
      }
    } else {
      logger.info('Step 4: Kafka not configured, skipping');
    }

    // Step 5: Create and start Express application
    logger.info('Step 5: Starting Express server...');
    const app = createApp();
    const port = parseInt(process.env.PORT || '3003');

    const server = app.listen(port, '0.0.0.0', () => {
      logger.info(`✅ Zone Service initialized successfully`);
      logger.info(`⚡ Server running on port ${port}`);
      logger.info(`🔗 Health check: http://localhost:${port}/health`);
      logger.info(`🔗 API endpoint: http://localhost:${port}/api/v1`);
    });

    // Graceful shutdown handling
    const gracefulShutdown = async (signal: string) => {
      logger.info(`${signal} received, starting graceful shutdown...`);

      // Stop accepting new connections
      server.close(async () => {
        logger.info('HTTP server closed');

        // Disconnect from database
        await PrismaClientSingleton.disconnect();
        logger.info('Database disconnected');

        // Disconnect from Kafka
        if (kafkaService.getConnectionStatus()) {
          await kafkaService.disconnect();
          logger.info('Kafka disconnected');
        }

        logger.info('Graceful shutdown completed');
        process.exit(0);
      });

      // Force shutdown after 30 seconds
      setTimeout(() => {
        logger.error('Forced shutdown after timeout');
        process.exit(1);
      }, 30000);
    };

    // Register shutdown handlers
    process.on('SIGTERM', () => gracefulShutdown('SIGTERM'));
    process.on('SIGINT', () => gracefulShutdown('SIGINT'));

  } catch (error) {
    logger.error('❌ Failed to initialize Zone Service', { error });
    process.exit(1);
  }
}

// Start the service
bootstrap();
