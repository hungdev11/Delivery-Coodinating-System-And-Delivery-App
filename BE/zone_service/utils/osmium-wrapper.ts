/**
 * Wrapper for osmium-tool command-line utility
 * Provides a clean interface to extract OSM data from PBF files
 *
 * Prerequisites:
 * - osmium-tool must be installed on the system
 *   Ubuntu/Debian: sudo apt-get install osmium-tool
 *   Ubuntu 22.04 (manual): See .docs/SETUP.md for manual installation steps
 *   macOS: brew install osmium-tool
 * 
 * Version Compatibility:
 * - Version >= 1.18: Uses modern command format
 * - Version < 1.18: Uses legacy command format (auto-detected)
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
			
			// Extract version number (e.g., "osmium version 1.18.0" -> 1.18)
			const versionMatch = this.version.match(/version\s+(\d+)\.(\d+)/);
			if (versionMatch && versionMatch[1] && versionMatch[2]) {
				const major = parseInt(versionMatch[1], 10);
				const minor = parseInt(versionMatch[2], 10);
				this.versionNumber = major + minor / 100; // e.g., 1.18 -> 1.18, 1.17 -> 1.17
			}
			
			if (this.verbose) {
				console.log(`✓ Osmium found: ${this.version} (parsed: ${this.versionNumber})`);
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
	 * Get osmium version number (e.g., 1.18, 1.17)
	 * Returns null if version cannot be determined
	 */
	private async getVersionNumber(): Promise<number | null> {
		// If already cached, return it
		if (this.versionNumber !== null) {
			return this.versionNumber;
		}

		// Try to get version
		try {
			const { stdout } = await execAsync('osmium --version');
			const versionMatch = stdout.match(/version\s+(\d+)\.(\d+)/);
			if (versionMatch && versionMatch[1] && versionMatch[2]) {
				const major = parseInt(versionMatch[1], 10);
				const minor = parseInt(versionMatch[2], 10);
				this.versionNumber = major + minor / 100;
				return this.versionNumber;
			}
		} catch {
			// Ignore errors
		}

		return null;
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
					reject(
						new Error(`Osmium exited with code ${code}\n${stderr}`)
					);
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

		// Auto-detect osmium version and use appropriate flags
		const version = await this.getVersionNumber();
		const useModernFormat = version !== null && version >= 1.18;

		const args: string[] = ['extract'];

		if (useModernFormat) {
			// Modern osmium (>= 1.18): osmium extract -s complete_ways --overwrite input -o output --polygon poly
			args.push('-s', 'complete_ways', '--overwrite', inputPbf, '-o', outputPbf, '--polygon', polyFile);
		} else {
			// Legacy osmium (< 1.18): osmium extract -p poly -s complete_ways -O -o output input
			args.push('-p', polyFile, '-s', 'complete_ways', '-O', '-o', outputPbf, inputPbf);
		}

		if (this.verbose) {
			const format = useModernFormat
				? 'modern format (>= 1.18)'
				: 'legacy format (< 1.18)';
			console.log(
				`Extracting polygon (${format}): osmium ${args.join(' ')}`
			);
		}

		return new Promise((resolve, reject) => {
			const osmium = spawn('osmium', args);

			let stderr = '';

			osmium.stdout.on(
				'data',
				(d) => this.verbose && process.stdout.write(d)
			);
			osmium.stderr.on('data', (d) => {
				stderr += d.toString();
				if (this.verbose) process.stderr.write(d);
			});

			osmium.on('close', (code) => {
				code === 0
					? resolve()
					: reject(
							new Error(
								`Osmium extract exited with code ${code}\n${stderr}`
							)
					  );
			});

			osmium.on('error', (err) => {
				reject(new Error(`Failed to start osmium: ${err.message}`));
			});
		});
	}

	/**
	 * Extract all address nodes from PBF file
	 * Captures addr:* tags including floating nodes outside road network
	 */
	async extractAddressNodes(
		inputPbf: string,
		outputPbf: string
	): Promise<void> {
		if (!existsSync(inputPbf)) {
			throw new Error(`Input file not found: ${inputPbf}`);
		}

		const outputDir = dirname(outputPbf);
		if (!existsSync(outputDir)) {
			mkdirSync(outputDir, { recursive: true });
		}

		const args: string[] = [
			'tags-filter',
			inputPbf,
			'n/addr:housenumber',
			'n/addr:street',
			'n/addr:city',
			'n/addr:district',
			'n/addr:suburb',
			'n/addr:neighbourhood',
			'--overwrite',
			'--output',
			outputPbf,
		];

		if (this.verbose) {
			console.log(`Extracting address nodes: osmium ${args.join(' ')}`);
		}

		return new Promise((resolve, reject) => {
			const osmium = spawn('osmium', args);

			let stderr = '';

			osmium.stdout.on('data', (d) => {
				if (this.verbose) process.stdout.write(d);
			});

			osmium.stderr.on('data', (d) => {
				stderr += d.toString();
				if (this.verbose) process.stderr.write(d);
			});

			osmium.on('close', (code) => {
				code === 0
					? resolve()
					: reject(
							new Error(
								`Osmium tags-filter exited with code ${code}\n${stderr}`
							)
					  );
			});

			osmium.on('error', (err) => {
				reject(new Error(`Failed to start osmium: ${err.message}`));
			});
		});
	}

	/**
	 * Merge multiple PBF files into one
	 * Useful for combining routing data + address data
	 */
	async mergePBFs(
		inputPbfs: string[],
		outputPbf: string
	): Promise<void> {
		for (const input of inputPbfs) {
			if (!existsSync(input)) {
				throw new Error(`Input file not found: ${input}`);
			}
		}

		const outputDir = dirname(outputPbf);
		if (!existsSync(outputDir)) {
			mkdirSync(outputDir, { recursive: true });
		}

		const args: string[] = [
			'merge',
			...inputPbfs,
			'--overwrite',
			'--output',
			outputPbf,
		];

		if (this.verbose) {
			console.log(`Merging PBF files: osmium ${args.join(' ')}`);
		}

		return new Promise((resolve, reject) => {
			const osmium = spawn('osmium', args);

			let stderr = '';

			osmium.stdout.on('data', (d) => {
				if (this.verbose) process.stdout.write(d);
			});

			osmium.stderr.on('data', (d) => {
				stderr += d.toString();
				if (this.verbose) process.stderr.write(d);
			});

			osmium.on('close', (code) => {
				code === 0
					? resolve()
					: reject(
							new Error(
								`Osmium merge exited with code ${code}\n${stderr}`
							)
					  );
			});

			osmium.on('error', (err) => {
				reject(new Error(`Failed to start osmium: ${err.message}`));
			});
		});
	}

	/**
	 * Two-stage extract: routing graph + complete address coverage
	 * 
	 * Strategy:
	 * 1. Extract polygon with complete ways/nodes/relations (for routing)
	 * 2. Extract all address nodes from source (captures floaters)
	 * 3. Clip addresses to polygon
	 * 4. Merge routing + addresses
	 * 
	 * This ensures:
	 * - Clean routing graph (no broken ways)
	 * - Complete address coverage (no missing house numbers)
	 * - Optimal for both OSRM + geocoding
	 */
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

		// Temp files
		const tempDir = join(outputDir, 'temp_extract');
		if (!existsSync(tempDir)) {
			mkdirSync(tempDir, { recursive: true });
		}

		const routingPbf = join(tempDir, 'routing.osm.pbf');
		const allAddrPbf = join(tempDir, 'all_addresses.osm.pbf');
		const clippedAddrPbf = join(tempDir, 'addresses_clipped.osm.pbf');

		try {
			// Step 1: Extract routing graph with complete ways
			if (this.verbose) {
				console.log(
					'\n[1/4] Extracting routing graph with complete ways...'
				);
			}
			await this.extractByPoly(inputPbf, routingPbf, polyFile);

			// Step 2: Extract all address nodes from source
			if (this.verbose) {
				console.log('\n[2/4] Extracting all address nodes...');
			}
			await this.extractAddressNodes(inputPbf, allAddrPbf);

			// Step 3: Clip addresses to polygon
			if (this.verbose) {
				console.log(
					'\n[3/4] Clipping addresses to polygon boundary...'
				);
			}
			await this.extractByPoly(allAddrPbf, clippedAddrPbf, polyFile);

			// Step 4: Merge routing + addresses
			if (this.verbose) {
				console.log('\n[4/4] Merging routing graph + addresses...');
			}
			await this.mergePBFs([routingPbf, clippedAddrPbf], outputPbf);

			if (this.verbose) {
				console.log(
					`\n✓ Complete extract created: ${outputPbf}`
				);
			}
		} finally {
			// Clean up temp files
			if (existsSync(routingPbf)) unlinkSync(routingPbf);
			if (existsSync(allAddrPbf)) unlinkSync(allAddrPbf);
			if (existsSync(clippedAddrPbf)) unlinkSync(clippedAddrPbf);
			if (existsSync(tempDir)) {
				try {
					const { rmdirSync } = await import('fs');
					rmdirSync(tempDir);
				} catch {
					// Ignore cleanup errors
				}
			}
		}
	}

	/**
	 * Extract only road ways from PBF file
	 * This uses osmium tags-filter to extract only highway=* ways
	 */
	async extractRoads(
		inputPbf: string,
		outputPbf: string,
		_options: OsmiumOptions = {}
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
			'--output',
			outputPbf,
			'--overwrite',
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
					reject(
						new Error(
							`Osmium tags-filter exited with code ${code}\n${stderr}`
						)
					);
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
	async convertToXml(inputPbf: string, outputXml: string): Promise<void> {
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
			'--output',
			outputXml,
			'--output-format',
			'xml',
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
					reject(
						new Error(
							`Osmium cat exited with code ${code}\n${stderr}`
						)
					);
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
			const { stdout } = await execAsync(
				`osmium fileinfo ${pbfPath} --json`
			);
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
		await writeFile(tempIdFile, wayIds.map((id) => `w${id}`).join('\n'));

		try {
			const args: string[] = [
				'getid',
				'--id-file',
				tempIdFile,
				'--add-referenced',
				inputPbf,
				'--output',
				outputPbf,
				'--overwrite',
			];

			if (this.verbose) {
				console.log(
					`Extracting ways with nodes: osmium ${args.join(' ')}`
				);
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
						reject(
							new Error(
								`Osmium getid exited with code ${code}\n${stderr}`
							)
						);
					}
				});

				osmium.on('error', (error) => {
					reject(
						new Error(`Failed to start osmium: ${error.message}`)
					);
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
