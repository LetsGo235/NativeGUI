#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if command -v gradle >/dev/null; then
  gradle --no-daemon build
else
  mkdir -p .tools
  if [ ! -x .tools/gradle-9.2.1/bin/gradle ]; then
    curl -fL 'https://services.gradle.org/distributions/gradle-9.2.1-bin.zip' -o .tools/gradle.zip
    unzip -qo .tools/gradle.zip -d .tools
    rm .tools/gradle.zip
  fi
  .tools/gradle-9.2.1/bin/gradle --no-daemon build
fi
echo "Installable JAR: build/libs/"
