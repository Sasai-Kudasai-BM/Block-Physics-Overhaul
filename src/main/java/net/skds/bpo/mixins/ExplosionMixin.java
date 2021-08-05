package net.skds.bpo.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.Explosion;

@Mixin(value = { Explosion.class })
public abstract interface ExplosionMixin {
	
	@Accessor("size")
	public float getPower();
}