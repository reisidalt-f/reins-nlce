#!/usr/bin/env bash

set -euo pipefail

MAVEN_HOME="${MAVEN_HOME}"
MVN_CMD="$MAVEN_HOME/bin/mvn"

usage() {
  echo "Usage: $0 <path-to-java-file> [-- <extra maven args>]"
  echo
  echo "Compiles one Java source file using the same compiler and compile classpath"
  echo "configured by this project's pom.xml compile phase."
}

if [[ $# -lt 1 ]]; then
  usage
  exit 1
fi

find_project_root() {
  local dir="$1"
  while [[ "$dir" != "/" ]]; do
    if [[ -f "$dir/pom.xml" ]]; then
      echo "$dir"
      return 0
    fi
    dir="$(dirname "$dir")"
  done
  return 1
}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(find_project_root "$SCRIPT_DIR" || true)"

if [[ -z "$PROJECT_ROOT" ]]; then
  PROJECT_ROOT="$(find_project_root "$PWD" || true)"
fi

if [[ -z "$PROJECT_ROOT" ]]; then
  echo "Error: could not locate project root containing pom.xml." >&2
  exit 1
fi
INPUT_PATH="$1"
JAVA_FILE="$INPUT_PATH"
shift || true

SOURCE_BASE="$PROJECT_ROOT/target/generated-sources/reins"
TEST_BASE="$PROJECT_ROOT/target/generated-test-sources/reins"
MAIN_OUTPUT="$PROJECT_ROOT/target/classes"
TEST_OUTPUT="$PROJECT_ROOT/target/test-classes"

EXTRA_MVN_ARGS=()
if [[ $# -gt 0 ]]; then
  if [[ ${1:-} == "--" ]]; then
    shift
  fi
  EXTRA_MVN_ARGS=("$@")
fi

if [[ ! -x "$MVN_CMD" ]]; then
  echo "Error: Maven executable not found or not executable: $MVN_CMD" >&2
  echo "Set MAVEN_HOME to your Maven installation directory." >&2
  exit 1
fi

if [[ ! -f "$JAVA_FILE" ]]; then
  JAVA_FILE="$PROJECT_ROOT/$JAVA_FILE"
fi

if [[ ! -f "$JAVA_FILE" ]]; then
  JAVA_FILE="$SOURCE_BASE/$INPUT_PATH"
fi

if [[ ! -f "$JAVA_FILE" ]]; then
  JAVA_FILE="$TEST_BASE/$INPUT_PATH"
fi

if [[ ! -f "$JAVA_FILE" ]]; then
  echo "Error: Java file not found: $INPUT_PATH" >&2
  exit 1
fi

JAVA_FILE="$(cd "$(dirname "$JAVA_FILE")" && pwd)/$(basename "$JAVA_FILE")"

if [[ "$JAVA_FILE" == "$SOURCE_BASE"/* ]]; then
  RELATIVE="${JAVA_FILE#"$SOURCE_BASE/"}"
  OUTPUT_BASE="$MAIN_OUTPUT"
  # generate-sources registers the generated source root via build-helper:add-source;
  # without it, compiler:compile finds no source roots and silently compiles nothing.
  COMPILER_GOALS=(generate-sources compiler:compile)
elif [[ "$JAVA_FILE" == "$TEST_BASE"/* ]]; then
  RELATIVE="${JAVA_FILE#"$TEST_BASE/"}"
  OUTPUT_BASE="$TEST_OUTPUT"
  COMPILER_GOALS=(generate-test-sources compiler:testCompile)
else
  echo "Error: file must be under $SOURCE_BASE or $TEST_BASE." >&2
  echo "Given: $JAVA_FILE" >&2
  exit 1
fi

cd "$PROJECT_ROOT"

mkdir -p "$OUTPUT_BASE"

TMP_OUTPUT="$(mktemp)"
trap 'rm -f "$TMP_OUTPUT"' EXIT

set +e
"$MVN_CMD" "${EXTRA_MVN_ARGS[@]}" \
  -DskipTests -DskipReins -Dmaven.compiler.useIncrementalCompilation=false \
  -Dmaven.compiler.outputDirectory="$OUTPUT_BASE" \
  -Dmaven.compiler.includes="$RELATIVE" \
  "${COMPILER_GOALS[@]}" \
  >"$TMP_OUTPUT" 2>&1
MVN_STATUS=$?
set -e

if [[ $MVN_STATUS -eq 0 ]]; then
  echo "Successful compilation"
  exit 0
fi

cat "$TMP_OUTPUT"
exit "$MVN_STATUS"
