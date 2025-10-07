/**
 * Zone Router
 * Defines routes for zone management
 */

import { Router } from 'express';
import { ZoneController } from './zone.controller';

export const zoneRouter = Router();

// Get all zones
zoneRouter.get('/', ZoneController.getAllZones);

// Get zone by ID
zoneRouter.get('/:id', ZoneController.getZoneById);

// Get zone by code
zoneRouter.get('/code/:code', ZoneController.getZoneByCode);

// Get zones by center ID
zoneRouter.get('/center/:centerId', ZoneController.getZonesByCenterId);

// Create zone
zoneRouter.post('/', ZoneController.createZone);

// Update zone
zoneRouter.put('/:id', ZoneController.updateZone);

// Delete zone
zoneRouter.delete('/:id', ZoneController.deleteZone);
