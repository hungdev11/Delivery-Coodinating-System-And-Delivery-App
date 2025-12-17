"use strict";
/**
 * OSRM Management System
 * Main entry point
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
const dotenv = __importStar(require("dotenv"));
const path = __importStar(require("path"));
const app_1 = require("./app");
const logger_1 = require("./common/logger");
const client_1 = require("@prisma/client");
// Load .env file from the same directory as the compiled code (dist/)
// In production, .env should be in osrm-management-system/ directory (parent of dist/)
const envPath = path.resolve(__dirname, '../.env');
const result = dotenv.config({ path: envPath });
if (result.error) {
    // Fallback: try current working directory
    dotenv.config();
    console.warn(`[WARN] Could not load .env from ${envPath}, trying current directory`);
}
else {
    console.log(`[INFO] Loaded .env from ${envPath}`);
}
async function main() {
    try {
        const PORT = parseInt(process.env.PORT || '21520', 10);
        const NODE_ENV = process.env.NODE_ENV || 'production';
        const DB_HOST = process.env.DB_HOST || '127.0.0.1';
        const RAW_DATA_PATH = process.env.RAW_DATA_PATH || './raw_data';
        const OSRM_DATA_PATH = process.env.OSRM_DATA_PATH || './osrm_data';
        logger_1.logger.info('Starting OSRM Management System...');
        logger_1.logger.info('Configuration', {
            port: PORT,
            nodeEnv: NODE_ENV,
            dbHost: DB_HOST,
            rawDataPath: RAW_DATA_PATH,
            osrmDataPath: OSRM_DATA_PATH,
        });
        // Initialize Prisma
        const dbConnectionString = process.env.DB_CONNECTION_STRING ||
            `mysql://${process.env.DB_USERNAME || 'root'}:${process.env.DB_PASSWORD || 'root'}@${DB_HOST}:${parseInt(process.env.DB_PORT || '3306', 10)}/${process.env.ZONE_DB_NAME || 'ds_zone_service'}`;
        const prisma = new client_1.PrismaClient({
            datasources: {
                db: {
                    url: dbConnectionString,
                },
            },
        });
        // Test database connection
        await prisma.$connect();
        logger_1.logger.info('Database connected successfully');
        // Create Express app
        const app = (0, app_1.createApp)(prisma);
        // Start server
        const server = app.listen(PORT, () => {
            logger_1.logger.info(`OSRM Management System started on port ${PORT}`);
            logger_1.logger.info(`Health check: http://localhost:${PORT}/api/v1/health`);
        });
        // Graceful shutdown
        const shutdown = async () => {
            logger_1.logger.info('Shutting down...');
            server.close(async () => {
                await prisma.$disconnect();
                logger_1.logger.info('Shutdown complete');
                process.exit(0);
            });
        };
        process.on('SIGTERM', shutdown);
        process.on('SIGINT', shutdown);
    }
    catch (error) {
        logger_1.logger.error('Failed to start server', { error: error.message, stack: error.stack });
        process.exit(1);
    }
}
main();
//# sourceMappingURL=index.js.map