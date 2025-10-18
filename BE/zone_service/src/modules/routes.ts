/**
 * Main Routes Configuration
 * Combines all module routes
 */

import { Router } from 'express';
import { centerRouter } from './center';
import { zoneRouter } from './zone';
import { routingRouter } from './routing';
import { addressRouter } from './address';

const routes = Router();

// Register module routes
routes.use('/centers', centerRouter);
routes.use('/zones', zoneRouter);
routes.use('/routing', routingRouter);
routes.use('/addresses', addressRouter);

export { routes };
