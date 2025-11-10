# Build stage
FROM node:20-alpine AS builder

WORKDIR /app

# Copy package files
COPY package*.json ./
COPY tsconfig.json ./

# Install dependencies
RUN npm ci

# Copy prisma schema
COPY prisma ./prisma

# Generate Prisma Client
RUN npm run prisma:generate

# Copy source code, utils, and raw data
COPY src ./src
COPY utils ./utils
COPY raw_data ./raw_data

# Create OSRM data directory structure (empty, will be populated at runtime)
# Motorbike instances
RUN mkdir -p osrm_data/osrm-strict-priority-with-delta \
    osrm_data/osrm-flexible-priority-with-delta \
    osrm_data/osrm-strict-priority-no-delta \
    osrm_data/osrm-flexible-priority-no-delta \
    osrm_data/osrm-base
# Car instances
RUN mkdir -p osrm_data/osrm-car-strict-priority-with-delta \
    osrm_data/osrm-car-flexible-priority-with-delta \
    osrm_data/osrm-car-strict-priority-no-delta \
    osrm_data/osrm-car-flexible-priority-no-delta \
    osrm_data/osrm-car-base

# Build application
RUN npm run build

# Production stage
FROM node:20-alpine AS production

# Create non-root user
RUN addgroup -g 1001 -S nodejs && \
    adduser -S nodejs -u 1001

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install production dependencies only
RUN npm ci --production

# Copy prisma schema for runtime
COPY prisma ./prisma

# Copy built application and raw data
COPY --from=builder /app/dist ./dist
COPY --from=builder /app/node_modules/.prisma ./node_modules/.prisma
COPY --from=builder /app/raw_data ./raw_data

# Create OSRM data directory structure (empty, will be populated at runtime)
# Motorbike instances
RUN mkdir -p osrm_data/osrm-strict-priority-with-delta \
    osrm_data/osrm-flexible-priority-with-delta \
    osrm_data/osrm-strict-priority-no-delta \
    osrm_data/osrm-flexible-priority-no-delta \
    osrm_data/osrm-base
# Car instances
RUN mkdir -p osrm_data/osrm-car-strict-priority-with-delta \
    osrm_data/osrm-car-flexible-priority-with-delta \
    osrm_data/osrm-car-strict-priority-no-delta \
    osrm_data/osrm-car-flexible-priority-no-delta \
    osrm_data/osrm-car-base

# Change ownership to non-root user
RUN chown -R nodejs:nodejs /app

# Set environment
ENV NODE_ENV=production

# Expose port
EXPOSE 21503

# Set default environment variables
ENV OSRM_DATA_PATH=/app/osrm_data
ENV OSM_RAW_DATA_PATH=/app/raw_data/vietnam
ENV NODE_ENV=production

# Switch to non-root user
USER nodejs

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD node -e "console.log('healthy')" || exit 1

# Run migrations then start application (use local devDependency via npx)
CMD sh -c "npx prisma migrate deploy && npm run start:prod"
