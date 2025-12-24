"use strict";
/**
 * Build Tracker Service
 * Tracks OSRM builds in database (osrm_builds table)
 * Ensures sequential processing (sync-like behavior)
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.BuildTrackerService = void 0;
const logger_1 = require("../common/logger");
// Build status enum values (matching Prisma schema)
const OsrmBuildStatus = {
    PENDING: 'PENDING',
    BUILDING: 'BUILDING',
    TESTING: 'TESTING',
    READY: 'READY',
    DEPLOYED: 'DEPLOYED',
    FAILED: 'FAILED',
    DEPRECATED: 'DEPRECATED',
};
class BuildTrackerService {
    prisma;
    activeBuilds = new Map();
    constructor(prisma) {
        this.prisma = prisma;
    }
    /**
     * Start tracking a build (creates PENDING record)
     * Returns build_id
     */
    async startBuild(instanceName, totalSegments, pbfFilePath) {
        const build = await this.prisma.osrm_builds.create({
            data: {
                instance_name: instanceName,
                status: OsrmBuildStatus.PENDING,
                data_snapshot_time: new Date(),
                total_segments: totalSegments,
                pbf_file_path: pbfFilePath,
                lua_script_version: '2.0',
            },
        });
        logger_1.logger.info(`Started tracking build ${build.build_id} for ${instanceName}`);
        return build.build_id;
    }
    /**
     * Update build status to BUILDING
     */
    async markBuilding(buildId) {
        await this.prisma.osrm_builds.update({
            where: { build_id: buildId },
            data: {
                status: OsrmBuildStatus.BUILDING,
                started_at: new Date(),
            },
        });
        logger_1.logger.info(`Build ${buildId} marked as BUILDING`);
    }
    /**
     * Update build status to READY (completed successfully)
     */
    async markReady(buildId, osrmOutputPath, avgWeight) {
        await this.prisma.osrm_builds.update({
            where: { build_id: buildId },
            data: {
                status: OsrmBuildStatus.READY,
                completed_at: new Date(),
                osrm_output_path: osrmOutputPath,
                avg_weight: avgWeight,
            },
        });
        logger_1.logger.info(`Build ${buildId} marked as READY`);
    }
    /**
     * Update build status to FAILED
     */
    async markFailed(buildId, errorMessage) {
        // Truncate error message to prevent database field overflow
        // Reduce to 150 chars to be safe (including '...')
        const MAX_ERROR_LENGTH = 150;
        const truncatedError = errorMessage.length > MAX_ERROR_LENGTH
            ? errorMessage.substring(0, MAX_ERROR_LENGTH - 3) + '...'
            : errorMessage;
        try {
            await this.prisma.osrm_builds.update({
                where: { build_id: buildId },
                data: {
                    status: OsrmBuildStatus.FAILED,
                    completed_at: new Date(),
                    error_message: truncatedError,
                },
            });
            logger_1.logger.error(`Build ${buildId} marked as FAILED: ${errorMessage}`);
        }
        catch (error) {
            // If update fails (e.g., database connection issue), log the error but don't throw
            // This prevents cascading failures
            logger_1.logger.error(`Failed to update build status in database: ${error.message}`, {
                buildId,
                originalError: errorMessage.substring(0, 500), // Log full error message
            });
        }
    }
    /**
     * Mark build as DEPLOYED
     */
    async markDeployed(buildId) {
        await this.prisma.osrm_builds.update({
            where: { build_id: buildId },
            data: {
                status: OsrmBuildStatus.DEPLOYED,
                deployed_at: new Date(),
            },
        });
        logger_1.logger.info(`Build ${buildId} marked as DEPLOYED`);
    }
    /**
     * Get current build status for an instance
     */
    async getCurrentBuild(instanceName) {
        const build = await this.prisma.osrm_builds.findFirst({
            where: {
                instance_name: instanceName,
                status: {
                    in: [OsrmBuildStatus.PENDING, OsrmBuildStatus.BUILDING, OsrmBuildStatus.TESTING],
                },
            },
            orderBy: { created_at: 'desc' },
        });
        if (!build)
            return null;
        return {
            buildId: build.build_id,
            instanceName: build.instance_name,
            status: build.status,
            totalSegments: build.total_segments,
            avgWeight: build.avg_weight || undefined,
            pbfFilePath: build.pbf_file_path || undefined,
            osrmOutputPath: build.osrm_output_path || undefined,
            errorMessage: build.error_message || undefined,
        };
    }
    /**
     * Get latest READY build for an instance
     */
    async getLatestReadyBuild(instanceName) {
        const build = await this.prisma.osrm_builds.findFirst({
            where: {
                instance_name: instanceName,
                status: OsrmBuildStatus.READY,
            },
            orderBy: { completed_at: 'desc' },
        });
        if (!build)
            return null;
        return {
            buildId: build.build_id,
            instanceName: build.instance_name,
            status: build.status,
            totalSegments: build.total_segments,
            avgWeight: build.avg_weight || undefined,
            pbfFilePath: build.pbf_file_path || undefined,
            osrmOutputPath: build.osrm_output_path || undefined,
        };
    }
    /**
     * Get latest DEPLOYED build for an instance
     */
    async getLatestDeployedBuild(instanceName) {
        const build = await this.prisma.osrm_builds.findFirst({
            where: {
                instance_name: instanceName,
                status: OsrmBuildStatus.DEPLOYED,
            },
            orderBy: { deployed_at: 'desc' },
        });
        if (!build)
            return null;
        return {
            buildId: build.build_id,
            instanceName: build.instance_name,
            status: build.status,
            totalSegments: build.total_segments,
            avgWeight: build.avg_weight || undefined,
            pbfFilePath: build.pbf_file_path || undefined,
            osrmOutputPath: build.osrm_output_path || undefined,
        };
    }
    /**
     * Get build history for an instance
     */
    async getBuildHistory(instanceName, limit = 10) {
        const builds = await this.prisma.osrm_builds.findMany({
            where: { instance_name: instanceName },
            orderBy: { created_at: 'desc' },
            take: limit,
        });
        return builds.map((build) => ({
            buildId: build.build_id,
            instanceName: build.instance_name,
            status: build.status,
            totalSegments: build.total_segments,
            avgWeight: build.avg_weight || undefined,
            pbfFilePath: build.pbf_file_path || undefined,
            osrmOutputPath: build.osrm_output_path || undefined,
            errorMessage: build.error_message || undefined,
        }));
    }
    /**
     * Execute build operation sequentially (sync-like)
     * Ensures only one build per instance runs at a time
     */
    async executeSequentially(instanceName, operation) {
        // Check if there's already an active build for this instance
        const existingPromise = this.activeBuilds.get(instanceName);
        if (existingPromise) {
            logger_1.logger.info(`Waiting for existing build to complete for ${instanceName}`);
            await existingPromise;
        }
        // Create new promise for this build
        const buildPromise = (async () => {
            try {
                // Check for current build or create new one
                let currentBuild = await this.getCurrentBuild(instanceName);
                if (!currentBuild) {
                    // Create new build record
                    const buildId = await this.startBuild(instanceName, 0);
                    currentBuild = await this.getCurrentBuild(instanceName);
                    if (!currentBuild) {
                        throw new Error('Failed to create build record');
                    }
                }
                const buildId = currentBuild.buildId;
                await this.markBuilding(buildId);
                // Execute the actual operation
                const result = await operation(buildId);
                return result;
            }
            finally {
                // Remove from active builds
                this.activeBuilds.delete(instanceName);
            }
        })();
        // Store promise
        this.activeBuilds.set(instanceName, buildPromise);
        return buildPromise;
    }
}
exports.BuildTrackerService = BuildTrackerService;
//# sourceMappingURL=build-tracker.service.js.map