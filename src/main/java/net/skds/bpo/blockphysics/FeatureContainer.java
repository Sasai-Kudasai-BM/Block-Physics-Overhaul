package net.skds.bpo.blockphysics;

import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.block.Block;
import net.minecraft.state.properties.BlockStateProperties;
import net.skds.bpo.BPO;
import net.skds.bpo.blockphysics.features.IFeature;
import net.skds.bpo.blockphysics.features.TransformFeature;
import net.skds.bpo.blockphysics.features.IFeature.IFeatureFactory;
import net.skds.bpo.util.BFUtils;
import net.skds.bpo.util.BFUtils.ParsGroup;
import net.skds.core.util.Object2ObjectMap;

public class FeatureContainer extends Object2ObjectMap<Object, IFeature> {

	public static final FeatureContainer EMPTY = new FeatureContainer();

	public void put(IFeature f) {
		super.put(f.getType(), f);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends IFeature> T get(Object arg0) {
		T fe = super.get(arg0);
		return fe == null ? (T) TransformFeature.EMPTY : fe;
	}

	public static ParsGroup<IFeature> readFromJson(JsonElement json, String name) {
		if (json == null || !json.isJsonObject()) {
			BPO.LOGGER.error("Invalid feature properties: \"" + name + "\"");
			return null;
		}
		JsonObject jo = json.getAsJsonObject();
		Set<Block> blocks = BFUtils.getBlocksFromJA(jo.get("blocks").getAsJsonArray());
		IFeature fe = readFeature(jo, blocks);
		if (fe == null) {
			BPO.LOGGER.error("Invalid feature properties: \"" + name + "\"");
			return null;
		}
		return new ParsGroup<IFeature>(fe, blocks);
	}

	public boolean isEmpty() {
		return size() == 0;
	}

	private static IFeature readFeature(JsonObject jo, Set<Block> blocks) {

		String type = jo.get("type").getAsString();
		IFeature feature = null;
		try {
			feature = Type.valueOf(type).create(jo, blocks);
		} catch (Exception e) {
		}
		return feature;
	}

	public static enum Type implements IFeatureFactory {

		LEAVES {
			@Override
			public IFeature create(JsonObject jo, Set<Block> blocks) {
				blocks.removeIf(b -> !b.getDefaultState().hasProperty(BlockStateProperties.DISTANCE_1_7));
				return new IFeature.Simp(this);
			}
		},
		LOGS {
			@Override
			public IFeature create(JsonObject jo, Set<Block> blocks) {
				blocks.removeIf(b -> !b.getDefaultState().hasProperty(BlockStateProperties.AXIS));
				return new IFeature.Simp(this);
			}
		},
		TNT {
			@Override
			public IFeature create(JsonObject jo, Set<Block> blocks) {
				return new IFeature.Simp(this);
			}
		},
		TRANSFORM {
			@Override
			public IFeature create(JsonObject jo, Set<Block> blocks) {
				return TransformFeature.createFromJson(jo, this);
			}
		};

		@Override
		public IFeature create(JsonObject jo, Set<Block> blocks) {
			return null;
		}
	}

}