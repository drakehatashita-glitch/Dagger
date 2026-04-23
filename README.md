# DaggerSMP 2.2.0

Paper plugin for Minecraft 1.21.11 (Java 21).

## Contents

- `pom.xml` — Maven build file (Paper API 1.21.11, Java 21).
- `src/main/resources/plugin.yml` — plugin descriptor.
- `src/main/resources/config.yml` — full configuration (all durations in seconds).
- `src/main/java/...` — Java source for all 14 classes.
- `DaggerSMP-2.2.0.jar` — the compiled, ready-to-run plugin jar (drop into your server's `plugins/` folder).

## Building

```
mvn -B package
```

Output appears in `target/DaggerSMP-2.2.0.jar`.

## Note on the source

The Java source in `src/main/java/` was decompiled from the working build jar.
Decompilers (CFR was used) reproduce the **logic** faithfully, but a handful of
constructs (anonymous `BukkitRunnable` subclasses with captured locals, some
generic type parameters, lambda forms) are lowered in ways that the Java
compiler will reject when fed back in directly. The shipped `DaggerSMP-2.2.0.jar`
is the canonical build — use the source for reference and edits, and expect
small touch-ups before a clean `mvn package` succeeds again.

The `plugin.yml` and `config.yml` are extracted verbatim from the jar and are
fully accurate.
