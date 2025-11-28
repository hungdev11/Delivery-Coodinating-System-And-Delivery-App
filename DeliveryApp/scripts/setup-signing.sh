#!/bin/bash
set -euo pipefail

# Setup Android app signing configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
APP_DIR="$PROJECT_DIR/app"
KEYSTORE_PATH="$APP_DIR/release.keystore"
KEYSTORE_PROPERTIES="$APP_DIR/keystore.properties"
BUILD_GRADLE="$APP_DIR/build.gradle"

log_info() {
    echo "[INFO] $1"
}

log_warn() {
    echo "[WARN] $1"
}

log_error() {
    echo "[ERROR] $1"
}

# Read environment variables
KEYSTORE_BASE64="${KEYSTORE_BASE64:-}"
KEYSTORE_PASSWORD="${KEYSTORE_PASSWORD:-}"
KEY_ALIAS="${KEY_ALIAS:-}"
KEY_PASSWORD="${KEY_PASSWORD:-}"

# Setup keystore
setup_keystore() {
    cd "$APP_DIR"
    
    if [ -n "$KEYSTORE_BASE64" ] && [ -n "$KEYSTORE_PASSWORD" ] && [ -n "$KEY_ALIAS" ] && [ -n "$KEY_PASSWORD" ]; then
        log_info "Using provided keystore from secrets"
        echo "$KEYSTORE_BASE64" | base64 -d > "$KEYSTORE_PATH"
        STORE_PASSWORD="$KEYSTORE_PASSWORD"
        KEY_ALIAS_VALUE="$KEY_ALIAS"
        KEY_PASSWORD_VALUE="$KEY_PASSWORD"
    else
        log_warn "No keystore secrets found, creating mock keystore"
        keytool -genkey -v \
            -keystore "$KEYSTORE_PATH" \
            -alias release-key \
            -keyalg RSA \
            -keysize 2048 \
            -validity 10000 \
            -storepass android123 \
            -keypass android123 \
            -dname "CN=Delivery App, OU=Development, O=DS, L=City, ST=State, C=VN" \
            -noprompt 2>/dev/null || true
        
        STORE_PASSWORD="android123"
        KEY_ALIAS_VALUE="release-key"
        KEY_PASSWORD_VALUE="android123"
    fi
    
    cat > "$KEYSTORE_PROPERTIES" << EOF
storeFile=release.keystore
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
        log_error "build.gradle not found"
        exit 1
    fi
    
    log_info "Setting up signing configuration in build.gradle"
    
    python3 << 'PYEOF'
import re
import sys

build_gradle = 'app/build.gradle'

try:
    with open(build_gradle, 'r') as f:
        content = f.read()
    
    # Remove ALL existing signing config completely
    # Remove keystore properties
    content = re.sub(r'^def keystorePropertiesFile.*?\n.*?keystoreProperties\.load.*?\n\n', '', content, flags=re.MULTILINE | re.DOTALL)
    
    # Remove signingConfigs block
    content = re.sub(r'\n\s*signingConfigs\s*\{[^}]*?\n\s*release\s*\{[^}]*?\n\s*\}\s*\}\s*\n', '\n', content, flags=re.DOTALL)
    
    # Remove signingConfig from release
    content = re.sub(r'\n\s*if\s*\(keystorePropertiesFile\.exists\(\)\)\s*\{[^}]*?signingConfig[^}]*?\}', '', content, flags=re.DOTALL)
    content = re.sub(r'\n\s*signingConfig\s+signingConfigs\.release\s*\n', '\n', content)
    
    # Ensure compileSdk exists
    if 'compileSdk' not in content:
        print("ERROR: compileSdk not found!")
        sys.exit(1)
    
    # Add keystore properties before android {
    if 'def keystorePropertiesFile' not in content:
        keystore_code = '''def keystorePropertiesFile = rootProject.file("app/keystore.properties")
def keystoreProperties = new Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(new FileInputStream(keystorePropertiesFile))
}

'''
        content = keystore_code + content
    
    # Add signingConfigs after compileSdk line
    if 'signingConfigs {' not in content:
        compile_sdk_pattern = r'(    compileSdk \d+\n)'
        match = re.search(compile_sdk_pattern, content)
        if match:
            pos = match.end()
            signing_block = '''    signingConfigs {
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
            content = content[:pos] + signing_block + content[pos:]
    
    # Add signingConfig to release buildType
    release_pattern = r'(        release \{)(.*?)(\n        \})'
    match = re.search(release_pattern, content, re.DOTALL)
    if match and 'signingConfig signingConfigs.release' not in match.group(2):
        release_body = match.group(2)
        # Clean any existing signingConfig from body
        release_body = re.sub(r'\s*if\s*\(keystorePropertiesFile\.exists\(\)\).*?signingConfig.*?\n', '', release_body, flags=re.DOTALL)
        release_body = re.sub(r'\s*signingConfig.*?\n', '', release_body)
        
        signing_lines = '''            if (keystorePropertiesFile.exists()) {
                signingConfig signingConfigs.release
            }
'''
        new_body = signing_lines + release_body
        content = content.replace(match.group(0), match.group(1) + new_body + match.group(3))
    
    with open(build_gradle, 'w') as f:
        f.write(content)
    
    print("Signing configuration updated successfully")
    sys.exit(0)
except Exception as e:
    import traceback
    print(f"Error: {e}")
    traceback.print_exc()
    sys.exit(1)
PYEOF
    
    if [ $? -ne 0 ]; then
        log_error "Failed to setup signing in build.gradle"
        exit 1
    fi
}

main() {
    log_info "Starting Android signing setup..."
    setup_keystore
    setup_build_gradle
    log_info "Android signing setup completed!"
}

main "$@"
