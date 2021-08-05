package net.skds.bpo.registry;

import net.minecraft.entity.EntityType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.skds.bpo.BPO;
import net.skds.bpo.entity.AdvancedFallingBlockEntity;

public class Entities {

	static final String AFBID = "advanced_falling_block";
	
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITIES, BPO.MOD_ID);
    
	public static final RegistryObject<EntityType<?>> ADVANCED_FALLING_BLOCK = ENTITIES.register(AFBID, () -> AdvancedFallingBlockEntity.getForReg(AFBID));
	
	public static void register() {
		ENTITIES.register(FMLJavaModLoadingContext.get().getModEventBus());
	}
}