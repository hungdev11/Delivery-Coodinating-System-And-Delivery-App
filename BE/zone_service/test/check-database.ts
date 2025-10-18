/**
 * Check database content
 */

import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

async function checkDatabase() {
  console.log('🔍 Checking database content...');
  console.log('='.repeat(50));

  try {
    // Check centers
    const centers = await prisma.centers.findMany();
    console.log(`📊 Centers: ${centers.length}`);
    if (centers.length > 0) {
      console.log('Sample center:', centers[0]);
    }

    // Check zones
    const zones = await prisma.zones.findMany();
    console.log(`📊 Zones: ${zones.length}`);
    if (zones.length > 0) {
      console.log('Sample zone:', zones[0]);
    }

    // Check roads
    const roads = await prisma.roads.findMany();
    console.log(`📊 Roads: ${roads.length}`);
    if (roads.length > 0) {
      console.log('Sample road:', roads[0]);
    }

    // Check road segments
    const segments = await prisma.road_segments.findMany();
    console.log(`📊 Road Segments: ${segments.length}`);
    if (segments.length > 0) {
      console.log('Sample segment:', segments[0]);
    }

    // Check road nodes
    const nodes = await prisma.road_nodes.findMany();
    console.log(`📊 Road Nodes: ${nodes.length}`);
    if (nodes.length > 0) {
      console.log('Sample node:', nodes[0]);
    }

    // Check if we have any data in Thu Duc area
    if (segments.length > 0) {
      const sampleSegment = segments[0];
      console.log('\n📍 Sample segment geometry:');
      console.log(JSON.stringify(sampleSegment.geometry, null, 2));
    }

  } catch (error) {
    console.error('❌ Database check failed:', error);
  } finally {
    await prisma.$disconnect();
  }
}

checkDatabase();
