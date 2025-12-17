/**
 * Build Status Controller
 * Provides endpoints for zone_service to query build status
 */
import { Request, Response, NextFunction } from 'express';
import { PrismaClient } from '@prisma/client';
export declare class BuildStatusController {
    private buildTracker;
    private prisma;
    constructor(prisma: PrismaClient);
    /**
     * GET /api/v1/builds/status
     * Get build status for all instances
     */
    getBuildStatus(req: Request, res: Response, next: NextFunction): Promise<void>;
    /**
     * GET /api/v1/builds/status/:model
     * Get build status for a specific model
     */
    getModelBuildStatus(req: Request, res: Response, next: NextFunction): Promise<void>;
    /**
     * GET /api/v1/builds/history
     * Get build history for all models
     */
    getBuildHistory(req: Request, res: Response, next: NextFunction): Promise<void>;
}
//# sourceMappingURL=build-status.controller.d.ts.map