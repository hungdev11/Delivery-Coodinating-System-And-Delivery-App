/**
 * Data extraction script for Thu Duc roads
 * Extracts road data from Ho Chi Minh City OSM PBF filtered by Thu Duc boundary
 *
 * This script uses osmium-tool to:
 * 1. Filter data by old Thu Duc city boundary
 * 2. Extract only roads
 * 3. Convert to GeoJSON for processing
 *
 * Run: npm run extract:thuduc
 */

import { join } from 'path';
import { existsSync } from 'fs';
import { OSMParser } from '../utils/osm-parser';

async function extractThuDucRoads() {
  console.log('='.repeat(60));
  console.log('Thu Duc Road Data Extraction');
  console.log('='.repeat(60));
  console.log();

  try {
    // Paths to data files
    const rawDataDir = join(process.cwd(), './raw_data');
    const hcmPbfPath = join(rawDataDir, 'new_hochiminh_city/hochiminh_city.osm.pbf');
    const thuDucPolyPath = join(rawDataDir, 'old_thuduc_city/thuduc_cu.poly');

    // Verify files exist
    if (!existsSync(hcmPbfPath)) {
      throw new Error(`Ho Chi Minh City PBF file not found: ${hcmPbfPath}`);
    }

    if (!existsSync(thuDucPolyPath)) {
      console.warn(`Thu Duc poly file not found: ${thuDucPolyPath}`);
      console.warn('Will extract all roads from Ho Chi Minh City without Thu Duc filter');
    }

    console.log('Data files:');
    console.log(`  PBF: ${hcmPbfPath}`);
    console.log(`  Poly: ${thuDucPolyPath}`);
    console.log();

    // Parse OSM data
    console.log('Starting OSM data extraction...');
    console.log('This may take several minutes depending on data size.');
    console.log();

    const parser = new OSMParser(true); // verbose = true

    const osmData = existsSync(thuDucPolyPath)
      ? await parser.parsePBF(hcmPbfPath, thuDucPolyPath)
      : await parser.parsePBF(hcmPbfPath);

    console.log();
    console.log('='.repeat(60));
    console.log('Extraction Complete!');
    console.log('='.repeat(60));
    console.log();
    console.log('Summary:');
    console.log(`  Total Nodes: ${osmData.nodes.size}`);
    console.log(`  Total Ways (Roads): ${osmData.ways.length}`);
    console.log();

    // Analyze road types
    const roadTypes = new Map<string, number>();
    const roadNames = new Map<string, number>();
    let unnamedRoads = 0;

    for (const way of osmData.ways) {
      const type = way.tags.highway || 'unknown';
      roadTypes.set(type, (roadTypes.get(type) || 0) + 1);

      const name = way.tags.name || way.tags['name:vi'];
      if (name) {
        roadNames.set(name, (roadNames.get(name) || 0) + 1);
      } else {
        unnamedRoads++;
      }
    }

    console.log('Road Types:');
    for (const [type, count] of Array.from(roadTypes.entries()).sort((a, b) => b[1] - a[1])) {
      console.log(`  ${type}: ${count}`);
    }
    console.log();

    console.log('Road Statistics:');
    console.log(`  Named roads: ${roadNames.size}`);
    console.log(`  Unnamed roads: ${unnamedRoads}`);

    // Find duplicate road names
    const duplicates = Array.from(roadNames.entries())
      .filter(([_, count]) => count > 1)
      .sort((a, b) => b[1] - a[1]);

    if (duplicates.length > 0) {
      console.log();
      console.log(`Found ${duplicates.length} duplicate road names:`);
      console.log('Top 10 duplicates:');
      for (const [name, count] of duplicates.slice(0, 10)) {
        console.log(`  "${name}": ${count} segments`);
      }
    }

    console.log();
    console.log('Next steps:');
    console.log('  1. Run: npm run seed:zones   - Seed district zones');
    console.log('  2. Run: npm run seed:roads   - Seed roads into database');
    console.log('  3. Check: npm run prisma:studio - View data in Prisma Studio');
    console.log();

  } catch (error: any) {
    console.error();
    console.error('❌ Extraction failed:');
    console.error(error.message);

    if (error.message.includes('osmium-tool')) {
      console.error();
      console.error('Please install osmium-tool:');
      console.error('  Ubuntu/Debian: sudo apt-get install osmium-tool');
      console.error('  macOS: brew install osmium-tool');
    }

    process.exit(1);
  }
}

// Run if executed directly
if (import.meta.url === `file://${process.argv[1]}`) {
  extractThuDucRoads()
    .then(() => {
      console.log('Done!');
      process.exit(0);
    })
    .catch((error) => {
      console.error('Failed:', error);
      process.exit(1);
    });
}

export { extractThuDucRoads };
