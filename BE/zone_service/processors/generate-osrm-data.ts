/**
 * Generate OSRM Data Files
 *
 * This script:
 * 1. Exports road network from database to OSM XML
 * 2. Generates custom Lua profile
 * 3. Uses OSRM Docker image to process the data
 * 4. Generates OSRM routing files for both instances
 */

import { PrismaClient } from '@prisma/client';
import { writeFileSync, existsSync, mkdirSync } from 'fs';
import { join } from 'path';
import { exec } from 'child_process';
import { promisify } from 'util';
import { TomTomTrafficService } from '../services/tomtom-traffic-service.js';

const execAsync = promisify(exec);
const prisma = new PrismaClient();

/**
 * Export road network to OSM XML format
 */
async function exportToOSMXML(instanceName: string): Promise<string> {
  console.log(`\n📦 Exporting road network to OSM XML for ${instanceName}...`);

  const outputDir = join(process.cwd(), 'osrm_data', instanceName);
  if (!existsSync(outputDir)) {
    mkdirSync(outputDir, { recursive: true });
  }

  const osmFilePath = join(outputDir, 'network.osm.xml');

  // Fetch all roads, nodes, segments, and traffic conditions
  console.log('   Fetching data from database...');
  const [roads, nodes, segments, trafficConditions] = await Promise.all([
    prisma.roads.findMany(),
    prisma.road_nodes.findMany(),
    prisma.road_segments.findMany({
      include: {
        from_node: true,
        to_node: true,
        traffic_conditions: {
          where: {
            expires_at: {
              gte: new Date() // Only get non-expired traffic data
            }
          },
          orderBy: {
            source_timestamp: 'desc' // Get latest traffic data
          },
          take: 1 // Only the most recent per segment
        }
      },
    }),
    prisma.traffic_conditions.findMany({
      where: {
        expires_at: {
          gte: new Date()
        }
      },
      include: {
        road_segment: true
      }
    })
  ]);

  console.log(`   Found ${roads.length} roads, ${nodes.length} nodes, ${segments.length} segments`);
  console.log(`   Found ${trafficConditions.length} active traffic conditions`);

  // Create mapping from string IDs to numeric IDs
  console.log('   Creating ID mappings...');
  const nodeIdMap = new Map<string, number>();
  const roadIdMap = new Map<string, number>();

  nodes.forEach((node, index) => {
    const numericId = node.osm_id && !isNaN(Number(node.osm_id))
      ? Number(node.osm_id)
      : 1000000 + index;
    nodeIdMap.set(node.node_id, numericId);
  });

  roads.forEach((road, index) => {
    const numericId = road.osm_id && !isNaN(Number(road.osm_id))
      ? Number(road.osm_id)
      : 2000000 + index;
    roadIdMap.set(road.road_id, numericId);
  });

  // Generate OSM XML
  console.log('   Generating OSM XML...');
  let xml = '<?xml version="1.0" encoding="UTF-8"?>\n';
  xml += '<osm version="0.6" generator="zone-service">\n';
  xml += '  <bounds minlat="10.3" minlon="106.3" maxlat="11.2" maxlon="107.0"/>\n';

  // Add nodes
  for (const node of nodes) {
    const numericId = nodeIdMap.get(node.node_id)!;
    xml += `  <node id="${numericId}" lat="${node.lat}" lon="${node.lon}"/>\n`;
  }

  // Add ways (roads) with segments
  for (const road of roads) {
    const roadSegments = segments.filter(s => s.road_id === road.road_id);
    if (roadSegments.length === 0) continue;

    const wayId = roadIdMap.get(road.road_id)!;
    xml += `  <way id="${wayId}">\n`;

    // Build ordered node list for the way
    const orderedNodes: string[] = [];
    const segmentMap = new Map(roadSegments.map(s => [s.from_node_id, s]));

    // Start with the first segment
    if (roadSegments.length > 0) {
      let currentSegment = roadSegments[0];
      orderedNodes.push(currentSegment.from_node_id);
      orderedNodes.push(currentSegment.to_node_id);

      // Try to chain segments (simple approach for now)
      for (let i = 1; i < roadSegments.length; i++) {
        const nextSegment = roadSegments[i];
        if (nextSegment.from_node_id === orderedNodes[orderedNodes.length - 1]) {
          orderedNodes.push(nextSegment.to_node_id);
        } else if (!orderedNodes.includes(nextSegment.from_node_id)) {
          orderedNodes.push(nextSegment.from_node_id);
          orderedNodes.push(nextSegment.to_node_id);
        }
      }
    }

    // Add node references using numeric IDs
    for (const nodeId of orderedNodes) {
      const numericNodeId = nodeIdMap.get(nodeId)!;
      xml += `    <nd ref="${numericNodeId}"/>\n`;
    }

    // Add tags
    const roadType = mapRoadTypeToOSM(road.road_type);
    xml += `    <tag k="highway" v="${roadType}"/>\n`;
    xml += `    <tag k="name" v="${escapeXML(road.name)}"/>\n`;

    if (road.name_en) {
      xml += `    <tag k="name:en" v="${escapeXML(road.name_en)}"/>\n`;
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

    // Add custom weight tag (average from segments with traffic conditions)
    const avgWeight = calculateAverageWeightWithTraffic(roadSegments);
    xml += `    <tag k="custom_weight" v="${avgWeight.toFixed(2)}"/>\n`;
    
    // Add traffic level tag (most common traffic level for this road)
    const trafficLevel = getMostCommonTrafficLevel(roadSegments);
    if (trafficLevel) {
      xml += `    <tag k="traffic_level" v="${trafficLevel}"/>\n`;
    }
    
    // Add congestion score tag
    const congestionScore = getAverageCongestionScore(roadSegments);
    if (congestionScore > 0) {
      xml += `    <tag k="congestion_score" v="${congestionScore.toFixed(1)}"/>\n`;
    }

    xml += `  </way>\n`;
  }

  xml += '</osm>\n';

  // Write to file
  writeFileSync(osmFilePath, xml, 'utf-8');

  console.log(`   ✓ Exported to ${osmFilePath}`);
  return osmFilePath;
}

/**
 * Generate Lua profile for OSRM
 */
function generateLuaProfile(instanceName: string): string {
  console.log(`\n🔧 Generating custom Lua profile for ${instanceName}...`);

  const outputDir = join(process.cwd(), 'osrm_data', instanceName);
  const luaFilePath = join(outputDir, 'custom_car.lua');

  const luaScript = `-- Custom car profile for OSRM with dynamic weights
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
  local custom_weight = tonumber(way:get_value_by_key("custom_weight"))
  local traffic_level = way:get_value_by_key("traffic_level")
  local congestion_score = tonumber(way:get_value_by_key("congestion_score"))

  if not highway then
    return
  end

  -- Default speeds
  local speed_map = {
    motorway = 90,
    trunk = 70,
    primary = 60,
    secondary = 50,
    tertiary = 40,
    residential = 30,
    service = 20,
  }

  local speed = speed_map[highway] or 30
  local maxspeed = tonumber(way:get_value_by_key("maxspeed"))
  if maxspeed and maxspeed > 0 then
    speed = maxspeed
  end

  -- Apply traffic-based speed reduction
  if traffic_level then
    local traffic_multiplier = 1.0
    if traffic_level == "FREE_FLOW" then
      traffic_multiplier = 1.1  -- Slightly faster
    elseif traffic_level == "NORMAL" then
      traffic_multiplier = 1.0  -- Normal speed
    elseif traffic_level == "SLOW" then
      traffic_multiplier = 0.7  -- 30% slower
    elseif traffic_level == "CONGESTED" then
      traffic_multiplier = 0.4  -- 60% slower
    elseif traffic_level == "BLOCKED" then
      traffic_multiplier = 0.1  -- 90% slower
    end
    
    -- Apply congestion score if available
    if congestion_score and congestion_score > 0 then
      local congestion_multiplier = math.max(0.1, 1.0 - (congestion_score / 100))
      traffic_multiplier = traffic_multiplier * congestion_multiplier
    end
    
    speed = speed * traffic_multiplier
  end

  -- Handle oneway
  local oneway = way:get_value_by_key("oneway")
  if oneway == "yes" then
    result.forward_mode = mode.driving
    result.backward_mode = mode.inaccessible
  else
    result.forward_mode = mode.driving
    result.backward_mode = mode.driving
  end

  -- Set speed
  result.forward_speed = speed
  result.backward_speed = speed

  -- Apply custom weight (includes traffic conditions)
  if custom_weight and custom_weight > 0 then
    result.forward_rate = 60.0 / custom_weight
    result.backward_rate = 60.0 / custom_weight
    result.weight = custom_weight
  else
    result.forward_rate = speed
    result.backward_rate = speed
  end
end

function process_node(profile, node, result, relations)
  local traffic_signal = node:get_value_by_key("highway")
  if traffic_signal == "traffic_signals" then
    result.traffic_lights = true
  end
end

function process_turn(profile, turn)
  local angle = math.abs(turn.angle)
  if angle >= 170 and angle <= 190 then
    turn.duration = turn.duration + profile.u_turn_penalty
  elseif angle >= 45 then
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

  writeFileSync(luaFilePath, luaScript, 'utf-8');
  console.log(`   ✓ Generated Lua profile at ${luaFilePath}`);
  return luaFilePath;
}

/**
 * Run OSRM processing using Docker
 */
async function runOSRMProcessing(instanceName: string): Promise<void> {
  console.log(`\n⚙️  Running OSRM processing for ${instanceName}...`);

  const dataDir = join(process.cwd(), 'osrm_data', instanceName);
  const osmFile = 'network.osm.xml';
  const profileFile = 'custom_car.lua';
  const osrmFile = 'network.osrm';

  try {
    // Step 1: Extract
    console.log('   Running osrm-extract...');
    const extractCmd = `docker run --rm -v "${dataDir}:/data" osrm/osrm-backend:latest osrm-extract -p /data/${profileFile} /data/${osmFile}`;
    console.log(`   Command: ${extractCmd}`);
    const { stdout: extractOut, stderr: extractErr } = await execAsync(extractCmd, {
      maxBuffer: 100 * 1024 * 1024
    });
    if (extractOut) console.log('   ', extractOut.trim());
    if (extractErr) console.error('   ', extractErr.trim());
    console.log('   ✓ Extract completed');

    // Step 2: Partition
    console.log('   Running osrm-partition...');
    const partitionCmd = `docker run --rm -v "${dataDir}:/data" osrm/osrm-backend:latest osrm-partition /data/${osrmFile}`;
    console.log(`   Command: ${partitionCmd}`);
    const { stdout: partOut, stderr: partErr } = await execAsync(partitionCmd, {
      maxBuffer: 100 * 1024 * 1024
    });
    if (partOut) console.log('   ', partOut.trim());
    if (partErr) console.error('   ', partErr.trim());
    console.log('   ✓ Partition completed');

    // Step 3: Customize
    console.log('   Running osrm-customize...');
    const customizeCmd = `docker run --rm -v "${dataDir}:/data" osrm/osrm-backend:latest osrm-customize /data/${osrmFile}`;
    console.log(`   Command: ${customizeCmd}`);
    const { stdout: custOut, stderr: custErr } = await execAsync(customizeCmd, {
      maxBuffer: 100 * 1024 * 1024
    });
    if (custOut) console.log('   ', custOut.trim());
    if (custErr) console.error('   ', custErr.trim());
    console.log('   ✓ Customize completed');

    console.log(`   ✅ OSRM processing completed for ${instanceName}`);
  } catch (error: any) {
    console.error(`   ❌ OSRM processing failed: ${error.message}`);
    throw error;
  }
}

/**
 * Helper functions
 */
function mapRoadTypeToOSM(roadType: string): string {
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

function escapeXML(text: string): string {
  if (!text) return '';
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;');
}

function calculateAverageWeightWithTraffic(segments: any[]): number {
  if (segments.length === 0) return 1.0;
  
  const sum = segments.reduce((acc, segment) => {
    // Use current_weight (includes traffic adjustments) if available
    let weight = segment.current_weight || segment.base_weight || 1.0;
    
    // Apply traffic condition multiplier if available
    if (segment.traffic_conditions && segment.traffic_conditions.length > 0) {
      const traffic = segment.traffic_conditions[0];
      weight *= traffic.weight_multiplier || 1.0;
    }
    
    return acc + weight;
  }, 0);
  
  return sum / segments.length;
}

function getMostCommonTrafficLevel(segments: any[]): string | null {
  const trafficLevels = segments
    .filter(s => s.traffic_conditions && s.traffic_conditions.length > 0)
    .map(s => s.traffic_conditions[0].traffic_level);
    
  if (trafficLevels.length === 0) return null;
  
  // Count occurrences
  const counts: Record<string, number> = {};
  trafficLevels.forEach(level => {
    counts[level] = (counts[level] || 0) + 1;
  });
  
  // Return most common
  return Object.entries(counts)
    .sort(([,a], [,b]) => b - a)[0]?.[0] || null;
}

function getAverageCongestionScore(segments: any[]): number {
  const scores = segments
    .filter(s => s.traffic_conditions && s.traffic_conditions.length > 0)
    .map(s => s.traffic_conditions[0].congestion_score);
    
  if (scores.length === 0) return 0;
  
  return scores.reduce((sum, score) => sum + score, 0) / scores.length;
}

/**
 * Main function
 */
async function main() {
  console.log('='.repeat(70));
  console.log('🚀 OSRM Data Generator with Real-time Traffic');
  console.log('='.repeat(70));

  const instances = ['osrm-instance-1', 'osrm-instance-2'];

  try {
    // Step 0: Fetch and update traffic data
    console.log('\n🌐 Step 0: Fetching real-time traffic data...');
    console.log('─'.repeat(70));
    
    try {
      const trafficService = new TomTomTrafficService();
      
      // Clean up expired conditions first
      await trafficService.cleanupExpiredConditions();
      
      // Fetch fresh traffic data for Thu Duc area
      const boundingBox = '10.3,106.3,11.2,107.0'; // Thu Duc area
      await trafficService.fetchAndUpdateTrafficData(boundingBox);
      
      console.log('✅ Traffic data updated successfully');
    } catch (error) {
      console.error('❌ Failed to fetch traffic data:', error);
      console.log('⚠️  Continuing with base weights only');
    }

    // Step 1-3: Generate OSRM data for each instance
    for (const instance of instances) {
      console.log(`\n${'─'.repeat(70)}`);
      console.log(`Processing ${instance}`);
      console.log('─'.repeat(70));

      // Step 1: Export to OSM XML
      await exportToOSMXML(instance);

      // Step 2: Generate Lua profile
      generateLuaProfile(instance);

      // Step 3: Run OSRM processing
      await runOSRMProcessing(instance);

      console.log(`\n✅ ${instance} is ready!`);
    }

    console.log('\n' + '='.repeat(70));
    console.log('🎉 All OSRM instances generated successfully!');
    console.log('='.repeat(70));
    console.log('\nYou can now start the OSRM containers:');
    console.log('  docker-compose up osrm-instance-1 osrm-instance-2');
    console.log('\nOr start all services:');
    console.log('  docker-compose up -d');
    console.log('='.repeat(70));

    process.exit(0);
  } catch (error) {
    console.error('\n❌ Generation failed:', error);
    process.exit(1);
  } finally {
    await prisma.$disconnect();
  }
}

main();
