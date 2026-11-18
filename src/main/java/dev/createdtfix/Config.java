package dev.createdtfix;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * config/create_dt_fix-common.toml. One switch per fix, all on by default.
 * Read at the moment a fix runs, so a change applies without a restart
 * (NeoForge reloads the file when it is saved).
 */
public final class Config {
	private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

	public static final ModConfigSpec.BooleanValue SAW_FIX = B
		.comment("Mechanical Saw cuts a whole Dynamic Tree, with drops (into contraption storage on contraptions).",
			"Off: Create's own saw code (in Create 6.0.10 that means trees break with no drops).",
			"If Create has its own fix, this mod's saw fix is off and this switch does nothing.")
		.define("sawFix", true);

	public static final ModConfigSpec.BooleanValue BREAKER_FIX = B
		.comment("Drill, plough and add-on breakers that use Create's breaker code cut a whole Dynamic Tree, with drops.")
		.define("breakerFix", true);

	public static final ModConfigSpec.BooleanValue FALL_AWAY = B
		.comment("A tree cut by a machine falls away from the machine. Off: it falls in a random direction.")
		.define("fallAwayFromMachine", true);

	public static final ModConfigSpec.BooleanValue DEPLOYER_DROPS = B
		.comment("A deployer with an axe gets the drops of the tree it cuts.")
		.define("deployerDrops", true);

	public static final ModConfigSpec.BooleanValue SAW_CUTS_ROOTS = B
		.comment("Saws cut Dynamic Trees surface roots (a moving saw no longer stops at a root).")
		.define("sawCutsRoots", true);

	public static final ModConfigSpec.BooleanValue AXE_ROOT_SPEED = B
		.comment("Axes cut Dynamic Trees surface roots at log speed.")
		.define("axeRootSpeed", true);

	public static final ModConfigSpec.BooleanValue BREAK_CRACKS = B
		.comment("Break cracks always show on branches while a machine cuts them.")
		.define("breakCracks", true);

	public static final ModConfigSpec SPEC = B.build();

	/** True when MixinPlugin turned this mod's saw fix on (Create's compat is still empty). */
	private static final boolean OUR_SAW_FIX = Boolean.getBoolean(MixinPlugin.SAW_FIX_PROPERTY);

	private Config() {}

	/** The switch's value, or "on" if the config file is not loaded yet. */
	public static boolean on(ModConfigSpec.BooleanValue value) {
		return !SPEC.isLoaded() || value.get();
	}

	/** True when the saw should go back to Create's own code. */
	public static boolean sawFixOff() {
		return OUR_SAW_FIX && !on(SAW_FIX);
	}
}
