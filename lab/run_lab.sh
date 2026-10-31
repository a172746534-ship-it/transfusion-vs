#!/usr/bin/env bash
# Runs on the CI emulator. One suite failing never stops the next: every result is uploaded and
# read by a human (me) afterwards; the score comes from results.json files, not the exit code.
set -u
API=$1
ONLY=${2:-}
ROBUST=${3:-true}
OUT=lab-results/api$API
mkdir -p "$OUT"

python3 lab/reliability.py --apk apks/debug/app-debug.apk --out "$OUT/reliability" --only "$ONLY" 2>&1 | tee "$OUT/reliability.log"

if [ "$ROBUST" = "true" ]; then
  bash lab/robustness.sh "$API" "$OUT/robustness" 2>&1 | tee "$OUT/robustness.log"
fi
exit 0
