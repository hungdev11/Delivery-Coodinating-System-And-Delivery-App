/**
 * Comprehensive Service Stress Test
 *
 * Tests:
 * 1. Routing API (OSRM)
 * 2. Address Nearest Query API
 * 3. Address Search API
 * 4. Concurrent load testing
 */

import { PrismaClient } from '@prisma/client';
import axios from 'axios';

const prisma = new PrismaClient();
const API_BASE = process.env.API_BASE || 'http://localhost:21503/api/v1';

interface RoutingTestResult {
  testNumber: number;
  fromCoords: { lat: number; lon: number };
  toCoords: { lat: number; lon: number };
  success: boolean;
  distance?: number;
  duration?: number;
  responseTime: number;
  error?: string;
}

interface AddressTestResult {
  testNumber: number;
  queryCoords: { lat: number; lon: number };
  success: boolean;
  resultCount: number;
  responseTime: number;
  nearestDistance?: number;
  error?: string;
}

interface LoadTestResult {
  endpoint: string;
  totalRequests: number;
  successCount: number;
  failCount: number;
  avgResponseTime: number;
  minResponseTime: number;
  maxResponseTime: number;
  requestsPerSecond: number;
}

/**
 * Test 1: Routing API
 */
async function testRoutingAPI(): Promise<RoutingTestResult[]> {
  console.log('\n' + '='.repeat(70));
  console.log('🚗 TEST 1: ROUTING API');
  console.log('='.repeat(70));

  const results: RoutingTestResult[] = [];

  try {
    // Get random road nodes as test points
    const nodes = await prisma.road_nodes.findMany({
      take: 20,
      orderBy: { node_id: 'asc' }
    });

    if (nodes.length < 10) {
      console.log('⚠️  Not enough road nodes for testing');
      return results;
    }

    const testCount = 10;
    console.log(`\nRunning ${testCount} routing tests...\n`);

    for (let i = 0; i < testCount; i++) {
      const fromNode = nodes[i];
      const toNode = nodes[testCount + i];

      console.log(`Test ${i + 1}/${testCount}:`);
      console.log(`  From: (${fromNode.lat.toFixed(6)}, ${fromNode.lon.toFixed(6)})`);
      console.log(`  To: (${toNode.lat.toFixed(6)}, ${toNode.lon.toFixed(6)})`);

      const startTime = Date.now();

      try {
        const response = await axios.get(`${API_BASE}/routing/route`, {
          params: {
            fromLat: fromNode.lat,
            fromLon: fromNode.lon,
            toLat: toNode.lat,
            toLon: toNode.lon
          },
          timeout: 5000
        });

        const responseTime = Date.now() - startTime;

        if (response.data.result && response.data.result.distance > 0) {
          const distanceKm = (response.data.result.distance / 1000).toFixed(2);
          const durationMin = (response.data.result.duration / 60).toFixed(1);

          console.log(`  ✅ SUCCESS (${responseTime}ms)`);
          console.log(`     Distance: ${distanceKm} km, Duration: ${durationMin} min\n`);

          results.push({
            testNumber: i + 1,
            fromCoords: { lat: fromNode.lat, lon: fromNode.lon },
            toCoords: { lat: toNode.lat, lon: toNode.lon },
            success: true,
            distance: response.data.result.distance,
            duration: response.data.result.duration,
            responseTime
          });
        } else {
          console.log(`  ❌ FAILED: No route found\n`);
          results.push({
            testNumber: i + 1,
            fromCoords: { lat: fromNode.lat, lon: fromNode.lon },
            toCoords: { lat: toNode.lat, lon: toNode.lon },
            success: false,
            responseTime,
            error: 'No route'
          });
        }
      } catch (error: any) {
        const responseTime = Date.now() - startTime;
        console.log(`  ❌ ERROR: ${error.message} (${responseTime}ms)\n`);

        results.push({
          testNumber: i + 1,
          fromCoords: { lat: fromNode.lat, lon: fromNode.lon },
          toCoords: { lat: toNode.lat, lon: toNode.lon },
          success: false,
          responseTime,
          error: error.message
        });
      }

      await new Promise(resolve => setTimeout(resolve, 100));
    }
  } catch (error: any) {
    console.error('Routing test setup failed:', error);
  }

  return results;
}

/**
 * Test 2: Address Nearest Query API
 */
async function testAddressNearestAPI(): Promise<AddressTestResult[]> {
  console.log('\n' + '='.repeat(70));
  console.log('📍 TEST 2: ADDRESS NEAREST QUERY API');
  console.log('='.repeat(70));

  const results: AddressTestResult[] = [];

  try {
    // Get sample coordinates from road nodes
    const nodes = await prisma.road_nodes.findMany({
      take: 20,
      orderBy: { node_id: 'asc' }
    });

    if (nodes.length === 0) {
      console.log('⚠️  No road nodes found for testing');
      return results;
    }

    const testCount = 15;
    console.log(`\nRunning ${testCount} nearest address queries...\n`);

    for (let i = 0; i < Math.min(testCount, nodes.length); i++) {
      const node = nodes[i];

      console.log(`Test ${i + 1}/${testCount}:`);
      console.log(`  Query Point: (${node.lat.toFixed(6)}, ${node.lon.toFixed(6)})`);

      const startTime = Date.now();

      try {
        const response = await axios.get(`${API_BASE}/addresses/nearest`, {
          params: {
            lat: node.lat,
            lon: node.lon,
            limit: 10,
            maxDistance: 1000 // 1km
          },
          timeout: 3000
        });

        const responseTime = Date.now() - startTime;
        const addresses = response.data.result || [];

        if (addresses.length > 0) {
          const nearest = addresses[0];
          console.log(`  ✅ SUCCESS (${responseTime}ms)`);
          console.log(`     Found: ${addresses.length} addresses`);
          console.log(`     Nearest: ${nearest.name} (${nearest.distance.toFixed(1)}m away)\n`);

          results.push({
            testNumber: i + 1,
            queryCoords: { lat: node.lat, lon: node.lon },
            success: true,
            resultCount: addresses.length,
            responseTime,
            nearestDistance: nearest.distance
          });
        } else {
          console.log(`  ⚠️  No addresses found within 1km (${responseTime}ms)\n`);

          results.push({
            testNumber: i + 1,
            queryCoords: { lat: node.lat, lon: node.lon },
            success: true,
            resultCount: 0,
            responseTime
          });
        }
      } catch (error: any) {
        const responseTime = Date.now() - startTime;
        console.log(`  ❌ ERROR: ${error.message} (${responseTime}ms)\n`);

        results.push({
          testNumber: i + 1,
          queryCoords: { lat: node.lat, lon: node.lon },
          success: false,
          resultCount: 0,
          responseTime,
          error: error.message
        });
      }

      await new Promise(resolve => setTimeout(resolve, 50));
    }
  } catch (error: any) {
    console.error('Address test setup failed:', error);
  }

  return results;
}

/**
 * Test 3: Address Search API
 */
async function testAddressSearchAPI(): Promise<AddressTestResult[]> {
  console.log('\n' + '='.repeat(70));
  console.log('🔍 TEST 3: ADDRESS SEARCH API');
  console.log('='.repeat(70));

  const results: AddressTestResult[] = [];
  const searchTerms = ['school', 'hospital', 'bank', 'restaurant', 'hotel'];

  console.log(`\nRunning ${searchTerms.length} search queries...\n`);

  for (let i = 0; i < searchTerms.length; i++) {
    const term = searchTerms[i];

    console.log(`Test ${i + 1}/${searchTerms.length}:`);
    console.log(`  Search: "${term}"`);

    const startTime = Date.now();

    try {
      const response = await axios.get(`${API_BASE}/addresses`, {
        params: {
          search: term,
          page: 0,
          size: 20
        },
        timeout: 3000
      });

      const responseTime = Date.now() - startTime;
      const data = response.data.result;

      if (data && data.data) {
        console.log(`  ✅ SUCCESS (${responseTime}ms)`);
        console.log(`     Found: ${data.data.length} results (total: ${data.page.totalElements})\n`);

        results.push({
          testNumber: i + 1,
          queryCoords: { lat: 0, lon: 0 },
          success: true,
          resultCount: data.data.length,
          responseTime
        });
      } else {
        console.log(`  ⚠️  No results (${responseTime}ms)\n`);

        results.push({
          testNumber: i + 1,
          queryCoords: { lat: 0, lon: 0 },
          success: true,
          resultCount: 0,
          responseTime
        });
      }
    } catch (error: any) {
      const responseTime = Date.now() - startTime;
      console.log(`  ❌ ERROR: ${error.message} (${responseTime}ms)\n`);

      results.push({
        testNumber: i + 1,
        queryCoords: { lat: 0, lon: 0 },
        success: false,
        resultCount: 0,
        responseTime,
        error: error.message
      });
    }

    await new Promise(resolve => setTimeout(resolve, 50));
  }

  return results;
}

/**
 * Test 4: Concurrent Load Test
 */
async function testConcurrentLoad(): Promise<LoadTestResult[]> {
  console.log('\n' + '='.repeat(70));
  console.log('⚡ TEST 4: CONCURRENT LOAD TEST');
  console.log('='.repeat(70));

  const results: LoadTestResult[] = [];

  try {
    // Get sample node for testing
    const node = await prisma.road_nodes.findFirst();
    if (!node) {
      console.log('⚠️  No road nodes found');
      return results;
    }

    const testConfig = [
      { name: 'Address Nearest', endpoint: `${API_BASE}/addresses/nearest?lat=${node.lat}&lon=${node.lon}&limit=10`, concurrent: 20 },
      { name: 'Address List', endpoint: `${API_BASE}/addresses?page=0&size=20`, concurrent: 20 },
      { name: 'Address Search', endpoint: `${API_BASE}/addresses?search=school&page=0&size=10`, concurrent: 15 }
    ];

    for (const test of testConfig) {
      console.log(`\n${test.name}: ${test.concurrent} concurrent requests...`);

      const promises: Promise<{ success: boolean; responseTime: number }>[] = [];
      const startTime = Date.now();

      for (let i = 0; i < test.concurrent; i++) {
        promises.push(
          (async () => {
            const reqStart = Date.now();
            try {
              await axios.get(test.endpoint, { timeout: 5000 });
              return { success: true, responseTime: Date.now() - reqStart };
            } catch (error) {
              return { success: false, responseTime: Date.now() - reqStart };
            }
          })()
        );
      }

      const allResults = await Promise.all(promises);
      const totalTime = Date.now() - startTime;

      const successCount = allResults.filter(r => r.success).length;
      const failCount = allResults.filter(r => !r.success).length;
      const responseTimes = allResults.map(r => r.responseTime);

      const avgResponseTime = responseTimes.reduce((a, b) => a + b, 0) / responseTimes.length;
      const minResponseTime = Math.min(...responseTimes);
      const maxResponseTime = Math.max(...responseTimes);
      const requestsPerSecond = (test.concurrent / (totalTime / 1000)).toFixed(1);

      console.log(`  ✓ Completed in ${totalTime}ms`);
      console.log(`  Success: ${successCount}/${test.concurrent}`);
      console.log(`  Avg Response: ${avgResponseTime.toFixed(0)}ms`);
      console.log(`  Min/Max: ${minResponseTime}ms / ${maxResponseTime}ms`);
      console.log(`  Throughput: ${requestsPerSecond} req/s`);

      results.push({
        endpoint: test.name,
        totalRequests: test.concurrent,
        successCount,
        failCount,
        avgResponseTime,
        minResponseTime,
        maxResponseTime,
        requestsPerSecond: parseFloat(requestsPerSecond)
      });
    }
  } catch (error: any) {
    console.error('Load test failed:', error);
  }

  return results;
}

/**
 * Print summary
 */
function printSummary(
  routingResults: RoutingTestResult[],
  nearestResults: AddressTestResult[],
  searchResults: AddressTestResult[],
  loadResults: LoadTestResult[]
) {
  console.log('\n' + '='.repeat(70));
  console.log('📊 COMPREHENSIVE TEST SUMMARY');
  console.log('='.repeat(70));

  // Routing Summary
  if (routingResults.length > 0) {
    const successCount = routingResults.filter(r => r.success).length;
    const avgResponseTime = routingResults.reduce((sum, r) => sum + r.responseTime, 0) / routingResults.length;

    console.log('\n🚗 ROUTING API:');
    console.log(`  Tests: ${routingResults.length}`);
    console.log(`  Success: ${successCount}/${routingResults.length} (${((successCount/routingResults.length)*100).toFixed(1)}%)`);
    console.log(`  Avg Response: ${avgResponseTime.toFixed(0)}ms`);
  }

  // Address Nearest Summary
  if (nearestResults.length > 0) {
    const successCount = nearestResults.filter(r => r.success).length;
    const avgResponseTime = nearestResults.reduce((sum, r) => sum + r.responseTime, 0) / nearestResults.length;
    const withResults = nearestResults.filter(r => r.resultCount > 0);
    const avgResults = withResults.length > 0
      ? withResults.reduce((sum, r) => sum + r.resultCount, 0) / withResults.length
      : 0;

    console.log('\n📍 ADDRESS NEAREST API:');
    console.log(`  Tests: ${nearestResults.length}`);
    console.log(`  Success: ${successCount}/${nearestResults.length} (${((successCount/nearestResults.length)*100).toFixed(1)}%)`);
    console.log(`  Avg Response: ${avgResponseTime.toFixed(0)}ms`);
    console.log(`  Avg Results: ${avgResults.toFixed(1)} addresses per query`);
  }

  // Address Search Summary
  if (searchResults.length > 0) {
    const successCount = searchResults.filter(r => r.success).length;
    const avgResponseTime = searchResults.reduce((sum, r) => sum + r.responseTime, 0) / searchResults.length;

    console.log('\n🔍 ADDRESS SEARCH API:');
    console.log(`  Tests: ${searchResults.length}`);
    console.log(`  Success: ${successCount}/${searchResults.length} (${((successCount/searchResults.length)*100).toFixed(1)}%)`);
    console.log(`  Avg Response: ${avgResponseTime.toFixed(0)}ms`);
  }

  // Load Test Summary
  if (loadResults.length > 0) {
    console.log('\n⚡ CONCURRENT LOAD TEST:');
    for (const result of loadResults) {
      console.log(`  ${result.endpoint}:`);
      console.log(`    Success Rate: ${((result.successCount/result.totalRequests)*100).toFixed(1)}%`);
      console.log(`    Throughput: ${result.requestsPerSecond} req/s`);
      console.log(`    Avg Response: ${result.avgResponseTime.toFixed(0)}ms`);
    }
  }

  // Overall Grade
  console.log('\n' + '='.repeat(70));
  console.log('🎯 OVERALL GRADE');
  console.log('='.repeat(70));

  const allTests = [...routingResults, ...nearestResults, ...searchResults];
  const totalSuccess = allTests.filter(r => r.success).length;
  const totalTests = allTests.length;
  const successRate = (totalSuccess / totalTests) * 100;

  let grade = '';
  let emoji = '';

  if (successRate >= 95) {
    grade = 'EXCELLENT';
    emoji = '🏆';
  } else if (successRate >= 80) {
    grade = 'GOOD';
    emoji = '✅';
  } else if (successRate >= 60) {
    grade = 'ACCEPTABLE';
    emoji = '⚠️';
  } else {
    grade = 'NEEDS IMPROVEMENT';
    emoji = '❌';
  }

  console.log(`\n${emoji} Grade: ${grade}`);
  console.log(`Overall Success Rate: ${successRate.toFixed(1)}% (${totalSuccess}/${totalTests})`);
  console.log();
}

/**
 * Main test runner
 */
async function runStressTests() {
  console.log('🔥 COMPREHENSIVE SERVICE STRESS TEST');
  console.log(`API Base: ${API_BASE}`);
  console.log(`Started: ${new Date().toLocaleString()}`);

  const startTime = Date.now();

  try {
    // Run all tests
    const routingResults = await testRoutingAPI();
    const nearestResults = await testAddressNearestAPI();
    const searchResults = await testAddressSearchAPI();
    const loadResults = await testConcurrentLoad();

    // Print summary
    printSummary(routingResults, nearestResults, searchResults, loadResults);

    const duration = ((Date.now() - startTime) / 1000).toFixed(1);
    console.log(`\n⏱️  Total Duration: ${duration}s`);
    console.log('='.repeat(70));

  } catch (error) {
    console.error('\n❌ Stress test failed:', error);
    throw error;
  } finally {
    await prisma.$disconnect();
  }
}

// Run if executed directly
if (import.meta.url === `file://${process.argv[1]}`) {
  runStressTests()
    .then(() => {
      process.exit(0);
    })
    .catch((error) => {
      console.error('Failed:', error);
      process.exit(1);
    });
}

export { runStressTests };
