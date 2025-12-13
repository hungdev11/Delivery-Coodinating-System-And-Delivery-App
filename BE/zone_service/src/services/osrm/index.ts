/**
 * OSRM service index
 */

export { OSRMGeneratorService } from './osrm-generator.service';
export type { OSRMBuildConfig } from './osrm-generator.service';

export { OSRMRouterService } from './osrm-router.service';
export type {
  Coordinate,
  RouteOptions,
  OSRMRoute,
  OSRMLeg,
  OSRMStep,
  OSRMManeuver,
  OSRMIntersection,
  OSRMRouteResponse,
} from './osrm-router.service';

export { OSRMV2GeneratorService } from './osrm-v2-generator.service';
