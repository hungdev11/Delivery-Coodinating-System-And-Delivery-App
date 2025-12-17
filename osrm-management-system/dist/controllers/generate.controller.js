"use strict";
/**
 * Generate Controller
 * RESTful API endpoints for OSRM data generation
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.GenerateController = void 0;
const generate_service_1 = require("../services/generate.service");
const restful_1 = require("../common/types/restful");
const logger_1 = require("../common/logger");
class GenerateController {
    generateService;
    prisma;
    constructor(prisma) {
        this.prisma = prisma;
        this.generateService = new generate_service_1.GenerateService(prisma);
    }
    /**
     * POST /api/v1/generate/osrm-v2
     * Generate all OSRM V2 models from database
     */
    async generateOSRMV2(req, res, next) {
        try {
            logger_1.logger.info('Generate OSRM V2 request received');
            const result = await this.generateService.generateAllModels();
            if (result.success) {
                res.json(restful_1.BaseResponseBuilder.success(result, `Generated ${result.models?.length || 0} OSRM models successfully`));
            }
            else {
                res.status(500).json(restful_1.BaseResponseBuilder.error(result.error || 'Generation failed'));
            }
        }
        catch (error) {
            next(error);
        }
    }
}
exports.GenerateController = GenerateController;
//# sourceMappingURL=generate.controller.js.map