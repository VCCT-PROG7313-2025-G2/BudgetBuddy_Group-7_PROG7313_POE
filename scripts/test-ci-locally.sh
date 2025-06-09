#!/bin/bash

# Script to test CI process locally
# This mimics what happens in GitHub Actions

set -e  # Exit on any error

echo "🚀 Starting local CI test simulation..."

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Check if we're in the right directory
if [ ! -f "gradlew" ]; then
    print_error "gradlew not found. Make sure you're in the project root directory."
    exit 1
fi

print_status "Found gradlew in current directory"

# Create mock google-services.json if it doesn't exist
if [ ! -f "app/google-services.json" ]; then
    print_warning "Creating mock google-services.json file..."
    cat > app/google-services.json << 'EOF'
{
  "project_info": {
    "project_number": "000000000000",
    "project_id": "mock-project-id"
  },
  "client": [
    {
      "client_info": {
        "mobilesdk_app_id": "1:000000000000:android:0000000000000000000000",
        "android_client_info": {
          "package_name": "com.example.budgetbuddy"
        }
      },
      "oauth_client": [],
      "api_key": [
        {
          "current_key": "mock-api-key"
        }
      ],
      "services": {
        "appinvite_service": {
          "other_platform_oauth_client": []
        }
      }
    }
  ],
  "configuration_version": "1"
}
EOF
    print_status "Mock google-services.json created"
else
    print_status "google-services.json already exists"
fi

# Make gradlew executable
chmod +x gradlew
print_status "Made gradlew executable"

# Step 1: Clean
echo -e "\n${YELLOW}🧹 Step 1: Cleaning project...${NC}"
./gradlew clean
print_status "Clean completed"

# Step 2: Run unit tests
echo -e "\n${YELLOW}🧪 Step 2: Running unit tests...${NC}"
./gradlew testDebugUnitTest --continue --stacktrace
if [ -d "app/build/reports/tests/testDebugUnitTest" ]; then
    print_status "Unit tests completed and reports generated"
else
    print_error "Unit test reports not found!"
    exit 1
fi

# Step 3: Run lint
echo -e "\n${YELLOW}🔍 Step 3: Running lint analysis...${NC}"
./gradlew lintDebug --continue --stacktrace
if [ -f "app/build/reports/lint-results-debug.html" ]; then
    print_status "Lint analysis completed and report generated"
else
    print_error "Lint results not found!"
    exit 1
fi

# Step 4: Build APK
echo -e "\n${YELLOW}🔨 Step 4: Building debug APK...${NC}"
./gradlew assembleDebug --stacktrace
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    print_status "APK build completed successfully"
    ls -la app/build/outputs/apk/debug/
else
    print_error "APK file not found!"
    exit 1
fi

echo -e "\n${GREEN}🎉 All CI steps completed successfully!${NC}"
echo -e "${GREEN}Your project should now pass GitHub Actions CI.${NC}"

# Optional: Open test reports
if command -v open >/dev/null 2>&1; then
    echo -e "\n${YELLOW}Opening test reports...${NC}"
    open app/build/reports/tests/testDebugUnitTest/index.html 2>/dev/null || true
    open app/build/reports/lint-results-debug.html 2>/dev/null || true
fi 