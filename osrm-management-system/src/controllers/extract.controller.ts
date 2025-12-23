/**
 * Extract Controller
 * RESTful API endpoints for OSM data extraction
 */

import { Request, Response, NextFunction } from 'express';
import { ExtractService } from '../services/extract.service';
import { BaseResponseBuilder } from '../common/types/restful';
import { logger } from '../common/logger';
import { PrismaClient } from '@prisma/client';

export class ExtractController {
  private extractService: ExtractService;

  constructor(prisma: PrismaClient) {
    this.extractService = new ExtractService(prisma);
  }

  /**
   * POST /api/v1/extract/complete
   * Extract complete OSM data (routing + addresses)
   */
  async extractComplete(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { polyFile } = req?.body || { polyFile: undefined };

      logger.info('Extract complete request received', { polyFile });

      const result = await this.extractService.extractCompleteData(polyFile);

      if (result.success) {
        res.json(BaseResponseBuilder.success(result, 'Extraction completed successfully'));
      } else {
        res.status(500).json(BaseResponseBuilder.error(result.error || 'Extraction failed'));
      }
    } catch (error) {
      next(error);
    }
  }
}
