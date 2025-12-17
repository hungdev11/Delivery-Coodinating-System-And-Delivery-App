/**
 * Generate Controller
 * RESTful API endpoints for OSRM data generation
 */
import { Request, Response, NextFunction } from 'express';
import { PrismaClient } from '@prisma/client';
export declare class GenerateController {
    private generateService;
    private prisma;
    constructor(prisma: PrismaClient);
    /**
     * POST /api/v1/generate/osrm-v2
     * Generate all OSRM V2 models from database
     */
    generateOSRMV2(req: Request, res: Response, next: NextFunction): Promise<void>;
}
//# sourceMappingURL=generate.controller.d.ts.map