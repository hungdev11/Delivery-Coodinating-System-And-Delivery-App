#!/bin/bash
# OSRM Build Script for Production
# 
# This script:
# 1. Extracts OSM data from PBF using osmium (ALWAYS via Docker to prevent OOM)
# 2. Builds all OSRM models (full, rating-only, blocking-only, base)
# 3. Copies lib folder to each instance
# 4. Starts all OSRM containers
#
# Usage:
#   ./scripts/build-osrm.sh [--skip-extract] [--skip-start]
#
# Environment variables:
#   - OSMIUM_USE_DOCKER: Use Docker for osmium if not installed (default: true)
#   - OSRM_DATA_DIR: Directory for OSRM data (default: ./osrm_data)
#   - RAW_DATA_DIR: Directory for raw data (default: ./raw_data)

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default paths (relative to script location)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROD_PACK_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
OSRM_DATA_DIR="${OSRM_DATA_DIR:-$PROD_PACK_DIR/osrm_data}"
RAW_DATA_DIR="${RAW_DATA_DIR:-$PROD_PACK_DIR/raw_data}"
# Zone service directory no longer needed - script uses API endpoint

# Flags
SKIP_EXTRACT=false
SKIP_START=false
USE_PREBUILT=false
PREBUILT_DIR=""
EXTRACTED_FILE=""

# Parse arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --skip-extract)
      SKIP_EXTRACT=true
      shift
      ;;
    --skip-start)
      SKIP_START=true
      shift
      ;;
    --use-prebuilt)
      USE_PREBUILT=true
      shift
      if [[ $# -gt 0 ]] && [[ ! "$1" =~ ^-- ]]; then
        PREBUILT_DIR="$1"
        shift
      fi
      ;;
    --extracted-file)
      EXTRACTED_FILE="$2"
      SKIP_EXTRACT=true
      shift 2
      ;;
    *)
      echo -e "${RED}Unknown option: $1${NC}"
      exit 1
      ;;
  esac
done

# Logging functions
log_info() {
  echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
  echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warn() {
  echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
  echo -e "${RED}[ERROR]${NC} $1"
}

# Verify OSM PBF file is valid (simple check - just size)
verify_osm_file() {
  local file_path="$1"
  
  if [[ ! -f "$file_path" ]]; then
    return 1
  fi
  
  # Get file size (try multiple methods for compatibility)
  local file_size=0
  if [[ "$(uname)" == "Linux" ]]; then
    file_size=$(stat -c%s "$file_path" 2>/dev/null || echo "0")
  else
    file_size=$(stat -f%z "$file_path" 2>/dev/null || echo "0")
  fi
  
  # If we can't get size, try ls
  if [[ "$file_size" == "0" ]] || [[ -z "$file_size" ]]; then
    file_size=$(ls -l "$file_path" 2>/dev/null | awk '{print $5}' || echo "0")
  fi
  
  # Convert to number
  file_size=$(echo "$file_size" | tr -d ' ' | grep -E '^[0-9]+$' || echo "0")
  
  # If file exists and has size > 0, consider it valid
  if [[ $file_size -gt 0 ]]; then
    log_info "File size: $((file_size / 1024 / 1024))MB"
    if [[ $file_size -lt 1048576 ]]; then
      log_warn "File seems small (${file_size} bytes), but will use it"
    fi
    return 0
  fi
  
  log_warn "File appears to be empty (0 bytes)"
  return 1
}

# Find extracted OSM file automatically (any .osm.pbf file)
find_extracted_file() {
  local search_dirs=(
    "$RAW_DATA_DIR/extracted"
    "$PROD_PACK_DIR/raw_data/extracted"
    "$ZONE_SERVICE_DIR/raw_data/extracted"
    "$(dirname "$PROD_PACK_DIR")/BE/zone_service/raw_data/extracted"
    "$(dirname "$RAW_DATA_DIR")/zone_service/raw_data/extracted"
    "$(dirname "$PROD_PACK_DIR")/zone_service/raw_data/extracted"
  )
  
  # Remove duplicates and empty entries
  local unique_dirs=()
  for dir in "${search_dirs[@]}"; do
    if [[ -n "$dir" ]] && [[ -d "$dir" ]]; then
      # Check if not already in array
      local found=false
      for existing in "${unique_dirs[@]}"; do
        if [[ "$existing" == "$dir" ]]; then
          found=true
          break
        fi
      done
      if [[ "$found" == "false" ]]; then
        unique_dirs+=("$dir")
      fi
    fi
  done
  
  # Search in each directory
  for dir in "${unique_dirs[@]}"; do
    # Find any .osm.pbf file (not locked to specific name)
    local found_file=$(find "$dir" -maxdepth 1 -type f \( -name "*.osm.pbf" -o -name "*.pbf" \) 2>/dev/null | head -n1)
    if [[ -n "$found_file" ]] && [[ -f "$found_file" ]]; then
      echo "$found_file"
      return 0
    fi
  done
  
  return 1
}

# Extract OSM data using osmium (prefer native, fallback to Docker)
# Or use provided/extracted file automatically
extract_osm_data() {
  local vietnam_pbf="$RAW_DATA_DIR/vietnam/vietnam-251013.osm.pbf"
  local poly_file="$RAW_DATA_DIR/poly/thuduc_cu.poly"
  local extracted_dir="$RAW_DATA_DIR/extracted"
  local extracted_pbf="$extracted_dir/thuduc_complete.osm.pbf"
  
  # Create extracted directory
  mkdir -p "$extracted_dir"
  
  # Priority 1: User provided file via --extracted-file
  if [[ -n "$EXTRACTED_FILE" ]]; then
    log_info "Using provided extracted file: $EXTRACTED_FILE"
    
    if [[ ! -f "$EXTRACTED_FILE" ]]; then
      log_error "Provided extracted file not found: $EXTRACTED_FILE"
      exit 1
    fi
    
    # Copy to extracted directory with standard name
    log_info "Copying to: $extracted_pbf"
    cp "$EXTRACTED_FILE" "$extracted_pbf" || {
      log_error "Failed to copy extracted file"
      exit 1
    }
    
    if verify_osm_file "$extracted_pbf"; then
      log_success "Using provided extracted file"
      return 0
    fi
  fi
  
  # Priority 2: Auto-detect extracted file in common locations
  log_info "Scanning for existing extracted OSM files..."
  local found_file=$(find_extracted_file)
  
  if [[ -n "$found_file" ]] && [[ -f "$found_file" ]]; then
    log_info "Found extracted file: $found_file"
    
    # If it's already in the right place with right name, use it
    if [[ "$found_file" == "$extracted_pbf" ]]; then
      log_info "File already in correct location"
    else
      # Copy to standard location
      log_info "Copying to standard location: $extracted_pbf"
      cp "$found_file" "$extracted_pbf" || {
        log_error "Failed to copy extracted file"
        exit 1
      }
    fi
    
    if verify_osm_file "$extracted_pbf"; then
      log_success "Using auto-detected extracted file: $found_file"
      return 0
    else
      log_warn "File verification failed, will re-extract..."
    fi
  fi
  
  # Priority 3: Check if standard file exists
  if [[ -f "$extracted_pbf" ]]; then
    log_info "Found extracted file at standard location: $extracted_pbf"
    if verify_osm_file "$extracted_pbf"; then
      log_success "Using existing extracted file"
      return 0
    else
      log_warn "Existing file may be corrupted, will re-extract..."
    fi
  fi
  
  # Priority 4: Extract from source PBF
  log_info "No extracted file found, starting OSM data extraction..."
  
  # Check if source files exist
  if [[ ! -f "$vietnam_pbf" ]]; then
    log_error "Vietnam PBF not found: $vietnam_pbf"
    log_error "Please provide extracted file or ensure source PBF exists"
    exit 1
  fi
  
  if [[ ! -f "$poly_file" ]]; then
    log_error "Poly file not found: $poly_file"
    exit 1
  fi
  
  # Check if osmium-tool is installed natively (preferred)
  local use_native_osmium=false
  if command -v osmium &> /dev/null; then
    local osmium_version=$(osmium --version 2>&1 | head -n1)
    log_info "Found native osmium-tool: $osmium_version"
    use_native_osmium=true
  else
    log_info "osmium-tool not found, will use Docker"
  fi
  
  # If using native osmium, check available memory
  if [[ "$use_native_osmium" == "true" ]]; then
    # Get available memory
    local available_mem_mb=0
    if command -v free &> /dev/null; then
      available_mem_mb=$(free -m | grep Mem | awk '{print $7}')
    elif [[ -f /proc/meminfo ]]; then
      available_mem_mb=$(grep MemAvailable /proc/meminfo | awk '{print $2}')
      available_mem_mb=$((available_mem_mb / 1024))
    fi
    
    # Check if we have enough memory (need at least 2GB for extraction)
    if [[ $available_mem_mb -gt 0 ]] && [[ $available_mem_mb -lt 2048 ]]; then
      log_warn "Low available memory: ${available_mem_mb}MB"
      log_warn "Extraction may fail or be very slow"
      log_warn "Consider using Docker with memory limits or extracting on another machine"
    fi
    
    log_info "Using native osmium-tool for extraction"
  else
    log_info "Using Docker for osmium-tool (with memory limits)"
  fi
  
  # Get available memory for checks
  local available_mem_mb=0
  if command -v free &> /dev/null; then
    available_mem_mb=$(free -m | grep Mem | awk '{print $7}')
  elif [[ -f /proc/meminfo ]]; then
    available_mem_mb=$(grep MemAvailable /proc/meminfo | awk '{print $2}')
    available_mem_mb=$((available_mem_mb / 1024))
  fi
  
  # If using native osmium, check memory and warn if low
  if [[ "$use_native_osmium" == "true" ]]; then
    if [[ $available_mem_mb -gt 0 ]] && [[ $available_mem_mb -lt 2048 ]]; then
      log_warn "Low available memory: ${available_mem_mb}MB"
      log_warn "Extraction may fail or be very slow"
      log_warn "Consider using Docker with memory limits: export FORCE_DOCKER_OSMIUM=true"
    fi
    log_info "Using native osmium-tool for extraction"
  else
    # Using Docker - calculate memory limit
    local memory_limit=""
    if [[ -n "${OSMIUM_MEMORY_LIMIT:-}" ]]; then
      memory_limit="${OSMIUM_MEMORY_LIMIT}"
      log_info "Using user-specified memory limit: $memory_limit"
    elif [[ $available_mem_mb -gt 0 ]]; then
      local memory_limit_mb=$((available_mem_mb * 80 / 100))
      if [[ $memory_limit_mb -lt 1024 ]]; then
        memory_limit_mb=1024
      elif [[ $memory_limit_mb -gt 8192 ]]; then
        memory_limit_mb=8192
      fi
      memory_limit="${memory_limit_mb}m"
      log_info "Detected available memory: ${available_mem_mb}MB"
      log_info "Setting Docker memory limit to: $memory_limit (80% of available)"
    else
      memory_limit="2g"
      log_warn "Could not detect available memory, using default: $memory_limit"
    fi
    
    # Check if we have enough memory for Docker
    if [[ $available_mem_mb -gt 0 ]] && [[ $available_mem_mb -lt 1536 ]]; then
      log_error "Insufficient memory for OSM extraction with Docker!"
      log_error "Available: ${available_mem_mb}MB, Required: ~2GB minimum"
      log_error ""
      log_error "Recommended solutions:"
      log_error "  1. Build on a machine with more RAM (4GB+ recommended)"
      log_error "  2. Use pre-extracted OSM data:"
      log_error "     - Extract on another machine with more RAM"
      log_error "     - Copy extracted file to: $extracted_pbf"
      log_error "     - Run: ./scripts/build-osrm.sh --skip-extract"
      exit 1
    fi
    
    log_info "Extracting with Docker (memory limit: $memory_limit)..."
  fi
  
  # Run extraction using native osmium or Docker
  if [[ "$use_native_osmium" == "true" ]]; then
    # Use native osmium-tool
    log_info "Running native osmium extraction..."
    log_info "This may take 10-30 minutes depending on file size..."
    
    cd "$RAW_DATA_DIR"
    osmium extract \
      --polygon "poly/thuduc_cu.poly" \
      -s complete_ways \
      --overwrite \
      --output "extracted/$(basename "$extracted_pbf")" \
      "vietnam/$(basename "$vietnam_pbf")" || {
      log_error ""
      log_error "Osmium extraction failed!"
      log_error ""
      log_error "Possible causes:"
      log_error "  1. Out of memory - system killed the process"
      log_error "  2. Disk space full - check: df -h"
      log_error "  3. Input file corrupted - verify PBF file"
      log_error ""
      if [[ $available_mem_mb -gt 0 ]]; then
        log_error "Available memory: ${available_mem_mb}MB"
        log_error "OSM extraction typically needs 2-4GB RAM"
      fi
      log_error ""
      log_error "Solutions:"
      log_error "  1. Use Docker with memory limits (recommended for low-memory systems):"
      log_error "     export FORCE_DOCKER_OSMIUM=true"
      log_error "     ./scripts/build-osrm.sh"
      log_error "  2. Extract on a machine with more RAM"
      log_error "  3. Use pre-extracted OSM data:"
      log_error "     ./scripts/build-osrm.sh --skip-extract"
      exit 1
    }
  else
    # Use Docker with memory limits
    # Check if Docker is available
    if ! command -v docker &> /dev/null; then
      log_error "Docker is required but not installed"
      log_error "Please install Docker: curl -fsSL https://get.docker.com | sh"
      log_error "Or install osmium-tool natively: sudo apt-get install osmium-tool"
      exit 1
    fi
    
    # Use Debian-based image and install osmium-tool
    log_info "Using Debian image with osmium-tool installation..."
    
    # Pull Debian image
    if ! docker image inspect debian:bookworm-slim &>/dev/null; then
      log_info "Pulling Debian image..."
      docker pull debian:bookworm-slim
    fi
    
    # Run extraction in Docker container with memory limits
    log_info "Installing osmium-tool and extracting OSM data..."
    log_info "This may take 10-30 minutes depending on file size..."
    
    # For low-memory systems, don't restrict swap (allow container to use swap)
    # For normal systems, limit swap to prevent excessive swapping
    local swap_limit=""
    if [[ $available_mem_mb -lt 2048 ]]; then
      # Low memory - allow swap (set swap = 2x memory limit)
      local memory_limit_mb_num=$(echo "$memory_limit" | sed 's/[^0-9]//g')
      local swap_limit_mb=$((memory_limit_mb_num * 2))
      swap_limit="${swap_limit_mb}m"
      log_info "Allowing swap usage (limit: $swap_limit) for low-memory system"
    else
      # Normal memory - restrict swap to memory limit
      swap_limit="$memory_limit"
    fi
    
    docker run --rm \
      --memory="$memory_limit" \
      --memory-swap="$swap_limit" \
      -v "$RAW_DATA_DIR:/data" \
      -w /data \
      debian:bookworm-slim \
      bash -c "
        set -e
        export DEBIAN_FRONTEND=noninteractive
        echo 'Installing osmium-tool...'
        apt-get update -qq > /dev/null 2>&1
        apt-get install -y -qq osmium-tool > /dev/null 2>&1
        echo 'Starting extraction (this may take a while, especially on low-memory systems)...'
        osmium extract \
          --polygon poly/thuduc_cu.poly \
          -s complete_ways \
          --overwrite \
          --output extracted/$(basename "$extracted_pbf") \
          vietnam/$(basename "$vietnam_pbf")
        echo 'Extraction completed successfully'
      " || {
    log_error ""
    log_error "Osmium extraction failed!"
    log_error ""
    log_error "Possible causes:"
    log_error "  1. Out of memory (most likely) - system has limited RAM"
    log_error "  2. Disk space full - check: df -h"
    log_error "  3. Input file corrupted - verify PBF file"
    log_error ""
    log_error "Memory information:"
    log_error "  Docker memory limit: $memory_limit"
    if [[ $available_mem_mb -gt 0 ]]; then
      log_error "  Available system memory: ${available_mem_mb}MB"
    fi
    log_error ""
    log_error "Solutions:"
    log_error "  1. Build on a machine with more RAM (recommended)"
    log_error "  2. Use pre-extracted OSM data:"
    log_error "     - Extract on another machine"
    log_error "     - Copy to: $extracted_pbf"
    log_error "     - Run: ./scripts/build-osrm.sh --skip-extract"
    log_error "  3. If you have more RAM, manually set limit:"
    log_error "     export OSMIUM_MEMORY_LIMIT=8g"
    exit 1
    }
  fi
  
  log_success "OSM data extracted to: $extracted_pbf"
}

# Copy lib folder to OSRM instance
copy_lib_folder() {
  local instance_dir="$1"
  local lib_source="$OSRM_DATA_DIR/lib"
  local lib_target="$instance_dir/lib"
  
  if [[ ! -d "$lib_source" ]]; then
    log_warn "Lib folder not found at $lib_source, skipping..."
    return 0
  fi
  
  log_info "Copying lib folder to $instance_dir"
  mkdir -p "$instance_dir"
  cp -r "$lib_source" "$lib_target"
  log_success "Lib folder copied"
}

# Build all OSRM models using the zone_service container via API
build_all_osrm_models() {
  log_info "Building all OSRM models from database..."
  
  # Check if using pre-built data
  if [[ "$USE_PREBUILT" == "true" ]]; then
    log_info "Using pre-built OSRM data..."
    copy_prebuilt_data
    return 0
  fi
  
  # Use zone-service container via API (production mode)
  if docker ps --format '{{.Names}}' | grep -q "^dss-zone-service$"; then
    log_info "Using zone-service container to generate OSRM data via API..."
    build_osrm_via_container
  else
    log_error "Cannot build OSRM data:"
    log_error "  - zone-service container is not running"
    log_error ""
    log_error "Options:"
    log_error "  1. Start zone-service container: docker-compose up -d zone-service"
    log_error "  2. Use pre-built data: ./scripts/build-osrm.sh --use-prebuilt [path]"
    log_error "  3. Build OSRM data separately and copy to $OSRM_DATA_DIR"
    exit 1
  fi
}

# Copy pre-built OSRM data
copy_prebuilt_data() {
  local source_dir="${PREBUILT_DIR:-$OSRM_DATA_DIR}"
  
  if [[ ! -d "$source_dir" ]]; then
    log_error "Pre-built data directory not found: $source_dir"
    exit 1
  fi
  
  log_info "Copying pre-built OSRM data from: $source_dir"
  
  local models=("osrm-full" "osrm-rating-only" "osrm-blocking-only" "osrm-base")
  
  for model in "${models[@]}"; do
    local source_model_dir="$source_dir/$model"
    local target_dir="$OSRM_DATA_DIR/$model"
    
    if [[ -d "$source_model_dir" ]]; then
      log_info "  Copying $model..."
      mkdir -p "$target_dir"
      cp -r "$source_model_dir"/* "$target_dir/" 2>/dev/null || {
        log_warn "  Failed to copy $model, trying individual files..."
        # Try copying individual files
        find "$source_model_dir" -type f -exec cp {} "$target_dir/" \; 2>/dev/null || true
      }
      
      # Ensure lib folder exists in each instance
      if [[ -d "$source_dir/lib" ]]; then
        log_info "    Copying lib folder to $model..."
        mkdir -p "$target_dir/lib"
        cp -r "$source_dir/lib"/* "$target_dir/lib/" 2>/dev/null || true
      fi
    else
      log_warn "  Model $model not found, skipping..."
    fi
  done
  
  # Copy lib folder to root
  if [[ -d "$source_dir/lib" ]]; then
    if [[ ! -d "$OSRM_DATA_DIR/lib" ]]; then
      log_info "Copying lib folder to OSRM data root..."
      mkdir -p "$OSRM_DATA_DIR/lib"
      cp -r "$source_dir/lib"/* "$OSRM_DATA_DIR/lib/"
    fi
  fi
  
  log_success "Pre-built OSRM data copied"
}

# Build OSRM data using zone-service container via API
build_osrm_via_container() {
  log_info "Running OSRM generator in zone-service container via API..."
  
  local zone_service_image=$(docker inspect dss-zone-service --format '{{.Config.Image}}' 2>/dev/null || echo "")
  
  if [[ -z "$zone_service_image" ]]; then
    log_error "zone-service container not found"
    exit 1
  fi
  
  # Wait for zone-service to be ready
  log_info "Waiting for zone-service to be ready..."
  local max_wait=30
  local waited=0
  while [[ $waited -lt $max_wait ]]; do
    if docker exec dss-zone-service wget --no-verbose --tries=1 --spider http://127.0.0.1:21503/health 2>/dev/null; then
      log_success "zone-service is ready"
      break
    fi
    sleep 2
    waited=$((waited + 1))
  done
  
  if [[ $waited -eq $max_wait ]]; then
    log_warn "zone-service may not be ready, but continuing..."
  fi
  
  # Use API endpoint to trigger generation (no tsx required)
  log_info "Triggering OSRM V2 generation via API endpoint..."
  
  # Get zone-service URL (try container name first, then localhost)
  local zone_service_url=""
  if docker exec dss-zone-service wget --no-verbose --tries=1 --spider http://zone-service:21503/health 2>/dev/null; then
    zone_service_url="http://zone-service:21503"
  else
    # Try from host
    local host_port=$(docker port dss-zone-service 21503/tcp 2>/dev/null | cut -d: -f2 || echo "")
    if [[ -n "$host_port" ]]; then
      zone_service_url="http://localhost:${host_port}"
    else
      zone_service_url="http://localhost:21503"
    fi
  fi
  
  local generate_url="${zone_service_url}/api/v1/osrm/generate-v2"
  log_info "Calling: $generate_url"
  
  # Call API endpoint (synchronous - wait for completion)
  local response=$(curl -s -X POST "$generate_url" \
    -H "Content-Type: application/json" \
    -w "\n%{http_code}" \
    --max-time 7200 || echo "ERROR 000")
  
  local http_code=$(echo "$response" | tail -n1)
  local body=$(echo "$response" | sed '$d')
  
  if [[ "$http_code" == "200" ]]; then
    log_success "OSRM V2 generation completed successfully"
    # Parse response to get model count
    local model_count=$(echo "$body" | grep -o '"models":\[.*\]' | grep -o ',' | wc -l || echo "0")
    model_count=$((model_count + 1))
    log_info "Generated $model_count models"
  else
    log_error "OSRM V2 generation failed"
    log_error "HTTP Code: $http_code"
    log_error "Response: $body"
    log_error ""
    log_error "Troubleshooting:"
    log_error "  1. Check container logs: docker logs dss-zone-service"
    log_error "  2. Check if zone-service is healthy: docker exec dss-zone-service wget -q -O- http://127.0.0.1:21503/health"
    log_error "  3. Try manual API call: curl -X POST $generate_url"
    exit 1
  fi
  
  # Copy generated data from container
  log_info "Copying generated OSRM data from container..."
  local models=("osrm-full" "osrm-rating-only" "osrm-blocking-only" "osrm-base")
  
  for model in "${models[@]}"; do
    log_info "  Copying $model..."
    local target_dir="$OSRM_DATA_DIR/$model"
    mkdir -p "$target_dir"
    
    # Copy from container's osrm_data directory
    docker cp "dss-zone-service:/app/osrm_data/$model/." "$target_dir/" 2>/dev/null || {
      log_warn "  Model $model not found in container, skipping..."
      continue
    }
    
    # Copy lib folder to each instance
    if docker exec dss-zone-service test -d "/app/osrm_data/lib" 2>/dev/null; then
      log_info "    Copying lib folder to $model..."
      mkdir -p "$target_dir/lib"
      docker cp "dss-zone-service:/app/osrm_data/lib/." "$target_dir/lib/" 2>/dev/null || true
    fi
  done
  
  # Copy lib folder to root
  if docker exec dss-zone-service test -d "/app/osrm_data/lib" 2>/dev/null; then
    if [[ ! -d "$OSRM_DATA_DIR/lib" ]]; then
      log_info "Copying lib folder to OSRM data root..."
      mkdir -p "$OSRM_DATA_DIR/lib"
      docker cp "dss-zone-service:/app/osrm_data/lib/." "$OSRM_DATA_DIR/lib/" 2>/dev/null || true
    fi
  fi
  
  log_success "All OSRM models copied from container"
}

# Start all OSRM containers
start_osrm_containers() {
  log_info "Starting all OSRM containers..."
  
  cd "$PROD_PACK_DIR"
  
  # Start OSRM services
  docker-compose up -d osrm-v2-full osrm-v2-rating-only osrm-v2-blocking-only osrm-v2-base
  
  log_success "All OSRM containers started"
  log_info "Waiting for containers to be healthy..."
  
  # Wait a bit for containers to start
  sleep 5
  
  # Check container status
  docker-compose ps osrm-v2-full osrm-v2-rating-only osrm-v2-blocking-only osrm-v2-base
}

# Main execution
main() {
  echo "=========================================="
  echo "  OSRM Build Script for Production"
  echo "=========================================="
  echo ""
  
  log_info "OSRM Data Directory: $OSRM_DATA_DIR"
  log_info "Raw Data Directory: $RAW_DATA_DIR"
  log_info "Zone Service: Will use Docker container API"
  echo ""
  
  # Step 1: Extract OSM data (if not skipped)
  if [[ "$SKIP_EXTRACT" == "false" ]]; then
    extract_osm_data
    echo ""
  else
    log_info "Skipping OSM extraction (--skip-extract)"
    echo ""
  fi
  
  # Step 2: Build all OSRM models
  build_all_osrm_models
  echo ""
  
  # Step 3: Start containers (if not skipped)
  if [[ "$SKIP_START" == "false" ]]; then
    start_osrm_containers
    echo ""
  else
    log_info "Skipping container start (--skip-start)"
    echo ""
  fi
  
  log_success "OSRM build completed successfully!"
  echo ""
  log_info "Usage tips:"
  log_info "  - Script auto-detects extracted .osm.pbf files in common locations"
  log_info "  - Rebuild with new data: ./scripts/build-osrm.sh --skip-extract"
  log_info "  - Use pre-built OSRM data: ./scripts/build-osrm.sh --use-prebuilt [path]"
  log_info "  - Use specific extracted file: ./scripts/build-osrm.sh --extracted-file /path/to/file.osm.pbf"
  log_info "  - Extract only: ./scripts/build-osrm.sh --skip-start"
  log_info "  - Start containers only: docker-compose up -d osrm-v2-full osrm-v2-rating-only osrm-v2-blocking-only osrm-v2-base"
  echo ""
}

# Run main
main "$@"
