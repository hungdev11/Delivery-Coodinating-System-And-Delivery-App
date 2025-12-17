/**
 * Health Controller
 * Health check endpoints
 */

import { Request, Response } from 'express';
import { BaseResponseBuilder } from '../common/types/restful';

export class HealthController {
  /**
   * GET /api/v1/health
   * Health check endpoint
   */
  async health(req: Request, res: Response): Promise<void> {
    res.json(BaseResponseBuilder.success({
      status: 'healthy',
      timestamp: new Date().toISOString(),
      service: 'osrm-management-system',
    }));
  }
}
