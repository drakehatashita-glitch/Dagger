#!/usr/bin/env bash
# Builds the DaggerSMP texture pack on top of AltarSMP's 3D weapon assets.
# - Reuses AltarSMP's Blockbench 3D weapon models (true cuboid geometry, not
#   flat extrusion) and their textures/animations.
# - Overrides assets/minecraft/items/netherite_sword.json so the 25 dagger CMDs
#   (1001-1025) each render as one of those 3D weapon models.
# - Pack format & structure match AltarSMP exactly.
set -euo pipefail

SRC_ZIP="attached_assets/AltarSMP_1776990116869.zip"
ROOT="DaggerSMP-TexturePack"
OUT_ZIP="DaggerSMP-TexturePack.zip"

rm -rf "$ROOT" "$OUT_ZIP"
mkdir -p "$ROOT"

# Start from a clean copy of AltarSMP so all weapon models, textures,
# animations, and supporting files come along.
unzip -oq "$SRC_ZIP" -d "$ROOT"

# Replace netherite_sword.json with our 25-dagger mapping.
# Each dagger maps to one of AltarSMP's existing 3D weapon models, chosen
# thematically. Several daggers intentionally share the same base model
# (e.g. Pirate + Mafia both use the cutlass) since we have 25 daggers and
# ~16 unique weapon models in the pack.
cat > "$ROOT/assets/minecraft/items/netherite_sword.json" <<'JSON'
{
  "model": {
    "type": "minecraft:range_dispatch",
    "property": "minecraft:custom_model_data",
    "fallback": {
      "type": "minecraft:model",
      "model": "minecraft:item/netherite_sword"
    },
    "entries": [
      {"threshold": 1001, "model": {"type": "minecraft:model", "model": "weapons/arc3/pure_blade"}},
      {"threshold": 1002, "model": {"type": "minecraft:model", "model": "weapons/arc3/dagger"}},
      {"threshold": 1003, "model": {"type": "minecraft:model", "model": "weapons/arc3/wind"}},
      {"threshold": 1004, "model": {"type": "minecraft:model", "model": "weapons/arc3/cutlass"}},
      {"threshold": 1005, "model": {"type": "minecraft:model", "model": "weapons/bloodlust"}},
      {"threshold": 1006, "model": {"type": "minecraft:model", "model": "weapons/arc5/sculk"}},
      {"threshold": 1007, "model": {"type": "minecraft:model", "model": "weapons/arc3/gauntlet"}},
      {"threshold": 1008, "model": {"type": "minecraft:model", "model": "weapons/frost"}},
      {"threshold": 1009, "model": {"type": "minecraft:model", "model": "weapons/hyperion"}},
      {"threshold": 1010, "model": {"type": "minecraft:model", "model": "weapons/arc3/cutlass"}},
      {"threshold": 1011, "model": {"type": "minecraft:model", "model": "weapons/arc5/eclipse"}},
      {"threshold": 1012, "model": {"type": "minecraft:model", "model": "weapons/arc3/pure_blade"}},
      {"threshold": 1013, "model": {"type": "minecraft:model", "model": "weapons/illusion_wand"}},
      {"threshold": 1014, "model": {"type": "minecraft:model", "model": "weapons/bone_blade"}},
      {"threshold": 1015, "model": {"type": "minecraft:model", "model": "weapons/arc5/mace/lv3"}},
      {"threshold": 1016, "model": {"type": "minecraft:model", "model": "weapons/arc3/witherbone"}},
      {"threshold": 1017, "model": {"type": "minecraft:model", "model": "weapons/arc3/dagger"}},
      {"threshold": 1018, "model": {"type": "minecraft:model", "model": "custom/vampire"}},
      {"threshold": 1019, "model": {"type": "minecraft:model", "model": "weapons/arc5/nukeremote"}},
      {"threshold": 1020, "model": {"type": "minecraft:model", "model": "weapons/arc3/axe"}},
      {"threshold": 1021, "model": {"type": "minecraft:model", "model": "weapons/arc5/mace/lv2"}},
      {"threshold": 1022, "model": {"type": "minecraft:model", "model": "weapons/arc5/mace/lv1"}},
      {"threshold": 1023, "model": {"type": "minecraft:model", "model": "custom/pale"}},
      {"threshold": 1024, "model": {"type": "minecraft:model", "model": "weapons/illusion_wand"}},
      {"threshold": 1025, "model": {"type": "minecraft:model", "model": "weapons/arc3/wind"}}
    ]
  }
}
JSON

# Validate the JSON we just wrote.
jq . "$ROOT/assets/minecraft/items/netherite_sword.json" > /dev/null

# Update pack.mcmeta to credit both packs and bump description.
cat > "$ROOT/pack.mcmeta" <<'JSON'
{
  "pack": {
    "pack_format": 47,
    "description": "§cDaggerSMP §7— 25 3D daggers §8(weapon models from AltarSMP by Gwamba & Quacker)"
  }
}
JSON

# Bundle as zip via jar (no `zip` cmd in this nix env).
( cd "$ROOT" && jar cf "../$OUT_ZIP" . )

echo "Done."
echo "  Folder: $ROOT/"
echo "  Zip:    $OUT_ZIP"
echo
echo "Dagger -> 3D model mapping:"
jq -r '.model.entries[] | "  CMD \(.threshold)  ->  \(.model.model)"' \
  "$ROOT/assets/minecraft/items/netherite_sword.json"
