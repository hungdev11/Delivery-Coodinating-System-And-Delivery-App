"use strict";
/**
 * Extract Controller
 * RESTful API endpoints for OSM data extraction
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.ExtractController = void 0;
const extract_service_1 = require("../services/extract.service");
const restful_1 = require("../common/types/restful");
const logger_1 = require("../common/logger");
class ExtractController {
    extractService;
    constructor(prisma) {
        this.extractService = new extract_service_1.ExtractService(prisma);
    }
    /**
     * POST /api/v1/extract/complete
     * Extract complete OSM data (routing + addresses)
     */
    async extractComplete(req, res, next) {
        try {
            const { polyFile } = req?.body || { polyFile: undefined };
            logger_1.logger.info('Extract complete request received', { polyFile });
            const result = await this.extractService.extractCompleteData(polyFile);
            if (result.success) {
                res.json(restful_1.BaseResponseBuilder.success(result, 'Extraction completed successfully'));
            }
            else {
                res.status(500).json(restful_1.BaseResponseBuilder.error(result.error || 'Extraction failed'));
            }
        }
        catch (error) {
            next(error);
        }
    }
}
exports.ExtractController = ExtractController;
//# sourceMappingURL=extract.controller.js.map