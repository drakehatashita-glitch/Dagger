#!/usr/bin/env bash
# Builds the DaggerSMP texture pack with 25 custom-generated 2D dagger
# textures (one per dagger) extruded into 3D in-hand via item/handheld.
#
# Pipeline:
#   1. Write a themed SVG for each dagger (curved scythe blade silhouette).
#   2. Rasterize SVG -> 64x64 PNG with ImageMagick.
#   3. Write an item/handheld model JSON for each dagger (auto 3D extrude).
#   4. Write daggersmp namespace item-model definitions used by the plugin's
#      setItemModel(NamespacedKey("daggersmp","dagger/<id>")) call.
#   5. Write a legacy CMD range_dispatch on netherite_sword.json as a fallback.
#   6. Write pack.mcmeta using the new 1.21.9+ schema (min_format/max_format).
#      Minecraft 1.21.11 = resource pack format 75.
#   7. Bundle into a .zip via `jar` (no `zip` command in this nix env).
set -euo pipefail

ROOT="DaggerSMP-TexturePack"
OUT_ZIP="DaggerSMP-TexturePack.zip"
ASSET="$ROOT/assets/minecraft"
TEX_DIR="$ASSET/textures/item/daggers"
MODEL_DIR="$ASSET/models/item/daggers"
ITEMS_DIR="$ASSET/items"
DAG_ITEMS_DIR="$ROOT/assets/daggersmp/items/dagger"

rm -rf "$ROOT" "$OUT_ZIP"
mkdir -p "$TEX_DIR" "$MODEL_DIR" "$ITEMS_DIR" "$DAG_ITEMS_DIR"

# id|cmd|main|edge|glow  (matches DaggerType.customModelData 1001..1025)
DAGGERS=(
  "strength|1001|#8B0000|#FF6840|#FF3010"
  "speed|1002|#00B8E6|#A0F4FF|#40E0FF"
  "wind|1003|#D8E8F0|#FFFFFF|#B0D8F0"
  "life|1004|#1FA838|#A8FFB0|#40FF60"
  "crimson|1005|#A8000C|#FF4030|#FF0010"
  "darkness|1006|#1A0028|#5A2080|#7A20C0"
  "hack|1007|#00B040|#A8FFB0|#00FF60"
  "frost|1008|#5AB8E0|#D8F4FF|#80E8FF"
  "mafia|1009|#A8741A|#FFCC60|#FFB020"
  "pirate|1010|#1F4A8A|#80B0E0|#3070D0"
  "void|1011|#5A0098|#C088FF|#9020E0"
  "lucky|1012|#E8B810|#FFF098|#FFD040"
  "mirror|1013|#9090A0|#F0F0FF|#C0C0D8"
  "jungle|1014|#207820|#A0E058|#40C040"
  "midas|1015|#E8A000|#FFE060|#FFB820"
  "toxic|1016|#60B820|#D8FF80|#A0FF40"
  "arachnid|1017|#202028|#605070|#A02050"
  "vampire|1018|#580010|#E03040|#A00018"
  "gravity|1019|#7868C8|#D0C0FF|#9080F0"
  "earth|1020|#5A3818|#B08858|#80502A"
  "titan|1021|#C03018|#FF8050|#FF5030"
  "guardian|1022|#1AA890|#80F0D8|#30E0C0"
  "ghost|1023|#A0B8D0|#FFFFFF|#D8E8FF"
  "chance|1024|#C030A0|#FFA0E0|#F050C0"
  "storm|1025|#E8C820|#FFF880|#FFE040"
)

write_svg() {
  local out="$1" main="$2" edge="$3" glow="$4"
  cat > "$out" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="64" height="64" viewBox="0 0 64 64" shape-rendering="crispEdges">
  <defs>
    <linearGradient id="blade" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%"  stop-color="${edge}"/>
      <stop offset="55%" stop-color="${main}"/>
      <stop offset="100%" stop-color="${glow}"/>
    </linearGradient>
    <linearGradient id="bladeEdge" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%"  stop-color="#ffffff"/>
      <stop offset="100%" stop-color="${edge}"/>
    </linearGradient>
    <linearGradient id="hilt" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0%"  stop-color="#3a2a18"/>
      <stop offset="50%" stop-color="#1a0f08"/>
      <stop offset="100%" stop-color="#000000"/>
    </linearGradient>
  </defs>
  <path d="M 14 14 Q 36 18 54 28 Q 60 32 58 38 Q 50 38 40 36 Q 26 30 14 22 Z"
        fill="url(#blade)" stroke="${main}" stroke-width="0.8"/>
  <path d="M 16 16 Q 36 20 52 28 Q 58 31 56 35 Q 48 34 38 31 Q 26 26 16 20 Z"
        fill="url(#bladeEdge)" opacity="0.55"/>
  <path d="M 18 18 Q 32 22 46 28 Q 40 28 30 26 Q 22 22 18 20 Z"
        fill="${main}" opacity="0.7"/>
  <rect x="6"  y="6"  width="14" height="6"  rx="1" fill="url(#hilt)" stroke="#000" stroke-width="0.5"/>
  <rect x="4"  y="8"  width="4"  height="4"        fill="#1a0f08"     stroke="#000" stroke-width="0.4"/>
  <rect x="18" y="4"  width="4"  height="10"       fill="url(#hilt)"  stroke="#000" stroke-width="0.5"/>
  <circle cx="20"   cy="11"   r="2.2" fill="#c00010" stroke="#400000" stroke-width="0.4"/>
  <circle cx="19.4" cy="10.4" r="0.7" fill="#ff8080"/>
  <circle cx="58"   cy="38"   r="1.6" fill="${glow}" opacity="0.85"/>
</svg>
EOF
}

echo "Generating 25 dagger textures + models..."
for row in "${DAGGERS[@]}"; do
  IFS='|' read -r id cmd main edge glow <<< "$row"
  svg="$TEX_DIR/$id.svg"
  png="$TEX_DIR/$id.png"
  write_svg "$svg" "$main" "$edge" "$glow"
  magick -background none "$svg" -resize 64x64 "$png"
  rm "$svg"

  # Per-dagger handheld model (vanilla auto-3D extrusion of the 2D texture).
  cat > "$MODEL_DIR/$id.json" <<JSON
{
  "parent": "minecraft:item/handheld",
  "textures": { "layer0": "minecraft:item/daggers/$id" }
}
JSON

  # Modern item_model definition under daggersmp namespace.
  # Plugin sets item_model = daggersmp:dagger/<id>, client looks up this file.
  cat > "$DAG_ITEMS_DIR/$id.json" <<JSON
{
  "model": {
    "type": "minecraft:model",
    "model": "minecraft:item/daggers/$id"
  }
}
JSON
done

# Legacy fallback: CMD range_dispatch on netherite_sword (works pre-1.21.4
# and as a backup if any item somehow lacks the item_model component).
{
  echo '{'
  echo '  "model": {'
  echo '    "type": "minecraft:range_dispatch",'
  echo '    "property": "minecraft:custom_model_data",'
  echo '    "fallback": {"type": "minecraft:model", "model": "minecraft:item/netherite_sword"},'
  echo '    "entries": ['
  first=1
  for row in "${DAGGERS[@]}"; do
    IFS='|' read -r id cmd _ _ _ <<< "$row"
    [[ $first -eq 1 ]] || echo ','
    first=0
    printf '      {"threshold": %s, "model": {"type": "minecraft:model", "model": "minecraft:item/daggers/%s"}}' "$cmd" "$id"
  done
  echo
  echo '    ]'
  echo '  }'
  echo '}'
} > "$ITEMS_DIR/netherite_sword.json"

# Validate every JSON we produced.
jq . "$ITEMS_DIR/netherite_sword.json" > /dev/null
for row in "${DAGGERS[@]}"; do
  IFS='|' read -r id _ _ _ _ <<< "$row"
  jq . "$MODEL_DIR/$id.json"     > /dev/null
  jq . "$DAG_ITEMS_DIR/$id.json" > /dev/null
done

# pack.mcmeta — Minecraft 1.21.11 = resource pack format 75. Since 1.21.9+
# the schema uses min_format / max_format (NOT pack_format); using the old
# field on 1.21.11 makes the client silently treat the pack as invalid.
cat > "$ROOT/pack.mcmeta" <<'JSON'
{
  "pack": {
    "min_format": 75,
    "max_format": 75,
    "description": "§cDaggerSMP §7— 25 unique 3D daggers"
  }
}
JSON

# Pack icon (use the strength dagger as the cover art).
magick "$TEX_DIR/strength.png" -resize 128x128 "$ROOT/pack.png"

# Bundle as zip via jar (no `zip` cmd in this nix env).
( cd "$ROOT" && jar cf "../$OUT_ZIP" . )

echo "Done."
echo "  Folder: $ROOT/"
echo "  Zip:    $OUT_ZIP"
