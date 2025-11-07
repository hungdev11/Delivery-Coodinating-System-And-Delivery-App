/**
 * Zone Routes V2
 * Enhanced filtering
 */

import express from 'express';
import { ZoneControllerV2 } from './zone.controller.v2';

const router = express.Router();

router.post('/', ZoneControllerV2.getZones);

export default router;
