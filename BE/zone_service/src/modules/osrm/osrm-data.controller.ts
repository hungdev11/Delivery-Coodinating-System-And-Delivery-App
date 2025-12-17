/**
 * OSRM Data Management Controller
 * 
 * Provides REST API endpoints for:
 * - Generating OSRM V2 data from database (all 4 models)
 */

import { Request, Response, NextFunction } from 'express';
import { BaseResponse } from '../../common/types/restful';
import { logger } from '../../common/logger';

export class OSRMDataController {
  /**
   * Generate OSRM V2 data from database (all 4 models)
   * POST /api/v1/osrm/generate-v2
   * 
   * This endpoint generates all OSRM models (osrm-full, osrm-rating-only, osrm-blocking-only, osrm-base)
   * from the current database state. It replaces the old build-all endpoint.
   */
  public static async generateV2(_req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      logger.info('Starting OSRM V2 generation via API...');
      
      // Import service dynamically to avoid circular dependencies
      const { OSRMV2GeneratorService } = await import('../../services/osrm/osrm-v2-generator.service');
      const generator = new OSRMV2GeneratorService();
      
      // Run generation (this may take a while)
      const result = await generator.generateAllModels();
      
      if (result.success) {
        res.json(BaseResponse.success(result, `OSRM V2 generation completed: ${result.models.length} models`));
      } else {
        res.status(500).json(BaseResponse.error(`OSRM V2 generation failed: ${result.error || 'Unknown error'}`));
      }
    } catch (error) {
      next(error);
    }
  }

  /**
   * Get OSRM generation status
   * GET /api/v1/osrm/status
   * 
   * Returns status of OSRM data files and build status from osrm-management-system
   */
  public static async getStatus(_req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { existsSync } = await import('fs');
      const { join } = await import('path');
      
      const osrmDataPath = process.env.OSRM_DATA_PATH || join(process.cwd(), 'osrm_data');
      const models = ['osrm-full', 'osrm-rating-only', 'osrm-blocking-only', 'osrm-base'];
      
      // Check file existence
      const fileStatus = models.map(model => {
        const modelPath = join(osrmDataPath, model);
        const osrmFile = join(modelPath, 'network.osrm');
        const exists = existsSync(osrmFile);
        
        return {
          name: model,
          exists,
          path: modelPath,
        };
      });
      
      // Query build status from osrm-management-system if available
      let buildStatus: any = null;
      const osrmManagementUrl = process.env.OSRM_MANAGEMENT_URL || 'http://localhost:21520';
      
      try {
        const response = await fetch(`${osrmManagementUrl}/api/v1/builds/status`);
        if (response.ok) {
          const data = await response.json() as { result?: any; [key: string]: any };
          buildStatus = data.result || data;
        }
      } catch (error) {
        logger.warn('Failed to fetch build status from osrm-management-system', { error });
        // Continue without build status
      }
      
      const allExist = fileStatus.every(s => s.exists);
      const existingCount = fileStatus.filter(s => s.exists).length;
      
      res.json(BaseResponse.success({
        models: fileStatus,
        allExist,
        existingCount,
        totalModels: models.length,
        ready: allExist,
        buildStatus, // Include build status from osrm-management-system
      }, allExist 
        ? `All ${models.length} OSRM models are ready` 
        : `Only ${existingCount}/${models.length} OSRM models exist`));
    } catch (error) {
      next(error);
    }
  }
}
