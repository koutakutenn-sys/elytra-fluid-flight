# Elytra Fluid Flight

**Keep gliding — and keep using firework boosts — underwater and in lava.**

Elytra Fluid Flight is a small Fabric mod for Minecraft Java 26.2. Normally your elytra folds away the moment you touch water or lava; with this mod installed you keep flying, using the vanilla glide physics plus a configurable amount of extra drag.

[English](README.md) | [简体中文](README.zh-CN.md)

## Features

- **Glide in water and lava.** Enter a liquid while flying and the glide continues instead of ending.
- **Start flying while submerged.** Float off the bottom of the water or lava, then press jump to open your elytra.
- **Fireworks still work.** Right-click a firework rocket to boost — underwater and in lava too — with vanilla consumption and boost duration preserved.
- **Tunable drag.** Choose how much of your speed is kept each tick in water and in lava.
- **Optional Fire Resistance rule.** Require an actual Fire Resistance effect before lava gliding is allowed.
- **Vanilla stays vanilla.** Elytra durability, landing, levitation and ladder restrictions, and drowning, burning and explosion damage all still apply.

## Requirements

| | |
| --- | --- |
| Minecraft | Java Edition **26.2** |
| Mod loader | **Fabric Loader 0.19.5** or newer (verified with 0.19.5) |
| Java | **25** or newer |
| Other mods | None required — no Fabric API, Cloth Config or Mod Menu |

This is a Fabric mod. It does not work on Forge, NeoForge, Bedrock Edition, or other Minecraft versions.

## Installation

**Singleplayer (client only)**

1. Install Fabric Loader 0.19.5 or newer for Minecraft 26.2.
2. Put `elytra-fluid-flight-<version>+mc26.2.jar` into your instance's `mods` folder.
3. Launch Minecraft. That's it — no extra dependencies.

Do not install the file whose name ends with `-sources.jar`; that one is for developers.

**Multiplayer**

- Both the client **and** the server must have the mod installed.
- Both sides should use the same settings. This version does not sync config between client and server.

## How to use

- Fly into water or lava with your elytra open: the glide continues, at the speed set by your config.
- Already in a liquid? Swim clear of the bottom so you are no longer standing on a block, then press jump to open the elytra.
- Right-click a firework while gliding — including in water and lava — to boost.
- Landing on the ground closes the elytra, exactly as in vanilla.

## Configuration

The config file is created automatically the first time you launch:

- Client: `.minecraft/config/elytra_fluid_flight.json`
- Server: `<server folder>/config/elytra_fluid_flight.json`

```json
{
  "waterSpeedMultiplier": 0.6,
  "lavaSpeedMultiplier": 0.35,
  "lavaRequiresFireResistance": false
}
```

| Option | Default | Description |
| --- | --- | --- |
| `waterSpeedMultiplier` | `0.6` | Fraction of the vanilla elytra velocity kept each tick while in water. |
| `lavaSpeedMultiplier` | `0.35` | Same, for lava. |
| `lavaRequiresFireResistance` | `false` | Set to `true` to require the Fire Resistance effect before gliding in lava. When the effect runs out, lava gliding ends. |

Things worth knowing:

- The valid range is `0 < value <= 1`. Each tick, the velocity produced by the vanilla elytra physics is multiplied by this value. It is not a fixed top-speed percentage, and vanilla air drag still applies on top.
- Lower values mean more drag. The defaults `0.6` and `0.35` slow you down quickly and are meant to be used together with fireworks. For long, floaty glides, try `0.9` and `0.8`.
- If water and lava are detected at the same time on a liquid boundary, the lava setting wins.
- An out-of-range value falls back to that option's default and is logged. If the file is corrupt, the defaults are used and your file is left untouched so you can fix it.
- Settings are read at startup: restart the game or server after editing. In multiplayer, keep both sides the same.

## What this mod does not change

- Elytra durability, landing, levitation, ladder restrictions, and the vanilla "needs a working elytra" check.
- Drowning, lava burn, and firework explosion damage. This mod grants no Water Breathing and no Fire Resistance.
- The firework rocket itself: no item or entity is replaced. Vanilla already propels a player who is fall-flying, so boosts simply work in liquids too.

## Compatibility

- Mods that change player movement or elytra physics, and server-side anticheat plugins, may need their own compatibility check.
- No Fabric API is required. Fabric API based modpacks should be fine as long as the mod is installed on both sides where relevant.
- Verified with 47 automated checks on a real Minecraft 26.2 Fabric server, using actual water and lava blocks. Manual graphical client flying, modpack combinations, flowing liquids and bubble columns are still worth a look on your setup.

## Building from source

JDK 25 is required. The project ships with a Gradle wrapper, so the first build downloads its dependencies.

```sh
# macOS / Linux
chmod +x gradlew
./gradlew build
```

```bat
:: Windows
gradlew.bat build
```

The result is `build/libs/elytra-fluid-flight-<version>+mc26.2.jar`.

Integration tests run against a real Minecraft 26.2 + Fabric Loader 0.19.5 server on a temporary test world. They start a server bound to `127.0.0.1` on a dynamic port and shut it down when finished; your own saves are never touched. Run them only if you have read and accepted the Minecraft EULA:

```sh
./gradlew runIntegrationTest -PacceptMinecraftEula=true
```

The summary is written to `run-test/test-result.txt`.

## Reporting problems

Please open an issue and include your Minecraft version, Fabric Loader version, whether you were in water or lava, your config file, and the relevant part of the log.

## License

MIT — see [LICENSE](LICENSE).
