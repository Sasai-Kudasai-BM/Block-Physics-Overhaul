package net.skds.bpo.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.skds.bpo.BPO;
import net.skds.bpo.client.models.AdvancedFallingBlockRenderer;
import net.skds.bpo.client.particles.ExplodeParticle;
import net.skds.bpo.entity.AdvancedFallingBlockEntity;
import net.skds.bpo.registry.Entities;
import net.skds.bpo.registry.ParticleTypesReg;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = BPO.MOD_ID, bus = Bus.MOD)
public class ClientRegistry {

	@SubscribeEvent
	@SuppressWarnings("unchecked")
	public static void setup(FMLClientSetupEvent e) {
        RenderingRegistry.registerEntityRenderingHandler((EntityType<AdvancedFallingBlockEntity>) Entities.ADVANCED_FALLING_BLOCK.get(), AdvancedFallingBlockRenderer::new);
	}

	@SubscribeEvent
	public static void refParticleFactory(ParticleFactoryRegisterEvent e) {

		Minecraft mc = Minecraft.getInstance();
		
		mc.particles.registerFactory(ParticleTypesReg.EXPLODE.get(), ExplodeParticle.ExplodeParticleFactory::new);
	}	
}