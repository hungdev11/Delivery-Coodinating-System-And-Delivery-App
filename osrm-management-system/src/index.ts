/**
 * OSRM Management System
 * Main entry point
 */

import * as dotenv from 'dotenv';
import * as path from 'path';
import { createApp } from './app';
import { logger } from './common/logger';
import { PrismaClient } from '@prisma/client';

// Load .env file from the same directory as the compiled code (dist/)
// In production, .env should be in osrm-management-system/ directory (parent of dist/)
const envPath = path.resolve(__dirname, '../.env');
const result = dotenv.config({ path: envPath });

if (result.error) {
  // Fallback: try current working directory
  dotenv.config();
  console.warn(`[WARN] Could not load .env from ${envPath}, trying current directory`);
} else {
  console.log(`[INFO] Loaded .env from ${envPath}`);
}

async function main() {
  try {
    const PORT = parseInt(process.env.PORT || '21520', 10);
    const NODE_ENV = process.env.NODE_ENV || 'production';
    const DB_HOST = process.env.DB_HOST || '127.0.0.1';
    const RAW_DATA_PATH = process.env.RAW_DATA_PATH || './raw_data';
    const OSRM_DATA_PATH = process.env.OSRM_DATA_PATH || './osrm_data';

    logger.info('Starting OSRM Management System...');
    logger.info('Configuration', {
      port: PORT,
      nodeEnv: NODE_ENV,
      dbHost: DB_HOST,
      rawDataPath: RAW_DATA_PATH,
      osrmDataPath: OSRM_DATA_PATH,
    });

    // Initialize Prisma
    const dbConnectionString = process.env.DB_CONNECTION_STRING || 
      `mysql://${process.env.DB_USERNAME || 'root'}:${process.env.DB_PASSWORD || 'root'}@${DB_HOST}:${parseInt(process.env.DB_PORT || '3306', 10)}/${process.env.ZONE_DB_NAME || 'ds_zone_service'}`;

    const prisma = new PrismaClient({
      datasources: {
        db: {
          url: dbConnectionString,
        },
      },
    });

    // Test database connection
    await prisma.$connect();
    logger.info('Database connected successfully');

    // Create Express app
    const app = createApp(prisma);

    // Start server
    const server = app.listen(PORT, () => {
      logger.info(`OSRM Management System started on port ${PORT}`);
      logger.info(`Health check: http://localhost:${PORT}/api/v1/health`);
    });

    // Graceful shutdown
    const shutdown = async () => {
      logger.info('Shutting down...');
      server.close(async () => {
        await prisma.$disconnect();
        logger.info('Shutdown complete');
        process.exit(0);
      });
    };

    process.on('SIGTERM', shutdown);
    process.on('SIGINT', shutdown);
  } catch (error: any) {
    logger.error('Failed to start server', { error: error.message, stack: error.stack });
    process.exit(1);
  }
}

main();
