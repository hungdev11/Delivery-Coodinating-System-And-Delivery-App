#!/bin/bash
# Download and extract GitHub release to /www/wwwroot/dss
# 
# Usage:
#   ./scripts/download-release.sh <release-url>
#
# Example:
#   ./scripts/download-release.sh https://github.com/hungdev11/Delivery-Coodinating-System-And-Delivery-App/archive/refs/tags/v1.1.1.zip

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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

# Check if URL is provided
if [ $# -eq 0 ]; then
    log_error "Release URL is required"
    echo ""
    echo "Usage: $0 <release-url>"
    echo ""
    echo "Example:"
    echo "  $0 https://github.com/hungdev11/Delivery-Coodinating-System-And-Delivery-App/archive/refs/tags/v1.1.1.zip"
    exit 1
fi

RELEASE_URL="$1"
TARGET_DIR="/www/wwwroot/dss"
TEMP_DIR=$(mktemp -d)
ZIP_FILE="${TEMP_DIR}/release.zip"

# Validate URL format
if [[ ! "$RELEASE_URL" =~ ^https?:// ]]; then
    log_error "Invalid URL format: $RELEASE_URL"
    exit 1
fi

log_info "Downloading release from: $RELEASE_URL"
log_info "Target directory: $TARGET_DIR"
log_info "Temp directory: $TEMP_DIR"

# Check if target directory exists
if [ ! -d "$TARGET_DIR" ]; then
    log_warn "Target directory does not exist, creating: $TARGET_DIR"
    mkdir -p "$TARGET_DIR"
fi

# Download release
log_info "Downloading release zip file..."
if command -v wget &> /dev/null; then
    wget -q --show-progress -O "$ZIP_FILE" "$RELEASE_URL" || {
        log_error "Failed to download release"
        rm -rf "$TEMP_DIR"
        exit 1
    }
elif command -v curl &> /dev/null; then
    curl -L -o "$ZIP_FILE" "$RELEASE_URL" || {
        log_error "Failed to download release"
        rm -rf "$TEMP_DIR"
        exit 1
    }
else
    log_error "Neither wget nor curl is available"
    rm -rf "$TEMP_DIR"
    exit 1
fi

log_success "Release downloaded successfully"

# Extract zip file
log_info "Extracting release..."
if ! command -v unzip &> /dev/null; then
    log_error "unzip is not installed. Please install it first:"
    log_error "  Ubuntu/Debian: sudo apt-get install unzip"
    log_error "  CentOS/RHEL: sudo yum install unzip"
    rm -rf "$TEMP_DIR"
    exit 1
fi

unzip -q "$ZIP_FILE" -d "$TEMP_DIR" || {
    log_error "Failed to extract release"
    rm -rf "$TEMP_DIR"
    exit 1
}

log_success "Release extracted successfully"

# Find extracted directory
EXTRACTED_DIR=$(find "$TEMP_DIR" -mindepth 1 -maxdepth 1 -type d | head -n 1)

if [ -z "$EXTRACTED_DIR" ]; then
    log_error "Could not find extracted directory"
    rm -rf "$TEMP_DIR"
    exit 1
fi

log_info "Extracted directory: $EXTRACTED_DIR"

# Remove dist folder from target directory before merging
DIST_DIR="${TARGET_DIR}/dist"
if [ -d "$DIST_DIR" ]; then
    log_info "Removing old dist folder: $DIST_DIR"
    rm -rf "$DIST_DIR"
    log_success "Dist folder removed successfully"
else
    log_info "No dist folder found, skipping removal"
fi

# Merge contents into target directory
log_info "Merging contents into $TARGET_DIR..."

# Use rsync if available (preferred method)
if command -v rsync &> /dev/null; then
    log_info "Using rsync to merge contents..."
    rsync -av --ignore-existing "$EXTRACTED_DIR/" "$TARGET_DIR/" || {
        log_warn "rsync failed, trying alternative method..."
        # Fallback to cp
        cp -r -u "$EXTRACTED_DIR"/* "$TARGET_DIR/" 2>/dev/null || true
    }
else
    log_info "Using cp to merge contents..."
    # Copy files, update only if newer
    cp -r -u "$EXTRACTED_DIR"/* "$TARGET_DIR/" 2>/dev/null || true
    
    # Copy hidden files
    find "$EXTRACTED_DIR" -maxdepth 1 -name ".*" -not -name "." -not -name ".." -exec cp -r {} "$TARGET_DIR/" \; 2>/dev/null || true
fi

log_success "Contents merged successfully"

# Cleanup
log_info "Cleaning up temporary files..."
rm -rf "$TEMP_DIR"

log_success "Release deployment completed!"
log_info "Files are now in: $TARGET_DIR"

# Show summary
echo ""
log_info "Summary:"
echo "  Release URL: $RELEASE_URL"
echo "  Target directory: $TARGET_DIR"
echo "  Status: Success"
echo ""
