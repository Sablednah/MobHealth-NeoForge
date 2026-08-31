# MobHealth permission nodes

Every permission MobHealth defines, what it gates, and what it does **not** gate.

Accurate as of **MobHealth 2.5.1**. Identical on all three lines — 1.21.11, 26.1 and 26.2 register
the same nodes, so nothing here is version-specific.

MobHealth uses NeoForge's `PermissionAPI`. With no permissions manager installed, each node falls
back to the default below. Install a manager with a NeoForge build — **LuckPerms**, for instance —
and the nodes become manageable per player, group and rank with no extra setup on MobHealth's side.

---

## The nodes

| Node | Type | Default | Controls |
|------|------|---------|----------|
| `mobhealth.see` | boolean | `true` (everyone) | Whether a player **receives** MobHealth displays. |

That is the complete list. MobHealth deliberately defines one node rather than one per display
mode: which modes are active is a **server config** decision (`mobhealth-common.toml`), and which
players are eligible for them is the permission decision. Splitting the modes across seven nodes
would put the same switch in two places.

### `mobhealth.see`

Deny it to hide MobHealth entirely from a rank — a guest group, a spectator rank, a hardcore world
where health readouts are the point of the challenge.

```
/lp group guest permission set mobhealth.see false
/lp user Steve permission set mobhealth.see false
```

It is evaluated per player, not per mob, so a denied player sees nothing from any mob. Granting it
back does not force a mode on: the server's config still decides which modes exist at all.

---

## What `mobhealth.see` covers

| Display mode | Gated by `mobhealth.see`? | Notes |
|---|---|---|
| Chat | Yes | Checked per hit. |
| Action bar | Yes | Checked per hit. |
| Toast | Yes | Checked per hit. |
| Damage indicators | Yes | Checked per hit. |
| Boss bar | Yes | Checked per hit. |
| Graphical | Yes | Enforced client-side via the policy the server pushes — see *Refresh timing*. |
| **Nameplate** | **No** | **Cannot be hidden per player.** |

⚠ **Nameplates are the exception, and it is a Minecraft limitation rather than an oversight.** A
nameplate bar is written into the entity's custom name, which is a property of the *entity* and is
replicated to everyone who can see it. There is no per-viewer version of it. A player denied
`mobhealth.see`, or one who has run `/mobhealth toggle off`, still sees nameplate bars.

If you need a rank to see no MobHealth output whatsoever, turn the nameplate mode off server-wide
in `mobhealth-common.toml` rather than relying on the permission.

---

## Command permissions

MobHealth's commands are gated by **vanilla operator levels**, not by permission nodes:

| Command | Requirement | Level |
|---------|-------------|-------|
| `/mobhealth toggle [on\|off]` | Anyone | 0 |
| `/mobhealth reload` | Operator / gamemaster | 2 |

⚠ **These are not nodes, so a permissions manager cannot grant them.** LuckPerms can neither give
`/mobhealth reload` to a non-operator nor take it from an operator, because the check never reaches
`PermissionAPI`. Grant operator level 2 if someone needs it.

`/mobhealth toggle` is a personal mute. It is independent of `mobhealth.see` — the permission
decides whether a player *may* receive displays, the toggle whether they currently *want* them —
and both must be satisfied. It persists across logout and death, and the same nameplate caveat
above applies to it.

---

## Refresh timing

The server-side modes (chat, action bar, toast, damage indicators, boss bar) re-check
`mobhealth.see` on **every hit**, so a permission change takes effect immediately.

Graphical bars are drawn by the client, so the server pushes each player their effective permission
as a policy packet. That happens on **login**, on **`/mobhealth toggle`**, and on
**`/mobhealth reload`**. Changing a player's `mobhealth.see` in LuckPerms mid-session therefore does
not reach their graphical bars until one of those occurs — run `/mobhealth reload` after a
permission change, or have them reconnect.

---

## Installs without a server

`mobhealth.see` exists only where MobHealth is installed **on the server**. A client-only install
against a vanilla server has no node to check and no policy packet to receive, so the client's own
graphical bars and its config are the only controls in play. Nothing on this page applies to that
setup.
