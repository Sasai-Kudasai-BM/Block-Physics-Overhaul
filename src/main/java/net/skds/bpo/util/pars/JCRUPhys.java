package net.skds.bpo.util.pars;

import static net.skds.bpo.BPO.LOGGER;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.block.Block;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraftforge.registries.ForgeRegistries;
import net.skds.bpo.BPO;
import net.skds.core.api.IBlockExtended;
import net.skds.core.api.IJsonConfigUnit;
import net.skds.core.util.CustomBlockPars;
import net.skds.core.util.configs.UniversalJsonReader;

public class JCRUPhys implements IJsonConfigUnit {

	// JsonConfigReadingUnit
	public static final JCRUPhys INSTANCE = new JCRUPhys();

	private boolean clear = true;

	public static void applyBlockPhysicsPars(ParsGroup<BlockPhysicsPars> BG) {
		for (Block b : BG.blocks) {
			CustomBlockPars pars = ((IBlockExtended) b).getCustomBlockPars();
			pars.put(BG.param);
		}
	}

	@Override
	public String getPath() {
		return "config/" + BPO.MOD_ID;
	}

	@Override
	public String getName() {
		return "blockphysics";
	}

	@Override
	public String getJarPath() {
		return BPO.MOD_ID + "/special";
	}

	@Override
	public String getFormat() {
		return "json";
	}

	@Override
	public boolean apply(JsonObject jo) {

		List<ParsGroup<BlockPhysicsPars>> list = new ArrayList<>();

		for (Map.Entry<String, JsonElement> e : jo.entrySet()) {
			String key = e.getKey();
			ParsGroup<BlockPhysicsPars> group = null;
			try {
				group = BlockPhysicsPars.readFromJson(e.getValue(), key);
			} catch (Exception ex) {
			}
			if (group != null) {
				list.add(group);
			}
		}
		if (list.isEmpty()) {
			return false;
		}

		long t0 = System.currentTimeMillis();
		if (clear) {
			LOGGER.info("Cleaning old blockphysics config data...");
			ForgeRegistries.BLOCKS.getValues().forEach(block -> {
				((IBlockExtended) block).getCustomBlockPars().clear(BlockPhysicsPars.class);
			});
		}
		LOGGER.info("Reading blockphysics configs...");
		for (ParsGroup<BlockPhysicsPars> pg : list) {			
			applyBlockPhysicsPars(pg);
			//System.out.println(pg.param.name);
		}
		LOGGER.info("Configs reloaded in " + (System.currentTimeMillis() - t0) + "ms");

		return true;
	}

	public static void loadFromConfig() {
		INSTANCE.clear = true;
		UniversalJsonReader.read(INSTANCE);
	}

	public static void loadFromData() {
		INSTANCE.clear = false;
		if (UniversalJsonReader.DATA_PACK_RREGISTRIES == null) {
			return;
		}
		IResourceManager resourceManager = UniversalJsonReader.DATA_PACK_RREGISTRIES.getResourceManager();
		resourceManager.getAllResourceLocations("bpodata", s -> {
			return s.endsWith("blockphysics.json");
		}).forEach(rl -> {
			try {
				IResource resource = resourceManager.getResource(rl);
				if (!UniversalJsonReader.readResource(INSTANCE, resource.getInputStream())) {
					LOGGER.error("Error at " + rl);
				} else {
					//LOGGER.info("Good! " + rl);
				}
			} catch (IOException e) {
				LOGGER.error("Error at " + rl);
			}
		});
	}

	public static class ParsGroup<A> {
		public final Set<Block> blocks;
		public final A param;

		ParsGroup(A p, Set<Block> blockList) {
			param = p;
			blocks = blockList;
		}
	}
}