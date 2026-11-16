#!/usr/bin/env bash
# Offline build with javac. Usage: ./build.sh <folder with the compile jars>
set -euo pipefail
JARS="$1"; HERE="$(cd "$(dirname "$0")" && pwd)"
VER="$(grep '^mod_version=' "$HERE/gradle.properties" | cut -d= -f2)"
# Patched NeoForge client classes must come before the vanilla jar
CP="$( (find "$JARS" -name "*neoforge*client*.jar"; find "$JARS" -name "*.jar" ! -name "*neoforge*client*.jar") | tr "\n" ":")"
rm -rf "$HERE/out" && mkdir -p "$HERE/out/classes"
javac --release 21 -encoding UTF-8 -proc:none -nowarn -cp "$CP" -d "$HERE/out/classes" $(find "$HERE/src/main/java" -name '*.java')
cp -r "$HERE/src/main/resources/." "$HERE/out/classes/"
cp "$HERE/LICENSE" "$HERE/out/classes/LICENSE_create_dt_fix"
(cd "$HERE/out/classes" && jar --create --file "$HERE/out/create_dt_fix-$VER.jar" .)
echo "Built: $HERE/out/create_dt_fix-$VER.jar"
