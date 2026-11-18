package dev.createdtfix.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.kinetics.saw.SawMovementBehaviour;
import com.simibubi.create.content.kinetics.saw.TreeCutter;
import com.simibubi.create.foundation.utility.AbstractBlockBreakQueue;
import dev.createdtfix.Config;
import dev.createdtfix.ToolDir;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Moving saw: records the side of the tree that faces the saw (fall direction), and the sawFix switch. */
@Mixin(value = SawMovementBehaviour.class, remap = false)
public abstract class SawMovementBehaviourMixin {

	@Inject(method = "onBlockBroken", at = @At("HEAD"))
	private void createdtfix$recordSide(MovementContext context, BlockPos pos, BlockState brokenState, CallbackInfo ci) {
		ToolDir.setFromPositions(pos, context.position, context.motion);
	}

	/** sawFix off: the saw skips the Dynamic Trees path, as in plain Create. */
	@Redirect(method = "onBlockBroken", at = @At(value = "INVOKE",
		target = "Lcom/simibubi/create/content/kinetics/saw/TreeCutter;findDynamicTree(Lnet/minecraft/world/level/block/Block;Lnet/minecraft/core/BlockPos;)Ljava/util/Optional;"))
	private Optional<AbstractBlockBreakQueue> createdtfix$sawFixSwitch(Block block, BlockPos pos) {
		return Config.sawFixOff() ? Optional.empty() : TreeCutter.findDynamicTree(block, pos);
	}
}
