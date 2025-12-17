/**
 * OSRM Container Management Service
 * Manages Docker containers for OSRM instances
 */

import { exec } from 'child_process';
import { promisify } from 'util';
import { resolve } from 'path';
import { logger } from '../common/logger';

const execAsync = promisify(exec);

export interface OSRMContainer {
  name: string;
  containerName: string;
  model: string;
  port: number;
  dataPath: string;
}

// OSRM containers configuration
// name: service name in docker-compose.yml
// containerName: actual Docker container name (with project prefix)
// model: model identifier used in API endpoints
const OSRM_CONTAINERS: OSRMContainer[] = [
  {
    name: 'osrm-v2-full', // Service name in docker-compose.yml
    containerName: 'dss-osrm-v2-full', // Actual container name (project_name + service_name)
    model: 'osrm-full', // Model identifier for API
    port: 5000,
    dataPath: 'osrm-full',
  },
  {
    name: 'osrm-v2-rating-only',
    containerName: 'dss-osrm-v2-rating-only',
    model: 'osrm-rating-only',
    port: 5000,
    dataPath: 'osrm-rating-only',
  },
  {
    name: 'osrm-v2-blocking-only',
    containerName: 'dss-osrm-v2-blocking-only',
    model: 'osrm-blocking-only',
    port: 5000,
    dataPath: 'osrm-blocking-only',
  },
  {
    name: 'osrm-v2-base',
    containerName: 'dss-osrm-v2-base',
    model: 'osrm-base',
    port: 5000,
    dataPath: 'osrm-base',
  },
];

export interface ContainerStatus {
  name: string;
  containerName: string;
  status: 'running' | 'stopped' | 'error' | 'not-found';
  health?: 'healthy' | 'unhealthy' | 'starting';
  port?: number;
}

export interface ContainerActionResult {
  success: boolean;
  container: string;
  message?: string;
  error?: string;
}

export class OSRMContainerService {
  private osrmDataPath: string;
  private dockerImage: string;
  private composeProjectName: string;
  private composeFilePath: string;

  constructor() {
    this.osrmDataPath = resolve(process.env.OSRM_DATA_PATH || './osrm_data');
    this.dockerImage = process.env.OSRM_DOCKER_IMAGE || 'osrm/osrm-backend:latest';
    this.composeProjectName = process.env.DOCKER_COMPOSE_PROJECT_NAME || 'dss';
    // Resolve compose file path to absolute path
    this.composeFilePath = resolve(process.env.DOCKER_COMPOSE_FILE || './docker-compose.yml');
  }

  /**
   * Get status of all OSRM containers using docker-compose
   */
  async getStatus(): Promise<ContainerStatus[]> {
    const statuses: ContainerStatus[] = [];

    for (const container of OSRM_CONTAINERS) {
      try {
        // Use docker-compose ps to check container status
        // Try JSON format first (docker-compose v2+)
        let status: ContainerStatus['status'] = 'not-found';
        let health: ContainerStatus['health'] | undefined;

        try {
          const { stdout: statusOutput } = await execAsync(
            `docker-compose -p ${this.composeProjectName} -f ${this.composeFilePath} ps ${container.name} --format json 2>&1`
          );
          
          const psData = JSON.parse(statusOutput.trim() || '[]');
          if (Array.isArray(psData) && psData.length > 0) {
            const containerInfo = psData[0];
            const state = containerInfo.State || '';
            
            if (state.includes('Up')) {
              status = 'running';
              // Check health status
              const healthStatus = containerInfo.Health || '';
              if (healthStatus === 'healthy') {
                health = 'healthy';
              } else if (healthStatus === 'unhealthy') {
                health = 'unhealthy';
              } else {
                health = 'starting';
              }
            } else if (state.includes('Exit')) {
              status = 'stopped';
            } else {
              status = 'error';
            }
          }
        } catch (jsonError) {
          // Fallback to text format (docker-compose v1 or if JSON fails)
          try {
            const { stdout: textOutput } = await execAsync(
              `docker-compose -p ${this.composeProjectName} -f ${this.composeFilePath} ps ${container.name} 2>&1`
            );
            const statusLine = textOutput.trim();
            if (statusLine.includes('Up')) {
              status = 'running';
              health = 'starting';
            } else if (statusLine.includes('Exit')) {
              status = 'stopped';
            }
          } catch (textError) {
            // Final fallback to docker ps
            const { stdout: dockerOutput } = await execAsync(
              `docker ps -a --filter "name=${container.containerName}" --format "{{.Status}}" 2>&1 || echo ""`
            );
            const statusLine = dockerOutput.trim();
            if (statusLine.includes('Up')) {
              status = 'running';
              health = 'starting';
            } else if (statusLine.includes('Exited')) {
              status = 'stopped';
            }
          }
        }

        statuses.push({
          name: container.name,
          containerName: container.containerName,
          status,
          health,
          port: container.port,
        });
      } catch (error: any) {
        // If container doesn't exist, that's fine - return not-found
        statuses.push({
          name: container.name,
          containerName: container.containerName,
          status: 'not-found',
        });
      }
    }

    return statuses;
  }

  /**
   * Start a specific OSRM container using docker-compose
   */
  async startContainer(model: string): Promise<ContainerActionResult> {
    const container = OSRM_CONTAINERS.find(c => c.model === model);
    if (!container) {
      return {
        success: false,
        container: model,
        error: `Container not found for model: ${model}`,
      };
    }

    try {
      // Use docker-compose up to start the service
      await execAsync(
        `docker-compose -p ${this.composeProjectName} -f ${this.composeFilePath} up -d ${container.name} 2>&1`
      );
      logger.info(`Started container: ${container.containerName} via docker-compose`);
      return {
        success: true,
        container: container.name,
        message: `Container ${container.containerName} started successfully`,
      };
    } catch (error: any) {
      logger.error(`Failed to start container ${container.containerName}`, { error: error.message });
      return {
        success: false,
        container: container.name,
        error: error.message,
      };
    }
  }

  /**
   * Stop a specific OSRM container using docker-compose
   */
  async stopContainer(model: string): Promise<ContainerActionResult> {
    const container = OSRM_CONTAINERS.find(c => c.model === model);
    if (!container) {
      return {
        success: false,
        container: model,
        error: `Container not found for model: ${model}`,
      };
    }

    try {
      // Use docker-compose stop
      await execAsync(
        `docker-compose -p ${this.composeProjectName} -f ${this.composeFilePath} stop ${container.name} 2>&1`
      );
      logger.info(`Stopped container: ${container.containerName} via docker-compose`);
      return {
        success: true,
        container: container.name,
        message: `Container ${container.containerName} stopped successfully`,
      };
    } catch (error: any) {
      logger.error(`Failed to stop container ${container.containerName}`, { error: error.message });
      return {
        success: false,
        container: container.name,
        error: error.message,
      };
    }
  }

  /**
   * Restart a specific OSRM container using docker-compose
   */
  async restartContainer(model: string): Promise<ContainerActionResult> {
    const container = OSRM_CONTAINERS.find(c => c.model === model);
    if (!container) {
      return {
        success: false,
        container: model,
        error: `Container not found for model: ${model}`,
      };
    }

    try {
      // Use docker-compose restart
      await execAsync(
        `docker-compose -p ${this.composeProjectName} -f ${this.composeFilePath} restart ${container.name} 2>&1`
      );
      logger.info(`Restarted container: ${container.containerName} via docker-compose`);
      return {
        success: true,
        container: container.name,
        message: `Container ${container.containerName} restarted successfully`,
      };
    } catch (error: any) {
      // If restart fails, try stop then start
      try {
        await this.stopContainer(model);
        return await this.startContainer(model);
      } catch (fallbackError: any) {
        logger.error(`Failed to restart container ${container.containerName}`, { error: error.message });
        return {
          success: false,
          container: container.name,
          error: error.message,
        };
      }
    }
  }

  /**
   * Rebuild a specific OSRM container using docker-compose (stop, remove, recreate)
   */
  async rebuildContainer(model: string): Promise<ContainerActionResult> {
    const container = OSRM_CONTAINERS.find(c => c.model === model);
    if (!container) {
      return {
        success: false,
        container: model,
        error: `Container not found for model: ${model}`,
      };
    }

    try {
      // Stop and remove existing container using docker-compose
      try {
        await execAsync(
          `docker-compose -p ${this.composeProjectName} -f ${this.composeFilePath} stop ${container.name} 2>&1`
        );
      } catch (e) {
        // Ignore if container doesn't exist or already stopped
      }

      try {
        await execAsync(
          `docker-compose -p ${this.composeProjectName} -f ${this.composeFilePath} rm -f ${container.name} 2>&1`
        );
      } catch (e) {
        // Ignore if container doesn't exist
      }

      // Create and start new container using docker-compose
      await execAsync(
        `docker-compose -p ${this.composeProjectName} -f ${this.composeFilePath} up -d --force-recreate ${container.name} 2>&1`
      );
      
      logger.info(`Rebuilt container: ${container.containerName} via docker-compose`);
      return {
        success: true,
        container: container.name,
        message: `Container ${container.containerName} rebuilt successfully`,
      };
    } catch (error: any) {
      logger.error(`Failed to rebuild container ${container.containerName}`, { error: error.message });
      return {
        success: false,
        container: container.name,
        error: error.message,
      };
    }
  }

  /**
   * Health check for a specific container
   */
  async healthCheck(model: string): Promise<{ healthy: boolean; message: string }> {
    const container = OSRM_CONTAINERS.find(c => c.model === model);
    if (!container) {
      return {
        healthy: false,
        message: `Container not found for model: ${model}`,
      };
    }

    try {
      // Check if container is running
      let isRunning = false;
      try {
        const { stdout } = await execAsync(
          `docker ps --filter "name=${container.containerName}" --format "{{.Names}}" 2>&1`
        );
        isRunning = stdout.trim().length > 0;
      } catch {
        isRunning = false;
      }

      if (!isRunning) {
        return {
          healthy: false,
          message: 'Container is not running',
        };
      }

      // Use docker exec to check health from inside container
      // This works regardless of port exposure
      const testCmd = `docker exec ${container.containerName} curl -f http://localhost:${container.port}/route/v1/driving/106.7718,10.8505;106.8032,10.8623?overview=false 2>&1`;
      
      try {
        await execAsync(testCmd, { timeout: 5000 });
        return {
          healthy: true,
          message: 'Container is healthy and responding',
        };
      } catch (execError: any) {
        return {
          healthy: false,
          message: `Health check failed: ${execError.message || 'Container not responding'}`,
        };
      }
    } catch (error: any) {
      return {
        healthy: false,
        message: error.message || 'Health check failed',
      };
    }
  }
}
