package dev.createdtfix;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

/** Entry point: registers the config file. All other work is done by the mixins. */
@Mod(CreateDtFix.MOD_ID)
public class CreateDtFix {
	public static final String MOD_ID = "create_dt_fix";

	public CreateDtFix(ModContainer container) {
		container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
	}
}
