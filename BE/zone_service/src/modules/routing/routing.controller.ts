/**
 * Routing Controller
 * Handles HTTP requests for routing
 */

import { Request, Response, NextFunction } from 'express';
import { plainToClass } from 'class-transformer';
import { RoutingService } from './routing.service';
import { RouteRequestDto, DemoRouteRequestDto, TableMatrixRequestDto, VRPAssignmentRequestDto } from './routing.model';
import { BaseResponse } from '../../common/types/restful';

export class RoutingController {
  /**
   * Calculate route between waypoints
   */
  public static async calculateRoute(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const data = plainToClass(RouteRequestDto, req.body);

      if (!data.waypoints || data.waypoints.length < 2) {
        res.status(400).json(BaseResponse.error('At least 2 waypoints are required'));
        return;
      }

      const result = await RoutingService.calculateRoute(data);
      res.json(BaseResponse.success(result));
    } catch (error) {
      next(error);
    }
  }

  /**
   * Calculate priority-based multi-stop route
   */
  public static async calculatePriorityRoute(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const data = plainToClass(RouteRequestDto, req.body);

      if (!data.waypoints || data.waypoints.length < 2) {
        res.status(400).json(BaseResponse.error('At least 2 waypoints are required'));
        return;
      }

      if (!data.priorities || data.priorities.length !== data.waypoints.length - 1) {
        res.status(400).json(BaseResponse.error('Priorities array length must be waypoints.length - 1'));
        return;
      }

      const result = await RoutingService.calculatePriorityRoutes(data);
      res.json(BaseResponse.success(result));
    } catch (error) {
      next(error);
    }
  }

  /**
   * Get route between two points (simple GET endpoint)
   */
  public static async getSimpleRoute(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { fromLat, fromLon, toLat, toLon } = req.query;

      if (!fromLat || !fromLon || !toLat || !toLon) {
        res.status(400).json(BaseResponse.error('fromLat, fromLon, toLat, toLon are required'));
        return;
      }

      const data: RouteRequestDto = {
        waypoints: [
          { lat: Number(fromLat), lon: Number(fromLon) },
          { lat: Number(toLat), lon: Number(toLon) },
        ],
        steps: true,
        annotations: true,
      };

      const result = await RoutingService.calculateRoute(data);
      res.json(BaseResponse.success(result));
    } catch (error) {
      next(error);
    }
  }

  /**
   * Get OSRM status (which instance is active, health checks)
   */
  public static async getOSRMStatus(_req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const status = await RoutingService.getOSRMStatus();
      res.json(BaseResponse.success(status));
    } catch (error) {
      next(error);
    }
  }

  /**
   * Switch OSRM instance (admin endpoint)
   */
  public static async switchOSRMInstance(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { instance } = req.body;

      if (instance !== 1 && instance !== 2) {
        res.status(400).json(BaseResponse.error('Instance must be 1 or 2'));
        return;
      }

      await RoutingService.switchOSRMInstance(instance);
      res.json(BaseResponse.success({ message: `Switched to instance ${instance}` }));
    } catch (error) {
      next(error);
    }
  }

  /**
   * Calculate demo route with priority-based ordering
   * POST /routing/demo-route
   */
  public static async calculateDemoRoute(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const data = plainToClass(DemoRouteRequestDto, req.body);

      if (!data.startPoint) {
        res.status(400).json(BaseResponse.error('Start point is required'));
        return;
      }

      if (!data.priorityGroups || data.priorityGroups.length === 0) {
        res.status(400).json(BaseResponse.error('At least one priority group is required'));
        return;
      }

      // Validate that each priority group has waypoints
      for (const group of data.priorityGroups) {
        if (!group.waypoints || group.waypoints.length === 0) {
          res.status(400).json(BaseResponse.error('Each priority group must have at least one waypoint'));
          return;
        }
      }

      const result = await RoutingService.calculateDemoRoute(data);
      res.json(BaseResponse.success(result));
    } catch (error) {
      next(error);
    }
  }

  /**
   * Get OSRM table matrix (distance/duration matrix) for VRP solving
   * POST /routing/table-matrix
   */
  public static async getTableMatrix(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const data = plainToClass(TableMatrixRequestDto, req.body);

      if (!data.coordinates || data.coordinates.length === 0) {
        res.status(400).json(BaseResponse.error('At least one coordinate is required'));
        return;
      }

      const result = await RoutingService.getTableMatrix(data);
      res.json(BaseResponse.success(result));
    } catch (error) {
      next(error);
    }
  }

  /**
   * Solve VRP assignment problem
   * POST /routing/vrp-assignment
   */
  public static async solveVRPAssignment(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const data = plainToClass(VRPAssignmentRequestDto, req.body);

      if (!data.shippers || data.shippers.length === 0) {
        res.status(400).json(BaseResponse.error('At least one shipper is required'));
        return;
      }

      if (!data.orders || data.orders.length === 0) {
        res.status(400).json(BaseResponse.error('At least one order is required'));
        return;
      }

      const result = await RoutingService.solveVRPAssignment(data);
      res.json(BaseResponse.success(result));
    } catch (error) {
      next(error);
    }
  }
}
