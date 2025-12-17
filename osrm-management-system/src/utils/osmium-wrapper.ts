/**
 * Osmium Wrapper
 * Independent copy for OSRM Management Service
 * 
 * Version Compatibility:
 * - Version >= 1.18: Uses modern command format
 * - Version < 1.18: Uses legacy command format (auto-detected)
 */

import { exec } from 'child_process';
import { promisify } from 'util';
import { existsSync, mkdirSync } from 'fs';
import { join, dirname } from 'path';
import { logger } from '../common/logger';

const execAsync = promisify(exec);

export class OsmiumWrapper {
  private verbose: boolean;
  private version: string | null = null;
  private versionNumber: number | null = null;

  constructor(verbose: boolean = false) {
    this.verbose = verbose;
  }

  /**
   * Check if osmium-tool is installed and detect version
   */
  async checkInstallation(): Promise<boolean> {
    try {
      const { stdout } = await execAsync('osmium --version');
      this.version = stdout.trim();
      
      logger.info('Osmium version check', { 
        rawOutput: this.version,
        stdoutLength: stdout.length 
      });
      
      // Extract version number (e.g., "osmium version 1.18.0" -> 1.18)
      const versionMatch = this.version.match(/version\s+(\d+)\.(\d+)/);
      if (versionMatch && versionMatch[1] && versionMatch[2]) {
        const major = parseInt(versionMatch[1], 10);
        const minor = parseInt(versionMatch[2], 10);
        this.versionNumber = major + minor / 100; // e.g., 1.18 -> 1.18, 1.17 -> 1.17
        
        logger.info('Osmium version detected', {
          fullVersion: this.version,
          major,
          minor,
          versionNumber: this.versionNumber,
          isModern: this.versionNumber >= 1.18
        });
      } else {
        logger.warn('Could not parse osmium version', {
          rawOutput: this.version,
          versionMatch: versionMatch
        });
      }
      
      if (this.verbose) {
        console.log(`✓ Osmium found: ${this.version} (parsed: ${this.versionNumber})`);
      }
      return true;
    } catch (error: any) {
      logger.error('Osmium installation check failed', {
        error: error.message,
        stderr: error.stderr,
        stdout: error.stdout
      });
      console.error('❌ osmium-tool is not installed!');
      return false;
    }
  }

  /**
   * Get osmium version number (e.g., 1.18, 1.17)
   * Returns null if version cannot be determined
   */
  private async getVersionNumber(): Promise<number | null> {
    // If already cached, return it
    if (this.versionNumber !== null) {
      logger.debug('Using cached version number', { versionNumber: this.versionNumber });
      return this.versionNumber;
    }

    // Try to get version
    try {
      const { stdout } = await execAsync('osmium --version');
      logger.debug('Fetching osmium version', { stdout: stdout.trim() });
      
      const versionMatch = stdout.match(/version\s+(\d+)\.(\d+)/);
      if (versionMatch && versionMatch[1] && versionMatch[2]) {
        const major = parseInt(versionMatch[1], 10);
        const minor = parseInt(versionMatch[2], 10);
        this.versionNumber = major + minor / 100;
        
        logger.info('Osmium version parsed', {
          major,
          minor,
          versionNumber: this.versionNumber,
          rawMatch: versionMatch
        });
        
        return this.versionNumber;
      } else {
        logger.warn('Could not parse version from output', {
          stdout: stdout.trim(),
          versionMatch
        });
      }
    } catch (error: any) {
      logger.error('Failed to get osmium version', {
        error: error.message,
        stderr: error.stderr
      });
    }

    return null;
  }

  /**
   * Check if osmium supports --strategy flag (legacy method, kept for compatibility)
   * @deprecated Use getVersionNumber() instead
   */
  private async supportsStrategyFlag(): Promise<boolean> {
    try {
      const { stdout } = await execAsync('osmium extract --help');
      const supports = stdout.includes('--strategy') || stdout.includes('-s [');
      logger.debug('Strategy flag support check', { supports, stdoutPreview: stdout.substring(0, 200) });
      return supports;
    } catch (error: any) {
      logger.warn('Failed to check strategy flag support', { error: error.message });
      return true; // Default to true (assume modern)
    }
  }

  async extractRoutingWithAddresses(
    inputPbf: string,
    polyFile: string,
    outputPbf: string
  ): Promise<void> {
    logger.info('Starting extractRoutingWithAddresses', {
      inputPbf,
      polyFile,
      outputPbf
    });

    if (!existsSync(inputPbf)) {
      const error = `Input file not found: ${inputPbf}`;
      logger.error(error);
      throw new Error(error);
    }
    if (!existsSync(polyFile)) {
      const error = `Poly file not found: ${polyFile}`;
      logger.error(error);
      throw new Error(error);
    }

    const outputDir = dirname(outputPbf);
    if (!existsSync(outputDir)) {
      logger.info('Creating output directory', { outputDir });
      mkdirSync(outputDir, { recursive: true });
    }

    // Get version number for accurate command format detection
    const version = await this.getVersionNumber();
    const useModernFormat = version !== null && version >= 1.18;

    logger.info('Osmium version detection for extract', {
      version,
      versionString: this.version,
      useModernFormat,
      threshold: 1.18
    });

    // Stage 1: Extract routing graph with complete ways
    const tempRouting = join(outputDir, 'temp_routing.osm.pbf');
    let extractCmd: string;

    if (useModernFormat) {
      // Modern osmium (>= 1.18): osmium extract --polygon poly -s complete_ways --overwrite input -o output
      extractCmd = `osmium extract --polygon "${polyFile}" -s complete_ways --overwrite "${inputPbf}" -o "${tempRouting}"`;
      logger.info('Using modern osmium format (>= 1.18)', { extractCmd });
    } else {
      // Legacy osmium (< 1.18): Use -s complete_ways (works for 1.7.1+)
      // Version 1.7.1 doesn't support --complete-ways flags, but supports -s complete_ways
      extractCmd = `osmium extract -p "${polyFile}" -s complete_ways -O -o "${tempRouting}" "${inputPbf}"`;
      logger.info('Using legacy osmium format (< 1.18) with -s complete_ways', { extractCmd });
    }

    if (this.verbose) {
      console.log('Stage 1: Extracting routing graph...');
      console.log(`Command: ${extractCmd}`);
    }
    
    logger.info('Executing Stage 1: Extract routing graph', { extractCmd });
    try {
      await execAsync(extractCmd);
      logger.info('Stage 1 completed successfully');
    } catch (error: any) {
      logger.error('Stage 1 failed', {
        error: error.message,
        stderr: error.stderr,
        stdout: error.stdout,
        extractCmd
      });
      throw error;
    }

    // Stage 2: Extract all address nodes
    const tempAddresses = join(outputDir, 'temp_addresses.osm.pbf');
    const addressCmd = `osmium tags-filter --overwrite "${inputPbf}" n/addr:street n/addr:housenumber n/addr:city -o "${tempAddresses}"`;
    
    logger.info('Executing Stage 2: Extract address nodes', { addressCmd });
    if (this.verbose) {
      console.log('Stage 2: Extracting address nodes...');
      console.log(`Command: ${addressCmd}`);
    }
    
    try {
      await execAsync(addressCmd);
      logger.info('Stage 2 completed successfully');
    } catch (error: any) {
      logger.error('Stage 2 failed', {
        error: error.message,
        stderr: error.stderr,
        stdout: error.stdout,
        addressCmd
      });
      throw error;
    }

    // Stage 3: Clip addresses to polygon
    const tempAddressesClipped = join(outputDir, 'temp_addresses_clipped.osm.pbf');
    let clipCmd: string;
    
    if (useModernFormat) {
      // Modern format: osmium extract --polygon poly --overwrite input -o output
      clipCmd = `osmium extract --polygon "${polyFile}" --overwrite "${tempAddresses}" -o "${tempAddressesClipped}"`;
    } else {
      // Legacy format: osmium extract -p poly --overwrite -o output input
      clipCmd = `osmium extract -p "${polyFile}" --overwrite -o "${tempAddressesClipped}" "${tempAddresses}"`;
    }
    
    logger.info('Executing Stage 3: Clip addresses to polygon', { 
      clipCmd,
      useModernFormat 
    });
    if (this.verbose) {
      console.log('Stage 3: Clipping addresses to polygon...');
      console.log(`Command: ${clipCmd}`);
    }
    
    try {
      await execAsync(clipCmd);
      logger.info('Stage 3 completed successfully');
    } catch (error: any) {
      logger.error('Stage 3 failed', {
        error: error.message,
        stderr: error.stderr,
        stdout: error.stdout,
        clipCmd
      });
      throw error;
    }

    // Stage 4: Merge routing + addresses
    const mergeCmd = `osmium merge --overwrite "${tempRouting}" "${tempAddressesClipped}" -o "${outputPbf}"`;
    
    logger.info('Executing Stage 4: Merge routing and addresses', { mergeCmd });
    if (this.verbose) {
      console.log('Stage 4: Merging routing and addresses...');
      console.log(`Command: ${mergeCmd}`);
    }
    
    try {
      await execAsync(mergeCmd);
      logger.info('Stage 4 completed successfully');
      logger.info('extractRoutingWithAddresses completed', { outputPbf });
    } catch (error: any) {
      logger.error('Stage 4 failed', {
        error: error.message,
        stderr: error.stderr,
        stdout: error.stdout,
        mergeCmd
      });
      throw error;
    }

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
