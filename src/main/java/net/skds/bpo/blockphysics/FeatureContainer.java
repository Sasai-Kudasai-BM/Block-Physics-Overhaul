package net.skds.bpo.blockphysics;

import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.block.Block;
import net.minecraft.state.properties.BlockStateProperties;
import net.skds.bpo.BPO;
import net.skds.bpo.blockphysics.features.IFeature;
import net.skds.bpo.util.BFUtils;
import net.skds.bpo.util.BFUtils.ParsGroup;
import net.skds.core.util.Object2ObjectMap;

public class FeatureContainer extends Object2ObjectMap<Object, IFeature> {

	public void put(IFeature f) {
		if (f instanceof Simple) {
			super.put(f, f);
		} else {
			super.put(f.getClass(), f);
		}
	}

	public static ParsGroup<IFeature> readFromJson(JsonElement json, String name) {
		if (json == null || !json.isJsonObject()) {
			BPO.LOGGER.error("Invalid feature properties: \"" + name + "\"");
			return null;
		}
		JsonObject jo = json.getAsJsonObject();

		IFeature fe = readFeature(jo);
		if (fe == null) {
			BPO.LOGGER.error("Invalid feature properties: \"" + name + "\"");
			return null;
		}
		Set<Block> blocks = BFUtils.getBlocksFromJA(jo.get("blocks").getAsJsonArray());

		if (fe instanceof Simple) {
			switch ((Simple) fe) {
			case LEAVES:
				blocks.removeIf(b -> !b.getDefaultState().hasProperty(BlockStateProperties.DISTANCE_1_7));
				break;
			case LOGS:
				blocks.removeIf(b -> !b.getDefaultState().hasProperty(BlockStateProperties.AXIS));
				break;
			default:
				break;
			}
		}

		return new ParsGroup<IFeature>(fe, blocks);
	}

	public boolean isEmpty() {
		return size() == 0;
	}

	private static IFeature readFeature(JsonObject jo) {

		String type = jo.get("type").getAsString();
		Simple simple = null;
		try {
			simple = Simple.valueOf(type.toUpperCase());

		} catch (Exception e) {
		}
		return simple;
	}

	public static enum Simple implements IFeature {
		LEAVES, LOGS;
	}

}