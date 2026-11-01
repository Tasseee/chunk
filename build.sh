#!/usr/bin/env sh
set -eu
if ! command -v gradle >/dev/null 2>&1; then
  echo "Gradle was not found. Install Gradle and make sure it is on PATH." >&2
  exit 1
fi
exec gradle --no-daemon clean build
