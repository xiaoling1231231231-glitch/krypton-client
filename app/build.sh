#!/usr/bin/env bash
# Builds Krypton Client.app as a native macOS app and copies it to /Applications.
set -e

HERE="$(cd "$(dirname "$0")" && pwd)"
APP_NAME="Krypton Client"
BUILD_DIR="$HERE/build"
CONTENTS="$BUILD_DIR/$APP_NAME.app/Contents"
EXEC="$CONTENTS/MacOS/KryptonClient"
ICONS="$HERE/icons"
MACOS="$CONTENTS/MacOS"
RES="$CONTENTS/Resources"

echo "==> Compiling Swift app..."
rm -rf "$BUILD_DIR"
mkdir -p "$MACOS" "$RES"

# Add -sdk to ensure we target the installed SDK
SDK="$(xcrun --sdk macosx --show-sdk-path)"
swiftc -O "$HERE/main.swift" \
  -sdk "$SDK" \
  -framework Cocoa -framework WebKit \
  -o "$EXEC" 2>&1 | grep -vE "^warning|deprecated|unused" || true
[ -x "$EXEC" ] || { echo "compilation failed"; exit 1; }
echo "    binary compiled"

echo "==> Assembling app bundle..."
cat > "$CONTENTS/Info.plist" <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleName</key><string>Krypton Client</string>
    <key>CFBundleDisplayName</key><string>Krypton Client</string>
    <key>CFBundleIdentifier</key><string>com.krypton.client</string>
    <key>CFBundleVersion</key><string>1.0.0</string>
    <key>CFBundleShortVersionString</key><string>1.0.0</string>
    <key>CFBundleExecutable</key><string>KryptonClient</string>
    <key>CFBundlePackageType</key><string>APPL</string>
    <key>LSMinimumSystemVersion</key><string>11.0</string>
    <key>NSHighResolutionCapable</key><true/>
    <key>LSUIElement</key><false/>
</dict>
</plist>
PLIST

if [ -f "$ICONS/app.icns" ]; then
  cp "$ICONS/app.icns" "$RES/KryptonClient.icns"
  /usr/libexec/PlistBuddy -c "Add :CFBundleIconFile string KryptonClient.icns" "$CONTENTS/Info.plist" 2>/dev/null || true
  echo "    icon added"
fi

echo "==> Codesigning (ad-hoc) so macOS launches it..."
codesign --force --deep --sign - "$BUILD_DIR/$APP_NAME.app" 2>&1 | grep -v "satisfies its Designated Requirement" || true

echo "==> Copying to /Applications..."
if [ -d "/Applications/$APP_NAME.app" ]; then
  rm -rf "/Applications/$APP_NAME.app"
fi
cp -R "$BUILD_DIR/$APP_NAME.app" "/Applications/"
echo "    installed to /Applications/$APP_NAME.app"

echo ""
echo "Done. Launch it with:  open \"/Applications/$APP_NAME.app\""