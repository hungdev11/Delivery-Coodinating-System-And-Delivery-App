/**
 * Main seeder entry point
 * Runs all seeders in sequence
 */

import { seedZones } from './processors/zones-seeder.js';
import { seedRoads } from './processors/roads-seeder.js';

async function main() {
  console.log('='.repeat(60));
  console.log('Zone Service Database Seeding');
  console.log('='.repeat(60));
  console.log();

  try {
    // Step 1: Seed zones
    console.log('📍 Phase 1: Seeding zones and districts...\n');
    await seedZones();

    console.log('\n' + '-'.repeat(60) + '\n');

    // Step 2: Seed roads
    console.log('🛣️  Phase 2: Seeding roads and segments...\n');
    await seedRoads();

    console.log('\n' + '='.repeat(60));
    console.log('✓ All seeding completed successfully!');
    console.log('='.repeat(60));
  } catch (error) {
    console.error('\n❌ Seeding failed:', error);
    process.exit(1);
  }
}

main();
