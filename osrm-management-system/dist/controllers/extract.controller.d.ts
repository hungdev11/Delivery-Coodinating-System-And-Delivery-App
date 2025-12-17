/**
 * Extract Controller
 * RESTful API endpoints for OSM data extraction
 */
import { Request, Response, NextFunction } from 'express';
import { PrismaClient } from '@prisma/client';
export declare class ExtractController {
    private extractService;
    constructor(prisma: PrismaClient);
    /**
     * POST /api/v1/extract/complete
     * Extract complete OSM data (routing + addresses)
     */
    extractComplete(req: Request, res: Response, next: NextFunction): Promise<void>;
}
//# sourceMappingURL=extract.controller.d.ts.map