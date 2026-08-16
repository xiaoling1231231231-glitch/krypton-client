#!/usr/bin/env bash
# Build the Krypton mod and start the launcher.
set -e
cd "$(dirname "$0")"

GRADLE="${GRADLE:-$HOME/.gradle/wrapper/dists/gradle-9.5.0-bin/bvnork1r7n8i6kp5cnkibsc9q/gradle-9.5.0/bin/gradle}"
if [ -x "$GRADLE" ]; then
  echo "==> Building Krypton mod..."
  (cd mod && "$GRADLE" build -q)
else
  echo "Gradle 9.5 not found at $GRADLE. Skipping mod build."
fi

echo "==> Starting launcher at http://localhost:5757"
cd launcher && node server.js