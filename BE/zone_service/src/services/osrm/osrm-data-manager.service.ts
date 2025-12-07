/**
 * OSRM Data Management Service
 * 
 * Handles:
 * - Building OSRM data from OSM files
 * - Managing dual OSRM instances (rolling restart)
 * - Health monitoring of OSRM instances
 * - Data validation and integrity checks
 */

import { exec } from 'child_process';
import { promisify } from 'util';
import { existsSync, mkdirSync, readdirSync, statSync } from 'fs';
import { join } from 'path';
import { logger } from '../../common/logger';
import { PrismaClient, OsrmBuildStatus } from '@prisma/client';

const execAsync = promisify(exec);

export interface OSRMInstance {
  id: number;
  name: string;
  dataPath: string;
  port: number;
  status: 'stopped' | 'starting' | 'running' | 'stopping' | 'error';
  lastHealthCheck?: Date;
  pid?: number;
}

export interface OSRMBuildResult {
  success: boolean;
  instance: number;
  buildTime: number;
  dataSize: string;
  error?: string;
}

export interface OSRMRestartResult {
  success: boolean;
  stoppedInstance: number;
  startedInstance: number;
  downtime: number;
  error?: string;
}

export class OSRMDataManagerService {
  private prisma: PrismaClient;
  private instances: OSRMInstance[] = [
    {
      id: 1,
      name: 'osrm-instance-1',
      dataPath: '/app/osrm_data/osrm-instance-1',
      port: 5000,
      status: 'stopped'
    },
    {
      id: 2,
      name: 'osrm-instance-2', 
      dataPath: '/app/osrm_data/osrm-instance-2',
      port: 5001,
      status: 'stopped'
    }
  ];

  private activeInstance: number = 1;
  private readonly osmFilePath: string;
  private readonly osrmDataPath: string;

  constructor(prisma: PrismaClient) {
    this.prisma = prisma;
    
    // Load configuration from environment variables
    this.osrmDataPath = process.env.OSRM_DATA_PATH || '/app/osrm_data';
    this.osmFilePath = this.findLatestOsmFile();
    
    // Update instance data paths to use configured path
    this.instances.forEach(instance => {
      instance.dataPath = `${this.osrmDataPath}/${instance.name}`;
    });
    
    this.loadActiveInstance();
  }

  /**
   * Find the latest OSM file in the data directory
   */
  private findLatestOsmFile(): string {
    const rawDataPath = process.env.OSM_RAW_DATA_PATH || '/app/raw_data/vietnam';
    const fallbackPath = '/app/raw_data/vietnam/vietnam-251013.osm.pbf';
    
    try {
      if (!existsSync(rawDataPath)) {
        logger.warn(`OSM data directory not found: ${rawDataPath}, using fallback: ${fallbackPath}`);
        return fallbackPath;
      }

      const files = readdirSync(rawDataPath)
        .filter(file => file.endsWith('.osm.pbf') && file.startsWith('vietnam-'))
        .map(file => ({
          name: file,
          path: join(rawDataPath, file),
          mtime: statSync(join(rawDataPath, file)).mtime
        }))
        .sort((a, b) => b.mtime.getTime() - a.mtime.getTime());

      if (files.length === 0) {
        logger.warn(`No OSM files found in ${rawDataPath}, using fallback: ${fallbackPath}`);
        return fallbackPath;
      }

      const latestFile = files[0];
      if (!latestFile) {
        logger.warn(`No OSM files found in ${rawDataPath}, using fallback: ${fallbackPath}`);
        return fallbackPath;
      }
      
      logger.info(`Using latest OSM file: ${latestFile.name} (modified: ${latestFile.mtime.toISOString()})`);
      return latestFile.path;

    } catch (error) {
      logger.error(`Error finding latest OSM file:`, error);
      logger.warn(`Using fallback: ${fallbackPath}`);
      return fallbackPath;
    }
  }

  /**
   * Load active instance from database
   */
  private async loadActiveInstance(): Promise<void> {
    try {
      const deployedBuilds = await this.prisma.osrm_builds.findMany({
        where: { status: OsrmBuildStatus.DEPLOYED },
        orderBy: { deployed_at: 'desc' },
        take: 1,
      });

      if (deployedBuilds.length > 0) {
        const instanceName = deployedBuilds[0]?.instance_name;
        if (instanceName) {
          this.activeInstance = instanceName === 'osrm-instance-1' ? 1 : 2;
          logger.info(`Loaded active instance from database: ${this.activeInstance}`);
        }
      }
    } catch (error) {
      logger.error('Failed to load active instance from database:', error);
    }
  }

  /**
   * Build OSRM data for a specific instance
   */
  async buildOSRMData(instanceId: number): Promise<OSRMBuildResult> {
    const startTime = Date.now();
    const instance = this.instances.find(i => i.id === instanceId);
    
    if (!instance) {
      throw new Error(`OSRM instance ${instanceId} not found`);
    }

    // Create build record in database
    const buildRecord = await this.prisma.osrm_builds.create({
      data: {
        instance_name: instance.name,
        status: OsrmBuildStatus.BUILDING,
        data_snapshot_time: new Date(),
        total_segments: 0, // Will be updated after build
        started_at: new Date(),
        pbf_file_path: this.osmFilePath,
        osrm_output_path: instance.dataPath,
        lua_script_version: '1.0.0'
      }
    });

    try {
      logger.info(`Starting OSRM data build for instance ${instanceId} (Build ID: ${buildRecord?.build_id || 'unknown'})`);

      // Ensure data directory exists
      if (!existsSync(instance.dataPath)) {
        mkdirSync(instance.dataPath, { recursive: true });
      }

      // Step 1: Extract with osrm-extract using Docker
      logger.info(`Extracting OSM data for ${instance.name}...`);
      // Use Docker to run osrm-extract (OSRM tools not installed in container)
      // Mount the entire /app directory so OSRM can access both input and output files
      // Convert /app paths to /data paths for Docker mount
      const osmFileInDocker = this.osmFilePath.replace('/app', '/data');
      const profileFileInDocker = `/data/osrm_data/osrm-instance-${instanceId}/custom_car.lua`;
      const extractCmd = `docker run --rm -v /app:/data osrm/osrm-backend:latest osrm-extract -p ${profileFileInDocker} ${osmFileInDocker}`;
      await execAsync(extractCmd, { maxBuffer: 100 * 1024 * 1024 });

      // Step 2: Contract with osrm-contract using Docker
      logger.info(`Contracting OSRM data for ${instance.name}...`);
      // Use Docker to run osrm-contract
      const contractCmd = `docker run --rm -v /app:/data osrm/osrm-backend:latest osrm-contract /data/osrm_data/osrm-instance-${instanceId}/network.osrm`;
      await execAsync(contractCmd, { maxBuffer: 100 * 1024 * 1024 });

      // Get data size and segment count
      const dataSize = await this.getDataSize(instance.dataPath);
      const segmentCount = await this.getSegmentCount();
      const buildTime = Date.now() - startTime;

      // Update build record as successful
      if (buildRecord?.build_id) {
        await this.prisma.osrm_builds.update({
          where: { build_id: buildRecord.build_id },
          data: {
            status: OsrmBuildStatus.READY,
            completed_at: new Date(),
            total_segments: segmentCount,
            avg_weight: 1.0 // Default weight, can be calculated from traffic data
          }
        });
      }

      logger.info(`OSRM data build completed for ${instance.name} in ${buildTime}ms`);

      return {
        success: true,
        instance: instanceId,
        buildTime,
        dataSize
      };

    } catch (error) {
      const buildTime = Date.now() - startTime;
      logger.error(`OSRM data build failed for ${instance.name}:`, error);
      
      // Update build record as failed
      if (buildRecord?.build_id) {
        await this.prisma.osrm_builds.update({
          where: { build_id: buildRecord.build_id },
          data: {
            status: OsrmBuildStatus.FAILED,
            completed_at: new Date(),
            error_message: error instanceof Error ? error.message : 'Unknown error'
          }
        });
      }
      
      return {
        success: false,
        instance: instanceId,
        buildTime,
        dataSize: '0B',
        error: error instanceof Error ? error.message : 'Unknown error'
      };
    }
  }

  /**
   * Build OSRM data for both instances
   */
  async buildAllOSRMData(): Promise<OSRMBuildResult[]> {
    logger.info('Starting OSRM data build for all instances');
    
    const results: OSRMBuildResult[] = [];
    
    // Build instance 1
    results.push(await this.buildOSRMData(1));
    
    // Build instance 2
    results.push(await this.buildOSRMData(2));
    
    const successCount = results.filter(r => r.success).length;
    logger.info(`OSRM data build completed: ${successCount}/2 instances successful`);
    
    return results;
  }

  /**
   * Start OSRM instance
   */
  async startOSRMInstance(instanceId: number): Promise<boolean> {
    const instance = this.instances.find(i => i.id === instanceId);
    if (!instance) {
      throw new Error(`OSRM instance ${instanceId} not found`);
    }

    try {
      instance.status = 'starting';
      logger.info(`Starting OSRM instance ${instanceId} on port ${instance.port}`);

      // Check if data exists
      const dataFile = join(instance.dataPath, 'network.osrm');
      if (!existsSync(dataFile)) {
        throw new Error(`OSRM data not found for instance ${instanceId}. Run build first.`);
      }

      // Start OSRM server
      const startCmd = `osrm-routed --algorithm mld --port ${instance.port} ${dataFile}`;
      const child = exec(startCmd, { cwd: instance.dataPath });
      
      if (child.pid) {
        instance.pid = child.pid;
      }
      instance.status = 'running';
      instance.lastHealthCheck = new Date();

      logger.info(`OSRM instance ${instanceId} started with PID ${child.pid}`);
      return true;

    } catch (error) {
      instance.status = 'error';
      logger.error(`Failed to start OSRM instance ${instanceId}:`, error);
      return false;
    }
  }

  /**
   * Stop OSRM instance
   */
  async stopOSRMInstance(instanceId: number): Promise<boolean> {
    const instance = this.instances.find(i => i.id === instanceId);
    if (!instance) {
      throw new Error(`OSRM instance ${instanceId} not found`);
    }

    try {
      instance.status = 'stopping';
      logger.info(`Stopping OSRM instance ${instanceId}`);

      if (instance.pid) {
        // Graceful shutdown
        process.kill(instance.pid, 'SIGTERM');
        
        // Wait for graceful shutdown
        await new Promise(resolve => setTimeout(resolve, 5000));
        
        // Force kill if still running
        try {
          process.kill(instance.pid, 'SIGKILL');
        } catch (e) {
          // Process already stopped
        }
      }

      instance.status = 'stopped';
      delete instance.pid;
      delete instance.lastHealthCheck;

      logger.info(`OSRM instance ${instanceId} stopped`);
      return true;

    } catch (error) {
      instance.status = 'error';
      logger.error(`Failed to stop OSRM instance ${instanceId}:`, error);
      return false;
    }
  }

  /**
   * Rolling restart: stop one instance, start the other
   */
  async rollingRestart(): Promise<OSRMRestartResult> {
    const startTime = Date.now();
    
    try {
      logger.info('Starting rolling restart of OSRM instances');

      // Determine which instance to stop and which to start
      const currentInstance = this.activeInstance;
      const nextInstance = currentInstance === 1 ? 2 : 1;

      // Find the latest ready build for the next instance
      const nextInstanceData = this.instances[nextInstance - 1];
      if (!nextInstanceData) {
        throw new Error(`Instance ${nextInstance} not found`);
      }
      
      const latestBuild = await this.prisma.osrm_builds.findFirst({
        where: {
          instance_name: nextInstanceData.name,
          status: OsrmBuildStatus.READY
        },
        orderBy: { created_at: 'desc' }
      });

      if (!latestBuild) {
        throw new Error(`No ready build found for instance ${nextInstance}`);
      }

      logger.info(`Stopping instance ${currentInstance}, starting instance ${nextInstance} with build ${latestBuild.build_id}`);

      // Stop current instance
      const stopSuccess = await this.stopOSRMInstance(currentInstance);
      if (!stopSuccess) {
        throw new Error(`Failed to stop instance ${currentInstance}`);
      }

      // Start next instance
      const startSuccess = await this.startOSRMInstance(nextInstance);
      if (!startSuccess) {
        throw new Error(`Failed to start instance ${nextInstance}`);
      }

      // Mark build as deployed
      await this.prisma.osrm_builds.update({
        where: { build_id: latestBuild.build_id },
        data: {
          status: OsrmBuildStatus.DEPLOYED,
          deployed_at: new Date()
        }
      });

      // Mark previous builds as deprecated
      const currentInstanceData = this.instances[currentInstance - 1];
      if (currentInstanceData) {
        await this.prisma.osrm_builds.updateMany({
          where: {
            instance_name: currentInstanceData.name,
            status: OsrmBuildStatus.DEPLOYED
          },
          data: {
            status: OsrmBuildStatus.DEPRECATED
          }
        });
      }

      // Update active instance
      this.activeInstance = nextInstance;
      const downtime = Date.now() - startTime;

      logger.info(`Rolling restart completed. Active instance: ${this.activeInstance}`);

      return {
        success: true,
        stoppedInstance: currentInstance,
        startedInstance: nextInstance,
        downtime
      };

    } catch (error) {
      const downtime = Date.now() - startTime;
      logger.error('Rolling restart failed:', error);
      
      return {
        success: false,
        stoppedInstance: this.activeInstance,
        startedInstance: 0,
        downtime,
        error: error instanceof Error ? error.message : 'Unknown error'
      };
    }
  }

  /**
   * Check health of all OSRM instances
   */
  async checkHealth(): Promise<{ instance: number; healthy: boolean; responseTime?: number }[]> {
    const results = [];

    for (const instance of this.instances) {
      if (instance.status !== 'running') {
        results.push({ instance: instance.id, healthy: false });
        continue;
      }

      try {
        const startTime = Date.now();
        const response = await fetch(`http://localhost:${instance.port}/route/v1/driving/106.7718,10.8505;106.8032,10.8623?overview=false`);
        const responseTime = Date.now() - startTime;
        
        const healthy = response.ok;
        instance.lastHealthCheck = new Date();
        
        results.push({ instance: instance.id, healthy, responseTime });
        
      } catch (error) {
        results.push({ instance: instance.id, healthy: false });
        logger.warn(`Health check failed for instance ${instance.id}:`, error);
      }
    }

    return results;
  }

  /**
   * Get OSRM instance status
   */
  getInstanceStatus(instanceId: number): OSRMInstance | undefined {
    return this.instances.find(i => i.id === instanceId);
  }

  /**
   * Get all instances status
   */
  getAllInstancesStatus(): OSRMInstance[] {
    return [...this.instances];
  }

  /**
   * Get active instance
   */
  getActiveInstance(): number {
    return this.activeInstance;
  }

  /**
   * Get data size of OSRM instance
   */
  private async getDataSize(dataPath: string): Promise<string> {
    try {
      const { stdout } = await execAsync(`du -sh ${dataPath}`);
      const parts = stdout.trim().split('\t');
      return parts[0] || 'Unknown';
    } catch (error) {
      return 'Unknown';
    }
  }

  /**
   * Get segment count from database
   */
  private async getSegmentCount(): Promise<number> {
    try {
      const count = await this.prisma.road_segments.count();
      return count;
    } catch (error) {
      logger.warn('Failed to get segment count from database:', error);
      return 0;
    }
  }

  /**
   * Validate OSRM data integrity
   */
  async validateData(instanceId: number): Promise<boolean> {
    const instance = this.instances.find(i => i.id === instanceId);
    if (!instance) return false;

    const requiredFiles = [
      'network.osrm',
      'network.osrm.cells',
      'network.osrm.cnbg',
      'network.osrm.ebg',
      'network.osrm.edges',
      'network.osrm.geometry',
      'network.osrm.mldgr',
      'network.osrm.names',
      'network.osrm.partition',
      'network.osrm.properties'
    ];

    for (const file of requiredFiles) {
      const filePath = join(instance.dataPath, file);
      if (!existsSync(filePath)) {
        logger.warn(`Missing OSRM file: ${file} for instance ${instanceId}`);
        return false;
      }
    }

    return true;
  }

  /**
   * Get build history for an instance
   */
  async getBuildHistory(instanceId: number, limit: number = 10) {
    const instance = this.instances.find(i => i.id === instanceId);
    if (!instance) return [];

    return await this.prisma.osrm_builds.findMany({
      where: { instance_name: instance.name },
      orderBy: { created_at: 'desc' },
      take: limit
    });
  }

  /**
   * Get all build history
   */
  async getAllBuildHistory(limit: number = 20) {
    return await this.prisma.osrm_builds.findMany({
      orderBy: { created_at: 'desc' },
      take: limit
    });
  }

  /**
   * Get current deployment status
   */
  async getDeploymentStatus() {
    const deployedBuilds = await this.prisma.osrm_builds.findMany({
      where: { status: OsrmBuildStatus.DEPLOYED },
      orderBy: { deployed_at: 'desc' }
    });

    const readyBuilds = await this.prisma.osrm_builds.findMany({
      where: { status: OsrmBuildStatus.READY },
      orderBy: { created_at: 'desc' }
    });

    return {
      deployed: deployedBuilds,
      ready: readyBuilds,
      activeInstance: this.activeInstance
    };
  }

  /**
   * Refresh OSM file path (useful when new OSM files are added)
   */
  refreshOsmFilePath(): string {
    const newPath = this.findLatestOsmFile();
    logger.info(`OSM file path refreshed: ${newPath}`);
    return newPath;
  }

  /**
   * Get current OSM file info
   */
  getOsmFileInfo() {
    try {
      const stats = statSync(this.osmFilePath);
      return {
        path: this.osmFilePath,
        name: this.osmFilePath.split('/').pop(),
        size: stats.size,
        modified: stats.mtime,
        exists: true
      };
    } catch (error) {
      return {
        path: this.osmFilePath,
        name: this.osmFilePath.split('/').pop(),
        size: 0,
        modified: null,
        exists: false,
        error: error instanceof Error ? error.message : 'Unknown error'
      };
    }
  }
}
