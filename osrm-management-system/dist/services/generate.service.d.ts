/**
 * Generate Service
 * Handles OSRM data generation from database
 */
import { PrismaClient } from '@prisma/client';
export interface GenerateResult {
    success: boolean;
    models?: string[];
    error?: string;
    duration?: number;
}
export declare class GenerateService {
    private prisma;
    private osrmDataPath;
    private buildTracker;
    constructor(prisma: PrismaClient);
    /**
     * Generate all OSRM models (sync-like: sequential processing)
     */
    generateAllModels(): Promise<GenerateResult>;
    private _generateAllModels;
    private startModelBuild;
    private fetchRoadNetworkData;
    private calculateUserRating;
    private calculateBlockingStatus;
    private exportToOSMXML;
    private generateLuaProfile;
    private generateBicycleLuaProfile;
    private generateCarLuaProfile;
    private setupModelInstance;
    private processOSRMInstance;
    private mapRoadTypeToOSM;
    private escapeXML;
}
//# sourceMappingURL=generate.service.d.ts.map