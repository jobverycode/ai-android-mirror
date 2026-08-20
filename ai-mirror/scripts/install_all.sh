#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

export JAVA_HOME="${JAVA_HOME:-/Users/kele/Library/Java/JavaVirtualMachines/ms-17.0.19/Contents/Home}"
export ANDROID_HOME="${ANDROID_HOME:-/Users/kele/Library/Android/sdk}"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"

APK_PATH="$ROOT_DIR/app/build/outputs/apk/debug/app-debug.apk"

echo "🔨 Building Debug APK..."
cd "$ROOT_DIR"
./gradlew assembleDebug

if [ ! -f "$APK_PATH" ]; then
    echo "❌ APK file not found at $APK_PATH"
    exit 1
fi

DEVICES=$(adb devices | grep -w "device" | awk '{print $1}')

if [ -z "$DEVICES" ]; then
    echo "⚠️ No ADB devices connected."
    exit 0
fi

echo "📱 Found connected devices:"
echo "$DEVICES"
echo ""

for serial in $DEVICES; do
    echo "🚀 Installing to $serial..."
    adb -s "$serial" install -r "$APK_PATH"
    echo "✅ Successfully installed on $serial"
done

echo ""
echo "🎉 All devices updated successfully!"
