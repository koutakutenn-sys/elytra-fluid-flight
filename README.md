<h1 align="center">Elytra Fluid Flight</h1>

<p align="center"><strong>Keep gliding — and keep using firework boosts — underwater and in lava.</strong></p>

<p align="center">
  <img src="docs/icon-512.png" width="512" alt="Elytra Fluid Flight: a player gliding underwater with an elytra, trailing a firework rocket">
</p>

Elytra Fluid Flight is a small Fabric mod for Minecraft Java 26.2. Normally your elytra folds away the moment you touch water or lava; with this mod installed you keep flying, using the vanilla glide physics plus a configurable amount of extra drag.

[English](README.md) | [简体中文](README.zh-CN.md) | [繁體中文](README.zh-HK.md)

## Features

- **Glide in water and lava.** Enter a liquid while flying and the glide continues instead of ending.
- **Swimming stays swimming.** Hold sprint under water and the vanilla swimming state takes over — water movement, swimming and diving — and jump means "swim up" instead of opening the elytra. Gliding and swimming never overlap: stop sprinting to glide again.
- **Start flying while submerged.** Float off the bottom of the water or lava, then press jump to open your elytra.
- **Fireworks still work.** Right-click a firework rocket to boost — underwater and in lava too — with vanilla consumption and boost duration preserved.
- **Tunable drag.** Choose how strongly water and lava slow a glide down.
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
- Want to swim instead? Hold sprint with your eyes under water. That enters the vanilla swimming state: water movement takes over (swim, dive, float) and jump means "swim up" instead of opening the elytra. Stop sprinting and the glide resumes. Water only — sprinting in lava changes nothing.
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
  "lavaRequiresFireResistance": false,
  "lavaSwimmingWithFireResistance": false,
  "lavaSwimmingWithoutFireResistance": false
}
```

| Option | Default | Description |
| --- | --- | --- |
| `waterSpeedMultiplier` | `0.6` | Drag strength while gliding in water: lower means thicker water. See the notes below. |
| `lavaSpeedMultiplier` | `0.35` | Same, for lava — lower means thicker lava. |
| `lavaRequiresFireResistance` | `false` | Set to `true` to require the Fire Resistance effect before gliding in lava. When the effect runs out, lava gliding ends. |
| `lavaSwimmingWithFireResistance` | `false` | Allow the swimming state in lava while you have Fire Resistance. Vanilla never enters it outside of water. |
| `lavaSwimmingWithoutFireResistance` | `false` | Same, for lava swimming without Fire Resistance. |

Things worth knowing:

- The valid range is `0 < value <= 1`. It is a drag strength: lower values mean a thicker liquid, and `1.0` behaves exactly like vanilla air physics.
- For stability only a tenth of the configured strength is applied as damping each tick, so `0.6` keeps roughly 96% of your speed per tick and `0.35` roughly 93.5%. That gives a clear "thicker liquid" feel while still letting you glide; the raw value is never multiplied into your entire velocity, which would bring a glide to a standstill within half a second.
- If water and lava are detected at the same time on a liquid boundary, the lava setting wins.
- Lava is not swimmable in vanilla, so the two `lavaSwimming…` options are off by default. Turn on the one that matches your situation — they apply independently: holding sprint with your eyes in lava then puts you into the same swimming state as water, liquid movement takes over, and jump means "swim up" instead of opening the elytra.
- An out-of-range value falls back to that option's default and is logged. If the file is corrupt, the defaults are used and your file is left untouched so you can fix it.
- Settings are read at startup: restart the game or server after editing. In multiplayer, keep both sides the same.

## What this mod does not change

- Elytra durability, landing, levitation, ladder restrictions, and the vanilla "needs a working elytra" check.
- Drowning, lava burn, and firework explosion damage. This mod grants no Water Breathing and no Fire Resistance.
- Swimming: holding sprint under water always uses vanilla water movement, including buoyancy and the swimming pose.
- The firework rocket itself: no item or entity is replaced. Vanilla already propels a player who is fall-flying, so boosts simply work in liquids too.

## Compatibility

- Mods that change player movement or elytra physics, and server-side anticheat plugins, may need their own compatibility check.
- No Fabric API is required. Fabric API based modpacks should be fine as long as the mod is installed on both sides where relevant.
- Verified with 52 automated checks on a real Minecraft 26.2 Fabric server, using actual water and lava blocks. Manual graphical client flying, modpack combinations, flowing liquids and bubble columns are still worth a look on your setup.

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

Dual licensed, pick whichever you prefer: released into the public domain under the [Unlicense](https://unlicense.org), or under the [MIT License](https://opensource.org/license/mit). See [LICENSE](LICENSE) and [LICENSE-MIT](LICENSE-MIT).
