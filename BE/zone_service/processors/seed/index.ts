/**
 * Seed All Data
 * 
 * Runs all seeders in the correct order (REAL data only):
 * 1. Zones (foundational data)
 * 2. Roads (foundational data)
 * 3. Addresses from Overpass API (real addresses from OpenStreetMap)
 */

import { spawn } from 'child_process';
import { join } from 'path';  

async function runSeeder(scriptPath: string, name: string): Promise<void> {
  return new Promise((resolve, reject) => {
    console.log(`\n${'='.repeat(70)}`);
    console.log(`🌱 Running ${name}...`);
    console.log('='.repeat(70));

    const child = spawn('tsx', [scriptPath], {
      stdio: 'inherit',
      shell: true,
      cwd: join(__dirname, '../..'),
    });

    child.on('close', (code) => {
      if (code === 0) {
        console.log(`✅ ${name} completed successfully\n`);
        resolve();
      } else {
        console.error(`❌ ${name} failed with code ${code}\n`);
        reject(new Error(`${name} failed`));
      }
    });

    child.on('error', (error) => {
      console.error(`❌ Error running ${name}:`, error);
      reject(error);
    });
  });
}

async function main() {
  console.log('\n' + '='.repeat(70));
  console.log('🚀 Starting Complete Data Seeding Process (REAL DATA ONLY)');
  console.log('='.repeat(70));

  try {
    // Step 1: Seed zones (foundational)
    await runSeeder(
      join(__dirname, 'zones-seeder.ts'),
      'Zones Seeder'
    );

    // Step 2: Seed roads (foundational)
    await runSeeder(
      join(__dirname, 'roads-seeder.ts'),
      'Roads Seeder'
    );

    console.log('\n' + '='.repeat(70));
    console.log('🎉 All seeders completed successfully!');
    console.log('='.repeat(70));
    console.log('\n📋 Seeded Data:');
    console.log('  ✅ Zones (real administrative boundaries)');
    console.log('  ✅ Roads (real road network from OSM)');
    console.log('\n💡 Next Steps:');
    console.log('  • Run "npm run osrm:generate" to generate OSRM routing data');
    console.log('  • Start OSRM containers: docker-compose up osrm-*');
    console.log('='.repeat(70) + '\n');

    process.exit(0);
  } catch (error) {
    console.error('\n❌ Seeding process failed:', error);
    process.exit(1);
  }
}

main(); 
