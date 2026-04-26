#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Error: '$1' is required but not installed or not in PATH." >&2
    exit 1
  fi
}

require_cmd java
require_cmd javac
require_cmd find

DEPS_DIR="$ROOT_DIR/target/deps"
CLASSES_DIR="$ROOT_DIR/target/classes"
BUILD_SRC_DIR="$ROOT_DIR/target/build-src"
SOURCES_LIST="$BUILD_SRC_DIR/sources.list"
LOCAL_SWIFTBOT_JAR="$ROOT_DIR/SwiftBot-API-6.0.0.jar"

if [[ -f "$ROOT_DIR/com/labs/Task5.java" ]]; then
  SOURCE_LAYOUT="nested"
elif [[ -f "$ROOT_DIR/Task5.java" ]]; then
  SOURCE_LAYOUT="flat"
else
  echo "Error: Task5.java not found (expected com/labs/Task5.java or Task5.java)." >&2
  exit 1
fi

echo "[1/4] Resolving dependencies..."
mkdir -p "$DEPS_DIR"

if [[ -f "$ROOT_DIR/pom.xml" ]] && command -v mvn >/dev/null 2>&1; then
  mvn -q -f "$ROOT_DIR/pom.xml" dependency:copy-dependencies \
    -DoutputDirectory="$DEPS_DIR" \
    -DincludeScope=runtime
elif [[ -f "$LOCAL_SWIFTBOT_JAR" ]]; then
  cp -f "$LOCAL_SWIFTBOT_JAR" "$DEPS_DIR/"
else
  echo "Error: dependencies unavailable. Provide pom.xml + mvn, or place SwiftBot-API-6.0.0.jar in repo root." >&2
  exit 1
fi

echo "[2/4] Staging sources..."
rm -rf "$BUILD_SRC_DIR"
mkdir -p "$BUILD_SRC_DIR/com/labs"

if [[ "$SOURCE_LAYOUT" == "nested" ]]; then
  cp -R "$ROOT_DIR/com" "$BUILD_SRC_DIR/"
else
  cp "$ROOT_DIR/Task5.java" "$BUILD_SRC_DIR/com/labs/Task5.java"

  for student_dir in Ainesh Daniel Ferdous Hosna Janika Laur Lawrence Miye Ruben; do
    if [[ -d "$ROOT_DIR/$student_dir" ]]; then
      cp -R "$ROOT_DIR/$student_dir" "$BUILD_SRC_DIR/com/labs/$student_dir"
    fi
  done
fi

# Fix filename mismatch: Assignment3.java contains public class Assignment3.
if [[ -f "$BUILD_SRC_DIR/com/labs/Ferdous/task6.java" ]]; then
  mv "$BUILD_SRC_DIR/com/labs/Ferdous/task6.java" "$BUILD_SRC_DIR/com/labs/Ferdous/Assignment3.java"
fi
rm -f "$BUILD_SRC_DIR/com/labs/Ferdous/task6.java"

# Fix missing package declaration used by Task5 import.
DANCE_FILE="$BUILD_SRC_DIR/com/labs/Miye/SwiftBotDance.java"
if [[ -f "$DANCE_FILE" ]] && ! grep -qE '^package\s+com\.labs\.Miye;' "$DANCE_FILE"; then
  TMP_FILE="$(mktemp)"
  {
    echo "package com.labs.Miye;"
    echo
    cat "$DANCE_FILE"
  } > "$TMP_FILE"
  mv "$TMP_FILE" "$DANCE_FILE"
fi

find "$BUILD_SRC_DIR/com" -name "*.java" ! -path "*/Ferdous/task6.java" | sort > "$SOURCES_LIST"
if [[ ! -s "$SOURCES_LIST" ]]; then
  echo "Error: no Java source files found under com/." >&2
  exit 1
fi

echo "[3/4] Compiling Java sources..."
mkdir -p "$CLASSES_DIR"
javac -cp "$DEPS_DIR/*" -d "$CLASSES_DIR" @"$SOURCES_LIST"

echo "[4/4] Running com.labs.Task5..."
exec java -cp "$CLASSES_DIR:$DEPS_DIR/*" com.labs.Task5 "$@"

