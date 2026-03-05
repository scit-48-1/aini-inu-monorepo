#!/usr/bin/env bash
set -euo pipefail

FRONTEND_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPO_ROOT="$(cd "$FRONTEND_DIR/.." && pwd)"
COMMON_IMAGES_DIR="$REPO_ROOT/common-docs/images"

fail() {
  echo "ASSET_CHECK_FAIL: $1" >&2
  exit 1
}

[ -d "$COMMON_IMAGES_DIR" ] || fail "Missing source directory: $COMMON_IMAGES_DIR"
[ -L "$FRONTEND_DIR/public/images" ] || fail "public/images must be a symlink"
[ -L "$FRONTEND_DIR/public/AINIINU_ROGO_B.png" ] || fail "public/AINIINU_ROGO_B.png must be a symlink"
[ -L "$FRONTEND_DIR/public/AINIINU_ROGO_W.png" ] || fail "public/AINIINU_ROGO_W.png must be a symlink"
[ -L "$FRONTEND_DIR/public/favicon.ico" ] || fail "public/favicon.ico must be a symlink"

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
