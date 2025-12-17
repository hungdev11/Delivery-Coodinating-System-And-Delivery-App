/**
 * Osmium Wrapper
 * Independent copy for OSRM Management Service
 */
export declare class OsmiumWrapper {
    private verbose;
    constructor(verbose?: boolean);
    checkInstallation(): Promise<boolean>;
    private supportsStrategyFlag;
    extractRoutingWithAddresses(inputPbf: string, polyFile: string, outputPbf: string): Promise<void>;
    getFileInfo(filePath: string): Promise<any>;
}
//# sourceMappingURL=osmium-wrapper.d.ts.map