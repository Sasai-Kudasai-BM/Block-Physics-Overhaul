package net.skds.bpo.mixins;

import java.util.function.BooleanSupplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.Entity;
import net.minecraft.world.server.ServerWorld;
import net.skds.bpo.blockphysics.WWS;

@Mixin(value = { ServerWorld.class })
public abstract class ServerWorldMixin {

	
	@Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/IProfiler;endSection()V", ordinal = 3))
	public void afterEntityTick(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
		WWS.afterEntityTick();
	}

	@Inject(method = "updateEntity", at = @At(value = "HEAD"))
	public void updateEntity(Entity entity, CallbackInfo ci) {
		WWS.tickEntity(entity);
	}
}