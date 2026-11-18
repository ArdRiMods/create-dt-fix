package dev.createdtfix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.dtteam.dynamictrees.block.branch.SurfaceRootBlock;
import dev.createdtfix.Config;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Dynamic Trees surface roots are not in the axe's block list, so an axe (player or deployer)
 * cuts them at bare-hand speed. Axes now cut roots at log speed. Other blocks: no change.
 */
@Mixin(value = BlockBehaviour.class, remap = false)
public abstract class BlockBehaviourMixin {

	@Inject(method = "getDestroyProgress", at = @At("HEAD"), cancellable = true)
	private void createdtfix$axeOnRoots(BlockState state, Player player, BlockGetter level, BlockPos pos,
		CallbackInfoReturnable<Float> cir) {
		if (!((Object) this instanceof SurfaceRootBlock) || !player.getMainHandItem().is(ItemTags.AXES)
			|| !Config.on(Config.AXE_ROOT_SPEED))
			return;
		float hardness = state.getDestroySpeed(level, pos);
		if (hardness < 0)
			return;
		cir.setReturnValue(player.getDestroySpeed(Blocks.OAK_LOG.defaultBlockState()) / hardness / 30f);
	}
}
