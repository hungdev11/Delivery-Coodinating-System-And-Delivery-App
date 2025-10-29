/**
 * Zone Router
 * Defines routes for zone management
 */

import { Router } from 'express';
import { ZoneController } from './zone.controller';

export const zoneRouter = Router();

// Create zone service instance
const zoneService = new (require('../../common/services/zone-service').ZoneService)(require('@prisma/client').PrismaClient);
const zoneController = new ZoneController(zoneService);

// Get zones with filtering and sorting
zoneRouter.post('/', zoneController.getZones.bind(zoneController));

// Get zone by ID
zoneRouter.get('/:id', zoneController.getZoneById.bind(zoneController));

// Create zone
zoneRouter.post('/create', zoneController.createZone.bind(zoneController));

// Update zone
zoneRouter.put('/:id', zoneController.updateZone.bind(zoneController));

// Delete zone
zoneRouter.delete('/:id', zoneController.deleteZone.bind(zoneController));

// Get filterable fields
zoneRouter.get('/filterable-fields', zoneController.getFilterableFields.bind(zoneController));

// Get sortable fields
zoneRouter.get('/sortable-fields', zoneController.getSortableFields.bind(zoneController));
