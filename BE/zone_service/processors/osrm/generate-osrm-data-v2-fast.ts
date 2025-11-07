/**
 * OSRM Data Generator V2 - Fast (Multi-threaded)
 * 
 * Optimizations:
 * - Fetch DB data once (shared across all models)
 * - Build XML once, clone to instances (fast file copy)
 * - Parallel OSRM processing (4 workers for 4 models)
 * 
 * Expected speedup: 4x faster than sequential
 */

import { PrismaClient } from '@prisma/client';
import { writeFileSync, existsSync, mkdirSync, copyFileSync } from 'fs';
import { join } from 'path';
import { Worker } from 'worker_threads';
import { promisify } from 'util';
import { exec } from 'child_process';

const execAsync = promisify(exec);
const prisma = new PrismaClient();

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

interface RoadNetworkData {
	roads: any[];
	nodes: any[];
	segments: any[];
	userFeedback: Map<string, number>;
	trafficValue: Map<string, number>;
}

/**
 * Fetch all road network data (once, shared)
 */
async function fetchRoadNetworkData(): Promise<RoadNetworkData> {
	console.log('\n📊 Fetching road network data from database...');
	console.log('─'.repeat(70));

	const [roads, nodes] = await Promise.all([
		prisma.roads.findMany(),
		prisma.road_nodes.findMany(),
	]);

	console.log(`   ✓ Found ${roads.length} roads, ${nodes.length} nodes`);

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

	const userFeedback = new Map<string, number>();
	for (const segment of segments) {
		const rating = calculateUserRating(segment);
		if (rating !== null) {
			userFeedback.set(segment.segment_id, rating);
		}
	}

	const trafficValue = new Map<string, number>();
	for (const segment of segments) {
		const traffic = calculateBlockingStatus(segment);
		trafficValue.set(segment.segment_id, traffic);
	}

	console.log(`   ✓ Calculated ratings for ${userFeedback.size} segments`);
	console.log(`   ✓ Calculated traffic for ${trafficValue.size} segments`);

	return { roads, nodes, segments, userFeedback, trafficValue };
}

function calculateUserRating(segment: any): number | null {
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

function calculateBlockingStatus(segment: any): number {
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
 * Export to OSM XML (once)
 */
async function exportToOSMXML(data: RoadNetworkData): Promise<string> {
	console.log('\n📦 Exporting road network to OSM XML (unified)...');

	const outputDir = join(process.cwd(), 'osrm_data', '_shared');
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
		const roadType = road ? mapRoadTypeToOSM(road.road_type) : 'unclassified';
		tags.push({ k: 'highway', v: roadType });

		if (segment.name) {
			tags.push({ k: 'name', v: escapeXML(segment.name) });
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

	console.log(`   Built ${ways.length} ways with ${nodeCoords.size} nodes`);

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
 * Clone XML to all instances (fast file copy)
 */
async function cloneXMLToInstances(masterXMLPath: string): Promise<void> {
	console.log('\n📋 Cloning XML to all instances...');
	console.log('─'.repeat(70));

	for (const model of MODELS) {
		const instanceDir = join(process.cwd(), 'osrm_data', model.name);
		if (!existsSync(instanceDir)) {
			mkdirSync(instanceDir, { recursive: true });
		}

		const targetXMLPath = join(instanceDir, 'network.osm.xml');
		copyFileSync(masterXMLPath, targetXMLPath);

		// Generate Lua profile
		const luaScript = generateLuaProfile(model);
		const luaPath = join(instanceDir, 'custom_bicycle.lua');
		writeFileSync(luaPath, luaScript, 'utf-8');

		console.log(`   ✓ ${model.name} ready (XML + Lua)`);
	}
}

function generateLuaProfile(config: ModelConfig): string {
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
 * Process OSRM instance (parallel workers)
 */
async function processOSRMInstance(config: ModelConfig): Promise<void> {
	const dataDir = join(process.cwd(), 'osrm_data', config.name);
	const osmFile = 'network.osm.xml';
	const profileFile = 'custom_bicycle.lua';
	const osrmFile = 'network.osrm';

	console.log(`   Processing ${config.name}...`);

	await execAsync(
		`docker run --rm -v "${dataDir}:/data" osrm/osrm-backend:latest osrm-extract -p /data/${profileFile} /data/${osmFile}`,
		{ maxBuffer: 100 * 1024 * 1024 }
	);

	await execAsync(
		`docker run --rm -v "${dataDir}:/data" osrm/osrm-backend:latest osrm-partition /data/${osrmFile}`,
		{ maxBuffer: 100 * 1024 * 1024 }
	);

	await execAsync(
		`docker run --rm -v "${dataDir}:/data" osrm/osrm-backend:latest osrm-customize /data/${osrmFile}`,
		{ maxBuffer: 100 * 1024 * 1024 }
	);

	console.log(`   ✅ ${config.name} completed`);
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
 * Main function - Fast parallel processing
 */
async function main() {
	console.log('='.repeat(70));
	console.log('🚀 OSRM Data Generator V2 - FAST (Multi-threaded)');
	console.log('='.repeat(70));

	const startTime = Date.now();

	try {
		// Step 1: Fetch data once (shared)
		const roadNetworkData = await fetchRoadNetworkData();

		// Step 2: Build master XML once
		const masterXMLPath = await exportToOSMXML(roadNetworkData);

		// Step 3: Clone XML to all instances (fast)
		await cloneXMLToInstances(masterXMLPath);

		// Step 4: Parallel OSRM processing (4 models in parallel)
		console.log('\n⚙️  Processing OSRM instances in parallel...');
		console.log('─'.repeat(70));

		await Promise.all(MODELS.map(model => processOSRMInstance(model)));

		const elapsed = ((Date.now() - startTime) / 1000).toFixed(1);

		console.log('\n' + '='.repeat(70));
		console.log(`🎉 All ${MODELS.length} OSRM models generated in ${elapsed}s!`);
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
