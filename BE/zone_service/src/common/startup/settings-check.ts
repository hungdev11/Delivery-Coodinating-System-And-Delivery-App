/**
 * Settings Service Health Check
 * Check if settings service is up before starting the application
 */

import axios from 'axios';
import { logger } from '../logger/logger.service';
import { config } from '@config/config';

const MAX_RETRIES = 10;
const RETRY_DELAY = 10000; // 10 seconds

/**
 * Sleep for a given number of milliseconds
 */
function sleep(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function checkOnce(baseUrl: string): Promise<boolean> {
  // Try multiple candidate endpoints (some services don't expose actuator)
  const candidates = [
    '/health',
    '/actuator/health',
    '/api/v1/settings', // list settings as a lightweight readiness signal
  ];

  for (const path of candidates) {
    const url = `${baseUrl.replace(/\/$/, '')}${path}`;
    try {
      // Prefer HEAD when possible; fall back to GET
      try {
        const head = await axios.head(url, { timeout: 5000 });
        if (head.status >= 200 && head.status < 500) return true;
      } catch {
        const get = await axios.get(url, { timeout: 5000 });
        if (get.status >= 200 && get.status < 500) return true;
      }
    } catch (err) {
      // continue to next candidate
    }
  }
  return false;
}

/**
 * Check if settings service is available
 */
export async function checkSettingsService(): Promise<boolean> {
  const settingsUrl = config.settings.serviceUrl;

  if (!settingsUrl) {
    logger.warn('SETTINGS_SERVICE_URL not configured, skipping health check');
    return true;
  }

  logger.info('Checking Settings Service availability...', { url: settingsUrl });

  for (let i = 1; i <= MAX_RETRIES; i++) {
    try {
      const ok = await checkOnce(settingsUrl);
      if (ok) {
        logger.info('Settings Service is available', { attempt: i });
        return true;
      }
      throw new Error('No health endpoint responded 2xx/4xx');
    } catch (error) {
      logger.warn(`Settings Service not ready (attempt ${i}/${MAX_RETRIES})`, {
        error: error instanceof Error ? error.message : 'Unknown error',
      });

      if (i < MAX_RETRIES) {
        logger.info(`Retrying in ${RETRY_DELAY / 1000} seconds...`);
        await sleep(RETRY_DELAY);
      }
    }
  }

  logger.error('Settings Service is not available after maximum retries');
  return false;
}
