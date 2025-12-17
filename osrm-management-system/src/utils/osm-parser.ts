/**
 * OSM Parser Utilities
 */

import { existsSync, readdirSync } from 'fs';
import { join } from 'path';

export function findLatestVietnamPBF(rawDataDir: string): string {
  const vietnamDir = join(rawDataDir, 'vietnam');
  if (!existsSync(vietnamDir)) {
    throw new Error(`Vietnam data directory not found: ${vietnamDir}`);
  }

  const files = readdirSync(vietnamDir)
    .filter(f => f.endsWith('.osm.pbf'))
    .sort()
    .reverse();

  if (files.length === 0) {
    throw new Error(`No PBF files found in ${vietnamDir}`);
  }

  return join(vietnamDir, files[0]);
}
