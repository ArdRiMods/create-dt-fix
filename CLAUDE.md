# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A server-side-only NeoForge 1.21.1 mod (`create_dt_fix`) that makes Create 6.0.10 machines (saw, drill, deployer, add-on breakers) work with Dynamic Trees 1.7+. It is pure mixins: `CreateDtFix.java` is an empty `@Mod` entry point. Create and Dynamic Trees are `compileOnly` dependencies and are never bundled.

The project owner is a pack maker, not a Java programmer; the code is written by Claude and tested in game by the owner. There are no automated tests. Verification means building, then the owner tests in their pack.

## Build

- `./gradlew build`: the jar goes to `build/libs/`. Needs internet (ModDevGradle plus the Modrinth Maven for Create and Dynamic Trees). CI (`.github/workflows/build.yml`) runs exactly this on every push and PR.
- `./build.sh <jar-folder>`: offline javac build to `out/create_dt_fix-<ver>.jar`. The folder must hold the Create, Dynamic Trees, NeoForge client/universal, SRG client, FML loader and sponge-mixin jars (README "Building" lists them). The NeoForge client jar is put first on the classpath on purpose: its patched classes must shadow vanilla.

Versions live in `gradle.properties` (`mod_version`, `neo_version`, `create_version`, `dynamic_trees_version`). Java 21.

## Architecture

All behaviour is in `src/main/java/dev/createdtfix/mixin/`, registered in `src/main/resources/create_dt_fix.mixins.json` (`defaultRequire: 1`, so a missing injection target crashes at launch instead of silently doing nothing). Every mixin uses `remap = false`. A new mixin class must be added to that JSON list.

How the pieces fit:
- **`MixinPlugin`** (set as `plugin` in the mixins JSON) is the Create-update safety. Before each mixin is applied, it reads the original Create and Dynamic Trees class files (`getClassNode(name, false)`, no transformers) and checks every method, field and call that mixin needs. If one is missing, only that mixin is skipped, with a warning in the log. It also skips `DynamicTreeMixin` when Create's `isDynamicBranch` is no longer just `return false` (Create fixed it upstream). A new mixin, or a changed target, needs its entry in `MixinPlugin.FIXES`.
- **`Config`** is `config/create_dt_fix-common.toml` (COMMON, so pack makers ship it in `config/`). There is one switch per fix, all on by default, and each mixin reads its switch when it runs (`Config.on(...)`, which counts as "on" before the file loads). The saw fix and the `TreeCutterMixin` guard have no switch on purpose: turning them off only brings back trees vanishing with no drops. (A `sawFix` switch existed during 1.1.0 development and was removed: with it off, a moving saw stalled at a branch, because `SawMovementBehaviour.shouldDestroyStartBlock` skips `destroyBlock` for Dynamic Trees branches, and a stationary saw fell through to the breaker path and still cut the tree.)
- **`DynamicTreeMixin`** `@Overwrite`s Create's `compat.dynamictrees.DynamicTree`. Create ships it with the logic commented out; this restores it, based on Create PR #9898. This is the core saw/drill compat: it cuts the whole tree and gathers drops so Create routes them into contraption storage or onto the ground.
- **Breaker entry points** (`SawBlockEntityMixin`, `SawMovementBehaviourMixin`, `BlockBreakingKineticBlockEntityMixin`, `BlockBreakingMovementBehaviourMixin`) send Dynamic Trees branches and roots down the compat path for stationary and moving breakers. The `Mth.clamp` redirects in the `tick` methods keep break cracks showing on branches.
- **`ToolDir`** is a server-thread `ThreadLocal` handoff. A breaker mixin records which side of the tree faces the machine just before the cut, and the Dynamic Trees cut reads it once (`take`) so the falling-tree animation falls away from the machine. Always set it right before the cut; `take` clears it.
- **`BranchBlockMixin`** (`futureBreak`) sends deployer-with-axe drops to the deployer.
- **`BlockBehaviourMixin`** (`getDestroyProgress`) makes axes break surface roots at log speed.
- **`TreeCutterMixin`** (`isLog`) stops Create's vanilla tree cutter from treating Dynamic Trees branches as logs, which used to delete trees with no drops.

The mixin targets were checked against the real Create 6.0.10 and Dynamic Trees 1.7.2 jars. When you change a target or descriptor, check it against those jars' bytecode (`javap -c -p`) instead of guessing; Create's internals change between versions.

## Create version range

`neoforge.mods.toml` allows Create `[6.0.10,6.1)`. Within 6.0.x, `MixinPlugin` turns off only the fixes whose Create code changed, so a Create patch can't crash the game at launch. Create 6.1 or newer still stops the game, so the mod gets checked first (or retired, if Create merged the fix upstream). Don't widen the range without checking the new Create source and running `verify-mixin-targets`. The Create roller is not covered (it uses its own break code).

## Background and roadmap

From the original design chat (owner's notes, not in the repo):
- The bug: Create issue #9657 (confirmed; duplicates such as #10220 were reported on 6.0.10), also reported as Dynamic Trees #1216. Create's fix is PR #9898, which was not merged at the time.
- Only Minecraft 1.21.1 has the no-drops bug. Create 6.0.8 for 1.20.1 ships the Dynamic Trees saw compat active, so a 1.20.1 port would only bring the extras (falling trees, roots, deployer drops, break cracks). It is low priority, done only if users ask.
- Dynamic Trees has a Fabric 1.21.1 beta, so a Fabric port is possible if Create runs on Fabric 1.21.1 (not yet checked).
- The old "Dynamic Trees Create Compat" add-on does not work on Create 6 and must not be in the pack alongside this mod.

1.1.0 (2026-11-19): Create-update safety (`MixinPlugin`), the config file (`Config`), the Create range widened to `[6.0.10,6.1)`, and the "For modpack makers", "Config" and "Tested with" page sections. Tested in game by the owner.

Planned for 1.2.0 (split out of 1.1.0 so the replant tests don't hold up the safety fix):
- Optional `sawSilkTouchMushrooms` switch (default off): saws drop Dynamic Trees Plus mushroom stems. Today the saw passes no tool to `Species.getBranchesDrops`, and the DT+ mushroom branch loot table needs Silk Touch, so stems drop nothing (same as plain Create on vanilla huge mushrooms). A deployer with a Silk Touch axe already drops them. Owner asked for this on 2026-11-19, for after 1.1.0.
- Auto-replant. After a machine cuts a tree at its lowest branch, take one seed of that tree from the drops (never a free seed) and plant it (`TreeHelper.findRootNode`, `Species.getSeedStack`, `Species.plantSapling`). The config default is off.
- In-game control per machine: shift + right-click a breaker (saw, drill) to switch between "replant" and "clear forest", with an action-bar message to confirm the change (vanilla message, so it works server-side only).
- Before writing it, the owner tests: a moving saw contraption over a Dynamic Trees sapling (old Create #2946; the new sapling may be cut right away), and whether rooty soil turns back to dirt straight after the cut.

Not yet tested in game; each needs the owner's test before any code is written:
- Mechanical Harvester on Dynamic Trees fruit (apples, add-on fruit and nut trees)
- A saw contraption passing over a planted Dynamic Trees sapling (old Create #2946)
- A piston or gantry moving a branch or rooty soil
- Create Aeronautics (Sable) contraptions cutting trees (Dynamic Trees #1173, #1216)
- A stress test in a copy of ATM10 (about 500 mods)

## Skills

Project skills in `.claude/skills/`:
- `deploy-test`: build, then copy the jar into a Prism instance (default `MegaPack2_BUP`), replacing old copies.
- `log-triage`: find mixin, Create and Dynamic Trees failures in an instance's `latest.log` and newest crash report.
- `verify-mixin-targets`: use javap to check every mixin target against the Create and Dynamic Trees jars. Run before every release and after dependency updates.

## Releases and metadata

- Bump the version only in `gradle.properties` (`mod_version`). `src/main/resources/META-INF/neoforge.mods.toml` has `version = "${mod_version}"`, which `processResources` (Gradle `expand`) and `build.sh` (sed) fill in. Because Gradle `expand` is Groovy templating, a literal `$` in that file must be escaped as `\$`.
- Releases update `CHANGELOG.md`. `README.md` (GitHub) and `MODRINTH.md` (the Modrinth page, with absolute image URLs) hold mostly the same content and should be kept in sync.
- User-facing text uses short, plain sentences, and the "vibe coded" credit to Claude stays in the README, MODRINTH page and mods.toml `credits`.
- `docs/` and `src/main/resources/icon.png` are hand-made by the owner (no AI); don't regenerate them.
