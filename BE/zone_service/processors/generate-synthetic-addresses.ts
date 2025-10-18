/**
 * Synthetic Address Generator
 *
 * Generates realistic addresses along road segments with:
 * - Vietnamese naming patterns
 * - Address density based on road type
 * - Proper address types and categories
 * - Full segment association
 * - Geohash indexing
 *
 * Expected output: 12,000-15,000 addresses from 4,957 road segments
 */

import { PrismaClient, AddressType } from '@prisma/client';
import * as geohash from '../utils/geohash.js';

const prisma = new PrismaClient();

interface AddressToCreate {
  name: string;
  name_en?: string;
  address_text: string;
  lat: number;
  lon: number;
  geohash: string;
  segment_id: string;
  segment_position: number;
  distance_to_segment: number;
  projected_lat: number;
  projected_lon: number;
  zone_id?: string;
  ward_name?: string;
  district_name: string;
  address_type: AddressType;
}

/**
 * Address density configuration by road type
 * Interval in meters between addresses
 */
const ROAD_TYPE_CONFIG = {
  'MOTORWAY': { interval: 1000, types: ['GAS_STATION', 'PARKING'] },
  'TRUNK': { interval: 200, types: ['SHOPPING', 'RESTAURANT', 'BANK', 'HOTEL'] },
  'PRIMARY': { interval: 100, types: ['SHOPPING', 'RESTAURANT', 'BANK', 'HOSPITAL', 'SCHOOL'] },
  'SECONDARY': { interval: 50, types: ['SHOPPING', 'RESTAURANT', 'GENERAL', 'SCHOOL'] },
  'TERTIARY': { interval: 40, types: ['SHOPPING', 'RESTAURANT', 'GENERAL'] },
  'RESIDENTIAL': { interval: 30, types: ['GENERAL', 'SHOPPING', 'RESTAURANT'] },
  'SERVICE': { interval: 100, types: ['GENERAL', 'PARKING'] },
  'UNCLASSIFIED': { interval: 50, types: ['GENERAL'] },
  'LIVING_STREET': { interval: 25, types: ['GENERAL'] },
};

/**
 * Vietnamese address naming patterns
 */
const VIETNAMESE_NAMES = {
  GENERAL: [
    'Nhà số {num}',
    'Tòa nhà {num}',
    'Văn phòng {num}',
    'Căn hộ {num}',
    'Chung cư {num}',
  ],
  SHOPPING: [
    'Cửa hàng tiện lợi',
    'Siêu thị mini',
    'Tạp hóa',
    'Cửa hàng tổng hợp',
    'Cửa hàng Bách Hóa',
    'Shop thời trang',
    'Cửa hàng điện thoại',
    'Cửa hàng đồ gia dụng',
  ],
  RESTAURANT: [
    'Quán cơm',
    'Nhà hàng',
    'Quán café',
    'Quán phở',
    'Quán ăn',
    'Nhà hàng tiệc cưới',
    'Quán bún',
    'Quán bánh mì',
    'Tiệm bánh ngọt',
  ],
  SCHOOL: [
    'Trường Tiểu học',
    'Trường Trung học',
    'Trường Mầm non',
    'Trung tâm học',
    'Trường Đại học',
    'Trung tâm Ngoại ngữ',
  ],
  HOSPITAL: [
    'Phòng khám',
    'Bệnh viện',
    'Trạm y tế',
    'Hiệu thuốc',
    'Phòng khám đa khoa',
  ],
  BANK: [
    'ATM',
    'Ngân hàng',
    'Quầy giao dịch',
  ],
  GAS_STATION: [
    'Trạm xăng',
    'Cây xăng',
  ],
  PARKING: [
    'Bãi đỗ xe',
    'Bãi giữ xe',
  ],
  HOTEL: [
    'Khách sạn',
    'Nhà nghỉ',
    'Homestay',
  ],
  GOVERNMENT: [
    'Ủy ban nhân dân',
    'Bưu điện',
    'Công an',
  ],
  BUS_STOP: [
    'Trạm xe buýt',
    'Bến xe buýt',
  ],
  LANDMARK: [
    'Công viên',
    'Quảng trường',
    'Tượng đài',
  ],
};

/**
 * Generate a random Vietnamese name for address type
 */
function generateVietnameseName(type: AddressType, houseNumber: number): string {
  const patterns = VIETNAMESE_NAMES[type] || VIETNAMESE_NAMES.GENERAL;
  const pattern = patterns[Math.floor(Math.random() * patterns.length)];

  // Replace {num} with house number if present
  return pattern.replace('{num}', houseNumber.toString());
}

/**
 * Select address type based on road type and probability
 */
function selectAddressType(roadType: string, streetName: string): AddressType {
  const config = ROAD_TYPE_CONFIG[roadType as keyof typeof ROAD_TYPE_CONFIG] || ROAD_TYPE_CONFIG.UNCLASSIFIED;
  const possibleTypes = config.types;

  // Weight distribution
  const weights: Record<string, number> = {
    'GENERAL': 50,
    'SHOPPING': 20,
    'RESTAURANT': 15,
    'SCHOOL': 3,
    'HOSPITAL': 2,
    'BANK': 3,
    'GAS_STATION': 1,
    'PARKING': 3,
    'HOTEL': 1,
    'GOVERNMENT': 1,
    'BUS_STOP': 0, // Generated separately
    'LANDMARK': 0,
  };

  // Filter by possible types
  const availableTypes = possibleTypes.filter(t => weights[t] > 0);

  // Random weighted selection
  const totalWeight = availableTypes.reduce((sum, t) => sum + weights[t], 0);
  let random = Math.random() * totalWeight;

  for (const type of availableTypes) {
    random -= weights[type];
    if (random <= 0) {
      return type as AddressType;
    }
  }

  return 'GENERAL';
}

/**
 * Calculate point along a line segment
 */
function interpolatePoint(start: [number, number], end: [number, number], ratio: number): [number, number] {
  const lat = start[0] + (end[0] - start[0]) * ratio;
  const lon = start[1] + (end[1] - start[1]) * ratio;
  return [lat, lon];
}

/**
 * Calculate distance between two points (Haversine)
 */
function haversineDistance(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6371000; // Earth radius in meters
  const φ1 = lat1 * Math.PI / 180;
  const φ2 = lat2 * Math.PI / 180;
  const Δφ = (lat2 - lat1) * Math.PI / 180;
  const Δλ = (lon2 - lon1) * Math.PI / 180;

  const a = Math.sin(Δφ / 2) * Math.sin(Δφ / 2) +
            Math.cos(φ1) * Math.cos(φ2) *
            Math.sin(Δλ / 2) * Math.sin(Δλ / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

  return R * c;
}

/**
 * Calculate total length of a linestring
 */
function calculateLineLength(coordinates: number[][]): number {
  let totalLength = 0;
  for (let i = 1; i < coordinates.length; i++) {
    totalLength += haversineDistance(
      coordinates[i - 1][1], coordinates[i - 1][0],
      coordinates[i][1], coordinates[i][0]
    );
  }
  return totalLength;
}

/**
 * Generate addresses along a segment
 */
function generateAddressesForSegment(
  segment: any,
  startHouseNumber: number
): AddressToCreate[] {
  const addresses: AddressToCreate[] = [];

  // Parse geometry
  const geometry = typeof segment.geometry === 'string'
    ? JSON.parse(segment.geometry)
    : segment.geometry;

  if (!geometry || !geometry.coordinates || geometry.coordinates.length < 2) {
    return addresses;
  }

  const coordinates = geometry.coordinates;
  const segmentLength = calculateLineLength(coordinates);

  // Get road type configuration
  const roadType = segment.road_type || 'UNCLASSIFIED';
  const config = ROAD_TYPE_CONFIG[roadType as keyof typeof ROAD_TYPE_CONFIG] || ROAD_TYPE_CONFIG.UNCLASSIFIED;

  // Calculate number of addresses
  const numAddresses = Math.max(1, Math.floor(segmentLength / config.interval));

  // Generate addresses
  let houseNumber = startHouseNumber;

  for (let i = 0; i < numAddresses; i++) {
    // Calculate position along segment (0.0 to 1.0)
    const position = (i + 0.5) / numAddresses; // Center of each interval

    // Find which segment of the linestring this falls on
    let cumulativeLength = 0;
    let targetLength = position * segmentLength;
    let segmentIndex = 0;
    let segmentRatio = 0;

    for (let j = 1; j < coordinates.length; j++) {
      const segLen = haversineDistance(
        coordinates[j - 1][1], coordinates[j - 1][0],
        coordinates[j][1], coordinates[j][0]
      );

      if (cumulativeLength + segLen >= targetLength) {
        segmentIndex = j - 1;
        segmentRatio = (targetLength - cumulativeLength) / segLen;
        break;
      }

      cumulativeLength += segLen;
    }

    // Interpolate point
    const [lat, lon] = interpolatePoint(
      [coordinates[segmentIndex][1], coordinates[segmentIndex][0]],
      [coordinates[segmentIndex + 1][1], coordinates[segmentIndex + 1][0]],
      segmentRatio
    );

    // Add small offset (5-10m) perpendicular to road for realism
    const offset = (Math.random() - 0.5) * 0.0001; // ~10m
    const offsetLat = lat + offset;
    const offsetLon = lon + offset;

    // Select address type
    const addressType = selectAddressType(roadType, segment.name || '');

    // Generate name
    const name = generateVietnameseName(addressType, houseNumber);

    // Create address text
    const streetName = segment.name || segment.name_en || `Đường ${segment.segment_id.substring(0, 8)}`;
    const districtPart = segment.district_name ? `, ${segment.district_name}` : '';
    const addressText = `${houseNumber} ${streetName}${districtPart}`;

    addresses.push({
      name,
      name_en: addressType === 'GENERAL' ? `House ${houseNumber}` : undefined,
      address_text: addressText,
      lat: offsetLat,
      lon: offsetLon,
      geohash: geohash.encode(offsetLat, offsetLon, 7),
      segment_id: segment.segment_id,
      segment_position: position,
      distance_to_segment: Math.abs(offset) * 111000, // Approximate offset in meters
      projected_lat: lat,
      projected_lon: lon,
      zone_id: segment.zone_id || null,
      ward_name: segment.ward_name || null,
      district_name: segment.district_name || null,
      address_type: addressType,
    });

    houseNumber += 2; // Vietnamese addressing often uses even/odd sides
  }

  return addresses;
}

/**
 * Main generator function
 */
async function generateSyntheticAddresses() {
  console.log('🏗️  Synthetic Address Generator\n');
  console.log('='.repeat(60));
  const startTime = Date.now();

  try {
    // Step 1: Clear existing addresses (optional)
    console.log('Step 1: Clearing existing addresses...');
    const deletedCount = await prisma.addresses.deleteMany({});
    console.log(`✓ Cleared ${deletedCount.count} existing addresses\n`);

    // Step 2: Load road segments
    console.log('Step 2: Loading road segments...');
    const segments = await prisma.road_segments.findMany({
      select: {
        segment_id: true,
        road_id: true,
        geometry: true,
        zone_id: true,
        name: true,
        road_type: true,
      }
    });
    console.log(`✓ Loaded ${segments.length} road segments\n`);

    // Step 3: Generate addresses
    console.log('Step 3: Generating addresses along segments...');
    const allAddresses: AddressToCreate[] = [];
    let houseNumberCounter = 1;
    let processedCount = 0;

    for (const segment of segments) {
      const segmentAddresses = generateAddressesForSegment(
        {
          ...segment,
          ward_name: null,  // Smaller unit - not available for synthetic
          district_name: null,  // District - should be derived from zone if needed
        },
        houseNumberCounter
      );

      allAddresses.push(...segmentAddresses);
      houseNumberCounter += segmentAddresses.length * 2;

      processedCount++;
      if (processedCount % 500 === 0) {
        console.log(`  Processed ${processedCount}/${segments.length} segments (${allAddresses.length} addresses so far)...`);
      }
    }

    console.log(`✓ Generated ${allAddresses.length} addresses\n`);

    // Step 4: Show breakdown by type
    const typeBreakdown = new Map<AddressType, number>();
    for (const addr of allAddresses) {
      typeBreakdown.set(addr.address_type, (typeBreakdown.get(addr.address_type) || 0) + 1);
    }

    console.log('Address breakdown by type:');
    for (const [type, count] of Array.from(typeBreakdown.entries()).sort((a, b) => b[1] - a[1])) {
      console.log(`  - ${type}: ${count}`);
    }
    console.log();

    // Step 5: Insert addresses in batches
    console.log('Step 4: Inserting addresses into database...');
    const batchSize = 500;
    let inserted = 0;

    for (let i = 0; i < allAddresses.length; i += batchSize) {
      const batch = allAddresses.slice(i, i + batchSize);

      await prisma.addresses.createMany({
        data: batch,
        skipDuplicates: true,
      });

      inserted += batch.length;
      console.log(`  Inserted ${inserted}/${allAddresses.length} addresses...`);
    }

    console.log(`✓ Inserted ${inserted} addresses\n`);

    // Final summary
    const finalCount = await prisma.addresses.count();
    const duration = ((Date.now() - startTime) / 1000).toFixed(1);

    console.log('='.repeat(60));
    console.log('✅ Synthetic Address Generation Complete!');
    console.log('='.repeat(60));
    console.log(`\nTime taken: ${duration}s`);
    console.log(`Total addresses: ${finalCount}`);
    console.log(`Addresses with segments: ${finalCount} (100%)`);
    console.log(`Average addresses per segment: ${(finalCount / segments.length).toFixed(1)}`);

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
    console.error('\n❌ Address generation failed:', error);
    throw error;
  } finally {
    await prisma.$disconnect();
  }
}

// Run if executed directly
if (import.meta.url === `file://${process.argv[1]}`) {
  generateSyntheticAddresses()
    .then(() => {
      process.exit(0);
    })
    .catch((error) => {
      console.error('Failed:', error);
      process.exit(1);
    });
}

export { generateSyntheticAddresses };
