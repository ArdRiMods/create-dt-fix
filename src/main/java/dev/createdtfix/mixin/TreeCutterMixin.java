package dev.createdtfix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.dtteam.dynamictrees.block.branch.TrunkShellBlock;
import com.dtteam.dynamictrees.tree.TreeHelper;
import com.simibubi.create.content.kinetics.saw.TreeCutter;

import net.minecraft.world.level.block.state.BlockState;

/**
 * Dynamic Trees puts its branches in the minecraft:logs tag. Create's normal (vanilla) tree
 * finder then treats a Dynamic Tree as plain logs and deletes it with no drops, for example
 * after a saw cuts a surface root or a vanilla log next to the tree.
 * Dynamic Trees are cut only by the Dynamic Trees path (DynamicTreeMixin).
 */
@Mixin(value = TreeCutter.class, remap = false)
public abstract class TreeCutterMixin {

	@Inject(method = "isLog", at = @At("HEAD"), cancellable = true)
	private static void createdtfix$notAVanillaLog(BlockState state, CallbackInfoReturnable<Boolean> cir) {
		if (TreeHelper.isBranch(state.getBlock()) || state.getBlock() instanceof TrunkShellBlock)
			cir.setReturnValue(false);
	}
}
