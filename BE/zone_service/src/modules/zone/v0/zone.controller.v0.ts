/**
 * Zone Controller V0
 * Simple paging and sorting without dynamic filters
 */

import { Request, Response, NextFunction } from 'express';
import { ZoneService } from '../zone.service';
import { PagingRequestV0 } from '../../../common/types/filter-v2';
import { logger } from '../../../common/logger/logger.service';

export class ZoneControllerV0 {
  /**
   * Get zones with simple paging (V0)
   */
  public static async getZones(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      logger.info('POST /api/v0/zones - Get zones with simple paging (V0)');
      
      const request: PagingRequestV0 = req.body;
      const result = await ZoneService.getZonesV0(request);
      
      res.json({ result });
    } catch (error) {
      next(error);
    }
  }
}
