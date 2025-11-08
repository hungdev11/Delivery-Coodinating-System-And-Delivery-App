/**
 * Zone Service
 * Static class for zone business logic
 */

import { prisma } from '../../common/database/prisma.client';
import { logger } from '../../common/logger/logger.service';
import { PagedData, Paging as RestfulPaging } from '../../common/types/restful';
import { createError } from '../../common/middleware/error.middleware';
import { CreateZoneDto, UpdateZoneDto, ZoneDto, ZonePagingRequest } from './zone.model';
import { PagingRequest } from '../../common/types/filter';
import { PagingRequestV0, PagingRequestV2 } from '../../common/types/filter-v2';
import { QueryParser } from '../../common/utils/query-parser';
import { QueryParserV2 } from '../../common/utils/query-parser-v2';

export class ZoneService {
  /**
   * V0: Get zones with simple paging and sorting (no dynamic filters)
   */
  public static async getZonesV0(request: PagingRequestV0): Promise<PagedData<any>> {
    try {
      const page = request.page || 0;
      const size = request.size || 10;
      const skip = page * size;
      const take = size;

      // Build orderBy from sorts
      const orderBy: any = request.sorts && request.sorts.length > 0
        ? request.sorts.map(sort => ({ [sort.field]: sort.direction }))
        : [{ id: 'desc' }];

      // Get total count
      const totalElements = await prisma.zones.count();

      // Get paginated data
      const data = await prisma.zones.findMany({
        orderBy,
        skip,
        take,
        include: {
          centers: true,
        }
      });

      const totalPages = Math.ceil(totalElements / take);

      const paging = new RestfulPaging<any>();
      paging.page = page;
      paging.size = size;
      paging.totalElements = totalElements;
      paging.totalPages = totalPages;
      paging.filters = null;
      paging.sorts = request.sorts || [];
      paging.selected = request.selected || [];

      return {
        data,
        page: paging
      };
    } catch (error) {
      logger.error('Failed to get zones (V0)', { error });
      throw createError('Failed to get zones', 500);
    }
  }

  /**
   * V2: Get zones with enhanced filtering (operations between each pair)
   */
  public static async getZonesV2(request: PagingRequestV2): Promise<PagedData<any>> {
    try {
      const page = request.page || 0;
      const size = request.size || 10;
      const skip = page * size;
      const take = size;

      // Parse V2 filters
      const where = request.filters ? QueryParserV2.parseFilterGroup(request.filters) : {};

      // Build orderBy from sorts
      const orderBy: any = request.sorts && request.sorts.length > 0
        ? request.sorts.map(sort => ({ [sort.field]: sort.direction }))
        : [{ id: 'desc' }];

      // Get total count
      const totalElements = await prisma.zones.count({ where });

      // Get paginated data
      const data = await prisma.zones.findMany({
        where,
        orderBy,
        skip,
        take,
        include: {
          centers: true,
        }
      });

      const totalPages = Math.ceil(totalElements / take);

      const paging = new RestfulPaging<any>();
      paging.page = page;
      paging.size = size;
      paging.totalElements = totalElements;
      paging.totalPages = totalPages;
      paging.filters = null; // V2 filters not returned in paging
      paging.sorts = request.sorts || [];
      paging.selected = request.selected || [];

      return {
        data,
        page: paging
      };
    } catch (error) {
      logger.error('Failed to get zones (V2)', { error });
      throw createError('Failed to get zones', 500);
    }
  }

  /**
   * Get zones with advanced filtering and sorting (V1)
   */
  public static async getZones(request: PagingRequest): Promise<PagedData<any>> {
    try {
      console.log('Parsing request:', JSON.stringify(request, null, 2));
      
      const { skip, take, where, orderBy } = QueryParser.parsePagingRequest(request);
      
      console.log('Parsed query params:', { skip, take, where, orderBy });
      
      // Add global search if provided
      const globalSearch = request.search ? QueryParser.buildGlobalSearch(request.search) : {};
      const finalWhere = request.search ? { ...where, ...globalSearch } : where;

      // Get total count
      const totalElements = await prisma.zones.count({
        where: finalWhere
      });

      // Get paginated data
      const data = await prisma.zones.findMany({
        where: finalWhere,
        orderBy,
        skip,
        take,
        include: {
          centers: true,
        }
      });

      const totalPages = Math.ceil(totalElements / take);

      const paging = new RestfulPaging<any>();
      paging.page = request.page || 0;
      paging.size = request.size || 10;
      paging.totalElements = totalElements;
      paging.totalPages = totalPages;
      paging.filters = request.filters;
      paging.sorts = request.sorts || [];
      paging.selected = request.selected || [];

      return {
        data,
        page: paging
      };
    } catch (error) {
      logger.error('Failed to get zones with advanced filtering', { error });
      throw createError('Failed to get zones', 500);
    }
  }

  /**
   * Get all zones with pagination
   */
  public static async getAllZones(request: ZonePagingRequest): Promise<PagedData<ZoneDto>> {
    try {
      const where: any = {};

      // Apply search filter
      if (request.search) {
        where.OR = [
          { name: { contains: request.search } },
          { code: { contains: request.search } },
        ];
      }

      // Apply code filter
      if (request.code) {
        where.code = request.code;
      }

      // Apply center filter
      if (request.centerId) {
        where.center_id = request.centerId;
      }

      // Get total count
      const totalElements = await prisma.zones.count({ where });

      // Get data with center information
      const zones = await prisma.zones.findMany({
        where,
        include: {
          centers: true,
        },
        skip: request.getSkip(),
        take: request.getTake(),
        orderBy: { name: 'asc' },
      });

      // Map to DTOs
      const data = zones.map(zone => this.mapToDto(zone));

      // Create paging
      const paging = request.createPaging<string>(totalElements);

      return new PagedData(data, paging);
    } catch (error) {
      logger.error('Failed to get all zones', { error });
      throw createError('Failed to get zones', 500);
    }
  }

  /**
   * Get zone by ID
   */
  public static async getZoneById(id: string): Promise<ZoneDto | null> {
    try {
      const zone = await prisma.zones.findUnique({
        where: { zone_id: id },
        include: {
          centers: true,
        },
      });

      return zone ? this.mapToDto(zone) : null;
    } catch (error) {
      logger.error('Failed to get zone by ID', { id, error });
      throw createError('Failed to get zone', 500);
    }
  }

  /**
   * Get zone by code
   */
  public static async getZoneByCode(code: string): Promise<ZoneDto | null> {
    try {
      const zone = await prisma.zones.findUnique({
        where: { code },
        include: {
          centers: true,
        },
      });

      return zone ? this.mapToDto(zone) : null;
    } catch (error) {
      logger.error('Failed to get zone by code', { code, error });
      throw createError('Failed to get zone', 500);
    }
  }

  /**
   * Get zones by center ID
   */
  public static async getZonesByCenterId(centerId: string): Promise<ZoneDto[]> {
    try {
      const zones = await prisma.zones.findMany({
        where: { center_id: centerId },
        include: {
          centers: true,
        },
        orderBy: { name: 'asc' },
      });

      return zones.map(zone => this.mapToDto(zone));
    } catch (error) {
      logger.error('Failed to get zones by center ID', { centerId, error });
      throw createError('Failed to get zones', 500);
    }
  }

  /**
   * Create a new zone
   */
  public static async createZone(data: CreateZoneDto): Promise<ZoneDto> {
    try {
      // Check if code already exists
      const exists = await this.existsByCode(data.code);
      if (exists) {
        throw createError('Zone with this code already exists', 400);
      }

      // Check if center exists
      const center = await prisma.centers.findUnique({
        where: { center_id: data.centerId },
      });

      if (!center) {
        throw createError('Center not found', 404);
      }

      const zone = await prisma.zones.create({
        data: {
          code: data.code,
          name: data.name,
          polygon: data.polygon,
          center_id: data.centerId,
        },
        include: {
          centers: true,
        },
      });

      logger.info('Zone created', { zoneId: zone.zone_id, code: zone.code });
      return this.mapToDto(zone);
    } catch (error) {
      logger.error('Failed to create zone', { data, error });
      if (error instanceof Error && (error.message.includes('already exists') || error.message.includes('not found'))) {
        throw error;
      }
      throw createError('Failed to create zone', 500);
    }
  }

  /**
   * Update a zone
   */
  public static async updateZone(id: string, data: UpdateZoneDto): Promise<ZoneDto> {
    try {
      // Check if zone exists
      const existing = await this.getZoneById(id);
      if (!existing) {
        throw createError('Zone not found', 404);
      }

      // Check if code is being updated and already exists
      if (data.code && data.code !== existing.code) {
        const codeExists = await this.existsByCode(data.code);
        if (codeExists) {
          throw createError('Zone with this code already exists', 400);
        }
      }

      // Check if center exists if being updated
      if (data.centerId) {
        const center = await prisma.centers.findUnique({
          where: { center_id: data.centerId },
        });

        if (!center) {
          throw createError('Center not found', 404);
        }
      }

      const updateData: any = {};
      if (data.code !== undefined) updateData.code = data.code;
      if (data.name !== undefined) updateData.name = data.name;
      if (data.polygon !== undefined) updateData.polygon = data.polygon ?? null;
      if (data.centerId !== undefined) updateData.center_id = data.centerId;

      const zone = await prisma.zones.update({
        where: { zone_id: id },
        data: updateData,
        include: {
          centers: true,
        },
      });

      logger.info('Zone updated', { zoneId: zone.zone_id });
      return this.mapToDto(zone);
    } catch (error) {
      logger.error('Failed to update zone', { id, data, error });
      if (error instanceof Error && (error.message.includes('not found') || error.message.includes('already exists'))) {
        throw error;
      }
      throw createError('Failed to update zone', 500);
    }
  }

  /**
   * Delete a zone
   */
  public static async deleteZone(id: string): Promise<void> {
    try {
      // Check if zone exists
      const existing = await this.getZoneById(id);
      if (!existing) {
        throw createError('Zone not found', 404);
      }

      await prisma.zones.delete({
        where: { zone_id: id },
      });

      logger.info('Zone deleted', { zoneId: id });
    } catch (error) {
      logger.error('Failed to delete zone', { id, error });
      if (error instanceof Error && error.message.includes('not found')) {
        throw error;
      }
      throw createError('Failed to delete zone', 500);
    }
  }

  /**
   * Check if zone exists by code
   */
  public static async existsByCode(code: string): Promise<boolean> {
    try {
      const count = await prisma.zones.count({
        where: { code },
      });
      return count > 0;
    } catch (error) {
      logger.error('Failed to check zone existence', { code, error });
      return false;
    }
  }

  /**
   * Get filterable fields for frontend
   */
  public static getFilterableFields(): string[] {
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
  public static getSortableFields(): string[] {
    return [
      'zone_id',
      'code',
      'name',
      'center_id'
    ];
  }

  /**
   * Map database entity to DTO
   */
  private static mapToDto(zone: any): ZoneDto {
    return {
      id: zone.zone_id,
      code: zone.code,
      name: zone.name,
      polygon: zone.polygon,
      centerId: zone.center_id,
      centerCode: zone.centers?.code,
      centerName: zone.centers?.name,
    };
  }
}
