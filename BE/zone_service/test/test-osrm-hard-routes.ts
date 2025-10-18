/**
 * OSRM Stress Test - Hard Routes
 * Tests routing between Phạm Văn Đồng and Man Thiện Street
 *
 * Success criteria:
 * - Route found (not "NoRoute")
 * - Distance > 0
 * - Duration > 0
 * - Reasonable distance (not crazy long detours)
 */

import { PrismaClient } from '@prisma/client';
import axios from 'axios';

const prisma = new PrismaClient();

interface TestResult {
  testNumber: number;
  fromStreet: string;
  toStreet: string;
  fromCoords: { lat: number; lon: number };
  toCoords: { lat: number; lon: number };
  success: boolean;
  distance?: number;
  duration?: number;
  error?: string;
  osrmInstance: number;
}

async function testRouting() {
  console.log('🔥 OSRM Hard Stress Test');
  console.log('Testing routes: Phạm Văn Đồng → Man Thiện Street\n');
  console.log('='.repeat(70));

  try {
    // Step 1: Find segments on Phạm Văn Đồng
    console.log('\n📍 Finding points on Phạm Văn Đồng...');
    const phamVanDongSegments = await prisma.road_segments.findMany({
      where: {
        name: {
          contains: 'Phạm Văn Đồng',
          mode: 'insensitive',
        },
      },
      include: {
        from_node: true,
        to_node: true,
      },
      take: 20,
    });

    if (phamVanDongSegments.length === 0) {
      console.log('⚠️  No segments found for Phạm Văn Đồng');
      console.log('   Searching for similar names...');

      const similarNames = await prisma.road_segments.findMany({
        where: {
          name: {
            contains: 'Phạm',
            mode: 'insensitive',
          },
        },
        distinct: ['name'],
        take: 10,
      });

      console.log('   Similar street names found:');
      similarNames.forEach(s => console.log(`     - ${s.name}`));
      return;
    }

    console.log(`✓ Found ${phamVanDongSegments.length} segments on Phạm Văn Đồng`);

    // Step 2: Find segments on Man Thiện
    console.log('\n📍 Finding points on Man Thiện Street...');
    const manThienSegments = await prisma.road_segments.findMany({
      where: {
        name: {
          equals: 'Man Thiện',
          mode: 'insensitive',
        },
      },
      include: {
        from_node: true,
        to_node: true,
      },
      take: 20,
    });

    if (manThienSegments.length === 0) {
      console.log('⚠️  No segments found for Man Thiện');
      console.log('   Searching for similar names...');

      const similarNames = await prisma.road_segments.findMany({
        where: {
          OR: [
            { name: { contains: 'Man', mode: 'insensitive' } },
            { name: { contains: 'Thiện', mode: 'insensitive' } },
          ],
        },
        distinct: ['name'],
        take: 10,
      });

      console.log('   Similar street names found:');
      similarNames.forEach(s => console.log(`     - ${s.name}`));
      return;
    }

    console.log(`✓ Found ${manThienSegments.length} segments on Man Thiện`);

    // Step 3: Extract random coordinates
    console.log('\n📊 Selecting test points...');

    const phamVanDongPoints = phamVanDongSegments.slice(0, 10).map((seg, i) => ({
      name: seg.name,
      lat: seg.from_node.lat,
      lon: seg.from_node.lon,
      index: i + 1,
    }));

    const manThienPoints = manThienSegments.slice(0, 10).map((seg, i) => ({
      name: seg.name,
      lat: seg.from_node.lat,
      lon: seg.from_node.lon,
      index: i + 1,
    }));

    console.log(`✓ Selected ${phamVanDongPoints.length} points from Phạm Văn Đồng`);
    console.log(`✓ Selected ${manThienPoints.length} points from Man Thiện\n`);

    // Step 4: Run tests
    console.log('='.repeat(70));
    console.log('🚗 Starting Routing Tests...\n');

    const results: TestResult[] = [];
    const testCount = Math.min(10, phamVanDongPoints.length, manThienPoints.length);

    for (let i = 0; i < testCount; i++) {
      const fromPoint = phamVanDongPoints[i];
      const toPoint = manThienPoints[i];
      const osrmInstance = (i % 2) + 1; // Alternate between instances
      const osrmPort = osrmInstance === 1 ? 5000 : 5001;

      console.log(`\nTest ${i + 1}/${testCount}:`);
      console.log(`  From: ${fromPoint.name} (${fromPoint.lat.toFixed(6)}, ${fromPoint.lon.toFixed(6)})`);
      console.log(`  To: ${toPoint.name} (${toPoint.lat.toFixed(6)}, ${toPoint.lon.toFixed(6)})`);
      console.log(`  OSRM Instance: ${osrmInstance} (port ${osrmPort})`);

      try {
        const url = `http://localhost:${osrmPort}/route/v1/driving/${fromPoint.lon},${fromPoint.lat};${toPoint.lon},${toPoint.lat}?overview=false&steps=true`;

        const response = await axios.get(url, { timeout: 5000 });

        if (response.data.code === 'Ok' && response.data.routes && response.data.routes.length > 0) {
          const route = response.data.routes[0];
          const distanceKm = (route.distance / 1000).toFixed(2);
          const durationMin = (route.duration / 60).toFixed(1);

          console.log(`  ✅ SUCCESS`);
          console.log(`     Distance: ${distanceKm} km`);
          console.log(`     Duration: ${durationMin} min`);
          console.log(`     Steps: ${route.legs[0].steps.length} steps`);

          results.push({
            testNumber: i + 1,
            fromStreet: fromPoint.name,
            toStreet: toPoint.name,
            fromCoords: { lat: fromPoint.lat, lon: fromPoint.lon },
            toCoords: { lat: toPoint.lat, lon: toPoint.lon },
            success: true,
            distance: route.distance,
            duration: route.duration,
            osrmInstance,
          });
        } else {
          console.log(`  ❌ FAILED: ${response.data.code || 'Unknown error'}`);

          results.push({
            testNumber: i + 1,
            fromStreet: fromPoint.name,
            toStreet: toPoint.name,
            fromCoords: { lat: fromPoint.lat, lon: fromPoint.lon },
            toCoords: { lat: toPoint.lat, lon: toPoint.lon },
            success: false,
            error: response.data.code || 'Unknown error',
            osrmInstance,
          });
        }
      } catch (error: any) {
        console.log(`  ❌ ERROR: ${error.message}`);

        results.push({
          testNumber: i + 1,
          fromStreet: fromPoint.name,
          toStreet: toPoint.name,
          fromCoords: { lat: fromPoint.lat, lon: fromPoint.lon },
          toCoords: { lat: toPoint.lat, lon: toPoint.lon },
          success: false,
          error: error.message,
          osrmInstance,
        });
      }

      // Small delay between requests
      await new Promise(resolve => setTimeout(resolve, 100));
    }

    // Step 5: Summary
    console.log('\n' + '='.repeat(70));
    console.log('📊 TEST RESULTS SUMMARY');
    console.log('='.repeat(70));

    const successCount = results.filter(r => r.success).length;
    const failCount = results.filter(r => !r.success).length;
    const successRate = ((successCount / results.length) * 100).toFixed(1);

    console.log(`\nTotal Tests: ${results.length}`);
    console.log(`✅ Successful: ${successCount}`);
    console.log(`❌ Failed: ${failCount}`);
    console.log(`📈 Success Rate: ${successRate}%\n`);

    // Success breakdown by instance
    const instance1Success = results.filter(r => r.osrmInstance === 1 && r.success).length;
    const instance2Success = results.filter(r => r.osrmInstance === 2 && r.success).length;
    const instance1Total = results.filter(r => r.osrmInstance === 1).length;
    const instance2Total = results.filter(r => r.osrmInstance === 2).length;

    console.log('Instance Performance:');
    console.log(`  Instance 1 (port 5000): ${instance1Success}/${instance1Total} (${((instance1Success/instance1Total)*100).toFixed(1)}%)`);
    console.log(`  Instance 2 (port 5001): ${instance2Success}/${instance2Total} (${((instance2Success/instance2Total)*100).toFixed(1)}%)`);

    // Distance statistics
    if (successCount > 0) {
      const successfulResults = results.filter(r => r.success && r.distance);
      const distances = successfulResults.map(r => r.distance! / 1000);
      const durations = successfulResults.map(r => r.duration! / 60);

      const avgDistance = (distances.reduce((a, b) => a + b, 0) / distances.length).toFixed(2);
      const minDistance = Math.min(...distances).toFixed(2);
      const maxDistance = Math.max(...distances).toFixed(2);

      const avgDuration = (durations.reduce((a, b) => a + b, 0) / durations.length).toFixed(1);
      const minDuration = Math.min(...durations).toFixed(1);
      const maxDuration = Math.max(...durations).toFixed(1);

      console.log('\nRoute Statistics:');
      console.log(`  Distance: ${minDistance} - ${maxDistance} km (avg: ${avgDistance} km)`);
      console.log(`  Duration: ${minDuration} - ${maxDuration} min (avg: ${avgDuration} min)`);
      console.log(`  Avg Speed: ${((parseFloat(avgDistance) / (parseFloat(avgDuration) / 60))).toFixed(1)} km/h`);
    }

    // Failed routes details
    if (failCount > 0) {
      console.log('\n❌ Failed Routes Details:');
      results.filter(r => !r.success).forEach(r => {
        console.log(`  Test ${r.testNumber}: ${r.error || 'NoRoute'}`);
        console.log(`    From: ${r.fromStreet} (${r.fromCoords.lat.toFixed(6)}, ${r.fromCoords.lon.toFixed(6)})`);
        console.log(`    To: ${r.toStreet} (${r.toCoords.lat.toFixed(6)}, ${r.toCoords.lon.toFixed(6)})`);
      });
    }

    // Grade the results
    console.log('\n' + '='.repeat(70));
    console.log('🎯 FINAL GRADE');
    console.log('='.repeat(70));

    let grade = '';
    let emoji = '';

    if (parseFloat(successRate) >= 90) {
      grade = 'EXCELLENT';
      emoji = '🏆';
    } else if (parseFloat(successRate) >= 75) {
      grade = 'GOOD';
      emoji = '✅';
    } else if (parseFloat(successRate) >= 50) {
      grade = 'ACCEPTABLE';
      emoji = '⚠️';
    } else {
      grade = 'NEEDS IMPROVEMENT';
      emoji = '❌';
    }

    console.log(`\n${emoji} Grade: ${grade}`);
    console.log(`Success Rate: ${successRate}%`);

    if (parseFloat(successRate) >= 75) {
      console.log('\n✨ OSRM routing is working well!');
    } else if (parseFloat(successRate) >= 50) {
      console.log('\n⚠️  Routing is partially working. Some points may be in disconnected areas.');
    } else {
      console.log('\n❌ Routing needs improvement. Check network connectivity.');
    }

    console.log('\n' + '='.repeat(70));

  } catch (error) {
    console.error('\n❌ Test failed:', error);
    throw error;
  } finally {
    await prisma.$disconnect();
  }
}

// Run if executed directly
if (import.meta.url === `file://${process.argv[1]}`) {
  testRouting()
    .then(() => {
      process.exit(0);
    })
    .catch((error) => {
      console.error('Failed:', error);
      process.exit(1);
    });
}

export { testRouting };
