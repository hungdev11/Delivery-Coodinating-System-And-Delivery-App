/**
 * Zone Controller
 * Handles HTTP requests for zone management
 */

import { Request, Response, NextFunction } from 'express';
import { plainToClass } from 'class-transformer';
import { ZoneService } from './zone.service';
import { CreateZoneDto, UpdateZoneDto, ZonePagingRequest } from './zone.model';
import { BaseResponse } from '../../common/types/restful';

export class ZoneController {
  /**
   * Get all zones with pagination
   */
  public static async getAllZones(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const request = plainToClass(ZonePagingRequest, req.query);
      const result = await ZoneService.getAllZones(request);
      res.json(BaseResponse.success(result));
    } catch (error) {
      next(error);
    }
  }

  /**
   * Get zone by ID
   */
  public static async getZoneById(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { id } = req.params;
      if (!id) {
        res.status(400).json(BaseResponse.error('ID is required'));
        return;
      }
      const result = await ZoneService.getZoneById(id);
      
      if (!result) {
        res.status(404).json(BaseResponse.error('Zone not found'));
        return;
      }

      res.json(BaseResponse.success(result));
    } catch (error) {
      next(error);
    }
  }

  /**
   * Get zone by code
   */
  public static async getZoneByCode(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { code } = req.params;
      if (!code) {
        res.status(400).json(BaseResponse.error('Code is required'));
        return;
      }
      const result = await ZoneService.getZoneByCode(code);
      
      if (!result) {
        res.status(404).json(BaseResponse.error('Zone not found'));
        return;
      }

      res.json(BaseResponse.success(result));
    } catch (error) {
      next(error);
    }
  }

  /**
   * Get zones by center ID
   */
  public static async getZonesByCenterId(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { centerId } = req.params;
      if (!centerId) {
        res.status(400).json(BaseResponse.error('Center ID is required'));
        return;
      }
      const result = await ZoneService.getZonesByCenterId(centerId);
      res.json(BaseResponse.success(result));
    } catch (error) {
      next(error);
    }
  }

  /**
   * Create a new zone
   */
  public static async createZone(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const data = plainToClass(CreateZoneDto, req.body);
      const result = await ZoneService.createZone(data);
      res.status(201).json(BaseResponse.success(result, 'Zone created successfully'));
    } catch (error) {
      next(error);
    }
  }

  /**
   * Update a zone
   */
  public static async updateZone(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { id } = req.params;
      if (!id) {
        res.status(400).json(BaseResponse.error('ID is required'));
        return;
      }
      const data = plainToClass(UpdateZoneDto, req.body);
      const result = await ZoneService.updateZone(id, data);
      res.json(BaseResponse.success(result, 'Zone updated successfully'));
    } catch (error) {
      next(error);
    }
  }

  /**
   * Delete a zone
   */
  public static async deleteZone(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { id } = req.params;
      if (!id) {
        res.status(400).json(BaseResponse.error('ID is required'));
        return;
      }
      await ZoneService.deleteZone(id);
      res.json(BaseResponse.success(null, 'Zone deleted successfully'));
    } catch (error) {
      next(error);
    }
  }
}
