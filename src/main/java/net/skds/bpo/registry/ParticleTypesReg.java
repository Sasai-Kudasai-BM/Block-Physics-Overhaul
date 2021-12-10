package net.skds.bpo.registry;

import net.minecraft.particles.BasicParticleType;
import net.minecraft.particles.ParticleType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.skds.bpo.BPO;

public class ParticleTypesReg {
	
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, BPO.MOD_ID);

	public static final RegistryObject<BasicParticleType> EXPLODE = PARTICLES.register("explode", () -> new BasicParticleType(false));

	
	public static void register() {
		PARTICLES.register(FMLJavaModLoadingContext.get().getModEventBus());
	}

}
