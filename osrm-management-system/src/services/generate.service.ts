/**
 * Generate Service
 * Handles OSRM data generation from database
 */

import { PrismaClient } from '@prisma/client';
import { writeFileSync, existsSync, mkdirSync, copyFileSync, readFileSync } from 'fs';
import { join, resolve } from 'path';
import { exec } from 'child_process';
import { promisify } from 'util';
import { logger } from '../common/logger';
import { BuildTrackerService } from './build-tracker.service';

const execAsync = promisify(exec);

interface ModelConfig {
  name: string;
  vehicleType: 'bicycle' | 'car';
  useRating: boolean;
  useBlocking: boolean;
  description: string;
}

const MODELS: ModelConfig[] = [
  // Bicycle (motorbike) models
  {
    name: 'osrm-full',
    vehicleType: 'bicycle',
    useRating: true,
    useBlocking: true,
    description: 'Full model: rating affects weight, blocking affects speed',
  },
  {
    name: 'osrm-rating-only',
    vehicleType: 'bicycle',
    useRating: true,
    useBlocking: false,
    description: 'Rating only: user feedback affects weight',
  },
  {
    name: 'osrm-blocking-only',
    vehicleType: 'bicycle',
    useRating: false,
    useBlocking: true,
    description: 'Blocking only: traffic affects speed',
  },
  {
    name: 'osrm-base',
    vehicleType: 'bicycle',
    useRating: false,
    useBlocking: false,
    description: 'Base: VN motorbike profile with no modifiers',
  },
  // Car models
  {
    name: 'osrm-car-full',
    vehicleType: 'car',
    useRating: true,
    useBlocking: true,
    description: 'Car full model: rating affects weight, blocking affects speed',
  },
  {
    name: 'osrm-car-rating-only',
    vehicleType: 'car',
    useRating: true,
    useBlocking: false,
    description: 'Car rating only: user feedback affects weight',
  },
  {
    name: 'osrm-car-blocking-only',
    vehicleType: 'car',
    useRating: false,
    useBlocking: true,
    description: 'Car blocking only: traffic affects speed',
  },
  {
    name: 'osrm-car-base',
    vehicleType: 'car',
    useRating: false,
    useBlocking: false,
    description: 'Car base: standard car profile with no modifiers',
  },
];

export interface GenerateResult {
  success: boolean;
  models?: string[];
  error?: string;
  duration?: number;
}

export class GenerateService {
  private prisma: PrismaClient;
  private osrmDataPath: string;
  private rawDataPath: string;
  private buildTracker: BuildTrackerService;

  constructor(prisma: PrismaClient) {
    this.prisma = prisma;
    this.osrmDataPath = process.env.OSRM_DATA_PATH || './osrm_data';
    this.rawDataPath = resolve(process.env.RAW_DATA_PATH || './raw_data');
    this.buildTracker = new BuildTrackerService(prisma);
  }

  /**
   * Generate all OSRM models (sync-like: sequential processing)
   */
  async generateAllModels(): Promise<GenerateResult> {
    const instanceName = 'generate-all-models';
    
    return this.buildTracker.executeSequentially(instanceName, async (buildId) => {
      return this._generateAllModels(buildId);
    });
  }

  private async _generateAllModels(buildId: string): Promise<GenerateResult> {
    const startTime = Date.now();

    try {
      logger.info(`Starting OSRM V2 generation (Build ID: ${buildId})...`);

      // Step 1: Fetch data from database
      const roadNetworkData = await this.fetchRoadNetworkData();

      // Update build with segment count
      await this.prisma.osrm_builds.update({
        where: { build_id: buildId },
        data: {
          total_segments: roadNetworkData.segments.length,
        },
      });

      // Step 2: Export to OSM XML
      const sharedXmlPath = await this.exportToOSMXML(roadNetworkData);

      // Step 3: Setup all model instances
      logger.info('Setting up model instances...');
      for (const config of MODELS) {
        await this.setupModelInstance(config, sharedXmlPath);
      }

      // Step 4: Process all OSRM instances
      logger.info('Processing OSRM instances...');
      const buildResults: Array<{ model: string; buildId: string }> = [];
      
      for (const config of MODELS) {
        const modelBuildId = await this.startModelBuild(config.name, roadNetworkData.segments.length);
        try {
          await this.processOSRMInstance(config);
          const outputPath = join(this.osrmDataPath, config.name, 'network.osrm');
          await this.buildTracker.markReady(modelBuildId, outputPath);
          buildResults.push({ model: config.name, buildId: modelBuildId });
        } catch (error: any) {
          await this.buildTracker.markFailed(modelBuildId, error.message);
          throw error;
        }
      }

      // Mark main build as READY
      await this.buildTracker.markReady(buildId, sharedXmlPath);

      const duration = Date.now() - startTime;
      const modelNames = MODELS.map(m => m.name);

      logger.info(`Generation completed in ${duration}ms`);

      return {
        success: true,
        models: modelNames,
        duration,
      };
    } catch (error: any) {
      logger.error('Generation failed', { error: error.message, buildId });
      await this.buildTracker.markFailed(buildId, error.message);
      return {
        success: false,
        error: error.message,
        duration: Date.now() - startTime,
      };
    }
  }

  private async startModelBuild(instanceName: string, totalSegments: number): Promise<string> {
    return this.buildTracker.startBuild(instanceName, totalSegments);
  }

  private async fetchRoadNetworkData(): Promise<{
    roads: any[];
    nodes: any[];
    segments: any[];
    userFeedback: Map<string, number>;
    trafficValue: Map<string, number>;
  }> {
    logger.info('Fetching road network data from database...');

    const [roads, nodes] = await Promise.all([
      this.prisma.roads.findMany(),
      this.prisma.road_nodes.findMany(),
    ]);

    logger.info(`Found ${roads.length} roads, ${nodes.length} nodes`);

    // Load segments with feedback and traffic
    const BATCH_SIZE = 20000;
    const segments: any[] = [];
    let offset = 0;

    try {
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
    } catch (error: any) {
      // Handle database connection errors
      if (error.message && error.message.includes("Can't reach database server")) {
        throw new Error(`Database connection failed: Unable to connect to database server. Please check database configuration and ensure the server is running.`);
      }
      throw error;
    }

    logger.info(`Loaded ${segments.length} segments`);

    // Calculate ratings and traffic
    const userFeedback = new Map<string, number>();
    const trafficValue = new Map<string, number>();

    for (const segment of segments) {
      const rating = this.calculateUserRating(segment);
      if (rating !== null) {
        userFeedback.set(segment.segment_id, rating);
      }
      const traffic = this.calculateBlockingStatus(segment);
      trafficValue.set(segment.segment_id, traffic);
    }

    return { roads, nodes, segments, userFeedback, trafficValue };
  }

  private calculateUserRating(segment: any): number | null {
    if (!segment.user_feedback || segment.user_feedback.length === 0) {
      return null;
    }

    // Calculate rating from weight_adjustment or severity
    // If weight_adjustment exists, use it to infer rating
    // Otherwise, use severity as a proxy (MINOR=good, CRITICAL=bad)
    const ratings: number[] = [];

    for (const feedback of segment.user_feedback) {
      // If weight_adjustment exists, convert to rating (negative = bad, positive = good)
      if (feedback.weight_adjustment !== null && feedback.weight_adjustment !== undefined) {
        // weight_adjustment > 0 means road is worse (higher weight = avoid)
        // Convert to 0-1 scale: -1.0 (very bad) -> 0.0, 0.0 (neutral) -> 0.5, 1.0 (very good) -> 1.0
        const normalized = Math.max(0, Math.min(1, 0.5 - feedback.weight_adjustment * 0.5));
        ratings.push(normalized);
      } else if (feedback.severity) {
        // Use severity as proxy: MINOR=good (0.8), MODERATE=ok (0.6), MAJOR=bad (0.4), CRITICAL=very bad (0.2)
        const severityMap: Record<string, number> = {
          MINOR: 0.8,
          MODERATE: 0.6,
          MAJOR: 0.4,
          CRITICAL: 0.2,
        };
        const rating = severityMap[feedback.severity] || 0.5;
        ratings.push(rating);
      }
    }

    if (ratings.length === 0) return null;

    const avgRating = ratings.reduce((sum: number, r: number) => sum + r, 0) / ratings.length;
    return avgRating;
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

  private async exportToOSMXML(data: {
    roads: any[];
    nodes: any[];
    segments: any[];
    userFeedback: Map<string, number>;
    trafficValue: Map<string, number>;
  }): Promise<string> {
    logger.info('Exporting road network to OSM XML...');

    const outputDir = join(this.osrmDataPath, '_shared');
    if (!existsSync(outputDir)) {
      mkdirSync(outputDir, { recursive: true });
    }

    const osmFilePath = join(outputDir, 'network.osm.xml');

    const { roads, nodes, segments, userFeedback, trafficValue } = data;

    // Build coordinate -> node mapping
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

    // Build ways
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

      const road = roads.find((r) => r.road_id === segment.road_id);
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

    // Write XML
    let xml = '<?xml version="1.0" encoding="UTF-8"?>\n';
    xml += '<osm version="0.6" generator="osrm-management-system">\n';
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
    logger.info(`Saved OSM XML to ${osmFilePath}`);

    return osmFilePath;
  }

  private generateLuaProfile(config: ModelConfig): string {
    if (config.vehicleType === 'car') {
      return this.generateCarLuaProfile(config);
    } else {
      return this.generateBicycleLuaProfile(config);
    }
  }

  private generateBicycleLuaProfile(config: ModelConfig): string {
    const { useRating, useBlocking } = config;
    
    // Read bicycle.lua base from zone_service
    const bicycleLuaPath = join(this.rawDataPath, 'osrm-logic', 'bicycle.lua');
    if (!existsSync(bicycleLuaPath)) {
      logger.warn(`bicycle.lua not found at ${bicycleLuaPath}, using fallback`);
      return this.generateBicycleLuaProfileFallback(config);
    }
    
    let bicycleLua = readFileSync(bicycleLuaPath, 'utf-8');
    
    // Build hardcode profile: remove TrafficSignal, add modifiers
    
    // 1. Remove TrafficSignal require
    bicycleLua = bicycleLua.replace(/TrafficSignal\s*=\s*require\(["']lib\/traffic_signal["']\)\s*\n/g, '');
    
    // 2. Replace TrafficSignal.get_value(node) with simple highway check
    bicycleLua = bicycleLua.replace(
      /result\.traffic_lights\s*=\s*TrafficSignal\.get_value\(node\)/g,
      'result.traffic_lights = highway == "traffic_signals"'
    );
    
    // 3. Add modifiers to setup() - find avoid Set closing and inject before final }
    bicycleLua = bicycleLua.replace(
      /(avoid\s*=\s*Set\s*\{[^}]*\})(\n\s*\})/,
      `$1,\n\n    use_rating = ${useRating},\n    use_blocking = ${useBlocking}$2`
    );
    
    // 4. Inject dynamic modifiers into process_way() - after WayHandlers.run
    const modifiersCode = `
  
  -- Apply dynamic modifiers AFTER WayHandlers.run()
  if profile.use_blocking then
    local traffic_value = tonumber(way:get_value_by_key("traffic_value"))
    if traffic_value then
      local traffic_multiplier = traffic_value / 5.0
      if result.forward_speed and result.forward_speed > 0 then
        result.forward_speed = result.forward_speed * traffic_multiplier
      end
      if result.backward_speed and result.backward_speed > 0 then
        result.backward_speed = result.backward_speed * traffic_multiplier
      end
    end
  end

  if profile.use_rating then
    local user_rating = tonumber(way:get_value_by_key("user_rating"))
    if user_rating and result.weight and result.weight > 0 then
      local rating_multiplier = 2.0 - user_rating
      result.weight = result.weight * rating_multiplier
    end
  end
`;
    bicycleLua = bicycleLua.replace(
      /(WayHandlers\.run\(profile, way, result, data, handlers\)\s*\n)(end)/,
      `$1${modifiersCode}$2`
    );
    
    // Add header
    const header = `-- OSRM Bicycle Profile with Dynamic Modifiers
-- Generated by OSRM Management System
-- Model: ${config.name}
-- Config: rating=${useRating}, blocking=${useBlocking}
-- Base: ${bicycleLuaPath}

`;
    
    return header + bicycleLua;
  }
  
  private generateBicycleLuaProfileFallback(config: ModelConfig): string {
    const { useRating, useBlocking } = config;
    logger.warn('Using fallback bicycle profile (hardcoded)');
    return `-- OSRM Bicycle Profile V2 (VN Motorbike Optimized) - FALLBACK
-- Generated by OSRM Management System
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
  if not highway then
    return
  end

  local speed_map = {
    trunk = 60,
    primary = 50,
    secondary = 45,
    tertiary = 40,
    unclassified = 30,
    residential = 30,
    service = 20,
    living_street = 15,
    track = 15,
    path = 12,
    cycleway = 25,
  }

  local speed = speed_map[highway] or 25
  
  local maxspeed = tonumber(way:get_value_by_key("maxspeed"))
  if maxspeed and maxspeed > 0 then
    speed = math.min(speed, maxspeed * 0.9)
  end

  if profile.use_blocking then
    local traffic_value = tonumber(way:get_value_by_key("traffic_value"))
    if traffic_value then
      local traffic_multiplier = traffic_value / 5.0
      speed = speed * traffic_multiplier
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

  local name = way:get_value_by_key("name")
  if name then
    result.name = name
  end

  result.duration = 1
  result.weight = 1

  if profile.use_rating then
    local user_rating = tonumber(way:get_value_by_key("user_rating"))
    if user_rating then
      local rating_multiplier = 2.0 - user_rating
      result.weight = result.weight * rating_multiplier
    end
  end
end

function process_node(profile, node, result, relations)
  local highway = node:get_value_by_key("highway")
  if highway == "traffic_signals" then
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

return {
  setup = setup,
  process_way = process_way,
  process_node = process_node,
  process_turn = process_turn,
}
`;
  }

  private generateCarLuaProfile(config: ModelConfig): string {
    const { useRating, useBlocking } = config;
    
    // Read car.lua base from zone_service
    const carLuaPath = join(this.rawDataPath, 'osrm-logic', 'car.lua');
    if (!existsSync(carLuaPath)) {
      logger.warn(`car.lua not found at ${carLuaPath}, using fallback`);
      return this.generateCarLuaProfileFallback(config);
    }
    
    let carLua = readFileSync(carLuaPath, 'utf-8');
    
    // Build hardcode profile: remove Obstacles, add modifiers, fix process_node and process_turn
    
    // 1. Remove Obstacles require
    carLua = carLua.replace(/Obstacles\s*=\s*require\(["']lib\/obstacles["']\)\s*\n/g, '');
    
    // 2. Add modifiers to setup() - find access_turn_classification closing and inject before final }
    carLua = carLua.replace(
      /(access_turn_classification\s*=\s*\{\s*\n\s*\})(\n\s*\})/,
      `$1,\n\n    use_rating = ${useRating},\n    use_blocking = ${useBlocking}$2`
    );
    
    // 3. Fix process_node - remove obstacle_map calls and Obstacles.process_node
    // Replace obstacle_map:add with result.barrier = true
    carLua = carLua.replace(
      /obstacle_map:add\(node, Obstacle\.new\(obstacle_type\.barrier\)\)/g,
      'result.barrier = true'
    );
    
    // Remove Obstacles.process_node call
    carLua = carLua.replace(/\n\s*Obstacles\.process_node\(profile, node\)\s*\n/g, '\n');
    
    // 4. Fix process_way - inject modifiers after Relations.process_way_refs
    const modifiersCode = `
  
  -- Apply dynamic modifiers AFTER WayHandlers.run()
  if profile.use_blocking then
    local traffic_value = tonumber(way:get_value_by_key("traffic_value"))
    if traffic_value then
      local traffic_multiplier = traffic_value / 5.0
      if result.forward_speed and result.forward_speed > 0 then
        result.forward_speed = result.forward_speed * traffic_multiplier
      end
      if result.backward_speed and result.backward_speed > 0 then
        result.backward_speed = result.backward_speed * traffic_multiplier
      end
    end
  end

  if profile.use_rating then
    local user_rating = tonumber(way:get_value_by_key("user_rating"))
    if user_rating and result.weight and result.weight > 0 then
      local rating_multiplier = 2.0 - user_rating
      result.weight = result.weight * rating_multiplier
    end
  end
`;
    carLua = carLua.replace(
      /(if profile\.cardinal_directions then\s*\n\s*Relations\.process_way_refs\(way, relations, result\)\s*\n\s*end\s*\n)(end)/,
      `$1${modifiersCode}$2`
    );
    
    // 5. Fix process_turn - remove obstacle_map loop
    // Remove the entire for loop with obstacle_map:get (from "for _, obs" to matching "end")
    // Match multiline pattern: from "for _, obs" to the "end" that closes the for loop
    carLua = carLua.replace(
      /for\s+_,\s+obs\s+in\s+pairs\(obstacle_map:get\(turn\.from, turn\.via\)\)\s+do[\s\S]*?::skip::[\s\S]*?end\s*\n/g,
      ''
    );
    
    // Replace Obstacles.entering_by_minor_road with false
    carLua = carLua.replace(/Obstacles\.entering_by_minor_road\(turn\)/g, 'false');
    
    // Add header
    const header = `-- Car Profile with Dynamic Modifiers
-- Generated by OSRM Management System
-- Model: ${config.name}
-- Config: rating=${useRating}, blocking=${useBlocking}
-- Base: ${carLuaPath}

`;
    
    return header + carLua;
  }
  
  private generateCarLuaProfileFallback(config: ModelConfig): string {
    const { useRating, useBlocking } = config;
    logger.warn('Using fallback car profile (hardcoded)');
    return `-- Car profile with dynamic modifiers - FALLBACK
-- Generated by OSRM Management System
-- Model: ${config.name}
-- Config: rating=${useRating}, blocking=${useBlocking}

api_version = 4

Set = require('lib/set')
Sequence = require('lib/sequence')
Handlers = require("lib/way_handlers")
Relations = require("lib/relations")
find_access_tag = require("lib/access").find_access_tag
limit = require("lib/maxspeed").limit
Utils = require("lib/utils")
Measure = require("lib/measure")

function setup()
  return {
    properties = {
      max_speed_for_map_matching      = 180/3.6,
      weight_name                     = 'routability',
      process_call_tagless_node      = false,
      u_turn_penalty                 = 50,
      continue_straight_at_waypoint  = true,
      use_turn_restrictions          = true,
      left_hand_driving              = false,
    },

    default_mode              = mode.driving,
    default_speed             = 10,
    oneway_handling           = true,
    side_road_multiplier      = 0.8,
    turn_penalty              = 7.5,
    speed_reduction           = 0.8,
    turn_bias                 = 1.075,
    cardinal_directions       = false,

    vehicle_height = 2.0,
    vehicle_width = 1.9,
    vehicle_length = 4.8,
    vehicle_weight = 2000,

    suffix_list = {
      'N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW', 'North', 'South', 'West', 'East', 'Nor', 'Sou', 'We', 'Ea'
    },

    barrier_whitelist = Set {
      'cattle_grid', 'border_control', 'toll_booth', 'sally_port', 'gate', 'lift_gate', 'no', 'entrance', 'height_restrictor', 'arch'
    },

    access_tag_whitelist = Set {
      'yes', 'motorcar', 'motor_vehicle', 'vehicle', 'permissive', 'designated', 'hov'
    },

    access_tag_blacklist = Set {
      'no', 'agricultural', 'forestry', 'emergency', 'psv', 'customers', 'private', 'delivery', 'destination'
    },

    service_access_tag_blacklist = Set { 'private' },

    restricted_access_tag_list = Set {
      'private', 'delivery', 'destination', 'customers',
    },

    access_tags_hierarchy = Sequence {
      'motorcar', 'motor_vehicle', 'vehicle', 'access'
    },

    service_tag_forbidden = Set { 'emergency_access' },

    restrictions = Sequence {
      'motorcar', 'motor_vehicle', 'vehicle'
    },

    classes = Sequence {
        'toll', 'motorway', 'ferry', 'restricted', 'tunnel'
    },

    excludable = Sequence {
        Set {'toll'}, Set {'motorway'}, Set {'ferry'}
    },

    avoid = Set {
      'area', 'reversible', 'impassable', 'hov_lanes', 'steps', 'construction', 'proposed'
    },

    speeds = Sequence {
      highway = {
        motorway        = 90,
        motorway_link   = 45,
        trunk           = 85,
        trunk_link      = 40,
        primary         = 65,
        primary_link    = 30,
        secondary       = 55,
        secondary_link  = 25,
        tertiary        = 40,
        tertiary_link   = 20,
        unclassified    = 25,
        residential     = 25,
        living_street   = 10,
        service         = 15
      }
    },

    service_penalties = {
      alley = 0.5, parking = 0.5, parking_aisle = 0.5, driveway = 0.5, ["drive-through"] = 0.5, ["drive-thru"] = 0.5
    },

    restricted_highway_whitelist = Set {
      'motorway', 'motorway_link', 'trunk', 'trunk_link', 'primary', 'primary_link',
      'secondary', 'secondary_link', 'tertiary', 'tertiary_link', 'residential',
      'living_street', 'unclassified', 'service'
    },

    construction_whitelist = Set { 'no', 'widening', 'minor' },

    route_speeds = { ferry = 5, shuttle_train = 10 },
    bridge_speeds = { movable = 5 },

    surface_speeds = {
      asphalt = nil, concrete = nil, ["concrete:plates"] = nil, ["concrete:lanes"] = nil, paved = nil,
      cement = 80, compacted = 80, fine_gravel = 80,
      paving_stones = 60, metal = 60, bricks = 60,
      grass = 40, wood = 40, sett = 40, grass_paver = 40, gravel = 40, unpaved = 40, ground = 40, dirt = 40, pebblestone = 40, tartan = 40,
      cobblestone = 30, clay = 30,
      earth = 20, stone = 20, rocky = 20, sand = 20,
      mud = 10
    },

    tracktype_speeds = {
      grade1 = 60, grade2 = 40, grade3 = 30, grade4 = 25, grade5 = 20
    },

    smoothness_speeds = {
      intermediate = 80, bad = 40, very_bad = 20, horrible = 10, very_horrible = 5, impassable = 0
    },

    maxspeed_table_default = {
      urban = 50, rural = 90, trunk = 110, motorway = 130
    },

    maxspeed_table = {
      ["at:rural"] = 100, ["at:trunk"] = 100, ["be:motorway"] = 120, ["be-bru:rural"] = 70, ["be-bru:urban"] = 30,
      ["be-vlg:rural"] = 70, ["bg:motorway"] = 140, ["by:urban"] = 60, ["by:motorway"] = 110, ["ca-on:rural"] = 80,
      ["ch:rural"] = 80, ["ch:trunk"] = 100, ["ch:motorway"] = 120, ["cz:trunk"] = 0, ["cz:motorway"] = 0,
      ["de:living_street"] = 7, ["de:rural"] = 100, ["de:motorway"] = 0, ["dk:rural"] = 80, ["es:trunk"] = 90,
      ["fr:rural"] = 80, ["gb:nsl_single"] = (60*1609)/1000, ["gb:nsl_dual"] = (70*1609)/1000, ["gb:motorway"] = (70*1609)/1000,
      ["nl:rural"] = 80, ["nl:trunk"] = 100, ['no:rural'] = 80, ['no:motorway'] = 110,
      ['ph:urban'] = 40, ['ph:rural'] = 80, ['ph:motorway'] = 100,
      ['pl:rural'] = 100, ['pl:expressway'] = 120, ['pl:motorway'] = 140,
      ["ro:trunk"] = 100, ["ru:living_street"] = 20, ["ru:urban"] = 60, ["ru:motorway"] = 110,
      ["uk:nsl_single"] = (60*1609)/1000, ["uk:nsl_dual"] = (70*1609)/1000, ["uk:motorway"] = (70*1609)/1000,
      ['za:urban'] = 60, ['za:rural'] = 100, ["none"] = 140
    },

    relation_types = Sequence { "route" },
    highway_turn_classification = {},
    access_turn_classification = {},

    use_rating = ${useRating},
    use_blocking = ${useBlocking},
  }
end

function process_node(profile, node, result, relations)
  -- Process node for car profile
  -- Note: Obstacles module is not available in OSRM backend, so we skip obstacle handling
  -- Barrier and access restrictions are handled in process_way instead
end

-- Apply dynamic modifiers after WayHandlers.run()
local function apply_dynamic_modifiers(result, way, profile)
  if profile.use_blocking then
    local traffic_value = tonumber(way:get_value_by_key("traffic_value"))
    if traffic_value then
      local traffic_multiplier = traffic_value / 5.0
      if result.forward_speed and result.forward_speed > 0 then
        result.forward_speed = result.forward_speed * traffic_multiplier
      end
      if result.backward_speed and result.backward_speed > 0 then
        result.backward_speed = result.backward_speed * traffic_multiplier
      end
    end
  end

  if profile.use_rating then
    local user_rating = tonumber(way:get_value_by_key("user_rating"))
    if user_rating then
      local rating_multiplier = 2.0 - user_rating
      if result.weight and result.weight > 0 then
        result.weight = result.weight * rating_multiplier
      end
    end
  end
end

function process_way(profile, way, result, relations)
  local data = {
    highway = way:get_value_by_key('highway'),
    bridge = way:get_value_by_key('bridge'),
    route = way:get_value_by_key('route')
  }

  if (not data.highway or data.highway == '') and (not data.route or data.route == '') then
    return
  end

  handlers = Sequence {
    WayHandlers.default_mode,
    WayHandlers.blocked_ways,
    WayHandlers.avoid_ways,
    WayHandlers.handle_height,
    WayHandlers.handle_width,
    WayHandlers.handle_length,
    WayHandlers.handle_weight,
    WayHandlers.access,
    WayHandlers.oneway,
    WayHandlers.destinations,
    WayHandlers.ferries,
    WayHandlers.movables,
    WayHandlers.service,
    WayHandlers.hov,
    WayHandlers.speed,
    WayHandlers.maxspeed,
    WayHandlers.surface,
    WayHandlers.penalties,
    WayHandlers.classes,
    WayHandlers.turn_lanes,
    WayHandlers.classification,
    WayHandlers.roundabouts,
    WayHandlers.startpoint,
    WayHandlers.driving_side,
    WayHandlers.names,
    WayHandlers.weights,
    WayHandlers.way_classification_for_turn
  }

  WayHandlers.run(profile, way, result, data, handlers, relations)

  if profile.cardinal_directions then
      Relations.process_way_refs(way, relations, result)
  end

  -- Apply dynamic modifiers AFTER WayHandlers.run()
  apply_dynamic_modifiers(result, way, profile)
end

function process_turn(profile, turn)
  local turn_penalty = profile.turn_penalty
  local turn_bias = turn.is_left_hand_driving and 1. / profile.turn_bias or profile.turn_bias

  -- Note: Obstacles module is not available, so we skip obstacle-based turn penalties

  if turn.number_of_roads > 2 or turn.source_mode ~= turn.target_mode or turn.is_u_turn then
    if turn.angle >= 0 then
      turn.duration = turn.duration + turn_penalty / (1 + math.exp( -((13 / turn_bias) *  turn.angle/180 - 6.5*turn_bias)))
    else
      turn.duration = turn.duration + turn_penalty / (1 + math.exp( -((13 * turn_bias) * -turn.angle/180 - 6.5/turn_bias)))
    end

    if turn.is_u_turn then
      turn.duration = turn.duration + profile.properties.u_turn_penalty
    end
  end

  if profile.properties.weight_name == 'distance' then
     turn.weight = 0
  else
     turn.weight = turn.duration
  end

  if profile.properties.weight_name == 'routability' then
      if not turn.source_restricted and turn.target_restricted then
          turn.weight = constants.max_turn_weight
      end
  end
end

return {
  setup = setup,
  process_way = process_way,
  process_node = process_node,
  process_turn = process_turn
}
`;
  }

  private async setupModelInstance(config: ModelConfig, sharedXmlPath: string): Promise<void> {
    const instanceDir = join(this.osrmDataPath, config.name);
    if (!existsSync(instanceDir)) {
      mkdirSync(instanceDir, { recursive: true });
    }

    const instanceXmlPath = join(instanceDir, 'network.osm.xml');
    copyFileSync(sharedXmlPath, instanceXmlPath);

    const luaScript = this.generateLuaProfile(config);
    const profileFileName = config.vehicleType === 'car' ? 'custom_car.lua' : 'custom_bicycle.lua';
    const luaPath = join(instanceDir, profileFileName);
    writeFileSync(luaPath, luaScript, 'utf-8');

    logger.info(`Setup ${config.name} (${config.vehicleType})`);
  }

  private async processOSRMInstance(config: ModelConfig): Promise<void> {
    logger.info(`Processing ${config.name} (${config.vehicleType})...`);

    // Use absolute path for Docker volume mount
    const dataDir = resolve(this.osrmDataPath, config.name);
    const osmFile = 'network.osm.xml';
    const profileFile = config.vehicleType === 'car' ? 'custom_car.lua' : 'custom_bicycle.lua';
    const osrmFile = 'network.osrm';

    try {
      const dockerImage = process.env.OSRM_DOCKER_IMAGE || 'osrm/osrm-backend:latest';

      // Extract
      await execAsync(
        `docker run --rm -v "${dataDir}:/data" ${dockerImage} osrm-extract -p /data/${profileFile} /data/${osmFile}`,
        { maxBuffer: 100 * 1024 * 1024 }
      );

      // Partition
      await execAsync(
        `docker run --rm -v "${dataDir}:/data" ${dockerImage} osrm-partition /data/${osrmFile}`,
        { maxBuffer: 100 * 1024 * 1024 }
      );

      // Customize
      await execAsync(
        `docker run --rm -v "${dataDir}:/data" ${dockerImage} osrm-customize /data/${osrmFile}`,
        { maxBuffer: 100 * 1024 * 1024 }
      );

      logger.info(`${config.name} completed`);
    } catch (error: any) {
      logger.error(`${config.name} failed: ${error.message}`);
      throw error;
    }
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
