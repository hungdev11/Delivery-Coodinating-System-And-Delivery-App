/**
 * Osmium Wrapper
 * Independent copy for OSRM Management Service
 */

import { exec } from 'child_process';
import { promisify } from 'util';
import { existsSync, mkdirSync } from 'fs';
import { join, dirname } from 'path';

const execAsync = promisify(exec);

export class OsmiumWrapper {
  private verbose: boolean;

  constructor(verbose: boolean = false) {
    this.verbose = verbose;
  }

  async checkInstallation(): Promise<boolean> {
    try {
      await execAsync('osmium --version');
      if (this.verbose) {
        console.log('✓ Osmium found');
      }
      return true;
    } catch (error) {
      console.error('❌ osmium-tool is not installed!');
      return false;
    }
  }

  private async supportsStrategyFlag(): Promise<boolean> {
    try {
      const { stdout } = await execAsync('osmium extract --help');
      return stdout.includes('--strategy') || stdout.includes('-s [');
    } catch {
      return true;
    }
  }

  async extractRoutingWithAddresses(
    inputPbf: string,
    polyFile: string,
    outputPbf: string
  ): Promise<void> {
    if (!existsSync(inputPbf)) {
      throw new Error(`Input file not found: ${inputPbf}`);
    }
    if (!existsSync(polyFile)) {
      throw new Error(`Poly file not found: ${polyFile}`);
    }

    const outputDir = dirname(outputPbf);
    if (!existsSync(outputDir)) {
      mkdirSync(outputDir, { recursive: true });
    }

    const supportsStrategy = await this.supportsStrategyFlag();

    // Stage 1: Extract routing graph with complete ways
    const tempRouting = join(outputDir, 'temp_routing.osm.pbf');
    let extractCmd: string;

    if (supportsStrategy) {
      extractCmd = `osmium extract -s complete_ways --overwrite "${inputPbf}" -p "${polyFile}" -o "${tempRouting}"`;
    } else {
      extractCmd = `osmium extract --complete-ways --complete-nodes --complete-relations --overwrite "${inputPbf}" -p "${polyFile}" -o "${tempRouting}"`;
    }

    if (this.verbose) {
      console.log('Stage 1: Extracting routing graph...');
    }
    await execAsync(extractCmd);

    // Stage 2: Extract all address nodes
    const tempAddresses = join(outputDir, 'temp_addresses.osm.pbf');
    const addressCmd = `osmium tags-filter --overwrite "${inputPbf}" n/addr:street n/addr:housenumber n/addr:city -o "${tempAddresses}"`;
    
    if (this.verbose) {
      console.log('Stage 2: Extracting address nodes...');
    }
    await execAsync(addressCmd);

    // Stage 3: Clip addresses to polygon
    const tempAddressesClipped = join(outputDir, 'temp_addresses_clipped.osm.pbf');
    const clipCmd = `osmium extract --overwrite "${tempAddresses}" -p "${polyFile}" -o "${tempAddressesClipped}"`;
    
    if (this.verbose) {
      console.log('Stage 3: Clipping addresses to polygon...');
    }
    await execAsync(clipCmd);

    // Stage 4: Merge routing + addresses
    if (this.verbose) {
      console.log('Stage 4: Merging routing and addresses...');
    }
    const mergeCmd = `osmium merge --overwrite "${tempRouting}" "${tempAddressesClipped}" -o "${outputPbf}"`;
    await execAsync(mergeCmd);

    // Cleanup temp files
    const { unlink } = await import('fs/promises');
    try {
      await unlink(tempRouting);
      await unlink(tempAddresses);
      await unlink(tempAddressesClipped);
    } catch (e) {
      // Ignore cleanup errors
    }
  }

  async getFileInfo(filePath: string): Promise<any> {
    try {
      const { stdout } = await execAsync(`osmium fileinfo "${filePath}"`);
      return { info: stdout };
    } catch (error: any) {
      return { error: error.message };
    }
  }
}
