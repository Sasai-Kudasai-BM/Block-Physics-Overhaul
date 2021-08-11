package net.skds.bpo;

import java.io.File;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.skds.bpo.config.Main;
import net.skds.bpo.config.MainCash;

public class BPOConfig {

	public static MainCash MAIN;

	private static final Main COMMON;
	private static final ForgeConfigSpec SPEC;


	public static final int SLIDESTEPS = 5;

	static {
		Pair<Main, ForgeConfigSpec> cm = new ForgeConfigSpec.Builder().configure(Main::new);
		COMMON = cm.getLeft();
		SPEC = cm.getRight();
	}

	public static Main getCommon() {
		return COMMON;
	}

	public static void cash() {
		MAIN = new MainCash(COMMON);
	}

	public static void init() {	   
		File dir = new File("config/" + BPO.MOD_ID);		
		dir.mkdir();
		ModLoadingContext.get().registerConfig(Type.COMMON, SPEC, BPO.MOD_ID + "/main.toml");
	}
}