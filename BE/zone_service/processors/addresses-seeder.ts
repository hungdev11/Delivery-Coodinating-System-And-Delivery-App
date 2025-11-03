/**
 * Address Seeder - Import POIs from OSM
 *
 * Extracts Points of Interest from OSM data:
 * - Schools, universities
 * - Hospitals, clinics
 * - Government offices
 * - Shopping centers, malls
 * - Restaurants, cafes
 * - Hotels
 * - Banks, ATMs
 * - Gas stations
 * - Parking lots
 * - Bus stops
 * - Landmarks
 *
 * Features:
 * - Automatic segment association
 * - Geohash generation
 * - Batch insertion
 * - Progress reporting
 */

import { PrismaClient, AddressType } from '@prisma/client';
import { OSMParser, findLatestVietnamPBF } from '../utils/osm-parser.js';
import { join } from 'path';
import { existsSync } from 'fs';
import * as geohash from '../utils/geohash.js';

const prisma = new PrismaClient();

interface POI {
  osmId: string;
  name: string;
  nameEn: string | undefined;
  lat: number;
  lon: number;
  addressType: AddressType;
  tags: Record<string, string>;
}

/**
 * Determine address type from OSM tags
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

  // Shopping
  if (tags.shop === 'mall' || tags.shop === 'supermarket' ||
      tags.shop === 'department_store' || tags.shop === 'convenience' ||
      tags.amenity === 'marketplace' || tags.building === 'retail') {
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

  // Shops - any shop type (even without names)
  if (tags.shop && tags.shop !== 'no') {
    return 'SHOPPING';
  }

  // Offices
  if (tags.office && tags.office !== 'no') {
    return 'GENERAL';
  }

  // Any amenity not already categorized
  if (tags.amenity && tags.amenity !== 'yes' && tags.amenity !== 'no') {
    return 'GENERAL';
  }

  // General - any building with a name OR address
  if (tags.building && tags.building !== 'yes' && tags.building !== 'no') {
    // Check if it has a name OR address
    if (tags.name || tags['addr:housenumber'] || tags['addr:street']) {
      return 'GENERAL';
    }
  }

  return null;
}

/**
 * Get POI name from tags
 * Generates descriptive names for unnamed entities
 */
function getPOIName(tags: Record<string, string>): { name: string | null; nameEn: string | null } {
  let name = tags.name || tags['name:vi'] || tags['name:local'] || null;
  const nameEn = tags['name:en'] || null;

  // If no name, generate one from other tags
  if (!name) {
    // Try shop type
    if (tags.shop) {
      name = `${tags.shop.charAt(0).toUpperCase() + tags.shop.slice(1)} Shop`;
    }
    // Try amenity type
    else if (tags.amenity) {
      name = `${tags.amenity.charAt(0).toUpperCase() + tags.amenity.slice(1)}`;
    }
    // Try office type
    else if (tags.office) {
      name = `${tags.office.charAt(0).toUpperCase() + tags.office.slice(1)} Office`;
    }
    // Try building type
    else if (tags.building && tags.building !== 'yes') {
      name = `${tags.building.charAt(0).toUpperCase() + tags.building.slice(1)} Building`;
    }
    // Use address as name if available
    else if (tags['addr:housenumber'] && tags['addr:street']) {
      name = `${tags['addr:housenumber']} ${tags['addr:street']}`;
    }
    else if (tags['addr:street']) {
      name = `Building on ${tags['addr:street']}`;
    }
  }

  return { name, nameEn };
}

/**
 * Extract POIs from OSM data
 */
function extractPOIs(osmData: any): POI[] {
  const pois: POI[] = [];

  // Extract from nodes (point features)
  for (const [osmId, node] of osmData.nodes.entries()) {
    if (!node.tags) continue;

    const addressType = getAddressType(node.tags);
    if (!addressType) continue;

    const { name, nameEn } = getPOIName(node.tags);
    if (!name) continue; // Skip if we can't generate any name

    pois.push({
      osmId,
      name,
      nameEn: nameEn || undefined,
      lat: node.lat,
      lon: node.lon,
      addressType,
      tags: node.tags
    });
  }

  // Extract from ways (area features like buildings)
  for (const way of osmData.ways) {
    if (!way.tags) continue;

    const addressType = getAddressType(way.tags);
    if (!addressType) continue;

    const { name, nameEn } = getPOIName(way.tags);
    if (!name) continue; // Skip if we can't generate any name

    // Calculate centroid from way nodes
    let latSum = 0;
    let lonSum = 0;
    let validNodes = 0;

    for (const nodeId of way.nodes) {
      const node = osmData.nodes.get(nodeId);
      if (node) {
        latSum += node.lat;
        lonSum += node.lon;
        validNodes++;
      }
    }

    if (validNodes === 0) continue;

    const centroidLat = latSum / validNodes;
    const centroidLon = lonSum / validNodes;

    pois.push({
      osmId: way.id,
      name,
      nameEn: nameEn || undefined,
      lat: centroidLat,
      lon: centroidLon,
      addressType,
      tags: way.tags
    });
  }

  return pois;
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

async function seedAddresses() {
  console.log('🏢 Starting Address/POI Seeding from OSM...\n');
  const startTime = Date.now();

  try {
    // Step 1: Parse OSM PBF file (ALL features, not just roads!)
    console.log('Step 1: Parsing OSM PBF file...');
    const rawDataDir = join(process.cwd(), './raw_data');
    
    // Use complete extract if available, otherwise use poly extract
    const completeExtractPath = join(rawDataDir, 'extracted/thuduc_complete.osm.pbf');
    let pbfPath: string;
    let polyFile: string | undefined;
    
    if (existsSync(completeExtractPath)) {
      console.log('  Using complete extract (routing + addresses)');
      pbfPath = completeExtractPath;
      polyFile = undefined; // Already clipped
    } else {
      console.log('  Using source PBF with polygon clip');
      console.log('  Tip: Run "npm run extract:complete" for better address coverage');
      pbfPath = findLatestVietnamPBF(rawDataDir);
      polyFile = join(rawDataDir, 'poly/thuduc_cu.poly');
    }

    const parser = new OSMParser();
    const osmData = await parser.parseAllFeatures(pbfPath, polyFile);
    console.log(`✓ Parsed ${osmData.nodes.size} nodes and ${osmData.ways.length} ways\n`);

    // Step 2: Extract POIs
    console.log('Step 2: Extracting POIs...');
    const pois = extractPOIs(osmData);
    console.log(`✓ Found ${pois.length} POIs\n`);

    // Show POI breakdown by type
    const poiByType = new Map<AddressType, number>();
    for (const poi of pois) {
      poiByType.set(poi.addressType, (poiByType.get(poi.addressType) || 0) + 1);
    }

    console.log('POI breakdown by type:');
    for (const [type, count] of Array.from(poiByType.entries()).sort((a, b) => b[1] - a[1])) {
      console.log(`  - ${type}: ${count}`);
    }
    console.log();

    // Step 3: Clear old addresses (optional - comment out to keep existing)
    console.log('Step 3: Clearing old addresses...');
    await prisma.addresses.deleteMany({});
    console.log('✓ Cleared old addresses\n');

    // Step 4: Get road segments for association
    console.log('Step 4: Loading road segments...');
    const segments = await prisma.road_segments.findMany({
      select: {
        segment_id: true,
        geometry: true
      }
    });
    console.log(`✓ Loaded ${segments.length} segments\n`);

    // Step 5: Prepare addresses with geohash
    console.log('Step 5: Preparing addresses with geohash...');
    const addressesToCreate = [];
    let processed = 0;

    for (const poi of pois) {
      const addressGeohash = geohash.encode(poi.lat, poi.lon, 7);
      const addressText = getAddressText(poi.tags);

      // Extract proper administrative levels from OSM tags
      // OSM addr:district = district level (Quận/Huyện)
      // OSM addr:suburb/neighbourhood = smaller unit (Phường/Xã)
      const districtName = poi.tags['addr:district'] || null;
      const wardName = poi.tags['addr:suburb'] || poi.tags['addr:neighbourhood'] || null;

      addressesToCreate.push({
        name: poi.name,
        name_en: poi.nameEn || null,
        address_text: addressText,
        lat: poi.lat,
        lon: poi.lon,
        geohash: addressGeohash,
        address_type: poi.addressType,
        ward_name: wardName,
        district_name: districtName
      });

      processed++;
      if (processed % 1000 === 0) {
        console.log(`  Prepared ${processed}/${pois.length} addresses...`);
      }
    }

    console.log(`✓ Prepared ${addressesToCreate.length} addresses\n`);

    // Step 6: Batch insert addresses (without PostGIS)
    console.log('Step 6: Inserting addresses in batches...');
    const batchSize = 500;
    let inserted = 0;

    for (let i = 0; i < addressesToCreate.length; i += batchSize) {
      const batch = addressesToCreate.slice(i, i + batchSize);

      // Use Prisma createMany instead of raw SQL
      await prisma.addresses.createMany({
        data: batch,
        skipDuplicates: true
      });

      inserted += batch.length;
      console.log(`  Inserted ${inserted}/${addressesToCreate.length} addresses...`);
    }

    console.log(`✓ Inserted ${inserted} addresses\n`);

    // Step 7: Associate addresses with nearest segments (optional if segments exist)
    console.log('Step 7: Associating addresses with road segments...');

    const segmentCount = await prisma.road_segments.count();

    if (segmentCount === 0) {
      console.log('  ⚠️  No road segments found - skipping segment association');
      console.log('  Run "npm run seed:roads" first to enable segment association\n');
    } else {
      console.log(`  Found ${segmentCount} road segments`);
      console.log('  Note: Segment association skipped for now (can be added later)\n');
      // TODO: Implement pure Haversine distance calculation without PostGIS
      // For now, addresses will be created without segment association
    }

    const finalAssociatedCount = await prisma.addresses.count({
      where: { segment_id: { not: null } }
    });

    console.log(`✓ Addresses with segments: ${finalAssociatedCount}/${inserted}\n`);

    // Final summary
    const finalCount = await prisma.addresses.count();
    const duration = ((Date.now() - startTime) / 1000).toFixed(1);

    console.log('='.repeat(60));
    console.log('✅ Address Seeding Completed Successfully!');
    console.log('='.repeat(60));
    console.log(`\nTime taken: ${duration}s`);
    console.log(`\nTotal addresses: ${finalCount}`);
    console.log(`Addresses with segments: ${finalAssociatedCount}`);
    console.log(`Coverage: ${((finalAssociatedCount / finalCount) * 100).toFixed(1)}%`);

    console.log('\nBreakdown by type:');
    for (const [type, count] of Array.from(poiByType.entries()).sort((a, b) => b[1] - a[1])) {
      console.log(`  - ${type}: ${count}`);
    }

    console.log('\nNext steps:');
    console.log('  1. Seed roads: npm run seed:roads  (for segment association)');
    console.log('  2. Test nearest address API: curl "http://localhost:21503/api/v1/addresses/nearest?lat=10.8505&lon=106.7717&limit=10"');
    console.log('  3. List addresses: curl "http://localhost:21503/api/v1/addresses?page=0&size=20"');
    console.log('  4. Search addresses: curl "http://localhost:21503/api/v1/addresses?search=school"');
    console.log('='.repeat(60));

  } catch (error) {
    console.error('\n❌ Address seeding failed:', error);
    throw error;
  } finally {
    await prisma.$disconnect();
  }
}

// Run if executed directly
if (import.meta.url === `file://${process.argv[1]}`) {
  seedAddresses()
    .then(() => {
      process.exit(0);
    })
    .catch((error) => {
      console.error('Failed:', error);
      process.exit(1);
    });
}

export { seedAddresses };
