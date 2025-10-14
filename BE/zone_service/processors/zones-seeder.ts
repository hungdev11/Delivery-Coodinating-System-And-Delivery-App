/**
 * Seeder for zones and districts
 * Processes .poly files and populates the zones table
 */

import { PrismaClient } from '@prisma/client';
import { readdirSync, statSync } from 'fs';
import { join } from 'path';
import { parsePolyFile, polyToGeoJSON, calculateCentroid } from '../utils/polygon-parser.js';

const prisma = new PrismaClient();

interface DistrictInfo {
  name: string;
  code: string;
  polyFile: string;
}

// District mapping for new Thu Duc districts
const THUDUC_DISTRICTS: DistrictInfo[] = [
  { name: 'Thủ Đức', code: 'TD_WARD', polyFile: 'thuduc_ward.poly' },
  { name: 'Linh Xuân', code: 'LX', polyFile: 'linhxuan_district.poly' },
  { name: 'Tăng Nhơn Phú', code: 'TNP', polyFile: 'tangnhonphu_district.poly' },
  { name: 'Long Bình', code: 'LB', polyFile: 'longbinh_district.poly' },
  { name: 'Long Phước', code: 'LP', polyFile: 'longphuoc_district.poly' },
  { name: 'Long Trường', code: 'LT', polyFile: 'longtruong_district.poly' },
  { name: 'Phước Long', code: 'PL', polyFile: 'phuoclong_district.poly' },
  { name: 'Đông Hòa', code: 'DH', polyFile: 'donghoa_district.poly' },
];

async function seedZones() {
  console.log('Starting zones seeding...');

  try {
    // First, ensure we have a center for Ho Chi Minh City
    const center = await prisma.centers.upsert({
      where: { code: 'HCMC' },
      update: {},
      create: {
        code: 'HCMC',
        name: 'Thành phố Hồ Chí Minh',
        address: 'Thành phố Hồ Chí Minh, Việt Nam',
        lat: 10.8231,
        lon: 106.6297,
      },
    });

    console.log(`✓ Center created/found: ${center.name}`);

    // Process old Thu Duc boundary
    const oldThudDucPath = join(
      process.cwd(),
      './raw_data/old_thuduc_city/thuduc_cu.poly'
    );

    console.log('\nProcessing old Thu Duc boundary...');
    const oldThudDuc = parsePolyFile(oldThudDucPath);
    const oldThudDucGeoJSON = polyToGeoJSON(oldThudDuc);

    console.log(`✓ Parsed old Thu Duc boundary with ${oldThudDuc.coordinates.length} rings`);

    // Process each new district within old Thu Duc
    const polyDir = join(process.cwd(), './raw_data/new_hochiminh_city');

    for (const district of THUDUC_DISTRICTS) {
      const polyPath = join(polyDir, district.polyFile);

      try {
        console.log(`\nProcessing ${district.name}...`);
        const polygon = parsePolyFile(polyPath);
        const geoJSON = polyToGeoJSON(polygon);

        // Check if we have valid coordinates
        if (!polygon.coordinates || polygon.coordinates.length === 0 || !polygon.coordinates[0] || polygon.coordinates[0].length === 0) {
          console.error(`✗ No valid coordinates found for ${district.name}`);
          continue;
        }

        const centroid = calculateCentroid(polygon.coordinates[0]);

        // Create or update zone
        const zone = await prisma.zones.upsert({
          where: { code: district.code },
          update: {
            name: district.name,
            polygon: geoJSON,
          },
          create: {
            code: district.code,
            name: district.name,
            polygon: geoJSON,
            center_id: center.center_id,
          },
        });

        console.log(`✓ Zone created/updated: ${zone.name} (${zone.code})`);
        console.log(`  Centroid: ${centroid.lat.toFixed(4)}, ${centroid.lon.toFixed(4)}`);
        console.log(`  Coordinates: ${polygon.coordinates[0].length} points`);
      } catch (error) {
        console.error(`✗ Error processing ${district.name}:`, error);
      }
    }

    console.log('\n✓ Zones seeding completed successfully!');
  } catch (error) {
    console.error('Error seeding zones:', error);
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
