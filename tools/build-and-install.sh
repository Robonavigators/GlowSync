#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if (( $# > 1 )); then printf 'Usage: %s [ADB_SERIAL]\n' "$0" >&2; exit 2; fi
if ! command -v adb >/dev/null 2>&1; then printf 'adb not found on PATH. Install Android platform-tools.\n' >&2; exit 1; fi
serial="${1:-}"
run_adb() { if [[ -n "$serial" ]]; then adb -s "$serial" "$@"; else adb "$@"; fi; }
run_adb get-state >/dev/null
cd "$root"
./gradlew assembleDebug
run_adb install -r app/build/outputs/apk/debug/app-debug.apk
printf 'GlowSync debug APK installed. Open GlowSync on the TV to configure it.\n'
