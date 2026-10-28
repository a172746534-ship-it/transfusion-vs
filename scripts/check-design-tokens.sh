#!/usr/bin/env bash
# Fails if a screen hard-codes a style instead of using the design system.
# Allowed only in: ui/theme (the tokens) and ui/components (the shared composables).
set -euo pipefail
cd "$(dirname "$0")/.."
SRC=app/src/main/java/app/transfusionvs

files=$(find "$SRC" -name '*.kt' -not -path "*/ui/theme/*" -not -path "*/ui/components/*")
pattern='\b[0-9]+(\.[0-9]+)?\.sp\b|\b([1-9][0-9]*(\.[0-9]+)?|0\.[0-9]*[1-9][0-9]*)\.dp\b|Color\(0x|\.copy\(alpha|RoundedCornerShape\(|\bColor\.(White|Black|Red|Green|Blue|Yellow|Gray|Cyan|Magenta)\b'

if hits=$(grep -nE "$pattern" $files); then
  echo "::error::Hard-coded style outside the design system (use TvsFont / TvsSpace / TvsSize / TvsShape / Tvs colours):"
  echo "$hits"
  exit 1
fi
echo "Design-token guard: OK (no hard-coded colours, font sizes, spacing or radii in screens)."
