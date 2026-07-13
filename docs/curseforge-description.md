# MobHealth

**This simple but invaluable mod lets you know the damage you just caused to a mob (including other players), and how much health it has left.**

MobHealth is a modern NeoForge rewrite of the classic MobHealth Bukkit plugin — the same idea it had in 2011, rebuilt for Minecraft 1.21.11 with six display modes you can mix and match.

Hit a zombie, and MobHealth tells you exactly what you did to it:

`Zombie [||||||||||] 14/20 (-6)`

Or shows you a floating health bar above its head. Or a boss bar. Or a name-tag bar. Your call — every mode is independent, and you can enable as many as you like.

---

## Display modes

<table>
  <tr>
    <td align="center" width="33%">
      <img src="https://media.forgecdn.net/attachments/description/1608871/description_5d33a378-d5df-4786-a7e5-ad04362709e7.png" alt="Graphical display mode" width="100%"><br>
      <b>Graphical</b><br>
      <sub>A floating pixel bar above the mob</sub>
    </td>
    <td align="center" width="33%">
      <img src="https://media.forgecdn.net/attachments/description/1608871/description_1642c25b-61ff-41fc-9910-d12a577ee356.png" alt="Toast display mode" width="100%"><br>
      <b>Toast</b><br>
      <sub>An achievement-style popup, with the weapon used</sub>
    </td>
    <td align="center" width="33%">
      <img src="https://media.forgecdn.net/attachments/description/1608871/description_1eacbae6-3c6b-45fa-abf5-ddddce3ca56e.png" alt="Chat display mode" width="100%"><br>
      <b>Chat</b><br>
      <sub>Damage and health, in your chat log</sub>
    </td>
  </tr>
  <tr>
    <td align="center" width="33%">
      <img src="https://media.forgecdn.net/attachments/description/1608871/description_7e17833d-d5b4-4c0f-9a41-074d4b27bbd0.png" alt="Action bar display mode" width="100%"><br>
      <b>Action bar</b><br>
      <sub>The readout above your hotbar</sub>
    </td>
    <td align="center" width="33%">
      <img src="https://media.forgecdn.net/attachments/description/1608871/description_6f1809e4-8127-47de-8759-94ba85bcdeff.png" alt="Nameplate display mode" width="100%"><br>
      <b>Nameplate</b><br>
      <sub>A health bar on the mob's name tag</sub>
    </td>
    <td align="center" width="33%">
      <img src="https://media.forgecdn.net/attachments/description/1608871/description_f77542be-769d-4137-9852-3b4d81471ffa.png" alt="Boss bar display mode" width="100%"><br>
      <b>Boss bar</b><br>
      <sub>The vanilla widget at the top of the screen</sub>
    </td>
  </tr>
</table>

| Mode | What it looks like | Needs the mod on the client? |
|------|--------------------|------------------------------|
| **Chat** | A message with damage dealt and health remaining | No |
| **Action bar** | The same readout on the line above your hotbar | No |
| **Nameplate** | A coloured health bar on the mob's name tag | No |
| **Boss bar** | The vanilla boss-bar widget at the top of the screen | No |
| **Toast** | An achievement-style popup showing the mob, the weapon used, and its health | Yes |
| **Graphical** | A crisp pixel health bar floating above the mob in the world | Yes |

Bars are coloured by remaining health — green, to yellow, to red.

**The first four modes work on completely unmodified vanilla clients.** Install MobHealth on your server and every player gets chat, action bar, nameplate and boss-bar health readouts without downloading anything. Players who *also* install the mod additionally get toasts and graphical bars.

MobHealth runs happily as a **server-only**, **client-only**, or **both-sides** install. Client-only on a vanilla server? You still get graphical bars over everything you can see.

---

## Choose what gets a health bar

- Per-group toggles for **hostile**, **neutral**, **passive**, **boss** mobs and **other players** (PvP).
- **Per-entity overrides** that beat the group setting — `"minecraft:villager=false"`, `"somemod:custom_boss=true"`. Works with modded mobs.
- **Hide until damaged**, so untouched mobs stay clean.
- Choose the **audience**: only the attacker, or everyone within a configurable radius.
- Per-group **display timers** — let boss bars linger, let chicken bars vanish.

## Make the bars yours

Text bars (chat / action bar / nameplate) let you set the segment count, the filled and empty glyphs, and whether the numbers show as `14/20`, `70%`, or not at all.

Graphical bars have four styles — **solid, rounded, segmented, tapered** — plus width, height, scale, vertical offset, draw distance, optional line-of-sight checks, and optional scale-and-fade with distance so they feel anchored in the world rather than pasted on the screen.

## Server-enforced graphical settings

Graphical bars are drawn by the client, but a server can enforce any of them. Each option resolves as *server override if set, otherwise the client's own choice*, so admins can force line-of-sight (no seeing mobs through walls), cap the draw distance, or lock a consistent look — while leaving everything else up to the player. On a vanilla server, nothing is enforced and the client keeps full control.

---

## Commands

| Command | Who | Description |
|---------|-----|-------------|
| `/mobhealth toggle [on\|off]` | Everyone | Turn **your own** displays on or off. Your choice persists across logouts and deaths. |
| `/mobhealth reload` | Ops (level 2+) | Re-push settings to online players. Config edits auto-apply on save, so this is rarely needed. |

## Permissions

MobHealth uses NeoForge's permission system, so it works out of the box with vanilla operator levels — and if you run **LuckPerms** you can manage the nodes per-group with no extra setup.

| Node | Default | Controls |
|------|---------|----------|
| `mobhealth.see` | everyone | Whether a player receives displays at all. Deny it to hide MobHealth from a rank. |

```
/lp group guest permission set mobhealth.see false
```

---

## Configuration

Two TOML files are generated on first run, and both are editable **in-game** via **Esc → Mods → MobHealth → Config**:

- `mobhealth-common.toml` — server side: what is shown, to whom, and how.
- `mobhealth-client.toml` — client side: the appearance of your graphical bars.

Changes apply as soon as you save. The full config reference, plus ready-made recipes (vanilla-friendly server, MMO always-on bars, PvP fairness, classic chat-only, immersive graphical-only), is on the GitHub page linked below.

---

## Requirements

- Minecraft **1.21.11**
- **NeoForge** 21.11.42+
- Java 21
- No other dependencies.

---

## About

MobHealth was originally a Bukkit plugin (2011–2016, ~1 million downloads) by Sablednah. This is a complete rewrite for NeoForge by the same author — none of the original code carries over, but the spirit does. Licensed **MIT**.

Bug reports, feature requests and source: **https://github.com/Sablednah/MobHealth-Forge**
