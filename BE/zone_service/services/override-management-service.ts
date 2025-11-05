/**
 * Override Management Service
 * 
 * CRUD operations for road overrides and POI priorities.
 * Triggers OSRM rebuild when needed.
 */

import { PrismaClient, BlockLevel } from '@prisma/client';
import { exec } from 'child_process';
import { promisify } from 'util';

const execAsync = promisify(exec);
const prisma = new PrismaClient();

export interface CreateRoadOverrideInput {
  segment_id?: string;
  osm_way_id?: bigint | string;
  block_level?: BlockLevel;
  delta?: number;
  point_score?: number;
  recommend_enabled?: boolean;
  soft_penalty_factor?: number;
  min_penalty_factor?: number;
  updated_by?: string;
}

export interface UpdateRoadOverrideInput extends CreateRoadOverrideInput {
  override_id: string;
}

export interface CreatePOIPriorityInput {
  poi_id: string;
  poi_name?: string;
  poi_type?: string;
  priority: number;
  time_windows?: any;
  latitude?: number;
  longitude?: number;
  updated_by?: string;
}

export interface UpdatePOIPriorityInput extends Partial<CreatePOIPriorityInput> {
  priority_id: string;
}

export class OverrideManagementService {
  /**
   * Create or update a road override
   */
  async upsertRoadOverride(input: CreateRoadOverrideInput): Promise<any> {
    if (!input.segment_id && !input.osm_way_id) {
      throw new Error('Either segment_id or osm_way_id must be provided');
    }

    // Check if override already exists
    const existing = await prisma.road_overrides.findFirst({
      where: {
        OR: [
          input.segment_id ? { segment_id: input.segment_id } : {},
          input.osm_way_id ? { osm_way_id: typeof input.osm_way_id === 'string' ? BigInt(input.osm_way_id) : input.osm_way_id } : {},
        ],
      },
    });

    const data = {
      segment_id: input.segment_id,
      osm_way_id: input.osm_way_id ? (typeof input.osm_way_id === 'string' ? BigInt(input.osm_way_id) : input.osm_way_id) : undefined,
      block_level: input.block_level || 'none',
      delta: input.delta,
      point_score: input.point_score,
      recommend_enabled: input.recommend_enabled !== undefined ? input.recommend_enabled : true,
      soft_penalty_factor: input.soft_penalty_factor,
      min_penalty_factor: input.min_penalty_factor,
      updated_by: input.updated_by,
    };

    if (existing) {
      return prisma.road_overrides.update({
        where: { override_id: existing.override_id },
        data,
      });
    } else {
      return prisma.road_overrides.create({
        data,
      });
    }
  }

  /**
   * Get road override by segment_id or osm_way_id
   */
  async getRoadOverride(segment_id?: string, osm_way_id?: bigint | string): Promise<any> {
    if (!segment_id && !osm_way_id) {
      throw new Error('Either segment_id or osm_way_id must be provided');
    }

    return prisma.road_overrides.findFirst({
      where: {
        OR: [
          segment_id ? { segment_id } : {},
          osm_way_id ? { osm_way_id: typeof osm_way_id === 'string' ? BigInt(osm_way_id) : osm_way_id } : {},
        ],
      },
    });
  }

  /**
   * List all road overrides with filters
   */
  async listRoadOverrides(filters?: {
    block_level?: BlockLevel;
    recommend_enabled?: boolean;
    limit?: number;
    offset?: number;
  }): Promise<any[]> {
    const { block_level, recommend_enabled, limit = 100, offset = 0 } = filters || {};

    return prisma.road_overrides.findMany({
      where: {
        ...(block_level && { block_level }),
        ...(recommend_enabled !== undefined && { recommend_enabled }),
      },
      take: limit,
      skip: offset,
      orderBy: { updated_at: 'desc' },
    });
  }

  /**
   * Delete road override
   */
  async deleteRoadOverride(override_id: string): Promise<void> {
    await prisma.road_overrides.delete({
      where: { override_id },
    });
  }

  /**
   * Batch update multiple road overrides
   */
  async batchUpdateRoadOverrides(
    segment_ids: string[],
    updates: Partial<CreateRoadOverrideInput>
  ): Promise<number> {
    const result = await prisma.road_overrides.updateMany({
      where: {
        segment_id: {
          in: segment_ids,
        },
      },
      data: {
        ...(updates.block_level && { block_level: updates.block_level }),
        ...(updates.delta !== undefined && { delta: updates.delta }),
        ...(updates.point_score !== undefined && { point_score: updates.point_score }),
        ...(updates.recommend_enabled !== undefined && { recommend_enabled: updates.recommend_enabled }),
        ...(updates.soft_penalty_factor !== undefined && { soft_penalty_factor: updates.soft_penalty_factor }),
        ...(updates.min_penalty_factor !== undefined && { min_penalty_factor: updates.min_penalty_factor }),
        ...(updates.updated_by && { updated_by: updates.updated_by }),
      },
    });

    return result.count;
  }

  /**
   * Create POI priority
   */
  async createPOIPriority(input: CreatePOIPriorityInput): Promise<any> {
    return prisma.poi_priorities.create({
      data: {
        poi_id: input.poi_id,
        poi_name: input.poi_name,
        poi_type: input.poi_type,
        priority: input.priority,
        time_windows: input.time_windows,
        latitude: input.latitude,
        longitude: input.longitude,
        updated_by: input.updated_by,
      },
    });
  }

  /**
   * Update POI priority
   */
  async updatePOIPriority(input: UpdatePOIPriorityInput): Promise<any> {
    const { priority_id, ...data } = input;
    
    return prisma.poi_priorities.update({
      where: { priority_id },
      data,
    });
  }

  /**
   * Get POI priority by poi_id
   */
  async getPOIPriority(poi_id: string): Promise<any> {
    return prisma.poi_priorities.findUnique({
      where: { poi_id },
    });
  }

  /**
   * List POI priorities with filters
   */
  async listPOIPriorities(filters?: {
    priority?: number;
    poi_type?: string;
    limit?: number;
    offset?: number;
  }): Promise<any[]> {
    const { priority, poi_type, limit = 100, offset = 0 } = filters || {};

    return prisma.poi_priorities.findMany({
      where: {
        ...(priority && { priority }),
        ...(poi_type && { poi_type }),
      },
      take: limit,
      skip: offset,
      orderBy: { priority: 'asc' },
    });
  }

  /**
   * Delete POI priority
   */
  async deletePOIPriority(priority_id: string): Promise<void> {
    await prisma.poi_priorities.delete({
      where: { priority_id },
    });
  }

  /**
   * Trigger OSRM rebuild (motorbike instance only)
   */
  async triggerRebuild(instanceName: string = 'osrm-instance-2'): Promise<{ success: boolean; message: string }> {
    try {
      console.log(`🔄 Triggering OSRM rebuild for ${instanceName}...`);
      
      // Run the generator script
      const { stdout, stderr } = await execAsync(
        'npm run generate:osrm',
        {
          cwd: process.cwd(),
          maxBuffer: 100 * 1024 * 1024, // 100MB buffer
        }
      );

      console.log('✅ OSRM rebuild completed');
      if (stdout) console.log(stdout);
      if (stderr) console.error(stderr);

      return {
        success: true,
        message: 'OSRM rebuild triggered successfully',
      };
    } catch (error: any) {
      console.error('❌ OSRM rebuild failed:', error);
      return {
        success: false,
        message: `OSRM rebuild failed: ${error.message}`,
      };
    }
  }

  /**
   * Get rebuild statistics
   */
  async getRebuildStats(): Promise<any> {
    const [overridesCount, poisCount, blockedCount, recommendDisabledCount] = await Promise.all([
      prisma.road_overrides.count(),
      prisma.poi_priorities.count(),
      prisma.road_overrides.count({
        where: {
          block_level: {
            in: ['soft', 'min', 'hard'],
          },
        },
      }),
      prisma.road_overrides.count({
        where: { recommend_enabled: false },
      }),
    ]);

    return {
      total_overrides: overridesCount,
      total_poi_priorities: poisCount,
      blocked_roads: blockedCount,
      recommendation_disabled: recommendDisabledCount,
    };
  }

  /**
   * Cleanup and disconnect
   */
  async disconnect() {
    await prisma.$disconnect();
  }
}

export const overrideManagementService = new OverrideManagementService();
