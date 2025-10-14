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

  // Fetch all roads, nodes, and segments
  console.log('   Fetching data from database...');
  const [roads, nodes, segments] = await Promise.all([
    prisma.roads.findMany(),
    prisma.road_nodes.findMany(),
    prisma.road_segments.findMany({
      include: {
        from_node: true,
        to_node: true,
      },
    }),
  ]);

  console.log(`   Found ${roads.length} roads, ${nodes.length} nodes, ${segments.length} segments`);

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

    // Add custom weight tag (average from segments)
    const avgWeight = calculateAverageWeight(roadSegments);
    xml += `    <tag k="custom_weight" v="${avgWeight.toFixed(2)}"/>\n`;

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

  -- Apply custom weight
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

function calculateAverageWeight(segments: any[]): number {
  if (segments.length === 0) return 1.0;
  const sum = segments.reduce((acc, s) => acc + (s.current_weight || s.base_weight || 1.0), 0);
  return sum / segments.length;
}

/**
 * Main function
 */
async function main() {
  console.log('='.repeat(70));
  console.log('🚀 OSRM Data Generator');
  console.log('='.repeat(70));

  const instances = ['osrm-instance-1', 'osrm-instance-2'];

  try {
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
