# Create × Dynamic Trees Fix

Makes **Create** machines work with **Dynamic Trees** again.

In Create 6.0.10 the Dynamic Trees support is switched off in the code. A Mechanical Saw cuts a Dynamic Tree, the tree disappears, and **nothing drops**. This mod fixes that, and more.

## What it does
- **Mechanical Saw** cuts a whole Dynamic Tree. Drops go into contraption storage (moving saw) or on the ground (stationary saw).
- **Mechanical Drill**, the plough, and add-on breakers that use Create's breaker code do the same.
- The tree **falls away from the machine**, like a tree a player cuts. The fall is an animation only: no duplicate drops.
- **Deployer with an axe**: drops go to the deployer, then to contraption storage or out of the deployer. Fortune and Silk Touch work.
- **Surface roots**: saws cut them, so a root no longer stops a contraption. Axes (player or deployer) cut roots at log speed.
- **No more vanished trees**: Create's vanilla tree cutter no longer deletes a Dynamic Tree with no drops (for example after a saw cuts a root or a vanilla log next to the tree).
- **Break cracks** always show on branches.

## Requirements
| | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.219 or newer |
| Create | **6.0.10** (exact) |
| Dynamic Trees | 1.7.0 or newer |

**Server-side only.** Install it on the server (or in single player). Players without the mod can still join.

## Why "6.0.10 exact"?
Create has a pending fix for the saw (Create issue #9657, PR #9898). When a new Create version comes out, this mod stops the game at launch with a clear message. Then I check the new Create and update this mod (or retire it, if Create fixed everything).

## Notes
- Tool durability follows Dynamic Trees' own `axeDamageMode` setting (`dynamictrees-server.toml`). Set it to `VANILLA` for 1 damage per tree.
- The Create roller is not covered (it uses its own break code).

## How this mod was made
This mod is **vibe coded**. I am the pack maker who found the bug, tested every build in my own pack and decided what the mod should do. The code itself was written by an AI (Claude, by Anthropic) in a chat session, from reading the Create and Dynamic Trees source code. I am not a Java programmer, so I cannot take credit for the code.

What that means for you:
- Every feature was tested in game in a large NeoForge 1.21.1 pack (saw, drill, deployer, roots, thick trees, mushroom trees).
- Every mixin target was checked against the real Create 6.0.10 and Dynamic Trees 1.7.2 jars.
- Bug reports are welcome on [GitHub](https://github.com/ArdRiMods/create-dt-fix/issues). I fix them the same way: I report the bug in game, and the AI and I find the cause in the source.

## Credits
- Saw compat logic based on Create PR #9898 (MIT, The Create Team).
- Dynamic Trees by DynamicTreesTeam (MIT).
- Code written by Claude (Anthropic AI). Idea, testing and design decisions by ArdRiMods.
- Not affiliated with the Create team or the Dynamic Trees team.

## Building
Two ways:

1. **Gradle** (needs internet): `./gradlew build`. The jar is in `build/libs/`.
2. **Offline, with javac**: put these jars in one folder, then run `./build.sh <folder>`:
   - `create-1.21.1-6.0.10.jar` and `dynamictrees-neoforge-1.21.1-1.7.x.jar` (from `mods/`)
   - From the launcher `libraries/` folder: `neoforge-21.1.x-client.jar`, `neoforge-21.1.x-universal.jar`, `client-1.21.1-<date>-srg.jar`, `loader-4.0.x.jar`, `sponge-mixin-*.jar`
