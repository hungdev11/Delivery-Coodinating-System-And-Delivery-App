/**
 * OSRM Container Management Service
 * Manages Docker containers for OSRM instances
 */
export interface OSRMContainer {
    name: string;
    containerName: string;
    model: string;
    port: number;
    dataPath: string;
}
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
export declare class OSRMContainerService {
    private osrmDataPath;
    private dockerImage;
    private composeProjectName;
    private composeFilePath;
    constructor();
    /**
     * Get status of all OSRM containers using docker-compose
     */
    getStatus(): Promise<ContainerStatus[]>;
    /**
     * Start a specific OSRM container using docker-compose
     */
    startContainer(model: string): Promise<ContainerActionResult>;
    /**
     * Stop a specific OSRM container using docker-compose
     */
    stopContainer(model: string): Promise<ContainerActionResult>;
    /**
     * Restart a specific OSRM container using docker-compose
     */
    restartContainer(model: string): Promise<ContainerActionResult>;
    /**
     * Rebuild a specific OSRM container using docker-compose (stop, remove, recreate)
     */
    rebuildContainer(model: string): Promise<ContainerActionResult>;
    /**
     * Health check for a specific container
     */
    healthCheck(model: string): Promise<{
        healthy: boolean;
        message: string;
    }>;
}
//# sourceMappingURL=osrm-container.service.d.ts.map