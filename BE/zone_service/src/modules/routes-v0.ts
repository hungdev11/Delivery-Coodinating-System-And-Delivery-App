/**
 * Main Routes Configuration
 * Combines all module routes
 */

import { Router } from 'express';
import zoneRouterV0 from './zone/v0/zone.routes.v0';

const routes = Router();

// Zone routes - V0, V1, V2
routes.use('/zones', zoneRouterV0);


export { routes };
