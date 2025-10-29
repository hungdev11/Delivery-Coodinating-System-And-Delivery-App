/**
 * Test TomTom Traffic Service
 */

import { TomTomTrafficService } from '../services/tomtom-traffic-service.js';
import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

async function testTrafficService() {
  console.log('🧪 Testing TomTom Traffic Service...');
  console.log('='.repeat(50));

  try {
    const trafficService = new TomTomTrafficService();
    
    // Test with a known working bounding box (from TomTom docs)
    // This is a small area that should work
    const boundingBox = '10.7,106.6,10.8,106.7'; // Small area in HCMC
    
    console.log('📡 Testing traffic model ID fetch...');
    const trafficModelId = await trafficService.getTrafficModelId(boundingBox, 5); // Lower zoom level
    console.log(`✅ Traffic Model ID: ${trafficModelId}`);
    
    console.log('\n🚨 Testing traffic incidents fetch...');
    const incidents = await trafficService.fetchTrafficIncidents(boundingBox, trafficModelId, 5); // Lower zoom level
    console.log(`✅ Found ${incidents.length} incidents`);
    
    if (incidents.length > 0) {
      console.log('\n📊 Sample incident:');
      console.log(JSON.stringify(incidents[0], null, 2));
      
      console.log('\n💾 Testing database update...');
      await trafficService.updateTrafficConditions(incidents);
      
      // Check what was created
      const trafficConditions = await prisma.traffic_conditions.findMany({
        where: {
          source: 'tomtom'
        },
        include: {
          road_segment: true
        },
        take: 5
      });
      
      console.log(`✅ Created ${trafficConditions.length} traffic conditions`);
      
      if (trafficConditions.length > 0) {
        console.log('\n📋 Sample traffic condition:');
        console.log(JSON.stringify(trafficConditions[0], null, 2));
      }
    }
    
    console.log('\n🧹 Testing cleanup...');
    await trafficService.cleanupExpiredConditions();
    console.log('✅ Cleanup completed');
    
  } catch (error) {
    console.error('❌ Test failed:', error);
  } finally {
    await prisma.$disconnect();
  }
}

testTrafficService();
