/**
 * Seeder for zones (wards/phường)
 * Processes multi-polygon .poly file for old Thu Duc City and its wards
 * Creates:
 * - 1 center: TP Thủ Đức (cũ) - represents the entire old Thu Duc City area
 * - N zones: Individual wards (phường) that belong to the Thu Duc center
 */

import { PrismaClient } from '@prisma/client';
import { join } from 'path';
import { 
  parseMultiPolyFile, 
  unionPolygons, 
  generateZoneCode,
  calculateCentroid
} from '../../utils/polygon-parser.js';

const prisma = new PrismaClient();

async function seedZones() {
  console.log('🌱 Starting zones seeding...\n');
  console.log('='.repeat(60));

  try {
    // Step 0: Debug database connection
    console.log('Step 0: Checking database connection...');
    const dbUrl = process.env.ZONE_DB_CONNECTION;
    console.log(`Database URL: ${dbUrl ? dbUrl + '...' : 'NOT SET'}`);
    console.log(`Environment: ${process.env.NODE_ENV || 'development'}`);
    
    try {
      const existingCenters = await prisma.centers.findMany();
      const existingZones = await prisma.zones.findMany();
      console.log(`✓ Database connected successfully`);
      console.log(`  - Existing centers: ${existingCenters.length}`);
      console.log(`  - Existing zones: ${existingZones.length}`);
    } catch (dbError) {
      console.error('❌ Database connection failed:', dbError);
      console.error('Error details:', {
        message: dbError.message,
        code: dbError.code,
        name: dbError.name
      });
      throw dbError;
    }
    console.log();

    // Step 1: Parse multi-polygon file (contains all wards)
    console.log('Step 1: Parsing Thu Duc Cu multi-polygon file...');
    const polyPath = join(process.cwd(), './raw_data/poly/thuduc_cu.poly');
    
    const wards = parseMultiPolyFile(polyPath);
    console.log(`✓ Parsed ${wards.length} wards from poly file\n`);

    if (wards.length === 0) {
      throw new Error('No wards found in poly file');
    }

    // Show parsed wards
    console.log('Found wards:');
    for (const ward of wards) {
      const code = generateZoneCode(ward.name);
      console.log(`  - ${ward.name} (${code}): ${ward.coordinates.length} points`);
    }
    console.log();

    // Step 2: Create/update center for TP Thủ Đức (cũ)
    console.log('Step 2: Creating center for TP Thủ Đức (cũ)...');
    const centerBoundary = unionPolygons(wards);
    const centerCentroid = calculateCentroid(centerBoundary);
    
    const centerGeoJSON = {
      type: 'Polygon',
      coordinates: [centerBoundary],
    };

    const thuDucCenter = await prisma.centers.upsert({
      where: { code: 'THUDUC_OLD' },
      update: {
        name: 'TP Thủ Đức (cũ)',
        address: 'Thành phố Thủ Đức (cũ), Thành phố Hồ Chí Minh',
        lat: centerCentroid.lat,
        lon: centerCentroid.lon,
        polygon: centerGeoJSON,
      },
      create: {
        code: 'THUDUC_OLD',
        name: 'TP Thủ Đức (cũ)',
        address: 'Thành phố Thủ Đức (cũ), Thành phố Hồ Chí Minh',
        lat: centerCentroid.lat,
        lon: centerCentroid.lon,
        polygon: centerGeoJSON,
      },
    });

    console.log(`✓ Center created: ${thuDucCenter.name}`);
    console.log(`  Code: ${thuDucCenter.code}`);
    console.log(`  Centroid: ${centerCentroid.lat.toFixed(6)}, ${centerCentroid.lon.toFixed(6)}\n`);

    // Step 3: Create zones (wards) that belong to Thu Duc center
    console.log('Step 3: Creating zones (wards)...');
    let createdCount = 0;
    let updatedCount = 0;

    for (const ward of wards) {
      try {
        const code = generateZoneCode(ward.name);
        const centroid = calculateCentroid(ward.coordinates);
        
        const wardGeoJSON = {
          type: 'Polygon',
          coordinates: [ward.coordinates],
        };

        // Check if zone already exists
        const existing = await prisma.zones.findUnique({
          where: { code },
        });

        const wardZone = await prisma.zones.upsert({
          where: { code },
          update: {
            name: ward.name,
            polygon: wardGeoJSON,
            center_id: thuDucCenter.center_id,
          },
          create: {
            code,
            name: ward.name,
            polygon: wardGeoJSON,
            center_id: thuDucCenter.center_id,
          },
        });

        if (existing) {
          updatedCount++;
          console.log(`  ✓ Updated: ${wardZone.name} (${wardZone.code})`);
        } else {
          createdCount++;
          console.log(`  ✓ Created: ${wardZone.name} (${wardZone.code})`);
        }
        console.log(`    Centroid: ${centroid.lat.toFixed(6)}, ${centroid.lon.toFixed(6)}`);
      } catch (error) {
        console.error(`  ✗ Error processing ${ward.name}:`, error);
      }
    }

    console.log();

    // Final summary
    const totalCenters = await prisma.centers.count();
    const totalZones = await prisma.zones.count();

    console.log('='.repeat(60));
    console.log('✅ Zones Seeding Completed Successfully!');
    console.log('='.repeat(60));
    console.log('\nSummary:');
    console.log(`  - Center: TP Thủ Đức (cũ)`);
    console.log(`  - Zones (wards): ${wards.length}`);
    console.log(`  - Created: ${createdCount}`);
    console.log(`  - Updated: ${updatedCount}`);
    console.log(`  - Total centers in DB: ${totalCenters}`);
    console.log(`  - Total zones in DB: ${totalZones}`);
    console.log('\nNext steps:');
    console.log('  1. Seed roads: npm run seed:roads');
    console.log('  2. Seed addresses: npm run seed:addresses');
    console.log('='.repeat(60));

  } catch (error) {
    console.error('\n❌ Error seeding zones:', error);
    throw error;
  } finally {
    await prisma.$disconnect();
  }
}

// Run if executed directly
if (import.meta.url === `file://${process.argv[1]}`) {
  seedZones()
    .then(() => {
      console.log('Done!');
      process.exit(0);
    })
    .catch((error) => {
      console.error('Failed:', error);
      process.exit(1);
    });
}

export { seedZones };
