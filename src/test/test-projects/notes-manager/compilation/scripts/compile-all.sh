#!/usr/bin/env bash

set -euo pipefail

MAVEN_HOME="${MAVEN_HOME}"
MVN_CMD="$MAVEN_HOME/bin/mvn"

usage() {
	echo "Usage: $0 [-- <extra maven args>]"
	echo
	echo "Runs full Maven compile for the project root containing pom.xml."
}

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

if [[ ${1:-} == "-h" || ${1:-} == "--help" ]]; then
	usage
	exit 0
fi

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

cd "$PROJECT_ROOT"

TMP_OUTPUT="$(mktemp)"
trap 'rm -f "$TMP_OUTPUT"' EXIT

set +e
"$MVN_CMD" "${EXTRA_MVN_ARGS[@]}" \
	-DskipTests -DskipReins -Dmaven.compiler.useIncrementalCompilation=false \
	compile \
	>"$TMP_OUTPUT" 2>&1
MVN_STATUS=$?
set -e

if [[ $MVN_STATUS -eq 0 ]]; then
	echo "Successful compilation"
	exit 0
fi

grep -E '^(\[ERROR\]|.* error: )' "$TMP_OUTPUT" || true
exit "$MVN_STATUS"
