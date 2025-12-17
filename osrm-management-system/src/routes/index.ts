/**
 * Routes Configuration
 */

import { Router } from 'express';
import { ExtractController } from '../controllers/extract.controller';
import { GenerateController } from '../controllers/generate.controller';
import { HealthController } from '../controllers/health.controller';
import { OSRMContainerController } from '../controllers/osrm-container.controller';
import { BuildStatusController } from '../controllers/build-status.controller';
import { PrismaClient } from '@prisma/client';

export function createRoutes(prisma: PrismaClient): Router {
  const router = Router();

  // Controllers
  const healthController = new HealthController();
  const extractController = new ExtractController(prisma);
  const generateController = new GenerateController(prisma);
  const containerController = new OSRMContainerController();
  const buildStatusController = new BuildStatusController(prisma);

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
