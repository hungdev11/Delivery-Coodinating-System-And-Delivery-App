/**
 * Seeder v2 for zones (districts/phường)
 * Source: GeoJSON exported from PBF (see README)
 * - Center: HCMC
 * - Zones: admin_level=6 (districts). Wards are optional and skipped.
 */

import { PrismaClient } from '@prisma/client';
import { readFileSync } from 'fs';
import { join } from 'path';
import { generateZoneCode, calculateCentroid, parseMultiPolyFile, unionPolygons } from '../../utils/polygon-parser';

const prisma = new PrismaClient();
const START_INDEX = process.env.ZONE_START ? parseInt(process.env.ZONE_START, 10) : 0;
const END_INDEX = process.env.ZONE_END ? parseInt(process.env.ZONE_END, 10) : undefined;
const BATCH_LOG = 100;

type Feature = {
  type: string;
  properties: Record<string, any>;
  geometry: {
    type: 'Polygon' | 'MultiPolygon';
    coordinates: number[][][] | number[][][][];
  };
};

function createStepLogger(stepName: string) {
  const start = Date.now();
  let lastLine = '';

  const logProgress = (current: number, total?: number) => {
    const percent = total && total > 0 ? ` (${((current / total) * 100).toFixed(1)}%)` : '';
    const line = `  ${stepName}: ${current}${total ? '/' + total : ''}${percent}`;
    if (line === lastLine) return;
    lastLine = line;
    process.stdout.write(`\r${line}`);
  };

  const done = (extra?: string) => {
    const duration = ((Date.now() - start) / 1000).toFixed(1);
    process.stdout.write('\n');
    console.log(`✓ ${stepName} done in ${duration}s${extra ? ` (${extra})` : ''}`);
  };

  return { logProgress, done };
}

function normalizePolygon(geom: Feature['geometry']): number[][] {
  if (geom.type === 'Polygon') {
    return geom.coordinates?.[0] || [];
  }
  // MultiPolygon: take first polygon ring
  const mp = geom.coordinates as number[][][][];
  return mp?.[0]?.[0] || [];
}

function loadFeatures(geoPath: string): Feature[] {
  const raw = readFileSync(geoPath, 'utf-8');
  const parsed = JSON.parse(raw);
  if (!parsed || !parsed.features) return [];
  return parsed.features as Feature[];
}

function loadBoundaryRing(polyPath: string): number[][] | null {
  try {
    const polys = parseMultiPolyFile(polyPath);
    if (!polys || polys.length === 0) return null;
    const union = unionPolygons(polys);
    return union && union.length >= 3 ? union : null;
  } catch (e) {
    console.warn(`⚠️ Cannot load boundary poly ${polyPath}:`, e);
    return null;
  }
}

function pointInRing(point: { lat: number; lon: number }, ring: number[][] | null): boolean {
  if (!ring || ring.length < 3) return true; // skip check if missing boundary
  const x = point.lon;
  const y = point.lat;
  let inside = false;
  for (let i = 0, j = ring.length - 1; i < ring.length; j = i++) {
    const cur = ring[i];
    const prev = ring[j];
    if (!cur || !prev || cur.length < 2 || prev.length < 2) continue;
    const xi = cur[0], yi = cur[1];
    const xj = prev[0], yj = prev[1];
    const denom = (yj - yi) + Number.EPSILON;
    const intersect = ((yi > y) !== (yj > y)) && (x < (xj - xi) * (y - yi) / denom + xi);
    if (intersect) inside = !inside;
  }
  return inside;
}

async function upsertCenter(name: string, code: string, boundary: number[][]) {
  const centroid = calculateCentroid(boundary);
  const geoJson = { type: 'Polygon', coordinates: [boundary] };
  return prisma.centers.upsert({
    where: { code },
    update: { name, address: name, lat: centroid.lat, lon: centroid.lon, polygon: geoJson },
    create: { code, name, address: name, lat: centroid.lat, lon: centroid.lon, polygon: geoJson },
  });
}

async function upsertZones(centerId: string, features: Feature[], levelLabel: string) {
  let created = 0;
  let updated = 0;
  const batchSize = 100;
  const step = createStepLogger(`Inserting ${levelLabel.toLowerCase()}s`);

  for (let i = 0; i < features.length; i += batchSize) {
    const batch = features.slice(i, i + batchSize);
    const data: Array<{ code: string; name: string; polygon: any; center_id: string }> = [];

    for (const f of batch) {
      try {
        const name = f.properties?.name || f.properties?.['name:vi'] || f.properties?.['ref'] || 'UNKNOWN';
        const code = generateZoneCode(name);
        const ring = normalizePolygon(f.geometry);
        if (!ring || ring.length < 3) {
          console.warn(`⚠️ Skip ${name}: invalid polygon`);
          continue;
        }
        const geoJson =
          f.geometry.type === 'Polygon'
            ? { type: 'Polygon', coordinates: [ring] }
            : { type: 'MultiPolygon', coordinates: f.geometry.coordinates };

        data.push({
          code,
          name,
          polygon: geoJson,
          center_id: centerId,
        });
      } catch (e) {
        console.error(`  ✗ Error processing feature:`, e);
        continue;
      }
    }

    if (data.length > 0) {
      await prisma.zones.createMany({
        data,
        skipDuplicates: true,
      });
      created += data.length;
    }

    step.logProgress(Math.min(i + batchSize, features.length), features.length);
  }

  step.done(`created=${created}, updated=${updated}`);
  return { created, updated };
}

async function seedZonesV2() {
  console.log('🌱 Zones seeder v2 (GeoJSON from PBF) ...');
  console.log('='.repeat(60));

  try {
    // Step 0: check DB
    console.log('Step 0: Checking database connection...');
    await prisma.$queryRaw`SELECT 1`;
    console.log('✓ DB ok\n');

    // Guard: run only when zones table is empty
    const existing = await prisma.zones.count();
    if (existing > 0) {
      console.log(`⚠️ zones table is not empty (${existing} rows). Skip seeding.`);
      return;
    }

    // Paths
    const districtsPath = join(process.cwd(), './raw_data/extracted/hcmc-districts.geojson'); // admin_level=6
    const boundaryPath = join(process.cwd(), './raw_data/poly/hcmc.poly'); // HCMC boundary for filter

    // Load features
    console.log('Step 1: Loading districts geojson...');
    let districtFeatures = loadFeatures(districtsPath);
    console.log(`✓ districts loaded (raw): ${districtFeatures.length}`);

    // Filter admin_level=6 only
    districtFeatures = districtFeatures.filter(f => {
      const lvl = f.properties?.admin_level;
      return lvl === '6' || lvl === 6;
    });

    // Optional: filter by HCMC boundary
    const boundaryRing = loadBoundaryRing(boundaryPath);
    if (boundaryRing) {
      const before = districtFeatures.length;
      districtFeatures = districtFeatures.filter(f => {
        const ring = normalizePolygon(f.geometry);
        const centroid = calculateCentroid(ring);
        return pointInRing({ lat: centroid.lat, lon: centroid.lon }, boundaryRing);
      });
      console.log(`✓ boundary filter applied: ${before} → ${districtFeatures.length}`);
    }
    // Optional range slicing to speed up experimentation
    if (END_INDEX !== undefined || START_INDEX > 0) {
      districtFeatures = districtFeatures.slice(START_INDEX, END_INDEX);
      console.log(`✓ applied range slice [${START_INDEX}, ${END_INDEX ?? 'end'}) → ${districtFeatures.length} districts`);
    }

    console.log(`✓ districts kept: ${districtFeatures.length}\n`);

    if (districtFeatures.length === 0) {
      throw new Error('Missing districts features after filtering');
    }

    // Center = HCMC boundary = union of districts (take first MultiPolygon for simplicity)
    const firstDistrict = districtFeatures[0];
    const centerBoundary = normalizePolygon(firstDistrict.geometry);
    const center = await upsertCenter('Thành phố Hồ Chí Minh', 'HCMC', centerBoundary);
    console.log(`✓ Center upserted: ${center.name}\n`);

    console.log('Step 3: Upserting districts (admin_level=6)...');
    const dRes = await upsertZones(center.center_id, districtFeatures, 'District');
    console.log(`   → Created ${dRes.created}, Updated ${dRes.updated}\n`);

    const totalCenters = await prisma.centers.count();
    const totalZones = await prisma.zones.count();
    console.log('='.repeat(60));
    console.log('✅ Zones Seeding v2 Completed (Districts only)');
    console.log(`Centers: ${totalCenters} | Zones: ${totalZones}`);
  } catch (err) {
    console.error('❌ Error:', err);
    throw err;
  } finally {
    await prisma.$disconnect();
  }
}

// Run if executed directly
if (import.meta.url === `file://${process.argv[1]}`) {
  seedZonesV2()
    .then(() => {
      console.log('Done!');
      process.exit(0);
    })
    .catch((error) => {
      console.error('Failed:', error);
      process.exit(1);
    });
}

export { seedZonesV2 };
