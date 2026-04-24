# DaggerSMP

A Minecraft Paper plugin (Java 21, Paper API 1.21) providing custom dagger abilities.

## Project Layout
- `src/main/java/com/daggersmp/...` — Java source (decompiled reference; the canonical artifact is the prebuilt jar)
- `src/main/resources/plugin.yml` — Bukkit plugin descriptor
- `src/main/resources/config.yml` — runtime config
- `pom.xml` — Maven build (Paper API 1.21.11-R0.1-SNAPSHOT, Java 21)
- `DaggerSMP-2.2.0.jar` — prebuilt plugin jar (canonical build, target Paper 1.21.11)
- `DaggerSMP-TexturePack/` — resource pack (`pack_format` 80, advertised as 1.21.11)

## Build
This project is plugin/texture pack development only — no server is run from this Repl.
The freshly built jar lands at `target/DaggerSMP-2.2.0.jar` and is also copied to the project root:
```
mvn -B clean package
cp target/DaggerSMP-2.2.0.jar DaggerSMP-2.2.0.jar
```
Drop `DaggerSMP-2.2.0.jar` into your own Paper 1.21.11 server's `plugins/` folder.

## v2.2.0 follow-up fixes (April 24, 2026 — round 6)
Dramatic visual overhaul for the remaining dagger abilities:
- **Vampire A1 (blood drain):** Activation blood surge ring, continuous coiling crimson aura around caster, visible blood particle streams flowing from nearby enemies toward the vampire every 2 ticks, pulsing blood ring + floating hearts every 10 ticks.
- **Darkness A1 (shadow pulse):** Expanding dark sphere — three-layer SQUID_INK ring growing outward at 60 steps/ring, deep purple DUST accents, 8 spiraling shadow tendrils rotating outward from caster, dense dark cloud at the core during the opening frames.
- **Darkness A2 (sonic beam):** Pre-cast muzzle flash of SQUID_INK + purple DUST at the caster's eye; dense dark ink core along the full beam path; purple-violet outer halo; perpendicular shockwave rings every ~1 block along the beam; hit-burst of ink + violet DUST on each target struck.
- **Ghost A1 (spectral dash):** Launch-point soul-ring burst + END_ROD glints; interpolated 4-sample trail between previous/current position each tick (SOUL + CLOUD + ice-blue DUST); spectral afterimage rings + END_ROD sparks left every 4 ticks during flight.
- **Ghost A2 (noclip form):** Dense orbiting SOUL aura (8 particles/tick), ice-blue DUST shimmer wisps scattered around the body, spectral shroud ring + END_ROD glints every 8 ticks. Trail now updates every 2 ticks instead of 3.
- **Earth A1 (obsidian wall):** Per-block eruption burst of BLOCK + LARGE_SMOKE as each block places; seismic ground crack: LARGE_SMOKE + BLOCK debris cloud + EXPLOSION particles at the wall midpoint.
- **Earth A2 (boulder):** Trail now emits cobblestone + gravel BLOCK debris cloud, LARGE_SMOKE, and sandy/brown DUST each tick; impact crater now fires 12 EXPLOSION_EMITTER, 120 EXPLOSION, 280 cobblestone + 120 gravel + 80 dirt BLOCK particles, a 40-point shockwave ring of stones at ground level, a DUST crater dust cloud, and a LARGE_SMOKE column rising from the crater.
- **Mirror A1 (reflect shield):** Silver flash burst on activation (END_ROD + DUST ring + FLASH); continuous rotating silver-white mirror-shard ring following the player, END_ROD glints every 6 ticks, for the full reflect duration.

## v2.2.0 follow-up fixes (April 24, 2026 — round 5)
- **Custom ability sound effects.** 14 unique synthesized `.ogg` sounds (44.1kHz mono, libvorbis q4) generated with FFmpeg lavfi audio graphs and shipped in the resource pack at `assets/daggersmp/sounds/ability/*.ogg`. Sound events declared in `assets/daggersmp/sounds.json` under the `ability.<dagger>.<name>` namespace. 21 vanilla `Sound.X` enum playSound calls in `AbilityManager.java` were swapped to namespaced custom sound IDs (`"daggersmp:ability.frost.field"`, `"daggersmp:ability.storm.bolt"`, etc.). Covers wind dash/leap, darkness sonic, frost field, pirate wave, void dash, gravity blackhole, guardian beam, ghost dash, earth boulder, toxic cloud, vampire heal, life steal, storm bolt. The synthesized sounds are stylized magical/elemental tones — easy to swap out later by dropping a real `.ogg` over the same filename.
- **Realistic particle textures.** 4 vanilla particle textures overridden in the resource pack at `assets/minecraft/textures/particle/`: `snowflake.png` (frost), `heart.png` (life), `bubble.png` (pirate), `end_rod.png` (guardian beam). All are AI-generated 16×16 sprites. **Side effect:** these overrides are global — any vanilla Minecraft use of those particles (powder snow, cat purrs, end rods, water bubbles) will also use the new textures. Multi-frame animated particles (portal, soul, electric_spark, etc.) were not touched to avoid mid-animation flicker between custom and vanilla frames.

## v2.2.0 follow-up fixes (April 24, 2026 — round 4)
- **All 25 dagger textures regenerated** as proper 256×256 ornate fantasy daggers (curved scimitar blade, dark wrapped handle with gold crossguard, gem pommel, themed glow aura per dagger). Each dagger has unique themed details — life has heart particles, hack has matrix code, frost has snowflakes, vampire has bat silhouettes, chance has rainbow sparkles, storm has lightning bolts, etc. Replaces the previous 16×16 colored-stripe stubs.
- **`dagger_base.json` model rewritten** to extend `minecraft:item/handheld` (the same parent vanilla swords use). The texture renders as a flat 2D sprite that Minecraft auto-extrudes into a 3D model in-hand, with the blade angled outward and away from the player's view in both right and left hand. The previous custom voxel cuboid model only used the texture as a tiny color palette so the artwork never showed.
- **Texture pack zip rebuilt** at `DaggerSMP-TexturePack.zip` (~1.4 MB).

## v2.2.0 follow-up fixes (April 24, 2026 — round 3)
- **Target = Paper 1.21.11.** `pom.xml` already builds against `paper-api 1.21.11-R0.1-SNAPSHOT`; the texture pack `pack.mcmeta` advertises 1.21.11.
- **No server runs from this Repl** — this is plugin/texture pack development only. Build with `mvn -B clean package`.
- **Earth A1 — wall = OBSIDIAN.** `daggers.earth.ability1.material` corrected from `STONE` to `OBSIDIAN` in `config.yml` (code default was already OBSIDIAN).
- **Storm A2 — lightning rain now actually hits.** Each bolt now picks a random valid living entity within `radius` and strikes ON top of them, dealing exactly `daggers.storm.ability2.damage` (default `4.0` = 2 hearts). When no targets are nearby it falls back to the old random-spot strike for visuals.
- **Gravity passive — fully rewritten.**
  - Wearer now takes **zero** fall damage (the fall damage event is cancelled).
  - At `min-fall-blocks` (default `10`) or higher, an explosion erupts at the landing point.
  - Damage scales at `damage-per-block` (default `0.4` = 0.20 hearts/block) up to `max-damage` (default `10.0` = 5 hearts).
  - Explosion power grows with fall distance: `explosion-power-base` (1.5) + `(fallDist - minFall) * explosion-power-per-block` (0.08), clamped to `explosion-power-max` (6.0). Bigger fall → bigger boom.
- **Mirror A1 — reflection actually works.** Now reflects on any `EntityDamageByEntityEvent`, including mob attacks and projectiles (resolves projectile shooter to the firing entity). Reflects `daggers.mirror.ability1.reflect-percent` (now defaulting to `0.5` = 50%). Pure environmental damage (FALL, LAVA, FIRE, DROWNING, …) is **not** reflected to anything because it isn't an entity-caused event. Reflected damage bypasses i-frames so it always lands.
- **Wind A1 — dash retuned.** `dash-blocks` lowered from `35.0` to `20.0` and the dash now follows the player's full look direction (X/Y/Z) — you can dash up or down by aiming there. The previous code zeroed-out vertical aim and only dashed horizontally.
- **All ability/passive chat spam removed.** Every `*.sendMessage(...)` call inside `AbilityManager.java` and the ability/passive paths in `DaggerListener.java` was deleted, including cooldown notices, "ability activated" toasts, and passive triggers like "Dodged!" / "Lucky! Saved from death." `/dagger` admin command output (give/list/cooldown/reload) is preserved.

## v2.2.0 follow-up fixes (April 24, 2026 — round 2)
- **Texture pack — bigger daggers + correct first-person orientation.** All 25 dagger models now extend a new `daggersmp:item/dagger_base` parent that defines explicit display transforms: ~2.4× scale in first person and ~1.35× in third person, plus a Y-axis rotation flip in first person so the blade points away from the player instead of back toward them.
- **Arachnid cobweb passive** rewritten to be adaptive: instead of a flat 5× multiplier on the move delta, the move event now scales the player's intended motion up to a target walking speed (`daggers.arachnid.passive.cobweb-walk-speed`, default 0.215 blocks/tick) capped by `cobweb-multiplier` (default 30×), and immediately boosts the player's velocity vector to the same speed. Cobwebs feel like normal walking instead of a slow glitchy slog.
- **Guardian beam** now spawns a real invisible vanilla `Guardian` entity at the caster's eye and updates its target each tick (either the ray-traced living entity, or an invisible marker armor stand at the beam endpoint). The client renders the actual ocean-monument beam (correct visual + `ENTITY_GUARDIAN_ATTACK` sound). Knockback default lowered from 0.5 to 0.12 and applied additively (`vel.add(kb)`) instead of overwriting the velocity, and Y bump reduced from 0.2 → 0.05. The guardian and anchor are cleaned up when the beam ends or is cancelled.
- **All ability particles ~3× denser.** Bulk-bumped the `count` parameter on every `spawnParticle` call across the ability suite (Wind shockwave, Frost slam/aura, Pirate splash, Jungle vines, Ghost dash, Earth boulder/impact, Toxic cloud, Guardian aura, Storm bolts, Mafia/Lucky/Vampire bursts, etc.) for noticeably richer ability VFX.

## v2.2.0 follow-up fixes (April 2026)
- **Tab completion** registered for `/dagger`, `/trust`, `/untrust`, `/ability1`, `/ability2`.
- **`/dagger cooldown`** rewritten: simply resets all cooldowns; defaults to self when no player given.
- **DaggerType lore** now strips `(parenthetical)` text from ability and passive descriptions.
- **ActionBar** no longer shows the ability-2 cooldown; only A1 is displayed.
- **Life A1** forces damage through `noDamageTicks=0` and falls back to direct HP subtraction so half-heart steals never get absorbed by i-frames.
- **Frost A1** removes Slowness/Mining-Fatigue and instead anchors targets in place each tick (teleport-back) so they're truly frozen but not slowed.
- **Jungle A2** rewritten as a directional vine raytrace — pulls/teleports the first hit target back to the caster.
- **Midas A2** upgrades caster's and trusted players' armor pieces to Netherite (preserving enchants/lore).
- **Guardian A1** beam now fires in the look direction (no entity lock-on), is visible via dust+end-rod particles, and damages anything it sweeps.
- **Hack A2** boosts `ENTITY_INTERACTION_RANGE` and `BLOCK_INTERACTION_RANGE` attributes by `+3` for the duration.
- **Ghost A1** changed from instant teleport to a launched dash with fall-damage immunity.
- **Ghost A2** uses spectator mode + invisibility for true noclip with armor hidden, then restores prior gamemode.
- **Earth A1** default wall material changed to OBSIDIAN.
- **Earth A2** boulder is now a 3x-scaled BlockDisplay riding the FallingBlock, larger explosion radius, real block-break explosion at impact.
- **Toxic A1** emits dense visible green dust + slime particles through the cloud's lifetime.
- **Pirate A2** now spawns an expanding circular splash wave with blue dust and dripping water for visual flair.
- **Void passive** removes Speed/Strength explicitly when the player no longer holds the Void dagger.
- **25 dagger textures** regenerated as 256×256 fantasy curved daggers (themed glow color, transparent BG, wood handle, gold pommel).

## Recent bug fixes (v2.2.0 patch)
- **Vampire passive** now actually grants +2 hearts (max-health bonus 4.0). New `VAMPIRE_HP_KEY` attribute modifier mirrors the existing Life dagger pattern.
- **Midas passive** now drops `daggers.midas.passive.gold-ingot-amount` gold ingots whenever a Midas-wielder kills a living entity (skips ArmorStand and self-kills).
- **Arachnid cobweb passive** rewritten as a `PlayerMoveEvent` handler that scales the player's intended move delta by `daggers.arachnid.passive.cobweb-multiplier` (default 5.0), undoing vanilla's slowdown so cobwebs feel walkable.
- **Arachnid wallclimb** default Y velocity bumped to 0.42 (jump strength), and climbing now triggers when sneaking *or* when actively jumping into the wall.
- **Lore audit** — fixed descriptions that didn't match code: Wind, Crimson, Darkness, Hack, Void, Earth, Storm now list the actual permanent passive effects they apply.

## Environment
- JDK 21 (Nix `jdk21`)
- Maven (Nix `maven`)
- `unzip` (system) — used to extract the original project archive
