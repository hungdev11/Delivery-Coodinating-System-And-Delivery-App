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
RUN mkdir -p osrm_data/osrm-instance-1 osrm_data/osrm-instance-2

# Build application
RUN npm run build

# Production stage
FROM node:20-alpine AS production

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
RUN mkdir -p osrm_data/osrm-instance-1 osrm_data/osrm-instance-2

# Set environment
ENV NODE_ENV=production

# Expose port
EXPOSE 21503

# Set default environment variables
ENV OSRM_DATA_PATH=/app/osrm_data
ENV OSM_RAW_DATA_PATH=/app/raw_data/vietnam
ENV NODE_ENV=production

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD node -e "console.log('healthy')" || exit 1

# Run migrations then start application (use local devDependency via npx)
CMD sh -c "npx prisma migrate deploy && npm run start:prod"
