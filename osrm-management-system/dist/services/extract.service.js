"use strict";
/**
 * Extract Service
 * Handles OSM data extraction
 * Tracks builds in database for sequential processing
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.ExtractService = void 0;
const osmium_wrapper_1 = require("../utils/osmium-wrapper");
const osm_parser_1 = require("../utils/osm-parser");
const path_1 = require("path");
const fs_1 = require("fs");
const logger_1 = require("../common/logger");
const build_tracker_service_1 = require("./build-tracker.service");
class ExtractService {
    rawDataPath;
    buildTracker;
    prisma;
    constructor(prisma) {
        this.prisma = prisma;
        this.rawDataPath = process.env.RAW_DATA_PATH || './raw_data';
        this.buildTracker = new build_tracker_service_1.BuildTrackerService(prisma);
    }
    /**
     * Extract complete data (sync-like: sequential processing)
     */
    async extractCompleteData(polyFile) {
        const instanceName = 'extract-complete';
        return this.buildTracker.executeSequentially(instanceName, async (buildId) => {
            return this._extractCompleteData(buildId, polyFile);
        });
    }
    async _extractCompleteData(buildId, polyFile) {
        const startTime = Date.now();
        try {
            logger_1.logger.info(`Starting complete OSM data extraction (Build ID: ${buildId})...`);
            // Setup paths
            const pbfPath = (0, osm_parser_1.findLatestVietnamPBF)(this.rawDataPath);
            const defaultPolyFile = (0, path_1.join)(this.rawDataPath, 'poly/hcmc.poly');
            const poly = polyFile || defaultPolyFile;
            const outputPbf = (0, path_1.join)(this.rawDataPath, 'extracted/hcmc.osm.pbf');
            if (!(0, fs_1.existsSync)(poly)) {
                throw new Error(`Poly file not found: ${poly}`);
            }
            logger_1.logger.info(`Source PBF: ${pbfPath}`);
            logger_1.logger.info(`Polygon: ${poly}`);
            logger_1.logger.info(`Output: ${outputPbf}`);
            // Initialize osmium wrapper
            const osmium = new osmium_wrapper_1.OsmiumWrapper(true);
            // Check installation
            const isInstalled = await osmium.checkInstallation();
            if (!isInstalled) {
                throw new Error('osmium-tool is not installed');
            }
            // Run extraction
            await osmium.extractRoutingWithAddresses(pbfPath, poly, outputPbf);
            const duration = Date.now() - startTime;
            // Mark build as READY
            await this.buildTracker.markReady(buildId, outputPbf);
            logger_1.logger.info(`Extraction completed in ${duration}ms`);
            return {
                success: true,
                outputPath: outputPbf,
                duration,
                buildId,
            };
        }
        catch (error) {
            logger_1.logger.error('Extraction failed', { error: error.message, buildId });
            await this.buildTracker.markFailed(buildId, error.message);
            return {
                success: false,
                error: error.message,
                duration: Date.now() - startTime,
                buildId,
            };
        }
    }
}
exports.ExtractService = ExtractService;
//# sourceMappingURL=extract.service.js.map