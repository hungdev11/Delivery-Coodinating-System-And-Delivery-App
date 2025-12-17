"use strict";
/**
 * Health Controller
 * Health check endpoints
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.HealthController = void 0;
const restful_1 = require("../common/types/restful");
class HealthController {
    /**
     * GET /api/v1/health
     * Health check endpoint
     */
    async health(req, res) {
        res.json(restful_1.BaseResponseBuilder.success({
            status: 'healthy',
            timestamp: new Date().toISOString(),
            service: 'osrm-management-system',
        }));
    }
}
exports.HealthController = HealthController;
//# sourceMappingURL=health.controller.js.map