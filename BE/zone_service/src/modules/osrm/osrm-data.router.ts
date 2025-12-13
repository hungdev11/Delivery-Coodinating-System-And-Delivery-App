/**
 * OSRM Data Management Router
 * 
 * Routes for OSRM V2 data generation from database
 */

import { Router } from 'express';
import { OSRMDataController } from './osrm-data.controller';

const router = Router();

// OSRM V2 Generation (replaces old build-all)
router.post('/generate-v2', OSRMDataController.generateV2);

// Status check
router.get('/status', OSRMDataController.getStatus);

export default router;
