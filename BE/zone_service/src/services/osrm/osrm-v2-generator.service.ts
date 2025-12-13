/**
 * OSRM V2 Generator Service
 * 
 * Generates OSRM data from database using the same logic as generate-osrm-data-v2-fast.ts
 * This service can run in production containers (no tsx required)
 */

import { PrismaClient } from '@prisma/client';
import { prisma } from '../../common/database/prisma.client';
import { logger } from '../../common/logger/logger.service';
import { writeFileSync, existsSync, mkdirSync, copyFileSync } from 'fs';
import { join } from 'path';
import { exec } from 'child_process';
import { promisify } from 'util';

const execAsync = promisify(exec);

interface ModelConfig {
  name: string;
  useRating: boolean;
  useBlocking: boolean;
  description: string;
}

const MODELS: ModelConfig[] = [
  {
    name: 'osrm-full',
    useRating: true,
    useBlocking: true,
    description: 'Full model: rating affects weight, blocking affects speed',
  },
  {
    name: 'osrm-rating-only',
    useRating: true,
    useBlocking: false,
    description: 'Rating only: user feedback affects weight',
  },
  {
    name: 'osrm-blocking-only',
    useRating: false,
    useBlocking: true,
    description: 'Blocking only: traffic affects speed',
  },
  {
    name: 'osrm-base',
    useRating: false,
    useBlocking: false,
    description: 'Base: VN motorbike profile with no modifiers',
  },
];

export class OSRMV2GeneratorService {
  private prisma: PrismaClient;
  private osrmDataPath: string;
  private rawDataPath: string;

  constructor() {
    this.prisma = prisma;
    this.osrmDataPath = process.env.OSRM_DATA_PATH || join(process.cwd(), 'osrm_data');
    this.rawDataPath = process.env.OSM_RAW_DATA_PATH || join(process.cwd(), 'raw_data');
  }

  /**
   * Generate all OSRM models from database
   */
  async generateAllModels(): Promise<{ success: boolean; models: string[]; error?: string }> {
    const startTime = Date.now();
    const successfulModels: string[] = [];

    try {
      logger.info('Starting OSRM V2 generation from database...');

      // Step 1: Fetch road network data
      const roadNetworkData = await this.fetchRoadNetworkData();

      // Step 2: Export to OSM XML (master file)
      const masterXMLPath = await this.exportToOSMXML(roadNetworkData);

      // Step 3: Clone XML to all instances and generate Lua profiles
      await this.cloneXMLToInstances(masterXMLPath);

      // Step 4: Process each OSRM model
      for (const model of MODELS) {
        try {
          await this.processOSRMInstance(model);
          successfulModels.push(model.name);
        } catch (error: any) {
          logger.error(`Failed to process ${model.name}:`, error);
          // Continue with other models
        }
      }

      const elapsed = ((Date.now() - startTime) / 1000).toFixed(1);

      if (successfulModels.length === MODELS.length) {
        logger.info(`All ${MODELS.length} OSRM models generated successfully in ${elapsed}s`);
        return { success: true, models: successfulModels };
      } else {
        logger.warn(`Only ${successfulModels.length}/${MODELS.length} models generated`);
        return {
          success: successfulModels.length > 0,
          models: successfulModels,
          error: `Only ${successfulModels.length}/${MODELS.length} models generated`,
        };
      }
    } catch (error: any) {
      logger.error('OSRM V2 generation failed:', error);
      return {
        success: false,
        models: successfulModels,
        error: error.message || String(error),
      };
    }
  }

  /**
   * Fetch all road network data from database
   */
  private async fetchRoadNetworkData() {
    logger.info('Fetching road network data from database...');

    const [roads, nodes] = await Promise.all([
      this.prisma.roads.findMany(),
      this.prisma.road_nodes.findMany(),
    ]);

    logger.info(`Found ${roads.length} roads, ${nodes.length} nodes`);

    const BATCH_SIZE = 20000;
    const segments: any[] = [];
    let offset = 0;

    while (true) {
      const batch = await this.prisma.road_segments.findMany({
        skip: offset,
        take: BATCH_SIZE,
        include: {
          from_node: true,
          to_node: true,
          user_feedback: {
            orderBy: { created_at: 'desc' },
            take: 10,
          },
          traffic_conditions: {
            where: { expires_at: { gte: new Date() } },
            orderBy: { source_timestamp: 'desc' },
            take: 1,
          },
        },
      });

      if (batch.length === 0) break;
      segments.push(...batch);
      offset += BATCH_SIZE;

      if (batch.length < BATCH_SIZE) break;
    }

    logger.info(`Loaded ${segments.length} segments`);

    const userFeedback = new Map<string, number>();
    for (const segment of segments) {
      const rating = this.calculateUserRating(segment);
      if (rating !== null) {
        userFeedback.set(segment.segment_id, rating);
      }
    }

    const trafficValue = new Map<string, number>();
    for (const segment of segments) {
      const traffic = this.calculateBlockingStatus(segment);
      trafficValue.set(segment.segment_id, traffic);
    }

    logger.info(`Calculated ratings for ${userFeedback.size} segments`);
    logger.info(`Calculated traffic for ${trafficValue.size} segments`);

    return { roads, nodes, segments, userFeedback, trafficValue };
  }

  private calculateUserRating(segment: any): number | null {
    if (!segment.user_feedback || segment.user_feedback.length === 0) {
      return null;
    }

    const scores = segment.user_feedback
      .map((f: any) => f.score)
      .filter((s: any) => typeof s === 'number' && s >= 1 && s <= 5);

    if (scores.length === 0) return null;

    const avgScore = scores.reduce((sum: number, s: number) => sum + s, 0) / scores.length;
    return avgScore / 5.0;
  }

  private calculateBlockingStatus(segment: any): number {
    const traffic = segment.traffic_conditions?.[0];
    if (!traffic) return 5.0;

    const levelMap: Record<string, number> = {
      FREE_FLOW: 5.0,
      NORMAL: 4.0,
      SLOW: 2.5,
      CONGESTED: 1.0,
      BLOCKED: 0.0,
    };

    return levelMap[traffic.traffic_level] || 5.0;
  }

  /**
   * Export to OSM XML (master file)
   */
  private async exportToOSMXML(data: any): Promise<string> {
    logger.info('Exporting road network to OSM XML...');

    const outputDir = join(this.osrmDataPath, '_shared');
    if (!existsSync(outputDir)) {
      mkdirSync(outputDir, { recursive: true });
    }

    const osmFilePath = join(outputDir, 'network.osm.xml');
    const { roads, nodes, segments, userFeedback, trafficValue } = data;

    const coordToNodeId = new Map<string, number>();
    const nodeCoords = new Map<number, { lat: number; lon: number }>();
    const coordToDbNode = new Map<string, any>();

    for (const node of nodes) {
      if (node.lat && node.lon) {
        const key = `${node.lat.toFixed(7)},${node.lon.toFixed(7)}`;
        coordToDbNode.set(key, node);
      }
    }

    const ensureNodeForCoord = (lat: number, lon: number): number => {
      const key = `${lat.toFixed(7)},${lon.toFixed(7)}`;
      if (coordToNodeId.has(key)) return coordToNodeId.get(key)!;

      const dbNode = coordToDbNode.get(key);
      let nodeId: number;

      if (dbNode && dbNode.osm_id && !isNaN(Number(dbNode.osm_id))) {
        nodeId = Number(dbNode.osm_id);
      } else {
        nodeId = 10_000_000 + coordToNodeId.size;
      }

      coordToNodeId.set(key, nodeId);
      nodeCoords.set(nodeId, { lat, lon });
      return nodeId;
    };

    const ways: Array<{
      id: number;
      nodeRefs: number[];
      tags: Array<{ k: string; v: string }>;
    }> = [];

    let wayIdCounter = 5_000_000;

    for (const segment of segments) {
      const wayId = wayIdCounter++;
      const nodeRefs: number[] = [];
      const tags: Array<{ k: string; v: string }> = [];

      let coords: Array<[number, number]> = [];
      try {
        const geom = typeof segment.geometry === 'string'
          ? JSON.parse(segment.geometry)
          : segment.geometry;
        coords = geom?.coordinates || [];
      } catch (e) {
        continue;
      }

      if (coords.length < 2) continue;

      for (const [lon, lat] of coords) {
        nodeRefs.push(ensureNodeForCoord(lat, lon));
      }

      if (nodeRefs.length < 2) continue;

      const road = roads.find((r: any) => r.road_id === segment.road_id);
      const roadType = road ? this.mapRoadTypeToOSM(road.road_type) : 'unclassified';
      tags.push({ k: 'highway', v: roadType });

      if (segment.name) {
        tags.push({ k: 'name', v: this.escapeXML(segment.name) });
      }

      const rating = userFeedback.get(segment.segment_id);
      if (rating !== undefined) {
        tags.push({ k: 'user_rating', v: rating.toFixed(3) });
      }

      const traffic = trafficValue.get(segment.segment_id);
      if (traffic !== undefined) {
        tags.push({ k: 'traffic_value', v: traffic.toFixed(1) });
      }

      if (segment.max_speed) {
        tags.push({ k: 'maxspeed', v: String(segment.max_speed) });
      }

      const isOneWay = segment.one_way || (road && road.one_way);
      if (isOneWay) {
        tags.push({ k: 'oneway', v: 'yes' });
        ways.push({ id: wayId, nodeRefs, tags });
      } else {
        ways.push({ id: wayId, nodeRefs, tags });
        const reverseWayId = wayIdCounter++;
        ways.push({ id: reverseWayId, nodeRefs: [...nodeRefs].reverse(), tags });
      }
    }

    logger.info(`Built ${ways.length} ways with ${nodeCoords.size} nodes`);

    let xml = '<?xml version="1.0" encoding="UTF-8"?>\n';
    xml += '<osm version="0.6" generator="zone-service-v2">\n';
    xml += '  <bounds minlat="10.3" minlon="106.3" maxlat="11.2" maxlon="107.0"/>\n';

    for (const [nodeId, coords] of nodeCoords.entries()) {
      xml += `  <node id="${nodeId}" lat="${coords.lat}" lon="${coords.lon}"/>\n`;
    }

    for (const way of ways) {
      xml += `  <way id="${way.id}">\n`;
      for (const nodeRef of way.nodeRefs) {
        xml += `    <nd ref="${nodeRef}"/>\n`;
      }
      for (const tag of way.tags) {
        xml += `    <tag k="${tag.k}" v="${tag.v}"/>\n`;
      }
      xml += `  </way>\n`;
    }

    xml += '</osm>\n';

    writeFileSync(osmFilePath, xml, 'utf-8');
    logger.info(`Saved to ${osmFilePath}`);

    return osmFilePath;
  }

  /**
   * Clone XML to all instances and generate Lua profiles
   */
  private async cloneXMLToInstances(masterXMLPath: string): Promise<void> {
    logger.info('Cloning XML to all instances and generating Lua profiles...');

    for (const model of MODELS) {
      const instanceDir = join(this.osrmDataPath, model.name);
      if (!existsSync(instanceDir)) {
        mkdirSync(instanceDir, { recursive: true });
      }

      const targetXMLPath = join(instanceDir, 'network.osm.xml');
      copyFileSync(masterXMLPath, targetXMLPath);

      // Generate Lua profile
      const luaScript = this.generateLuaProfile(model);
      const luaPath = join(instanceDir, 'custom_bicycle.lua');
      writeFileSync(luaPath, luaScript, 'utf-8');

      // Copy lib folder if exists
      const libSource = join(this.osrmDataPath, 'lib');
      if (existsSync(libSource)) {
        const libTarget = join(instanceDir, 'lib');
        if (!existsSync(libTarget)) {
          mkdirSync(libTarget, { recursive: true });
        }
        // Copy lib files
        const { readdirSync, statSync } = await import('fs');
        const libFiles = readdirSync(libSource);
        for (const file of libFiles) {
          const sourceFile = join(libSource, file);
          const targetFile = join(libTarget, file);
          if (statSync(sourceFile).isFile()) {
            copyFileSync(sourceFile, targetFile);
          }
        }
      }

      logger.info(`${model.name} ready (XML + Lua + lib)`);
    }
  }

  private generateLuaProfile(config: ModelConfig): string {
    const { useRating, useBlocking } = config;

    return `-- OSRM Bicycle Profile V2 (VN Motorbike Optimized)
-- Model: ${config.name}
-- Config: rating=${useRating}, blocking=${useBlocking}

api_version = 4

function setup()
  return {
    properties = {
      max_speed_for_map_matching = 40/3.6,
      use_turn_restrictions = false,
      continue_straight_at_waypoint = true,
      weight_name = 'routability',
    },
    default_mode = mode.cycling,
    default_speed = 35,
    oneway_handling = false,
    turn_penalty = 4,
    u_turn_penalty = 5,
    use_rating = ${useRating},
    use_blocking = ${useBlocking},
  }
end

function process_way(profile, way, result, relations)
  local highway = way:get_value_by_key("highway")
  if not highway then return end

  local speed_map = {
    trunk = 60, primary = 50, secondary = 45, tertiary = 40,
    unclassified = 30, residential = 30, service = 20,
    living_street = 15, track = 15, path = 12, cycleway = 25,
  }

  local speed = speed_map[highway] or 25
  local maxspeed = tonumber(way:get_value_by_key("maxspeed"))
  if maxspeed and maxspeed > 0 then
    speed = math.min(speed, maxspeed * 0.9)
  end

  if profile.use_blocking then
    local traffic_value = tonumber(way:get_value_by_key("traffic_value"))
    if traffic_value then
      speed = speed * (traffic_value / 5.0)
    end
  end

  local oneway = way:get_value_by_key("oneway")
  if oneway == "yes" or oneway == "1" then
    result.forward_mode = mode.cycling
    result.backward_mode = mode.inaccessible
  elseif oneway == "-1" then
    result.forward_mode = mode.inaccessible
    result.backward_mode = mode.cycling
  else
    result.forward_mode = mode.cycling
    result.backward_mode = mode.cycling
  end

  result.forward_speed = speed
  result.backward_speed = speed
  result.name = way:get_value_by_key("name")
  result.duration = 1
  result.weight = 1

  if profile.use_rating then
    local user_rating = tonumber(way:get_value_by_key("user_rating"))
    if user_rating then
      result.weight = result.weight * (2.0 - user_rating)
    end
  end
end

function process_node(profile, node, result, relations)
  if node:get_value_by_key("highway") == "traffic_signals" then
    result.traffic_lights = true
  end
end

function process_turn(profile, turn)
  local angle = math.abs(turn.angle)
  if angle >= 160 then
    turn.duration = turn.duration + profile.u_turn_penalty
  elseif angle >= 45 then
    turn.duration = turn.duration + profile.turn_penalty
  end
  turn.weight = turn.duration
end

return { setup = setup, process_way = process_way, process_node = process_node, process_turn = process_turn }
`;
  }

  /**
   * Process OSRM instance (build .osrm files)
   */
  private async processOSRMInstance(config: ModelConfig): Promise<void> {
    const dataDir = join(this.osrmDataPath, config.name);
    const osmFile = 'network.osm.xml';
    const profileFile = 'custom_bicycle.lua';
    const osrmFile = 'network.osrm';

    logger.info(`Processing ${config.name}...`);

    // Use Docker to run OSRM tools
    const dockerVolume = `${dataDir}:/data`;

    // Step 1: Extract
    await execAsync(
      `docker run --rm -v "${dataDir}:/data" osrm/osrm-backend:latest osrm-extract -p /data/${profileFile} /data/${osmFile}`,
      { maxBuffer: 100 * 1024 * 1024 }
    );

    // Step 2: Partition
    await execAsync(
      `docker run --rm -v "${dataDir}:/data" osrm/osrm-backend:latest osrm-partition /data/${osrmFile}`,
      { maxBuffer: 100 * 1024 * 1024 }
    );

    // Step 3: Customize
    await execAsync(
      `docker run --rm -v "${dataDir}:/data" osrm/osrm-backend:latest osrm-customize /data/${osrmFile}`,
      { maxBuffer: 100 * 1024 * 1024 }
    );

    logger.info(`${config.name} completed`);
  }

  private mapRoadTypeToOSM(roadType: string): string {
    const mapping: Record<string, string> = {
      MOTORWAY: 'motorway',
      TRUNK: 'trunk',
      PRIMARY: 'primary',
      SECONDARY: 'secondary',
      TERTIARY: 'tertiary',
      RESIDENTIAL: 'residential',
      SERVICE: 'service',
      UNCLASSIFIED: 'unclassified',
    };
    return mapping[roadType] || 'unclassified';
  }

  private escapeXML(text: string): string {
    if (!text) return '';
    return text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&apos;');
  }
}
