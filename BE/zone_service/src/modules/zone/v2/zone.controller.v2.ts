/**
 * Zone Controller V2
 * Enhanced filtering with operations between each pair
 */

import { Request, Response, NextFunction } from 'express';
import { ZoneService } from '../zone.service';
import { PagingRequestV2 } from '../../../common/types/filter-v2';
import { logger } from '../../../common/logger/logger.service';

export class ZoneControllerV2 {
  /**
   * Get zones with enhanced filtering (V2)
   */
  public static async getZones(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      logger.info('POST /api/v2/zones - Get zones with enhanced filtering (V2)');
      
      const request: PagingRequestV2 = req.body;
      const result = await ZoneService.getZonesV2(request);
      
      res.json({ result });
    } catch (error) {
      next(error);
    }
  }
}
