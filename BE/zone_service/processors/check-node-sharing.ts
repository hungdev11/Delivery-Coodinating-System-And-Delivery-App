/**
 * Check if roads share node IDs at intersection points
 * This is critical for segment connectivity
 */

import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

async function checkNodeSharing() {
  console.log('🔍 Analyzing Node Sharing Between Roads...\n');

  try {
    // 1. Get all road_nodes and check OSM IDs
    console.log('📊 Road Nodes Analysis:');
    const nodes = await prisma.road_nodes.findMany({
      take: 1000,
      select: {
        node_id: true,
        osm_id: true,
        lat: true,
        lon: true,
      },
    });

    const withOsmId = nodes.filter(n => n.osm_id).length;
    const withoutOsmId = nodes.length - withOsmId;
    
    console.log(`  Total nodes sampled: ${nodes.length}`);
    console.log(`  With OSM ID: ${withOsmId}`);
    console.log(`  Without OSM ID: ${withoutOsmId}`);
    console.log();

    // 2. Check coordinate duplication
    console.log('📍 Coordinate Duplication Analysis:');
    const coordMap = new Map<string, number>();
    
    const allNodes = await prisma.road_nodes.findMany({
      select: { lat: true, lon: true },
    });

    for (const node of allNodes) {
      const key = `${node.lat.toFixed(7)},${node.lon.toFixed(7)}`;
      coordMap.set(key, (coordMap.get(key) || 0) + 1);
    }

    const duplicateCoords = Array.from(coordMap.values()).filter(count => count > 1);
    const totalDuplicates = duplicateCoords.reduce((sum, count) => sum + count, 0);
    
    console.log(`  Total unique coordinates: ${coordMap.size}`);
    console.log(`  Total nodes: ${allNodes.length}`);
    console.log(`  Duplicate nodes (same coords): ${totalDuplicates}`);
    console.log(`  Duplication ratio: ${((totalDuplicates / allNodes.length) * 100).toFixed(1)}%`);
    console.log();

    // 3. Check segment node reuse
    console.log('🔗 Segment Node Reuse:');
    const segmentNodes = await prisma.$queryRaw<Array<{
      node_id: string;
      usage_count: bigint;
    }>>`
      SELECT node_id, SUM(cnt) as usage_count
      FROM (
        SELECT from_node_id as node_id, COUNT(*) as cnt FROM road_segments GROUP BY from_node_id
        UNION ALL
        SELECT to_node_id as node_id, COUNT(*) as cnt FROM road_segments GROUP BY to_node_id
      ) as combined
      GROUP BY node_id
      ORDER BY usage_count DESC
      LIMIT 20
    `;

    console.log('  Top 20 most-used nodes:');
    for (const node of segmentNodes) {
      const count = Number(node.usage_count);
      console.log(`    Node ${node.node_id.substring(0, 8)}...: ${count} segments`);
    }
    console.log();

    // 4. Sample intersection check
    console.log('🛣️  Sample Intersection Check:');
    
    // Find a coordinate with multiple nodes
    const duplicateCoord = Array.from(coordMap.entries())
      .find(([_, count]) => count > 1);
    
    if (duplicateCoord) {
      const [coordKey, count] = duplicateCoord;
      const [latStr, lonStr] = coordKey.split(',');
      const lat = parseFloat(latStr);
      const lon = parseFloat(lonStr);
      
      console.log(`  Found ${count} nodes at coordinate (${lat.toFixed(7)}, ${lon.toFixed(7)})`);
      
      const nodesAtCoord = await prisma.road_nodes.findMany({
        where: {
          lat: { gte: lat - 0.0000001, lte: lat + 0.0000001 },
          lon: { gte: lon - 0.0000001, lte: lon + 0.0000001 },
        },
        include: {
          segments_from: {
            take: 2,
            include: { road: { select: { name: true } } },
          },
          segments_to: {
            take: 2,
            include: { road: { select: { name: true } } },
          },
        },
      });

      console.log(`  Database nodes at this location: ${nodesAtCoord.length}`);
      for (const node of nodesAtCoord) {
        const fromCount = node.segments_from.length;
        const toCount = node.segments_to.length;
        const total = fromCount + toCount;
        console.log(`    Node ${node.node_id.substring(0, 8)}... (OSM: ${node.osm_id || 'NONE'}): ${total} segments`);
        
        if (node.segments_from.length > 0) {
          console.log(`      From segments: ${node.segments_from.map(s => s.road.name).join(', ')}`);
        }
        if (node.segments_to.length > 0) {
          console.log(`      To segments: ${node.segments_to.map(s => s.road.name).join(', ')}`);
        }
      }
    }
    console.log();

    // 5. Analysis summary
    console.log('📋 Summary & Diagnosis:');
    
    const duplicateRatio = (totalDuplicates / allNodes.length) * 100;
    
    if (duplicateRatio > 30) {
      console.log('  ⚠️  HIGH DUPLICATION: Many nodes at same coordinates');
      console.log('  → Problem: Intersection detection is working, but nodes are NOT being reused');
      console.log('  → Solution: Roads need to SHARE the same node ID at intersection points');
      console.log('  → Check: OSM parsing - are node IDs from original OSM data consistent?');
    } else {
      console.log('  ⚠️  LOW DUPLICATION: Nodes at intersections are separate');
      console.log('  → Problem: Roads are not sharing nodes at intersection coordinates');
      console.log('  → Solution: Need to merge nodes by coordinate BEFORE creating segments');
    }
    console.log();

    console.log('💡 Recommended fixes:');
    console.log('  1. In roads-seeder: Build a coordKey→nodeId map FIRST');
    console.log('  2. When processing road.nodeIds, replace with shared node from map');
    console.log('  3. This ensures segments at same coordinate use same node_id');
    console.log('  4. Re-run seed:roads after fix');

  } catch (error) {
    console.error('❌ Analysis failed:', error);
    throw error;
  } finally {
    await prisma.$disconnect();
  }
}

// Run if executed directly
if (import.meta.url === `file://${process.argv[1]}`) {
  checkNodeSharing()
    .then(() => process.exit(0))
    .catch((error) => {
      console.error('Failed:', error);
      process.exit(1);
    });
}

export { checkNodeSharing };
