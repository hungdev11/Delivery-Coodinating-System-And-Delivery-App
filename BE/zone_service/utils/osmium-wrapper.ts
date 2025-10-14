/**
 * Wrapper for osmium-tool command-line utility
 * Provides a clean interface to extract OSM data from PBF files
 *
 * Prerequisites:
 * - osmium-tool must be installed on the system
 *   Ubuntu/Debian: sudo apt-get install osmium-tool
 *   macOS: brew install osmium-tool
 */

import { exec, spawn } from 'child_process';
import { promisify } from 'util';
import { existsSync, mkdirSync, unlinkSync } from 'fs';
import { join, dirname } from 'path';
import { readFile } from 'fs/promises';

const execAsync = promisify(exec);

export interface OsmiumOptions {
  bbox?: [number, number, number, number]; // [minLon, minLat, maxLon, maxLat]
  outputFormat?: 'json' | 'xml' | 'pbf';
  verbose?: boolean;
}

export class OsmiumWrapper {
  private verbose: boolean;

  constructor(verbose: boolean = false) {
    this.verbose = verbose;
  }

  /**
   * Check if osmium-tool is installed
   */
  async checkInstallation(): Promise<boolean> {
    try {
      const { stdout } = await execAsync('osmium --version');
      if (this.verbose) {
        console.log(`✓ Osmium found: ${stdout.trim()}`);
      }
      return true;
    } catch (error) {
      console.error('❌ osmium-tool is not installed!');
      console.error('\nPlease install osmium-tool:');
      console.error('  Ubuntu/Debian: sudo apt-get install osmium-tool');
      console.error('  macOS: brew install osmium-tool');
      return false;
    }
  }

  /**
   * Extract specific data types from PBF file
   * @param inputPbf - Path to input PBF file
   * @param outputFile - Path to output file (will be created in JSON format)
   * @param options - Additional options
   */
  async extractToJson(
    inputPbf: string,
    outputFile: string,
    options: OsmiumOptions = {}
  ): Promise<void> {
    if (!existsSync(inputPbf)) {
      throw new Error(`Input file not found: ${inputPbf}`);
    }

    // Ensure output directory exists
    const outputDir = dirname(outputFile);
    if (!existsSync(outputDir)) {
      mkdirSync(outputDir, { recursive: true });
    }

    // Build osmium command
    const args: string[] = ['export', inputPbf];

    // Add bounding box filter if provided
    if (options.bbox) {
      const [minLon, minLat, maxLon, maxLat] = options.bbox;
      args.push('--bbox', `${minLon},${minLat},${maxLon},${maxLat}`);
    }

    // Output format (GeoJSON)
    args.push('--output', outputFile);
    args.push('--output-format', 'geojson');
    args.push('--add-unique-id', 'counter');

    if (this.verbose) {
      console.log(`Running: osmium ${args.join(' ')}`);
    }

    return new Promise((resolve, reject) => {
      const osmium = spawn('osmium', args);

      let stdout = '';
      let stderr = '';

      osmium.stdout.on('data', (data) => {
        stdout += data.toString();
        if (this.verbose) {
          process.stdout.write(data);
        }
      });

      osmium.stderr.on('data', (data) => {
        stderr += data.toString();
        if (this.verbose) {
          process.stderr.write(data);
        }
      });

      osmium.on('close', (code) => {
        if (code === 0) {
          resolve();
        } else {
          reject(new Error(`Osmium exited with code ${code}\n${stderr}`));
        }
      });

      osmium.on('error', (error) => {
        reject(new Error(`Failed to start osmium: ${error.message}`));
      });
    });
  }

  /**
   * Extract data within a polygon boundary
   * @param inputPbf - Path to input PBF file
   * @param outputPbf - Path to output PBF file
   * @param polyFile - Path to .poly file defining the boundary
   */
  async extractByPoly(
    inputPbf: string,
    outputPbf: string,
    polyFile: string
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

    // Use osmium extract with polygon
    const args: string[] = [
      'extract',
      '--polygon', polyFile,
      inputPbf,
      '--output', outputPbf,
      '--overwrite'
    ];

    if (this.verbose) {
      console.log(`Extracting by polygon: osmium ${args.join(' ')}`);
    }

    return new Promise((resolve, reject) => {
      const osmium = spawn('osmium', args);

      let stderr = '';

      osmium.stdout.on('data', (data) => {
        if (this.verbose) {
          process.stdout.write(data);
        }
      });

      osmium.stderr.on('data', (data) => {
        stderr += data.toString();
        if (this.verbose) {
          process.stderr.write(data);
        }
      });

      osmium.on('close', (code) => {
        if (code === 0) {
          resolve();
        } else {
          reject(new Error(`Osmium extract exited with code ${code}\n${stderr}`));
        }
      });

      osmium.on('error', (error) => {
        reject(new Error(`Failed to start osmium: ${error.message}`));
      });
    });
  }

  /**
   * Extract only road ways from PBF file
   * This uses osmium tags-filter to extract only highway=* ways
   */
  async extractRoads(
    inputPbf: string,
    outputPbf: string,
    options: OsmiumOptions = {}
  ): Promise<void> {
    if (!existsSync(inputPbf)) {
      throw new Error(`Input file not found: ${inputPbf}`);
    }

    // Ensure output directory exists
    const outputDir = dirname(outputPbf);
    if (!existsSync(outputDir)) {
      mkdirSync(outputDir, { recursive: true });
    }

    // Build osmium command to filter roads
    const args: string[] = [
      'tags-filter',
      inputPbf,
      'w/highway=motorway,trunk,primary,secondary,tertiary,unclassified,residential,service,motorway_link,trunk_link,primary_link,secondary_link,tertiary_link,living_street,pedestrian,track,road',
      '--output', outputPbf,
      '--overwrite'
    ];

    if (this.verbose) {
      console.log(`Filtering roads: osmium ${args.join(' ')}`);
    }

    return new Promise((resolve, reject) => {
      const osmium = spawn('osmium', args);

      let stderr = '';

      osmium.stdout.on('data', (data) => {
        if (this.verbose) {
          process.stdout.write(data);
        }
      });

      osmium.stderr.on('data', (data) => {
        stderr += data.toString();
        if (this.verbose) {
          process.stderr.write(data);
        }
      });

      osmium.on('close', (code) => {
        if (code === 0) {
          resolve();
        } else {
          reject(new Error(`Osmium tags-filter exited with code ${code}\n${stderr}`));
        }
      });

      osmium.on('error', (error) => {
        reject(new Error(`Failed to start osmium: ${error.message}`));
      });
    });
  }

  /**
   * Convert PBF to OSM XML format (useful for debugging)
   */
  async convertToXml(
    inputPbf: string,
    outputXml: string
  ): Promise<void> {
    if (!existsSync(inputPbf)) {
      throw new Error(`Input file not found: ${inputPbf}`);
    }

    const outputDir = dirname(outputXml);
    if (!existsSync(outputDir)) {
      mkdirSync(outputDir, { recursive: true });
    }

    const args: string[] = [
      'cat',
      inputPbf,
      '--output', outputXml,
      '--output-format', 'xml'
    ];

    if (this.verbose) {
      console.log(`Converting to XML: osmium ${args.join(' ')}`);
    }

    return new Promise((resolve, reject) => {
      const osmium = spawn('osmium', args);

      let stderr = '';

      osmium.stderr.on('data', (data) => {
        stderr += data.toString();
      });

      osmium.on('close', (code) => {
        if (code === 0) {
          resolve();
        } else {
          reject(new Error(`Osmium cat exited with code ${code}\n${stderr}`));
        }
      });

      osmium.on('error', (error) => {
        reject(new Error(`Failed to start osmium: ${error.message}`));
      });
    });
  }

  /**
   * Get file info using osmium fileinfo
   */
  async getFileInfo(pbfPath: string): Promise<any> {
    if (!existsSync(pbfPath)) {
      throw new Error(`Input file not found: ${pbfPath}`);
    }

    try {
      const { stdout } = await execAsync(`osmium fileinfo ${pbfPath} --json`);
      return JSON.parse(stdout);
    } catch (error: any) {
      throw new Error(`Failed to get file info: ${error.message}`);
    }
  }

  /**
   * Extract nodes and ways separately using osmium getid
   * This is more efficient for large files
   */
  async extractWaysWithNodes(
    inputPbf: string,
    outputPbf: string,
    wayIds: string[]
  ): Promise<void> {
    if (!existsSync(inputPbf)) {
      throw new Error(`Input file not found: ${inputPbf}`);
    }

    const outputDir = dirname(outputPbf);
    if (!existsSync(outputDir)) {
      mkdirSync(outputDir, { recursive: true });
    }

    // Create a temporary file with way IDs
    const tempIdFile = join(outputDir, 'temp_way_ids.txt');
    const { writeFile } = await import('fs/promises');
    await writeFile(tempIdFile, wayIds.map(id => `w${id}`).join('\n'));

    try {
      const args: string[] = [
        'getid',
        '--id-file', tempIdFile,
        '--add-referenced',
        inputPbf,
        '--output', outputPbf,
        '--overwrite'
      ];

      if (this.verbose) {
        console.log(`Extracting ways with nodes: osmium ${args.join(' ')}`);
      }

      await new Promise<void>((resolve, reject) => {
        const osmium = spawn('osmium', args);

        let stderr = '';

        osmium.stderr.on('data', (data) => {
          stderr += data.toString();
        });

        osmium.on('close', (code) => {
          if (code === 0) {
            resolve();
          } else {
            reject(new Error(`Osmium getid exited with code ${code}\n${stderr}`));
          }
        });

        osmium.on('error', (error) => {
          reject(new Error(`Failed to start osmium: ${error.message}`));
        });
      });
    } finally {
      // Clean up temp file
      if (existsSync(tempIdFile)) {
        unlinkSync(tempIdFile);
      }
    }
  }
}

/**
 * Parse GeoJSON output from osmium export
 */
export async function parseOsmiumGeoJson(filePath: string): Promise<any> {
  if (!existsSync(filePath)) {
    throw new Error(`File not found: ${filePath}`);
  }

  const content = await readFile(filePath, 'utf-8');
  return JSON.parse(content);
}
