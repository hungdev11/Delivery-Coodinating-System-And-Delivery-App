/**
 * Center Controller
 * Handles HTTP requests for center management
 */

import { Request, Response, NextFunction } from 'express';
import { plainToClass } from 'class-transformer';
import { CenterService } from './center.service';
import { CreateCenterDto, UpdateCenterDto, CenterPagingRequest } from './center.model';
import { BaseResponse } from '../../common/types/restful';

export class CenterController {
  /**
   * Get all centers with pagination
   */
  public static async getAllCenters(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const request = plainToClass(CenterPagingRequest, req.query);
      const result = await CenterService.getAllCenters(request);
      res.json(BaseResponse.success(result));
    } catch (error) {
      next(error);
    }
  }

  /**
   * Get center by ID
   */
  public static async getCenterById(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { id } = req.params;
      if (!id) {
        res.status(400).json(BaseResponse.error('ID is required'));
        return;
      }
      const result = await CenterService.getCenterById(id);
      
      if (!result) {
        res.status(404).json(BaseResponse.error('Center not found'));
        return;
      }

      res.json(BaseResponse.success(result));
    } catch (error) {
      next(error);
    }
  }

  /**
   * Get center by code
   */
  public static async getCenterByCode(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { code } = req.params;
      if (!code) {
        res.status(400).json(BaseResponse.error('Code is required'));
        return;
      }
      const result = await CenterService.getCenterByCode(code);
      
      if (!result) {
        res.status(404).json(BaseResponse.error('Center not found'));
        return;
      }

      res.json(BaseResponse.success(result));
    } catch (error) {
      next(error);
    }
  }

  /**
   * Create a new center
   */
  public static async createCenter(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const data = plainToClass(CreateCenterDto, req.body);
      const result = await CenterService.createCenter(data);
      res.status(201).json(BaseResponse.success(result, 'Center created successfully'));
    } catch (error) {
      next(error);
    }
  }

  /**
   * Update a center
   */
  public static async updateCenter(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { id } = req.params;
      if (!id) {
        res.status(400).json(BaseResponse.error('ID is required'));
        return;
      }
      const data = plainToClass(UpdateCenterDto, req.body);
      const result = await CenterService.updateCenter(id, data);
      res.json(BaseResponse.success(result, 'Center updated successfully'));
    } catch (error) {
      next(error);
    }
  }

  /**
   * Delete a center
   */
  public static async deleteCenter(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { id } = req.params;
      if (!id) {
        res.status(400).json(BaseResponse.error('ID is required'));
        return;
      }
      await CenterService.deleteCenter(id);
      res.json(BaseResponse.success(null, 'Center deleted successfully'));
    } catch (error) {
      next(error);
    }
  }
}
