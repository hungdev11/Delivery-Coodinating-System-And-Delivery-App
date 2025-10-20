/**
 * Zone Routes with advanced filtering and sorting
 */

import { Router } from 'express';
import { ZoneController } from './zone.controller';
import { ZoneService } from '../../common/services/zone-service';
import { PrismaClient } from '@prisma/client';

const router = Router();
const prisma = new PrismaClient();
const zoneService = new ZoneService(prisma);
const zoneController = new ZoneController(zoneService);

// Zone CRUD operations with new API standard
router.post('/', zoneController.getZones.bind(zoneController)); // Get zones with filtering/sorting
router.get('/:id', zoneController.getZoneById.bind(zoneController)); // Get zone by ID
router.post('/create', zoneController.createZone.bind(zoneController)); // Create zone
router.put('/:id', zoneController.updateZone.bind(zoneController)); // Update zone
router.delete('/:id', zoneController.deleteZone.bind(zoneController)); // Delete zone

// Query metadata endpoints
router.get('/filterable-fields', zoneController.getFilterableFields.bind(zoneController));
router.get('/sortable-fields', zoneController.getSortableFields.bind(zoneController));

export default router;
