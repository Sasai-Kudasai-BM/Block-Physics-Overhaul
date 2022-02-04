package net.skds.bpo.blockphysics.features;

import java.util.Set;

import com.google.gson.JsonObject;

import net.minecraft.block.Block;
import net.skds.bpo.blockphysics.FeatureContainer;
import net.skds.bpo.blockphysics.FeatureContainer.Type;

public interface IFeature {

	public FeatureContainer.Type getType();

	public static interface IFeatureFactory {
		public IFeature create(JsonObject jo, Set<Block> blocks);
	}

	public static class Simp implements IFeature {
		public final FeatureContainer.Type type;

		public Simp(FeatureContainer.Type type) {
			this.type = type;
		}

		@Override
		public Type getType() {
			return type;
		}

	}
}
