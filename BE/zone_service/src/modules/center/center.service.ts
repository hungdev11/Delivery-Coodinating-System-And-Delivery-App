/**
 * Center Service
 * Static class for center business logic
 */

import { centers } from '@prisma/client';
import { prisma } from '../../common/database/prisma.client';
import { logger } from '../../common/logger/logger.service';
import { PagedData } from '../../common/types/restful';
import { createError } from '../../common/middleware/error.middleware';
import { CreateCenterDto, UpdateCenterDto, CenterDto, CenterPagingRequest } from './center.model';

export class CenterService {
  /**
   * Get all centers with pagination
   */
  public static async getAllCenters(request: CenterPagingRequest): Promise<PagedData<CenterDto>> {
    try {
      const where: any = {};

      // Apply search filter
      if (request.search) {
        where.OR = [
          { name: { contains: request.search } },
          { code: { contains: request.search } },
          { address: { contains: request.search } },
        ];
      }

      // Apply code filter
      if (request.code) {
        where.code = request.code;
      }

      // Get total count
      const totalElements = await prisma.centers.count({ where });

      // Get data
      const centers = await prisma.centers.findMany({
        where,
        skip: request.getSkip(),
        take: request.getTake(),
        orderBy: { name: 'asc' },
      });

      // Map to DTOs
      const data = centers.map(center => this.mapToDto(center));

      // Create paging
      const paging = request.createPaging<string>(totalElements);

      return new PagedData(data, paging);
    } catch (error) {
      logger.error('Failed to get all centers', { error });
      throw createError('Failed to get centers', 500);
    }
  }

  /**
   * Get center by ID
   */
  public static async getCenterById(id: string): Promise<CenterDto | null> {
    try {
      const center = await prisma.centers.findUnique({
        where: { center_id: id },
      });

      return center ? this.mapToDto(center) : null;
    } catch (error) {
      logger.error('Failed to get center by ID', { id, error });
      throw createError('Failed to get center', 500);
    }
  }

  /**
   * Get center by code
   */
  public static async getCenterByCode(code: string): Promise<CenterDto | null> {
    try {
      const center = await prisma.centers.findUnique({
        where: { code },
      });

      return center ? this.mapToDto(center) : null;
    } catch (error) {
      logger.error('Failed to get center by code', { code, error });
      throw createError('Failed to get center', 500);
    }
  }

  /**
   * Create a new center
   */
  public static async createCenter(data: CreateCenterDto): Promise<CenterDto> {
    try {
      // Check if code already exists
      const exists = await this.existsByCode(data.code);
      if (exists) {
        throw createError('Center with this code already exists', 400);
      }

      const center = await prisma.centers.create({
        data: {
          code: data.code,
          name: data.name,
          address: data.address ?? null,
          lat: data.lat ?? null,
          lon: data.lon ?? null,
          polygon: data.polygon ?? null,
        },
      });

      logger.info('Center created', { centerId: center.center_id, code: center.code });
      return this.mapToDto(center);
    } catch (error) {
      logger.error('Failed to create center', { data, error });
      if (error instanceof Error && error.message.includes('already exists')) {
        throw error;
      }
      throw createError('Failed to create center', 500);
    }
  }

  /**
   * Update a center
   */
  public static async updateCenter(id: string, data: UpdateCenterDto): Promise<CenterDto> {
    try {
      // Check if center exists
      const existing = await this.getCenterById(id);
      if (!existing) {
        throw createError('Center not found', 404);
      }

      // Check if code is being updated and already exists
      if (data.code && data.code !== existing.code) {
        const codeExists = await this.existsByCode(data.code);
        if (codeExists) {
          throw createError('Center with this code already exists', 400);
        }
      }

      const updateData: any = {};
      if (data.code !== undefined) updateData.code = data.code;
      if (data.name !== undefined) updateData.name = data.name;
      if (data.address !== undefined) updateData.address = data.address ?? null;
      if (data.lat !== undefined) updateData.lat = data.lat ?? null;
      if (data.lon !== undefined) updateData.lon = data.lon ?? null;
      if (data.polygon !== undefined) updateData.polygon = data.polygon ?? null;

      const center = await prisma.centers.update({
        where: { center_id: id },
        data: updateData,
      });

      logger.info('Center updated', { centerId: center.center_id });
      return this.mapToDto(center);
    } catch (error) {
      logger.error('Failed to update center', { id, data, error });
      if (error instanceof Error && (error.message.includes('not found') || error.message.includes('already exists'))) {
        throw error;
      }
      throw createError('Failed to update center', 500);
    }
  }

  /**
   * Delete a center
   */
  public static async deleteCenter(id: string): Promise<void> {
    try {
      // Check if center exists
      const existing = await this.getCenterById(id);
      if (!existing) {
        throw createError('Center not found', 404);
      }

      await prisma.centers.delete({
        where: { center_id: id },
      });

      logger.info('Center deleted', { centerId: id });
    } catch (error) {
      logger.error('Failed to delete center', { id, error });
      if (error instanceof Error && error.message.includes('not found')) {
        throw error;
      }
      throw createError('Failed to delete center', 500);
    }
  }

  /**
   * Check if center exists by code
   */
  public static async existsByCode(code: string): Promise<boolean> {
    try {
      const count = await prisma.centers.count({
        where: { code },
      });
      return count > 0;
    } catch (error) {
      logger.error('Failed to check center existence', { code, error });
      return false;
    }
  }

  /**
   * Map database entity to DTO
   */
  private static mapToDto(center: centers): CenterDto {
    return {
      id: center.center_id,
      code: center.code,
      name: center.name,
      address: center.address,
      lat: center.lat,
      lon: center.lon,
      polygon: center.polygon,
    };
  }
}
