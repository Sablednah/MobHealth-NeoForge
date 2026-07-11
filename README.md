# MobHealth (NeoForge)

A modern **NeoForge** rewrite of the classic [MobHealth](https://github.com/Sablednah/Mobhealth)
Bukkit plugin. It shows the damage you deal to a mob and its remaining health, through any
combination of display modes — from vanilla-friendly text to real graphical floating bars.

- **Minecraft:** 1.21.11
- **Loader:** NeoForge 21.11.42
- **Java:** 21

## Display modes

All modes are independently toggleable. The first three work on **unmodified vanilla clients**
(server-side only); the graphical bars require the mod on the client.

| Mode | Description | Needs client mod? |
|------|-------------|-------------------|
| **Chat** | A message to the attacker with damage dealt and health left | No |
| **Nameplate** | A coloured health bar on the mob's name tag | No |
| **Boss bar** | The top-of-screen boss-bar widget | No |
| **Graphical** | A pixel health bar floating above the mob | Yes |

## Highlights

- **Configurable targets:** hostile / neutral / passive groups, plus `bosses` and `players`
  toggles and per-entity `overrides` (`"minecraft:villager=false"`).
- **Audience:** show to just the attacker, or everyone nearby (configurable radius).
- **Content:** bar, numbers, or both — per mode.
- **Bar styling:** segment count, glyphs, colour by health (green → yellow → red), value style
  (`14/20` or `70%`). Constant-width text bars by default.
- **Timeouts:** per-group display duration and a short death timeout so bars clear on a kill.
- **Graphical bars:** vertical offset, size, distance limit, line-of-sight occlusion, optional
  numbers — all client-side.
- **Commands & permissions:**
  - `/mobhealth reload` — ops.
  - `/mobhealth toggle [on|off]` — anyone; mutes/unmutes your own displays (persists).
  - `mobhealth.see` permission node (default: everyone) gates who receives displays — manageable
    with [LuckPerms](https://luckperms.net/) (NeoForge build); falls back to op levels otherwise.
- **Server-enforced graphical gate:** the server's `graphical` config (and each player's
  permission/mute) controls whether modded clients draw graphical bars.
- **Loader-portable core:** the `core` package has no Minecraft imports, easing a future port.

## Configuration

- Server/common: `config/mobhealth-common.toml` (display modes, targets, bar styling, timing).
- Client: `config/mobhealth-client.toml` (graphical bar appearance).
- Both are editable in-game via **Mods → MobHealth → Config**.

## Building

Requires a JDK 21.

```bash
./gradlew build
# output: build/libs/mobhealth-<version>.jar
```

Drop the jar into your NeoForge instance's `mods/` folder (client and/or server).

## License

[MIT](LICENSE) © Darren Douglas (Sablednah)
