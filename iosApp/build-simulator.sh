#!/bin/bash
# Builds Vocabee for the iOS simulator without an Xcode project:
# Kotlin framework via Gradle + a thin Swift shell via swiftc.
set -euo pipefail

cd "$(dirname "$0")/.."

FRAMEWORK_DIR="app/build/bin/iosSimulatorArm64/debugFramework"
APP_DIR="iosApp/build/VocabeeApp.app"

./gradlew :app:linkDebugFrameworkIosSimulatorArm64

rm -rf "$APP_DIR"
mkdir -p "$APP_DIR"

xcrun -sdk iphonesimulator swiftc \
  -target arm64-apple-ios15.0-simulator \
  -parse-as-library \
  -F "$FRAMEWORK_DIR" \
  -framework Vocabee \
  -lc++ \
  -lsqlite3 \
  -o "$APP_DIR/VocabeeApp" \
  iosApp/Sources/iOSApp.swift

cp iosApp/Info.plist "$APP_DIR/Info.plist"
codesign --force --sign - "$APP_DIR"

echo "Built: $APP_DIR"
