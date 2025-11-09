/**
 * Overpass API Address Seeder
 *
 * Gets REAL address data from OpenStreetMap using Overpass API
 * Much more comprehensive than PBF parsing!
 *
 * Queries for:
 * - All amenities (restaurants, schools, hospitals, banks, etc.)
 * - All shops
 * - All tourism locations
 * - All offices
 * - Buildings with addresses
 */

import { PrismaClient, AddressType } from '@prisma/client';
import axios from 'axios';
import * as geohash from '../../utils/geohash';
import { readFileSync } from 'fs';
import { join } from 'path';

const prisma = new PrismaClient();

const OVERPASS_API = 'https://overpass-api.de/api/interpreter';

/**
 * Parse .poly file and calculate bounding box
 */
function parsePolyFileBbox(polyPath: string): string {
  const content = readFileSync(polyPath, 'utf-8');
  const lines = content.split('\n');

  let minLat = Infinity;
  let maxLat = -Infinity;
  let minLon = Infinity;
  let maxLon = -Infinity;

  for (const line of lines) {
    const trimmed = line.trim();
    // Skip empty lines, headers, and END markers
    if (!trimmed || trimmed === 'END' || trimmed.includes('area_') || !trimmed.includes('.')) {
      continue;
    }

    // Parse coordinate line: "lon lat"
    const parts = trimmed.split(/\s+/);
    if (parts.length >= 2) {
      const lon = parseFloat(parts[0]);
      const lat = parseFloat(parts[1]);

      if (!isNaN(lon) && !isNaN(lat)) {
        minLat = Math.min(minLat, lat);
        maxLat = Math.max(maxLat, lat);
        minLon = Math.min(minLon, lon);
        maxLon = Math.max(maxLon, lon);
      }
    }
  }

  // Format: south,west,north,east
  return `${minLat.toFixed(6)},${minLon.toFixed(6)},${maxLat.toFixed(6)},${maxLon.toFixed(6)}`;
}

// Read bounding box from old Thủ Đức polygon file
const thuDucPolyPath = join(process.cwd(), 'raw_data/poly/thuduc_cu.poly');
const THUDUC_BBOX = parsePolyFileBbox(thuDucPolyPath);

interface OverpassElement {
  type: string;
  id: number;
  lat?: number;
  lon?: number;
  tags?: Record<string, string>;
  center?: { lat: number; lon: number };
}

/**
 * Overpass QL query to get POIs in old Thủ Đức City area
 * Simplified query to reduce server load
 */
const OVERPASS_QUERY = `
[out:json][timeout:180][bbox:${THUDUC_BBOX}];
(
  node["amenity"]["name"](${THUDUC_BBOX});
  way["amenity"]["name"](${THUDUC_BBOX});
  node["shop"]["name"](${THUDUC_BBOX});
  way["shop"]["name"](${THUDUC_BBOX});
);
out center;
`;

/**
 * Map OSM tags to address types
 */
function getAddressType(tags: Record<string, string>): AddressType | null {
  // Schools and education
  if (tags.amenity === 'school' || tags.amenity === 'university' ||
      tags.amenity === 'college' || tags.amenity === 'kindergarten' ||
      tags.building === 'school' || tags.building === 'university') {
    return 'SCHOOL';
  }

  // Healthcare
  if (tags.amenity === 'hospital' || tags.amenity === 'clinic' ||
      tags.amenity === 'doctors' || tags.amenity === 'pharmacy' ||
      tags.healthcare || tags.building === 'hospital') {
    return 'HOSPITAL';
  }

  // Government
  if (tags.amenity === 'townhall' || tags.amenity === 'public_building' ||
      tags.amenity === 'community_centre' || tags.amenity === 'police' ||
      tags.amenity === 'fire_station' || tags.amenity === 'post_office' ||
      tags.office === 'government' || tags.government ||
      tags.building === 'public') {
    return 'GOVERNMENT';
  }

  // Shopping - EXPANDED
  if (tags.shop || tags.amenity === 'marketplace' || tags.building === 'retail') {
    return 'SHOPPING';
  }

  // Food and drink
  if (tags.amenity === 'restaurant' || tags.amenity === 'cafe' ||
      tags.amenity === 'fast_food' || tags.amenity === 'food_court' ||
      tags.amenity === 'bar' || tags.amenity === 'pub') {
    return 'RESTAURANT';
  }

  // Hotels
  if (tags.tourism === 'hotel' || tags.tourism === 'hostel' ||
      tags.tourism === 'motel' || tags.tourism === 'guest_house' ||
      tags.building === 'hotel') {
    return 'HOTEL';
  }

  // Banks
  if (tags.amenity === 'bank' || tags.amenity === 'atm' ||
      tags.building === 'bank') {
    return 'BANK';
  }

  // Gas stations
  if (tags.amenity === 'fuel' || tags.building === 'gas_station') {
    return 'GAS_STATION';
  }

  // Parking
  if (tags.amenity === 'parking' || tags.amenity === 'parking_space' ||
      tags.building === 'parking') {
    return 'PARKING';
  }

  // Bus stops
  if (tags.highway === 'bus_stop' || tags.public_transport === 'stop_position' ||
      tags.public_transport === 'platform' || tags.amenity === 'bus_station') {
    return 'BUS_STOP';
  }

  // Landmarks and tourism
  if (tags.tourism === 'attraction' || tags.tourism === 'museum' ||
      tags.tourism === 'artwork' || tags.tourism === 'viewpoint' ||
      tags.historic || tags.building === 'cathedral' ||
      tags.building === 'church' || tags.building === 'temple' ||
      tags.amenity === 'place_of_worship') {
    return 'LANDMARK';
  }

  // Offices
  if (tags.office) {
    return 'GENERAL';
  }

  // Any other amenity
  if (tags.amenity) {
    return 'GENERAL';
  }

  // Buildings with addresses
  if (tags.building && (tags.name || tags['addr:housenumber'])) {
    return 'GENERAL';
  }

  return null;
}

/**
 * Get name from tags
 */
function getName(tags: Record<string, string>): { name: string | null; nameEn: string | null } {
  let name = tags.name || tags['name:vi'] || tags['name:local'] || null;
  const nameEn = tags['name:en'] || null;

  // Generate name from other tags if missing
  if (!name) {
    if (tags.shop) {
      name = `${tags.shop.charAt(0).toUpperCase() + tags.shop.slice(1)} Shop`;
    } else if (tags.amenity) {
      name = `${tags.amenity.charAt(0).toUpperCase() + tags.amenity.slice(1)}`;
    } else if (tags.office) {
      name = `${tags.office.charAt(0).toUpperCase() + tags.office.slice(1)} Office`;
    } else if (tags['addr:housenumber'] && tags['addr:street']) {
      name = `${tags['addr:housenumber']} ${tags['addr:street']}`;
    } else if (tags['addr:street']) {
      name = `Building on ${tags['addr:street']}`;
    }
  }

  return { name, nameEn };
}

/**
 * Get address text from tags
 */
function getAddressText(tags: Record<string, string>): string | null {
  const parts: string[] = [];

  if (tags['addr:housenumber']) parts.push(tags['addr:housenumber']);
  if (tags['addr:street']) parts.push(tags['addr:street']);
  if (tags['addr:district']) parts.push(tags['addr:district']);
  if (tags['addr:city']) parts.push(tags['addr:city']);

  return parts.length > 0 ? parts.join(', ') : null;
}

/**
 * Query Overpass API
 */
async function queryOverpass(): Promise<OverpassElement[]> {
  console.log('Querying Overpass API...');
  console.log(`Bounding box: ${THUDUC_BBOX} (Old Thủ Đức City from polygon)`);
  console.log('This may take 1-2 minutes...\n');

  try {
    const response = await axios.post(
      OVERPASS_API,
      OVERPASS_QUERY,
      {
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        timeout: 180000, // 3 minutes
      }
    );

    return response.data.elements || [];
  } catch (error: any) {
    if (error.code === 'ECONNABORTED') {
      throw new Error('Overpass API timeout - the query is too large. Try a smaller area.');
    }
    throw error;
  }
}

/**
 * Main seeder function
 */
async function seedFromOverpass() {
  console.log('🌍 Overpass API Address Seeder\n');
  console.log('='.repeat(60));
  console.log('Getting REAL address data from OpenStreetMap');
  console.log('='.repeat(60));
  console.log();

  const startTime = Date.now();

  try {
    // Step 1: Query Overpass API
    console.log('Step 1: Querying Overpass API for Old Thủ Đức City POIs...');
    const elements = await queryOverpass();
    console.log(`✓ Retrieved ${elements.length} elements from OSM\n`);

    // Step 2: Parse elements
    console.log('Step 2: Parsing and filtering elements...');
    const addresses: Array<{
      name: string;
      name_en: string | null;
      address_text: string | null;
      lat: number;
      lon: number;
      geohash: string;
      ward_name: string | null;
      district_name: string | null;
      address_type: AddressType;
    }> = [];
    const typeBreakdown = new Map<AddressType, number>();

    for (const element of elements) {
      if (!element.tags) continue;

      const addressType = getAddressType(element.tags);
      if (!addressType) continue;

      const { name, nameEn } = getName(element.tags);
      if (!name) continue; // Skip if no name can be determined

      // Get coordinates
      let lat: number;
      let lon: number;

      if (element.lat && element.lon) {
        // Node
        lat = element.lat;
        lon = element.lon;
      } else if (element.center) {
        // Way (building/area) - use center point
        lat = element.center.lat;
        lon = element.center.lon;
      } else {
        continue; // Skip if no coordinates
      }

      // Count by type
      typeBreakdown.set(addressType, (typeBreakdown.get(addressType) || 0) + 1);

      // Extract proper administrative levels from OSM tags
      // OSM addr:district = district level (Quận/Huyện)
      // OSM addr:suburb/neighbourhood = smaller unit (Phường/Xã)
      const districtName = element.tags['addr:district'] || null;
      const wardName = element.tags['addr:suburb'] || element.tags['addr:neighbourhood'] || null;

      addresses.push({
        name,
        name_en: nameEn || null,
        address_text: getAddressText(element.tags),
        lat,
        lon,
        geohash: geohash.encode(lat, lon, 7),
        ward_name: wardName,
        district_name: districtName,
        address_type: addressType,
      });
    }

    console.log(`✓ Parsed ${addresses.length} valid addresses\n`);

    console.log('Address breakdown by type:');
    for (const [type, count] of Array.from(typeBreakdown.entries()).sort((a, b) => b[1] - a[1])) {
      console.log(`  - ${type}: ${count}`);
    }
    console.log();

    // Step 3: Clear old addresses
    console.log('Step 3: Clearing old addresses...');
    const deleted = await prisma.addresses.deleteMany({});
    console.log(`✓ Cleared ${deleted.count} old addresses\n`);

    // Step 4: Insert new addresses
    console.log('Step 4: Inserting addresses into database...');
    const batchSize = 500;
    let inserted = 0;

    for (let i = 0; i < addresses.length; i += batchSize) {
      const batch = addresses.slice(i, i + batchSize);

      await prisma.addresses.createMany({
        data: batch,
        skipDuplicates: true,
      });

      inserted += batch.length;
      console.log(`  Inserted ${inserted}/${addresses.length} addresses...`);
    }

    console.log(`✓ Inserted ${inserted} addresses\n`);

    // Final summary
    const finalCount = await prisma.addresses.count();
    const duration = ((Date.now() - startTime) / 1000).toFixed(1);

    console.log('='.repeat(60));
    console.log('✅ Overpass API Seeding Complete!');
    console.log('='.repeat(60));
    console.log(`\nTime taken: ${duration}s`);
    console.log(`Total REAL addresses: ${finalCount}`);

    console.log('\nAddress breakdown by type:');
    for (const [type, count] of Array.from(typeBreakdown.entries()).sort((a, b) => b[1] - a[1])) {
      const percentage = ((count / finalCount) * 100).toFixed(1);
      console.log(`  - ${type}: ${count} (${percentage}%)`);
    }

    console.log('\nNext steps:');
    console.log('  1. Test address API: curl "http://localhost:21503/api/v1/addresses?page=0&size=20"');
    console.log('  2. Test nearest query: curl "http://localhost:21503/api/v1/addresses/nearest?lat=10.8505&lon=106.7717&limit=10"');
    console.log('  3. Run comprehensive tests: npm run test:all');
    console.log('='.repeat(60));

  } catch (error) {
    console.error('\n❌ Overpass API seeding failed:', error);
    throw error;
  } finally {
    await prisma.$disconnect();
  }
}

// Run if executed directly
if (import.meta.url === `file://${process.argv[1]}`) {
  seedFromOverpass()
    .then(() => {
      process.exit(0);
    })
    .catch((error) => {
      console.error('Failed:', error);
      process.exit(1);
    });
}

export { seedFromOverpass };
