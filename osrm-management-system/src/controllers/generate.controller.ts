/**
 * Generate Controller
 * RESTful API endpoints for OSRM data generation
 */

import { Request, Response, NextFunction } from 'express';
import { GenerateService } from '../services/generate.service';
import { BaseResponseBuilder } from '../common/types/restful';
import { logger } from '../common/logger';
import { PrismaClient } from '@prisma/client';

export class GenerateController {
  private generateService: GenerateService;
  private prisma: PrismaClient;

  constructor(prisma: PrismaClient) {
    this.prisma = prisma;
    this.generateService = new GenerateService(prisma);
  }

  /**
   * POST /api/v1/generate/osrm-v2
   * Generate all OSRM V2 models from database
   */
  async generateOSRMV2(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      logger.info('Generate OSRM V2 request received');

      const result = await this.generateService.generateAllModels();

      if (result.success) {
        res.json(BaseResponseBuilder.success(result, `Generated ${result.models?.length || 0} OSRM models successfully`));
      } else {
        res.status(500).json(BaseResponseBuilder.error(result.error || 'Generation failed'));
      }
    } catch (error) {
      next(error);
    }
  }
}
