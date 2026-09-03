# CLAUDE.md

Working notes for this repo. Read `README.md` for what the mod does and `docs/VERSIONS.md` for the
multi-version porting record — this file is only the conventions those two do not state.

## Three branches, one mod

| Branch | Minecraft | NeoForge | JDK |
|---|---|---|---|
| `main` | 1.21.11 | 21.11.42 | 21 |
| `mc26.1` | 26.1.2 | 26.1.2.95 | **25** |
| `mc26.2` | 26.2 | 26.2.0.59 | **25** |

- **Features land on `main` and cherry-pick forward** (`git cherry-pick -x`). Never develop on a
  version branch.
- **Documentation lives on `main` only.** `README.md`, `docs/` and this file are not carried
  forward, so do not "fix" their absence on `mc26.*`.
- **Code that ships must go to all three.** A change to a user-visible string or behaviour is only
  half-done on `main` — check whether `mc26.1` and `mc26.2` carry the same lines before calling it
  finished.
- Compile each branch with **its own JDK** before pushing: `JAVA_HOME=$PWD/tools/jdk21` on `main`,
  `tools/jdk25` on the 26.x branches. `tools/` is gitignored, so a fresh clone has to repopulate it.
- `./deploy.sh` builds and drops the jar into a CurseForge test instance, choosing the JDK and the
  instance from the branch's `minecraft_version`.

## Versioning

`gradle.properties` distinguishes what the mod is **built against** from what it **refuses to load
on**, and the ranges are deliberately wider than the build:

```
minecraft_version=26.1.2          # one exact build
minecraft_version_range=[26.1,26.2)   # the line the jar actually runs on
```

Keep that distinction. A range pinned to the build strands users on an earlier patch for no reason.
All three branches share one `mod_version`, so a jar is identified by
`mobhealth-<mod_version>+mc<minecraft_version>.jar` — the `+mc` suffix is load-bearing, and both
publishing scripts parse it.

## Publishing

A published GitHub release fans its attached jars out to **CurseForge and Modrinth** automatically —
`.github/workflows/curseforge.yml` and `modrinth.yml`, each calling its `scripts/*-upload.sh`. Both
live on `main` only, because GitHub runs release workflows from the default branch.

- Attach **all three jars** to the release or a line goes unpublished.
- Both workflows skip rather than fail when unconfigured, and both take a `workflow_dispatch` for
  re-uploading an existing tag.
- Secrets and variables are already set on the repo. **Never handle the tokens** — not to read them,
  echo them, or write them to a file.
- `docs/curseforge-description.md` is the source for both store pages, but the pages themselves are
  **pasted in by hand**. Editing that file changes nothing until someone uploads it.
- Store rules differ: Modrinth requires alt text on every image (a stated rejection cause) and
  renders HTML through the js-xss default whitelist, which does pass `table`/`td` with
  `align`/`width` and `img` with `src`/`alt`/`width`.

## Permissions

`docs/NODES.md` is the reference — one node (`mobhealth.see`), and two things that surprise people:
nameplates cannot be hidden per player (a name tag is one shared entity property), and the commands
are gated by vanilla operator level rather than by nodes, so LuckPerms cannot grant
`/mobhealth reload` at all. Keep that file in step with any change to gating.

## Gotchas that have already cost time

- **Never enumerate display modes in prose.** "chat & boss bar" has gone stale twice as modes were
  added. Name the exception instead — the nameplate — and let the rest be implied.
- **`${VAR:?message}` parses quotes even inside double quotes.** An apostrophe in one of those shell
  error messages silently swallows the rest of the script. Write those messages without `'` or `"`.
- **`Entity.getX(double)`/`getY`/`getZ` are not partial-tick interpolation** — they return a point
  along the bounding box. Use `entity.getPosition(float partialTick)`.
- **An error count from a failed type is not a list of API changes.** It over- and under-reports at
  the same time; read the whole error list and count distinct causes.
- Gradle on `/mnt/d` (a Windows drive over 9p) can degrade mid-session into hanging `:compileJava`.
  `wsl --shutdown` from PowerShell clears it.
- `jq` is not installed in this WSL environment, though CI runners have it. Scripts may rely on it;
  local dry runs need it fetched.
