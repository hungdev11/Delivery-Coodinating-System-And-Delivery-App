/**
 * Main seeder entry point
 * Runs all seeders in sequence
 */

import { seedZones } from './processors/zones-seeder.js';
import { seedRoads } from './processors/roads-seeder.js';
import { seedAddresses } from './processors/addresses-seeder.js';

async function main() {
  console.log('='.repeat(60));
  console.log('Zone Service Database Seeding');
  console.log('='.repeat(60));
  console.log();

  const startTime = Date.now();

  try {
    // Step 1: Seed zones
    console.log('📍 Phase 1: Seeding zones and districts...\n');
    await seedZones();

    console.log('\n' + '-'.repeat(60) + '\n');

    // Step 2: Seed roads
    console.log('🛣️  Phase 2: Seeding roads and segments...\n');
    await seedRoads();

    console.log('\n' + '-'.repeat(60) + '\n');

    // Step 3: Seed addresses
    console.log('🏢 Phase 3: Seeding addresses and POIs...\n');
    await seedAddresses();

    const duration = ((Date.now() - startTime) / 1000).toFixed(1);

    console.log('\n' + '='.repeat(60));
    console.log('✅ All seeding completed successfully!');
    console.log('='.repeat(60));
    console.log(`\nTotal time: ${duration}s`);
    console.log('\nNext steps:');
    console.log('  1. Start service: npm run dev');
    console.log('  2. Test routing: npm run test:routing');
    console.log('  3. Run stress test: npm run test:stress');
    console.log('  4. Query addresses: curl "http://localhost:21503/api/v1/addresses?page=0&size=20"');
    console.log('='.repeat(60));
  } catch (error) {
    console.error('\n❌ Seeding failed:', error);
    process.exit(1);
  }
}

main();
