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
 * Fetch all road network data from database (called once)
 */
interface RoadNetworkData {
	roads: any[];
	nodes: any[];
	segments: any[];
	trafficConditions: any[];
	overrides: any[];
	overridesBySegmentId: Map<string, any>;
	overridesByOsmWayId: Map<string, any>;
}

async function fetchRoadNetworkData(): Promise<RoadNetworkData> {
	console.log('\n📊 Fetching road network data from database...');
	console.log('─'.repeat(70));

	// Load basic data first
	console.log('   Loading roads and nodes...');
	const [roads, nodes] = await Promise.all([
		prisma.roads.findMany(),
		prisma.road_nodes.findMany(),
	]);

	console.log(`   ✓ Found ${roads.length} roads, ${nodes.length} nodes`);

	// Load segments in batches (MySQL prepared statement limit workaround)
	console.log('   Loading segments in batches...');
	const BATCH_SIZE = 20000;
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
							gte: new Date(),
						},
					},
					orderBy: {
						source_timestamp: 'desc',
					},
					take: 1,
				},
				user_feedback: {
					orderBy: {
						created_at: 'desc',
					},
					take: 10,
				},
			},
		});

		if (segmentBatch.length === 0) break;

		segments.push(...segmentBatch);
		offset += BATCH_SIZE;
		if (batch % 10 === 0 || segmentBatch.length < BATCH_SIZE) {
			console.log(
				`   ✓ Loaded batch ${batch}: ${segments.length} segments total`
			);
		}

		if (segmentBatch.length < BATCH_SIZE) break; // Last batch
	}

	// Traffic conditions (not used in current logic, but kept for compatibility)
	console.log('   Loading traffic conditions...');
	const trafficConditions = await prisma.traffic_conditions.findMany({
		where: {
			expires_at: {
				gte: new Date(),
			},
		},
		take: 1000, // Limit for performance
	});

	// Load road overrides for dynamic routing adjustments
	console.log('   Loading road overrides...');
	const overrides = await prisma.road_overrides.findMany();

	// Build lookup maps for fast access
	const overridesBySegmentId = new Map(
		overrides.filter((o) => o.segment_id).map((o) => [o.segment_id, o])
	);
	const overridesByOsmWayId = new Map(
		overrides
			.filter((o) => o.osm_way_id)
			.map((o) => [o.osm_way_id?.toString(), o])
	);

	console.log(`   ✓ Found ${segments.length} segments`);
	console.log(
		`   ✓ Found ${trafficConditions.length} active traffic conditions`
	);
	console.log(`   ✓ Found ${overrides.length} road overrides`);

	// Calculate and update delta_weight from user feedback (once for all instances)
	console.log('   Calculating delta weights from user feedback...');
	const deltaWeightUpdates: Array<{
		segment_id: string;
		delta_weight: number;
	}> = [];

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
				delta_weight,
			});
		}
	}

	// Batch update delta_weight in database (once for all instances)
	if (deltaWeightUpdates.length > 0) {
		console.log(
			`   Updating ${deltaWeightUpdates.length} segment delta weights...`
		);
		const DELTA_BATCH_SIZE = 1000;

		for (let i = 0; i < deltaWeightUpdates.length; i += DELTA_BATCH_SIZE) {
			const batch = deltaWeightUpdates.slice(i, i + DELTA_BATCH_SIZE);
			const caseStatements = batch
				.map((u) => `WHEN '${u.segment_id}' THEN ${u.delta_weight}`)
				.join(' ');
			const segmentIds = batch.map((u) => `'${u.segment_id}'`).join(',');

			await prisma.$executeRawUnsafe(`
        UPDATE road_segments
        SET delta_weight = CASE segment_id ${caseStatements} END
        WHERE segment_id IN (${segmentIds})
      `);
		}
		console.log(
			`   ✓ Updated ${deltaWeightUpdates.length} segment delta weights`
		);
	} else {
		console.log(
			`   ℹ️  No user feedback found, skipping delta weight update`
		);
	}

	// Update segments with new delta_weight values
	for (const segment of segments) {
		const update = deltaWeightUpdates.find(
			(u) => u.segment_id === segment.segment_id
		);
		if (update) {
			segment.delta_weight = update.delta_weight;
		}
	}

	console.log('✅ Road network data fetched successfully');
	console.log('─'.repeat(70));

	return {
		roads,
		nodes,
		segments,
		trafficConditions,
		overrides,
		overridesBySegmentId,
		overridesByOsmWayId,
	};
}

/**
 * Export road network to OSM XML format (using pre-fetched data)
 */
async function exportToOSMXML(
	instanceName: string,
	mode:
		| 'strict_priority_with_delta'
		| 'flexible_priority_with_delta'
		| 'strict_priority_no_delta'
		| 'flexible_priority_no_delta'
		| 'base' = 'base',
	data: RoadNetworkData
): Promise<string> {
	console.log(
		`\n📦 Exporting road network to OSM XML for ${instanceName} (mode: ${mode})...`
	);

	const outputDir = join(process.cwd(), 'osrm_data', instanceName);
	if (!existsSync(outputDir)) {
		mkdirSync(outputDir, { recursive: true });
	}

	const osmFilePath = join(outputDir, 'network.osm.xml');

	// Use pre-fetched data
	const {
		roads,
		nodes,
		segments,
		overridesBySegmentId,
		overridesByOsmWayId,
	} = data;

	// Create mapping from string IDs to numeric IDs
	const nodeIdMap = new Map<string, number>();
	const roadIdMap = new Map<string, number>();

	nodes.forEach((node, index) => {
		const numericId =
			node.osm_id && !isNaN(Number(node.osm_id))
				? Number(node.osm_id)
				: 1000000 + index;
		nodeIdMap.set(node.node_id, numericId);
	});

	roads.forEach((road, index) => {
		const numericId =
			road.osm_id && !isNaN(Number(road.osm_id))
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
			nodeId = dbNode
				? Math.abs(hashString(dbNode.node_id))
				: 10_000_000 + coordToNodeId.size;
		}

		coordToNodeId.set(key, nodeId);
		nodeCoords.set(nodeId, { lat, lon });
		return nodeId;
	}

	function hashString(str: string): number {
		let hash = 0;
		for (let i = 0; i < str.length; i++) {
			const char = str.charCodeAt(i);
			hash = (hash << 5) - hash + char;
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
			} else if (
				segment.geometry &&
				typeof segment.geometry === 'object' &&
				'coordinates' in segment.geometry
			) {
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
		const road = roads.find((r) => r.road_id === segment.road_id);
		const roadType = road
			? mapRoadTypeToOSM(road.road_type)
			: 'unclassified';
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
			tags.push({
				k: 'maxspeed',
				v: String(segment.max_speed || road!.max_speed),
			});
		}

		if (road && road.lanes) {
			tags.push({ k: 'lanes', v: String(road.lanes) });
		}

		// Calculate final weight: base_weight + delta_weight (from user feedback)
		const baseWeight = segment.base_weight ?? 1.0;
		const deltaWeight = segment.delta_weight ?? 0.0;
		const segWeight = baseWeight + deltaWeight;
		tags.push({ k: 'custom_weight', v: segWeight.toFixed(2) });

		const trafficCondition =
			segment.traffic_conditions && segment.traffic_conditions[0];
		if (trafficCondition) {
			tags.push({
				k: 'traffic_level',
				v: trafficCondition.traffic_level,
			});
			if (trafficCondition.congestion_score > 0) {
				tags.push({
					k: 'congestion_score',
					v: trafficCondition.congestion_score.toFixed(1),
				});
			}
		}

		const shipperScore = calculateShipperFeedbackScore([segment]);
		if (shipperScore !== null) {
			tags.push({ k: 'shipper_score', v: shipperScore.toFixed(2) });
		}

		// Add dynamic weight modifiers: priority_factor, rating, flow
		// These can come from overrides or be calculated from existing data
		const override =
			overridesBySegmentId.get(segment.segment_id) ||
			(segment.osm_way_id
				? overridesByOsmWayId.get(segment.osm_way_id.toString())
				: null);

		// Priority factor: destination or stop priority (default: 1.0)
		// Higher priority_factor = lower cost to reach (inverse relationship)
		// Reduced range: 0.8-1.2 (instead of 0.5-2.5) to make priority differences more subtle
		// This allows flexible mode to consider lower priority orders if convenient
		const priorityFactor =
			override?.point_score !== null &&
			override?.point_score !== undefined
				? 1.0 - (override.point_score - 0.5) * 0.4 // Convert 0-1 point_score to 0.8-1.2 priority_factor
				: 1.0; // Default: no priority adjustment
		tags.push({ k: 'priority_factor', v: priorityFactor.toFixed(2) });

		// Rating: road segment quality (-5 to +5, default: 0)
		// Positive rating = better road = higher speed
		// Negative rating = worse road = lower speed
		const rating =
			shipperScore !== null
				? (shipperScore - 0.5) * 10 // Convert 0-1 shipper_score to -5 to +5 rating
				: 0; // Default: neutral rating
		tags.push({ k: 'rating', v: rating.toFixed(1) });

		// Flow: traffic openness (0 to 5, default: 5)
		// Higher flow = less congestion = higher speed
		// Reuse trafficCondition from above (line 318)
		let flow = 5.0; // Default: maximum flow (no congestion)
		if (trafficCondition) {
			// Map traffic levels to flow (0-5 scale)
			const flowMap: Record<string, number> = {
				FREE_FLOW: 5.0,
				NORMAL: 4.0,
				SLOW: 2.5,
				CONGESTED: 1.0,
				BLOCKED: 0.0,
			};
			flow = flowMap[trafficCondition.traffic_level] || 5.0;

			// Adjust flow based on congestion_score (0-100)
			if (trafficCondition.congestion_score > 0) {
				const congestionFlow =
					5.0 * (1.0 - trafficCondition.congestion_score / 100);
				flow = Math.min(flow, congestionFlow); // Use the more restrictive value
			}
		}
		tags.push({ k: 'flow', v: flow.toFixed(1) });

		// Mode-specific override application
		if (mode !== 'base' && override) {
			// Block level (always applied except in base mode)
			if (override.block_level && override.block_level !== 'none') {
				tags.push({ k: 'knp:block_level', v: override.block_level });
			}

			// Recommendation enabled flag
			const recommendEnabled = override.recommend_enabled !== false;

			// Delta weight adjustment (if enabled and has delta)
			if (override.delta !== null && override.delta !== undefined) {
				tags.push({ k: 'knp:delta', v: override.delta.toFixed(3) });
			}

			// Point score (if enabled and has point_score)
			if (
				override.point_score !== null &&
				override.point_score !== undefined
			) {
				tags.push({
					k: 'knp:point_score',
					v: override.point_score.toFixed(3),
				});
			}

			// Recommendation enabled flag
			tags.push({
				k: 'knp:recommend_enabled',
				v: recommendEnabled ? '1' : '0',
			});

			// Custom penalty factors (if set)
			if (
				override.soft_penalty_factor !== null &&
				override.soft_penalty_factor !== undefined
			) {
				tags.push({
					k: 'knp:soft_penalty',
					v: override.soft_penalty_factor.toFixed(2),
				});
			}
			if (
				override.min_penalty_factor !== null &&
				override.min_penalty_factor !== undefined
			) {
				tags.push({
					k: 'knp:min_penalty',
					v: override.min_penalty_factor.toFixed(2),
				});
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

	console.log(
		`   Built ${ways.length} ways (${skippedSegments} segments skipped)`
	);
	console.log(`   Created ${nodeCoords.size} unique nodes from geometry`);

	// Log bidirectional stats
	const bidirectionalCount = segments.filter(
		(s) =>
			!s.one_way && !roads.find((r) => r.road_id === s.road_id)?.one_way
	).length;
	const onewayCount = segments.length - bidirectionalCount;
	console.log(
		`   Bidirectional segments: ${bidirectionalCount} (exported as ${
			bidirectionalCount * 2
		} ways)`
	);
	console.log(
		`   One-way segments: ${onewayCount} (exported as ${onewayCount} ways)`
	);

	// STEP 2: Write XML in correct order: header → nodes → ways
	console.log('   Writing OSM XML...');
	let xml = '<?xml version="1.0" encoding="UTF-8"?>\n';
	xml += '<osm version="0.6" generator="zone-service">\n';
	xml +=
		'  <bounds minlat="10.3" minlon="106.3" maxlat="11.2" maxlon="107.0"/>\n';

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

	console.log(
		`   ✓ Exported ${ways.length} ways with ${nodeCoords.size} unique nodes`
	);
	console.log(`   ✓ Saved to ${osmFilePath}`);
	return osmFilePath;
}

/**
 * Generate Car Lua Script (based on standard OSRM car.lua with custom weights)
 * @param priorityWeight - Weight for priority_factor influence (default: 1.0)
 * @param ratingWeight - Weight for rating influence (default: 1.0)
 * @param flowWeight - Weight for flow influence (default: 1.0)
 */
function generateCarLuaScript(
	priorityWeight: number = 1.0,
	ratingWeight: number = 1.0,
	flowWeight: number = 1.0
): string {
	return `-- Car profile with dynamic weights
-- Based on standard OSRM car.lua with injected dynamic modifiers
-- Generated by Zone Service
-- Supports dynamic modifiers: priority_factor, rating, flow

api_version = 4

Set = require('lib/set')
Sequence = require('lib/sequence')
Handlers = require("lib/way_handlers")
Relations = require("lib/relations")
Obstacles = require("lib/obstacles")
find_access_tag = require("lib/access").find_access_tag
limit = require("lib/maxspeed").limit
Utils = require("lib/utils")
Measure = require("lib/measure")

-- Dynamic weight parameters (configurable)
local PARAMS = {
  PRIORITY_WEIGHT = ${priorityWeight},  -- Weight for priority_factor influence
  RATING_WEIGHT = ${ratingWeight},    -- Weight for rating influence
  FLOW_WEIGHT = ${flowWeight},      -- Weight for flow influence
}

function setup()
  return {
    properties = {
      max_speed_for_map_matching      = 180/3.6, -- 180kmph -> m/s
      -- For routing based on duration, but weighted for preferring certain roads
      weight_name                     = 'routability',
      -- For shortest duration without penalties for accessibility
      -- weight_name                     = 'duration',
      -- For shortest distance without penalties for accessibility
      -- weight_name                     = 'distance',
      process_call_tagless_node      = false,
      u_turn_penalty                 = 20,
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

    -- Size of the vehicle, to be limited by physical restriction of the way
    vehicle_height = 2.0, -- in meters, 2.0m is the height slightly above biggest SUVs
    vehicle_width = 1.9, -- in meters, ways with narrow tag are considered narrower than 2.2m

    -- Size of the vehicle, to be limited mostly by legal restriction of the way
    vehicle_length = 4.8, -- in meters, 4.8m is the length of large or family car
    vehicle_weight = 2000, -- in kilograms

    -- a list of suffixes to suppress in name change instructions. The suffixes also include common substrings of each other
    suffix_list = {
      'N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW', 'North', 'South', 'West', 'East', 'Nor', 'Sou', 'We', 'Ea'
    },

    barrier_whitelist = Set {
      'cattle_grid',
      'border_control',
      'toll_booth',
      'sally_port',
      'gate',
      'lift_gate',
      'no',
      'entrance',
      'height_restrictor',
      'arch'
    },

    access_tag_whitelist = Set {
      'yes',
      'motorcar',
      'motor_vehicle',
      'vehicle',
      'permissive',
      'designated',
      'hov'
    },

    access_tag_blacklist = Set {
      'no',
      'agricultural',
      'forestry',
      'emergency',
      'psv',
      'customers',
      'private',
      'delivery',
      'destination'
    },

    -- tags disallow access to in combination with highway=service
    service_access_tag_blacklist = Set {
        'private'
    },

    restricted_access_tag_list = Set {
      'private',
      'delivery',
      'destination',
      'customers',
    },

    access_tags_hierarchy = Sequence {
      'motorcar',
      'motor_vehicle',
      'vehicle',
      'access'
    },

    service_tag_forbidden = Set {
      'emergency_access'
    },

    restrictions = Sequence {
      'motorcar',
      'motor_vehicle',
      'vehicle'
    },

    classes = Sequence {
        'toll', 'motorway', 'ferry', 'restricted', 'tunnel'
    },

    -- classes to support for exclude flags
    excludable = Sequence {
        Set {'toll'},
        Set {'motorway'},
        Set {'ferry'}
    },

    avoid = Set {
      'area',
      -- 'toll',    -- uncomment this to avoid tolls
      'reversible',
      'impassable',
      'hov_lanes',
      'steps',
      'construction',
      'proposed'
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
      alley             = 0.5,
      parking           = 0.5,
      parking_aisle     = 0.5,
      driveway          = 0.5,
      ["drive-through"] = 0.5,
      ["drive-thru"] = 0.5
    },

    restricted_highway_whitelist = Set {
      'motorway',
      'motorway_link',
      'trunk',
      'trunk_link',
      'primary',
      'primary_link',
      'secondary',
      'secondary_link',
      'tertiary',
      'tertiary_link',
      'residential',
      'living_street',
      'unclassified',
      'service'
    },

    construction_whitelist = Set {
      'no',
      'widening',
      'minor',
    },

    route_speeds = {
      ferry = 5,
      shuttle_train = 10
    },

    bridge_speeds = {
      movable = 5
    },

    -- surface/trackype/smoothness
    -- values were estimated from looking at the photos at the relevant wiki pages

    -- max speed for surfaces
    surface_speeds = {
      asphalt = nil,    -- nil mean no limit. removing the line has the same effect
      concrete = nil,
      ["concrete:plates"] = nil,
      ["concrete:lanes"] = nil,
      paved = nil,

      cement = 80,
      compacted = 80,
      fine_gravel = 80,

      paving_stones = 60,
      metal = 60,
      bricks = 60,

      grass = 40,
      wood = 40,
      sett = 40,
      grass_paver = 40,
      gravel = 40,
      unpaved = 40,
      ground = 40,
      dirt = 40,
      pebblestone = 40,
      tartan = 40,

      cobblestone = 30,
      clay = 30,

      earth = 20,
      stone = 20,
      rocky = 20,
      sand = 20,

      mud = 10
    },

    -- max speed for tracktypes
    tracktype_speeds = {
      grade1 =  60,
      grade2 =  40,
      grade3 =  30,
      grade4 =  25,
      grade5 =  20
    },

    -- max speed for smoothnesses
    smoothness_speeds = {
      intermediate    =  80,
      bad             =  40,
      very_bad        =  20,
      horrible        =  10,
      very_horrible   =  5,
      impassable      =  0
    },

    -- http://wiki.openstreetmap.org/wiki/Speed_limits
    maxspeed_table_default = {
      urban = 50,
      rural = 90,
      trunk = 110,
      motorway = 130
    },

    -- List only exceptions
    maxspeed_table = {
      ["at:rural"] = 100,
      ["at:trunk"] = 100,
      ["be:motorway"] = 120,
      ["be-bru:rural"] = 70,
      ["be-bru:urban"] = 30,
      ["be-vlg:rural"] = 70,
      ["bg:motorway"] = 140,
      ["by:urban"] = 60,
      ["by:motorway"] = 110,
      ["ca-on:rural"] = 80,
      ["ch:rural"] = 80,
      ["ch:trunk"] = 100,
      ["ch:motorway"] = 120,
      ["cz:trunk"] = 0,
      ["cz:motorway"] = 0,
      ["de:living_street"] = 7,
      ["de:rural"] = 100,
      ["de:motorway"] = 0,
      ["dk:rural"] = 80,
      ["es:trunk"] = 90,
      ["fr:rural"] = 80,
      ["gb:nsl_single"] = (60*1609)/1000,
      ["gb:nsl_dual"] = (70*1609)/1000,
      ["gb:motorway"] = (70*1609)/1000,
      ["nl:rural"] = 80,
      ["nl:trunk"] = 100,
      ['no:rural'] = 80,
      ['no:motorway'] = 110,
      ['ph:urban'] = 40,
      ['ph:rural'] = 80,
      ['ph:motorway'] = 100,
      ['pl:rural'] = 100,
      ['pl:expressway'] = 120,
      ['pl:motorway'] = 140,
      ["ro:trunk"] = 100,
      ["ru:living_street"] = 20,
      ["ru:urban"] = 60,
      ["ru:motorway"] = 110,
      ["uk:nsl_single"] = (60*1609)/1000,
      ["uk:nsl_dual"] = (70*1609)/1000,
      ["uk:motorway"] = (70*1609)/1000,
      ['za:urban'] = 60,
      ['za:rural'] = 100,
      ["none"] = 140
    },

    relation_types = Sequence {
      "route"
    },

    -- classify highway tags when necessary for turn weights
    highway_turn_classification = {
    },

    -- classify access tags when necessary for turn weights
    access_turn_classification = {
    }
  }
end

-- Apply dynamic modifiers to routing weights (parametric, no if/else)
local function apply_dynamic_modifiers(result, way)
  -- Read dynamic parameters from way tags (defaults ensure backward compatibility)
  local priority_factor = tonumber(way:get_value_by_key("priority_factor")) or 1.0
  local rating = tonumber(way:get_value_by_key("rating")) or 0
  local flow = tonumber(way:get_value_by_key("flow")) or 5

  -- Calculate modifiers using parametric formulas (no conditional logic)
  -- Rating modifier: rating from -5 to +5, maps to speed multiplier 0.1 to 1.0
  -- Formula: rating_mod = max(0.1, 1.0 - rating * 0.05 * RATING_WEIGHT)
  local rating_mod = math.max(0.1, 1.0 - rating * 0.05 * PARAMS.RATING_WEIGHT)

  -- Flow modifier: flow from 0 to 5, maps to speed multiplier 0.0 to 1.0
  -- Formula: flow_mod = (flow / 5.0) * FLOW_WEIGHT
  local flow_mod = (flow / 5.0) * PARAMS.FLOW_WEIGHT

  -- Priority modifier: priority_factor from 0.8 to 1.2, inversely affects weight
  -- Higher priority_factor = lower cost (inverse relationship)
  -- Reduced range (0.8-1.2) makes priority differences more subtle
  -- Formula: priority_mod = 1.0 / max(0.1, priority_factor * PRIORITY_WEIGHT)
  local priority_mod = 1.0 / math.max(0.1, priority_factor * PARAMS.PRIORITY_WEIGHT)

  -- Apply modifiers to speed (multiplicative)
  if result.forward_speed and result.forward_speed > 0 then
    result.forward_speed = result.forward_speed * flow_mod * rating_mod
  end
  if result.backward_speed and result.backward_speed > 0 then
    result.backward_speed = result.backward_speed * flow_mod * rating_mod
  end

  -- Apply modifiers to weight (priority reduces cost, flow/rating increase cost)
  -- Formula: weight = weight * priority_mod / (flow_mod * rating_mod)
  if result.weight and result.weight > 0 then
    result.weight = result.weight * priority_mod / (flow_mod * rating_mod)
  end
end

function process_node(profile, node, result, relations)
  -- parse access and barrier tags
  -- Note: obstacle_map is a global provided by OSRM, check if it exists before using
  local access = find_access_tag(node, profile.access_tags_hierarchy)
  if access then
    if profile.access_tag_blacklist[access] and not profile.restricted_access_tag_list[access] then
      if obstacle_map then
        obstacle_map:add(node, Obstacle.new(obstacle_type.barrier))
      end
    end
  else
    local barrier = node:get_value_by_key("barrier")
    if barrier then
      --  check height restriction barriers
      local restricted_by_height = false
      if barrier == 'height_restrictor' then
         local maxheight = Measure.get_max_height(node:get_value_by_key("maxheight"), node)
         restricted_by_height = maxheight and maxheight < profile.vehicle_height
      end

      --  make an exception for rising bollard barriers
      local bollard = node:get_value_by_key("bollard")
      local rising_bollard = bollard and "rising" == bollard

      -- make an exception for lowered/flat barrier=kerb
      -- and incorrect tagging of highway crossing kerb as highway barrier
      local kerb = node:get_value_by_key("kerb")
      local highway = node:get_value_by_key("highway")
      local flat_kerb = kerb and ("lowered" == kerb or "flush" == kerb)
      local highway_crossing_kerb = barrier == "kerb" and highway and highway == "crossing"

      if not profile.barrier_whitelist[barrier]
                and not rising_bollard
                and not flat_kerb
                and not highway_crossing_kerb
                or restricted_by_height then
        if obstacle_map then
          obstacle_map:add(node, Obstacle.new(obstacle_type.barrier))
        end
      end
    end
  end

  Obstacles.process_node(profile, node)
end

function process_way(profile, way, result, relations)
  -- the intial filtering of ways based on presence of tags
  -- affects processing times significantly, because all ways
  -- have to be checked.
  -- to increase performance, prefetching and intial tag check
  -- is done in directly instead of via a handler.

  -- in general we should  try to abort as soon as
  -- possible if the way is not routable, to avoid doing
  -- unnecessary work. this implies we should check things that
  -- commonly forbids access early, and handle edge cases later.

  -- data table for storing intermediate values during processing
  local data = {
    -- prefetch tags
    highway = way:get_value_by_key('highway'),
    bridge = way:get_value_by_key('bridge'),
    route = way:get_value_by_key('route')
  }

  -- perform an quick initial check and abort if the way is
  -- obviously not routable.
  -- highway or route tags must be in data table, bridge is optional
  if (not data.highway or data.highway == '') and
  (not data.route or data.route == '')
  then
    return
  end

  handlers = Sequence {
    -- set the default mode for this profile. if can be changed later
    -- in case it turns we're e.g. on a ferry
    WayHandlers.default_mode,

    -- check various tags that could indicate that the way is not
    -- routable. this includes things like status=impassable,
    -- toll=yes and oneway=reversible
    WayHandlers.blocked_ways,
    WayHandlers.avoid_ways,
    WayHandlers.handle_height,
    WayHandlers.handle_width,
    WayHandlers.handle_length,
    WayHandlers.handle_weight,

    -- determine access status by checking our hierarchy of
    -- access tags, e.g: motorcar, motor_vehicle, vehicle
    WayHandlers.access,

    -- check whether forward/backward directions are routable
    WayHandlers.oneway,

    -- check a road's destination
    WayHandlers.destinations,

    -- check whether we're using a special transport mode
    WayHandlers.ferries,
    WayHandlers.movables,

    -- handle service road restrictions
    WayHandlers.service,

    -- handle hov
    WayHandlers.hov,

    -- compute speed taking into account way type, maxspeed tags, etc.
    WayHandlers.speed,
    WayHandlers.maxspeed,
    WayHandlers.surface,
    WayHandlers.penalties,

    -- compute class labels
    WayHandlers.classes,

    -- handle turn lanes and road classification, used for guidance
    WayHandlers.turn_lanes,
    WayHandlers.classification,

    -- handle various other flags
    WayHandlers.roundabouts,
    WayHandlers.startpoint,
    WayHandlers.driving_side,

    -- set name, ref and pronunciation
    WayHandlers.names,

    -- set weight properties of the way
    WayHandlers.weights,

    -- set classification of ways relevant for turns
    WayHandlers.way_classification_for_turn
  }

  WayHandlers.run(profile, way, result, data, handlers, relations)

  if profile.cardinal_directions then
      Relations.process_way_refs(way, relations, result)
  end

  -- Apply dynamic modifiers AFTER WayHandlers.run() completes
  apply_dynamic_modifiers(result, way)
end

function process_turn(profile, turn)
  -- Use a sigmoid function to return a penalty that maxes out at turn_penalty
  -- over the space of 0-180 degrees.  Values here were chosen by fitting
  -- the function to some turn penalty samples from real driving.
  local turn_penalty = profile.turn_penalty
  local turn_bias = turn.is_left_hand_driving and 1. / profile.turn_bias or profile.turn_bias

  -- Check if obstacle_map exists before using it
  if obstacle_map then
    for _, obs in pairs(obstacle_map:get(turn.from, turn.via)) do
      -- disregard a minor stop if entering by the major road
      -- rationale: if a stop sign is tagged at the center of the intersection with stop=minor
      -- it should only penalize the minor roads entering the intersection
      if obs.type == obstacle_type.stop_minor and not Obstacles.entering_by_minor_road(turn) then
          goto skip
      end
      -- heuristic to infer the direction of a stop without an explicit direction tag
      -- rationale: a stop sign should not be placed farther than 20m from the intersection
      if turn.number_of_roads == 2
          and obs.type == obstacle_type.stop
          and obs.direction == obstacle_direction.none
          and turn.source_road.distance < 20
          and turn.target_road.distance > 20 then
              goto skip
      end
      turn.duration = turn.duration + obs.duration
      ::skip::
    end
  end

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

  -- for distance based routing we don't want to have penalties based on turn angle
  if profile.properties.weight_name == 'distance' then
     turn.weight = 0
  else
     turn.weight = turn.duration
  end

  if profile.properties.weight_name == 'routability' then
      -- penalize turns from non-local access only segments onto local access only tags
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

/**
 * Generate Motorbike Lua Script (Vietnam optimized with shipper feedback)
 * Refactored: Base script + mode-specific weight functions
 * @param mode - Routing mode
 * @param priorityWeight - Weight for priority_factor influence (default: 1.0)
 * @param ratingWeight - Weight for rating influence (default: 1.0)
 * @param flowWeight - Weight for flow influence (default: 1.0)
 */
function generateMotorbikeLuaScript(
	mode:
		| 'strict_priority_with_delta'
		| 'flexible_priority_with_delta'
		| 'strict_priority_no_delta'
		| 'flexible_priority_no_delta'
		| 'base' = 'base',
	priorityWeight: number = 1.0,
	ratingWeight: number = 1.0,
	flowWeight: number = 1.0
): string {
	// Determine mode flags
	const isStrictPriority = mode.startsWith('strict_priority');
	const hasDelta = mode.endsWith('_with_delta');
	const isBase = mode === 'base';

	return `-- Custom motorbike profile for OSRM with shipper feedback
-- Generated by Zone Service
-- Mode: ${mode.toUpperCase()}
-- Optimized for Vietnam traffic (no motorways, shipper ratings matter)
-- Supports dynamic modifiers: priority_factor, rating, flow

api_version = 4

properties.max_speed_for_map_matching = 140 / 3.6
properties.use_turn_restrictions = false  -- Vietnam motorbikes ignore most restrictions
properties.continue_straight_at_waypoint = true
properties.weight_name = 'custom'

-- Dynamic weight parameters (configurable)
local PARAMS = {
  PRIORITY_WEIGHT = ${priorityWeight},  -- Weight for priority_factor influence
  RATING_WEIGHT = ${ratingWeight},    -- Weight for rating influence
  FLOW_WEIGHT = ${flowWeight},      -- Weight for flow influence
}

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

-- Apply dynamic modifiers to routing weights (parametric, no if/else)
local function apply_dynamic_modifiers(result, way)
  -- Read dynamic parameters from way tags (defaults ensure backward compatibility)
  local priority_factor = tonumber(way:get_value_by_key("priority_factor")) or 1.0
  local rating = tonumber(way:get_value_by_key("rating")) or 0
  local flow = tonumber(way:get_value_by_key("flow")) or 5

  -- Calculate modifiers using parametric formulas (no conditional logic)
  -- Rating modifier: rating from -5 to +5, maps to speed multiplier 0.1 to 1.0
  -- Formula: rating_mod = max(0.1, 1.0 - rating * 0.05 * RATING_WEIGHT)
  local rating_mod = math.max(0.1, 1.0 - rating * 0.05 * PARAMS.RATING_WEIGHT)

  -- Flow modifier: flow from 0 to 5, maps to speed multiplier 0.0 to 1.0
  -- Formula: flow_mod = (flow / 5.0) * FLOW_WEIGHT
  local flow_mod = (flow / 5.0) * PARAMS.FLOW_WEIGHT

  -- Priority modifier: priority_factor from 0.8 to 1.2, inversely affects weight
  -- Higher priority_factor = lower cost (inverse relationship)
  -- Reduced range (0.8-1.2) makes priority differences more subtle
  -- Formula: priority_mod = 1.0 / max(0.1, priority_factor * PRIORITY_WEIGHT)
  local priority_mod = 1.0 / math.max(0.1, priority_factor * PARAMS.PRIORITY_WEIGHT)

  -- Apply modifiers to speed (multiplicative)
  result.forward_speed = result.forward_speed * flow_mod * rating_mod
  result.backward_speed = result.backward_speed * flow_mod * rating_mod

  -- Apply modifiers to weight (priority reduces cost, flow/rating increase cost)
  -- Formula: weight = weight * priority_mod / (flow_mod * rating_mod)
  if result.weight and result.weight > 0 then
    result.weight = result.weight * priority_mod / (flow_mod * rating_mod)
  end
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

  -- Apply dynamic modifiers (priority_factor, rating, flow)
  apply_dynamic_modifiers(result, way)
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
 * @param instanceName - Name of the OSRM instance
 * @param vehicleType - Type of vehicle (car or motorbike)
 * @param mode - Routing mode
 * @param priorityWeight - Weight for priority_factor influence (default: 1.0)
 * @param ratingWeight - Weight for rating influence (default: 1.0)
 * @param flowWeight - Weight for flow influence (default: 1.0)
 */
function generateLuaProfile(
	instanceName: string,
	vehicleType: 'car' | 'motorbike',
	mode:
		| 'strict_priority_with_delta'
		| 'flexible_priority_with_delta'
		| 'strict_priority_no_delta'
		| 'flexible_priority_no_delta'
		| 'base' = 'base',
	priorityWeight: number = 1.0,
	ratingWeight: number = 1.0,
	flowWeight: number = 1.0
): string {
	console.log(
		`\n🔧 Generating custom ${vehicleType} Lua profile for ${instanceName} (${mode})...`
	);
	console.log(
		`   Dynamic weights: priority=${priorityWeight}, rating=${ratingWeight}, flow=${flowWeight}`
	);

	const outputDir = join(process.cwd(), 'osrm_data', instanceName);
	const luaFilePath = join(outputDir, `custom_${vehicleType}.lua`);

	const luaScript =
		vehicleType === 'motorbike'
			? generateMotorbikeLuaScript(
					mode,
					priorityWeight,
					ratingWeight,
					flowWeight
			  )
			: generateCarLuaScript(priorityWeight, ratingWeight, flowWeight);

	writeFileSync(luaFilePath, luaScript, 'utf-8');
	console.log(`   ✓ Generated Lua profile at ${luaFilePath} (mode: ${mode})`);
	return luaFilePath;
}

/**
 * Run OSRM processing using Docker
 */
async function runOSRMProcessing(
	instanceName: string,
	vehicleType: 'car' | 'motorbike'
): Promise<void> {
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
		const { stdout: extractOut, stderr: extractErr } = await execAsync(
			extractCmd,
			{
				maxBuffer: 100 * 1024 * 1024,
			}
		);
		if (extractOut) console.log('   ', extractOut.trim());
		if (extractErr) console.error('   ', extractErr.trim());
		console.log('   ✓ Extract completed');

		// Step 2: Partition
		console.log('   Running osrm-partition...');
		const partitionCmd = `docker run --rm -v "${dataDir}:/data" osrm/osrm-backend:latest osrm-partition /data/${osrmFile}`;
		console.log(`   Command: ${partitionCmd}`);
		const { stdout: partOut, stderr: partErr } = await execAsync(
			partitionCmd,
			{
				maxBuffer: 100 * 1024 * 1024,
			}
		);
		if (partOut) console.log('   ', partOut.trim());
		if (partErr) console.error('   ', partErr.trim());
		console.log('   ✓ Partition completed');

		// Step 3: Customize
		console.log('   Running osrm-customize...');
		const customizeCmd = `docker run --rm -v "${dataDir}:/data" osrm/osrm-backend:latest osrm-customize /data/${osrmFile}`;
		console.log(`   Command: ${customizeCmd}`);
		const { stdout: custOut, stderr: custErr } = await execAsync(
			customizeCmd,
			{
				maxBuffer: 100 * 1024 * 1024,
			}
		);
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
		if (
			segment.traffic_conditions &&
			segment.traffic_conditions.length > 0
		) {
			const traffic = segment.traffic_conditions[0];
			weight *= traffic.weight_multiplier || 1.0;
		}

		return acc + weight;
	}, 0);

	return sum / segments.length;
}

function getMostCommonTrafficLevel(segments: any[]): string | null {
	const trafficLevels = segments
		.filter((s) => s.traffic_conditions && s.traffic_conditions.length > 0)
		.map((s) => s.traffic_conditions[0].traffic_level);

	if (trafficLevels.length === 0) return null;

	// Count occurrences
	const counts: Record<string, number> = {};
	trafficLevels.forEach((level) => {
		counts[level] = (counts[level] || 0) + 1;
	});

	// Return most common
	return Object.entries(counts).sort(([, a], [, b]) => b - a)[0]?.[0] || null;
}

function getAverageCongestionScore(segments: any[]): number {
	const scores = segments
		.filter((s) => s.traffic_conditions && s.traffic_conditions.length > 0)
		.map((s) => s.traffic_conditions[0].congestion_score);

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
	console.log('🚀 OSRM Data Generator - Multi-Vehicle Routing Profiles');
	console.log('='.repeat(70));

	// Check if car/motorbike generation is enabled (default: true for both)
	const generateCar = process.env.GENERATE_OSRM_CAR !== 'false';
	const generateMotorbike = process.env.GENERATE_OSRM_MOTORBIKE !== 'false';

	// 5 instances for different routing strategies (motorbike)
	// Logic: 2x2x1 (priority_mode x delta x blocking) + 1 base = 5
	// priority_mode: strict (always follow priority) vs flexible (allow lower priority if convenient)
	// delta: with_delta (apply point_score/delta) vs no_delta (ignore AI recommendations)
	// blocking: always applies (1)
	const motorbikeInstances = generateMotorbike
		? [
				{
					name: 'osrm-strict-priority-with-delta',
					vehicle: 'motorbike' as const,
					mode: 'strict_priority_with_delta' as const,
				},
				{
					name: 'osrm-flexible-priority-with-delta',
					vehicle: 'motorbike' as const,
					mode: 'flexible_priority_with_delta' as const,
				},
				{
					name: 'osrm-strict-priority-no-delta',
					vehicle: 'motorbike' as const,
					mode: 'strict_priority_no_delta' as const,
				},
				{
					name: 'osrm-flexible-priority-no-delta',
					vehicle: 'motorbike' as const,
					mode: 'flexible_priority_no_delta' as const,
				},
				{
					name: 'osrm-base',
					vehicle: 'motorbike' as const,
					mode: 'base' as const,
				},
		  ]
		: [];

	// Car instances: Create same 5 modes for car (with dynamic weights support)
	// Car profiles use full OSRM car.lua with dynamic modifiers (priority_factor, rating, flow)
	const carInstances = generateCar
		? [
				{
					name: 'osrm-car-strict-priority-with-delta',
					vehicle: 'car' as const,
					mode: 'strict_priority_with_delta' as const,
				},
				{
					name: 'osrm-car-flexible-priority-with-delta',
					vehicle: 'car' as const,
					mode: 'flexible_priority_with_delta' as const,
				},
				{
					name: 'osrm-car-strict-priority-no-delta',
					vehicle: 'car' as const,
					mode: 'strict_priority_no_delta' as const,
				},
				{
					name: 'osrm-car-flexible-priority-no-delta',
					vehicle: 'car' as const,
					mode: 'flexible_priority_no_delta' as const,
				},
				{
					name: 'osrm-car-base',
					vehicle: 'car' as const,
					mode: 'base' as const,
				},
		  ]
		: [];

	const instances = [
		...motorbikeInstances,
		//  ...carInstances
	];

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

		// Step 1: Fetch all road network data ONCE (reused for all instances)
		console.log(
			'\n📊 Step 1: Fetching road network data (once for all instances)...'
		);
		console.log('─'.repeat(70));
		const roadNetworkData = await fetchRoadNetworkData();

		// Step 2-4: Generate OSRM data for each instance (using cached data)
		for (const instance of instances) {
			console.log(`\n${'─'.repeat(70)}`);
			console.log(
				`Processing ${
					instance.name
				} (${instance.vehicle.toUpperCase()} - ${instance.mode.toUpperCase()})`
			);
			console.log('─'.repeat(70));

			// Step 2: Export to OSM XML (using pre-fetched data)
			await exportToOSMXML(instance.name, instance.mode, roadNetworkData);

			// Step 3: Generate Lua profile with mode-specific weights
			// Adjust PRIORITY_WEIGHT based on mode to control priority strictness
			let priorityWeight = 1.0;
			let ratingWeight = 1.0;
			let flowWeight = 1.0;

			if (
				instance.mode === 'strict_priority_with_delta' ||
				instance.mode === 'strict_priority_no_delta'
			) {
				// Strict mode: Higher priority weight (1.5-2.0) to enforce strict priority ordering
				priorityWeight = 1.8;
				ratingWeight = 1.0;
				flowWeight = 1.0;
			} else if (
				instance.mode === 'flexible_priority_with_delta' ||
				instance.mode === 'flexible_priority_no_delta'
			) {
				// Flexible mode: Lower priority weight (0.5-0.7) to allow lower priority orders if convenient
				priorityWeight = 0.6;
				ratingWeight = 1.0;
				flowWeight = 1.0;
			} else {
				// Base mode: Minimal priority influence (0.1) - almost ignore priority
				priorityWeight = 0.1;
				ratingWeight = 1.0;
				flowWeight = 1.0;
			}

			generateLuaProfile(
				instance.name,
				instance.vehicle,
				instance.mode,
				priorityWeight,
				ratingWeight,
				flowWeight
			);

			// Step 4: Run OSRM processing
			await runOSRMProcessing(instance.name, instance.vehicle);

			console.log(
				`\n✅ ${instance.name} (${instance.vehicle} - ${instance.mode}) is ready!`
			);
		}

		console.log('\n' + '='.repeat(70));
		console.log(
			`🎉 All ${instances.length} OSRM instance(s) generated successfully!`
		);
		console.log('='.repeat(70));
		if (generateMotorbike && motorbikeInstances.length > 0) {
			console.log('\n📍 Motorbike Instances (5 routing modes):');
			console.log(
				'  1. osrm-strict-priority-with-delta    - Luôn ưu tiên theo độ ưu tiên, có áp dụng delta/point_score'
			);
			console.log(
				'  2. osrm-flexible-priority-with-delta  - Ưu tiên linh hoạt (cho phép đơn thấp nếu tiện đường), có delta/point_score'
			);
			console.log(
				'  3. osrm-strict-priority-no-delta      - Luôn ưu tiên theo độ ưu tiên, KHÔNG áp dụng delta/point_score'
			);
			console.log(
				'  4. osrm-flexible-priority-no-delta    - Ưu tiên linh hoạt, KHÔNG áp dụng delta/point_score'
			);
			console.log(
				'  5. osrm-base                          - Base OSRM logic, chỉ shipper feedback'
			);
		}

		if (generateCar && carInstances.length > 0) {
			console.log(
				'\n📍 Car Instances (5 routing modes with dynamic weights):'
			);
			console.log(
				'  1. osrm-car-strict-priority-with-delta    - Car routing với strict priority + delta'
			);
			console.log(
				'  2. osrm-car-flexible-priority-with-delta  - Car routing với flexible priority + delta'
			);
			console.log(
				'  3. osrm-car-strict-priority-no-delta      - Car routing với strict priority, no delta'
			);
			console.log(
				'  4. osrm-car-flexible-priority-no-delta    - Car routing với flexible priority, no delta'
			);
			console.log(
				'  5. osrm-car-base                          - Car routing base (full OSRM car.lua + dynamic modifiers)'
			);
		}

		console.log('\n🐳 Start OSRM containers:');
		console.log(
			'  docker-compose up osrm-strict-priority-with-delta osrm-flexible-priority-with-delta osrm-strict-priority-no-delta osrm-flexible-priority-no-delta osrm-base'
		);
		if (generateCar) {
			console.log('  # + car instances if enabled');
		}
		console.log('\n🚀 Or start all services:');
		console.log('  docker-compose up -d');
		console.log('\n🌐 API endpoints (Motorbike):');
		console.log(
			'  • Strict-Priority-With-Delta:   /route/v1/motorbike/{coordinates}  (OSRM_STRICT_PRIORITY_WITH_DELTA_URL)'
		);
		console.log(
			'  • Flexible-Priority-With-Delta: /route/v1/motorbike/{coordinates}  (OSRM_FLEXIBLE_PRIORITY_WITH_DELTA_URL)'
		);
		console.log(
			'  • Strict-Priority-No-Delta:     /route/v1/motorbike/{coordinates}  (OSRM_STRICT_PRIORITY_NO_DELTA_URL)'
		);
		console.log(
			'  • Flexible-Priority-No-Delta:   /route/v1/motorbike/{coordinates}  (OSRM_FLEXIBLE_PRIORITY_NO_DELTA_URL)'
		);
		console.log(
			'  • Base:                         /route/v1/motorbike/{coordinates}  (OSRM_BASE_URL)'
		);
		if (generateCar) {
			console.log('\n🌐 API endpoints (Car):');
			console.log(
				'  • Car Strict-Priority-With-Delta:   /route/v1/car/{coordinates}  (OSRM_CAR_STRICT_PRIORITY_WITH_DELTA_URL)'
			);
			console.log(
				'  • Car Flexible-Priority-With-Delta: /route/v1/car/{coordinates}  (OSRM_CAR_FLEXIBLE_PRIORITY_WITH_DELTA_URL)'
			);
			console.log(
				'  • Car Strict-Priority-No-Delta:     /route/v1/car/{coordinates}  (OSRM_CAR_STRICT_PRIORITY_NO_DELTA_URL)'
			);
			console.log(
				'  • Car Flexible-Priority-No-Delta:   /route/v1/car/{coordinates}  (OSRM_CAR_FLEXIBLE_PRIORITY_NO_DELTA_URL)'
			);
			console.log(
				'  • Car Base:                         /route/v1/car/{coordinates}  (OSRM_CAR_BASE_URL)'
			);
		}
		console.log(
			'\n💡 To disable car/motorbike generation, set GENERATE_OSRM_CAR=false or GENERATE_OSRM_MOTORBIKE=false'
		);
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
