#!/bin/bash

# Test script to simulate CI environment and verify setup
echo "🧪 Testing CI setup locally..."

# Step 1: Create mock google-services.json (like CI does)
echo "📄 Creating mock google-services.json..."
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

# Step 2: Run tests
echo "🧪 Running unit tests..."
./gradlew testDebugUnitTest --stacktrace

if [ $? -eq 0 ]; then
    echo "✅ Unit tests passed!"
else
    echo "❌ Unit tests failed!"
    exit 1
fi

# Step 3: Run lint
echo "🔍 Running lint checks..."
./gradlew lintDebug --stacktrace

if [ $? -eq 0 ]; then
    echo "✅ Lint checks passed!"
else
    echo "❌ Lint checks failed!"
    exit 1
fi

# Step 4: Build APK
echo "🔨 Building debug APK..."
./gradlew assembleDebug --stacktrace

if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
else
    echo "❌ Build failed!"
    exit 1
fi

# Cleanup
echo "🧹 Cleaning up mock file..."
rm app/google-services.json

echo "🎉 All CI checks passed! Your GitHub Actions should work now." 