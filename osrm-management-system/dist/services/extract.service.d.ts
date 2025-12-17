/**
 * Extract Service
 * Handles OSM data extraction
 * Tracks builds in database for sequential processing
 */
import { PrismaClient } from '@prisma/client';
export interface ExtractResult {
    success: boolean;
    outputPath?: string;
    error?: string;
    duration?: number;
    buildId?: string;
}
export declare class ExtractService {
    private rawDataPath;
    private buildTracker;
    private prisma;
    constructor(prisma: PrismaClient);
    /**
     * Extract complete data (sync-like: sequential processing)
     */
    extractCompleteData(polyFile?: string): Promise<ExtractResult>;
    private _extractCompleteData;
}
//# sourceMappingURL=extract.service.d.ts.map