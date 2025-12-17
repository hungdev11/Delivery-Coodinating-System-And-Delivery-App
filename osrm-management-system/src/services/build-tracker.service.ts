/**
 * Build Tracker Service
 * Tracks OSRM builds in database (osrm_builds table)
 * Ensures sequential processing (sync-like behavior)
 */

import { PrismaClient } from '@prisma/client';
import { logger } from '../common/logger';

// Build status enum values (matching Prisma schema)
const OsrmBuildStatus = {
  PENDING: 'PENDING',
  BUILDING: 'BUILDING',
  TESTING: 'TESTING',
  READY: 'READY',
  DEPLOYED: 'DEPLOYED',
  FAILED: 'FAILED',
  DEPRECATED: 'DEPRECATED',
} as const;

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

export class BuildTrackerService {
  private prisma: PrismaClient;
  private activeBuilds: Map<string, Promise<any>> = new Map();

  constructor(prisma: PrismaClient) {
    this.prisma = prisma;
  }

  /**
   * Start tracking a build (creates PENDING record)
   * Returns build_id
   */
  async startBuild(instanceName: string, totalSegments: number, pbfFilePath?: string): Promise<string> {
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

    logger.info(`Started tracking build ${build.build_id} for ${instanceName}`);
    return build.build_id;
  }

  /**
   * Update build status to BUILDING
   */
  async markBuilding(buildId: string): Promise<void> {
    await this.prisma.osrm_builds.update({
      where: { build_id: buildId },
      data: {
        status: OsrmBuildStatus.BUILDING,
        started_at: new Date(),
      },
    });
    logger.info(`Build ${buildId} marked as BUILDING`);
  }

  /**
   * Update build status to READY (completed successfully)
   */
  async markReady(buildId: string, osrmOutputPath: string, avgWeight?: number): Promise<void> {
    await this.prisma.osrm_builds.update({
      where: { build_id: buildId },
      data: {
        status: OsrmBuildStatus.READY,
        completed_at: new Date(),
        osrm_output_path: osrmOutputPath,
        avg_weight: avgWeight,
      },
    });
    logger.info(`Build ${buildId} marked as READY`);
  }

  /**
   * Update build status to FAILED
   */
  async markFailed(buildId: string, errorMessage: string): Promise<void> {
    // Truncate error message to prevent database field overflow (limit to 500 chars for safety)
    // Most databases have VARCHAR(255) or TEXT limits, so we use 500 to be safe
    const truncatedError = errorMessage.length > 500 
      ? errorMessage.substring(0, 497) + '...' 
      : errorMessage;
    
    await this.prisma.osrm_builds.update({
      where: { build_id: buildId },
      data: {
        status: OsrmBuildStatus.FAILED,
        completed_at: new Date(),
        error_message: truncatedError,
      },
    });
    logger.error(`Build ${buildId} marked as FAILED: ${errorMessage}`);
  }

  /**
   * Mark build as DEPLOYED
   */
  async markDeployed(buildId: string): Promise<void> {
    await this.prisma.osrm_builds.update({
      where: { build_id: buildId },
      data: {
        status: OsrmBuildStatus.DEPLOYED,
        deployed_at: new Date(),
      },
    });
    logger.info(`Build ${buildId} marked as DEPLOYED`);
  }

  /**
   * Get current build status for an instance
   */
  async getCurrentBuild(instanceName: string): Promise<BuildInfo | null> {
    const build = await this.prisma.osrm_builds.findFirst({
      where: {
        instance_name: instanceName,
        status: {
          in: [OsrmBuildStatus.PENDING, OsrmBuildStatus.BUILDING, OsrmBuildStatus.TESTING],
        },
      },
      orderBy: { created_at: 'desc' },
    });

    if (!build) return null;

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
  async getLatestReadyBuild(instanceName: string): Promise<BuildInfo | null> {
    const build = await this.prisma.osrm_builds.findFirst({
      where: {
        instance_name: instanceName,
        status: OsrmBuildStatus.READY,
      },
      orderBy: { completed_at: 'desc' },
    });

    if (!build) return null;

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
  async getLatestDeployedBuild(instanceName: string): Promise<BuildInfo | null> {
    const build = await this.prisma.osrm_builds.findFirst({
      where: {
        instance_name: instanceName,
        status: OsrmBuildStatus.DEPLOYED,
      },
      orderBy: { deployed_at: 'desc' },
    });

    if (!build) return null;

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
  async getBuildHistory(instanceName: string, limit: number = 10): Promise<BuildInfo[]> {
    const builds = await this.prisma.osrm_builds.findMany({
      where: { instance_name: instanceName },
      orderBy: { created_at: 'desc' },
      take: limit,
    });

    return builds.map((build: any) => ({
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
  async executeSequentially<T>(
    instanceName: string,
    operation: (buildId: string) => Promise<T>
  ): Promise<T> {
    // Check if there's already an active build for this instance
    const existingPromise = this.activeBuilds.get(instanceName);
    if (existingPromise) {
      logger.info(`Waiting for existing build to complete for ${instanceName}`);
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
      } finally {
        // Remove from active builds
        this.activeBuilds.delete(instanceName);
      }
    })();

    // Store promise
    this.activeBuilds.set(instanceName, buildPromise);

    return buildPromise;
  }
}
