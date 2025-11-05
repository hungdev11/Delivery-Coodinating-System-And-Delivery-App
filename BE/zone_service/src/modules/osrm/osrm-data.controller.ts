/**
 * OSRM Data Management Controller
 * 
 * Provides REST API endpoints for:
 * - Building OSRM data
 * - Managing OSRM instances
 * - Rolling restart functionality
 * - Health monitoring
 */

import { Request, Response, NextFunction } from 'express';
import { BaseResponse } from '../../common/types/restful';
import { OSRMDataManagerService } from '../../services/osrm/osrm-data-manager.service';
import { logger } from '../../common/logger';
import { prisma } from '../../common/database/prisma.client';

export class OSRMDataController {
  private static dataManager = new OSRMDataManagerService(prisma);

  /**
   * Build OSRM data for specific instance
   * POST /api/v1/osrm/build/:instanceId
   */
  public static async buildInstance(_req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const instanceId = parseInt(_req.params.instanceId || '0');
      
      if (isNaN(instanceId) || (instanceId !== 1 && instanceId !== 2)) {
        res.status(400).json(BaseResponse.error('Invalid instance ID. Must be 1 or 2'));
        return;
      }

      logger.info(`Building OSRM data for instance ${instanceId}`);
      const result = await OSRMDataController.dataManager.buildOSRMData(instanceId);
      
      if (result.success) {
        res.json(BaseResponse.success(result, `OSRM data built successfully for instance ${instanceId}`));
      } else {
        res.status(500).json(BaseResponse.error(`Failed to build OSRM data: ${result.error}`));
      }
    } catch (error) {
      next(error);
    }
  }

  /**
   * Build OSRM data for all instances
   * POST /api/v1/osrm/build-all
   */
  public static async buildAllInstances(_req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      logger.info('Building OSRM data for all instances');
      const results = await OSRMDataController.dataManager.buildAllOSRMData();
      
      const successCount = results.filter(r => r.success).length;
      const message = `OSRM data build completed: ${successCount}/2 instances successful`;
      
      res.json(BaseResponse.success(results, message));
    } catch (error) {
      next(error);
    }
  }

  /**
   * Start OSRM instance
   * POST /api/v1/osrm/start/:instanceId
   */
  public static async startInstance(_req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const instanceId = parseInt(_req.params.instanceId || '0');
      
      if (isNaN(instanceId) || (instanceId !== 1 && instanceId !== 2)) {
        res.status(400).json(BaseResponse.error('Invalid instance ID. Must be 1 or 2'));
        return;
      }

      logger.info(`Starting OSRM instance ${instanceId}`);
      const success = await OSRMDataController.dataManager.startOSRMInstance(instanceId);
      
      if (success) {
        res.json(BaseResponse.success({ instanceId }, `OSRM instance ${instanceId} started successfully`));
      } else {
        res.status(500).json(BaseResponse.error(`Failed to start OSRM instance ${instanceId}`));
      }
    } catch (error) {
      next(error);
    }
  }

  /**
   * Stop OSRM instance
   * POST /api/v1/osrm/stop/:instanceId
   */
  public static async stopInstance(_req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const instanceId = parseInt(_req.params.instanceId || '0');
      
      if (isNaN(instanceId) || (instanceId !== 1 && instanceId !== 2)) {
        res.status(400).json(BaseResponse.error('Invalid instance ID. Must be 1 or 2'));
        return;
      }

      logger.info(`Stopping OSRM instance ${instanceId}`);
      const success = await OSRMDataController.dataManager.stopOSRMInstance(instanceId);
      
      if (success) {
        res.json(BaseResponse.success({ instanceId }, `OSRM instance ${instanceId} stopped successfully`));
      } else {
        res.status(500).json(BaseResponse.error(`Failed to stop OSRM instance ${instanceId}`));
      }
    } catch (error) {
      next(error);
    }
  }

  /**
   * Rolling restart of OSRM instances
   * POST /api/v1/osrm/rolling-restart
   */
  public static async rollingRestart(_req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      logger.info('Starting rolling restart of OSRM instances');
      const result = await OSRMDataController.dataManager.rollingRestart();
      
      if (result.success) {
        res.json(BaseResponse.success(result, 'Rolling restart completed successfully'));
      } else {
        res.status(500).json(BaseResponse.error(`Rolling restart failed: ${result.error}`));
      }
    } catch (error) {
      next(error);
    }
  }

  /**
   * Get OSRM instance status
   * GET /api/v1/osrm/status/:instanceId
   */
  public static async getInstanceStatus(_req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const instanceId = parseInt(_req.params.instanceId || '0');
      
      if (isNaN(instanceId) || (instanceId !== 1 && instanceId !== 2)) {
        res.status(400).json(BaseResponse.error('Invalid instance ID. Must be 1 or 2'));
        return;
      }

      const status = OSRMDataController.dataManager.getInstanceStatus(instanceId);
      
      if (status) {
        res.json(BaseResponse.success(status));
      } else {
        res.status(404).json(BaseResponse.error('Instance not found'));
      }
    } catch (error) {
      next(error);
    }
  }

  /**
   * Get all OSRM instances status
   * GET /api/v1/osrm/status
   */
  public static async getAllInstancesStatus(_req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const instances = OSRMDataController.dataManager.getAllInstancesStatus();
      const activeInstance = OSRMDataController.dataManager.getActiveInstance();
      
      res.json(BaseResponse.success({
        instances,
        activeInstance,
        totalInstances: instances.length
      }));
    } catch (error) {
      next(error);
    }
  }

  /**
   * Health check for all OSRM instances
   * GET /api/v1/osrm/health
   */
  public static async healthCheck(_req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const healthResults = await OSRMDataController.dataManager.checkHealth();
      const activeInstance = OSRMDataController.dataManager.getActiveInstance();
      
      const healthyCount = healthResults.filter(r => r.healthy).length;
      const overallHealthy = healthyCount > 0;
      
      res.json(BaseResponse.success({
        overallHealthy,
        activeInstance,
        instances: healthResults,
        healthyCount,
        totalInstances: healthResults.length
      }));
    } catch (error) {
      next(error);
    }
  }

  /**
   * Validate OSRM data integrity
   * GET /api/v1/osrm/validate/:instanceId
   */
  public static async validateData(_req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const instanceId = parseInt(_req.params.instanceId || '0');
      
      if (isNaN(instanceId) || (instanceId !== 1 && instanceId !== 2)) {
        res.status(400).json(BaseResponse.error('Invalid instance ID. Must be 1 or 2'));
        return;
      }

      const isValid = await OSRMDataController.dataManager.validateData(instanceId);
      
      res.json(BaseResponse.success({
        instanceId,
        valid: isValid
      }, isValid ? 'Data validation passed' : 'Data validation failed'));
    } catch (error) {
      next(error);
    }
  }

  /**
   * Get build history for specific instance
   * GET /api/v1/osrm/history/:instanceId
   */
  public static async getBuildHistory(_req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const instanceId = parseInt(_req.params.instanceId || '0');
      const limit = parseInt(_req.query.limit as string) || 10;
      
      if (isNaN(instanceId) || (instanceId !== 1 && instanceId !== 2)) {
        res.status(400).json(BaseResponse.error('Invalid instance ID. Must be 1 or 2'));
        return;
      }

      const history = await OSRMDataController.dataManager.getBuildHistory(instanceId, limit);
      
      res.json(BaseResponse.success(history));
    } catch (error) {
      next(error);
    }
  }

  /**
   * Get all build history
   * GET /api/v1/osrm/history
   */
  public static async getAllBuildHistory(_req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const limit = parseInt(_req.query.limit as string) || 20;
      const history = await OSRMDataController.dataManager.getAllBuildHistory(limit);
      
      res.json(BaseResponse.success(history));
    } catch (error) {
      next(error);
    }
  }

  /**
   * Get deployment status
   * GET /api/v1/osrm/deployment
   */
  public static async getDeploymentStatus(_req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const status = await OSRMDataController.dataManager.getDeploymentStatus();
      
      res.json(BaseResponse.success(status));
    } catch (error) {
      next(error);
    }
  }
}
