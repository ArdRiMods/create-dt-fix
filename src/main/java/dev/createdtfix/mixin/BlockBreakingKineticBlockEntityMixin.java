package dev.createdtfix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.compat.dynamictrees.DynamicTree;
import com.simibubi.create.content.kinetics.base.BlockBreakingKineticBlockEntity;
import com.simibubi.create.content.kinetics.saw.TreeCutter;
import dev.createdtfix.ToolDir;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Stationary block breakers (drill, and add-on breakers that use Create's base class).
 * The stationary saw has its own path and never reaches this code for Dynamic Trees.
 */
@Mixin(value = BlockBreakingKineticBlockEntity.class, remap = false)
public abstract class BlockBreakingKineticBlockEntityMixin {

	@Shadow
	protected BlockPos breakingPos;

	/** A Dynamic Trees branch is cut as a whole tree; drops land in front of the machine. */
	@Inject(method = "onBlockBroken", at = @At("HEAD"), cancellable = true)
	private void createdtfix$cutDynamicTree(BlockState stateToBreak, CallbackInfo ci) {
		if (!TreeCutter.canDynamicTreeCutFrom(stateToBreak.getBlock()))
			return;
		Level level = ((BlockEntity) (Object) this).getLevel();
		if (level == null)
			return;
		BlockPos cut = breakingPos;
		BlockPos self = ((BlockEntity) (Object) this).getBlockPos();
		ToolDir.set(Direction.getNearest(self.getX() - cut.getX(), self.getY() - cut.getY(), self.getZ() - cut.getZ()));
		new DynamicTree(cut).destroyBlocks(level, null, (dropPos, stack) -> {
			if (stack.isEmpty() || !level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS))
				return;
			Vec3 v = Vec3.atCenterOf(cut);
			ItemEntity item = new ItemEntity(level, v.x, v.y, v.z, stack);
			item.setDefaultPickUpDelay();
			item.setDeltaMovement(Vec3.ZERO);
			level.addFreshEntity(item);
		});
		ci.cancel();
	}

	/** Max 2 crack stages per step on Dynamic Trees branches, so the animation always shows. */
	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(III)I"))
	private int createdtfix$visibleCracks(int value, int min, int max) {
		int step = Mth.clamp(value, min, max);
		Level level = ((BlockEntity) (Object) this).getLevel();
		if (level != null && breakingPos != null
			&& TreeCutter.canDynamicTreeCutFrom(level.getBlockState(breakingPos).getBlock()))
			return Math.min(step, 2);
		return step;
	}
}
