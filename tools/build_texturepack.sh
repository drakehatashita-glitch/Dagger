#!/usr/bin/env bash
# Builds the DaggerSMP texture pack with 25 fantasy daggers — all sharing the
# same ornate scimitar shape (matching the user's reference artwork), each
# tinted with its own themed color and a matching radial glow aura.
#
# Pipeline per dagger:
#   1. Take the AI-generated master dagger silhouette (ornate hilt + curved
#      silver blade) from attached_assets/generated_images/dagger_master.png.
#   2. Build a luminance mask isolating the blade (bright pixels) from the
#      hilt (dark pixels) so we only recolor the blade.
#   3. Tint the blade: pre-stretch the tonal range, then +level-colors maps
#      black->BLADE (saturated theme color) and white->HIGHLIGHT (lighter
#      theme color), giving the reference's "bright themed core, deeper
#      themed edges" look while keeping the dark hilt unchanged.
#   4. Build a soft two-pass aura: large soft outer glow + tighter inner bloom,
#      both using the alpha silhouette blurred and tinted with AURA color.
#   5. Composite back-to-front: outer aura -> inner bloom -> recolored dagger.
#
# Resource pack metadata uses 1.21.9+ schema (min_format/max_format).
# Minecraft 1.21.11 = resource pack format 75.
set -euo pipefail

MASTER_SRC="attached_assets/generated_images/dagger_master.png"
ROOT="DaggerSMP-TexturePack"
OUT_ZIP="DaggerSMP-TexturePack.zip"
ASSET="$ROOT/assets/minecraft"
TEX_DIR="$ASSET/textures/item/daggers"
MODEL_DIR="$ASSET/models/item/daggers"
ITEMS_DIR="$ASSET/items"
DAG_ITEMS_DIR="$ROOT/assets/daggersmp/items/dagger"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

[[ -f "$MASTER_SRC" ]] || { echo "Missing $MASTER_SRC"; exit 1; }

rm -rf "$ROOT" "$OUT_ZIP"
mkdir -p "$TEX_DIR" "$MODEL_DIR" "$ITEMS_DIR" "$DAG_ITEMS_DIR"

# id|cmd|BLADE|HIGHLIGHT|AURA  (CMD = 1001..1025, matches DaggerType enum)
DAGGERS=(
  "strength|1001|#D02010|#FFB098|#FF6840"
  "speed|1002|#10A8E0|#C0F0FF|#A0F4FF"
  "wind|1003|#A8C8E0|#FFFFFF|#D8E8F0"
  "life|1004|#28C048|#C0FFC8|#A8FFB0"
  "crimson|1005|#D81020|#FF8070|#FF4030"
  "darkness|1006|#7028B0|#B080E0|#5A2080"
  "hack|1007|#10D058|#A8FFB8|#A8FFB0"
  "frost|1008|#60D0F0|#E8F8FF|#D8F4FF"
  "mafia|1009|#D89020|#FFE098|#FFCC60"
  "pirate|1010|#3070D0|#A0C8F0|#80B0E0"
  "void|1011|#9020E0|#E8C8FF|#C088FF"
  "lucky|1012|#F0C820|#FFF8B0|#FFF098"
  "mirror|1013|#B0B0C0|#FFFFFF|#F0F0FF"
  "jungle|1014|#28A028|#C0FF80|#A0E058"
  "midas|1015|#FFB820|#FFEC98|#FFE060"
  "toxic|1016|#80E020|#E0FFB0|#D8FF80"
  "arachnid|1017|#A02050|#FF80B0|#605070"
  "vampire|1018|#C01828|#FF6068|#E03040"
  "gravity|1019|#9080F0|#E0D8FF|#D0C0FF"
  "earth|1020|#8B5A2B|#D8B080|#B08858"
  "titan|1021|#E04020|#FFB098|#FF8050"
  "guardian|1022|#28D8B8|#A8FFE0|#80F0D8"
  "ghost|1023|#C0D0E0|#FFFFFF|#FFFFFF"
  "chance|1024|#E040C0|#FFB0E8|#FFA0E0"
  "storm|1025|#FFE040|#FFFCB0|#FFF880"
)

# Pre-process master once: trim transparent borders, fit centered on a square
# canvas so all daggers share identical layout.
MASTER="$WORK/master.png"
magick "$MASTER_SRC" -trim +repage -resize 480x480\> -background none \
       -gravity center -extent 512x512 "$MASTER"

# Reusable: alpha silhouette + blade luminance mask (constant across daggers).
magick "$MASTER" -alpha extract "$WORK/orig_alpha.png"
magick "$MASTER" -alpha off -colorspace gray -threshold 50% "$WORK/mask_raw.png"
magick "$WORK/mask_raw.png" "$WORK/orig_alpha.png" -compose multiply -composite \
       "$WORK/blade_mask.png"

echo "Generating 25 daggers..."
for row in "${DAGGERS[@]}"; do
  IFS='|' read -r id cmd BLADE HIGHLIGHT AURA <<< "$row"

  # Tinted blade (whole image): stretch tonal range, then map black->BLADE,
  # white->HIGHLIGHT. Hilt becomes tinted but we'll mask it back to original.
  magick "$MASTER" -level 25%x95% +level-colors "$BLADE,$HIGHLIGHT" \
         "$WORK/tinted_full.png"
  # Compose: where blade_mask is white use tinted, else keep original (hilt).
  magick "$MASTER" "$WORK/tinted_full.png" "$WORK/blade_mask.png" \
         -compose over -composite "$WORK/recolored.png"

  # Soft two-pass aura.
  magick "$WORK/orig_alpha.png" -blur 0x40 -evaluate Multiply 0.6 \
         "$WORK/outer_a.png"
  magick "$WORK/orig_alpha.png" -blur 0x12 -evaluate Multiply 0.9 \
         "$WORK/inner_a.png"
  magick -size 512x512 xc:"$AURA" "$WORK/outer_a.png" -alpha off \
         -compose copy_opacity -composite "$WORK/outer.png"
  magick -size 512x512 xc:"$AURA" "$WORK/inner_a.png" -alpha off \
         -compose copy_opacity -composite "$WORK/inner.png"

  # Composite back-to-front, downsize to 256x256 final texture.
  magick "$WORK/outer.png" "$WORK/inner.png" -compose over -composite \
         "$WORK/recolored.png" -compose over -composite \
         -resize 256x256 "$TEX_DIR/$id.png"

  # Per-dagger handheld model (vanilla auto-3D extrusion of the 2D texture).
  cat > "$MODEL_DIR/$id.json" <<JSON
{
  "parent": "minecraft:item/handheld",
  "textures": { "layer0": "minecraft:item/daggers/$id" }
}
JSON

  # Modern item_model definition under the daggersmp namespace. The plugin
  # calls setItemModel(NamespacedKey("daggersmp","dagger/<id>")), which makes
  # the client look up exactly this file.
  cat > "$DAG_ITEMS_DIR/$id.json" <<JSON
{
  "model": {
    "type": "minecraft:model",
    "model": "minecraft:item/daggers/$id"
  }
}
JSON

  printf '.'
done
echo

# Legacy fallback: CMD range_dispatch on the base netherite_sword item
# definition, in case any dagger ever lacks the item_model component.
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

# Validate JSON.
jq . "$ITEMS_DIR/netherite_sword.json" > /dev/null
for row in "${DAGGERS[@]}"; do
  IFS='|' read -r id _ _ _ _ <<< "$row"
  jq . "$MODEL_DIR/$id.json"     > /dev/null
  jq . "$DAG_ITEMS_DIR/$id.json" > /dev/null
done

# pack.mcmeta — Minecraft 1.21.11 = resource pack format 75. The 1.21.9+
# schema uses min_format/max_format (NOT pack_format); using the old field on
# 1.21.11 makes the client silently treat the pack as invalid.
cat > "$ROOT/pack.mcmeta" <<'JSON'
{
  "pack": {
    "min_format": 75,
    "max_format": 75,
    "description": "§cDaggerSMP §7— 25 fantasy daggers with themed auras"
  }
}
JSON

# Pack icon: use the void dagger as cover art.
magick "$TEX_DIR/void.png" -resize 128x128 "$ROOT/pack.png"

# Bundle as zip via jar (no `zip` cmd in this nix env).
( cd "$ROOT" && jar cf "../$OUT_ZIP" . )

echo "Done."
echo "  Folder: $ROOT/"
echo "  Zip:    $OUT_ZIP"
