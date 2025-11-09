# Build stage
FROM node:20-alpine AS builder

WORKDIR /app

# Copy package files
COPY package*.json ./
COPY tsconfig*.json ./
COPY vite.config.ts ./

# Install dependencies
RUN npm ci

# Copy source code and public assets
COPY src ./src
COPY public ./public
COPY index.html ./

# Build application with environment variables
ARG VITE_API_BASE_URL=http://localhost:8080/api/v1
ARG VITE_ENV=production
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_ENV=$VITE_ENV

RUN npm run build

# Production stage - using nginx to serve static files
FROM nginx:alpine

# Copy built files to nginx html directory
COPY --from=builder /app/dist /usr/share/nginx/html

# nginx image already runs as non-root user (nginx), so no USER directive needed
# The nginx:alpine image uses a non-root user by default

# Expose port 80
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost/ || exit 1

# Start nginx
CMD ["nginx", "-g", "daemon off;"]
