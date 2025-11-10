/**
 * OSRM Data Generator V2
 * 
 * Simplified Architecture:
 * - Application layer handles priority/waypoint sorting (routing.service.ts)
 * - OSRM handles routing through pre-sorted waypoints
 * - Lua profiles define parameters only, no complex logic
 * - XML data built once, cloned to all model instances
 * 
 * Formula Design:
 * - Rating (user feedback): affects WEIGHT (cost to traverse)
 *   weight = base_weight × (2.0 - rating_score)
 * - Blocking (traffic): affects SPEED (travel time)
 *   speed = base_speed × (traffic_value / 5.0)
 */

import { PrismaClient } from '@prisma/client';
import { writeFileSync, existsSync, mkdirSync, copyFileSync } from 'fs';
import { join } from 'path';
import { exec } from 'child_process';
import { promisify } from 'util';

const execAsync = promisify(exec);
const prisma = new PrismaClient();

/**
 * Model configurations
 */
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

/**
 * Road network data structure
 */
interface RoadNetworkData {
	roads: any[];
	nodes: any[];
	segments: any[];
	userFeedback: Map<string, number>; // segment_id -> rating (0-1 scale)
	trafficValue: Map<string, number>; // segment_id -> traffic (0-5 scale)
}

/**
 * Fetch all road network data from database (once)
 */
async function fetchRoadNetworkData(): Promise<RoadNetworkData> {
	console.log('\n📊 Fetching road network data from database...');
	console.log('─'.repeat(70));

	// Load basic data
	console.log('   Loading roads and nodes...');
	const [roads, nodes] = await Promise.all([
		prisma.roads.findMany(),
		prisma.road_nodes.findMany(),
	]);

	console.log(`   ✓ Found ${roads.length} roads, ${nodes.length} nodes`);

	// Load segments with user feedback and traffic
	console.log('   Loading segments with feedback and traffic...');
	const BATCH_SIZE = 20000;
	const segments: any[] = [];
	let offset = 0;

	while (true) {
		const batch = await prisma.road_segments.findMany({
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

	console.log(`   ✓ Loaded ${segments.length} segments`);

	// Calculate user feedback ratings (0-1 scale)
	console.log('   Calculating user feedback ratings...');
	const userFeedback = new Map<string, number>();

	for (const segment of segments) {
		const rating = calculateUserRating(segment);
		if (rating !== null) {
			userFeedback.set(segment.segment_id, rating);
		}
	}

	console.log(`   ✓ Calculated ratings for ${userFeedback.size} segments`);

	// Calculate traffic blocking status (0-5 scale)
	console.log('   Calculating traffic blocking values...');
	const trafficValue = new Map<string, number>();

	for (const segment of segments) {
		const traffic = calculateBlockingStatus(segment);
		trafficValue.set(segment.segment_id, traffic);
	}

	console.log(`   ✓ Calculated traffic for ${trafficValue.size} segments`);

	console.log('✅ Road network data fetched successfully');
	console.log('─'.repeat(70));

	return {
		roads,
		nodes,
		segments,
		userFeedback,
		trafficValue,
	};
}

/**
 * Calculate user rating from feedback (0-1 scale)
 * Returns 1.0 (neutral) if no feedback
 */
function calculateUserRating(segment: any): number | null {
	if (!segment.user_feedback || segment.user_feedback.length === 0) {
		return null; // No rating data
	}

	const scores = segment.user_feedback
		.map((f: any) => f.score)
		.filter((s: any) => typeof s === 'number' && s >= 1 && s <= 5);

	if (scores.length === 0) return null;

	// Average score (1-5) normalized to 0-1
	const avgScore = scores.reduce((sum: number, s: number) => sum + s, 0) / scores.length;
	return avgScore / 5.0; // 1→0.2, 3→0.6, 5→1.0
}

/**
 * Calculate blocking status from traffic conditions (0-5 scale)
 * Returns 5.0 (free flow) if no traffic data
 */
function calculateBlockingStatus(segment: any): number {
	const traffic = segment.traffic_conditions?.[0];
	if (!traffic) return 5.0; // Default: free flow

	// Map current enum to 0-5 scale (will be replaced by direct value later)
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
 * Export road network to OSM XML (once, with all data)
 */
async function exportToOSMXML(data: RoadNetworkData): Promise<string> {
	console.log('\n📦 Exporting road network to OSM XML (unified)...');

	const outputDir = join(process.cwd(), 'osrm_data', '_shared');
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

	function ensureNodeForCoord(lat: number, lon: number): number {
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
	}

	// Build ways
	console.log('   Building ways from segments...');
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

		// Parse geometry
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

		// Build node refs
		for (const [lon, lat] of coords) {
			nodeRefs.push(ensureNodeForCoord(lat, lon));
		}

		if (nodeRefs.length < 2) continue;

		// Build tags
		const road = roads.find((r) => r.road_id === segment.road_id);
		const roadType = road ? mapRoadTypeToOSM(road.road_type) : 'unclassified';
		tags.push({ k: 'highway', v: roadType });

		if (segment.name) {
			tags.push({ k: 'name', v: escapeXML(segment.name) });
		}

		// Add user rating (0-1 scale) - for weight modifier
		const rating = userFeedback.get(segment.segment_id);
		if (rating !== undefined) {
			tags.push({ k: 'user_rating', v: rating.toFixed(3) });
		}

		// Add traffic value (0-5 scale) - for speed modifier
		const traffic = trafficValue.get(segment.segment_id);
		if (traffic !== undefined) {
			tags.push({ k: 'traffic_value', v: traffic.toFixed(1) });
		}

		// Maxspeed
		if (segment.max_speed) {
			tags.push({ k: 'maxspeed', v: String(segment.max_speed) });
		}

		// Oneway
		const isOneWay = segment.one_way || (road && road.one_way);
		if (isOneWay) {
			tags.push({ k: 'oneway', v: 'yes' });
			ways.push({ id: wayId, nodeRefs, tags });
		} else {
			// Bidirectional: create forward + backward
			ways.push({ id: wayId, nodeRefs, tags });
			const reverseWayId = wayIdCounter++;
			ways.push({ id: reverseWayId, nodeRefs: [...nodeRefs].reverse(), tags });
		}
	}

	console.log(`   Built ${ways.length} ways with ${nodeCoords.size} nodes`);

	// Write XML
	console.log('   Writing OSM XML...');
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
	console.log(`   ✓ Saved to ${osmFilePath}`);

	return osmFilePath;
}

/**
 * Generate Lua profile based on model config
 * Based on bicycle.lua with VN motorbike adjustments
 */
function generateLuaProfile(config: ModelConfig): string {
	const { useRating, useBlocking } = config;

	return `-- OSRM Bicycle Profile V2 (VN Motorbike Optimized)
-- Generated by Zone Service V2
-- Model: ${config.name}
-- Config: rating=${useRating}, blocking=${useBlocking}

api_version = 4

function setup()
  return {
    properties = {
      max_speed_for_map_matching = 40/3.6,  -- 40 km/h
      use_turn_restrictions = false,
      continue_straight_at_waypoint = true,
      weight_name = 'routability',
    },
    
    default_mode = mode.cycling,
    default_speed = 35,  -- VN motorbike: 35 km/h (realistic Saigon speed)
    oneway_handling = false,  -- VN motorbikes often ignore oneways
    turn_penalty = 4,  -- VN motorbikes turn easier
    u_turn_penalty = 5,  -- Very easy for motorbikes
    
    -- Model config flags
    use_rating = ${useRating},
    use_blocking = ${useBlocking},
  }
end

function process_way(profile, way, result, relations)
  local highway = way:get_value_by_key("highway")
  if not highway then
    return
  end

  -- Speed map for VN motorbike
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
  
  -- Apply maxspeed if available
  local maxspeed = tonumber(way:get_value_by_key("maxspeed"))
  if maxspeed and maxspeed > 0 then
    speed = math.min(speed, maxspeed * 0.9)  -- Motorbikes slightly under car speed
  end

  -- Apply traffic blocking (if enabled) - affects SPEED
  if profile.use_blocking then
    local traffic_value = tonumber(way:get_value_by_key("traffic_value"))
    if traffic_value then
      -- Formula: speed = base_speed × (traffic_value / 5.0)
      -- traffic_value: 5 (free) → 0 (blocked)
      local traffic_multiplier = traffic_value / 5.0
      speed = speed * traffic_multiplier
    end
  end

  -- Handle oneway (VN motorbikes are flexible)
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

  -- Set name
  local name = way:get_value_by_key("name")
  if name then
    result.name = name
  end

  -- Calculate weight (duration-based, will be modified by rating)
  result.duration = 1  -- OSRM will calculate from speed
  result.weight = 1

  -- Apply user rating (if enabled) - affects WEIGHT
  if profile.use_rating then
    local user_rating = tonumber(way:get_value_by_key("user_rating"))
    if user_rating then
      -- Formula: weight = base_weight × (2.0 - rating_score)
      -- rating_score: 1.0 → weight unchanged
      --               0.5 → weight × 1.5
      --               0.0 → weight × 2.0 (avoid bad roads)
      local rating_multiplier = 2.0 - user_rating
      result.weight = result.weight * rating_multiplier
    end
  end
end

function process_node(profile, node, result, relations)
  -- Handle traffic signals
  local highway = node:get_value_by_key("highway")
  if highway == "traffic_signals" then
    result.traffic_lights = true
  end
end

function process_turn(profile, turn)
  -- VN motorbike turn penalties (reduced from bicycle)
  local angle = math.abs(turn.angle)
  
  if angle >= 160 then
    -- U-turn: very easy for VN motorbikes
    turn.duration = turn.duration + profile.u_turn_penalty
  elseif angle >= 45 then
    -- Regular turn
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

/**
 * Clone XML and generate Lua for a model instance
 */
async function setupModelInstance(config: ModelConfig, sharedXmlPath: string): Promise<void> {
	const instanceDir = join(process.cwd(), 'osrm_data', config.name);
	if (!existsSync(instanceDir)) {
		mkdirSync(instanceDir, { recursive: true });
	}

	// Copy XML from shared
	const instanceXmlPath = join(instanceDir, 'network.osm.xml');
	copyFileSync(sharedXmlPath, instanceXmlPath);
	console.log(`   ✓ Copied XML to ${config.name}`);

	// Generate Lua profile
	const luaScript = generateLuaProfile(config);
	const luaPath = join(instanceDir, 'custom_bicycle.lua');
	writeFileSync(luaPath, luaScript, 'utf-8');
	console.log(`   ✓ Generated Lua profile for ${config.name}`);
}

/**
 * Run OSRM processing for a model instance
 */
async function processOSRMInstance(config: ModelConfig): Promise<void> {
	console.log(`\n⚙️  Processing OSRM instance: ${config.name}`);

	const dataDir = join(process.cwd(), 'osrm_data', config.name);
	const osmFile = 'network.osm.xml';
	const profileFile = 'custom_bicycle.lua';
	const osrmFile = 'network.osrm';

	try {
		// Extract
		console.log('   Running osrm-extract...');
		await execAsync(
			`docker run --rm -v "${dataDir}:/data" osrm/osrm-backend:latest osrm-extract -p /data/${profileFile} /data/${osmFile}`,
			{ maxBuffer: 100 * 1024 * 1024 }
		);

		// Partition
		console.log('   Running osrm-partition...');
		await execAsync(
			`docker run --rm -v "${dataDir}:/data" osrm/osrm-backend:latest osrm-partition /data/${osrmFile}`,
			{ maxBuffer: 100 * 1024 * 1024 }
		);

		// Customize
		console.log('   Running osrm-customize...');
		await execAsync(
			`docker run --rm -v "${dataDir}:/data" osrm/osrm-backend:latest osrm-customize /data/${osrmFile}`,
			{ maxBuffer: 100 * 1024 * 1024 }
		);

		console.log(`   ✅ ${config.name} completed`);
	} catch (error: any) {
		console.error(`   ❌ ${config.name} failed: ${error.message}`);
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

/**
 * Main function
 */
async function main() {
	console.log('='.repeat(70));
	console.log('🚀 OSRM Data Generator V2 - Simplified Architecture');
	console.log('='.repeat(70));

	try {
		// Step 1: Fetch data from database (once)
		const roadNetworkData = await fetchRoadNetworkData();

		// Step 2: Export to OSM XML (once, with all data)
		const sharedXmlPath = await exportToOSMXML(roadNetworkData);

		// Step 3: Setup all model instances (clone XML + generate Lua)
		console.log('\n📋 Setting up model instances...');
		console.log('─'.repeat(70));
		for (const config of MODELS) {
			await setupModelInstance(config, sharedXmlPath);
		}

		// Step 4: Process all OSRM instances
		console.log('\n⚙️  Processing OSRM instances...');
		console.log('─'.repeat(70));
		for (const config of MODELS) {
			await processOSRMInstance(config);
		}

		// Summary
		console.log('\n' + '='.repeat(70));
		console.log(`🎉 All ${MODELS.length} OSRM models generated successfully!`);
		console.log('='.repeat(70));
		console.log('\n📍 Generated Models:');
		for (const config of MODELS) {
			console.log(`  • ${config.name}: ${config.description}`);
		}
		console.log('\n🐳 Start OSRM containers with docker-compose');
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
