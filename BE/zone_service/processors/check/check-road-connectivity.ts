/**
 * Check Road Network Connectivity
 * Analyzes the road network to find connected components
 */

import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

async function checkConnectivity() {
  console.log('🔍 Analyzing Road Network Connectivity\n');

  // Get all segments
  const segments = await prisma.road_segments.findMany({
    select: {
      segment_id: true,
      from_node_id: true,
      to_node_id: true,
      road_id: true,
    },
  });

  console.log(`Total segments: ${segments.length}`);

  // Build adjacency graph
  const graph = new Map<string, Set<string>>();

  for (const segment of segments) {
    if (!graph.has(segment.from_node_id)) {
      graph.set(segment.from_node_id, new Set());
    }
    if (!graph.has(segment.to_node_id)) {
      graph.set(segment.to_node_id, new Set());
    }

    graph.get(segment.from_node_id)!.add(segment.to_node_id);
    graph.get(segment.to_node_id)!.add(segment.from_node_id); // bidirectional
  }

  console.log(`Total nodes in graph: ${graph.size}\n`);

  // Find connected components using DFS
  const visited = new Set<string>();
  const components: string[][] = [];

  function dfs(node: string, component: string[]) {
    visited.add(node);
    component.push(node);

    const neighbors = graph.get(node);
    if (neighbors) {
      for (const neighbor of neighbors) {
        if (!visited.has(neighbor)) {
          dfs(neighbor, component);
        }
      }
    }
  }

  for (const node of graph.keys()) {
    if (!visited.has(node)) {
      const component: string[] = [];
      dfs(node, component);
      components.push(component);
    }
  }

  console.log(`\n📊 Connected Components Analysis:`);
  console.log(`Total connected components: ${components.length}`);

  // Sort components by size
  components.sort((a, b) => b.length - a.length);

  console.log(`\nTop 10 largest components:`);
  for (let i = 0; i < Math.min(10, components.length); i++) {
    const size = components[i].length;
    const percentage = ((size / graph.size) * 100).toFixed(2);
    console.log(`  Component ${i + 1}: ${size} nodes (${percentage}%)`);
  }

  // Get sample nodes from largest component
  if (components.length > 0) {
    const largestComponent = components[0];
    const sampleNodeIds = largestComponent.slice(0, 5);

    console.log(`\n🎯 Sample routable nodes from largest component:`);
    const sampleNodes = await prisma.road_nodes.findMany({
      where: { node_id: { in: sampleNodeIds } },
      select: { node_id: true, lat: true, lon: true },
      take: 4,
    });

    for (const node of sampleNodes) {
      console.log(`  Node: ${node.node_id}`);
      console.log(`    Lat: ${node.lat}, Lon: ${node.lon}`);
    }

    // Try a route between two nodes in the largest component
    if (sampleNodes.length >= 2) {
      const [start, end] = sampleNodes;
      console.log(`\n🚗 Test route in OSRM:`);
      console.log(`  curl "http://localhost:5000/route/v1/driving/${start.lon},${start.lat};${end.lon},${end.lat}?overview=false"`);
    }
  }

  // Check for isolated nodes (degree 0 or 1)
  let isolatedCount = 0;
  let lowDegreeCount = 0;

  for (const [node, neighbors] of graph.entries()) {
    if (neighbors.size === 0) isolatedCount++;
    else if (neighbors.size === 1) lowDegreeCount++;
  }

  console.log(`\n⚠️  Network Issues:`);
  console.log(`  Isolated nodes (degree 0): ${isolatedCount}`);
  console.log(`  Dead-end nodes (degree 1): ${lowDegreeCount}`);

  if (components.length > 100) {
    console.log(`  ⚠️  Too many disconnected components (${components.length})!`);
    console.log(`     This will cause frequent routing failures.`);
  }

  await prisma.$disconnect();
  process.exit(0);
}

checkConnectivity().catch(console.error);
