#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAR="$(ls "$ROOT"/build/libs/DonutLeaderboard-*.jar 2>/dev/null | head -1)"
MATRIX="${DONUT_LEADERBOARD_SMOKE_MATRIX:-1.20.1,26.3}"
ENABLED="${DONUT_LEADERBOARD_SMOKE_ENABLED:-false}"

if [[ "$ENABLED" != "true" ]]; then
  echo "Smoke harness ready. Set DONUT_LEADERBOARD_SMOKE_ENABLED=true to download Paper/Folia jars and boot servers."
  echo "Planned matrix: $MATRIX"
  exit 0
fi

if [[ -z "$JAR" ]]; then
  echo "Build the plugin first: ./gradlew build"
  exit 1
fi

echo "Smoke boot is environment-specific (Java 25 for Paper 26.3, separate Folia builds)."
echo "Copy $JAR into each server's plugins/ folder and verify clean enable in logs."
echo "This script documents the matrix; automated boot requires run-paper/run-task wiring per version."

for version in ${MATRIX//,/ }; do
  echo " - $version"
done

exit 0
