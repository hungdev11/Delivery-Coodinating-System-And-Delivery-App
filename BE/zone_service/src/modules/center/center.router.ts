/**
 * Center Router
 * Defines routes for center management
 */

import { Router } from 'express';
import { CenterController } from './center.controller';

export const centerRouter = Router();

// Get all centers
centerRouter.get('/', CenterController.getAllCenters);

// Get center by ID
centerRouter.get('/:id', CenterController.getCenterById);

// Get center by code
centerRouter.get('/code/:code', CenterController.getCenterByCode);

// Create center
centerRouter.post('/', CenterController.createCenter);

// Update center
centerRouter.put('/:id', CenterController.updateCenter);

// Delete center
centerRouter.delete('/:id', CenterController.deleteCenter);
