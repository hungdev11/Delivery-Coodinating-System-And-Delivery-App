/**
 * OSRM Container Controller
 * RESTful API endpoints for managing OSRM Docker containers
 */
import { Request, Response, NextFunction } from 'express';
export declare class OSRMContainerController {
    private containerService;
    constructor();
    /**
     * GET /api/v1/osrm/containers/status
     * Get status of all OSRM containers
     */
    getStatus(req: Request, res: Response, next: NextFunction): Promise<void>;
    /**
     * POST /api/v1/osrm/containers/:model/start
     * Start a specific OSRM container
     */
    startContainer(req: Request, res: Response, next: NextFunction): Promise<void>;
    /**
     * POST /api/v1/osrm/containers/:model/stop
     * Stop a specific OSRM container
     */
    stopContainer(req: Request, res: Response, next: NextFunction): Promise<void>;
    /**
     * POST /api/v1/osrm/containers/:model/restart
     * Restart a specific OSRM container
     */
    restartContainer(req: Request, res: Response, next: NextFunction): Promise<void>;
    /**
     * POST /api/v1/osrm/containers/:model/rebuild
     * Rebuild a specific OSRM container (stop, remove, recreate)
     */
    rebuildContainer(req: Request, res: Response, next: NextFunction): Promise<void>;
    /**
     * GET /api/v1/osrm/containers/:model/health
     * Health check for a specific OSRM container
     */
    healthCheck(req: Request, res: Response, next: NextFunction): Promise<void>;
}
//# sourceMappingURL=osrm-container.controller.d.ts.map