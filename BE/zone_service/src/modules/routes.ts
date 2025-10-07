/**
 * Main Routes Configuration
 * Combines all module routes
 */

import { Router } from 'express';
import { centerRouter } from './center';
import { zoneRouter } from './zone';

const routes = Router();

// Register module routes
routes.use('/centers', centerRouter);
routes.use('/zones', zoneRouter);

export { routes };
