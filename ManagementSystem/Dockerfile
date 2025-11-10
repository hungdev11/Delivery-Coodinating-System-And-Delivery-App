# Build-only Dockerfile for ManagementSystem
# This Dockerfile only builds the application and outputs dist files
# The built files are served by nginx-proxy in docker-compose.yml

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
COPY env.d.ts ./

# Build application with environment variables
ARG VITE_API_URL=http://localhost:8080/api
ARG VITE_WS_URL=http://localhost:8080/api/ws
ARG VITE_ENV=production
ARG VITE_MAPTILER_API_KEY=
ENV VITE_API_URL=$VITE_API_URL
ENV VITE_WS_URL=$VITE_WS_URL
ENV VITE_ENV=$VITE_ENV
ENV VITE_MAPTILER_API_KEY=$VITE_MAPTILER_API_KEY

# Build the application (outputs to /app/dist)
RUN npm run build-only

# Keep the dist directory as the final output
# The docker-compose.yml will copy files from /app/dist to shared volume
WORKDIR /app
