package net.skds.bpo.mixins.block;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.skds.bpo.blockphysics.BFManager;

@Mixin(value = { Block.class })
public abstract class BlockMixin {

	@Inject(method = "onEntityWalk", at = @At(value = "HEAD"), cancellable = false)
	public void onEntityWalk(World worldIn, BlockPos pos, Entity entityIn, CallbackInfo ci) {
		BFManager.stepOnBlock(worldIn, pos, entityIn, ((Block) (Object) this).getDefaultState());
	}
}