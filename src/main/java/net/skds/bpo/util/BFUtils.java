package net.skds.bpo.util;

import static net.skds.bpo.BPO.LOGGER;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ITag;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.AbstractChunkProvider;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunk;
import net.minecraftforge.registries.ForgeRegistries;
import net.skds.bpo.BPOConfig;
import net.skds.bpo.blockphysics.BlockPhysicsData;
import net.skds.bpo.blockphysics.FeatureContainer;
import net.skds.bpo.blockphysics.WWS;
import net.skds.bpo.util.data.ChunkData;
import net.skds.core.api.IBlockExtended;
import net.skds.core.api.IServerChunkProvider;
import net.skds.core.util.CustomBlockPars;
import net.skds.core.util.data.ChunkSectionAdditionalData;

public class BFUtils {

	public static boolean occupyPos(WWS wws, BlockPos pos) {
		if (!wws.collisionMap.getBoxesExeptE(null, new AxisAlignedBB(pos).shrink(1E-7D).grow(0, 1, 0)).isEmpty()) {
			//System.out.println("kjklj");
			//WWS.pushDelaydedTask(new BFTask(wws, pos, Type.NEIGHBOR), 20);
			return false;
		}
		return wws.collisionMap.addBox(pos, 2);
		// return false;
	}

	public static VoxelShape VoxelShapeFilter(VoxelShape shape) {
		if (shape.isEmpty()) {
			return shape;
		}
		Vector3d vpos = shape.getBoundingBox().getCenter();
		VoxelShape shape2 = VoxelShapes.fullCube().withOffset(Math.floor(vpos.x), Math.floor(vpos.y),
				Math.floor(vpos.z));
		//System.out.println(shape2.getBoundingBox().getCenter());
		return shape2;
	}

	public static FeatureContainer getFeatures(Block b) {
		CustomBlockPars cbp = ((IBlockExtended) b).getCustomBlockPars();
		FeatureContainer fe = cbp.get(FeatureContainer.class);
		return fe == null ? FeatureContainer.EMPTY : fe;
	}

	public static BlockPhysicsData getParam(Block b, BlockPos pos, World world) {
		if (b.getDefaultState().getMaterial() == Material.AIR) {
			return BlockPhysicsData.DEFAULT_AIR;
		}
		CustomBlockPars cbp = ((IBlockExtended) b).getCustomBlockPars();
		BlockPhysicsData par = cbp.get(BlockPhysicsData.class);
		if (par == null) {
			boolean empty = b.getDefaultState().getCollisionShape(world, pos).isEmpty();
			@SuppressWarnings("deprecation")
			float res = b.getExplosionResistance();
			par = new BlockPhysicsData(b, empty, res);
			cbp.put(par);
		} else if (par.getNatural() != null && !world.isRemote) {
			long lpos = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
			AbstractChunkProvider prov = world.getChunkProvider();
			IChunk iChunk = ((IServerChunkProvider) prov).getCustomChunk(lpos);
			if (iChunk instanceof Chunk) {
				Chunk chunk = (Chunk) iChunk;
				if (chunk != null && !chunk.isEmpty()) {
					ChunkData data = ChunkSectionAdditionalData.getTyped(chunk, pos.getY() >> 4, ChunkData.class);
					if (data != null) {
						if (data.isNatural(pos.getX(), pos.getY(), pos.getZ())) {
							//System.out.println("x");
							return par.getNatural();
						}
					}
				}
			}
		} else if (par.getDimOver() != null) {
			String worldName = world.getDimensionKey().getLocation().toString();
			BlockPhysicsData worldPar = par.getDimOver().get(worldName);
			if (worldPar != null) {
				//System.out.println(worldPar);
				return worldPar;
			}
		}
		return par;
	}

	public static boolean isAir(Block b) {
		return b.getDefaultState().getMaterial() == Material.AIR;
	}

	public static boolean isAir(BlockState bs) {
		return bs.getMaterial() == Material.AIR;
	}

	//public static ConversionPars getConvParam(Block b) {
	//	if (b instanceof AirBlock) {
	//		return ConversionPars.EMPTY;
	//	}
	//	CustomBlockPars cbp = ((IBlockExtended) b).getCustomBlockPars();
	//	ConversionPars par = cbp.get(ConversionPars.class);
	//	if (par == null) {
	//		par = ConversionPars.EMPTY;
	//	}
	//	return par;
	//}

	public static Set<BlockPos> getBlockPoses(AxisAlignedBB aabb) {
		int x0 = (int) Math.floor(aabb.minX + 1E-4);
		int y0 = (int) Math.floor(aabb.minY + 1E-4);
		int z0 = (int) Math.floor(aabb.minZ + 1E-4);

		int x2 = (int) Math.floor(aabb.maxX - 1E-4);
		int y2 = (int) Math.floor(aabb.maxY - 1E-4);
		int z2 = (int) Math.floor(aabb.maxZ - 1E-4);

		Set<BlockPos> set = new HashSet<>();
		for (int x = x0; x <= x2; x++) {
			for (int y = y0; y <= y2; y++) {
				for (int z = z0; z <= z2; z++) {
					set.add(new BlockPos(x, y, z));
				}
			}
		}
		return set;
	}

	public static Set<Block> getBlocksFromString(Set<String> list) {
		Set<Block> blocks = new HashSet<>();
		for (String id : list) {
			if (id.charAt(0) == '#') {
				id = id.substring(1);
				ITag<Block> tag = BlockTags.getCollection().get(new ResourceLocation(id));
				if (tag == null) {
					if (BPOConfig.MAIN.debug) {
						LOGGER.error("Block tag \"" + id + "\" does not exist!");
					}
					continue;
				}
				blocks.addAll(tag.getAllElements());
				continue;
			}
			Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));
			if (block != null && block != Blocks.AIR) {
				blocks.add(block);
			} else {
				if (BPOConfig.MAIN.debug) {
					LOGGER.error("Block \"" + id + "\" does not exist!");
				}
			}
		}
		return blocks;
	}

	public static Set<Block> getBlocksFromJA(JsonArray arr) {
		Set<Block> blocks = new HashSet<>();
		for (JsonElement je : arr) {
			String id = je.getAsString();
			if (id.charAt(0) == '#') {
				id = id.substring(1);
				ITag<Block> tag = BlockTags.getCollection().get(new ResourceLocation(id));
				if (tag == null) {
					LOGGER.error("Block tag \"" + id + "\" does not exist!");
					continue;
				}
				blocks.addAll(tag.getAllElements());
				continue;
			}
			Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));
			if (block != null && block != Blocks.AIR) {
				blocks.add(block);
			} else {
				LOGGER.error("Block \"" + id + "\" does not exist!");
			}
		}
		return blocks;
	}

	public static class ParsGroup<A> {
		public final Set<Block> blocks;
		public final A param;

		public ParsGroup(A p, Set<Block> blockList) {
			param = p;
			blocks = blockList;
		}
	}
}