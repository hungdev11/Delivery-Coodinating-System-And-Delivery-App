/**
 * Main Routes Configuration
 * Combines all module routes
 */

import { Router } from 'express';
import { centerRouter } from './center';
import { zoneRouter } from './zone';
import zoneRouterV0 from './zone/v0/zone.routes.v0';
import zoneRouterV2 from './zone/v2/zone.routes.v2';
import { routingRouter } from './routing';
import { addressRouter } from './address';
import { osrmDataRouter } from './osrm';

const routes = Router();

// Register module routes
routes.use('/centers', centerRouter);

// Zone routes - V0, V1, V2
routes.use('/v0/zones', zoneRouterV0);
routes.use('/zones', zoneRouter); // V1 (default)
routes.use('/v2/zones', zoneRouterV2);

routes.use('/routing', routingRouter);
routes.use('/addresses', addressRouter);
routes.use('/osrm', osrmDataRouter);

export { routes };
