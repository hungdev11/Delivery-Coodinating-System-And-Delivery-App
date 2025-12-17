#!/bin/bash
# Verify OSRM setup and data integrity

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROD_PACK_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
OSRM_DATA_DIR="${OSRM_DATA_DIR:-$PROD_PACK_DIR/osrm_data}"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
  echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
  echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_error() {
  echo -e "${RED}[ERROR]${NC} $1"
}

log_warn() {
  echo -e "${YELLOW}[WARN]${NC} $1"
}

# Check if OSRM data directory exists
check_osrm_data_dir() {
  if [[ ! -d "$OSRM_DATA_DIR" ]]; then
    log_error "OSRM data directory not found: $OSRM_DATA_DIR"
    return 1
  fi
  log_success "OSRM data directory exists"
  return 0
}

# Check if lib folder exists
check_lib_folder() {
  local lib_dir="$OSRM_DATA_DIR/lib"
  if [[ ! -d "$lib_dir" ]]; then
    log_warn "Lib folder not found at root: $lib_dir"
    return 1
  fi
  
  local lib_files=("set.lua" "sequence.lua" "way_handlers.lua" "traffic_signal.lua" "access.lua" "maxspeed.lua" "measure.lua")
  local missing=0
  
  for file in "${lib_files[@]}"; do
    if [[ ! -f "$lib_dir/$file" ]]; then
      log_warn "  Missing lib file: $file"
      missing=$((missing + 1))
    fi
  done
  
  if [[ $missing -eq 0 ]]; then
    log_success "Lib folder is complete"
    return 0
  else
    log_warn "Lib folder is missing $missing files"
    return 1
  fi
}

# Check OSRM model
check_osrm_model() {
  local model_name="$1"
  local model_dir="$OSRM_DATA_DIR/$model_name"
  
  log_info "Checking model: $model_name"
  
  if [[ ! -d "$model_dir" ]]; then
    log_error "  Model directory not found: $model_dir"
    return 1
  fi
  
  local required_files=("network.osrm" "custom_bicycle.lua")
  local missing=0
  
  for file in "${required_files[@]}"; do
    if [[ ! -f "$model_dir/$file" ]]; then
      log_error "  Missing required file: $file"
      missing=$((missing + 1))
    fi
  done
  
  # Check if lib folder exists in model directory
  if [[ ! -d "$model_dir/lib" ]]; then
    log_warn "  Lib folder not found in model directory"
    log_info "  Copying lib folder..."
    if [[ -d "$OSRM_DATA_DIR/lib" ]]; then
      mkdir -p "$model_dir/lib"
      cp -r "$OSRM_DATA_DIR/lib"/* "$model_dir/lib/"
      log_success "  Lib folder copied"
    else
      log_error "  Cannot copy lib folder (source not found)"
      missing=$((missing + 1))
    fi
  else
    log_success "  Lib folder exists in model directory"
  fi
  
  if [[ $missing -eq 0 ]]; then
    log_success "  Model $model_name is complete"
    return 0
  else
    log_error "  Model $model_name is missing $missing required items"
    return 1
  fi
}

# Check Docker containers
check_containers() {
  log_info "Checking OSRM containers..."
  
  local containers=("dss-osrm-v2-full" "dss-osrm-v2-rating-only" "dss-osrm-v2-blocking-only" "dss-osrm-v2-base")
  local running=0
  local total=${#containers[@]}
  
  for container in "${containers[@]}"; do
    if docker ps --format '{{.Names}}' | grep -q "^${container}$"; then
      log_success "  Container $container is running"
      running=$((running + 1))
    else
      log_warn "  Container $container is not running"
    fi
  done
  
  if [[ $running -eq $total ]]; then
    log_success "All containers are running"
    return 0
  else
    log_warn "$running/$total containers are running"
    return 1
  fi
}

# Main
main() {
  echo "=========================================="
  echo "  OSRM Setup Verification"
  echo "=========================================="
  echo ""
  
  local errors=0
  
  # Check OSRM data directory
  if ! check_osrm_data_dir; then
    errors=$((errors + 1))
  fi
  echo ""
  
  # Check lib folder
  if ! check_lib_folder; then
    errors=$((errors + 1))
  fi
  echo ""
  
  # Check each model
  local models=("osrm-full" "osrm-rating-only" "osrm-blocking-only" "osrm-base")
  for model in "${models[@]}"; do
    if ! check_osrm_model "$model"; then
      errors=$((errors + 1))
    fi
    echo ""
  done
  
  # Check containers
  if ! check_containers; then
    errors=$((errors + 1))
  fi
  echo ""
  
  # Summary
  echo "=========================================="
  if [[ $errors -eq 0 ]]; then
    log_success "All checks passed!"
    exit 0
  else
    log_error "Found $errors issue(s)"
    log_info "Build OSRM data manually via osrm-management-system API"
    log_info "  curl -X POST http://localhost:21520/api/v1/generate/osrm-v2"
    exit 1
  fi
}

main "$@"
