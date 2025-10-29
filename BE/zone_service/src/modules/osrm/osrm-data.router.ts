/**
 * OSRM Data Management Router
 * 
 * Routes for OSRM data building, instance management, and rolling restart
 */

import { Router } from 'express';
import { OSRMDataController } from './osrm-data.controller';

const router = Router();

// OSRM Data Building
router.post('/build/:instanceId', OSRMDataController.buildInstance);
router.post('/build-all', OSRMDataController.buildAllInstances);

// OSRM Instance Management
router.post('/start/:instanceId', OSRMDataController.startInstance);
router.post('/stop/:instanceId', OSRMDataController.stopInstance);
router.post('/rolling-restart', OSRMDataController.rollingRestart);

// Status and Health
router.get('/status', OSRMDataController.getAllInstancesStatus);
router.get('/status/:instanceId', OSRMDataController.getInstanceStatus);
router.get('/health', OSRMDataController.healthCheck);

// Data Validation
router.get('/validate/:instanceId', OSRMDataController.validateData);

// Build History
router.get('/history', OSRMDataController.getAllBuildHistory);
router.get('/history/:instanceId', OSRMDataController.getBuildHistory);

// Deployment Status
router.get('/deployment', OSRMDataController.getDeploymentStatus);

export default router;
