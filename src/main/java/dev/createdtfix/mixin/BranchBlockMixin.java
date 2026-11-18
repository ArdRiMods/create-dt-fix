package dev.createdtfix.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.dtteam.dynamictrees.api.network.BranchDestructionData;
import com.dtteam.dynamictrees.block.branch.BranchBlock;
import com.dtteam.dynamictrees.entity.FallingTreeEntity;
import com.dtteam.dynamictrees.systems.nodemapper.NetVolumeNode;
import com.dtteam.dynamictrees.utility.ItemUtils;
import com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer;
import dev.createdtfix.Config;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Deployer with an axe: Dynamic Trees normally drops the items from the falling tree when it
 * lands, so the deployer never gets them. For a deployer, the drops go into the deployer's
 * inventory (Create then sends them to contraption storage or out of the deployer), and the
 * tree still falls (animation only).
 */
@Mixin(value = BranchBlock.class, remap = false)
public abstract class BranchBlockMixin {

	@Inject(method = "futureBreak", at = @At("HEAD"), cancellable = true)
	private void createdtfix$deployerDrops(BlockState state, Level level, BlockPos cutPos, LivingEntity entity,
		CallbackInfo ci) {
		if (!(entity instanceof DeployerFakePlayer player) || !Config.on(Config.DEPLOYER_DROPS))
			return;
		BranchBlock self = (BranchBlock) (Object) this;

		// Fall away from the deployer
		double dx = player.getX() - (cutPos.getX() + 0.5), dz = player.getZ() - (cutPos.getZ() + 0.5);
		Direction toolDir = (dx * dx + dz * dz) < 1.0E-4 || !Config.on(Config.FALL_AWAY)
			? Direction.Plane.HORIZONTAL.getRandomDirection(level.random)
			: Direction.getNearest(dx, 0, dz);

		level.levelEvent(2001, cutPos, Block.getId(state));
		BranchDestructionData data = self.destroyBranchFromNode(level, cutPos, toolDir, false, entity);

		// Same wood amount as a player with this tool (Fortune included)
		ItemStack held = player.getMainHandItem();
		int fortune = ItemUtils.getEnchantmentLevel(Enchantments.FORTUNE, held, level.registryAccess());
		NetVolumeNode.Volume volume = data.woodVolume;
		volume.multiplyVolume(1.0f + 0.25f * fortune);
		List<ItemStack> wood = data.species.getBranchesDrops(level, volume, held);

		wood.forEach(stack -> player.getInventory().placeItemBackInInventory(stack));
		data.leavesDrops.forEach(sp -> player.getInventory().placeItemBackInInventory(sp.stack));

		data.leavesDrops.clear();
		FallingTreeEntity.dropTree(level, data, new ArrayList<>(), FallingTreeEntity.DestroyType.HARVEST);
		self.damageAxe(player, held, self.getRadius(state), volume, true);
		ci.cancel();
	}
}
