/**
 * Zone Routes V0
 * Simple paging and sorting
 */

import express from 'express';
import { ZoneControllerV0 } from './zone.controller.v0';

const router = express.Router();

router.post('/', ZoneControllerV0.getZones);

export default router;
