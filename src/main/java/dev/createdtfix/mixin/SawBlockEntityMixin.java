package dev.createdtfix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.dtteam.dynamictrees.block.branch.SurfaceRootBlock;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import dev.createdtfix.ToolDir;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Stationary saw: fall direction, and lets the saw cut Dynamic Trees surface roots (any tree family, incl. add-ons).
 * Without this, a root in front of a moving saw counts as a solid obstacle and the
 * contraption stops. Only the single root block is cut (no chain into other roots).
 */
@Mixin(value = SawBlockEntity.class, remap = false)
public abstract class SawBlockEntityMixin {

	/** Records the side of the tree that faces the stationary saw (for the fall direction). */
	@Inject(method = "onBlockBroken", at = @At("HEAD"))
	private void createdtfix$recordSide(BlockState stateToBreak, CallbackInfo ci) {
		BlockState self = ((BlockEntity) (Object) this).getBlockState();
		if (self.hasProperty(BlockStateProperties.FACING))
			ToolDir.set(self.getValue(BlockStateProperties.FACING).getOpposite());
	}

	@Inject(method = "isSawable", at = @At("HEAD"), cancellable = true)
	private static void createdtfix$sawSurfaceRoots(BlockState state, CallbackInfoReturnable<Boolean> cir) {
		if (state.getBlock() instanceof SurfaceRootBlock)
			cir.setReturnValue(true);
	}
}
