#!/bin/bash
set -euo pipefail

# Setup Android app signing configuration
# Usage: setup-signing.sh [keystore_base64] [keystore_password] [key_alias] [key_password]

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
APP_DIR="$PROJECT_DIR/app"
KEYSTORE_PATH="$APP_DIR/release.keystore"
KEYSTORE_PROPERTIES="$APP_DIR/keystore.properties"
BUILD_GRADLE="$APP_DIR/build.gradle"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Parse arguments or environment variables
KEYSTORE_BASE64="${1:-${KEYSTORE_BASE64:-}}"
KEYSTORE_PASSWORD="${2:-${KEYSTORE_PASSWORD:-}}"
KEY_ALIAS="${3:-${KEY_ALIAS:-}}"
KEY_PASSWORD="${4:-${KEY_PASSWORD:-}}"

# Setup keystore
setup_keystore() {
    # Check if real keystore secrets are provided
    if [ -n "$KEYSTORE_BASE64" ] && [ -n "$KEYSTORE_PASSWORD" ] && [ -n "$KEY_ALIAS" ] && [ -n "$KEY_PASSWORD" ]; then
        log_info "Using provided keystore from secrets"
        echo "$KEYSTORE_BASE64" | base64 -d > "$KEYSTORE_PATH"
        
        KEYSTORE_FILE="release.keystore"
        STORE_PASSWORD="$KEYSTORE_PASSWORD"
        KEY_ALIAS_VALUE="$KEY_ALIAS"
        KEY_PASSWORD_VALUE="$KEY_PASSWORD"
    else
        log_warn "No keystore secrets found, creating mock keystore for signing"
        
        # Create mock keystore
        keytool -genkey -v \
            -keystore "$KEYSTORE_PATH" \
            -alias release-key \
            -keyalg RSA \
            -keysize 2048 \
            -validity 10000 \
            -storepass damnmanweshouldpassthisshit \
            -keypass damnmanweshouldpassthisshit \
            -dname "CN=Delivery App, OU=Development, O=DS, L=City, ST=State, C=VN" \
            -noprompt 2>/dev/null || {
            log_error "Failed to create mock keystore"
            exit 1
        }
        
        KEYSTORE_FILE="release.keystore"
        STORE_PASSWORD="damnmanweshouldpassthisshit"
        KEY_ALIAS_VALUE="release-key"
        KEY_PASSWORD_VALUE="damnmanweshouldpassthisshit"
    fi
    
    # Create keystore.properties in app directory
    cat > "$KEYSTORE_PROPERTIES" << EOF
storeFile=$KEYSTORE_FILE
storePassword=$STORE_PASSWORD
keyAlias=$KEY_ALIAS_VALUE
keyPassword=$KEY_PASSWORD_VALUE
EOF
    
    log_info "Keystore setup completed"
}

# Setup signing in build.gradle
setup_build_gradle() {
    cd "$PROJECT_DIR"
    
    if [ ! -f "$BUILD_GRADLE" ]; then
        log_error "build.gradle not found at $BUILD_GRADLE"
        exit 1
    fi
    
    # Check if signing already configured
    if grep -q "signingConfigs" "$BUILD_GRADLE"; then
        log_info "Signing config already exists in build.gradle"
        return 0
    fi
    
    log_info "Setting up signing configuration in build.gradle"
    
    # Create Python script to modify build.gradle
    python3 << PYTHON_EOF
import re
import sys
import os

# Change to project directory
os.chdir('$PROJECT_DIR')
build_gradle_path = 'app/build.gradle'

try:
    with open(build_gradle_path, 'r') as f:
        content = f.read()
    
    # Add keystore properties loading before android block
    keystore_code = '''def keystorePropertiesFile = rootProject.file("app/keystore.properties")
def keystoreProperties = new Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(new FileInputStream(keystorePropertiesFile))
}

'''
    content = re.sub(r'^(android \{)', keystore_code + r'\1', content, flags=re.MULTILINE)
    
    # Add signingConfigs after namespace
    signing_code = '''
    signingConfigs {
        release {
            if (keystorePropertiesFile.exists()) {
                storeFile file(keystoreProperties['storeFile'])
                storePassword keystoreProperties['storePassword']
                keyAlias keystoreProperties['keyAlias']
                keyPassword keystoreProperties['keyPassword']
            }
        }
    }
'''
    content = re.sub(r'(    namespace [^\n]+)', r'\1' + signing_code, content)
    
    # Update release buildType to use signing
    release_match = re.search(r'(        release \{)([^}]+)(\})', content, re.DOTALL)
    if release_match:
        release_content = release_match.group(2)
        new_release = f'''        release {{
            if (keystorePropertiesFile.exists()) {{
                signingConfig signingConfigs.release
            }}{release_content}
        }}'''
        content = content.replace(release_match.group(0), new_release)
    
    with open(build_gradle_path, 'w') as f:
        f.write(content)
    
    print("Signing configuration added to build.gradle")
    sys.exit(0)
except Exception as e:
    print(f"Error setting up signing: {e}")
    sys.exit(1)
PYTHON_EOF
    
    if [ $? -ne 0 ]; then
        log_error "Failed to setup signing in build.gradle"
        exit 1
    fi
    
    log_info "Signing configuration added to build.gradle"
}

# Main execution
main() {
    log_info "Starting signing setup..."
    
    # Change to project directory
    cd "$PROJECT_DIR"
    
    # Setup keystore
    setup_keystore
    
    # Setup build.gradle
    setup_build_gradle
    
    log_info "Signing setup completed successfully!"
}

main "$@"
