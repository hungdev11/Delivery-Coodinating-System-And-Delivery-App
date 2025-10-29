/**
 * Zone Service with advanced filtering and sorting
 */

import { PrismaClient } from '@prisma/client';
import { PagingRequest, PagedData } from '../types/filter';
import { QueryParser } from '../utils/query-parser';

export class ZoneService {
  constructor(private prisma: PrismaClient) {}

  /**
   * Get zones with advanced filtering and sorting
   */
  async getZones(request: PagingRequest): Promise<PagedData<any>> {
    const { skip, take, where, orderBy } = QueryParser.parsePagingRequest(request);
    
    // Add global search if provided
    const globalSearch = request.search ? QueryParser.buildGlobalSearch(request.search) : {};
    const finalWhere = request.search ? { ...where, ...globalSearch } : where;

    // Get total count
    const totalElements = await this.prisma.zones.count({
      where: finalWhere
    });

    // Get paginated data
    const data = await this.prisma.zones.findMany({
      where: finalWhere,
      orderBy,
      skip,
      take,
      include: {
        // Add any relations you need
      }
    });

    const totalPages = Math.ceil(totalElements / take);

    return {
      data,
      page: {
        page: request.page || 0,
        size: request.size || 10,
        totalElements,
        totalPages,
        ...(request.filters && { filters: request.filters }),
        ...(request.sorts && { sorts: request.sorts }),
        ...(request.selected && { selected: request.selected })
      }
    };
  }

  /**
   * Get zone by ID
   */
  async getZoneById(id: string): Promise<any | null> {
    return this.prisma.zones.findUnique({
      where: { zone_id: id },
      include: {
        // Add any relations you need
      }
    });
  }

  /**
   * Create zone
   */
  async createZone(zoneData: any): Promise<any> {
    return this.prisma.zones.create({
      data: zoneData,
      include: {
        // Add any relations you need
      }
    });
  }

  /**
   * Update zone
   */
  async updateZone(id: string, zoneData: any): Promise<any> {
    return this.prisma.zones.update({
      where: { zone_id: id },
      data: zoneData,
      include: {
        // Add any relations you need
      }
    });
  }

  /**
   * Delete zone
   */
  async deleteZone(id: string): Promise<void> {
    await this.prisma.zones.delete({
      where: { zone_id: id }
    });
  }

  /**
   * Get filterable fields for frontend
   */
  getFilterableFields(): string[] {
    return [
      'zone_id',
      'code',
      'name',
      'polygon',
      'center_id'
    ];
  }

  /**
   * Get sortable fields for frontend
   */
  getSortableFields(): string[] {
    return [
      'zone_id',
      'code',
      'name',
      'center_id'
    ];
  }
}
