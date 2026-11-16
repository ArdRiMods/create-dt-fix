package dev.createdtfix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.compat.dynamictrees.DynamicTree;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.kinetics.base.BlockBreakingMovementBehaviour;
import com.simibubi.create.content.kinetics.saw.TreeCutter;
import dev.createdtfix.ToolDir;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/**
 * Moving block breakers on contraptions (drill, plough, and add-on breakers that use
 * Create's base class). The saw has its own path (DynamicTreeMixin).
 */
@Mixin(value = BlockBreakingMovementBehaviour.class, remap = false)
public abstract class BlockBreakingMovementBehaviourMixin {

	/** A Dynamic Trees branch is cut as a whole tree, with drops sent to contraption storage. */
	@Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
	private void createdtfix$cutDynamicTree(MovementContext context, BlockPos pos, CallbackInfo ci) {
		Level world = context.world;
		if (!TreeCutter.canDynamicTreeCutFrom(world.getBlockState(pos).getBlock()))
			return;
		ToolDir.setFromPositions(pos, context.position, context.motion);
		MovementBehaviour self = (MovementBehaviour) (Object) this;
		new DynamicTree(pos).destroyBlocks(world, null, (dropPos, stack) -> self.collectOrDropItem(context, stack));
		ci.cancel();
	}

	/** Max 2 crack stages per step on Dynamic Trees branches, so the animation always shows. */
	@Redirect(method = "tickBreaker", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(III)I"))
	private int createdtfix$visibleCracks(int value, int min, int max, MovementContext context) {
		int step = Mth.clamp(value, min, max);
		BlockPos pos = NbtUtils.readBlockPos(context.data, "BreakingPos").orElse(null);
		if (pos != null && TreeCutter.canDynamicTreeCutFrom(context.world.getBlockState(pos).getBlock()))
			return Math.min(step, 2);
		return step;
	}
}
