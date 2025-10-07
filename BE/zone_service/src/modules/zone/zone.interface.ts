/**
 * Zone Service Interface
 */

import { PagedData } from '../../common/types/restful';
import { CreateZoneDto, UpdateZoneDto, ZoneDto, ZonePagingRequest } from './zone.model';

export interface IZoneService {
  /**
   * Get all zones with pagination
   */
  getAllZones(request: ZonePagingRequest): Promise<PagedData<ZoneDto>>;

  /**
   * Get zone by ID
   */
  getZoneById(id: string): Promise<ZoneDto | null>;

  /**
   * Get zone by code
   */
  getZoneByCode(code: string): Promise<ZoneDto | null>;

  /**
   * Get zones by center ID
   */
  getZonesByCenterId(centerId: string): Promise<ZoneDto[]>;

  /**
   * Create a new zone
   */
  createZone(data: CreateZoneDto): Promise<ZoneDto>;

  /**
   * Update a zone
   */
  updateZone(id: string, data: UpdateZoneDto): Promise<ZoneDto>;

  /**
   * Delete a zone
   */
  deleteZone(id: string): Promise<void>;

  /**
   * Check if zone exists by code
   */
  existsByCode(code: string): Promise<boolean>;
}
