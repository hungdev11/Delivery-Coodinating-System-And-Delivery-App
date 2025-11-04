/**
 * Debug script to analyze road segments
 * Helps identify issues with segment generation
 */

import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

async function debugSegments() {
  console.log('🔍 Analyzing Road Segments...\n');

  try {
    // 1. Count segments per road
    console.log('📊 Segments per road:');
    const segmentsByRoad = await prisma.$queryRaw<Array<{
      road_id: string;
      name: string;
      segment_count: number;
    }>>`
      SELECT 
        rs.road_id,
        r.name,
        COUNT(*) as segment_count
      FROM road_segments rs
      JOIN roads r ON rs.road_id = r.road_id
      GROUP BY rs.road_id, r.name
      ORDER BY segment_count DESC
      LIMIT 20
    `;

    for (const row of segmentsByRoad) {
      console.log(`  ${row.name}: ${row.segment_count} segments`);
    }
    console.log();

    // 2. Analyze geometry complexity
    console.log('📐 Geometry complexity (nodes per segment):');
    const segments = await prisma.road_segments.findMany({
      take: 100,
      orderBy: { segment_id: 'asc' },
    });

    const nodeCounts: number[] = [];
    for (const segment of segments) {
      if (segment.geometry && typeof segment.geometry === 'object') {
        const geom = segment.geometry as any;
        if (geom.coordinates && Array.isArray(geom.coordinates)) {
          nodeCounts.push(geom.coordinates.length);
        }
      }
    }

    if (nodeCounts.length > 0) {
      const avg = nodeCounts.reduce((a, b) => a + b, 0) / nodeCounts.length;
      const max = Math.max(...nodeCounts);
      const min = Math.min(...nodeCounts);
      console.log(`  Average: ${avg.toFixed(1)} nodes`);
      console.log(`  Min: ${min} nodes`);
      console.log(`  Max: ${max} nodes`);
      
      if (max > 100) {
        console.log(`  ⚠️  Warning: Some segments have ${max} nodes! This may cause OSRM issues.`);
      }
    }
    console.log();

    // 3. Check node reuse (connectivity indicator)
    console.log('🔗 Node connectivity:');
    const nodeUsage = await prisma.$queryRaw<Array<{
      node_id: string;
      usage_count: number;
    }>>`
      SELECT 
        node_id,
        usage_count
      FROM (
        SELECT from_node_id as node_id, COUNT(*) as usage_count 
        FROM road_segments 
        GROUP BY from_node_id
        UNION ALL
        SELECT to_node_id as node_id, COUNT(*) as usage_count 
        FROM road_segments 
        GROUP BY to_node_id
      ) as node_counts
      GROUP BY node_id
      HAVING SUM(usage_count) > 1
      ORDER BY SUM(usage_count) DESC
      LIMIT 10
    `;

    console.log(`  Found ${nodeUsage.length} nodes used by multiple segments`);
    if (nodeUsage.length > 0) {
      const topNode = nodeUsage[0];
      console.log(`  Most connected node: ${topNode.node_id} (${topNode.usage_count} connections)`);
    }
    console.log();

    // 4. Check for isolated segments
    console.log('🏝️  Checking for isolated segments...');
    const totalSegments = await prisma.road_segments.count();
    const connectedNodes = new Set<string>();
    
    const allSegments = await prisma.road_segments.findMany({
      select: { from_node_id: true, to_node_id: true },
    });

    const nodeConnections = new Map<string, number>();
    for (const seg of allSegments) {
      nodeConnections.set(seg.from_node_id, (nodeConnections.get(seg.from_node_id) || 0) + 1);
      nodeConnections.set(seg.to_node_id, (nodeConnections.get(seg.to_node_id) || 0) + 1);
    }

    const isolatedNodes = Array.from(nodeConnections.entries())
      .filter(([_, count]) => count === 1)
      .length;

    const connectedNodeCount = nodeConnections.size - isolatedNodes;
    const connectivityRatio = (connectedNodeCount / nodeConnections.size) * 100;

    console.log(`  Total segments: ${totalSegments}`);
    console.log(`  Total nodes: ${nodeConnections.size}`);
    console.log(`  Isolated nodes (dead ends): ${isolatedNodes}`);
    console.log(`  Connected nodes: ${connectedNodeCount} (${connectivityRatio.toFixed(1)}%)`);
    console.log();

    // 5. Sample a few segments for manual inspection
    console.log('🔬 Sample segments:');
    const samples = await prisma.road_segments.findMany({
      take: 3,
      include: {
        from_node: true,
        to_node: true,
        road: true,
      },
    });

    for (const seg of samples) {
      const geom = seg.geometry as any;
      const nodeCount = geom?.coordinates?.length || 0;
      console.log(`  ${seg.road.name}:`);
      console.log(`    From: (${seg.from_node.lat}, ${seg.from_node.lon})`);
      console.log(`    To: (${seg.to_node.lat}, ${seg.to_node.lon})`);
      console.log(`    Geometry nodes: ${nodeCount}`);
      console.log(`    Length: ${seg.length_meters.toFixed(0)}m`);
    }
    console.log();

    console.log('✅ Analysis complete!');
    console.log('\n💡 Tips:');
    console.log('  • If segments have >100 nodes, reseed with lower MAX_NODES_PER_SEGMENT');
    console.log('  • If connectivity ratio <80%, check intersection detection logic');
    console.log('  • If many roads have only 1 segment, check if segmentation is working');

  } catch (error) {
    console.error('❌ Analysis failed:', error);
    throw error;
  } finally {
    await prisma.$disconnect();
  }
}

// Run if executed directly
if (import.meta.url === `file://${process.argv[1]}`) {
  debugSegments()
    .then(() => process.exit(0))
    .catch((error) => {
      console.error('Failed:', error);
      process.exit(1);
    });
}

export { debugSegments };
