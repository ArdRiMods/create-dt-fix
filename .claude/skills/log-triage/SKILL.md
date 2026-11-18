---
name: log-triage
description: Read the latest Minecraft log and crash report from a Prism instance and find failures caused by this mod's mixins, Create or Dynamic Trees. Use when the game crashed, failed to launch, or a feature did not work in game. Optional argument is the instance name (default MegaPack2_BUP).
---

# Triage a log or crash

Instance root: `/home/sultanaldhaheri/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/instances/`. Default instance `MegaPack2_BUP`. Use `minecraft/`, or `.minecraft/` if the first does not exist.

1. Read `<instance>/minecraft/logs/latest.log` and the newest file in `<instance>/minecraft/crash-reports/` (by modification time). Give the timestamp of each. If the crash report is older than `latest.log`, it probably belongs to an earlier session: say so.
2. Logs are large. Use grep instead of reading them whole. Search for:
   - `create_dt_fix`, `dev.createdtfix`, `create_dt_fix.mixins.json`
   - `InvalidInjectionException`, `InjectionError`, `MixinApplyError`, `MixinTransformerError`, `Critical injection failure`, `defaultRequire`, `@Overwrite`
   - Stack frames in `com.simibubi.create` or `com.dtteam.dynamictrees`, together with the exception above them
   - NeoForge mod loading errors that mention `create_dt_fix`, `create` or `dynamictrees` (version range, duplicate mod, missing dependency)
3. For each problem, report:
   - the failing mixin class (from `src/main/java/dev/createdtfix/mixin/`)
   - its target class and method or descriptor
   - the probable cause, for example: the target method was renamed or its signature changed in a new Create or Dynamic Trees version, an injection point matched 0 times, another mod's mixin conflicts on the same method, or a runtime exception in our injected code
   - the log lines that show it (short excerpt)
4. Leave out errors and warnings that have no link to this mod, Create or Dynamic Trees. Large packs log many of those. Mention them only if they are the real cause of the crash.
5. If a target looks changed, suggest running the `verify-mixin-targets` skill.

If nothing related is found, say so plainly and name the actual top-level crash cause.
