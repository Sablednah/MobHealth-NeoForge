#!/usr/bin/env bash
# Build MobHealth and copy the jar into a NeoForge test instance's mods/ folder,
# then you launch that instance from CurseForge to see the mod live.
#
# Usage:   ./deploy.sh
# Override the target instance:
#          MOBHEALTH_INSTANCE="/path/to/instance" ./deploy.sh
#
# One instance per Minecraft line, so the branch you are on decides where the jar goes.
# The default instance name carries the Minecraft version for every line except 1.21.11,
# whose instance predates the others and keeps its original name.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"

MC_VERSION="$(sed -n 's/^minecraft_version=//p' "$ROOT/gradle.properties" | head -1)"
if [ -z "$MC_VERSION" ]; then
    echo "!! Could not read minecraft_version from gradle.properties" >&2
    exit 1
fi

# The JDK tracks Minecraft, not the calendar: 26.1 ships java-runtime-epsilon to players, so
# every calendar-versioned line needs 25 and the 1.21.x line still needs 21. A JAVA_HOME set
# by the caller always wins, so a system JDK can be used without editing this.
if [ -z "${JAVA_HOME:-}" ]; then
    case "$MC_VERSION" in
        1.*) WANT_JDK="jdk21" ;;
        *)   WANT_JDK="jdk25" ;;
    esac
    if [ -x "$ROOT/tools/$WANT_JDK/bin/java" ]; then
        export JAVA_HOME="$ROOT/tools/$WANT_JDK"
    else
        echo "!! Minecraft $MC_VERSION needs $WANT_JDK, and tools/$WANT_JDK is not there." >&2
        echo "!! Drop a JDK in tools/$WANT_JDK, or set JAVA_HOME yourself and rerun." >&2
        exit 1
    fi
fi
export PATH="$JAVA_HOME/bin:$PATH"

case "$MC_VERSION" in
    1.21.11) DEFAULT_INSTANCE="MobHealth - Forge" ;;
    *)       DEFAULT_INSTANCE="MobHealth - $MC_VERSION" ;;
esac
INSTANCE="${MOBHEALTH_INSTANCE:-/mnt/c/Users/darre/curseforge/minecraft/Instances/$DEFAULT_INSTANCE}"
MODS="$INSTANCE/mods"

echo ">> Building MobHealth for Minecraft $MC_VERSION (JDK: $(basename "$JAVA_HOME"))..."
"$ROOT/gradlew" build --console=plain

if [ ! -d "$MODS" ]; then
    echo "!! Instance mods folder not found: $MODS" >&2
    echo "!! Create a NeoForge $MC_VERSION instance named '$DEFAULT_INSTANCE'," >&2
    echo "!! or run: MOBHEALTH_INSTANCE=\"/path/to/instance\" ./deploy.sh" >&2
    exit 1
fi

# Name the jar exactly rather than taking the newest match. build/libs keeps whatever every
# other branch has built here, and "newest" is the right answer only until a build is up to
# date and does not rewrite its jar -- at which point another line's jar deploys silently.
MOD_VERSION="$(sed -n 's/^mod_version=//p' "$ROOT/gradle.properties" | head -1)"
JAR="$ROOT/build/libs/mobhealth-${MOD_VERSION}+mc${MC_VERSION}.jar"
if [ ! -f "$JAR" ]; then
    echo "!! Expected jar not found: $JAR" >&2
    echo "!! build/libs holds: $(ls "$ROOT/build/libs" 2>/dev/null | tr '\n' ' ')" >&2
    exit 1
fi

# A running instance holds the jar open, so Windows refuses to replace it. Say so plainly: this
# otherwise fails looking like a success, and you test a stale jar wondering why nothing changed.
instance_locked() {
    echo "!! Could not $1 the jar in the instance's mods folder." >&2
    echo "!! Is the '$(basename "$INSTANCE")' instance still running? Close Minecraft and retry." >&2
    exit 1
}

echo ">> Removing previous MobHealth jars from the instance..."
rm -f "$MODS"/mobhealth-*.jar || instance_locked "remove"

cp "$JAR" "$MODS/" || instance_locked "copy"

# Confirm the jar really landed and matches: a half-written copy is worse than a loud failure.
if ! cmp -s "$JAR" "$MODS/$(basename "$JAR")"; then
    echo "!! The deployed jar does not match the one just built." >&2
    exit 1
fi

echo ">> Deployed: $(basename "$JAR") ($(stat -c%s "$JAR") bytes)"
echo ">> Launch the '$(basename "$INSTANCE")' instance in CurseForge to test."
