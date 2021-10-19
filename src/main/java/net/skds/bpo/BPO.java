package net.skds.bpo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.skds.bpo.registry.DataSerialize;
import net.skds.bpo.registry.Entities;
import net.skds.bpo.registry.RenderRegistry;
import net.skds.bpo.util.data.ChunkData;
import net.skds.core.util.SKDSUtils.Side;
import net.skds.core.util.data.ChunkSectionAdditionalData;

@Mod(BPO.MOD_ID)
public class BPO {

	public static final String MOD_ID = "bpo";
	public static final String MOD_NAME = "Block Physics Overhaul";

	private static boolean hasWPO = false;

	public static final Logger LOGGER = LogManager.getFormatterLogger(MOD_NAME);

	public static Events EVENTS = new Events();

	public BPO() {
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(EVENTS::onPacketReg);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(EVENTS::onConfigL);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(EVENTS::onConfigR);

		MinecraftForge.EVENT_BUS.register(EVENTS);
		MinecraftForge.EVENT_BUS.register(this);
	
		BPOConfig.init();
		DataSerialize.register();
		Entities.register();
	}

	public static boolean hasWPO() {
		return hasWPO;
	}
	

	private void setup(final FMLCommonSetupEvent event) {
		hasWPO = ModList.get().getModObjectById("wpo").isPresent();
		ChunkSectionAdditionalData.register(ChunkData::new, Side.SERVER);
	}

	private void doClientStuff(final FMLClientSetupEvent event) {
		RenderRegistry.register();		
	}
}
