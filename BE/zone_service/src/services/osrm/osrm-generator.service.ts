/**
 * OSRM Data Generator Service
 * Generates custom OSRM routing data with dynamic weights
 * Supports dual-instance deployment for zero-downtime updates
 */

import { PrismaClient, OsrmBuildStatus } from '@prisma/client';
import { prisma } from '../../common/database/prisma.client';
import { logger } from '../../common/logger/logger.service';
import { writeFileSync, existsSync, mkdirSync } from 'fs';
import { join } from 'path';
import { exec } from 'child_process';
import { promisify } from 'util';

const execAsync = promisify(exec);

export interface OSRMBuildConfig {
  instanceName: 'osrm-instance-1' | 'osrm-instance-2';
  outputDir: string;
  luaProfilePath?: string;
}

export class OSRMGeneratorService {
  private prisma: PrismaClient;
  private currentInstance: 'osrm-instance-1' | 'osrm-instance-2' = 'osrm-instance-1';

  constructor() {
    this.prisma = prisma;
  }

  /**
   * Generate OSRM data for the next instance
   */
  async generateOSRMData(): Promise<string> {
    logger.info('Starting OSRM data generation...');

    // Determine which instance to build
    const nextInstance = this.getNextInstance();
    const buildId = await this.createBuildRecord(nextInstance);

    try {
      // Step 1: Export road data to OSM XML
      await this.updateBuildStatus(buildId, 'BUILDING', 'Exporting road data');
      const osmFilePath = await this.exportToOSM(nextInstance);

      // Step 2: Generate Lua profile with custom weights
      await this.updateBuildStatus(buildId, 'BUILDING', 'Generating Lua profile');
      const luaProfile = await this.generateLuaProfile(nextInstance);

      // Step 3: Run OSRM processing
      await this.updateBuildStatus(buildId, 'BUILDING', 'Processing with OSRM');
      await this.runOSRMProcess(osmFilePath, luaProfile, nextInstance);

      // Step 4: Mark as ready
      await this.updateBuildStatus(buildId, 'READY', 'Build completed');

      logger.info(`OSRM data generation completed for ${nextInstance}`);
      return buildId;
    } catch (error) {
      await this.updateBuildStatus(buildId, 'FAILED', `Error: ${error}`);
      logger.error('OSRM generation failed:', error);
      throw error;
    }
  }

  /**
   * Export road network to OSM XML format with custom weights
   */
  private async exportToOSM(instanceName: string): Promise<string> {
    logger.info('Exporting road network to OSM format...');

    const outputDir = join(process.cwd(), 'osrm_data', instanceName);
    if (!existsSync(outputDir)) {
      mkdirSync(outputDir, { recursive: true });
    }

    const osmFilePath = join(outputDir, 'network.osm.xml');

    // Fetch all roads, nodes, and segments
    const [roads, nodes, segments] = await Promise.all([
      this.prisma.roads.findMany(),
      this.prisma.road_nodes.findMany(),
      this.prisma.road_segments.findMany({
        include: {
          from_node: true,
          to_node: true,
        },
      }),
    ]);

    // Generate OSM XML
    const xml = this.generateOSMXML(roads, nodes, segments);

    // Write to file
    writeFileSync(osmFilePath, xml, 'utf-8');

    logger.info(`Exported ${nodes.length} nodes and ${roads.length} ways to ${osmFilePath}`);
    return osmFilePath;
  }

  /**
   * Generate OSM XML from database data
   */
  private generateOSMXML(roads: any[], nodes: any[], segments: any[]): string {
    let xml = '<?xml version="1.0" encoding="UTF-8"?>\n';
    xml += '<osm version="0.6" generator="zone-service">\n';

    // Add bounds (Ho Chi Minh City approximate bounds)
    xml += '  <bounds minlat="10.3" minlon="106.3" maxlat="11.2" maxlon="107.0"/>\n';

    // Add nodes
    for (const node of nodes) {
      xml += `  <node id="${node.osm_id || node.node_id}" lat="${node.lat}" lon="${node.lon}"/>\n`;
    }

    // Add ways (roads)
    for (const road of roads) {
      const roadSegments = segments.filter(s => s.road_id === road.road_id);
      if (roadSegments.length === 0) continue;

      xml += `  <way id="${road.osm_id || road.road_id}">\n`;

      // Add node references
      const nodeSet = new Set<string>();
      for (const segment of roadSegments) {
        nodeSet.add(segment.from_node_id);
        nodeSet.add(segment.to_node_id);
      }

      for (const nodeId of nodeSet) {
        xml += `    <nd ref="${nodeId}"/>\n`;
      }

      // Add tags
      xml += `    <tag k="highway" v="${this.mapRoadTypeToOSM(road.road_type)}"/>\n`;
      xml += `    <tag k="name" v="${this.escapeXML(road.name)}"/>\n`;

      if (road.name_en) {
        xml += `    <tag k="name:en" v="${this.escapeXML(road.name_en)}"/>\n`;
      }

      if (road.max_speed) {
        xml += `    <tag k="maxspeed" v="${road.max_speed}"/>\n`;
      }

      if (road.one_way) {
        xml += `    <tag k="oneway" v="yes"/>\n`;
      }

      if (road.lanes) {
        xml += `    <tag k="lanes" v="${road.lanes}"/>\n`;
      }

      if (road.surface) {
        xml += `    <tag k="surface" v="${road.surface}"/>\n`;
      }

      // Add custom weight tag (OSRM Lua profile will read this)
      const avgWeight = this.calculateAverageWeight(roadSegments);
      xml += `    <tag k="custom_weight" v="${avgWeight.toFixed(2)}"/>\n`;

      xml += `  </way>\n`;
    }

    xml += '</osm>\n';
    return xml;
  }

  /**
   * Generate Lua profile for OSRM with custom weight handling
   */
  private async generateLuaProfile(instanceName: string): Promise<string> {
    logger.info('Generating custom Lua profile...');

    const outputDir = join(process.cwd(), 'osrm_data', instanceName);
    const luaFilePath = join(outputDir, 'custom_car.lua');

    const luaScript = this.getLuaProfileScript();
    writeFileSync(luaFilePath, luaScript, 'utf-8');

    logger.info(`Generated Lua profile at ${luaFilePath}`);
    return luaFilePath;
  }

  /**
   * Get Lua profile script content
   */
  private getLuaProfileScript(): string {
    return `
-- Custom car profile for OSRM with dynamic weights
-- Generated by Zone Service

api_version = 4

properties.max_speed_for_map_matching = 180 / 3.6
properties.use_turn_restrictions = true
properties.continue_straight_at_waypoint = true
properties.weight_name = 'custom'

function setup()
  return {
    properties = properties,
    default_mode = mode.driving,
    default_speed = 30,
    oneway_handling = true,
    turn_penalty = 7.5,
    turn_bias = 1.075,
    u_turn_penalty = 20,
  }
end

function process_way(profile, way, result, relations)
  local highway = way:get_value_by_key("highway")
  local name = way:get_value_by_key("name")
  local custom_weight = tonumber(way:get_value_by_key("custom_weight"))

  -- Skip non-highway ways
  if not highway then
    return
  end

  -- Default speeds by highway type
  local speed_map = {
    motorway = 90,
    trunk = 70,
    primary = 60,
    secondary = 50,
    tertiary = 40,
    unclassified = 35,
    residential = 30,
    service = 20,
    living_street = 15,
  }

  local speed = speed_map[highway] or 30

  -- Get maxspeed if specified
  local maxspeed = tonumber(way:get_value_by_key("maxspeed"))
  if maxspeed and maxspeed > 0 then
    speed = maxspeed
  end

  -- Check oneway
  local oneway = way:get_value_by_key("oneway")
  if oneway == "yes" or oneway == "true" or oneway == "1" then
    result.forward_mode = mode.driving
    result.backward_mode = mode.inaccessible
  else
    result.forward_mode = mode.driving
    result.backward_mode = mode.driving
  end

  -- Set speed
  result.forward_speed = speed
  result.backward_speed = speed

  -- Apply custom weight if available
  if custom_weight and custom_weight > 0 then
    result.forward_rate = 60.0 / custom_weight  -- Convert weight to rate
    result.backward_rate = 60.0 / custom_weight
    result.weight = custom_weight
  else
    -- Calculate default weight from speed
    result.forward_rate = speed
    result.backward_rate = speed
    result.weight = 1.0
  end

  -- Set road name
  if name then
    result.name = name
  end
end

function process_node(profile, node, result, relations)
  -- Process traffic lights, barriers, etc.
  local barrier = node:get_value_by_key("barrier")
  local traffic_signal = node:get_value_by_key("highway")

  if traffic_signal == "traffic_signals" then
    result.traffic_lights = true
  end

  if barrier and barrier ~= "" and barrier ~= "no" then
    result.barrier = true
  end
end

function process_turn(profile, turn)
  -- Add turn penalties
  local angle = math.abs(turn.angle)

  if angle >= 170 and angle <= 190 then
    -- U-turn
    turn.duration = turn.duration + profile.u_turn_penalty
  elseif angle >= 45 then
    -- Regular turn
    turn.duration = turn.duration + profile.turn_penalty
  end
end

return {
  setup = setup,
  process_way = process_way,
  process_node = process_node,
  process_turn = process_turn,
}
`;
  }

  /**
   * Run OSRM processing tools
   */
  private async runOSRMProcess(
    osmFilePath: string,
    luaProfilePath: string,
    _instanceName: string
  ): Promise<void> {
    logger.info('Running OSRM processing...');

    const baseFileName = osmFilePath.replace('.osm.xml', '');

    try {
      // Extract
      logger.info('Running osrm-extract...');
      await execAsync(
        `osrm-extract ${osmFilePath} -p ${luaProfilePath}`,
        { maxBuffer: 100 * 1024 * 1024 } // 100MB buffer
      );

      // Partition
      logger.info('Running osrm-partition...');
      await execAsync(
        `osrm-partition ${baseFileName}.osrm`,
        { maxBuffer: 100 * 1024 * 1024 }
      );

      // Customize
      logger.info('Running osrm-customize...');
      await execAsync(
        `osrm-customize ${baseFileName}.osrm`,
        { maxBuffer: 100 * 1024 * 1024 }
      );

      logger.info('OSRM processing completed successfully');
    } catch (error) {
      logger.error('OSRM processing failed:', error);
      throw new Error(`OSRM processing failed: ${error}`);
    }
  }

  /**
   * Deploy the newly built OSRM instance
   */
  async deployInstance(buildId: string): Promise<void> {
    logger.info(`Deploying OSRM instance for build ${buildId}`);

    try {
      const build = await this.prisma.osrm_builds.findUnique({
        where: { build_id: buildId },
      });

      if (!build || build.status !== 'READY') {
        throw new Error('Build not ready for deployment');
      }

      // TODO: Implement actual deployment logic
      // This would involve:
      // 1. Starting the new OSRM instance
      // 2. Health checking the new instance
      // 3. Switching load balancer to new instance
      // 4. Stopping the old instance

      await this.prisma.osrm_builds.update({
        where: { build_id: buildId },
        data: {
          status: 'DEPLOYED',
          deployed_at: new Date(),
        },
      });

      // Mark previous builds as deprecated
      await this.prisma.osrm_builds.updateMany({
        where: {
          instance_name: build.instance_name,
          build_id: { not: buildId },
          status: 'DEPLOYED',
        },
        data: { status: 'DEPRECATED' },
      });

      // Switch current instance
      this.currentInstance = build.instance_name as any;

      logger.info(`Successfully deployed ${build.instance_name}`);
    } catch (error) {
      logger.error('Deployment failed:', error);
      throw error;
    }
  }

  /**
   * Get the next instance to build (alternates between instance-1 and instance-2)
   */
  private getNextInstance(): 'osrm-instance-1' | 'osrm-instance-2' {
    return this.currentInstance === 'osrm-instance-1'
      ? 'osrm-instance-2'
      : 'osrm-instance-1';
  }

  /**
   * Create a build record in database
   */
  private async createBuildRecord(instanceName: string): Promise<string> {
    const segmentCount = await this.prisma.road_segments.count();
    const avgWeight = await this.prisma.road_segments.aggregate({
      _avg: { current_weight: true },
    });

    const build = await this.prisma.osrm_builds.create({
      data: {
        instance_name: instanceName,
        status: 'PENDING',
        data_snapshot_time: new Date(),
        total_segments: segmentCount,
        avg_weight: avgWeight._avg.current_weight || 0,
      },
    });

    return build.build_id;
  }

  /**
   * Update build status
   */
  private async updateBuildStatus(
    buildId: string,
    status: OsrmBuildStatus,
    message?: string
  ): Promise<void> {
    const updateData: any = { status };

    if (status === 'BUILDING' && !await this.prisma.osrm_builds.findFirst({
      where: { build_id: buildId, started_at: { not: null } }
    })) {
      updateData.started_at = new Date();
    }

    if (status === 'READY' || status === 'FAILED') {
      updateData.completed_at = new Date();
    }

    if (status === 'FAILED' && message) {
      updateData.error_message = message;
    }

    await this.prisma.osrm_builds.update({
      where: { build_id: buildId },
      data: updateData,
    });

    logger.info(`Build ${buildId} status updated to ${status}`);
  }

  /**
   * Helper functions
   */
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
      LIVING_STREET: 'living_street',
      PEDESTRIAN: 'pedestrian',
      TRACK: 'track',
      PATH: 'path',
    };

    return mapping[roadType] || 'unclassified';
  }

  private escapeXML(text: string): string {
    return text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&apos;');
  }

  private calculateAverageWeight(segments: any[]): number {
    if (segments.length === 0) return 1.0;

    const sum = segments.reduce((acc, s) => acc + s.current_weight, 0);
    return sum / segments.length;
  }
}
