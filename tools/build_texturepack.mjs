// Generates the DaggerSMP texture pack matching the Solarite-Gems structure.
// - 25 dagger PNG textures (curved scythe blade silhouette, themed per dagger)
// - 25 model JSONs (item/handheld for in-hand 3D extrusion)
// - assets/minecraft/items/netherite_sword.json range_dispatch override
// - pack.mcmeta
//
// Run:  node tools/build_texturepack.mjs

import { execSync } from "node:child_process";
import { mkdirSync, writeFileSync, rmSync, existsSync } from "node:fs";
import { join } from "node:path";

const ROOT = "DaggerSMP-TexturePack";
const ASSET = join(ROOT, "assets", "minecraft");
const ITEMS_DIR = join(ASSET, "items");
const MODEL_DIR = join(ASSET, "models", "item", "daggers");
const TEX_DIR = join(ASSET, "textures", "item", "daggers");

if (existsSync(ROOT)) rmSync(ROOT, { recursive: true, force: true });
for (const d of [ITEMS_DIR, MODEL_DIR, TEX_DIR]) mkdirSync(d, { recursive: true });

// [name, cmd, mainColor, edgeColor, glowColor]
const DAGGERS = [
  ["strength", 1001, "#8B0000", "#FF6840", "#FF3010"],
  ["speed",    1002, "#00B8E6", "#A0F4FF", "#40E0FF"],
  ["wind",     1003, "#D8E8F0", "#FFFFFF", "#B0D8F0"],
  ["life",     1004, "#1FA838", "#A8FFB0", "#40FF60"],
  ["crimson",  1005, "#A8000C", "#FF4030", "#FF0010"],
  ["darkness", 1006, "#1A0028", "#5A2080", "#7A20C0"],
  ["hack",     1007, "#00B040", "#A8FFB0", "#00FF60"],
  ["frost",    1008, "#5AB8E0", "#D8F4FF", "#80E8FF"],
  ["mafia",    1009, "#A8741A", "#FFCC60", "#FFB020"],
  ["pirate",   1010, "#1F4A8A", "#80B0E0", "#3070D0"],
  ["void",     1011, "#5A0098", "#C088FF", "#9020E0"],
  ["lucky",    1012, "#E8B810", "#FFF098", "#FFD040"],
  ["mirror",   1013, "#9090A0", "#F0F0FF", "#C0C0D8"],
  ["jungle",   1014, "#207820", "#A0E058", "#40C040"],
  ["midas",    1015, "#E8A000", "#FFE060", "#FFB820"],
  ["toxic",    1016, "#60B820", "#D8FF80", "#A0FF40"],
  ["arachnid", 1017, "#202028", "#605070", "#A02050"],
  ["vampire",  1018, "#580010", "#E03040", "#A00018"],
  ["gravity",  1019, "#7868C8", "#D0C0FF", "#9080F0"],
  ["earth",    1020, "#5A3818", "#B08858", "#80502A"],
  ["titan",    1021, "#C03018", "#FF8050", "#FF5030"],
  ["guardian", 1022, "#1AA890", "#80F0D8", "#30E0C0"],
  ["ghost",    1023, "#A0B8D0", "#FFFFFF", "#D8E8FF"],
  ["chance",   1024, "#C030A0", "#FFA0E0", "#F050C0"],
  ["storm",    1025, "#E8C820", "#FFF880", "#FFE040"],
];

// Curved scythe blade shape (matches reference image silhouette).
// Authored on a 64x64 grid. Hilt at top-left, blade swoops down-right.
function svgFor({ main, edge, glow }) {
  return `<?xml version="1.0" encoding="UTF-8"?>
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

  <!-- Curved scythe blade body -->
  <path d="M 14 14 Q 36 18 54 28 Q 60 32 58 38 Q 50 38 40 36 Q 26 30 14 22 Z"
        fill="url(#blade)" stroke="${main}" stroke-width="0.8"/>

  <!-- Bright cutting edge highlight -->
  <path d="M 16 16 Q 36 20 52 28 Q 58 31 56 35 Q 48 34 38 31 Q 26 26 16 20 Z"
        fill="url(#bladeEdge)" opacity="0.55"/>

  <!-- Inner blade shadow for depth -->
  <path d="M 18 18 Q 32 22 46 28 Q 40 28 30 26 Q 22 22 18 20 Z"
        fill="${main}" opacity="0.7"/>

  <!-- Hilt (handle) at top-left, angled -->
  <rect x="6"  y="6"  width="14" height="6" rx="1" fill="url(#hilt)" stroke="#000" stroke-width="0.5"/>
  <rect x="4"  y="8"  width="4"  height="4" fill="#1a0f08" stroke="#000" stroke-width="0.4"/>
  <rect x="18" y="4"  width="4"  height="10" fill="url(#hilt)" stroke="#000" stroke-width="0.5"/>

  <!-- Gem at hilt-blade junction -->
  <circle cx="20" cy="11" r="2.2" fill="#c00010" stroke="#400000" stroke-width="0.4"/>
  <circle cx="19.4" cy="10.4" r="0.7" fill="#ff8080"/>

  <!-- Glow tip accent -->
  <circle cx="58" cy="38" r="1.6" fill="${glow}" opacity="0.85"/>
</svg>`;
}

console.log("Generating textures...");
for (const [name, cmd, main, edge, glow] of DAGGERS) {
  const svg = svgFor({ main, edge, glow });
  const svgPath = join(TEX_DIR, `${name}.svg`);
  const pngPath = join(TEX_DIR, `${name}.png`);
  writeFileSync(svgPath, svg);
  // Rasterize SVG -> 64x64 PNG with transparency
  execSync(`magick -background none "${svgPath}" -resize 64x64 "${pngPath}"`, { stdio: "pipe" });
  rmSync(svgPath);

  // Model JSON: item/handheld so Minecraft auto-extrudes the 2D texture into a
  // full 3D model in-hand (this is the standard Minecraft 3D-item technique).
  writeFileSync(
    join(MODEL_DIR, `${name}.json`),
    JSON.stringify(
      {
        parent: "minecraft:item/handheld",
        textures: { layer0: `minecraft:item/daggers/${name}` },
      },
      null,
      2,
    ),
  );
}

// items/netherite_sword.json — range_dispatch on custom_model_data
const entries = DAGGERS.map(([name, cmd]) => ({
  threshold: cmd,
  model: { type: "minecraft:model", model: `minecraft:item/daggers/${name}` },
}));

writeFileSync(
  join(ITEMS_DIR, "netherite_sword.json"),
  JSON.stringify(
    {
      model: {
        type: "minecraft:range_dispatch",
        property: "minecraft:custom_model_data",
        fallback: { type: "minecraft:model", model: "minecraft:item/netherite_sword" },
        entries,
      },
    },
    null,
    2,
  ),
);

// pack.mcmeta — same format as Solarite (1.21.9–1.21.11 compatible)
writeFileSync(
  join(ROOT, "pack.mcmeta"),
  JSON.stringify(
    {
      pack: {
        pack_format: 81,
        supported_formats: { min_inclusive: 46, max_inclusive: 200 },
        description: "§cDaggerSMP §7— 25 unique 3D daggers",
      },
    },
    null,
    2,
  ),
);

// Generate a simple pack.png icon (use the strength dagger as the cover)
execSync(
  `magick "${join(TEX_DIR, "strength.png")}" -resize 128x128 "${join(ROOT, "pack.png")}"`,
  { stdio: "pipe" },
);

// Zip it for easy install
const zipName = "DaggerSMP-TexturePack.zip";
if (existsSync(zipName)) rmSync(zipName);
execSync(`cd "${ROOT}" && zip -rq "../${zipName}" .`, { stdio: "pipe" });

console.log(`Done: ${DAGGERS.length} daggers generated`);
console.log(`  Folder: ${ROOT}/`);
console.log(`  Zip:    ${zipName}`);
