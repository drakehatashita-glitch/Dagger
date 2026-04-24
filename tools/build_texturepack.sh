#!/usr/bin/env bash
# Builds the DaggerSMP texture pack on top of AltarSMP's 3D weapon assets.
#
# How model overrides work in 1.21.4+ (and especially 1.21.11):
#   - The plugin sets the item_model component on each dagger to
#     daggersmp:dagger/<id> via meta.setItemModel(...).
#   - The client looks up assets/daggersmp/items/dagger/<id>.json from this
#     resource pack and renders whatever model that file points to.
#   - We point each of those item-model files at one of AltarSMP's existing
#     3D Blockbench weapon models in assets/minecraft/models/weapons/...
#
# We also keep a CMD-based range_dispatch on netherite_sword.json as a
# legacy fallback for any tooling/environment that still relies on CMD.
set -euo pipefail

SRC_ZIP="attached_assets/AltarSMP_1776990116869.zip"
ROOT="DaggerSMP-TexturePack"
OUT_ZIP="DaggerSMP-TexturePack.zip"

rm -rf "$ROOT" "$OUT_ZIP"
mkdir -p "$ROOT"

# Start from a clean copy of AltarSMP so all weapon models, textures,
# animations, and supporting files come along.
unzip -oq "$SRC_ZIP" -d "$ROOT"

# 25 daggers -> AltarSMP 3D weapon model paths (chosen thematically; some
# share models since AltarSMP has ~16 unique weapons and we have 25 daggers).
# Format: dagger_id|cmd|model_path
DAGGERS=(
  "strength|1001|weapons/arc3/pure_blade"
  "speed|1002|weapons/arc3/dagger"
  "wind|1003|weapons/arc3/wind"
  "life|1004|weapons/arc3/cutlass"
  "crimson|1005|weapons/bloodlust"
  "darkness|1006|weapons/arc5/sculk"
  "hack|1007|weapons/arc3/gauntlet"
  "frost|1008|weapons/frost"
  "mafia|1009|weapons/hyperion"
  "pirate|1010|weapons/arc3/cutlass"
  "void|1011|weapons/arc5/eclipse"
  "lucky|1012|weapons/arc3/pure_blade"
  "mirror|1013|weapons/illusion_wand"
  "jungle|1014|weapons/bone_blade"
  "midas|1015|weapons/arc5/mace/lv3"
  "toxic|1016|weapons/arc3/witherbone"
  "arachnid|1017|weapons/arc3/dagger"
  "vampire|1018|custom/vampire"
  "gravity|1019|weapons/arc5/nukeremote"
  "earth|1020|weapons/arc3/axe"
  "titan|1021|weapons/arc5/mace/lv2"
  "guardian|1022|weapons/arc5/mace/lv1"
  "ghost|1023|custom/pale"
  "chance|1024|weapons/illusion_wand"
  "storm|1025|weapons/arc3/wind"
)

# --- Modern path: per-dagger item_model definitions under daggersmp namespace.
DAG_ITEMS_DIR="$ROOT/assets/daggersmp/items/dagger"
mkdir -p "$DAG_ITEMS_DIR"
for row in "${DAGGERS[@]}"; do
  IFS='|' read -r id cmd model <<< "$row"
  cat > "$DAG_ITEMS_DIR/$id.json" <<JSON
{
  "model": {
    "type": "minecraft:model",
    "model": "$model"
  }
}
JSON
done

# --- Legacy fallback: keep CMD range_dispatch on netherite_sword.json too.
{
  echo '{'
  echo '  "model": {'
  echo '    "type": "minecraft:range_dispatch",'
  echo '    "property": "minecraft:custom_model_data",'
  echo '    "fallback": {"type": "minecraft:model", "model": "minecraft:item/netherite_sword"},'
  echo '    "entries": ['
  first=1
  for row in "${DAGGERS[@]}"; do
    IFS='|' read -r id cmd model <<< "$row"
    [[ $first -eq 1 ]] || echo ','
    first=0
    printf '      {"threshold": %s, "model": {"type": "minecraft:model", "model": "%s"}}' "$cmd" "$model"
  done
  echo
  echo '    ]'
  echo '  }'
  echo '}'
} > "$ROOT/assets/minecraft/items/netherite_sword.json"

# Validate JSON.
jq . "$ROOT/assets/minecraft/items/netherite_sword.json" > /dev/null
for row in "${DAGGERS[@]}"; do
  IFS='|' read -r id cmd model <<< "$row"
  jq . "$DAG_ITEMS_DIR/$id.json" > /dev/null
done

# pack.mcmeta — Minecraft 1.21.11 = resource pack format 75.
# Critically, 1.21.9+ replaced `pack_format`/`supported_formats` with the new
# `min_format` / `max_format` schema (snapshot 25w31a). If you keep using the
# old `pack_format` field on 1.21.11, the client silently treats the pack as
# invalid and falls back to vanilla / missing-texture (the "purple block").
cat > "$ROOT/pack.mcmeta" <<'JSON'
{
  "pack": {
    "min_format": 75,
    "max_format": 75,
    "description": "§cDaggerSMP §7— 25 3D daggers §8(weapons by Gwamba & Quacker, AltarSMP)"
  }
}
JSON

# Bundle as zip via jar (no `zip` cmd in this nix env).
( cd "$ROOT" && jar cf "../$OUT_ZIP" . )

echo "Done."
echo "  Folder: $ROOT/"
echo "  Zip:    $OUT_ZIP"
echo
echo "Per-dagger item_model definitions written to assets/daggersmp/items/dagger/"
echo "Legacy CMD range_dispatch kept in assets/minecraft/items/netherite_sword.json"
