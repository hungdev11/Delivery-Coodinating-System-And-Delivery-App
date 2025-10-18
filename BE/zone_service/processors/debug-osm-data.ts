import { OSMParser } from '../utils/osm-parser.js';
import { join } from 'path';

async function debugOSMData() {
  const rawDataDir = join(process.cwd(), './raw_data');
  const pbfPath = join(rawDataDir, 'vietnam/vietnam-251013.osm.pbf');
  const thuDucPoly = join(rawDataDir, 'old_thuduc_city/thuduc_cu.poly');

  console.log('🔍 Debugging OSM Data Parsing...\n');
  console.log('PBF Path:', pbfPath);
  console.log('Polygon Path:', thuDucPoly);
  console.log();

  const parser = new OSMParser();
  const osmData = await parser.parsePBF(pbfPath, thuDucPoly);

  console.log('📊 PARSING RESULTS:');
  console.log('Total nodes parsed:', osmData.nodes.size);
  console.log('Total ways parsed:', osmData.ways.length);
  console.log();

  // Check how many have tags
  let nodesWithTags = 0;
  let nodesWithAmenity = 0;
  let nodesWithShop = 0;
  let nodesWithBuilding = 0;
  let nodesWithName = 0;

  console.log('🔍 Analyzing nodes...');
  for (const [id, node] of osmData.nodes.entries()) {
    if (node.tags) {
      nodesWithTags++;
      if (node.tags.amenity) nodesWithAmenity++;
      if (node.tags.shop) nodesWithShop++;
      if (node.tags.building) nodesWithBuilding++;
      if (node.tags.name) nodesWithName++;
    }
  }

  console.log('  Nodes with tags:', nodesWithTags);
  console.log('  Nodes with amenity tag:', nodesWithAmenity);
  console.log('  Nodes with shop tag:', nodesWithShop);
  console.log('  Nodes with building tag:', nodesWithBuilding);
  console.log('  Nodes with name tag:', nodesWithName);
  console.log();

  // Check ways
  let waysWithTags = 0;
  let waysWithAmenity = 0;
  let waysWithShop = 0;
  let waysWithBuilding = 0;
  let waysWithName = 0;

  console.log('🔍 Analyzing ways...');
  for (const way of osmData.ways) {
    if (way.tags) {
      waysWithTags++;
      if (way.tags.amenity) waysWithAmenity++;
      if (way.tags.shop) waysWithShop++;
      if (way.tags.building) waysWithBuilding++;
      if (way.tags.name) waysWithName++;
    }
  }

  console.log('  Ways with tags:', waysWithTags);
  console.log('  Ways with amenity tag:', waysWithAmenity);
  console.log('  Ways with shop tag:', waysWithShop);
  console.log('  Ways with building tag:', waysWithBuilding);
  console.log('  Ways with name tag:', waysWithName);
  console.log();

  // Sample some tagged nodes
  console.log('📝 Sample tagged nodes (first 10):');
  let sampleCount = 0;
  for (const [id, node] of osmData.nodes.entries()) {
    if (node.tags && sampleCount < 10) {
      console.log(`  Node ${id}:`, JSON.stringify(node.tags));
      sampleCount++;
    }
  }
  console.log();

  // Sample some tagged ways
  console.log('📝 Sample tagged ways (first 10):');
  sampleCount = 0;
  for (const way of osmData.ways) {
    if (way.tags && sampleCount < 10) {
      console.log(`  Way ${way.id}:`, JSON.stringify(way.tags));
      sampleCount++;
    }
  }

  process.exit(0);
}

debugOSMData().catch(console.error);
