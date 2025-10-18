/**
 * Routing Interface
 * Interfaces for routing module
 */

export interface IRoutingService {
  calculateRoute(request: any): Promise<any>;
  calculatePriorityRoutes(request: any): Promise<any>;
}
