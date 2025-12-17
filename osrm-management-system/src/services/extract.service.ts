/**
 * Extract Service
 * Handles OSM data extraction
 * Tracks builds in database for sequential processing
 */

import { OsmiumWrapper } from '../utils/osmium-wrapper';
import { findLatestVietnamPBF } from '../utils/osm-parser';
import { join } from 'path';
import { existsSync } from 'fs';
import { logger } from '../common/logger';
import { PrismaClient } from '@prisma/client';
import { BuildTrackerService } from './build-tracker.service';

export interface ExtractResult {
  success: boolean;
  outputPath?: string;
  error?: string;
  duration?: number;
  buildId?: string;
}

export class ExtractService {
  private rawDataPath: string;
  private buildTracker: BuildTrackerService;
  private prisma: PrismaClient;

  constructor(prisma: PrismaClient) {
    this.prisma = prisma;
    this.rawDataPath = process.env.RAW_DATA_PATH || './raw_data';
    this.buildTracker = new BuildTrackerService(prisma);
  }

  /**
   * Extract complete data (sync-like: sequential processing)
   */
  async extractCompleteData(polyFile?: string): Promise<ExtractResult> {
    const instanceName = 'extract-complete';
    
    return this.buildTracker.executeSequentially(instanceName, async (buildId) => {
      return this._extractCompleteData(buildId, polyFile);
    });
  }

  private async _extractCompleteData(buildId: string, polyFile?: string): Promise<ExtractResult> {
    const startTime = Date.now();
    
    try {
      logger.info(`Starting complete OSM data extraction (Build ID: ${buildId})...`);

      // Setup paths
      const pbfPath = findLatestVietnamPBF(this.rawDataPath);
      const defaultPolyFile = join(this.rawDataPath, 'poly/thuduc_cu.poly');
      const poly = polyFile || defaultPolyFile;
      const outputPbf = join(this.rawDataPath, 'extracted/thuduc_complete.osm.pbf');

      if (!existsSync(poly)) {
        throw new Error(`Poly file not found: ${poly}`);
      }

      logger.info(`Source PBF: ${pbfPath}`);
      logger.info(`Polygon: ${poly}`);
      logger.info(`Output: ${outputPbf}`);

      // Initialize osmium wrapper
      const osmium = new OsmiumWrapper(true);

      // Check installation
      const isInstalled = await osmium.checkInstallation();
      if (!isInstalled) {
        throw new Error('osmium-tool is not installed');
      }

      // Run extraction
      await osmium.extractRoutingWithAddresses(pbfPath, poly, outputPbf);

      const duration = Date.now() - startTime;

      // Mark build as READY
      await this.buildTracker.markReady(buildId, outputPbf);

      logger.info(`Extraction completed in ${duration}ms`);

      return {
        success: true,
        outputPath: outputPbf,
        duration,
        buildId,
      };
    } catch (error: any) {
      logger.error('Extraction failed', { error: error.message, buildId });
      await this.buildTracker.markFailed(buildId, error.message);
      return {
        success: false,
        error: error.message,
        duration: Date.now() - startTime,
        buildId,
      };
    }
  }
}
