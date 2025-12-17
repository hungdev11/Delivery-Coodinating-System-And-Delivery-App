/**
 * OSRM Container Controller
 * RESTful API endpoints for managing OSRM Docker containers
 */

import { Request, Response, NextFunction } from 'express';
import { OSRMContainerService } from '../services/osrm-container.service';
import { BaseResponseBuilder } from '../common/types/restful';
import { logger } from '../common/logger';

export class OSRMContainerController {
  private containerService: OSRMContainerService;

  constructor() {
    this.containerService = new OSRMContainerService();
  }

  /**
   * GET /api/v1/osrm/containers/status
   * Get status of all OSRM containers
   */
  async getStatus(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const statuses = await this.containerService.getStatus();
      res.json(BaseResponseBuilder.success(statuses, `Retrieved status for ${statuses.length} containers`));
    } catch (error) {
      next(error);
    }
  }

  /**
   * POST /api/v1/osrm/containers/:model/start
   * Start a specific OSRM container
   */
  async startContainer(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { model } = req.params;
      const result = await this.containerService.startContainer(model);

      if (result.success) {
        res.json(BaseResponseBuilder.success(result, result.message));
      } else {
        res.status(500).json(BaseResponseBuilder.error(result.error || 'Failed to start container'));
      }
    } catch (error) {
      next(error);
    }
  }

  /**
   * POST /api/v1/osrm/containers/:model/stop
   * Stop a specific OSRM container
   */
  async stopContainer(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { model } = req.params;
      const result = await this.containerService.stopContainer(model);

      if (result.success) {
        res.json(BaseResponseBuilder.success(result, result.message));
      } else {
        res.status(500).json(BaseResponseBuilder.error(result.error || 'Failed to stop container'));
      }
    } catch (error) {
      next(error);
    }
  }

  /**
   * POST /api/v1/osrm/containers/:model/restart
   * Restart a specific OSRM container
   */
  async restartContainer(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { model } = req.params;
      const result = await this.containerService.restartContainer(model);

      if (result.success) {
        res.json(BaseResponseBuilder.success(result, result.message));
      } else {
        res.status(500).json(BaseResponseBuilder.error(result.error || 'Failed to restart container'));
      }
    } catch (error) {
      next(error);
    }
  }

  /**
   * POST /api/v1/osrm/containers/:model/rebuild
   * Rebuild a specific OSRM container (stop, remove, recreate)
   */
  async rebuildContainer(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { model } = req.params;
      const result = await this.containerService.rebuildContainer(model);

      if (result.success) {
        res.json(BaseResponseBuilder.success(result, result.message));
      } else {
        res.status(500).json(BaseResponseBuilder.error(result.error || 'Failed to rebuild container'));
      }
    } catch (error) {
      next(error);
    }
  }

  /**
   * GET /api/v1/osrm/containers/:model/health
   * Health check for a specific OSRM container
   */
  async healthCheck(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { model } = req.params;
      const result = await this.containerService.healthCheck(model);

      if (result.healthy) {
        res.json(BaseResponseBuilder.success(result, result.message));
      } else {
        res.status(503).json(BaseResponseBuilder.error(result.message));
      }
    } catch (error) {
      next(error);
    }
  }
}
