/**
 * OSRM Data Management Controller
 * 
 * Provides REST API endpoints for:
 * - Generating OSRM V2 data from database (all 4 models) - delegates to osrm-management-system
 * - Getting OSRM status - delegates to osrm-management-system
 */

import { Request, Response, NextFunction } from 'express';
import { BaseResponse } from '../../common/types/restful';
import { logger } from '../../common/logger';
import { OSRMManagementClientService } from '../../services/osrm/osrm-management-client.service';

// Singleton instance
let osrmClient: OSRMManagementClientService | null = null;

function getOSRMClient(): OSRMManagementClientService {
  if (!osrmClient) {
    osrmClient = new OSRMManagementClientService();
  }
  return osrmClient;
}

export class OSRMDataController {
  /**
   * Generate OSRM V2 data from database (all 4 models)
   * POST /api/v1/osrm/generate-v2
   * 
   * This endpoint delegates to osrm-management-system to generate all OSRM models
   * (osrm-full, osrm-rating-only, osrm-blocking-only, osrm-base) from the current database state.
   */
  public static async generateV2(_req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      logger.info('Starting OSRM V2 generation via API (delegating to osrm-management-system)...');
      
      const client = getOSRMClient();
      const result = await client.generateOSRMV2();
      
      if (result.success) {
        res.json(BaseResponse.success(result, `OSRM V2 generation completed: ${result.models?.length || 0} models`));
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
      
      // Query build status and container status from osrm-management-system
      const client = getOSRMClient();
      const [buildStatus, containerStatuses] = await Promise.all([
        client.getBuildStatus(),
        client.getContainerStatus(),
      ]);
      
      // Merge container status with file status
      const modelsWithStatus = fileStatus.map(model => {
        const containerStatus = containerStatuses.find(c => c.model === model.name);
        return {
          ...model,
          containerStatus: containerStatus ? {
            status: containerStatus.status,
            health: containerStatus.health,
          } : null,
        };
      });
      
      const allExist = fileStatus.every(s => s.exists);
      const existingCount = fileStatus.filter(s => s.exists).length;
      
      res.json(BaseResponse.success({
        models: modelsWithStatus,
        allExist,
        existingCount,
        totalModels: models.length,
        ready: allExist,
        buildStatus, // Include build status from osrm-management-system
        containerStatuses, // Include container statuses
      }, allExist 
        ? `All ${models.length} OSRM models are ready` 
        : `Only ${existingCount}/${models.length} OSRM models exist`));
    } catch (error) {
      next(error);
    }
  }

  /**
   * Extract complete OSM data
   * POST /api/v1/osrm/extract/complete
   * 
   * Delegates to osrm-management-system
   */
  public static async extractComplete(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      logger.info('Extract complete request received (delegating to osrm-management-system)...');
      
      const client = getOSRMClient();
      const result = await client.extractComplete(req.body);
      
      if (result.success) {
        res.json(BaseResponse.success(result, 'Extraction completed successfully'));
      } else {
        res.status(500).json(BaseResponse.error(result.error || 'Extraction failed'));
      }
    } catch (error) {
      next(error);
    }
  }

  /**
   * Get container status
   * GET /api/v1/osrm/containers/status
   */
  public static async getContainerStatus(_req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const client = getOSRMClient();
      const statuses = await client.getContainerStatus();
      
      res.json(BaseResponse.success(statuses, `Retrieved status for ${statuses.length} containers`));
    } catch (error) {
      next(error);
    }
  }

  /**
   * Start OSRM container
   * POST /api/v1/osrm/containers/:model/start
   */
  public static async startContainer(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { model } = req.params;
      if (!model) {
        res.status(400).json(BaseResponse.error('Model parameter is required'));
        return;
      }
      
      const client = getOSRMClient();
      const result = await client.startContainer(model);
      
      if (result.success) {
        res.json(BaseResponse.success(result, result.message || 'Container started successfully'));
      } else {
        res.status(500).json(BaseResponse.error(result.error || 'Failed to start container'));
      }
    } catch (error) {
      next(error);
    }
  }

  /**
   * Stop OSRM container
   * POST /api/v1/osrm/containers/:model/stop
   */
  public static async stopContainer(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { model } = req.params;
      if (!model) {
        res.status(400).json(BaseResponse.error('Model parameter is required'));
        return;
      }
      
      const client = getOSRMClient();
      const result = await client.stopContainer(model);
      
      if (result.success) {
        res.json(BaseResponse.success(result, result.message || 'Container stopped successfully'));
      } else {
        res.status(500).json(BaseResponse.error(result.error || 'Failed to stop container'));
      }
    } catch (error) {
      next(error);
    }
  }

  /**
   * Restart OSRM container
   * POST /api/v1/osrm/containers/:model/restart
   */
  public static async restartContainer(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { model } = req.params;
      if (!model) {
        res.status(400).json(BaseResponse.error('Model parameter is required'));
        return;
      }
      
      const client = getOSRMClient();
      const result = await client.restartContainer(model);
      
      if (result.success) {
        res.json(BaseResponse.success(result, result.message || 'Container restarted successfully'));
      } else {
        res.status(500).json(BaseResponse.error(result.error || 'Failed to restart container'));
      }
    } catch (error) {
      next(error);
    }
  }

  /**
   * Health check for OSRM container
   * GET /api/v1/osrm/containers/:model/health
   */
  public static async healthCheck(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { model } = req.params;
      if (!model) {
        res.status(400).json(BaseResponse.error('Model parameter is required'));
        return;
      }
      
      const client = getOSRMClient();
      const result = await client.healthCheck(model);
      
      if (result.healthy) {
        res.json(BaseResponse.success(result, result.message || 'Container is healthy'));
      } else {
        res.status(503).json(BaseResponse.error(result.message || 'Container is unhealthy'));
      }
    } catch (error) {
      next(error);
    }
  }
}
