package net.skds.bpo.blockphysics;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.server.ServerWorld;
import net.skds.bpo.BPOConfig;
import net.skds.bpo.blockphysics.BFTask.Type;
import net.skds.bpo.entity.AdvancedFallingBlockEntity;
import net.skds.bpo.util.BFUtils;
import net.skds.bpo.util.data.ChunkData;
import net.skds.core.api.IWorldExtended;
import net.skds.core.util.data.ChunkSectionAdditionalData;

public class BFManager {

	public static void addTask(ServerWorld w, BlockPos pos, BlockState state, BFTask.Type type) {
		Material material = state.getMaterial();
		Block block = state.getBlock();
		if (material == Material.AIR || material.isLiquid() || block == Blocks.BEDROCK
				|| state.getBlock() instanceof FlowingFluidBlock) {
			return;
		}
		WWS wws = ((IWorldExtended) w).getWWS().getTyped(WWS.class);
		if (wws.inBlacklist) {
			return;
		}
		BFTask task = new BFTask(wws, pos, type);
		BlockPhysicsPars param = BFUtils.getParam(block, pos, w);
		// WWS.pushTask(task);
		int time = 0;
		switch (type) {

			case UPDATE:
				time = 1;
				break;

			case NEIGHBOR_UP:
				time = 1;
				break;

			case NEIGHBOR:
				time = param.slide ? 2 : 0;
				break;

			case PLACED:
				time = 0;
				break;

			default:
				break;
		}
		WWS.pushDelaydedTask(task, time);
	}

	public static void addRandomTask(ServerWorld w, BlockPos pos, BlockState state) {
		// addTask(w, pos, state, Type.RANDOM);
	}

	public static void addUpdateTask(ServerWorld w, BlockPos pos, BlockState state) {
		addTask(w, pos, state, Type.UPDATE);
	}

	public static void addOnAddedTask(ServerWorld w, BlockPos pos, BlockState state, BlockState oldState, int flags, Chunk chunk) {

		boolean flag16 = (flags & 16) != 0;

		
		ChunkData data = ChunkSectionAdditionalData.getTyped(chunk, pos.getY() >> 4, ChunkData.class);
		if (data != null) {
			//System.out.println(flag16);
			data.setNatural(pos.getX(), pos.getY(), pos.getZ(), flag16);
		}

		if (flag16) {
			return;
		}

		Block b = state.getBlock();
		Block bo = oldState.getBlock();
		//if (!(b instanceof FlowingFluidBlock)) {
		if (b instanceof FlowingFluidBlock || bo instanceof FlowingFluidBlock) {
			return;
		}

		if (state.getMaterial() != Material.AIR) {
			addTask(w, pos, state, Type.PLACED);
		} else if (BFUtils.getParam(oldState.getBlock(), pos, w).canBeDiagonal()) {

			for (Direction dir : Direction.Plane.HORIZONTAL) {
				BlockPos pos1 = pos.offset(dir);
				BlockPos pos1u = pos1.up();
				BlockState state1u = w.getBlockState(pos1u);
				addTask(w, pos1u, state1u, Type.NEIGHBOR);

				BlockPos pos2 = pos1u.offset(dir.rotateY());
				BlockState state2 = w.getBlockState(pos2);
				addTask(w, pos2, state2, Type.NEIGHBOR);

				BlockPos pos3 = pos1.offset(dir.rotateY());
				BlockState state3 = w.getBlockState(pos3);
				addTask(w, pos3, state3, Type.NEIGHBOR);

			}
		}
		for (Direction dir : Direction.values()) {
			BlockPos pos2 = pos.offset(dir);
			addNeighborTask(w, pos2, pos, w.getBlockState(pos2), b);
		}
	}

	public static void addNeighborTask(ServerWorld w, BlockPos pos, BlockPos fromPos, BlockState state, Block block) {

		if (state.getMaterial() != Material.AIR) {
			addTask(w, pos.up(), w.getBlockState(pos.up()), Type.NEIGHBOR_UP);
			boolean b = pos.down().equals(fromPos);
			addTask(w, pos, state, b ? Type.NEIGHBOR_UP : Type.NEIGHBOR);
		} /* else if (BFUtils.getParam(block, fromPos, w).diagonal) {
			
			System.out.println(state + " " + block);
			addTask(w, pos.up(), state, Type.NEIGHBOR);
			}*/
	}

	public static void stepOnBlock(World worldIn, BlockPos pos, Entity entityIn, BlockState state) {
		if (!worldIn.isRemote && entityIn.getType() == EntityType.PLAYER && BPOConfig.MAIN.triggerOnStep) {
			addTask((ServerWorld) worldIn, pos, state, Type.UPDATE);
		}
	}

	public static boolean onExplosion(Explosion expl, World w) {

		Vector3d ep = expl.getPosition();
		for (BlockPos pos : expl.getAffectedBlockPositions()) {
			Vector3d motion = new Vector3d(pos.getX() - ep.x + 0.5, pos.getY() - ep.y + 0.1, pos.getZ() - ep.z + 0.5);
			Vector3d norm = motion.normalize();
			motion = norm.scale(1D / motion.lengthSquared());
			fall(pos, w, motion);
		}
		return false;
	}

	@SuppressWarnings("deprecation")
	private static void fall(BlockPos pos, World w, Vector3d motion) {
		// if (!occupyPos(pss.down())) {
		// worker.owner.goNT(new BFTask(type, pos));
		// return;
		// }
		// w.setBlockState(pos.down(), Blocks.AIR.getDefaultState());
		BlockState fallstate = w.getBlockState(pos);
		if (fallstate.isAir()) {
			return;
		}
		w.setBlockState(pos, Blocks.AIR.getDefaultState());
		if (w.rand.nextDouble() > 0.0) {
			AdvancedFallingBlockEntity entity = new AdvancedFallingBlockEntity(w, pos.getX() + 0.5, pos.getY(),
					pos.getZ() + 0.5, fallstate);

			if (fallstate != null && fallstate.hasTileEntity()) {
				TileEntity te = w.getTileEntity(pos);
				if (te != null) {
					entity.tileEntityData = te.serializeNBT();
					te.remove();
				} else {
					System.out.println("no entity");
				}
			}

			BlockPhysicsPars fallParam = BFExecutor.getParam(fallstate.getBlock(), w, pos);

			// entity.fallTime = 1;
			entity.setMotion(motion);
			entity.pars = fallParam;
			entity.fallTime = -3;
			// entity.bounce = 0.5F;
			entity.wws.collisionMap.addBox(entity, entity.getBoundingBox());
			// System.out.println(entity.isOnGround());
			w.addEntity(entity);
		} else {
			Block.spawnDrops(fallstate, w, pos);
		}
	}
}