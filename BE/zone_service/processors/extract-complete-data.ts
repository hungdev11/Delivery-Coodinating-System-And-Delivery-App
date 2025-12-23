/**
 * Complete OSM Data Extract - Routing + Addresses
 * 
 * Two-stage strategy:
 * 1. Extract polygon with complete ways (routing integrity)
 * 2. Extract all address nodes + clip to polygon
 * 3. Merge both extracts
 * 
 * This gives you:
 * - Clean routing graph for OSRM
 * - Complete address coverage for geocoding
 * - No orphaned addresses or broken ways
 * 
 * Usage:
 *   npm run extract:complete
 */

import { OsmiumWrapper } from '../utils/osmium-wrapper';
import { findLatestVietnamPBF } from '../utils/osm-parser';
import { join } from 'path';
import { existsSync } from 'fs';

async function extractCompleteData() {
  console.log('🗺️  Starting Complete OSM Data Extract...\n');
  const startTime = Date.now();

  try {
    // Setup paths
    const rawDataDir = join(process.cwd(), './raw_data');
    const pbfPath = findLatestVietnamPBF(rawDataDir);
    const polyFile = join(rawDataDir, 'poly/hcmc.poly');
    const outputPbf = join(rawDataDir, 'extracted/thuduc_complete.osm.pbf');

    if (!existsSync(polyFile)) {
      throw new Error(`Poly file not found: ${polyFile}`);
    }

    console.log(`Source PBF: ${pbfPath}`);
    console.log(`Polygon: ${polyFile}`);
    console.log(`Output: ${outputPbf}\n`);

    // Initialize osmium wrapper with verbose output
    const osmium = new OsmiumWrapper(true);

    // Check installation
    const isInstalled = await osmium.checkInstallation();
    if (!isInstalled) {
      throw new Error('osmium-tool is not installed');
    }

    console.log('\n' + '='.repeat(60));
    console.log('Strategy: Two-Stage Extract + Merge');
    console.log('='.repeat(60));
    console.log('This will:');
    console.log('  1. Extract routing graph with complete ways/nodes/relations');
    console.log('  2. Extract all address nodes from source Vietnam PBF');
    console.log('  3. Clip addresses to your polygon boundary');
    console.log('  4. Merge routing + addresses into final PBF');
    console.log('='.repeat(60) + '\n');

    // Run two-stage extract
    await osmium.extractRoutingWithAddresses(pbfPath, polyFile, outputPbf);

    // Get file info
    console.log('\n' + '='.repeat(60));
    console.log('File Info:');
    console.log('='.repeat(60));
    const fileInfo = await osmium.getFileInfo(outputPbf);
    console.log(JSON.stringify(fileInfo, null, 2));

    const duration = ((Date.now() - startTime) / 1000).toFixed(1);

    console.log('\n' + '='.repeat(60));
    console.log('✅ Complete Extract Finished!');
    console.log('='.repeat(60));
    console.log(`Time taken: ${duration}s`);
    console.log(`Output: ${outputPbf}\n`);

    console.log('Next steps:');
    console.log('  1. Seed roads: npm run seed:roads');
    console.log('  2. Seed addresses: npm run seed:addresses');
    console.log('  3. Generate OSRM data: npm run osrm:generate');
    console.log('='.repeat(60));

  } catch (error) {
    console.error('\n❌ Extract failed:', error);
    throw error;
  }
}

// Run if executed directly
if (import.meta.url === `file://${process.argv[1]}`) {
  extractCompleteData()
    .then(() => {
      process.exit(0);
    })
    .catch((error) => {
      console.error('Failed:', error);
      process.exit(1);
    });
}

export { extractCompleteData };
