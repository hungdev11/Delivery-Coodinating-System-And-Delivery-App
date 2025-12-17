"use strict";
/**
 * Osmium Wrapper
 * Independent copy for OSRM Management Service
 *
 * Version Compatibility:
 * - Version >= 1.18: Uses modern command format
 * - Version < 1.18: Uses legacy command format (auto-detected)
 */
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.OsmiumWrapper = void 0;
const child_process_1 = require("child_process");
const util_1 = require("util");
const fs_1 = require("fs");
const path_1 = require("path");
const logger_1 = require("../common/logger");
const execAsync = (0, util_1.promisify)(child_process_1.exec);
class OsmiumWrapper {
    verbose;
    version = null;
    versionNumber = null;
    constructor(verbose = false) {
        this.verbose = verbose;
    }
    /**
     * Check if osmium-tool is installed and detect version
     */
    async checkInstallation() {
        try {
            const { stdout } = await execAsync('osmium --version');
            this.version = stdout.trim();
            logger_1.logger.info('Osmium version check', {
                rawOutput: this.version,
                stdoutLength: stdout.length
            });
            // Extract version number (e.g., "osmium version 1.18.0" -> 1.18)
            const versionMatch = this.version.match(/version\s+(\d+)\.(\d+)/);
            if (versionMatch && versionMatch[1] && versionMatch[2]) {
                const major = parseInt(versionMatch[1], 10);
                const minor = parseInt(versionMatch[2], 10);
                this.versionNumber = major + minor / 100; // e.g., 1.18 -> 1.18, 1.17 -> 1.17
                logger_1.logger.info('Osmium version detected', {
                    fullVersion: this.version,
                    major,
                    minor,
                    versionNumber: this.versionNumber,
                    isModern: this.versionNumber >= 1.18
                });
            }
            else {
                logger_1.logger.warn('Could not parse osmium version', {
                    rawOutput: this.version,
                    versionMatch: versionMatch
                });
            }
            if (this.verbose) {
                console.log(`✓ Osmium found: ${this.version} (parsed: ${this.versionNumber})`);
            }
            return true;
        }
        catch (error) {
            logger_1.logger.error('Osmium installation check failed', {
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
    async getVersionNumber() {
        // If already cached, return it
        if (this.versionNumber !== null) {
            logger_1.logger.debug('Using cached version number', { versionNumber: this.versionNumber });
            return this.versionNumber;
        }
        // Try to get version
        try {
            const { stdout } = await execAsync('osmium --version');
            logger_1.logger.debug('Fetching osmium version', { stdout: stdout.trim() });
            const versionMatch = stdout.match(/version\s+(\d+)\.(\d+)/);
            if (versionMatch && versionMatch[1] && versionMatch[2]) {
                const major = parseInt(versionMatch[1], 10);
                const minor = parseInt(versionMatch[2], 10);
                this.versionNumber = major + minor / 100;
                logger_1.logger.info('Osmium version parsed', {
                    major,
                    minor,
                    versionNumber: this.versionNumber,
                    rawMatch: versionMatch
                });
                return this.versionNumber;
            }
            else {
                logger_1.logger.warn('Could not parse version from output', {
                    stdout: stdout.trim(),
                    versionMatch
                });
            }
        }
        catch (error) {
            logger_1.logger.error('Failed to get osmium version', {
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
    async supportsStrategyFlag() {
        try {
            const { stdout } = await execAsync('osmium extract --help');
            const supports = stdout.includes('--strategy') || stdout.includes('-s [');
            logger_1.logger.debug('Strategy flag support check', { supports, stdoutPreview: stdout.substring(0, 200) });
            return supports;
        }
        catch (error) {
            logger_1.logger.warn('Failed to check strategy flag support', { error: error.message });
            return true; // Default to true (assume modern)
        }
    }
    async extractRoutingWithAddresses(inputPbf, polyFile, outputPbf) {
        logger_1.logger.info('Starting extractRoutingWithAddresses', {
            inputPbf,
            polyFile,
            outputPbf
        });
        if (!(0, fs_1.existsSync)(inputPbf)) {
            const error = `Input file not found: ${inputPbf}`;
            logger_1.logger.error(error);
            throw new Error(error);
        }
        if (!(0, fs_1.existsSync)(polyFile)) {
            const error = `Poly file not found: ${polyFile}`;
            logger_1.logger.error(error);
            throw new Error(error);
        }
        const outputDir = (0, path_1.dirname)(outputPbf);
        if (!(0, fs_1.existsSync)(outputDir)) {
            logger_1.logger.info('Creating output directory', { outputDir });
            (0, fs_1.mkdirSync)(outputDir, { recursive: true });
        }
        // Get version number for accurate command format detection
        const version = await this.getVersionNumber();
        const useModernFormat = version !== null && version >= 1.18;
        logger_1.logger.info('Osmium version detection for extract', {
            version,
            versionString: this.version,
            useModernFormat,
            threshold: 1.18
        });
        // Stage 1: Extract routing graph with complete ways
        const tempRouting = (0, path_1.join)(outputDir, 'temp_routing.osm.pbf');
        let extractCmd;
        if (useModernFormat) {
            // Modern osmium (>= 1.18): osmium extract --polygon poly -s complete_ways --overwrite input -o output
            extractCmd = `osmium extract --polygon "${polyFile}" -s complete_ways --overwrite "${inputPbf}" -o "${tempRouting}"`;
            logger_1.logger.info('Using modern osmium format (>= 1.18)', { extractCmd });
        }
        else {
            // Legacy osmium (< 1.18): Use -s complete_ways (works for 1.7.1+)
            // Version 1.7.1 doesn't support --complete-ways flags, but supports -s complete_ways
            extractCmd = `osmium extract -p "${polyFile}" -s complete_ways -O -o "${tempRouting}" "${inputPbf}"`;
            logger_1.logger.info('Using legacy osmium format (< 1.18) with -s complete_ways', { extractCmd });
        }
        if (this.verbose) {
            console.log('Stage 1: Extracting routing graph...');
            console.log(`Command: ${extractCmd}`);
        }
        logger_1.logger.info('Executing Stage 1: Extract routing graph', { extractCmd });
        try {
            await execAsync(extractCmd);
            logger_1.logger.info('Stage 1 completed successfully');
        }
        catch (error) {
            logger_1.logger.error('Stage 1 failed', {
                error: error.message,
                stderr: error.stderr,
                stdout: error.stdout,
                extractCmd
            });
            throw error;
        }
        // Stage 2: Extract all address nodes
        const tempAddresses = (0, path_1.join)(outputDir, 'temp_addresses.osm.pbf');
        const addressCmd = `osmium tags-filter --overwrite "${inputPbf}" n/addr:street n/addr:housenumber n/addr:city -o "${tempAddresses}"`;
        logger_1.logger.info('Executing Stage 2: Extract address nodes', { addressCmd });
        if (this.verbose) {
            console.log('Stage 2: Extracting address nodes...');
            console.log(`Command: ${addressCmd}`);
        }
        try {
            await execAsync(addressCmd);
            logger_1.logger.info('Stage 2 completed successfully');
        }
        catch (error) {
            logger_1.logger.error('Stage 2 failed', {
                error: error.message,
                stderr: error.stderr,
                stdout: error.stdout,
                addressCmd
            });
            throw error;
        }
        // Stage 3: Clip addresses to polygon
        const tempAddressesClipped = (0, path_1.join)(outputDir, 'temp_addresses_clipped.osm.pbf');
        let clipCmd;
        if (useModernFormat) {
            // Modern format: osmium extract --polygon poly --overwrite input -o output
            clipCmd = `osmium extract --polygon "${polyFile}" --overwrite "${tempAddresses}" -o "${tempAddressesClipped}"`;
        }
        else {
            // Legacy format: osmium extract -p poly --overwrite -o output input
            clipCmd = `osmium extract -p "${polyFile}" --overwrite -o "${tempAddressesClipped}" "${tempAddresses}"`;
        }
        logger_1.logger.info('Executing Stage 3: Clip addresses to polygon', {
            clipCmd,
            useModernFormat
        });
        if (this.verbose) {
            console.log('Stage 3: Clipping addresses to polygon...');
            console.log(`Command: ${clipCmd}`);
        }
        try {
            await execAsync(clipCmd);
            logger_1.logger.info('Stage 3 completed successfully');
        }
        catch (error) {
            logger_1.logger.error('Stage 3 failed', {
                error: error.message,
                stderr: error.stderr,
                stdout: error.stdout,
                clipCmd
            });
            throw error;
        }
        // Stage 4: Merge routing + addresses
        const mergeCmd = `osmium merge --overwrite "${tempRouting}" "${tempAddressesClipped}" -o "${outputPbf}"`;
        logger_1.logger.info('Executing Stage 4: Merge routing and addresses', { mergeCmd });
        if (this.verbose) {
            console.log('Stage 4: Merging routing and addresses...');
            console.log(`Command: ${mergeCmd}`);
        }
        try {
            await execAsync(mergeCmd);
            logger_1.logger.info('Stage 4 completed successfully');
            logger_1.logger.info('extractRoutingWithAddresses completed', { outputPbf });
        }
        catch (error) {
            logger_1.logger.error('Stage 4 failed', {
                error: error.message,
                stderr: error.stderr,
                stdout: error.stdout,
                mergeCmd
            });
            throw error;
        }
        // Cleanup temp files
        const { unlink } = await Promise.resolve().then(() => __importStar(require('fs/promises')));
        try {
            await unlink(tempRouting);
            await unlink(tempAddresses);
            await unlink(tempAddressesClipped);
        }
        catch (e) {
            // Ignore cleanup errors
        }
    }
    async getFileInfo(filePath) {
        try {
            const { stdout } = await execAsync(`osmium fileinfo "${filePath}"`);
            return { info: stdout };
        }
        catch (error) {
            return { error: error.message };
        }
    }
}
exports.OsmiumWrapper = OsmiumWrapper;
//# sourceMappingURL=osmium-wrapper.js.map