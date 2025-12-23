import { Request, Response } from 'express';
import { RoadNodesService } from './road_nodes.service';

export class RoadNodesController {
  constructor(private readonly roadNodesService: RoadNodesService) {}

  /**
   * GET /api/v1/road-nodes/nearest
   * Find nearest road nodes to a location
   */
  findNearest = async (req: Request, res: Response) => {
    try {
      const lat = Number(req.query.lat);
      const lon = Number(req.query.lon);
      const radius = req.query.radius ? Number(req.query.radius) : 100;

      const isValidNumber = (n: number) => Number.isFinite(n);

      if (
        !isValidNumber(lat) ||
        !isValidNumber(lon) ||
        lat < -90 ||
        lat > 90 ||
        lon < -180 ||
        lon > 180
      ) {
        return res.status(400).json({ error: 'Invalid lat/lon' });
      }

      if (!isValidNumber(radius) || radius <= 0) {
        return res.status(400).json({ error: 'Invalid radius' });
      }

      const nodes = await this.roadNodesService.findNearestNodes(lat, lon, radius || 100);
      return res.json(nodes);
    } catch (error) {
      console.error('[zone-service] [RoadNodesController.findNearest] Error:', error);
      return res.status(500).json({ error: 'Failed to find nearest nodes' });
    }
  };
}
