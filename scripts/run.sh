#!/usr/bin/env sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
JAVA_BIN=${JAVA_HOME:+"$JAVA_HOME/bin/"}
mkdir -p "$ROOT/target/native/classes"
find "$ROOT/src/main/java/frogger/core" "$ROOT/src/main/java/frogger/desktop" -name '*.java' -exec printf '"%s"\n' {} + > "$ROOT/target/native/sources.txt"
"${JAVA_BIN}javac" -encoding UTF-8 --release 17 -d "$ROOT/target/native/classes" @"$ROOT/target/native/sources.txt"
"${JAVA_BIN}jar" --create --file "$ROOT/target/frogger-desktop.jar" --main-class frogger.desktop.Main -C "$ROOT/target/native/classes" . -C "$ROOT" LICENSE
if [ "${1:-}" = '--build-only' ]; then
  printf '%s\n' "Built $ROOT/target/frogger-desktop.jar"
else
  "${JAVA_BIN}java" -jar "$ROOT/target/frogger-desktop.jar" "$@"
fi
