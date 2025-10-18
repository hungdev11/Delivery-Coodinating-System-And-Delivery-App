/**
 * Comprehensive Test Suite
 *
 * Runs all tests from basic to stress with detailed statistics:
 * 1. Basic API Health Check
 * 2. Simple Address Queries
 * 3. Zone Queries
 * 4. Routing Tests
 * 5. Address Search Tests
 * 6. Nearest Address Tests
 * 7. Concurrent Load Tests
 * 8. Stress Tests
 */

import axios, { AxiosError } from 'axios';

const API_BASE = process.env.API_BASE || 'http://localhost:21503/api/v1';

interface TestResult {
  name: string;
  success: boolean;
  duration: number;
  error?: string;
  details?: any;
}

interface TestStats {
  total: number;
  passed: number;
  failed: number;
  avgDuration: number;
  minDuration: number;
  maxDuration: number;
}

class ComprehensiveTestRunner {
  private results: TestResult[] = [];
  private startTime: number = 0;

  async run() {
    console.log('='.repeat(70));
    console.log('🧪 COMPREHENSIVE TEST SUITE - ALL TESTS');
    console.log('='.repeat(70));
    console.log(`API Base: ${API_BASE}`);
    console.log(`Started: ${new Date().toLocaleString()}`);
    console.log('='.repeat(70));
    console.log();

    this.startTime = Date.now();

    // Phase 1: Basic Tests
    await this.runPhase('Phase 1: Basic API Health Checks', [
      () => this.testBasicHealth(),
      () => this.testAddressesEndpoint(),
      () => this.testZonesEndpoint(),
      () => this.testRoutingEndpoint()
    ]);

    // Phase 2: Simple Queries
    await this.runPhase('Phase 2: Simple Data Queries', [
      () => this.testListAddresses(),
      () => this.testListZones(),
      () => this.testGetAddressById(),
      () => this.testSearchAddresses()
    ]);

    // Phase 3: Geospatial Queries
    await this.runPhase('Phase 3: Geospatial & Nearest Queries', [
      () => this.testNearestAddressBasic(),
      () => this.testNearestAddressWithFilters(),
      () => this.testNearestAddressLargeRadius(),
      () => this.testNearestMultiplePoints()
    ]);

    // Phase 4: Routing Tests
    await this.runPhase('Phase 4: Routing & Navigation', [
      () => this.testSimpleRoute(),
      () => this.testMultipleRoutes(),
      () => this.testLongDistanceRoute()
    ]);

    // Phase 5: Search Tests
    await this.runPhase('Phase 5: Search & Filtering', [
      () => this.testSearchByType(),
      () => this.testSearchByName(),
      () => this.testSearchPagination()
    ]);

    // Phase 6: Load Tests
    await this.runPhase('Phase 6: Concurrent Load Tests', [
      () => this.testConcurrentAddressQueries(10),
      () => this.testConcurrentNearestQueries(15),
      () => this.testConcurrentRouting(10)
    ]);

    // Phase 7: Stress Tests
    await this.runPhase('Phase 7: High Load Stress Tests', [
      () => this.testHighConcurrentLoad(30),
      () => this.testMixedLoadScenario(50)
    ]);

    // Print comprehensive results
    this.printComprehensiveResults();
  }

  private async runPhase(phaseName: string, tests: (() => Promise<void>)[]) {
    console.log('\n' + '='.repeat(70));
    console.log(`📋 ${phaseName}`);
    console.log('='.repeat(70));
    console.log();

    for (const test of tests) {
      await test();
      // Small delay between tests
      await this.sleep(100);
    }
  }

  private async testBasicHealth() {
    const start = Date.now();
    try {
      const response = await axios.get(`${API_BASE}/health`).catch(() =>
        axios.get(`http://localhost:21503/health`)
      );

      this.recordResult({
        name: 'Basic Health Check',
        success: response.status === 200 || response.status === 404, // 404 is OK if route doesn't exist
        duration: Date.now() - start,
        details: { status: response.status }
      });
    } catch (error) {
      this.recordResult({
        name: 'Basic Health Check',
        success: false,
        duration: Date.now() - start,
        error: this.getErrorMessage(error)
      });
    }
  }

  private async testAddressesEndpoint() {
    const start = Date.now();
    try {
      const response = await axios.get(`${API_BASE}/addresses?page=0&size=1`);

      this.recordResult({
        name: 'Addresses Endpoint Available',
        success: response.status === 200,
        duration: Date.now() - start,
        details: { hasData: response.data?.result?.data?.length > 0 }
      });
    } catch (error) {
      this.recordResult({
        name: 'Addresses Endpoint Available',
        success: false,
        duration: Date.now() - start,
        error: this.getErrorMessage(error)
      });
    }
  }

  private async testZonesEndpoint() {
    const start = Date.now();
    try {
      const response = await axios.get(`${API_BASE}/zones?page=0&size=1`);

      this.recordResult({
        name: 'Zones Endpoint Available',
        success: response.status === 200,
        duration: Date.now() - start,
        details: { hasData: response.data?.result?.data?.length > 0 }
      });
    } catch (error) {
      this.recordResult({
        name: 'Zones Endpoint Available',
        success: false,
        duration: Date.now() - start,
        error: this.getErrorMessage(error)
      });
    }
  }

  private async testRoutingEndpoint() {
    const start = Date.now();
    try {
      const response = await axios.post(`${API_BASE}/routing/route`, {
        waypoints: [
          { lat: 10.8505, lon: 106.7717 },
          { lat: 10.8520, lon: 106.7730 }
        ]
      });

      this.recordResult({
        name: 'Routing Endpoint Available',
        success: response.status === 200,
        duration: Date.now() - start,
        details: { hasRoute: !!response.data?.result }
      });
    } catch (error) {
      this.recordResult({
        name: 'Routing Endpoint Available',
        success: false,
        duration: Date.now() - start,
        error: this.getErrorMessage(error)
      });
    }
  }

  private async testListAddresses() {
    const start = Date.now();
    try {
      const response = await axios.get(`${API_BASE}/addresses?page=0&size=20`);
      const count = response.data?.result?.data?.length || 0;

      this.recordResult({
        name: 'List Addresses (20 items)',
        success: response.status === 200,
        duration: Date.now() - start,
        details: { count, totalElements: response.data?.result?.page?.totalElements }
      });
    } catch (error) {
      this.recordResult({
        name: 'List Addresses (20 items)',
        success: false,
        duration: Date.now() - start,
        error: this.getErrorMessage(error)
      });
    }
  }

  private async testListZones() {
    const start = Date.now();
    try {
      const response = await axios.get(`${API_BASE}/zones?page=0&size=20`);
      const count = response.data?.result?.data?.length || 0;

      this.recordResult({
        name: 'List Zones (20 items)',
        success: response.status === 200,
        duration: Date.now() - start,
        details: { count }
      });
    } catch (error) {
      this.recordResult({
        name: 'List Zones (20 items)',
        success: false,
        duration: Date.now() - start,
        error: this.getErrorMessage(error)
      });
    }
  }

  private async testGetAddressById() {
    const start = Date.now();
    try {
      // First get a list to find an ID
      const listResponse = await axios.get(`${API_BASE}/addresses?page=0&size=1`);
      const firstAddress = listResponse.data?.result?.data?.[0];

      if (!firstAddress) {
        this.recordResult({
          name: 'Get Address By ID',
          success: false,
          duration: Date.now() - start,
          error: 'No addresses found to test'
        });
        return;
      }

      const response = await axios.get(`${API_BASE}/addresses/${firstAddress.id}`);

      this.recordResult({
        name: 'Get Address By ID',
        success: response.status === 200 && response.data?.result?.id === firstAddress.id,
        duration: Date.now() - start,
        details: { id: firstAddress.id }
      });
    } catch (error) {
      this.recordResult({
        name: 'Get Address By ID',
        success: false,
        duration: Date.now() - start,
        error: this.getErrorMessage(error)
      });
    }
  }

  private async testSearchAddresses() {
    const start = Date.now();
    try {
      const response = await axios.get(`${API_BASE}/addresses?search=landmark&page=0&size=10`);
      const count = response.data?.result?.data?.length || 0;

      this.recordResult({
        name: 'Search Addresses (keyword)',
        success: response.status === 200,
        duration: Date.now() - start,
        details: { count, keyword: 'landmark' }
      });
    } catch (error) {
      this.recordResult({
        name: 'Search Addresses (keyword)',
        success: false,
        duration: Date.now() - start,
        error: this.getErrorMessage(error)
      });
    }
  }

  private async testNearestAddressBasic() {
    const start = Date.now();
    try {
      const response = await axios.get(
        `${API_BASE}/addresses/nearest?lat=10.8505&lon=106.7717&limit=10&maxDistance=5000`
      );
      const count = response.data?.result?.length || 0;

      this.recordResult({
        name: 'Nearest Address (5km radius)',
        success: response.status === 200,
        duration: Date.now() - start,
        details: { count, radius: '5km' }
      });
    } catch (error) {
      this.recordResult({
        name: 'Nearest Address (5km radius)',
        success: false,
        duration: Date.now() - start,
        error: this.getErrorMessage(error)
      });
    }
  }

  private async testNearestAddressWithFilters() {
    const start = Date.now();
    try {
      const response = await axios.get(
        `${API_BASE}/addresses/nearest?lat=10.8505&lon=106.7717&limit=10&maxDistance=10000&addressType=LANDMARK`
      );
      const count = response.data?.result?.length || 0;

      this.recordResult({
        name: 'Nearest Address (with filter)',
        success: response.status === 200,
        duration: Date.now() - start,
        details: { count, filter: 'LANDMARK' }
      });
    } catch (error) {
      this.recordResult({
        name: 'Nearest Address (with filter)',
        success: false,
        duration: Date.now() - start,
        error: this.getErrorMessage(error)
      });
    }
  }

  private async testNearestAddressLargeRadius() {
    const start = Date.now();
    try {
      const response = await axios.get(
        `${API_BASE}/addresses/nearest?lat=10.8505&lon=106.7717&limit=50&maxDistance=50000`
      );
      const count = response.data?.result?.length || 0;

      this.recordResult({
        name: 'Nearest Address (50km radius)',
        success: response.status === 200,
        duration: Date.now() - start,
        details: { count, radius: '50km' }
      });
    } catch (error) {
      this.recordResult({
        name: 'Nearest Address (50km radius)',
        success: false,
        duration: Date.now() - start,
        error: this.getErrorMessage(error)
      });
    }
  }

  private async testNearestMultiplePoints() {
    const start = Date.now();
    const points = [
      { lat: 10.8505, lon: 106.7717 },
      { lat: 10.8520, lon: 106.7730 },
      { lat: 10.8490, lon: 106.7700 }
    ];

    try {
      const promises = points.map(p =>
        axios.get(`${API_BASE}/addresses/nearest?lat=${p.lat}&lon=${p.lon}&limit=5&maxDistance=2000`)
      );

      const responses = await Promise.all(promises);
      const totalCount = responses.reduce((sum, r) => sum + (r.data?.result?.length || 0), 0);

      this.recordResult({
        name: 'Nearest Address (3 points)',
        success: responses.every(r => r.status === 200),
        duration: Date.now() - start,
        details: { points: points.length, totalFound: totalCount }
      });
    } catch (error) {
      this.recordResult({
        name: 'Nearest Address (3 points)',
        success: false,
        duration: Date.now() - start,
        error: this.getErrorMessage(error)
      });
    }
  }

  private async testSimpleRoute() {
    const start = Date.now();
    try {
      const response = await axios.post(`${API_BASE}/routing/route`, {
        waypoints: [
          { lat: 10.8505, lon: 106.7717 },
          { lat: 10.8520, lon: 106.7730 }
        ]
      });

      const route = response.data?.result;

      this.recordResult({
        name: 'Simple Route (short distance)',
        success: response.status === 200 && !!route,
        duration: Date.now() - start,
        details: {
          distance: route?.distance,
          duration: route?.duration
        }
      });
    } catch (error) {
      this.recordResult({
        name: 'Simple Route (short distance)',
        success: false,
        duration: Date.now() - start,
        error: this.getErrorMessage(error)
      });
    }
  }

  private async testMultipleRoutes() {
    const start = Date.now();
    const routes = [
      {
        waypoints: [
          { lat: 10.8505, lon: 106.7717 },
          { lat: 10.8520, lon: 106.7730 }
        ]
      },
      {
        waypoints: [
          { lat: 10.8520, lon: 106.7730 },
          { lat: 10.8550, lon: 106.7760 }
        ]
      },
      {
        waypoints: [
          { lat: 10.8490, lon: 106.7700 },
          { lat: 10.8510, lon: 106.7720 }
        ]
      }
    ];

    try {
      const promises = routes.map(r => axios.post(`${API_BASE}/routing/route`, r));
      const responses = await Promise.all(promises);
      const successCount = responses.filter(r => r.status === 200 && r.data?.result).length;

      this.recordResult({
        name: 'Multiple Routes (3 routes)',
        success: successCount === routes.length,
        duration: Date.now() - start,
        details: { total: routes.length, success: successCount }
      });
    } catch (error) {
      this.recordResult({
        name: 'Multiple Routes (3 routes)',
        success: false,
        duration: Date.now() - start,
        error: this.getErrorMessage(error)
      });
    }
  }

  private async testLongDistanceRoute() {
    const start = Date.now();
    try {
      const response = await axios.post(`${API_BASE}/routing/route`, {
        waypoints: [
          { lat: 10.8505, lon: 106.7717 },
          { lat: 10.8800, lon: 106.8000 }
        ]
      });

      const route = response.data?.result;

      this.recordResult({
        name: 'Long Distance Route',
        success: response.status === 200 && !!route,
        duration: Date.now() - start,
        details: {
          distance: route?.distance,
          duration: route?.duration
        }
      });
    } catch (error) {
      this.recordResult({
        name: 'Long Distance Route',
        success: false,
        duration: Date.now() - start,
        error: this.getErrorMessage(error)
      });
    }
  }

  private async testSearchByType() {
    const start = Date.now();
    try {
      const response = await axios.get(
        `${API_BASE}/addresses?addressType=LANDMARK&page=0&size=20`
      );
      const count = response.data?.result?.data?.length || 0;

      this.recordResult({
        name: 'Search By Type (LANDMARK)',
        success: response.status === 200,
        duration: Date.now() - start,
        details: { count, type: 'LANDMARK' }
      });
    } catch (error) {
      this.recordResult({
        name: 'Search By Type (LANDMARK)',
        success: false,
        duration: Date.now() - start,
        error: this.getErrorMessage(error)
      });
    }
  }

  private async testSearchByName() {
    const start = Date.now();
    try {
      const response = await axios.get(
        `${API_BASE}/addresses?search=vòng&page=0&size=10`
      );
      const count = response.data?.result?.data?.length || 0;

      this.recordResult({
        name: 'Search By Name',
        success: response.status === 200,
        duration: Date.now() - start,
        details: { count, search: 'vòng' }
      });
    } catch (error) {
      this.recordResult({
        name: 'Search By Name',
        success: false,
        duration: Date.now() - start,
        error: this.getErrorMessage(error)
      });
    }
  }

  private async testSearchPagination() {
    const start = Date.now();
    try {
      const [page1, page2] = await Promise.all([
        axios.get(`${API_BASE}/addresses?page=0&size=5`),
        axios.get(`${API_BASE}/addresses?page=1&size=5`)
      ]);

      const count1 = page1.data?.result?.data?.length || 0;
      const count2 = page2.data?.result?.data?.length || 0;

      this.recordResult({
        name: 'Search with Pagination',
        success: page1.status === 200 && page2.status === 200,
        duration: Date.now() - start,
        details: { page1Count: count1, page2Count: count2 }
      });
    } catch (error) {
      this.recordResult({
        name: 'Search with Pagination',
        success: false,
        duration: Date.now() - start,
        error: this.getErrorMessage(error)
      });
    }
  }

  private async testConcurrentAddressQueries(count: number) {
    const start = Date.now();
    try {
      const promises = Array.from({ length: count }, () =>
        axios.get(`${API_BASE}/addresses?page=0&size=10`)
      );

      const responses = await Promise.all(promises);
      const successCount = responses.filter(r => r.status === 200).length;
      const durations = responses.map((_, i) => Date.now() - start);
      const avgDuration = durations.reduce((sum, d) => sum + d, 0) / durations.length;

      this.recordResult({
        name: `Concurrent Address Queries (${count}x)`,
        success: successCount === count,
        duration: Date.now() - start,
        details: {
          total: count,
          success: successCount,
          avgResponseTime: Math.round(avgDuration / count)
        }
      });
    } catch (error) {
      this.recordResult({
        name: `Concurrent Address Queries (${count}x)`,
        success: false,
        duration: Date.now() - start,
        error: this.getErrorMessage(error)
      });
    }
  }

  private async testConcurrentNearestQueries(count: number) {
    const start = Date.now();
    try {
      const promises = Array.from({ length: count }, (_, i) => {
        const lat = 10.8505 + (Math.random() - 0.5) * 0.01;
        const lon = 106.7717 + (Math.random() - 0.5) * 0.01;
        return axios.get(`${API_BASE}/addresses/nearest?lat=${lat}&lon=${lon}&limit=10&maxDistance=5000`);
      });

      const responses = await Promise.all(promises);
      const successCount = responses.filter(r => r.status === 200).length;

      this.recordResult({
        name: `Concurrent Nearest Queries (${count}x)`,
        success: successCount === count,
        duration: Date.now() - start,
        details: {
          total: count,
          success: successCount,
          avgResponseTime: Math.round((Date.now() - start) / count)
        }
      });
    } catch (error) {
      this.recordResult({
        name: `Concurrent Nearest Queries (${count}x)`,
        success: false,
        duration: Date.now() - start,
        error: this.getErrorMessage(error)
      });
    }
  }

  private async testConcurrentRouting(count: number) {
    const start = Date.now();
    try {
      const promises = Array.from({ length: count }, () =>
        axios.post(`${API_BASE}/routing/route`, {
          waypoints: [
            { lat: 10.8505 + Math.random() * 0.01, lon: 106.7717 + Math.random() * 0.01 },
            { lat: 10.8520 + Math.random() * 0.01, lon: 106.7730 + Math.random() * 0.01 }
          ]
        })
      );

      const responses = await Promise.all(promises);
      const successCount = responses.filter(r => r.status === 200).length;

      this.recordResult({
        name: `Concurrent Routing (${count}x)`,
        success: successCount === count,
        duration: Date.now() - start,
        details: {
          total: count,
          success: successCount,
          avgResponseTime: Math.round((Date.now() - start) / count)
        }
      });
    } catch (error) {
      this.recordResult({
        name: `Concurrent Routing (${count}x)`,
        success: false,
        duration: Date.now() - start,
        error: this.getErrorMessage(error)
      });
    }
  }

  private async testHighConcurrentLoad(count: number) {
    const start = Date.now();
    try {
      const promises = Array.from({ length: count }, (_, i) => {
        if (i % 2 === 0) {
          return axios.get(`${API_BASE}/addresses?page=0&size=10`);
        } else {
          const lat = 10.8505 + (Math.random() - 0.5) * 0.01;
          const lon = 106.7717 + (Math.random() - 0.5) * 0.01;
          return axios.get(`${API_BASE}/addresses/nearest?lat=${lat}&lon=${lon}&limit=10&maxDistance=5000`);
        }
      });

      const responses = await Promise.all(promises);
      const successCount = responses.filter(r => r.status === 200).length;

      this.recordResult({
        name: `High Concurrent Load (${count}x mixed)`,
        success: successCount >= count * 0.9, // Allow 10% failure
        duration: Date.now() - start,
        details: {
          total: count,
          success: successCount,
          failed: count - successCount,
          successRate: `${((successCount / count) * 100).toFixed(1)}%`,
          avgResponseTime: Math.round((Date.now() - start) / count)
        }
      });
    } catch (error) {
      this.recordResult({
        name: `High Concurrent Load (${count}x mixed)`,
        success: false,
        duration: Date.now() - start,
        error: this.getErrorMessage(error)
      });
    }
  }

  private async testMixedLoadScenario(count: number) {
    const start = Date.now();
    try {
      const promises = Array.from({ length: count }, (_, i) => {
        const type = i % 4;
        if (type === 0) {
          return axios.get(`${API_BASE}/addresses?page=${i % 3}&size=10`);
        } else if (type === 1) {
          const lat = 10.8505 + (Math.random() - 0.5) * 0.02;
          const lon = 106.7717 + (Math.random() - 0.5) * 0.02;
          return axios.get(`${API_BASE}/addresses/nearest?lat=${lat}&lon=${lon}&limit=10&maxDistance=5000`);
        } else if (type === 2) {
          return axios.get(`${API_BASE}/zones?page=0&size=10`);
        } else {
          return axios.post(`${API_BASE}/routing/route`, {
            waypoints: [
              { lat: 10.8505 + Math.random() * 0.01, lon: 106.7717 + Math.random() * 0.01 },
              { lat: 10.8520 + Math.random() * 0.01, lon: 106.7730 + Math.random() * 0.01 }
            ]
          });
        }
      });

      const responses = await Promise.all(promises);
      const successCount = responses.filter(r => r.status === 200).length;

      this.recordResult({
        name: `Mixed Load Scenario (${count}x all types)`,
        success: successCount >= count * 0.85, // Allow 15% failure
        duration: Date.now() - start,
        details: {
          total: count,
          success: successCount,
          failed: count - successCount,
          successRate: `${((successCount / count) * 100).toFixed(1)}%`,
          avgResponseTime: Math.round((Date.now() - start) / count),
          throughput: `${((count / (Date.now() - start)) * 1000).toFixed(1)} req/s`
        }
      });
    } catch (error) {
      this.recordResult({
        name: `Mixed Load Scenario (${count}x all types)`,
        success: false,
        duration: Date.now() - start,
        error: this.getErrorMessage(error)
      });
    }
  }

  private recordResult(result: TestResult) {
    this.results.push(result);

    const status = result.success ? '✅ PASS' : '❌ FAIL';
    const duration = `${result.duration}ms`;

    console.log(`${status} | ${result.name.padEnd(40)} | ${duration.padStart(8)}`);

    if (result.details) {
      console.log(`       └─ ${JSON.stringify(result.details)}`);
    }

    if (result.error) {
      console.log(`       └─ Error: ${result.error}`);
    }
  }

  private printComprehensiveResults() {
    const totalDuration = Date.now() - this.startTime;

    console.log('\n' + '='.repeat(70));
    console.log('📊 COMPREHENSIVE TEST RESULTS');
    console.log('='.repeat(70));
    console.log();

    const phaseGroups = this.groupResultsByPhase();

    for (const [phase, results] of Object.entries(phaseGroups)) {
      const stats = this.calculateStats(results);

      console.log(`\n${phase}:`);
      console.log(`  Total Tests: ${stats.total}`);
      console.log(`  Passed: ${stats.passed} (${((stats.passed / stats.total) * 100).toFixed(1)}%)`);
      console.log(`  Failed: ${stats.failed} (${((stats.failed / stats.total) * 100).toFixed(1)}%)`);
      console.log(`  Avg Duration: ${stats.avgDuration.toFixed(1)}ms`);
      console.log(`  Min/Max: ${stats.minDuration}ms / ${stats.maxDuration}ms`);
    }

    const overallStats = this.calculateStats(this.results);

    console.log('\n' + '-'.repeat(70));
    console.log('📈 OVERALL STATISTICS:');
    console.log('-'.repeat(70));
    console.log(`  Total Tests Run: ${overallStats.total}`);
    console.log(`  Passed: ${overallStats.passed} ✅`);
    console.log(`  Failed: ${overallStats.failed} ❌`);
    console.log(`  Success Rate: ${((overallStats.passed / overallStats.total) * 100).toFixed(2)}%`);
    console.log(`  Average Duration: ${overallStats.avgDuration.toFixed(1)}ms`);
    console.log(`  Min/Max Duration: ${overallStats.minDuration}ms / ${overallStats.maxDuration}ms`);
    console.log(`  Total Test Time: ${(totalDuration / 1000).toFixed(1)}s`);
    console.log();

    // Grade
    const grade = this.calculateGrade(overallStats.passed / overallStats.total);
    console.log('='.repeat(70));
    console.log(`🎯 FINAL GRADE: ${grade.emoji} ${grade.name}`);
    console.log('='.repeat(70));
    console.log(`Overall Success Rate: ${((overallStats.passed / overallStats.total) * 100).toFixed(1)}% (${overallStats.passed}/${overallStats.total})`);
    console.log();
    console.log(`⏱️  Total Duration: ${(totalDuration / 1000).toFixed(1)}s`);
    console.log(`🏁 Completed: ${new Date().toLocaleString()}`);
    console.log('='.repeat(70));
  }

  private groupResultsByPhase(): Record<string, TestResult[]> {
    const groups: Record<string, TestResult[]> = {
      'Phase 1: Basic Health': [],
      'Phase 2: Simple Queries': [],
      'Phase 3: Geospatial': [],
      'Phase 4: Routing': [],
      'Phase 5: Search': [],
      'Phase 6: Load Tests': [],
      'Phase 7: Stress Tests': []
    };

    for (const result of this.results) {
      if (result.name.includes('Health') || result.name.includes('Endpoint Available')) {
        groups['Phase 1: Basic Health'].push(result);
      } else if (result.name.includes('List') || result.name.includes('By ID')) {
        groups['Phase 2: Simple Queries'].push(result);
      } else if (result.name.includes('Nearest')) {
        groups['Phase 3: Geospatial'].push(result);
      } else if (result.name.includes('Route')) {
        groups['Phase 4: Routing'].push(result);
      } else if (result.name.includes('Search') || result.name.includes('Type') || result.name.includes('Pagination')) {
        groups['Phase 5: Search'].push(result);
      } else if (result.name.includes('Concurrent') && !result.name.includes('High')) {
        groups['Phase 6: Load Tests'].push(result);
      } else {
        groups['Phase 7: Stress Tests'].push(result);
      }
    }

    return groups;
  }

  private calculateStats(results: TestResult[]): TestStats {
    if (results.length === 0) {
      return { total: 0, passed: 0, failed: 0, avgDuration: 0, minDuration: 0, maxDuration: 0 };
    }

    const passed = results.filter(r => r.success).length;
    const durations = results.map(r => r.duration);

    return {
      total: results.length,
      passed,
      failed: results.length - passed,
      avgDuration: durations.reduce((sum, d) => sum + d, 0) / durations.length,
      minDuration: Math.min(...durations),
      maxDuration: Math.max(...durations)
    };
  }

  private calculateGrade(successRate: number): { emoji: string; name: string } {
    if (successRate >= 0.95) return { emoji: '🏆', name: 'EXCELLENT' };
    if (successRate >= 0.85) return { emoji: '🥇', name: 'VERY GOOD' };
    if (successRate >= 0.75) return { emoji: '🥈', name: 'GOOD' };
    if (successRate >= 0.60) return { emoji: '🥉', name: 'FAIR' };
    return { emoji: '⚠️', name: 'NEEDS IMPROVEMENT' };
  }

  private getErrorMessage(error: any): string {
    if (axios.isAxiosError(error)) {
      return error.response?.data?.message || error.message;
    }
    return String(error);
  }

  private sleep(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }
}

// Run tests
const runner = new ComprehensiveTestRunner();
runner.run().catch(console.error);
