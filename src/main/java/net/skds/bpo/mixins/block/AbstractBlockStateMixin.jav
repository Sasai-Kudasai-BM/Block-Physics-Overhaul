package net.skds.bpo.mixins.block;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.block.AbstractBlock.AbstractBlockState;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.skds.bpo.blockphysics.BFManager;

@Mixin(value = { AbstractBlockState.class })
public abstract class AbstractBlockStateMixin {

	@Inject(method = "onBlockAdded", at = @At(value = "HEAD"), cancellable = false)
	public void onBlockAdded(World worldIn, BlockPos pos, BlockState oldState, boolean isMoving, CallbackInfo ci) {
		BlockState st = (BlockState) (Object) this;
		if (!worldIn.isRemote && !isMoving) {
			BFManager.addOnAddedTask((ServerWorld) worldIn, pos, st, oldState);
		}
	}

	// @Inject(method = "ticksRandomly", at = @At(value = "HEAD"), cancellable =
	// true)
	// public void ticksRandomlyM(CallbackInfoReturnable<Boolean> ci) {
	// }

	//@Inject(method = "randomTick", at = @At(value = "HEAD"), cancellable = false)
	//public void randomTick(ServerWorld w, BlockPos pos, Random randomIn, CallbackInfo ci) {
	//	BlockState st = (BlockState) (Object) this;
	//	if (st.getMaterial() != Material.AIR) {
	//		BFManager.addRandomTask(w, pos, (BlockState) (Object) this);
	//	}
	//}

	//@Inject(method = "tick", at = @At(value = "HEAD"), cancellable = false)
	//public void tickM(ServerWorld w, BlockPos pos, Random randomIn, CallbackInfo ci) {
	//	BlockState st = (BlockState) (Object) this;
	//	if (st.getMaterial() != Material.AIR) {
	//		BFManager.addUpdateTask(w, pos, (BlockState) (Object) this);
	//	}
	//}

	//@Inject(method = "neighborChanged", at = @At(value = "HEAD"), cancellable = false)
	//public void neighborChangedM(World worldIn, BlockPos posIn, Block blockIn, BlockPos fromPosIn, boolean isMoving,
	//		CallbackInfo ci) {
	//	if (!worldIn.isRemote) {
	//		//BFManager.addNeighborTask((ServerWorld) worldIn, posIn, fromPosIn, (BlockState) (Object) this, blockIn);
	//	}
	//}

	//@Inject(method = "updatePostPlacement", at = @At(value = "HEAD"), cancellable = false)
	//public void updatePostPlacementM(Direction face, BlockState queried, IWorld worldIn, BlockPos currentPos,
	//		BlockPos offsetPos, CallbackInfoReturnable<BlockState> ci) {
	//	//BlockState st = (BlockState) (Object) this;
	//	//if (st.getMaterial() != Material.AIR && worldIn instanceof ServerWorld) {
	//		// BFManager.addUpdateTask((ServerWorld) worldIn, currentPos, (BlockState)
	//		// (Object) this);
	//	//}
	//}
}