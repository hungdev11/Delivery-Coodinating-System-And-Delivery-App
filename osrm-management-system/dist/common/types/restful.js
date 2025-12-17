"use strict";
/**
 * RESTful API Types
 * Following RESTFUL.md standards
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.BaseResponseBuilder = void 0;
class BaseResponseBuilder {
    static success(data, message) {
        const response = {};
        if (data !== undefined) {
            response.result = data;
        }
        if (message) {
            response.message = message;
        }
        return response;
    }
    static error(message) {
        return { message };
    }
}
exports.BaseResponseBuilder = BaseResponseBuilder;
//# sourceMappingURL=restful.js.map