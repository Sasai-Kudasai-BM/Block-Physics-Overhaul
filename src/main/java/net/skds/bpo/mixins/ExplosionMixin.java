package net.skds.bpo.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.Entity;
import net.minecraft.util.DamageSource;
import net.minecraft.world.Explosion;
import net.minecraft.world.ExplosionContext;
import net.minecraft.world.World;
import net.skds.bpo.BPOConfig;
import net.skds.bpo.util.Interface.IExplosionMix;

@Mixin(value = { Explosion.class })
public class ExplosionMixin implements IExplosionMix {

	@Shadow
	private float size;

	@Override
	public float getPower() {
		return size;
	}

	@Inject(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/DamageSource;Lnet/minecraft/world/ExplosionContext;DDDFZLnet/minecraft/world/Explosion$Mode;)V", at = @At("TAIL"))
	void init(World world, Entity exploder, DamageSource source, ExplosionContext context, double x, double y, double z, float size, boolean causesFire, Explosion.Mode mode, CallbackInfo ci) {
		this.size = (float) (size * BPOConfig.MAIN.explosionMultiplier);
	}
}