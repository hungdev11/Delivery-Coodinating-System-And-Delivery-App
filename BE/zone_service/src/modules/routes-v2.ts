/**
 * Main Routes Configuration
 * Combines all module routes
 */

import { Router } from 'express';
import zoneRouterV0 from './zone/v0/zone.routes.v0';
import zoneRouterV2 from './zone/v2/zone.routes.v2';

const routes = Router();

// Zone routes - V0, V1, V2
routes.use('/v0/zones', zoneRouterV0);
routes.use('/zones', zoneRouterV2);


export { routes };
