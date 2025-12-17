"use strict";
/**
 * OSM Parser Utilities
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.findLatestVietnamPBF = findLatestVietnamPBF;
const fs_1 = require("fs");
const path_1 = require("path");
function findLatestVietnamPBF(rawDataDir) {
    const vietnamDir = (0, path_1.join)(rawDataDir, 'vietnam');
    if (!(0, fs_1.existsSync)(vietnamDir)) {
        throw new Error(`Vietnam data directory not found: ${vietnamDir}`);
    }
    const files = (0, fs_1.readdirSync)(vietnamDir)
        .filter(f => f.endsWith('.osm.pbf'))
        .sort()
        .reverse();
    if (files.length === 0) {
        throw new Error(`No PBF files found in ${vietnamDir}`);
    }
    return (0, path_1.join)(vietnamDir, files[0]);
}
//# sourceMappingURL=osm-parser.js.map