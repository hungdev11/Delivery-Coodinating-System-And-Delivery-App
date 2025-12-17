"use strict";
/**
 * Routes Configuration
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.createRoutes = createRoutes;
const express_1 = require("express");
const extract_controller_1 = require("../controllers/extract.controller");
const generate_controller_1 = require("../controllers/generate.controller");
const health_controller_1 = require("../controllers/health.controller");
const osrm_container_controller_1 = require("../controllers/osrm-container.controller");
const build_status_controller_1 = require("../controllers/build-status.controller");
function createRoutes(prisma) {
    const router = (0, express_1.Router)();
    // Controllers
    const healthController = new health_controller_1.HealthController();
    const extractController = new extract_controller_1.ExtractController(prisma);
    const generateController = new generate_controller_1.GenerateController(prisma);
    const containerController = new osrm_container_controller_1.OSRMContainerController();
    const buildStatusController = new build_status_controller_1.BuildStatusController(prisma);
    // Health check
    router.get('/api/v1/health', (req, res, next) => healthController.health(req, res).catch(next));
    // Extract endpoints
    router.post('/api/v1/extract/complete', (req, res, next) => extractController.extractComplete(req, res, next).catch(next));
    // Generate endpoints
    router.post('/api/v1/generate/osrm-v2', (req, res, next) => generateController.generateOSRMV2(req, res, next).catch(next));
    // Build Status endpoints (for zone_service)
    router.get('/api/v1/builds/status', (req, res, next) => buildStatusController.getBuildStatus(req, res, next).catch(next));
    router.get('/api/v1/builds/status/:model', (req, res, next) => buildStatusController.getModelBuildStatus(req, res, next).catch(next));
    router.get('/api/v1/builds/history', (req, res, next) => buildStatusController.getBuildHistory(req, res, next).catch(next));
    // OSRM Container management endpoints
    router.get('/api/v1/osrm/containers/status', (req, res, next) => containerController.getStatus(req, res, next).catch(next));
    router.post('/api/v1/osrm/containers/:model/start', (req, res, next) => containerController.startContainer(req, res, next).catch(next));
    router.post('/api/v1/osrm/containers/:model/stop', (req, res, next) => containerController.stopContainer(req, res, next).catch(next));
    router.post('/api/v1/osrm/containers/:model/restart', (req, res, next) => containerController.restartContainer(req, res, next).catch(next));
    router.post('/api/v1/osrm/containers/:model/rebuild', (req, res, next) => containerController.rebuildContainer(req, res, next).catch(next));
    router.get('/api/v1/osrm/containers/:model/health', (req, res, next) => containerController.healthCheck(req, res, next).catch(next));
    return router;
}
//# sourceMappingURL=index.js.map