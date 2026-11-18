---
name: deploy-test
description: Build the mod and copy the jar into a Prism Launcher instance for in-game testing. Use when asked to deploy, install or test a build in the pack. Optional argument is the instance name (default MegaPack2_BUP).
---

# Deploy a test build

1. Run `./gradlew build --no-daemon` from the repo root. If it fails, stop and report the error. Do not deploy.
2. Instance root: `/home/sultanaldhaheri/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/instances/`.
   Instance: the argument if one was given, otherwise `MegaPack2_BUP`. If the instance folder does not exist, list the instances and stop.
3. Mods folder: `<instance>/minecraft/mods`, or `<instance>/.minecraft/mods` if the first does not exist.
4. Delete old copies of this mod in that folder. The jar is named with underscores (`create_dt_fix-<ver>.jar`), so match both spellings:
   `create_dt_fix*.jar` and `create-dt-fix*.jar`. Do not touch any other jar (for example `dtbop`, `create-*`). Two copies of the mod crash NeoForge at launch.
5. Copy `build/libs/create_dt_fix-<mod_version>.jar` into the mods folder (`mod_version` from `gradle.properties`).
6. Print the deployed jar name and the version the jar itself reports:
   `unzip -p <jar> META-INF/neoforge.mods.toml | grep '^version'`. If that version differs from `mod_version`, say so.

The game must be closed while the jar is replaced. Remind the user to restart the instance.
