import { Router } from 'express';
import { RoadNodesController } from './road_nodes.controller';
import { RoadNodesService } from './road_nodes.service';

const router = Router();
const roadNodesService = new RoadNodesService();
const roadNodesController = new RoadNodesController(roadNodesService);

/**
 * GET /api/v1/road-nodes/nearest
 * Find nearest road nodes to a location
 * Query params: lat, lon, radius (optional, default 100m)
 */
router.get('/nearest', roadNodesController.findNearest);

export default router;
