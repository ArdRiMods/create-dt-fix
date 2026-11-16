package dev.createdtfix.mixin;

import java.util.ArrayList;
import java.util.function.BiConsumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.dtteam.dynamictrees.api.network.BranchDestructionData;
import com.dtteam.dynamictrees.block.branch.BranchBlock;
import com.dtteam.dynamictrees.block.branch.TrunkShellBlock;
import com.dtteam.dynamictrees.entity.FallingTreeEntity;
import com.dtteam.dynamictrees.tree.TreeHelper;
import com.simibubi.create.compat.dynamictrees.DynamicTree;
import dev.createdtfix.ToolDir;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Re-enables Create's Dynamic Trees saw compat. Create 6.0.10 ships this class with the
 * code commented out (isDynamicBranch() always returns false), so the saw breaks a branch
 * as a plain block and Dynamic Trees deletes the tree with no drops.
 * Logic is taken from Create PR #9898 (not merged as of 2026-11-16).
 * Remove this mod when Create ships the fix.
 */
@Mixin(value = DynamicTree.class, remap = false)
public abstract class DynamicTreeMixin {

	@Shadow
	private BlockPos startCutPos;

	/**
	 * @author ArdRiMods
	 * @reason Create 6.0.10 has the Dynamic Trees check commented out.
	 */
	@Overwrite
	public static boolean isDynamicBranch(Block block) {
		return TreeHelper.isBranch(block) || block instanceof TrunkShellBlock;
	}

	/**
	 * @author ArdRiMods
	 * @reason Create 6.0.10 has the Dynamic Trees destroy logic commented out.
	 */
	@Overwrite
	public void destroyBlocks(Level world, ItemStack toDamage, Player playerEntity,
		BiConsumer<BlockPos, ItemStack> drop) {

		BranchBlock start = TreeHelper.getBranch(world.getBlockState(startCutPos));
		if (start == null) // not a branch: try a trunk shell (thick trunk)
			start = createdtfix$branchFromShellMuse(world, world.getBlockState(startCutPos));
		if (start == null)
			return;

		// Break sound and particles
		world.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, startCutPos,
			Block.getId(world.getBlockState(startCutPos)));

		// Break the tree in HARVEST mode (no "sloppy break", so no double drops).
		// The tool side makes the falling tree fall away from the machine.
		BranchDestructionData data = start.destroyBranchFromNode(world, startCutPos, ToolDir.take(world.random), false, playerEntity);

		// Send all drops to Create (contraption storage, or the ground for a stationary machine)
		data.leavesDrops.forEach(stackPos -> drop.accept(stackPos.pos.offset(startCutPos), stackPos.stack));
		data.species.getBranchesDrops(world, data.woodVolume)
			.forEach(stack -> drop.accept(startCutPos, stack));

		// Falling tree animation only: drops were already given to Create above,
		// so the entity gets an empty payload and no leaf drops (no duplicates).
		data.leavesDrops.clear();
		FallingTreeEntity.dropTree(world, data, new ArrayList<>(), FallingTreeEntity.DestroyType.HARVEST);
	}

	@Unique
	private BranchBlock createdtfix$branchFromShellMuse(Level world, BlockState state) {
		if (state.getBlock() instanceof TrunkShellBlock shell) {
			TrunkShellBlock.ShellMuse muse = shell.getMuse(world, state, startCutPos);
			if (muse != null) {
				startCutPos = muse.pos(); // move the cut to the centre of the trunk
				return TreeHelper.getBranch(muse.state());
			}
		}
		return null;
	}
}
