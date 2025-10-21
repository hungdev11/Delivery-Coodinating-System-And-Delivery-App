/**
 * Zone Controller with advanced filtering and sorting
 */

import { Request, Response } from 'express';
import { ZoneService } from '../../common/services/zone-service';
import { PagingRequest, BaseResponse } from '../../common/types/filter';

export class ZoneController {
  constructor(private zoneService: ZoneService) {}

  /**
   * Get zones with advanced filtering and sorting
   * POST /api/v1/zones
   */
  async getZones(req: Request, res: Response) {
    try {
      const request: PagingRequest = req.body;
      
      console.log('Received getZones request:', request);
      
      const result = await this.zoneService.getZones(request);
      
      const response: BaseResponse<typeof result> = {
        result
      };
      
      res.json(response);
      
    } catch (error) {
      console.error('Error in getZones:', error);
      
      const response: BaseResponse<null> = {
        message: 'Failed to get zones'
      };
      
      res.status(500).json(response);
    }
  }

  /**
   * Get zone by ID
   * GET /api/v1/zones/:id
   */
  async getZoneById(req: Request, res: Response): Promise<void> {
    try {
      const { id } = req.params;
      
      if (!id) {
        const response: BaseResponse<null> = {
          message: 'Zone ID is required'
        };
        res.status(400).json(response);
        return;
      }
      
      console.log('Received getZoneById request:', id);
      
      const zone = await this.zoneService.getZoneById(id);
      
      if (!zone) {
        const response: BaseResponse<null> = {
          message: 'Zone not found'
        };
        res.status(404).json(response);
        return;
      }
      
      const response: BaseResponse<typeof zone> = {
        result: zone
      };
      
      res.json(response);
      
    } catch (error) {
      console.error('Error in getZoneById:', error);
      
      const response: BaseResponse<null> = {
        message: 'Failed to get zone'
      };
      
      res.status(500).json(response);
    }
  }

  /**
   * Create zone
   * POST /api/v1/zones/create
   */
  async createZone(req: Request, res: Response) {
    try {
      const zoneData = req.body;
      
      console.log('Received createZone request');
      
      const zone = await this.zoneService.createZone(zoneData);
      
      const response: BaseResponse<typeof zone> = {
        result: zone,
        message: 'Zone created successfully'
      };
      
      res.status(201).json(response);
      
    } catch (error) {
      console.error('Error in createZone:', error);
      
      const response: BaseResponse<null> = {
        message: 'Failed to create zone'
      };
      
      res.status(500).json(response);
    }
  }

  /**
   * Update zone
   * PUT /api/v1/zones/:id
   */
  async updateZone(req: Request, res: Response): Promise<void> {
    try {
      const { id } = req.params;
      const zoneData = req.body;
      
      if (!id) {
        const response: BaseResponse<null> = {
          message: 'Zone ID is required'
        };
        res.status(400).json(response);
        return;
      }
      
      console.log('Received updateZone request:', id);
      
      const zone = await this.zoneService.updateZone(id, zoneData);
      
      const response: BaseResponse<typeof zone> = {
        result: zone,
        message: 'Zone updated successfully'
      };
      
      res.json(response);
      
    } catch (error) {
      console.error('Error in updateZone:', error);
      
      const response: BaseResponse<null> = {
        message: 'Failed to update zone'
      };
      
      res.status(500).json(response);
    }
  }

  /**
   * Delete zone
   * DELETE /api/v1/zones/:id
   */
  async deleteZone(req: Request, res: Response): Promise<void> {
    try {
      const { id } = req.params;
      
      if (!id) {
        const response: BaseResponse<null> = {
          message: 'Zone ID is required'
        };
        res.status(400).json(response);
        return;
      }
      
      console.log('Received deleteZone request:', id);
      
      await this.zoneService.deleteZone(id);
      
      const response: BaseResponse<null> = {
        message: 'Zone deleted successfully'
      };
      
      res.json(response);
      
    } catch (error) {
      console.error('Error in deleteZone:', error);
      
      const response: BaseResponse<null> = {
        message: 'Failed to delete zone'
      };
      
      res.status(500).json(response);
    }
  }

  /**
   * Get filterable fields
   * GET /api/v1/zones/filterable-fields
   */
  async getFilterableFields(_req: Request, res: Response): Promise<void> {
    try {
      const fields = this.zoneService.getFilterableFields();
      
      const response: BaseResponse<string[]> = {
        result: fields
      };
      
      res.json(response);
      
    } catch (error) {
      console.error('Error in getFilterableFields:', error);
      
      const response: BaseResponse<null> = {
        message: 'Failed to get filterable fields'
      };
      
      res.status(500).json(response);
    }
  }

  /**
   * Get sortable fields
   * GET /api/v1/zones/sortable-fields
   */
  async getSortableFields(_req: Request, res: Response): Promise<void> {
    try {
      const fields = this.zoneService.getSortableFields();
      
      const response: BaseResponse<string[]> = {
        result: fields
      };
      
      res.json(response);
      
    } catch (error) {
      console.error('Error in getSortableFields:', error);
      
      const response: BaseResponse<null> = {
        message: 'Failed to get sortable fields'
      };
      
      res.status(500).json(response);
    }
  }
}
