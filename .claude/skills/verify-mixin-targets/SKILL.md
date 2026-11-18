---
name: verify-mixin-targets
description: Check with javap that every @Mixin target class and every injected or overwritten method still exists in the Create and Dynamic Trees jars. Use before every release and after any Create, Dynamic Trees or NeoForge update.
---

# Verify mixin targets

1. Find the jars:
   - Instance mods folder (default instance `MegaPack2_BUP`, under `/home/sultanaldhaheri/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/instances/<instance>/minecraft/mods/`): `create-1.21.1-*.jar` and `dynamictrees-neoforge-*.jar`.
   - Or the Gradle cache: `~/.gradle/caches/modules-2/files-2.1/maven.modrinth/{create,dynamictrees}/`.
   Report the exact jar versions found, and whether they match `create_version` and `dynamic_trees_version` in `gradle.properties`.
2. For each mixin listed in `src/main/resources/create_dt_fix.mixins.json`, read its source in `src/main/java/dev/createdtfix/mixin/` and collect:
   - the `@Mixin` target class
   - every `@Inject`, `@Redirect` and `@Overwrite` method, with its `method =` value and the handler's parameters
   - every `@Shadow` field or method
   - every `@At(target = ...)` invoke target (for example `Mth.clamp(III)I`)
3. Check each one against the jar, using classes in the jar's package path:
   `javap -p -s -cp <jar> <fully.qualified.Class>` for members and descriptors, and `javap -c -p -cp <jar> <Class>` to confirm that an `@At` invoke target is called inside the target method.
   - `BlockBehaviourMixin` targets vanilla `BlockBehaviour`, which is not in these jars. Check it against the NeoForge/Minecraft jar if one is available, or mark it "not checked".
   - Dynamic Trees classes are under `com.dtteam.dynamictrees`. Create classes are under `com.simibubi.create`.
4. Report a table: mixin, target, member, status (OK / missing / signature changed / invoke not found), with the actual signature from the jar when it differs.
5. Anything not OK will crash at launch (`defaultRequire: 1`). Do not change code unless asked. Explain what changed and what the fix would be.
