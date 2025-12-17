/**
 * OSRM Management Client Service
 * 
 * Client service to communicate with osrm-management-system API
 * This replaces direct OSRM operations in zone_service
 */

import { logger } from '../../common/logger';

export interface OSRMManagementClientConfig {
  baseUrl: string;
  timeout?: number;
}

export interface ExtractCompleteRequest {
  polyFile?: string;
}

export interface ExtractCompleteResponse {
  success: boolean;
  outputPath?: string;
  error?: string;
}

export interface GenerateOSRMV2Response {
  success: boolean;
  models?: string[];
  error?: string;
}

export interface ContainerStatus {
  model: string;
  status: 'stopped' | 'running' | 'starting' | 'stopping' | 'error';
  containerName?: string;
  port?: number;
  health?: {
    healthy: boolean;
    responseTime?: number;
    message?: string;
  };
}

export interface ContainerActionResult {
  success: boolean;
  message?: string;
  error?: string;
}

export class OSRMManagementClientService {
  private config: OSRMManagementClientConfig;

  constructor(config?: Partial<OSRMManagementClientConfig>) {
    const baseUrl = config?.baseUrl || process.env.OSRM_MANAGEMENT_URL || 'http://localhost:21520';
    this.config = {
      baseUrl: baseUrl.replace(/\/$/, ''), // Remove trailing slash
      timeout: config?.timeout || 300000, // 5 minutes default
    };
  }

  /**
   * Make HTTP request with timeout
   */
  private async fetchWithTimeout(url: string, options: RequestInit = {}): Promise<Response> {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), this.config.timeout);

    try {
      const response = await fetch(url, {
        ...options,
        signal: controller.signal,
      });
      clearTimeout(timeoutId);
      return response;
    } catch (error) {
      clearTimeout(timeoutId);
      if (error instanceof Error && error.name === 'AbortError') {
        throw new Error(`Request timeout after ${this.config.timeout}ms`);
      }
      throw error;
    }
  }

  /**
   * Extract complete OSM data
   * POST /api/v1/extract/complete
   */
  async extractComplete(request?: ExtractCompleteRequest): Promise<ExtractCompleteResponse> {
    try {
      const url = `${this.config.baseUrl}/api/v1/extract/complete`;
      logger.info(`Calling osrm-management-system: ${url}`);

      const response = await this.fetchWithTimeout(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(request || {}),
      });

      const data = await response.json() as { result?: ExtractCompleteResponse; [key: string]: any };

      if (!response.ok) {
        throw new Error(data.message || `HTTP ${response.status}: ${response.statusText}`);
      }

      return data.result || data as ExtractCompleteResponse;
    } catch (error) {
      logger.error('Failed to call extract complete API:', error);
      return {
        success: false,
        error: error instanceof Error ? error.message : 'Unknown error',
      };
    }
  }

  /**
   * Generate OSRM V2 data
   * POST /api/v1/generate/osrm-v2
   */
  async generateOSRMV2(): Promise<GenerateOSRMV2Response> {
    try {
      const url = `${this.config.baseUrl}/api/v1/generate/osrm-v2`;
      logger.info(`Calling osrm-management-system: ${url}`);

      const response = await this.fetchWithTimeout(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
      });

      const data = await response.json() as { result?: GenerateOSRMV2Response; [key: string]: any };

      if (!response.ok) {
        throw new Error(data.message || `HTTP ${response.status}: ${response.statusText}`);
      }

      return data.result || data as GenerateOSRMV2Response;
    } catch (error) {
      logger.error('Failed to call generate OSRM V2 API:', error);
      return {
        success: false,
        error: error instanceof Error ? error.message : 'Unknown error',
      };
    }
  }

  /**
   * Get build status for all models
   * GET /api/v1/builds/status
   */
  async getBuildStatus(): Promise<any> {
    try {
      const url = `${this.config.baseUrl}/api/v1/builds/status`;
      const response = await this.fetchWithTimeout(url);

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const data = await response.json() as { result?: any; [key: string]: any };
      return data.result || data;
    } catch (error) {
      logger.warn('Failed to fetch build status from osrm-management-system:', error);
      return null;
    }
  }

  /**
   * Get container status for all OSRM models
   * GET /api/v1/osrm/containers/status
   */
  async getContainerStatus(): Promise<ContainerStatus[]> {
    try {
      const url = `${this.config.baseUrl}/api/v1/osrm/containers/status`;
      const response = await this.fetchWithTimeout(url);

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const data = await response.json() as { result?: ContainerStatus[]; [key: string]: any };
      return data.result || data as ContainerStatus[];
    } catch (error) {
      logger.error('Failed to get container status:', error);
      return [];
    }
  }

  /**
   * Start OSRM container
   * POST /api/v1/osrm/containers/:model/start
   */
  async startContainer(model: string): Promise<ContainerActionResult> {
    try {
      const url = `${this.config.baseUrl}/api/v1/osrm/containers/${model}/start`;
      const response = await this.fetchWithTimeout(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
      });

      const data = await response.json() as { result?: ContainerActionResult; message?: string; [key: string]: any };

      if (!response.ok) {
        throw new Error(data.message || `HTTP ${response.status}: ${response.statusText}`);
      }

      if (data.result) {
        return data.result;
      }
      
      const result: ContainerActionResult = { success: true };
      if (data.message) {
        result.message = data.message;
      }
      return result;
    } catch (error) {
      logger.error(`Failed to start container ${model}:`, error);
      return {
        success: false,
        error: error instanceof Error ? error.message : 'Unknown error',
      };
    }
  }

  /**
   * Stop OSRM container
   * POST /api/v1/osrm/containers/:model/stop
   */
  async stopContainer(model: string): Promise<ContainerActionResult> {
    try {
      const url = `${this.config.baseUrl}/api/v1/osrm/containers/${model}/stop`;
      const response = await this.fetchWithTimeout(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
      });

      const data = await response.json() as { result?: ContainerActionResult; message?: string; [key: string]: any };

      if (!response.ok) {
        throw new Error(data.message || `HTTP ${response.status}: ${response.statusText}`);
      }

      if (data.result) {
        return data.result;
      }
      
      const result: ContainerActionResult = { success: true };
      if (data.message) {
        result.message = data.message;
      }
      return result;
    } catch (error) {
      logger.error(`Failed to stop container ${model}:`, error);
      return {
        success: false,
        error: error instanceof Error ? error.message : 'Unknown error',
      };
    }
  }

  /**
   * Restart OSRM container
   * POST /api/v1/osrm/containers/:model/restart
   */
  async restartContainer(model: string): Promise<ContainerActionResult> {
    try {
      const url = `${this.config.baseUrl}/api/v1/osrm/containers/${model}/restart`;
      const response = await this.fetchWithTimeout(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
      });

      const data = await response.json() as { result?: ContainerActionResult; message?: string; [key: string]: any };

      if (!response.ok) {
        throw new Error(data.message || `HTTP ${response.status}: ${response.statusText}`);
      }

      if (data.result) {
        return data.result;
      }
      
      const result: ContainerActionResult = { success: true };
      if (data.message) {
        result.message = data.message;
      }
      return result;
    } catch (error) {
      logger.error(`Failed to restart container ${model}:`, error);
      return {
        success: false,
        error: error instanceof Error ? error.message : 'Unknown error',
      };
    }
  }

  /**
   * Health check for OSRM container
   * GET /api/v1/osrm/containers/:model/health
   */
  async healthCheck(model: string): Promise<{ healthy: boolean; message?: string; responseTime?: number }> {
    try {
      const url = `${this.config.baseUrl}/api/v1/osrm/containers/${model}/health`;
      const response = await this.fetchWithTimeout(url);

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const data = await response.json() as { result?: any; [key: string]: any };
      return data.result || { healthy: false };
    } catch (error) {
      logger.error(`Failed to health check container ${model}:`, error);
      return {
        healthy: false,
        message: error instanceof Error ? error.message : 'Unknown error',
      };
    }
  }
}
