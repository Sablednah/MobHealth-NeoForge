# Building MobHealth for more than one Minecraft version

Minecraft moved to calendar versioning with quarterly drops. Supporting 1.21.11, 26.1 and 26.2 is
therefore not a port to be finished — it is a treadmill to be made cheap. This is what each drop
actually cost, measured rather than predicted, and what those numbers say about how to carry three
lines from here.

**Status: 26.1 and 26.2 both ported, built and jarred (2026-08-26). Neither has been run in a
client.** A green compile is not a working mod, and the gap between them is where the interesting
failures live — see "What is still unverified".

## The targets

| Branch | Minecraft | NeoForge | Java | ModDevGradle |
|---|---|---|---|---|
| `main` | 1.21.11 | 21.11.42 | 21 | 2.0.141 |
| `mc26.1` | 26.1.2 | 26.1.2.95 | **25** | 2.0.141 |
| `mc26.2` | 26.2 | 26.2.0.59 | **25** | 2.0.144 |

The Java bump is not optional: 26.1 ships the `java-runtime-epsilon` JRE to players, so a mod
targeting 21 is targeting a runtime nobody has.

## The measured deltas

Branch, set the versions and toolchain, `compileJava`, fix, repeat. Each branch is cut from the one
before it, so `mc26.2`'s number is the 26.1 → 26.2 delta and not the 1.21.11 → 26.2 one.

| | 26.1 | 26.2 |
|---|---|---|
| errors | **25** | **5** |
| files touched | 4 | 3 |
| distinct API changes | 8 (2 of them silent) | 3 |
| errors in `client/` | 18, in 3 files | 5, in 3 files |
| errors server-side | 7, all in `DisplayManager` | 0 |

**26.2 was five times cheaper than 26.1**, which is the opposite of what the sibling repos led us to
expect: CityWorld's 26.2 cost a block-declaration model rewrite and LegendQuest's was 53 errors
against 26.1's 36. The reason is not luck, and it is the most useful thing this exercise produced —
see "Why 26.2 was cheap here".

### 26.1: the GUI drawing surface moved

Eight changes, one of which is most of the count, and two of which the compiler never mentioned.

| ours | 26.1 | sites |
|---|---|---|
| `GuiGraphics` | `GuiGraphicsExtractor` | 14 errors, 3 files |
| `drawString` | `text` | 4 (**0 errors**) |
| `renderItem` | `item` | 1 (**0 errors**) |
| `Toast.render` | `Toast.extractRenderState` | 1 |
| `GameRenderer.getProjectionMatrix(fov)` | `Camera.getViewRotationProjectionMatrix(dest)` | 2 |
| `ServerPlayer.displayClientMessage(c, bool)` | `sendSystemMessage` / `sendOverlayMessage` | 2 |
| `LivingDamageEvent.Post.getNewDamage` | `getHealthDamage` | 4 |
| `new ServerBossEvent(name, colour, overlay)` | now takes a `UUID` first | 1 |

Two of those are worth more than a row.

**`getHealthDamage`, not `getInflictedDamage`.** 26.1 split `Post`'s single damage figure in two,
and the names do not tell you which one you had. The constructors do: 1.21.11's `getNewDamage()`
returned `container.getNewDamage()`, and in 26.1 that same field is `getHealthDamage()` carrying the
same javadoc — *"the amount of health this entity lost during this sequence"*. `getInflictedDamage()`
is a new, different quantity. Choosing it would have compiled cleanly and quietly changed the number
every display mode reports.

**The extractor *does* have a transform stack.** LegendQuest's `docs/VERSIONS.md` records that
`GuiGraphicsExtractor` "exposes no transform stack", and rebuilt its floating combat text on
`ActiveTextCollector` because of it. That is wrong, and this port is the evidence: `pose()` exists on
both 26.1 and 26.2, returns the same `Matrix3x2fStack`, and **both `fill()` and `text()` snapshot it
into their render state**. Their four `pose()` errors were the same resolution cascade that hid our
`drawString` calls — the class had failed to resolve, so every call on it failed with it. So the
floating damage numbers here keep the `pushMatrix / translate / scale / popMatrix` they were written
with, and cost a rename like everything else.

That is worth taking as a general caution rather than a note about one method: **an error list from
a failed type is not a list of API changes.** It over-reports (14 of our 25 errors are one rename)
and under-reports at the same time (five call sites had to change and produced no error at all).

### 26.2: three renames, all the same move

| ours | 26.2 |
|---|---|
| `mc.options.hideGui` | `mc.gui.hud.isHidden()` |
| `gameRenderer.getMainCamera()` | `gameRenderer.mainCamera()` |
| `mc.getToastManager()` | `mc.gui.toastManager()` |

All three are one change seen from three sides: 26.2 gathered what used to hang off `Minecraft` and
`Options` onto the `Gui`. The HUD owns whether it is hidden, the `Gui` owns the toast queue, and
`Minecraft.setScreen` moved the same way — which cost LegendQuest five call sites and cost us none,
because this mod opens no screens of its own.

## Why 26.2 was cheap here

The two changes that made 26.2 expensive across the other repos miss this mod completely, and not by
accident:

- **`EntityType` split its constants into `EntityTypes`.** This is the change that broke 145 of
  CityWorld's material constants and four of LegendQuest's sites. MobHealth **names no vanilla entity
  type at all** — `EntityCategorizer` asks what an entity *is* (`Enemy`, `NeutralMob`, `Mob`,
  `WitherBoss`, `EnderDragon`) rather than matching it against a list of names, and per-entity
  behaviour is configured as registry ids in a TOML file, which is data and does not compile.
- **`ChatFormatting.isFormat()` went away.** That lands on code parsing legacy `&`-codes. MobHealth
  builds components directly and never parses one.

So the pattern behind both is the same: **the surface that survives a drop is the one that describes
entities instead of naming them.** That is not a build decision, and no branch strategy would have
produced it — it is the same lesson CityWorld drew from generating `Material.java` and from moving
its palettes onto block tags, arriving at a mod small enough that it had never needed either.

## Where the risk actually is

Both drops agree, and they agree with the mod's own shape:

| | 26.1 | 26.2 |
|---|---|---|
| `client/` | 18 errors, 3 files | 5 errors, 3 files |
| server / common | 7 errors, 1 file | 0 errors |

**23 of the 30 errors across two drops are in `client/`.** Every one of them is the GUI drawing
surface or the camera: the four client files are the only place this mod touches rendering, and
rendering is what Mojang has been rewriting. The server side is a narrower story still — its seven
errors are **three changes in one file**, `DisplayManager`, and 26.2 did not touch it at all.

That is the same split LegendQuest measured (25 of 36, then 37 of 53) and it points the same way:
**a shared server tree with `client/` per version** is the shape the divergence has, if the branches
are ever merged into one tree.

**Not doing that yet, on the same reasoning as the siblings.** Two drops is two. Branch-per-version
costs nothing to keep until 26.3 (~Sept 2026) tests whether the client stays the whole story, and
merging three branches is cheap now and cheap later; guessing wrong about the mechanism is not.

One thing does argue more strongly here than in LegendQuest, though: our client divergence is
**method renames on a class we hold as a local variable**, not a supertype signature change. The one
supertype that did move — `Toast.render` → `Toast.extractRenderState` — is a single method on a
single class. So a `src/compat/<version>/java` shim is not ruled out here the way it is there. It is
just not yet worth building.

## What is still unverified

Everything below the compiler. Both 26.x branches produce a jar; neither has been loaded by a game.
The failures that would not show up in a build, roughly in order of how likely they are to bite:

1. **The floating bars and damage numbers projecting to the wrong place.**
   `Camera.getViewRotationProjectionMatrix` is a *different route to the matrix*, not a rename, and a
   wrong matrix does not throw — it draws bars in plausible but wrong positions. This is the single
   thing most worth a client test, and it is visible in three seconds of hitting a mob.
2. **The toast.** `extractRenderState` is a supertype change, so the drawing now happens in the
   extract pass. If the toast renders blank or mispositioned, that is where to look.
3. **`sendOverlayMessage` / `sendSystemMessage`.** The split is documented rather than tested; the
   symptom of getting them the wrong way round is chat and action bar swapping.
4. **The `ServerBossEvent` UUID.** A fresh random UUID per bar is right for a transient bar, but
   nothing has confirmed the client accepts it as a distinct bar per mob.
5. **The config screen.** Never opened on 26.x. `IConfigScreenFactory` and `ConfigurationScreen` are
   NeoForge classes and compiled, which says nothing about the screen's layout surviving 26.1's
   render split.

## Working across the branches

- **`main` carries the docs.** This file, and anything else written up, lives here only, so a
  write-up never has to be merged three ways.
- **Features land on `main` first**, then cherry-pick forward. The damage indicators were written on
  `main` and crossed both versions inside the port commits, which is why the port cost one pass
  rather than three.
- **Retarget by editing `gradle.properties`** (`minecraft_version`, `minecraft_version_range`,
  `neo_version`), plus the JDK in `build.gradle` and ModDevGradle's version in the plugins block.
- **`./deploy.sh` reads the branch's `minecraft_version`** and picks both the JDK and the CurseForge
  instance from it, so the same command does the right thing on every branch. It names the expected
  jar exactly rather than taking the newest in `build/libs`, because that directory keeps whatever
  every other branch has built there.

## Not yet done

- **CI.** No matrix builds the three branches, so a version can rot silently between drops.
  CityWorld's `selftest.yml` is the model; cache keyed on the NeoForge version, because a cold run
  has to let NeoForm decompile Minecraft.
- **Release plumbing.** Nothing uploads per-version jars to CurseForge or Modrinth with the right
  game versions declared. The jar names already carry the version a workflow would parse.
