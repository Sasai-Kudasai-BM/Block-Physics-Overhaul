package net.skds.bpo.mixins.block;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.block.BlockState;
import net.minecraft.block.FallingBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

@Mixin(value = { FallingBlock.class })
public abstract class FallingBlockMixin {

	@Inject(method = "onBlockAdded", at = @At(value = "HEAD"), cancellable = true)
	public void onBlockAdded(BlockState state, World worldIn, BlockPos pos, BlockState oldState, boolean isMoving, CallbackInfo ci) {
		ci.cancel();
	}

	@Inject(method = "tick", at = @At(value = "HEAD"), cancellable = true)
	public void tickM(BlockState state, ServerWorld w, BlockPos pos, Random randomIn, CallbackInfo ci) {
		ci.cancel();
	}
	
}