#!/usr/bin/env bash
# Shared build helper for this project (used by every module owner).
# JDK 21 is required by AGP 8.13; the Android SDK lives in /home/codespace/android-sdk.
set -euo pipefail

export JAVA_HOME="${JAVA_HOME_OVERRIDE:-/usr/local/sdkman/candidates/java/21.0.12+1-ms}"
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="/home/codespace/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"

cd "$(dirname "$0")/.."

case "${1:-assemble}" in
  assemble|build)    exec ./gradlew :app:assembleDebug --console=plain --no-daemon "${@:2}" ;;
  release)           exec ./gradlew :app:assembleRelease --console=plain --no-daemon "${@:2}" ;;
  typecheck|compile) exec ./gradlew :app:compileDebugKotlin :app:compileDebugJavaWithJavac --console=plain --no-daemon "${@:2}" ;;
  test)              exec ./gradlew :app:testDebugUnitTest --console=plain --no-daemon "${@:2}" ;;
  lint)              exec ./gradlew :app:lintDebug --console=plain --no-daemon "${@:2}" ;;
  clean)             exec ./gradlew clean --console=plain --no-daemon "${@:2}" ;;
  *)                 exec ./gradlew "$@" --console=plain --no-daemon ;;
esac
