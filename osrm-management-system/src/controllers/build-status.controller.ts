/**
 * Build Status Controller
 * Provides endpoints for zone_service to query build status
 */

import { Request, Response, NextFunction } from 'express';
import { BuildTrackerService } from '../services/build-tracker.service';
import { BaseResponseBuilder } from '../common/types/restful';
import { PrismaClient } from '@prisma/client';
import { logger } from '../common/logger';

export class BuildStatusController {
  private buildTracker: BuildTrackerService;
  private prisma: PrismaClient;

  constructor(prisma: PrismaClient) {
    this.prisma = prisma;
    this.buildTracker = new BuildTrackerService(prisma);
  }

  /**
   * GET /api/v1/builds/status
   * Get build status for all instances
   */
  async getBuildStatus(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const models = ['osrm-full', 'osrm-rating-only', 'osrm-blocking-only', 'osrm-base'];
      
      const statuses = await Promise.all(
        models.map(async (model) => {
          const currentBuild = await this.buildTracker.getCurrentBuild(model);
          const latestReady = await this.buildTracker.getLatestReadyBuild(model);
          const latestDeployed = await this.buildTracker.getLatestDeployedBuild(model);

          return {
            model,
            currentBuild: currentBuild ? {
              buildId: currentBuild.buildId,
              status: currentBuild.status,
              startedAt: currentBuild.status === 'BUILDING' ? new Date() : null,
            } : null,
            latestReady: latestReady ? {
              buildId: latestReady.buildId,
              completedAt: new Date(),
              outputPath: latestReady.osrmOutputPath,
            } : null,
            latestDeployed: latestDeployed ? {
              buildId: latestDeployed.buildId,
              deployedAt: new Date(),
            } : null,
          };
        })
      );

      res.json(BaseResponseBuilder.success(statuses, 'Build status retrieved'));
    } catch (error) {
      next(error);
    }
  }

  /**
   * GET /api/v1/builds/status/:model
   * Get build status for a specific model
   */
  async getModelBuildStatus(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { model } = req.params;

      const currentBuild = await this.buildTracker.getCurrentBuild(model);
      const latestReady = await this.buildTracker.getLatestReadyBuild(model);
      const latestDeployed = await this.buildTracker.getLatestDeployedBuild(model);
      const history = await this.buildTracker.getBuildHistory(model, 10);

      res.json(BaseResponseBuilder.success({
        model,
        currentBuild,
        latestReady,
        latestDeployed,
        history,
      }, `Build status for ${model}`));
    } catch (error) {
      next(error);
    }
  }

  /**
   * GET /api/v1/builds/history
   * Get build history for all models
   */
  async getBuildHistory(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const limit = parseInt(req.query.limit as string) || 20;
      const model = req.query.model as string;

      let history;
      if (model) {
        history = await this.buildTracker.getBuildHistory(model, limit);
      } else {
        // Get history for all models
        const models = ['osrm-full', 'osrm-rating-only', 'osrm-blocking-only', 'osrm-base'];
        const allHistory = await Promise.all(
          models.map(m => this.buildTracker.getBuildHistory(m, limit))
        );
        history = allHistory.flat();
      }

      res.json(BaseResponseBuilder.success(history, 'Build history retrieved'));
    } catch (error) {
      next(error);
    }
  }
}
