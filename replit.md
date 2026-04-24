# DaggerSMP

A Minecraft Paper plugin (Java 21, Paper API 1.21) providing custom dagger abilities.

## Project Layout
- `src/main/java/com/daggersmp/...` — Java source (decompiled reference; the canonical artifact is the prebuilt jar)
- `src/main/resources/plugin.yml` — Bukkit plugin descriptor
- `src/main/resources/config.yml` — runtime config
- `pom.xml` — Maven build (Paper API 1.21.11-R0.1-SNAPSHOT, Java 21)
- `DaggerSMP-2.2.0.jar` — prebuilt plugin jar (canonical build)
- `server/` — Paper server runtime
  - `paper.jar` — Paper 1.21.8 server (upgraded from 1.21.1 to support `CREAKING_HEART` / `DRIED_GHAST` materials referenced by the plugin)
  - `plugins/DaggerSMP-2.2.0.jar` — installed plugin
  - `eula.txt`, `server.properties`, world data

## Running
The "Start application" workflow runs the Paper server:
```
cd server && java -Xms512M -Xmx1G -jar paper.jar nogui
```
The Minecraft server listens on port 25565 (default). It is not a web application — there is no HTTP frontend.

## Build
The decompiled source has been touched up to compile cleanly. The freshly built jar lands at `target/DaggerSMP-2.2.0.jar`:
```
mvn -B clean package
```

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
