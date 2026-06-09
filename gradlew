#!/usr/bin/env sh
set -eu

GRADLE_VERSION=8.11.1
BASE_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
DIST_DIR="$BASE_DIR/.gradle/bootstrap/gradle-$GRADLE_VERSION"
DIST_ZIP="$BASE_DIR/.gradle/bootstrap/gradle-$GRADLE_VERSION-bin.zip"
GRADLE_EXE="$DIST_DIR/bin/gradle"

if [ ! -x "$GRADLE_EXE" ]; then
  mkdir -p "$BASE_DIR/.gradle/bootstrap"
  echo "Downloading Gradle $GRADLE_VERSION..."
  if command -v curl >/dev/null 2>&1; then
    curl -L "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$DIST_ZIP"
  else
    wget "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -O "$DIST_ZIP"
  fi
  unzip -o "$DIST_ZIP" -d "$BASE_DIR/.gradle/bootstrap" >/dev/null
fi

exec "$GRADLE_EXE" "$@"
