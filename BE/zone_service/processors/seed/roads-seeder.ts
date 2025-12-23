/**
 * Improved Roads Seeder - Optimized Version
 *
 * Improvements:
 * - Batch inserts (100x faster than individual inserts)
 * - Coordinate-based intersection detection (fixes connectivity)
 * - Automatic duplicate node merging
 * - Progress reporting
 *
 * Performance: ~17k streets in <5 minutes (was 1+ hour)
 */

import { PrismaClient } from '@prisma/client';
import { OSMParser, calculateLineLength, findLatestVietnamPBF } from '../../utils/osm-parser';
import { findIntersections, generateSegments } from '../../utils/intersection-finder';
import { calculateBaseWeight } from '../../utils/weight-calculator';
import { join } from 'path';
import { existsSync } from 'fs';

const prisma = new PrismaClient();

const COORDINATE_PRECISION = 7;
const SKIP_TO_STEP = process.env.SKIP_TO_STEP ? parseInt(process.env.SKIP_TO_STEP, 10) : undefined;

function coordinateKey(lat: number, lon: number): string {
  return `${lat.toFixed(COORDINATE_PRECISION)},${lon.toFixed(COORDINATE_PRECISION)}`;
}

function createStepLogger(stepName: string) {
  const start = Date.now();
  let lastLine = '';

  const logProgress = (current: number, total?: number) => {
    const percent = total && total > 0 ? ` (${((current / total) * 100).toFixed(1)}%)` : '';
    const line = `  ${stepName}: ${current}${total ? '/' + total : ''}${percent}`;
    if (line === lastLine) return;
    lastLine = line;
    process.stdout.write(`\r${line}`);
  };

  const done = (extra?: string) => {
    const duration = ((Date.now() - start) / 1000).toFixed(1);
    process.stdout.write('\n');
    console.log(`✓ ${stepName} done in ${duration}s${extra ? ` (${extra})` : ''}`);
  };

  return { logProgress, done };
}

/**
 * Step 18: Merge duplicate nodes by coordinates
 * Can be called standalone or as part of seedRoads()
 */
async function mergeDuplicateNodes() {
  console.log('Step 18: Merging duplicate nodes by coordinates...');
  const mergeStep = createStepLogger('Merge duplicates');
  
  // Load nodes with select only needed fields
  const allNodes = await prisma.road_nodes.findMany({
    select: { node_id: true, lat: true, lon: true },
  });
  
  mergeStep.logProgress(0, allNodes.length);
  
  const coordMap = new Map<string, string[]>();
  for (const node of allNodes) {
    const key = coordinateKey(node.lat, node.lon);
    if (!coordMap.has(key)) {
      coordMap.set(key, []);
    }
    coordMap.get(key)!.push(node.node_id);
  }

  const duplicateGroups = Array.from(coordMap.entries())
    .filter(([_, nodeIds]) => nodeIds.length > 1);

  if (duplicateGroups.length > 0) {
    console.log(`  Found ${duplicateGroups.length} locations with duplicates`);

    const nodeIdMap = new Map<string, string>();
    const nodesToDelete: string[] = [];

    for (const [_, nodeIds] of duplicateGroups) {
      const [keepNodeId, ...duplicateNodeIds] = nodeIds;
      if (!keepNodeId) continue;
      
      for (const dupId of duplicateNodeIds) {
        nodeIdMap.set(dupId, keepNodeId);
        nodesToDelete.push(dupId);
      }
    }

    // Simple approach: Update segments using raw SQL with CASE WHEN (batch all mappings)
    if (nodeIdMap.size > 0) {
      console.log(`  Updating ${nodeIdMap.size} duplicate node references in segments...`);
      
      const entries = Array.from(nodeIdMap.entries());
      const updateStep = createStepLogger('Update segments');
      const batchSize = 20000; // Large batches for CASE WHEN
      
      // Update from_node_id in batches
      for (let i = 0; i < entries.length; i += batchSize) {
        const batch = entries.slice(i, i + batchSize);
        const oldIds = batch.map(([old]) => `'${old.replace(/'/g, "''")}'`).join(',');
        const caseWhens = batch.map(([old, _new]) => 
          `WHEN '${old.replace(/'/g, "''")}' THEN '${_new.replace(/'/g, "''")}'`
        ).join(' ');
        
        await prisma.$executeRawUnsafe(`
          UPDATE road_segments
          SET from_node_id = CASE from_node_id
            ${caseWhens}
          END
          WHERE from_node_id IN (${oldIds})
        `);
        
        updateStep.logProgress(Math.min(i + batchSize, entries.length), entries.length);
      }
      
      // Update to_node_id in batches
      for (let i = 0; i < entries.length; i += batchSize) {
        const batch = entries.slice(i, i + batchSize);
        const oldIds = batch.map(([old]) => `'${old.replace(/'/g, "''")}'`).join(',');
        const caseWhens = batch.map(([old, _new]) => 
          `WHEN '${old.replace(/'/g, "''")}' THEN '${_new.replace(/'/g, "''")}'`
        ).join(' ');
        
        await prisma.$executeRawUnsafe(`
          UPDATE road_segments
          SET to_node_id = CASE to_node_id
            ${caseWhens}
          END
          WHERE to_node_id IN (${oldIds})
        `);
        
        updateStep.logProgress(Math.min(i + batchSize, entries.length), entries.length);
      }
      
      updateStep.done();

      // Delete duplicate nodes in batches
      console.log(`  Deleting ${nodesToDelete.length} duplicate nodes...`);
      const deleteBatchSize = 5000;
      for (let i = 0; i < nodesToDelete.length; i += deleteBatchSize) {
        const batch = nodesToDelete.slice(i, i + deleteBatchSize);
        await prisma.road_nodes.deleteMany({
          where: { node_id: { in: batch } },
        });
      }

      mergeStep.done(`merged=${nodeIdMap.size}, deleted=${nodesToDelete.length}`);
    }
  } else {
    console.log('  No duplicate nodes found');
    mergeStep.done('no duplicates');
  }
  console.log();
}

async function seedRoads() {
  console.log('🌱 Starting Optimized Roads Seeding...\n');
  const startTime = Date.now();

  try {
    // Skip to specific step if SKIP_TO_STEP is set
    if (SKIP_TO_STEP === 18) {
      console.log('⚠️ Skipping to Step 18 (merge duplicates)...\n');
      await mergeDuplicateNodes();
      
      // Final summary
      const finalNodeCount = await prisma.road_nodes.count();
      const finalSegmentCount = await prisma.road_segments.count();
      const finalRoadCount = await prisma.roads.count();
      const duration = ((Date.now() - startTime) / 1000).toFixed(1);

      console.log('='.repeat(60));
      console.log('✅ Step 18 Completed Successfully!');
      console.log('='.repeat(60));
      console.log(`\nTime taken: ${duration}s`);
      console.log('\nFinal counts:');
      console.log(`  - Roads: ${finalRoadCount}`);
      console.log(`  - Nodes: ${finalNodeCount}`);
      console.log(`  - Segments: ${finalSegmentCount}`);
      console.log('='.repeat(60));
      return;
    }

    // Guard: run only when roads table is empty (unless skipping to step 18)
    const existing = await prisma.roads.count();
    if (existing > 0 && !SKIP_TO_STEP) {
      console.log(`⚠️ roads table is not empty (${existing} rows). Skip seeding.`);
      return;
    }

    // Step 1: Parse OSM PBF file
    console.log('Step 1: Parsing OSM PBF file...');
    const rawDataDir = join(process.cwd(), './raw_data');
    
    // Prefer extracted HCMC (city-wide); fallback to latest Vietnam PBF + clip HCMC
    const hcmcExtractPath = join(rawDataDir, 'extracted/hcmc.osm.pbf');
    let pbfPath: string;
    let polyFile: string | undefined;
    
    if (existsSync(hcmcExtractPath)) {
      console.log('  Using HCMC extract (city-wide)');
      pbfPath = hcmcExtractPath;
      polyFile = undefined; // Already clipped
    } else {
      console.log('  Using source PBF with HCMC polygon clip');
      console.log('  Tip: Run "npm run extract:complete" to pre-extract hcmc.osm.pbf');
      pbfPath = findLatestVietnamPBF(rawDataDir);
      polyFile = join(rawDataDir, 'poly/hcmc.poly');
    }

    const parser = new OSMParser();
    const osmData = await parser.parsePBF(pbfPath, polyFile);
    console.log(`✓ Parsed ${osmData.nodes.size} nodes and ${osmData.ways.length} ways\n`);

    // Step 2: Filter road ways
    console.log('Step 2: Filtering road ways...');
    let roadWays = osmData.ways.filter(way => OSMParser.isRoadWay(way));
    const roadStart = process.env.ROAD_START ? parseInt(process.env.ROAD_START, 10) : 0;
    const roadEnd = process.env.ROAD_END ? parseInt(process.env.ROAD_END, 10) : undefined;
    if (roadStart > 0 || roadEnd !== undefined) {
      roadWays = roadWays.slice(roadStart, roadEnd);
      console.log(`✓ Applied road slice [${roadStart}, ${roadEnd ?? 'end'}) → ${roadWays.length}`);
    }
    console.log(`✓ Found ${roadWays.length} road ways\n`);

    // Step 3: Load zones
    console.log('Step 3: Loading zones...');
    const zones = await prisma.zones.findMany();
    console.log(`✓ Loaded ${zones.length} zones\n`);
    const defaultZoneId: string | null = zones.length > 0 && zones[0] ? zones[0].zone_id : null;

    // Step 4: Prepare roads for batch insert
    console.log('Step 4: Preparing roads data...');
    const prepRoadsStep = createStepLogger('Prepare roads');
    const roadsToCreate: Array<{
      osm_id: string;
      name: string;
      name_en: string | null;
      road_type: any;
      max_speed: number | null;
      avg_speed: number | null;
      one_way: boolean;
      lanes: number | null;
      surface: string | null;
      geometry: any;
      zone_id: string | null;
    }> = [];
    const roadsArray: Array<{
      osmId: string;
      name: string;
      coordinates: Array<[number, number]>;
      nodeIds: string[];
    }> = [];
    let generatedNameCount = 0;

    for (let idx = 0; idx < roadWays.length; idx++) {
      const way = roadWays[idx];
      const { name, nameEn, isNamed } = OSMParser.getRoadName(way.tags);
      
      if (!isNamed) {
        generatedNameCount++;
      }
      
      // Now we always have a name (original or generated)
      const roadType = OSMParser.getRoadType(way.tags);
      const { maxSpeed, avgSpeed } = OSMParser.getSpeedInfo(way.tags);
      const oneWay = OSMParser.isOneWay(way.tags);
      const lanes = OSMParser.getLanes(way.tags);
      const surface = OSMParser.getSurface(way.tags);

      // Build geometry
      const coordinates: Array<[number, number]> = [];
      for (const nodeId of way.nodes) {
        const node = osmData.nodes.get(nodeId);
        if (node) {
          coordinates.push([node.lon, node.lat]);
        }
      }

      if (coordinates.length < 2) continue;

      const geometry = {
        type: 'LineString',
        coordinates,
      };

      roadsToCreate.push({
        osm_id: way.id,
        name,
        name_en: nameEn !== undefined ? nameEn : null,
        road_type: roadType as any,
        max_speed: maxSpeed ?? null,
        avg_speed: avgSpeed ?? null,
        one_way: oneWay,
        lanes: lanes ?? null,
        surface: surface ?? null,
        geometry,
        zone_id: defaultZoneId,
      });

      roadsArray.push({
        osmId: way.id,
        name,
        coordinates,
        nodeIds: way.nodes,
      });

      if (roadsArray.length % 1000 === 0 || roadsArray.length === roadWays.length) {
        prepRoadsStep.logProgress(roadsArray.length, roadWays.length);
      }
    }
    prepRoadsStep.done();

    const originallyNamed = roadsArray.length - generatedNameCount;
    console.log(`✓ Prepared ${roadsArray.length} roads (${originallyNamed} có tên gốc, ${generatedNameCount} tên được tạo)\n`);

    // Step 5: Clear old data
    console.log('Step 5: Clearing old data...');
    await prisma.traffic_conditions.deleteMany({});
    await prisma.weight_history.deleteMany({});
    await prisma.user_feedback.deleteMany({});
    await prisma.road_segments.deleteMany({});
    await prisma.road_nodes.deleteMany({});
    await prisma.roads.deleteMany({});
    console.log('✓ Cleared old data\n');

    // Step 6: Batch insert roads
    console.log('Step 6: Inserting roads in batches...');
    const batchSize = 2000;
    for (let i = 0; i < roadsToCreate.length; i += batchSize) {
      const batch = roadsToCreate.slice(i, i + batchSize);
      await prisma.roads.createMany({
        data: batch,
        skipDuplicates: true,
      });
      console.log(`  Inserted ${Math.min(i + batchSize, roadsToCreate.length)}/${roadsToCreate.length} roads...`);
    }
    console.log(`✓ Inserted ${roadsToCreate.length} roads\n`);

    // Step 7: Load created roads and build OSM ID map
    console.log('Step 7: Loading created roads...');
    const createdRoads = await prisma.roads.findMany({
      select: { road_id: true, osm_id: true },
    });
    const osmIdToRoadId = new Map(createdRoads.map(r => [r.osm_id, r.road_id]));
    console.log(`✓ Loaded ${createdRoads.length} roads\n`);

    // Step 8: Build roads structure with DB IDs
    console.log('Step 8: Building roads structure...');
    const roads = roadsArray.map(r => ({
      roadId: osmIdToRoadId.get(r.osmId)!,
      name: r.name,
      coordinates: r.coordinates,
      nodeIds: r.nodeIds,
    })).filter(r => r.roadId); // Filter out any missing
    console.log(`✓ Built ${roads.length} roads\n`);

    // Step 9: Find intersections
    console.log('Step 9: Finding intersections...');
    const intersections = findIntersections(roads);
    console.log(`✓ Found ${intersections.length} intersections\n`);

    // Step 10: Generate segments
    console.log('Step 10: Generating segments...');
    const segments = generateSegments(roads, intersections);
    console.log(`✓ Generated ${segments.length} segments\n`);

    // Step 11: Collect ALL unique nodes from road ways (not just segment endpoints)
    // OSRM annotations may reference any node in the road geometry, so we need ALL nodes
    console.log('Step 11: Collecting all nodes from road ways...');
    const usedNodeIds = new Set<string>();
    
    // Collect from segments (endpoints) - these are definitely needed
    for (const segment of segments) {
      usedNodeIds.add(segment.fromNodeId);
      usedNodeIds.add(segment.toNodeId);
    }
    
    // Also collect ALL nodes from all road ways - OSRM may use intermediate nodes
    for (const way of roadWays) {
      for (const nodeId of way.nodes) {
        usedNodeIds.add(nodeId);
      }
    }
    
    console.log(`✓ Found ${usedNodeIds.size} unique nodes (${segments.length * 2} from segments + ${roadWays.reduce((sum, w) => sum + w.nodes.length, 0)} from ways)\n`);

    // Step 12: Prepare nodes for batch insert
    console.log('Step 12: Preparing nodes data...');
    const nodesToCreate: Array<{
      osm_id: string;
      lat: number;
      lon: number;
      node_type: any;
      zone_id: string | null;
    }> = [];

    // Build intersection node ID → roadsCount map for O(1) lookup
    const intersectionRoadsCount = new Map<string, number>();
    for (const inter of intersections) {
      intersectionRoadsCount.set(inter.nodeId, inter.roads.length);
    }

    const prepareNodesStep = createStepLogger('Prepare nodes');
    let nodeIndex = 0;
    let missingNodeCount = 0;
    const maxMissingLogs = 10;

    for (const osmNodeId of usedNodeIds) {
      const osmNode = osmData.nodes.get(osmNodeId);
      if (!osmNode) {
        missingNodeCount++;
        if (missingNodeCount <= maxMissingLogs) {
          console.warn(`  Warning: OSM node ${osmNodeId} not found`);
        }
        continue;
      }

      // Determine node type: INTERSECTION if it has 3+ connected roads, otherwise WAYPOINT
      const roadsCount = intersectionRoadsCount.get(osmNodeId) ?? 0;
      const nodeType = roadsCount >= 3 ? 'INTERSECTION' : 'WAYPOINT';

      nodesToCreate.push({
        osm_id: osmNodeId,
        lat: osmNode.lat,
        lon: osmNode.lon,
        node_type: nodeType as any,
        zone_id: defaultZoneId,
      });
      nodeIndex++;
      if (nodeIndex % 5000 === 0 || nodeIndex === usedNodeIds.size) {
        prepareNodesStep.logProgress(nodeIndex, usedNodeIds.size);
      }
    }

    const missingExtra =
      missingNodeCount > maxMissingLogs
        ? `, missing nodes skipped=${missingNodeCount}`
        : missingNodeCount > 0
        ? `, missing nodes skipped=${missingNodeCount}`
        : undefined;

    prepareNodesStep.done(missingExtra);
    console.log(`✓ Prepared ${nodesToCreate.length} nodes\n`);

    // Step 13: Batch insert nodes
    console.log('Step 13: Inserting nodes in batches...');
    const insertNodesStep = createStepLogger('Insert nodes');
    for (let i = 0; i < nodesToCreate.length; i += batchSize) {
      const batch = nodesToCreate.slice(i, i + batchSize);
      await prisma.road_nodes.createMany({
        data: batch,
        skipDuplicates: true,
      });
      insertNodesStep.logProgress(Math.min(i + batchSize, nodesToCreate.length), nodesToCreate.length);
    }
    insertNodesStep.done();
    console.log(`✓ Inserted ${nodesToCreate.length} nodes\n`);

    // Step 14: Build node ID map
    console.log('Step 14: Building node ID map...');
    const createdNodes = await prisma.road_nodes.findMany({
      select: { node_id: true, osm_id: true },
    });
    const nodeMap = new Map(createdNodes.map(n => [n.osm_id!, n.node_id]));
    console.log(`✓ Built map with ${nodeMap.size} nodes\n`);

    // Step 15: Load all roads for segment creation
    console.log('Step 15: Loading roads for segments...');
    const allRoads = await prisma.roads.findMany();
    const roadMap = new Map(allRoads.map(r => [r.road_id, r]));
    console.log(`✓ Loaded ${allRoads.length} roads\n`);

    // Step 16: Prepare segments for batch insert
    console.log('Step 16: Preparing segments data...');
    const prepareSegmentsStep = createStepLogger('Prepare segments');
    const segmentsToCreate: Array<{
      from_node_id: string;
      to_node_id: string;
      road_id: string;
      geometry: any;
      length_meters: number;
      name: string;
      road_type: any;
      max_speed: number | null;
      avg_speed: number | null;
      one_way: boolean;
      base_weight: number;
      current_weight: number;
      delta_weight: number;
      zone_id: string | null;
    }> = [];
    let skippedCount = 0;

    for (let idx = 0; idx < segments.length; idx++) {
      const segment = segments[idx];
      const road = roadMap.get(segment.roadId);
      if (!road) {
        skippedCount++;
        continue;
      }

      const fromNodeDbId = nodeMap.get(segment.fromNodeId);
      const toNodeDbId = nodeMap.get(segment.toNodeId);

      if (!fromNodeDbId || !toNodeDbId) {
        skippedCount++;
        continue;
      }

      const lengthMeters = calculateLineLength(segment.coordinates);
      const geometry = {
        type: 'LineString',
        coordinates: segment.coordinates,
      };

      const baseWeight = calculateBaseWeight(
        lengthMeters,
        road.avg_speed || 30,
        road.road_type,
        road.lanes || undefined
      );

      segmentsToCreate.push({
        from_node_id: fromNodeDbId,
        to_node_id: toNodeDbId,
        road_id: road.road_id,
        geometry,
        length_meters: lengthMeters,
        name: road.name,
        road_type: road.road_type,
        max_speed: road.max_speed,
        avg_speed: road.avg_speed,
        one_way: road.one_way,
        base_weight: baseWeight,
        current_weight: baseWeight,
        delta_weight: 0,
        zone_id: road.zone_id,
      });

      if (segmentsToCreate.length % 1000 === 0 || segmentsToCreate.length === segments.length) {
        prepareSegmentsStep.logProgress(segmentsToCreate.length, segments.length);
      }
    }

    prepareSegmentsStep.done(`skipped=${skippedCount}`);
    console.log(`✓ Prepared ${segmentsToCreate.length} segments (skipped ${skippedCount})\n`);

    // Step 17: Batch insert segments
    console.log('Step 17: Inserting segments in batches...');
    const insertSegmentsStep = createStepLogger('Insert segments');
    for (let i = 0; i < segmentsToCreate.length; i += batchSize) {
      const batch = segmentsToCreate.slice(i, i + batchSize);
      await prisma.road_segments.createMany({
        data: batch,
      });
      insertSegmentsStep.logProgress(Math.min(i + batchSize, segmentsToCreate.length), segmentsToCreate.length);
    }
    insertSegmentsStep.done();
    console.log(`✓ Inserted ${segmentsToCreate.length} segments\n`);

    // Step 18: Merge duplicate nodes by coordinates
    await mergeDuplicateNodes();

    // Final summary
    const finalNodeCount = await prisma.road_nodes.count();
    const finalSegmentCount = await prisma.road_segments.count();
    const finalRoadCount = await prisma.roads.count();

    const duration = ((Date.now() - startTime) / 1000).toFixed(1);

    console.log('='.repeat(60));
    console.log('✅ Roads Seeding Completed Successfully!');
    console.log('='.repeat(60));
    console.log(`\nTime taken: ${duration}s`);
    console.log('\nFinal counts:');
    console.log(`  - Roads: ${finalRoadCount}`);
    console.log(`  - Nodes: ${finalNodeCount}`);
    console.log(`  - Segments: ${finalSegmentCount}`);
    console.log('\nNext steps:');
    console.log('  1. Check connectivity: npm run check:connectivity');
    console.log('  2. Generate OSRM data: npm run osrm:generate');
    console.log('  3. Start services: docker-compose up -d');
    console.log('='.repeat(60));

  } catch (error) {
    console.error('\n❌ Seeding failed:', error);
    throw error;
  } finally {
    await prisma.$disconnect();
  }
}

// Run if executed directly
if (import.meta.url === `file://${process.argv[1]}`) {
  seedRoads()
    .then(() => {
      process.exit(0);
    })
    .catch((error) => {
      console.error('Failed:', error);
      process.exit(1);
    });
}

export { seedRoads, mergeDuplicateNodes };
