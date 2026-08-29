# Aethelon

A client-side-only Fabric mod for Minecraft **1.21.11** that gives you a clean
config menu (press **Right Shift**) and a few genuinely anti-cheat-safe survival
quality-of-life tools. It is designed so a single jar installs the whole "pack",
with every bundled mod pinned to an exact version and SHA-512 checksummed.

> Aethelon is display/utility only: it never alters entities, never manipulates
> the world, never reads worlds beyond your own client, and it plays every input
> through delayed, human-delay randomized simulated clicks. Use it only on
> servers that permit modded clients.

## Features

- **Right Shift** opens the Aethelon menu while in-game.
  - **Left click** a module to toggle it ON/OFF.
  - **Right click** a module to open its settings.
- **Auto Tool Swap** — when you start mining a block, swaps to the best tool in
  your hotbar after a short, randomized 1–5 tick delay (like a player doing it
  by hand). Never does anything while a screen is open, never reacts when you
  aren't the one mining, and ignores spectator.

## What's inside the jar

### Aethelon (this mod)
- MIT licensed. Original code only.

### Bundled mods (jar-in-jar)
| Mod | Version | License |
| --- | --- | --- |
| Fabric API | 0.141.6+1.21.11 | Apache-2.0 |
| Lithium | mc1.21.11-0.21.4-fabric | LGPL-3.0-only |
| FerriteCore | 8.2.0-fabric | MIT |
| ImmediatelyFast | 1.14.3+1.21.11-fabric | LGPL-3.0-only |
| C2ME | 0.3.6.0.0 | MIT |
| Krypton | 0.2.10 | LGPL-3.0-only |
| Mouse Tweaks | 1.21.11-2.30-fabric | BSD-3-Clause |
| uku's Armor HUD | 0.10.2+mc1.21.11 | MIT |
| ukulib | 1.10.2+1.21.11 | MPL-2.0 |

All bundled binaries are downloaded at build time from the Modrinth CDN and
SHA-512 verified. **No third-party binary is committed to this repository.**
See `NOTICE.txt` in the jar (and in `src/main/resources`) for full attribution
and their license texts.

### Installed separately (recommended)
| Mod | Version | License | Why separate |
| --- | --- | --- | --- |
| Sodium | mc1.21.11-0.8.14-fabric | Polyform Shield 1.0.0 | does not permit redistribution |
| EntityCulling | 1.10.5 | tr7zw Protective License | does not permit redistribution |

These are downloaded from their official channel, SHA-512 verified and copied
to your mods folder by the install script:

```
.\scripts\install-mods.ps1              # installs to %APPDATA%\.minecraft\mods
.\scripts\install-mods.ps1 -Dev         # installs to .\run\mods for the dev client
```

## Building

Requirements: JDK 21 or later (Gradle toolchain auto-resolves it),
Java 21 toolchain, and a network connection on first build.

```powershell
.\gradlew.bat build
```

The distributable jar is `build/libs/aethelon-1.21.11-0.1.0.jar`.

To launch the dev client (with Sodium + EntityCulling in `run/mods`):

```powershell
.\scripts\install-mods.ps1 -Dev
.\gradlew.bat runClient
```

## Configuration

New worlds / fresh installs start with auto-tool enabled at sensible defaults;
settings are saved to `config/aethelon.json` in your Minecraft directory.

## Legal

- This project is **not** affiliated with or endorsed by Mojang Studios or
  Microsoft.
- Aethelon source is MIT (see `LICENSE`).
- Third-party mods retain their own licenses; see `NOTICE.txt`.

**Minecraft, the Minecraft logo and all game assets belong to Mojang Studios / Microsoft.**