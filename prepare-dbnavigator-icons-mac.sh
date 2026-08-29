#!/usr/bin/env bash
set -euo pipefail

SRC="${1:-src/main/resources/images/DN.png}"
OUT="src/main/resources/packaging"

echo "========================================"
echo " DBNavigator Icon Generator"
echo "========================================"

if ! command -v magick >/dev/null 2>&1; then
    echo "ERROR: ImageMagick is not installed."
    echo "Install with: brew install imagemagick"
    exit 1
fi

if ! command -v iconutil >/dev/null 2>&1; then
    echo "ERROR: iconutil is not available."
    exit 1
fi

if [[ ! -f "$SRC" ]]; then
    echo "ERROR: Source image not found:"
    echo "$SRC"
    exit 1
fi

mkdir -p "$OUT"

MASTER="$OUT/DBNavigator-master.png"
ICONSET="$OUT/DBNavigator.iconset"

rm -rf "$ICONSET"
rm -f "$MASTER"
rm -f "$OUT/DBNavigator.png"
rm -f "$OUT/DBNavigator.ico"
rm -f "$OUT/DBNavigator.icns"

echo
echo "Source: $SRC"
echo "Output: $OUT"
echo

# ============================================================
# STEP 1
# Remove ONLY the connected background surrounding the icon.
#
# We intentionally do NOT remove all black/dark pixels.
# This preserves the dark DBNavigator artwork.
# ============================================================

echo "[1/3] Removing outside background..."

magick "$SRC" \
    -alpha on \
    -bordercolor none \
    -border 1 \
    -fuzz 15% \
    -fill none \
    -draw "color 0,0 floodfill" \
    -draw "color 100%,0 floodfill" \
    -draw "color 0,100% floodfill" \
    -draw "color 100%,100% floodfill" \
    -shave 1x1 \
    -trim \
    +repage \
    -gravity center \
    -resize '900x900' \
    -extent 1024x1024 \
    "$MASTER"

# ============================================================
# STEP 2
# Clean any remaining connected edge background.
# This catches dark/black pixels that may surround the artwork.
# It does NOT globally remove black.
# ============================================================

echo "[2/3] Cleaning outer edge..."

magick "$MASTER" \
    -alpha on \
    -fuzz 12% \
    -fill none \
    -draw "color 0,0 floodfill" \
    -draw "color 1023,0 floodfill" \
    -draw "color 0,1023 floodfill" \
    -draw "color 1023,1023 floodfill" \
    "$MASTER"

# Linux PNG
cp "$MASTER" "$OUT/DBNavigator.png"

# Windows ICO
magick "$MASTER" \
    -define icon:auto-resize=16,24,32,48,64,128,256 \
    "$OUT/DBNavigator.ico"

# ============================================================
# STEP 3
# Create macOS ICNS
# ============================================================

echo "[3/3] Creating macOS ICNS..."

mkdir -p "$ICONSET"

for SIZE in 16 32 128 256 512; do
    DOUBLE=$((SIZE * 2))

    magick "$MASTER" \
        -alpha on \
        -background none \
        -resize "${SIZE}x${SIZE}" \
        -gravity center \
        -extent "${SIZE}x${SIZE}" \
        "$ICONSET/icon_${SIZE}x${SIZE}.png"

    magick "$MASTER" \
        -alpha on \
        -background none \
        -resize "${DOUBLE}x${DOUBLE}" \
        -gravity center \
        -extent "${DOUBLE}x${DOUBLE}" \
        "$ICONSET/icon_${SIZE}x${SIZE}@2x.png"
done

iconutil \
    -c icns \
    "$ICONSET" \
    -o "$OUT/DBNavigator.icns"

rm -rf "$ICONSET"
rm -f "$MASTER"

echo
echo "========================================"
echo " DBNavigator icons generated"
echo "========================================"

echo
echo "Windows:"
echo "  $OUT/DBNavigator.ico"

echo
echo "Linux:"
echo "  $OUT/DBNavigator.png"

echo
echo "macOS:"
echo "  $OUT/DBNavigator.icns"

echo
echo "Checking PNG corner..."

magick "$OUT/DBNavigator.png" \
    -format "%[pixel:p{0,0}]\n" \
    info:

echo
ls -lh \
    "$OUT/DBNavigator.png" \
    "$OUT/DBNavigator.ico" \
    "$OUT/DBNavigator.icns"

echo
echo "DONE."