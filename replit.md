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
