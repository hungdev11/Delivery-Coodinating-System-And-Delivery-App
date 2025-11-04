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
import { TomTomTrafficService } from '../../services/tomtom-traffic-service';

const execAsync = promisify(exec);
const prisma = new PrismaClient();

/**
 * Export road network to OSM XML format
 */
async function exportToOSMXML(instanceName: string, mode: 'strict_priority_with_delta' | 'flexible_priority_with_delta' | 'strict_priority_no_delta' | 'flexible_priority_no_delta' | 'base' = 'base'): Promise<string> {
  console.log(`\n📦 Exporting road network to OSM XML for ${instanceName} (mode: ${mode})...`);

  const outputDir = join(process.cwd(), 'osrm_data', instanceName);
  if (!existsSync(outputDir)) {
    mkdirSync(outputDir, { recursive: true });
  }

  const osmFilePath = join(outputDir, 'network.osm.xml');

  // Fetch all roads, nodes, segments, traffic conditions, and user feedback
  // NOTE: Load segments in batches to avoid MySQL prepared statement limit
  console.log('   Fetching data from database...');

  // Load basic data first
  const [roads, nodes] = await Promise.all([
    prisma.roads.findMany(),
    prisma.road_nodes.findMany(),
  ]);

  console.log(`   Found ${roads.length} roads, ${nodes.length} nodes`);

  // Load segments in batches (MySQL prepared statement limit workaround)
  console.log('   Loading segments in batches...');
  const BATCH_SIZE = 5000;
  const segments: any[] = [];
  let offset = 0;
  let batch = 0;

  while (true) {
    batch++;
    const segmentBatch = await prisma.road_segments.findMany({
      skip: offset,
      take: BATCH_SIZE,
      include: {
        from_node: true,
        to_node: true,
        traffic_conditions: {
          where: {
            expires_at: {
              gte: new Date()
            }
          },
          orderBy: {
            source_timestamp: 'desc'
          },
          take: 1
        },
        user_feedback: {
          orderBy: {
            created_at: 'desc'
          },
          take: 10
        }
      },
    });

    if (segmentBatch.length === 0) break;

    segments.push(...segmentBatch);
    offset += BATCH_SIZE;
    console.log(`   Loaded batch ${batch}: ${segments.length} segments total`);

    if (segmentBatch.length < BATCH_SIZE) break; // Last batch
  }

  // Traffic conditions (not used in current logic, but kept for compatibility)
  const trafficConditions = await prisma.traffic_conditions.findMany({
      where: {
        expires_at: {
          gte: new Date()
        }
      },
    take: 1000, // Limit for performance
  });

  // Load road overrides for dynamic routing adjustments
  console.log('   Loading road overrides...');
  const overrides = await prisma.road_overrides.findMany();

  // Build lookup maps for fast access
  const overridesBySegmentId = new Map(
    overrides.filter(o => o.segment_id).map(o => [o.segment_id, o])
  );
  const overridesByOsmWayId = new Map(
    overrides.filter(o => o.osm_way_id).map(o => [o.osm_way_id?.toString(), o])
  );

  console.log(`   Found ${roads.length} roads, ${nodes.length} nodes, ${segments.length} segments`);
  console.log(`   Found ${trafficConditions.length} active traffic conditions`);
  console.log(`   Found ${overrides.length} road overrides`);

  // Calculate delta_weight from user feedback and update database
  console.log('   Calculating delta weights from user feedback...');
  const deltaWeightUpdates: Array<{ segment_id: string; delta_weight: number }> = [];

  for (const segment of segments) {
    const shipperScore = calculateShipperFeedbackScore([segment]);
    if (shipperScore !== null && segment.user_feedback.length > 0) {
      // Convert score (0-1) to weight penalty
      // Score 1.0 = no penalty (delta 0)
      // Score 0.5 = +50% penalty (delta 0.5 * base_weight)
      // Score 0.0 = +100% penalty (delta 1.0 * base_weight)
      const baseWeight = segment.base_weight ?? 1.0;
      const delta_weight = (1.0 - shipperScore) * baseWeight;
      deltaWeightUpdates.push({
        segment_id: segment.segment_id,
        delta_weight
      });
    }
  }

  // Batch update delta_weight in database
  if (deltaWeightUpdates.length > 0) {
    console.log(`   Updating ${deltaWeightUpdates.length} segment delta weights...`);
    const DELTA_BATCH_SIZE = 1000;

    for (let i = 0; i < deltaWeightUpdates.length; i += DELTA_BATCH_SIZE) {
      const batch = deltaWeightUpdates.slice(i, i + DELTA_BATCH_SIZE);
      const caseStatements = batch
        .map(u => `WHEN '${u.segment_id}' THEN ${u.delta_weight}`)
        .join(' ');
      const segmentIds = batch.map(u => `'${u.segment_id}'`).join(',');

      await prisma.$executeRawUnsafe(`
        UPDATE road_segments
        SET delta_weight = CASE segment_id ${caseStatements} END
        WHERE segment_id IN (${segmentIds})
      `);
    }
    console.log(`   ✓ Updated ${deltaWeightUpdates.length} segment delta weights`);
  } else {
    console.log(`   ℹ️  No user feedback found, skipping delta weight update`);
  }

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

  // Prepare coordinate -> numeric node id map for reusing nodes at same position
  // CRITICAL: Use real OSM node IDs so backend can match them to DB
  const coordToNodeId = new Map<string, number>();
  const nodeCoords = new Map<number, { lat: number; lon: number }>(); // Store coords for later export
  const coordToDbNode = new Map<string, any>(); // Map coord to DB node with OSM ID

  // Build reverse lookup: coordinates -> DB node
  for (const node of nodes) {
    if (node.lat && node.lon) {
      const key = coordKey(node.lat, node.lon);
      coordToDbNode.set(key, node);
    }
  }

  function coordKey(lat: number, lon: number): string {
    return `${lat.toFixed(7)},${lon.toFixed(7)}`; // 7 decimals for ~1cm precision
  }

  function ensureNodeForCoord(lat: number, lon: number): number {
    const key = coordKey(lat, lon);
    if (coordToNodeId.has(key)) return coordToNodeId.get(key)!;

    // Try to find matching DB node with real OSM ID
    const dbNode = coordToDbNode.get(key);
    let nodeId: number;

    if (dbNode && dbNode.osm_id && !isNaN(Number(dbNode.osm_id))) {
      // Use real OSM node ID from database
      nodeId = Number(dbNode.osm_id);
    } else {
      // Fallback: use DB node_id hash or generate (should rarely happen)
      nodeId = dbNode ? Math.abs(hashString(dbNode.node_id)) : 10_000_000 + coordToNodeId.size;
    }

    coordToNodeId.set(key, nodeId);
    nodeCoords.set(nodeId, { lat, lon });
    return nodeId;
  }

  function hashString(str: string): number {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
      const char = str.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash; // Convert to 32bit integer
    }
    return Math.abs(hash);
  }

  // STEP 1: Collect all ways first (build node references)
  console.log('   Building ways from segments...');
  const ways: Array<{
    id: number;
    nodeRefs: number[];
    tags: Array<{ k: string; v: string }>;
  }> = [];

  let wayIdCounter = 5_000_000;
  let skippedSegments = 0;

  for (const segment of segments) {
    const segWayId = wayIdCounter++;
    const nodeRefs: number[] = [];
    const tags: Array<{ k: string; v: string }> = [];

    // Parse geometry: expect GeoJSON LineString { coordinates: [[lon,lat], ...] }
    let coords: Array<[number, number]> = [];
    try {
      if (typeof segment.geometry === 'string') {
        const parsed = JSON.parse(segment.geometry);
        coords = parsed.coordinates || [];
      } else if (segment.geometry && typeof segment.geometry === 'object' && 'coordinates' in segment.geometry) {
        coords = (segment.geometry as any).coordinates;
      }
    } catch (e) {
      // Fallback: use endpoint nodes
      const fromNode = nodeIdMap.get(segment.from_node_id);
      const toNode = nodeIdMap.get(segment.to_node_id);
      if (fromNode && toNode) {
        nodeRefs.push(fromNode, toNode);
      } else {
        skippedSegments++;
        continue;
      }
    }

    // Build node references from geometry
    if (coords.length >= 2) {
      for (const [lon, lat] of coords) {
        nodeRefs.push(ensureNodeForCoord(lat, lon));
      }
    } else if (coords.length === 0 && nodeRefs.length === 0) {
      // Fallback to endpoint nodes
      const fromNode = nodeIdMap.get(segment.from_node_id);
      const toNode = nodeIdMap.get(segment.to_node_id);
      if (fromNode && toNode) {
        nodeRefs.push(fromNode, toNode);
      } else {
        skippedSegments++;
        continue;
        }
      }

    if (nodeRefs.length < 2) {
      skippedSegments++;
      continue;
    }

    // Build tags
    const road = roads.find(r => r.road_id === segment.road_id);
    const roadType = road ? mapRoadTypeToOSM(road.road_type) : 'unclassified';
    tags.push({ k: 'highway', v: roadType });

    if (segment.name) {
      tags.push({ k: 'name', v: escapeXML(segment.name) });
    } else if (road && road.name) {
      tags.push({ k: 'name', v: escapeXML(road.name) });
    }

    if (road && road.name_en) {
      tags.push({ k: 'name:en', v: escapeXML(road.name_en) });
    }

    if (segment.max_speed || (road && road.max_speed)) {
      tags.push({ k: 'maxspeed', v: String(segment.max_speed || road!.max_speed) });
    }

    if (road && road.lanes) {
      tags.push({ k: 'lanes', v: String(road.lanes) });
    }

    // Calculate final weight: base_weight + delta_weight (from user feedback)
    const baseWeight = segment.base_weight ?? 1.0;
    const deltaWeight = segment.delta_weight ?? 0.0;
    const segWeight = baseWeight + deltaWeight;
    tags.push({ k: 'custom_weight', v: segWeight.toFixed(2) });

    const trafficCondition = segment.traffic_conditions && segment.traffic_conditions[0];
    if (trafficCondition) {
      tags.push({ k: 'traffic_level', v: trafficCondition.traffic_level });
      if (trafficCondition.congestion_score > 0) {
        tags.push({ k: 'congestion_score', v: trafficCondition.congestion_score.toFixed(1) });
      }
    }

    const shipperScore = calculateShipperFeedbackScore([segment]);
    if (shipperScore !== null) {
      tags.push({ k: 'shipper_score', v: shipperScore.toFixed(2) });
    }

    // Add knp:* tags from overrides (for dynamic routing adjustments)
    // Different modes apply overrides differently
    const override = overridesBySegmentId.get(segment.segment_id) ||
                     (segment.osm_way_id ? overridesByOsmWayId.get(segment.osm_way_id.toString()) : null);

    // Mode-specific override application
    if (mode !== 'base' && override) {
      // Block level (always applied except in base mode)
      if (override.block_level && override.block_level !== 'none') {
        tags.push({ k: 'knp:block_level', v: override.block_level });
      }

      // For 'no_recommend' mode, disable recommendations
      const recommendEnabled = mode === 'no_recommend' ? false : (override.recommend_enabled !== false);

      // Delta weight adjustment (except no_recommend mode)
      if (mode !== 'no_recommend' && override.delta !== null && override.delta !== undefined) {
        tags.push({ k: 'knp:delta', v: override.delta.toFixed(3) });
      }

      // Point score (except no_recommend mode)
      if (mode !== 'no_recommend' && override.point_score !== null && override.point_score !== undefined) {
        tags.push({ k: 'knp:point_score', v: override.point_score.toFixed(3) });
      }

      // Recommendation enabled flag
      tags.push({ k: 'knp:recommend_enabled', v: recommendEnabled ? '1' : '0' });

      // Custom penalty factors (if set)
      if (override.soft_penalty_factor !== null && override.soft_penalty_factor !== undefined) {
        tags.push({ k: 'knp:soft_penalty', v: override.soft_penalty_factor.toFixed(2) });
      }
      if (override.min_penalty_factor !== null && override.min_penalty_factor !== undefined) {
        tags.push({ k: 'knp:min_penalty', v: override.min_penalty_factor.toFixed(2) });
      }

      // Mode tag for profile to use
      tags.push({ k: 'knp:mode', v: mode });
    } else if (mode === 'base') {
      // Base mode: no overrides applied
      tags.push({ k: 'knp:mode', v: 'base' });
    }

    // Handle oneway vs bidirectional roads
    const isOneWay = segment.one_way || (road && road.one_way);

    if (isOneWay) {
      // One-way road: create single way with oneway tag
      tags.push({ k: 'oneway', v: 'yes' });
      ways.push({ id: segWayId, nodeRefs, tags });
    } else {
      // Bidirectional road: create TWO ways (forward and backward)
      // This allows OSRM to route in both directions and make U-turns at any point

      // Forward way (original direction)
      ways.push({ id: segWayId, nodeRefs, tags });

      // Backward way (reversed direction)
      const reverseWayId = wayIdCounter++;
      const reverseNodeRefs = [...nodeRefs].reverse();
      ways.push({ id: reverseWayId, nodeRefs: reverseNodeRefs, tags });
    }
  }

  console.log(`   Built ${ways.length} ways (${skippedSegments} segments skipped)`);
  console.log(`   Created ${nodeCoords.size} unique nodes from geometry`);

  // Log bidirectional stats
  const bidirectionalCount = segments.filter(s => !s.one_way && !(roads.find(r => r.road_id === s.road_id)?.one_way)).length;
  const onewayCount = segments.length - bidirectionalCount;
  console.log(`   Bidirectional segments: ${bidirectionalCount} (exported as ${bidirectionalCount * 2} ways)`);
  console.log(`   One-way segments: ${onewayCount} (exported as ${onewayCount} ways)`);

  // STEP 2: Write XML in correct order: header → nodes → ways
  console.log('   Writing OSM XML...');
  let xml = '<?xml version="1.0" encoding="UTF-8"?>\n';
  xml += '<osm version="0.6" generator="zone-service">\n';
  xml += '  <bounds minlat="10.3" minlon="106.3" maxlat="11.2" maxlon="107.0"/>\n';

  // Write all nodes first
  console.log('   Writing nodes...');
  for (const [nodeId, coords] of nodeCoords.entries()) {
    xml += `  <node id="${nodeId}" lat="${coords.lat}" lon="${coords.lon}"/>\n`;
    }

  // Write all ways after nodes
  console.log('   Writing ways...');
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

  // Write to file
  writeFileSync(osmFilePath, xml, 'utf-8');

  console.log(`   ✓ Exported ${ways.length} ways with ${nodeCoords.size} unique nodes`);
  console.log(`   ✓ Saved to ${osmFilePath}`);
  return osmFilePath;
}

/**
 * Generate Car Lua Script (based on standard OSRM car.lua with custom weights)
 */
function generateCarLuaScript(): string {
  return `-- Custom car profile for OSRM with dynamic weights
-- Based on standard OSRM car.lua but with traffic-aware routing
-- Generated by Zone Service

api_version = 4

properties.max_speed_for_map_matching = 180 / 3.6
properties.use_turn_restrictions = true
properties.continue_straight_at_waypoint = true
properties.weight_name = 'routability'  -- Use routability for better turn handling
properties.weight_precision = 2

function setup()
  return {
    properties = properties,
    default_mode = mode.driving,
    default_speed = 30,
    oneway_handling = true,
    turn_penalty = 7.5,
    turn_bias = 1.075,
    u_turn_penalty = 50,
    continue_straight_at_waypoint = true,
    traffic_signal_penalty = 2,
    use_turn_restrictions = false,
    left_hand_driving = false,
    process_call_tagless_node = true,
  }
end

function process_way(profile, way, result, relations)
  local highway = way:get_value_by_key("highway")
  local name = way:get_value_by_key("name")
  local ref = way:get_value_by_key("ref")
  local custom_weight = tonumber(way:get_value_by_key("custom_weight"))
  local traffic_level = way:get_value_by_key("traffic_level")
  local congestion_score = tonumber(way:get_value_by_key("congestion_score"))

  if not highway then
    return
  end

  -- Default speeds
  local speed_map = {
    motorway = 90,
    motorway_link = 45,
    trunk = 70,
    trunk_link = 40,
    primary = 60,
    primary_link = 30,
    secondary = 50,
    secondary_link = 25,
    tertiary = 40,
    tertiary_link = 20,
    residential = 30,
    service = 20,
    unclassified = 25,
    living_street = 10,
  }

  local speed = speed_map[highway] or 30
  local maxspeed = tonumber(way:get_value_by_key("maxspeed"))
  if maxspeed and maxspeed > 0 then
    speed = maxspeed
  end

  -- Apply traffic-based speed reduction (critical for routing)
  if traffic_level then
    local traffic_multiplier = 1.0
    if traffic_level == "FREE_FLOW" then
      traffic_multiplier = 1.1
    elseif traffic_level == "NORMAL" then
      traffic_multiplier = 1.0
    elseif traffic_level == "SLOW" then
      traffic_multiplier = 0.7
    elseif traffic_level == "CONGESTED" then
      traffic_multiplier = 0.4
    elseif traffic_level == "BLOCKED" then
      traffic_multiplier = 0.1
    end

    if congestion_score and congestion_score > 0 then
      local congestion_multiplier = math.max(0.1, 1.0 - (congestion_score / 100))
      traffic_multiplier = traffic_multiplier * congestion_multiplier
    end

    speed = speed * traffic_multiplier
  end

  -- Handle oneway
  local oneway = way:get_value_by_key("oneway")
  if oneway == "yes" or oneway == "1" or oneway == "true" then
    result.forward_mode = mode.driving
    result.backward_mode = mode.inaccessible
  elseif oneway == "-1" or oneway == "reverse" then
    result.forward_mode = mode.inaccessible
    result.backward_mode = mode.driving
  else
    result.forward_mode = mode.driving
    result.backward_mode = mode.driving
  end

  result.forward_speed = speed
  result.backward_speed = speed

  -- Set name for OSRM to use in instructions
  if name then
    result.name = name
  elseif ref then
    result.name = ref
  end

  -- CRITICAL: Custom weight system for traffic-aware routing
  if custom_weight and custom_weight > 0 then
    -- Convert custom_weight to duration (seconds)
    -- weight is abstract units, convert to time penalty
    local weight_duration = custom_weight * 60.0  -- scale factor
    result.duration = weight_duration
    result.weight = custom_weight
  else
    -- Use speed-based calculation
    result.duration = 0  -- OSRM will calc from speed
    result.weight = 0    -- OSRM will calc from speed
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

  -- U-turn penalty
  if angle >= 170 and angle <= 190 then
    turn.duration = turn.duration + profile.u_turn_penalty
    turn.weight = turn.weight + profile.u_turn_penalty
  elseif angle >= 45 then
    -- Regular turn penalty
    turn.duration = turn.duration + profile.turn_penalty
    turn.weight = turn.weight + profile.turn_penalty
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
 * Generate Motorbike Lua Script (Vietnam optimized with shipper feedback)
 * Refactored: Base script + mode-specific weight functions
 */
function generateMotorbikeLuaScript(mode: 'strict_priority_with_delta' | 'flexible_priority_with_delta' | 'strict_priority_no_delta' | 'flexible_priority_no_delta' | 'base' = 'base'): string {
  
  // Determine mode flags
  const isStrictPriority = mode.startsWith('strict_priority');
  const hasDelta = mode.endsWith('_with_delta');
  const isBase = mode === 'base';

  return `-- Custom motorbike profile for OSRM with shipper feedback
-- Generated by Zone Service
-- Mode: ${mode.toUpperCase()}
-- Optimized for Vietnam traffic (no motorways, shipper ratings matter)

api_version = 4

properties.max_speed_for_map_matching = 140 / 3.6
properties.use_turn_restrictions = false  -- Vietnam motorbikes ignore most restrictions
properties.continue_straight_at_waypoint = true
properties.weight_name = 'custom'

function setup()
  return {
    properties = properties,
    default_mode = mode.driving,
    default_speed = 35,     -- Realistic Saigon motorbike speed
    oneway_handling = false, -- Allow motorbikes to reverse on non-critical oneways
    turn_penalty = 4,       -- Motorbikes turn easier
    turn_bias = 1.05,
    u_turn_penalty = 5,     -- Very low - motorbikes U-turn easily in Vietnam
    -- Mode flags
    strict_priority = ${isStrictPriority ? 'true' : 'false'},
    use_delta = ${hasDelta ? 'true' : 'false'},
    is_base = ${isBase ? 'true' : 'false'},
  }
end

-- Base shipper feedback adjustment (always applies except base mode)
function apply_shipper_feedback(custom_weight, shipper_score, is_base)
  if is_base then
    return custom_weight
  end
  -- Score 0-1: 0 = terrible road, 1 = excellent road
  local shipper_penalty = 2.0 - shipper_score
  return custom_weight * shipper_penalty
end

-- Calculate weight adjustment based on mode
function calculate_weight_adjustment(custom_weight, speed, shipper_score, knp_point_score, knp_delta, knp_recommend_enabled, strict_priority, use_delta, is_base)
  local adjusted_weight = custom_weight or 1.0
  
  if not custom_weight or custom_weight <= 0 then
    return adjusted_weight
  end

  -- Step 1: Apply shipper feedback (except base)
  adjusted_weight = apply_shipper_feedback(adjusted_weight, shipper_score, is_base)

  -- Step 2: Base mode - only speed adjustment
  if is_base then
    local speed_factor = 1.0 - ((speed - 25) / 100)
    return adjusted_weight * speed_factor
  end

  -- Step 3: Point score and delta (if enabled)
  if use_delta then
    -- Apply point_score if recommend enabled
    if knp_point_score and knp_recommend_enabled ~= "0" then
      if strict_priority then
        -- STRICT PRIORITY: Maximum impact (prioritize quality over speed)
        -- High point_score (0.8-1.0) = reduce weight significantly (0.2-0.5x)
        -- Low point_score (0.0-0.5) = increase weight significantly (1.5-2.5x)
        local point_factor = 2.5 - (knp_point_score * 2.0)
        adjusted_weight = adjusted_weight * point_factor
      else
        -- FLEXIBLE PRIORITY: Moderate impact (balance quality and speed)
        -- Point score influences but speed can override if convenient
        local point_factor = 1.8 - (knp_point_score * 1.2)
        adjusted_weight = adjusted_weight * point_factor
      end
    end
    
    -- Apply delta (additive adjustment)
    if strict_priority then
      adjusted_weight = adjusted_weight + (knp_delta * 2.0)  -- Strong delta impact
    else
      adjusted_weight = adjusted_weight + knp_delta  -- Moderate delta impact
    end
  end

  -- Step 4: Speed adjustment (based on priority mode)
  if strict_priority then
    -- STRICT: Minimal speed influence (quality > speed)
    local speed_factor = 1.0 - ((speed - 25) / 150)  -- Very minimal
    adjusted_weight = adjusted_weight * speed_factor
  else
    -- FLEXIBLE: Moderate speed influence (speed can override if convenient)
    local speed_factor = 1.0 - ((speed - 25) / 60)  -- Stronger speed influence
    adjusted_weight = adjusted_weight * speed_factor
  end

  return adjusted_weight
end

function process_way(profile, way, result, relations)
  local highway = way:get_value_by_key("highway")
  local name = way:get_value_by_key("name")
  local ref = way:get_value_by_key("ref")

  if not highway then
    return
  end

  -- Motorbikes CANNOT use motorways in Vietnam
  if highway == "motorway" or highway == "motorway_link" then
    return
  end

  local maxspeed = tonumber(way:get_value_by_key("maxspeed"))
  local custom_weight = tonumber(way:get_value_by_key("custom_weight"))
  local traffic_level = way:get_value_by_key("traffic_level")
  local congestion_score = tonumber(way:get_value_by_key("congestion_score"))

  -- SHIPPER FEEDBACK: Critical for Vietnam routing!
  local shipper_score = tonumber(way:get_value_by_key("shipper_score")) or 1.0

  -- KNP OVERRIDES: Dynamic routing adjustments
  local knp_block_level = way:get_value_by_key("knp:block_level")
  local knp_delta = tonumber(way:get_value_by_key("knp:delta")) or 0
  local knp_point_score = tonumber(way:get_value_by_key("knp:point_score"))
  local knp_recommend_enabled = way:get_value_by_key("knp:recommend_enabled")
  local knp_soft_penalty = tonumber(way:get_value_by_key("knp:soft_penalty")) or 2.0
  local knp_min_penalty = tonumber(way:get_value_by_key("knp:min_penalty")) or 5.0

  -- Handle blocking (highest priority - ALWAYS applies)
  if knp_block_level == "hard" then
    return  -- Hard block: road is completely inaccessible
  end

  -- Vietnam motorbike speed map (realistic)
  local speed_map = {
    trunk = 60,           -- Quốc lộ
    trunk_link = 40,
    primary = 50,         -- Đường chính
    primary_link = 30,
    secondary = 45,       -- Đường cấp 2
    secondary_link = 25,
    tertiary = 40,        -- Đường cấp 3
    tertiary_link = 20,
    residential = 30,     -- Đường dân cư
    service = 20,         -- Đường phụ
    unclassified = 25,    -- Đường nhỏ
    living_street = 15,   -- Đường nội bộ
    pedestrian = 10       -- Đường đi bộ (can ride slow)
  }

  local speed = speed_map[highway] or 25

  -- Respect maxspeed but don't exceed bike limits
  if maxspeed and maxspeed > 0 then
    speed = math.min(speed, maxspeed)
  end

  -- Apply traffic conditions
  if traffic_level then
    local traffic_multiplier = 1.0
    if traffic_level == "FREE_FLOW" then
      traffic_multiplier = 1.1
    elseif traffic_level == "NORMAL" then
      traffic_multiplier = 1.0
    elseif traffic_level == "SLOW" then
      traffic_multiplier = 0.7
    elseif traffic_level == "CONGESTED" then
      traffic_multiplier = 0.5   -- Bikes handle congestion slightly better
    elseif traffic_level == "BLOCKED" then
      traffic_multiplier = 0.2   -- Still some weaving ability
    end

    if congestion_score and congestion_score > 0 then
      local congestion_multiplier = math.max(0.2, 1.0 - (congestion_score / 100))
      traffic_multiplier = traffic_multiplier * congestion_multiplier
    end

    speed = speed * traffic_multiplier
  end

  -- VIETNAM MOTORBIKE: More flexible oneway handling
  -- Motorbikes can often reverse on residential/service/unclassified roads
  local oneway = way:get_value_by_key("oneway")
  local allow_reverse = highway == "residential" or highway == "service" or 
                        highway == "unclassified" or highway == "living_street" or
                        highway == "tertiary" or highway == "tertiary_link"
  
  if oneway == "yes" or oneway == "1" or oneway == "true" then
    if allow_reverse then
      -- Allow reverse but with penalty (motorbikes do this in Vietnam)
      result.forward_mode = mode.driving
      result.backward_mode = mode.driving  -- Allow but penalized by weight
    else
      -- Major roads: respect oneway
      result.forward_mode = mode.driving
      result.backward_mode = mode.inaccessible
    end
  elseif oneway == "-1" or oneway == "reverse" then
    if allow_reverse then
      result.forward_mode = mode.driving  -- Allow but penalized
      result.backward_mode = mode.driving
    else
      result.forward_mode = mode.inaccessible
      result.backward_mode = mode.driving
    end
  else
    -- Bidirectional
    result.forward_mode = mode.driving
    result.backward_mode = mode.driving
  end

  result.forward_speed = speed
  result.backward_speed = speed

  -- Set name for OSRM to use in instructions
  if name then
    result.name = name
  elseif ref then
    result.name = ref
  end

  -- Calculate weight using mode-specific logic
  local adjusted_weight = calculate_weight_adjustment(
    custom_weight,
    speed,
    shipper_score,
    knp_point_score,
    knp_delta,
    knp_recommend_enabled,
    profile.strict_priority,
    profile.use_delta,
    profile.is_base
  )

  -- Apply blocking penalties (always applies except base)
  if not profile.is_base then
    if knp_block_level == "soft" then
      adjusted_weight = adjusted_weight * knp_soft_penalty
    elseif knp_block_level == "min" then
      adjusted_weight = adjusted_weight * knp_min_penalty
    end
    
    -- Penalty for reverse on oneway (only on minor roads where we allow it)
    if allow_reverse and (oneway == "yes" or oneway == "1" or oneway == "true" or oneway == "-1") then
      adjusted_weight = adjusted_weight * 1.3  -- 30% penalty for going wrong way
    end
  end

  -- Ensure minimum positive weight
  adjusted_weight = math.max(0.1, adjusted_weight)

  -- Set result
  if custom_weight and custom_weight > 0 then
    local weight_duration = adjusted_weight * 60.0
    result.duration = weight_duration
    result.weight = adjusted_weight
  else
    -- Fallback: use speed-based calculation
    result.duration = 0  -- OSRM will calc from speed
    result.weight = 0    -- OSRM will calc from speed
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
  
  -- Vietnam motorbikes: U-turns are very easy and common
  if angle >= 160 and angle <= 200 then
    -- U-turn: very low penalty for motorbikes
    turn.duration = turn.duration + profile.u_turn_penalty
    turn.weight = turn.weight + profile.u_turn_penalty
  elseif angle >= 45 then
    -- Regular turn
    turn.duration = turn.duration + profile.turn_penalty
    turn.weight = turn.weight + profile.turn_penalty
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
 * Generate Lua profile for OSRM (car or motorbike)
 */
function generateLuaProfile(
  instanceName: string,
  vehicleType: 'car' | 'motorbike',
  mode: 'strict_priority_with_delta' | 'flexible_priority_with_delta' | 'strict_priority_no_delta' | 'flexible_priority_no_delta' | 'base' = 'base'
): string {
  console.log(`\n🔧 Generating custom ${vehicleType} Lua profile for ${instanceName} (${mode})...`);

  const outputDir = join(process.cwd(), 'osrm_data', instanceName);
  const luaFilePath = join(outputDir, `custom_${vehicleType}.lua`);

  const luaScript = vehicleType === 'motorbike'
    ? generateMotorbikeLuaScript(mode)
    : generateCarLuaScript();

  writeFileSync(luaFilePath, luaScript, 'utf-8');
  console.log(`   ✓ Generated Lua profile at ${luaFilePath} (mode: ${mode})`);
  return luaFilePath;
}

/**
 * Run OSRM processing using Docker
 */
async function runOSRMProcessing(instanceName: string, vehicleType: 'car' | 'motorbike'): Promise<void> {
  console.log(`\n⚙️  Running OSRM processing for ${instanceName}...`);

  const dataDir = join(process.cwd(), 'osrm_data', instanceName);
  const osmFile = 'network.osm.xml';
  const profileFile = `custom_${vehicleType}.lua`;
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
 * Calculate average shipper feedback score (0-1 scale)
 * Higher score = better road quality from shipper perspective
 */
function calculateShipperFeedbackScore(segments: any[]): number | null {
  const allScores: number[] = [];

  for (const segment of segments) {
    if (segment.user_feedback && segment.user_feedback.length > 0) {
      for (const feedback of segment.user_feedback) {
        if (typeof feedback.score === 'number') {
          // Normalize score to 0-1 range (assuming feedback.score is 1-5)
          const normalizedScore = feedback.score / 5.0;
          allScores.push(normalizedScore);
        }
      }
    }
  }

  if (allScores.length === 0) return null;

  return allScores.reduce((sum, score) => sum + score, 0) / allScores.length;
}

/**
 * Main function
 */
async function main() {
  console.log('='.repeat(70));
  console.log('🚀 OSRM Data Generator - 5 Routing Profiles');
  console.log('='.repeat(70));

  // Check if car generation is enabled
  const generateCar = process.env.GENERATE_OSRM_CAR === 'true' || process.env.GENERATE_OSRM_CAR === '1';
  
  // 5 instances for different routing strategies (motorbike)
  // Logic: 2x2x1 (priority_mode x delta x blocking) + 1 base = 5
  // priority_mode: strict (always follow priority) vs flexible (allow lower priority if convenient)
  // delta: with_delta (apply point_score/delta) vs no_delta (ignore AI recommendations)
  // blocking: always applies (1)
  const motorbikeInstances = [
    { name: 'osrm-strict-priority-with-delta', vehicle: 'motorbike' as const, mode: 'strict_priority_with_delta' as const },
    { name: 'osrm-flexible-priority-with-delta', vehicle: 'motorbike' as const, mode: 'flexible_priority_with_delta' as const },
    { name: 'osrm-strict-priority-no-delta', vehicle: 'motorbike' as const, mode: 'strict_priority_no_delta' as const },
    { name: 'osrm-flexible-priority-no-delta', vehicle: 'motorbike' as const, mode: 'flexible_priority_no_delta' as const },
    { name: 'osrm-base', vehicle: 'motorbike' as const, mode: 'base' as const },
  ];

  // Optional: Car instances (only if GENERATE_OSRM_CAR=true)
  const carInstances = generateCar ? [
    { name: 'osrm-car-balanced', vehicle: 'car' as const, mode: 'balanced' as const },
  ] : [];

  const instances = [...motorbikeInstances, ...carInstances];

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
      console.log(`Processing ${instance.name} (${instance.vehicle.toUpperCase()} - ${instance.mode.toUpperCase()})`);
      console.log('─'.repeat(70));

      // Step 1: Export to OSM XML
      await exportToOSMXML(instance.name, instance.mode);

      // Step 2: Generate Lua profile with mode
      generateLuaProfile(instance.name, instance.vehicle, instance.mode);

      // Step 3: Run OSRM processing
      await runOSRMProcessing(instance.name, instance.vehicle);

      console.log(`\n✅ ${instance.name} (${instance.vehicle} - ${instance.mode}) is ready!`);
    }

    console.log('\n' + '='.repeat(70));
    console.log(`🎉 All ${instances.length} OSRM instance(s) generated successfully!`);
    console.log('='.repeat(70));
    console.log('\n📍 Motorbike Instances (5 routing modes):');
    console.log('  1. osrm-strict-priority-with-delta    (port 5000) - Luôn ưu tiên theo độ ưu tiên, có áp dụng delta/point_score');
    console.log('  2. osrm-flexible-priority-with-delta  (port 5001) - Ưu tiên linh hoạt (cho phép đơn thấp nếu tiện đường), có delta/point_score');
    console.log('  3. osrm-strict-priority-no-delta      (port 5002) - Luôn ưu tiên theo độ ưu tiên, KHÔNG áp dụng delta/point_score');
    console.log('  4. osrm-flexible-priority-no-delta    (port 5003) - Ưu tiên linh hoạt, KHÔNG áp dụng delta/point_score');
    console.log('  5. osrm-base                          (port 5004) - Base OSRM logic (từ car.lua), chỉ shipper feedback');
    
    if (generateCar && carInstances.length > 0) {
      console.log('\n📍 Car Instances:');
      carInstances.forEach((inst, idx) => {
        console.log(`  ${idx + 1}. ${inst.name} - Car routing (balanced mode)`);
      });
    }
    
    console.log('\n🐳 Start OSRM containers:');
    console.log('  docker-compose up osrm-strict-priority-with-delta osrm-flexible-priority-with-delta osrm-strict-priority-no-delta osrm-flexible-priority-no-delta osrm-base');
    if (generateCar) {
      console.log('  # + car instances if enabled');
    }
    console.log('\n🚀 Or start all services:');
    console.log('  docker-compose up -d');
    console.log('\n🌐 API endpoints (Motorbike):');
    console.log('  • Strict-Priority-With-Delta:   /route/v1/motorbike/{coordinates}  (OSRM_STRICT_PRIORITY_WITH_DELTA_URL)');
    console.log('  • Flexible-Priority-With-Delta: /route/v1/motorbike/{coordinates}  (OSRM_FLEXIBLE_PRIORITY_WITH_DELTA_URL)');
    console.log('  • Strict-Priority-No-Delta:     /route/v1/motorbike/{coordinates}  (OSRM_STRICT_PRIORITY_NO_DELTA_URL)');
    console.log('  • Flexible-Priority-No-Delta:   /route/v1/motorbike/{coordinates}  (OSRM_FLEXIBLE_PRIORITY_NO_DELTA_URL)');
    console.log('  • Base:                         /route/v1/motorbike/{coordinates}  (OSRM_BASE_URL)');
    if (generateCar) {
      console.log('\n🌐 API endpoints (Car):');
      console.log('  • Car Balanced:  /route/v1/car/{coordinates}  (OSRM_INSTANCE_1_URL)');
    }
    console.log('\n💡 To generate car data, set GENERATE_OSRM_CAR=true');
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
