#!/bin/bash
set -euo pipefail
SRC="${1:-src/main/resources/images/DN.png}"
OUT="src/main/resources/packaging"
ICONSET="$OUT/DBNavigator.iconset"
mkdir -p "$OUT"
command -v magick >/dev/null || { echo "ImageMagick required: brew install imagemagick"; exit 1; }
command -v iconutil >/dev/null || { echo "iconutil is required; run on macOS."; exit 1; }
[ -f "$SRC" ] || { echo "Source not found: $SRC"; exit 1; }
rm -rf "$ICONSET"
mkdir -p "$ICONSET"
# Apple-style legacy macOS fallback: 824px artwork centered on a transparent 1024px canvas.
# This gives ~100px transparent margin around the artwork.
magick "$SRC" -alpha on -background none -gravity center -resize 824x824 -extent 824x824 /tmp/dbnavigator-inner.png
magick -size 1024x1024 xc:none /tmp/dbnavigator-inner.png -gravity center -composite "$OUT/DBNavigator-macos.png"
for SIZE in 16 32 128 256 512; do
  DOUBLE=$((SIZE*2))
  magick "$OUT/DBNavigator-macos.png" -resize "${SIZE}x${SIZE}" -background none -gravity center -extent "${SIZE}x${SIZE}" "$ICONSET/icon_${SIZE}x${SIZE}.png"
  magick "$OUT/DBNavigator-macos.png" -resize "${DOUBLE}x${DOUBLE}" -background none -gravity center -extent "${DOUBLE}x${DOUBLE}" "$ICONSET/icon_${SIZE}x${SIZE}@2x.png"
done
rm -f "$OUT/DBNavigator.icns"
iconutil -c icns "$ICONSET" -o "$OUT/DBNavigator.icns"
rm -rf "$ICONSET" /tmp/dbnavigator-inner.png
# Linux and Windows assets remain full artwork; macOS gets the inset version above.
magick "$SRC" -alpha on -background none -gravity center -resize 1024x1024 -extent 1024x1024 "$OUT/DBNavigator.png"
magick "$OUT/DBNavigator.png" -define icon:auto-resize=16,24,32,48,64,128,256 "$OUT/DBNavigator.ico"
echo "Created $OUT/DBNavigator.icns"
echo "Created $OUT/DBNavigator.png"
echo "Created $OUT/DBNavigator.ico"
