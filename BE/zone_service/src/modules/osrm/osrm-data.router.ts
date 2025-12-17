/**
 * OSRM Data Management Router
 * 
 * Routes for OSRM V2 data generation from database
 * All operations delegate to osrm-management-system
 */

import { Router } from 'express';
import { OSRMDataController } from './osrm-data.controller';

const router = Router();

// OSRM V2 Generation (delegates to osrm-management-system)
router.post('/generate-v2', OSRMDataController.generateV2);

// Status check (delegates to osrm-management-system)
router.get('/status', OSRMDataController.getStatus);

// Extract operations (delegates to osrm-management-system)
router.post('/extract/complete', OSRMDataController.extractComplete);

// Container management (delegates to osrm-management-system)
router.get('/containers/status', OSRMDataController.getContainerStatus);
router.post('/containers/:model/start', OSRMDataController.startContainer);
router.post('/containers/:model/stop', OSRMDataController.stopContainer);
router.post('/containers/:model/restart', OSRMDataController.restartContainer);
router.get('/containers/:model/health', OSRMDataController.healthCheck);

export default router;
