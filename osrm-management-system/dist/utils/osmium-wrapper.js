"use strict";
/**
 * Osmium Wrapper
 * Independent copy for OSRM Management Service
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
const execAsync = (0, util_1.promisify)(child_process_1.exec);
class OsmiumWrapper {
    verbose;
    constructor(verbose = false) {
        this.verbose = verbose;
    }
    async checkInstallation() {
        try {
            await execAsync('osmium --version');
            if (this.verbose) {
                console.log('✓ Osmium found');
            }
            return true;
        }
        catch (error) {
            console.error('❌ osmium-tool is not installed!');
            return false;
        }
    }
    async supportsStrategyFlag() {
        try {
            const { stdout } = await execAsync('osmium extract --help');
            return stdout.includes('--strategy') || stdout.includes('-s [');
        }
        catch {
            return true;
        }
    }
    async extractRoutingWithAddresses(inputPbf, polyFile, outputPbf) {
        if (!(0, fs_1.existsSync)(inputPbf)) {
            throw new Error(`Input file not found: ${inputPbf}`);
        }
        if (!(0, fs_1.existsSync)(polyFile)) {
            throw new Error(`Poly file not found: ${polyFile}`);
        }
        const outputDir = (0, path_1.dirname)(outputPbf);
        if (!(0, fs_1.existsSync)(outputDir)) {
            (0, fs_1.mkdirSync)(outputDir, { recursive: true });
        }
        const supportsStrategy = await this.supportsStrategyFlag();
        // Stage 1: Extract routing graph with complete ways
        const tempRouting = (0, path_1.join)(outputDir, 'temp_routing.osm.pbf');
        let extractCmd;
        if (supportsStrategy) {
            extractCmd = `osmium extract -s complete_ways --overwrite "${inputPbf}" -p "${polyFile}" -o "${tempRouting}"`;
        }
        else {
            extractCmd = `osmium extract --complete-ways --complete-nodes --complete-relations --overwrite "${inputPbf}" -p "${polyFile}" -o "${tempRouting}"`;
        }
        if (this.verbose) {
            console.log('Stage 1: Extracting routing graph...');
        }
        await execAsync(extractCmd);
        // Stage 2: Extract all address nodes
        const tempAddresses = (0, path_1.join)(outputDir, 'temp_addresses.osm.pbf');
        const addressCmd = `osmium tags-filter --overwrite "${inputPbf}" n/addr:street n/addr:housenumber n/addr:city -o "${tempAddresses}"`;
        if (this.verbose) {
            console.log('Stage 2: Extracting address nodes...');
        }
        await execAsync(addressCmd);
        // Stage 3: Clip addresses to polygon
        const tempAddressesClipped = (0, path_1.join)(outputDir, 'temp_addresses_clipped.osm.pbf');
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