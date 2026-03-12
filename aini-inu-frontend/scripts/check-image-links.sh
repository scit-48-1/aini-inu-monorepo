#!/usr/bin/env bash
set -euo pipefail

FRONTEND_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

fail() {
  echo "ASSET_CHECK_FAIL: $1" >&2
  exit 1
}

[ -d "$FRONTEND_DIR/public/images" ] || fail "Missing public/images directory"
[ -f "$FRONTEND_DIR/public/AINIINU_ROGO_B.png" ] || fail "Missing public/AINIINU_ROGO_B.png"
[ -f "$FRONTEND_DIR/public/AINIINU_ROGO_W.png" ] || fail "Missing public/AINIINU_ROGO_W.png"
[ -f "$FRONTEND_DIR/public/favicon.ico" ] || fail "Missing public/favicon.ico"

BROKEN_LINKS="$(find -L "$FRONTEND_DIR/public" -type l -print)"
[ -z "$BROKEN_LINKS" ] || fail "Broken symlink(s) detected:\n$BROKEN_LINKS"

for path in \
  "$FRONTEND_DIR/public/AINIINU_ROGO_B.png" \
  "$FRONTEND_DIR/public/AINIINU_ROGO_W.png" \
  "$FRONTEND_DIR/public/images/dog-portraits/Mixed Breed.png" \
  "$FRONTEND_DIR/public/favicon.ico"; do
  [ -e "$path" ] || fail "Missing required runtime asset: $path"
done

echo "ASSET_CHECK_OK"
