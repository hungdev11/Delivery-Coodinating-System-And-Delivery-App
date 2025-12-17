"use strict";
/**
 * Build Status Controller
 * Provides endpoints for zone_service to query build status
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.BuildStatusController = void 0;
const build_tracker_service_1 = require("../services/build-tracker.service");
const restful_1 = require("../common/types/restful");
class BuildStatusController {
    buildTracker;
    prisma;
    constructor(prisma) {
        this.prisma = prisma;
        this.buildTracker = new build_tracker_service_1.BuildTrackerService(prisma);
    }
    /**
     * GET /api/v1/builds/status
     * Get build status for all instances
     */
    async getBuildStatus(req, res, next) {
        try {
            const models = ['osrm-full', 'osrm-rating-only', 'osrm-blocking-only', 'osrm-base'];
            const statuses = await Promise.all(models.map(async (model) => {
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
            }));
            res.json(restful_1.BaseResponseBuilder.success(statuses, 'Build status retrieved'));
        }
        catch (error) {
            next(error);
        }
    }
    /**
     * GET /api/v1/builds/status/:model
     * Get build status for a specific model
     */
    async getModelBuildStatus(req, res, next) {
        try {
            const { model } = req.params;
            const currentBuild = await this.buildTracker.getCurrentBuild(model);
            const latestReady = await this.buildTracker.getLatestReadyBuild(model);
            const latestDeployed = await this.buildTracker.getLatestDeployedBuild(model);
            const history = await this.buildTracker.getBuildHistory(model, 10);
            res.json(restful_1.BaseResponseBuilder.success({
                model,
                currentBuild,
                latestReady,
                latestDeployed,
                history,
            }, `Build status for ${model}`));
        }
        catch (error) {
            next(error);
        }
    }
    /**
     * GET /api/v1/builds/history
     * Get build history for all models
     */
    async getBuildHistory(req, res, next) {
        try {
            const limit = parseInt(req.query.limit) || 20;
            const model = req.query.model;
            let history;
            if (model) {
                history = await this.buildTracker.getBuildHistory(model, limit);
            }
            else {
                // Get history for all models
                const models = ['osrm-full', 'osrm-rating-only', 'osrm-blocking-only', 'osrm-base'];
                const allHistory = await Promise.all(models.map(m => this.buildTracker.getBuildHistory(m, limit)));
                history = allHistory.flat();
            }
            res.json(restful_1.BaseResponseBuilder.success(history, 'Build history retrieved'));
        }
        catch (error) {
            next(error);
        }
    }
}
exports.BuildStatusController = BuildStatusController;
//# sourceMappingURL=build-status.controller.js.map