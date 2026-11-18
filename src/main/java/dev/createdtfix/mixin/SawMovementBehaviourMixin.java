package dev.createdtfix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.kinetics.saw.SawMovementBehaviour;
import dev.createdtfix.ToolDir;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Records the side of the tree that faces the moving saw (for the fall direction). */
@Mixin(value = SawMovementBehaviour.class, remap = false)
public abstract class SawMovementBehaviourMixin {

	@Inject(method = "onBlockBroken", at = @At("HEAD"))
	private void createdtfix$recordSide(MovementContext context, BlockPos pos, BlockState brokenState, CallbackInfo ci) {
		ToolDir.setFromPositions(pos, context.position, context.motion);
	}
}
