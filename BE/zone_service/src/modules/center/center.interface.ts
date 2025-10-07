/**
 * Center Service Interface
 */

import { PagedData } from '../../common/types/restful';
import { CreateCenterDto, UpdateCenterDto, CenterDto, CenterPagingRequest } from './center.model';

export interface ICenterService {
  /**
   * Get all centers with pagination
   */
  getAllCenters(request: CenterPagingRequest): Promise<PagedData<CenterDto>>;

  /**
   * Get center by ID
   */
  getCenterById(id: string): Promise<CenterDto | null>;

  /**
   * Get center by code
   */
  getCenterByCode(code: string): Promise<CenterDto | null>;

  /**
   * Create a new center
   */
  createCenter(data: CreateCenterDto): Promise<CenterDto>;

  /**
   * Update a center
   */
  updateCenter(id: string, data: UpdateCenterDto): Promise<CenterDto>;

  /**
   * Delete a center
   */
  deleteCenter(id: string): Promise<void>;

  /**
   * Check if center exists by code
   */
  existsByCode(code: string): Promise<boolean>;
}
