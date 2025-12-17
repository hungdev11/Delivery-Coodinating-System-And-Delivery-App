"use strict";
/**
 * OSRM Container Controller
 * RESTful API endpoints for managing OSRM Docker containers
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.OSRMContainerController = void 0;
const osrm_container_service_1 = require("../services/osrm-container.service");
const restful_1 = require("../common/types/restful");
class OSRMContainerController {
    containerService;
    constructor() {
        this.containerService = new osrm_container_service_1.OSRMContainerService();
    }
    /**
     * GET /api/v1/osrm/containers/status
     * Get status of all OSRM containers
     */
    async getStatus(req, res, next) {
        try {
            const statuses = await this.containerService.getStatus();
            res.json(restful_1.BaseResponseBuilder.success(statuses, `Retrieved status for ${statuses.length} containers`));
        }
        catch (error) {
            next(error);
        }
    }
    /**
     * POST /api/v1/osrm/containers/:model/start
     * Start a specific OSRM container
     */
    async startContainer(req, res, next) {
        try {
            const { model } = req.params;
            const result = await this.containerService.startContainer(model);
            if (result.success) {
                res.json(restful_1.BaseResponseBuilder.success(result, result.message));
            }
            else {
                res.status(500).json(restful_1.BaseResponseBuilder.error(result.error || 'Failed to start container'));
            }
        }
        catch (error) {
            next(error);
        }
    }
    /**
     * POST /api/v1/osrm/containers/:model/stop
     * Stop a specific OSRM container
     */
    async stopContainer(req, res, next) {
        try {
            const { model } = req.params;
            const result = await this.containerService.stopContainer(model);
            if (result.success) {
                res.json(restful_1.BaseResponseBuilder.success(result, result.message));
            }
            else {
                res.status(500).json(restful_1.BaseResponseBuilder.error(result.error || 'Failed to stop container'));
            }
        }
        catch (error) {
            next(error);
        }
    }
    /**
     * POST /api/v1/osrm/containers/:model/restart
     * Restart a specific OSRM container
     */
    async restartContainer(req, res, next) {
        try {
            const { model } = req.params;
            const result = await this.containerService.restartContainer(model);
            if (result.success) {
                res.json(restful_1.BaseResponseBuilder.success(result, result.message));
            }
            else {
                res.status(500).json(restful_1.BaseResponseBuilder.error(result.error || 'Failed to restart container'));
            }
        }
        catch (error) {
            next(error);
        }
    }
    /**
     * POST /api/v1/osrm/containers/:model/rebuild
     * Rebuild a specific OSRM container (stop, remove, recreate)
     */
    async rebuildContainer(req, res, next) {
        try {
            const { model } = req.params;
            const result = await this.containerService.rebuildContainer(model);
            if (result.success) {
                res.json(restful_1.BaseResponseBuilder.success(result, result.message));
            }
            else {
                res.status(500).json(restful_1.BaseResponseBuilder.error(result.error || 'Failed to rebuild container'));
            }
        }
        catch (error) {
            next(error);
        }
    }
    /**
     * GET /api/v1/osrm/containers/:model/health
     * Health check for a specific OSRM container
     */
    async healthCheck(req, res, next) {
        try {
            const { model } = req.params;
            const result = await this.containerService.healthCheck(model);
            if (result.healthy) {
                res.json(restful_1.BaseResponseBuilder.success(result, result.message));
            }
            else {
                res.status(503).json(restful_1.BaseResponseBuilder.error(result.message));
            }
        }
        catch (error) {
            next(error);
        }
    }
}
exports.OSRMContainerController = OSRMContainerController;
//# sourceMappingURL=osrm-container.controller.js.map