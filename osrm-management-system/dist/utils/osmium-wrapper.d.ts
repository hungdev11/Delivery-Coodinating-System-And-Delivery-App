/**
 * Osmium Wrapper
 * Independent copy for OSRM Management Service
 *
 * Version Compatibility:
 * - Version >= 1.18: Uses modern command format
 * - Version < 1.18: Uses legacy command format (auto-detected)
 */
export declare class OsmiumWrapper {
    private verbose;
    private version;
    private versionNumber;
    constructor(verbose?: boolean);
    /**
     * Check if osmium-tool is installed and detect version
     */
    checkInstallation(): Promise<boolean>;
    /**
     * Get osmium version number (e.g., 1.18, 1.17)
     * Returns null if version cannot be determined
     */
    private getVersionNumber;
    /**
     * Check if osmium supports --strategy flag (legacy method, kept for compatibility)
     * @deprecated Use getVersionNumber() instead
     */
    private supportsStrategyFlag;
    extractRoutingWithAddresses(inputPbf: string, polyFile: string, outputPbf: string): Promise<void>;
    getFileInfo(filePath: string): Promise<any>;
}
//# sourceMappingURL=osmium-wrapper.d.ts.map