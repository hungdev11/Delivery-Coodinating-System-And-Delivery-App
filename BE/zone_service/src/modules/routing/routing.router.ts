/**
 * Routing Router
 * Defines routes for routing/navigation
 */

import { Router } from 'express';
import { RoutingController } from './routing.controller';

export const routingRouter = Router();

/**
 * POST /routing/route
 * Calculate route between multiple waypoints
 */
routingRouter.post('/route', RoutingController.calculateRoute);

/**
 * POST /routing/priority-route
 * Calculate priority-based multi-stop route (for delivery)
 */
routingRouter.post('/priority-route', RoutingController.calculatePriorityRoute);

/**
 * GET /routing/simple
 * Get route between two points (simple query params)
 */
routingRouter.get('/simple', RoutingController.getSimpleRoute);

/**
 * GET /routing/status
 * Get OSRM instances status (active instance, health checks)
 */
routingRouter.get('/status', RoutingController.getOSRMStatus);

/**
 * POST /routing/switch-instance
 * Switch active OSRM instance (admin endpoint)
 */
routingRouter.post('/switch-instance', RoutingController.switchOSRMInstance);

/**
 * POST /routing/demo-route
 * Calculate demo route with priority-based ordering
 * For demo/testing page with express/fast/normal/economy priority groups
 */
routingRouter.post('/demo-route', RoutingController.calculateDemoRoute);
