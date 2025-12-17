/**
 * Build Tracker Service
 * Tracks OSRM builds in database (osrm_builds table)
 * Ensures sequential processing (sync-like behavior)
 */
import { PrismaClient } from '@prisma/client';
declare const OsrmBuildStatus: {
    readonly PENDING: "PENDING";
    readonly BUILDING: "BUILDING";
    readonly TESTING: "TESTING";
    readonly READY: "READY";
    readonly DEPLOYED: "DEPLOYED";
    readonly FAILED: "FAILED";
    readonly DEPRECATED: "DEPRECATED";
};
type OsrmBuildStatusType = typeof OsrmBuildStatus[keyof typeof OsrmBuildStatus];
export interface BuildInfo {
    buildId: string;
    instanceName: string;
    status: OsrmBuildStatusType | string;
    totalSegments?: number;
    avgWeight?: number;
    pbfFilePath?: string;
    osrmOutputPath?: string;
    errorMessage?: string;
}
export declare class BuildTrackerService {
    private prisma;
    private activeBuilds;
    constructor(prisma: PrismaClient);
    /**
     * Start tracking a build (creates PENDING record)
     * Returns build_id
     */
    startBuild(instanceName: string, totalSegments: number, pbfFilePath?: string): Promise<string>;
    /**
     * Update build status to BUILDING
     */
    markBuilding(buildId: string): Promise<void>;
    /**
     * Update build status to READY (completed successfully)
     */
    markReady(buildId: string, osrmOutputPath: string, avgWeight?: number): Promise<void>;
    /**
     * Update build status to FAILED
     */
    markFailed(buildId: string, errorMessage: string): Promise<void>;
    /**
     * Mark build as DEPLOYED
     */
    markDeployed(buildId: string): Promise<void>;
    /**
     * Get current build status for an instance
     */
    getCurrentBuild(instanceName: string): Promise<BuildInfo | null>;
    /**
     * Get latest READY build for an instance
     */
    getLatestReadyBuild(instanceName: string): Promise<BuildInfo | null>;
    /**
     * Get latest DEPLOYED build for an instance
     */
    getLatestDeployedBuild(instanceName: string): Promise<BuildInfo | null>;
    /**
     * Get build history for an instance
     */
    getBuildHistory(instanceName: string, limit?: number): Promise<BuildInfo[]>;
    /**
     * Execute build operation sequentially (sync-like)
     * Ensures only one build per instance runs at a time
     */
    executeSequentially<T>(instanceName: string, operation: (buildId: string) => Promise<T>): Promise<T>;
}
export {};
//# sourceMappingURL=build-tracker.service.d.ts.map