"use strict";
/**
 * Express Application Setup
 */
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.createApp = createApp;
const express_1 = __importDefault(require("express"));
const cors_1 = __importDefault(require("cors"));
const routes_1 = require("./routes");
const logger_1 = require("./common/logger");
function createApp(prisma) {
    const app = (0, express_1.default)();
    // Middleware
    app.use((0, cors_1.default)());
    app.use(express_1.default.json());
    app.use(express_1.default.urlencoded({ extended: true }));
    // Request logging
    app.use((req, res, next) => {
        logger_1.logger.info(`${req.method} ${req.path}`, {
            ip: req.ip,
            userAgent: req.get('user-agent'),
        });
        next();
    });
    // Routes
    app.use((0, routes_1.createRoutes)(prisma));
    // Error handler
    app.use((err, req, res, next) => {
        logger_1.logger.error('Unhandled error', {
            error: err.message,
            stack: err.stack,
            path: req.path,
        });
        res.status(500).json({
            message: err.message || 'Internal server error',
        });
    });
    // 404 handler
    app.use((req, res) => {
        res.status(404).json({
            message: 'Not found',
        });
    });
    return app;
}
//# sourceMappingURL=app.js.map