#!/usr/bin/env bash
# Build MobHealth and copy the jar into the CurseForge NeoForge test instance's mods/ folder,
# then you launch that instance from CurseForge to see the mod live.
#
# Usage: ./deploy.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
export JAVA_HOME="$ROOT/tools/jdk21"
export PATH="$JAVA_HOME/bin:$PATH"

INSTANCE="/mnt/c/Users/darre/curseforge/minecraft/Instances/MobHealth - Forge"
MODS="$INSTANCE/mods"

echo ">> Building MobHealth..."
"$ROOT/gradlew" build --console=plain

if [ ! -d "$MODS" ]; then
    echo "!! Instance mods folder not found: $MODS" >&2
    exit 1
fi

echo ">> Removing previous MobHealth jars from the instance..."
rm -f "$MODS"/mobhealth-*.jar

JAR="$(ls -t "$ROOT"/build/libs/mobhealth-*.jar 2>/dev/null | grep -v -- '-sources' | head -1 || true)"
if [ -z "$JAR" ]; then
    echo "!! No built jar found in build/libs" >&2
    exit 1
fi

cp "$JAR" "$MODS/"
echo ">> Deployed: $(basename "$JAR")"
echo ">> Launch the 'MobHealth - Forge' instance in CurseForge to test."
