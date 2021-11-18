package net.skds.bpo.blockphysics;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.block.material.Material;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tags.BlockTags;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ServerWorld;
import net.skds.bpo.BPOConfig;
import net.skds.bpo.blockphysics.BFTask.Type;
import net.skds.bpo.blockphysics.features.IFeature;
import net.skds.bpo.entity.AdvancedFallingBlockEntity;
import net.skds.bpo.util.BFUtils;
import net.skds.core.util.blockupdate.BasicExecutor;

public class BFExecutor extends BasicExecutor {

	// private final Long2ObjectOpenHashMap<BFSnapshot> snaps = new
	// Long2ObjectOpenHashMap<>();

	private final float G = 9.81E-6F;

	private final BlockState state;
	private final BlockPos pos;
	private final Block block;
	private final BFTask task;
	// private final BFTask.Type type;
	private final BlockPhysicsPars param;
	private final FeatureContainer feature;
	private final WWS castOwner;

	private boolean ocuFail = false;

	public BFExecutor(BFTask task) {
		super(task.owner.world, task.owner);
		this.task = task;
		this.pos = BlockPos.fromLong(task.pos);
		this.state = getBlockState(pos);
		this.block = this.state.getBlock();
		this.param = getParam(block, w, pos);
		this.feature = BFUtils.getFeatures(block);
		// this.type = task.type;
		this.castOwner = (WWS) owner;
	}

	@Override
	protected void applyAction(BlockPos pos, BlockState newState, BlockState oldState, World world) {
		if (newState == oldState) {
			return;
		}
		Chunk chunk = getChunk(pos);
		if (chunk == null) {
			return;
		}
		Block block = newState.getBlock();

		synchronized (world) {

			boolean b = false;

			if (chunk.getLocationType() != null
					&& chunk.getLocationType().isAtLeast(ChunkHolder.LocationType.ENTITY_TICKING)) {
				b = true;
			}
			if (b) {
				world.notifyBlockUpdate(pos, oldState, newState, 3);

				// newState.updateDiagonalNeighbors(world, pos, 512);
				world.notifyNeighborsOfStateChange(pos, block);
				if (newState.hasComparatorInputOverride()) {
					world.updateComparatorOutputLevel(pos, block);
				}

				// world.onBlockStateChange(pos, oldState, newState);

				newState.updateNeighbours(world, pos, 0);

				newState.onBlockAdded(world, pos, oldState, false);

				BFManager.addOnAddedTask((ServerWorld) w, pos, newState, oldState, 3, chunk);
			}
		}

		if ((newState.getOpacity(world, pos) != oldState.getOpacity(world, pos)
				|| newState.getLightValue(world, pos) != oldState.getLightValue(world, pos) || newState.isTransparent()
				|| oldState.isTransparent())) {
			world.getChunkProvider().getLightManager().checkBlock(pos);
		}
	}

	public static Direction[] getRandomizedDirections(Random r, boolean addVertical) {

		Direction[] dirs = new Direction[4];

		if (addVertical) {
			dirs = new Direction[6];
			dirs[4] = Direction.DOWN;
			dirs[5] = Direction.UP;
		}
		int i0 = r.nextInt(4);
		for (int index = 0; index < 4; ++index) {
			Direction dir = Direction.byHorizontalIndex((index + i0) % 4);
			dirs[index] = dir;
		}

		return dirs;
	}

	public static Direction[] getAllRandomizedDirections(Random r) {

		Direction[] dirs = new Direction[6];

		int i0 = r.nextInt(6);
		for (int index = 0; index < 6; ++index) {
			Direction dir = Direction.byIndex((index + i0) % 6);
			dirs[index] = dir;
		}

		return dirs;
	}

	@Override
	public void run() {
		int h = pos.getY() >> 4;
		if (h > 16 || h < 0) {
			return;
		}
		Chunk chunk = getChunk(pos);
		if (chunk == null) {
			return;
		}
		if (chunk.getLocationType() == null
				|| !chunk.getLocationType().isAtLeast(ChunkHolder.LocationType.ENTITY_TICKING)) {
			return;
		}
		//ChunkSection section = chunk.getSections()[h];
		//if (section == null) {
		//	return;
		//}
		//ChunkData chunkData = ChunkSectionAdditionalData.getTypedFromSection(section, ChunkData.class);
		//if (chunkData.isNatural(pos.getX(), pos.getY(), pos.getZ())) {
		//	return;
		//}

		runS();
	}

	private boolean isValidForReplace(BlockState state) {
		if (state.isIn(Blocks.MOVING_PISTON)) {
			return false;
		}
		Material mat = state.getMaterial();
		boolean b = mat.isReplaceable();
		BlockPhysicsPars bp = getParam(state);
		b = b || (bp.fragile && bp.strength < 0.001);

		return b;
	}

	private boolean slide(BlockPos pos0) {
		// *
		for (Direction d : BFExecutor.getRandomizedDirections(w.rand, false)) {
			BlockPos pos2 = pos0.offset(d);
			BlockState state2 = getBlockState(pos2);
			if (isValidForReplace(state2)) {
				BlockPos pos3 = pos2.down();
				BlockState state3 = getBlockState(pos3);
				if (isValidForReplace(state3) && occupyPos(pos3)) {

					BlockState oldstate = editor.setState(pos0, Blocks.AIR.getDefaultState());
					BlockState fallstate = fallConvert(oldstate);
					if (fallstate == null || fallstate.getMaterial() == Material.AIR) {
						return false;
					}
					AdvancedFallingBlockEntity entity = new AdvancedFallingBlockEntity(w, pos0.getX() + 0.5,
							pos0.getY(), pos0.getZ() + 0.5, fallstate);

					if (fallstate.hasTileEntity()) {
						TileEntity te = getTileEntity(pos0);
						if (te != null) {
							entity.tileEntityData = te.serializeNBT();
							te.remove();
						} else {
							// System.out.println("no entity");
						}
					}

					BlockPhysicsPars fallParam = getParam(fallstate);

					// entity.fallTime = 1;
					entity.slideDirection = (byte) d.getHorizontalIndex();
					entity.pars = fallParam;
					entity.setOnGround(false);
					addEntity(entity);

					return true;
				}
			}
		}
		// */
		if (ocuFail) {
			WWS.pushDelaydedTask(new BFTask(castOwner, pos, task.type), 2);
		}
		return false;
	}

	public boolean runS() {
		// if (block == Blocks.END_STONE)
		// System.out.println(param);
		if (isAir(state) || !param.falling || block == Blocks.BEDROCK || (block instanceof FlowingFluidBlock)) {
			return false;
		}
		boolean b = tryFall();
		return b;
	}

	private boolean tryFall() {
		BlockPos posd = pos.down();
		BlockState stated = getBlockState(posd);

		if (feature != null && !feature.isEmpty() && feature.contains(FeatureContainer.Simple.LEAVES)) {

			if (canFall(stated)) {
				if (!checkLeaves()) {
					fall();
					return true;
				}
			}
			return false;
		}

		if (task.type == Type.DOWNRAY) {
			// System.out.println("x");
			if (!checkDownray(pos, state, param)) {
				// fall();
				return true;
			}
		} else {
			if (canFall(stated)) {

				if (!checkSnap(pos, state, param)) {
					fall();
					return true;
				}
			} else {
				if (param.slide) {
					if (slide(pos)) {
						return false;
					}
				}
				addDownrayTask(posd);
			}
		}
		return false;
	}

	private boolean checkLeaves() {
		return state.get(BlockStateProperties.DISTANCE_1_7) < 7;
	}

	private BlockState fallConvert(BlockState state) {
		if (state == null) {
			return null;
		}
		ConversionPars cp = BFUtils.getConvParam(state.getBlock());
		if (cp.onFall()) {
			return cp.fallState;
		}
		return state;
	}

	private void fall(BlockPos pss) {
		boolean fire = false;
		BlockState oldstate = editor.setMaskedBlockState(pss, Blocks.AIR.getDefaultState());
		//BlockState oldstate = editor.setState(pss, Blocks.AIR.getDefaultState());
		BlockState fallstate = fallConvert(oldstate);
		// fallstate = fallstate.updatePostPlacement(Direction.UP,
		// Blocks.AIR.getDefaultState(), w, pss, pss);
		if (fallstate != null && fallstate.getMaterial() != Material.AIR) {

			for (Direction dir : Direction.values()) {
				if (fallstate.isFlammable(w, pss, dir)) {
					BlockPos pos2 = pss.offset(dir);
					if (getBlockState(pos2).isIn(BlockTags.FIRE)) {
						fire = true;
						break;
					}
				}
			}
			applyAction(pss, Blocks.AIR.getDefaultState(), oldstate, w);
		} else {
			return;
		}

		// BlockState fallstate = setState(pss, Blocks.AIR.getDefaultState());

		AdvancedFallingBlockEntity entity = new AdvancedFallingBlockEntity(w, pss.getX() + 0.5, pss.getY(),
				pss.getZ() + 0.5, fallstate);

		if (fallstate.hasTileEntity()) {
			TileEntity te = getTileEntity(pss);
			if (te != null) {
				entity.tileEntityData = te.serializeNBT();
				te.remove();
			} else {
				// System.out.println("no entity");
			}
		}

		BlockPhysicsPars fallParam = getParam(fallstate);

		if (fire) {
			entity.setFire(100);
		}
		// entity.fallTime = 1;
		entity.pars = fallParam;
		addEntity(entity);
	}

	private void fall() {
		fall(pos);
	}

	@SuppressWarnings("unused")
	private boolean tryHang(BlockPos pos0, BlockState state0, BlockPhysicsPars param0, float force) {
		return false;
	}

	private boolean checkSnap(BlockPos pos0, BlockState state0, BlockPhysicsPars param0) {

		boolean usp = false;
		if (task.type != Type.DOWNRAY) {
			BlockPos posu = pos.up();
			BlockState stateu = getBlockState(posu);
			usp = canSupport(state0, stateu, pos0, posu, param0, getParam(stateu), param0.mass * G);
		} else {
			usp = true;
		}
		if (param0.hanging && usp && checkUpray(pos0, state0, param0)) {
			return true;
		}

		boolean bl = false;
		if (param0.ceiling) {
			bl = bl || checkCeiling(pos0, state0, param0);
		}

		bl = bl || checkPane(pos0, state0, param0, usp);

		if (!bl && param0.arc > param0.linear) {
			bl = bl || checkArc(pos0, state0, param0);
		}

		if (!bl && param0.diagonal) {
			return checkDiagonal(pos0, state0, param0);
		}

		return bl;
	}

	private boolean checkDiagonal(BlockPos pos0, BlockState state0, BlockPhysicsPars param0) {
		for (Direction dir : Direction.Plane.HORIZONTAL) {
			BlockPos pos1 = pos0.offset(dir);
			BlockPos pos1d = pos1.down();
			BlockState state1d = getBlockState(pos1d);
			BlockPhysicsPars par1d = getParam(state1d);
			if (canSupport(state0, state1d, pos0, pos1, param0, par1d, param0.mass * G)) {
				addDownrayTask(pos1d);
				return true;
			}
			BlockPos pos2 = pos1.offset(dir.rotateY());
			// BlockState state2 = getBlockState(pos2);
			// BlockPhysicsPars par2 = getParam(state2);
			// if (canSupport(state0, state2, pos0, pos1, param0, par2, param0.mass * G)) {
			// addDownrayTask(pos2);
			// return true;
			// }
			BlockPos pos2d = pos2.down();
			BlockState state2d = getBlockState(pos2d);
			BlockPhysicsPars par2d = getParam(state2d);
			if (canSupport(state0, state2d, pos0, pos1, param0, par2d, param0.mass * G)) {
				addDownrayTask(pos2);
				return true;
			}
		}

		//System.out.println("x");

		return false;
	}

	private boolean checkUpray(BlockPos pos0, BlockState state0, BlockPhysicsPars param0) {
		return checkRay(pos0, state0, param0, true);
	}

	private boolean checkDownray(BlockPos pos0, BlockState state0, BlockPhysicsPars param0) {
		return checkRay(pos0, state0, param0, false);
	}

	private boolean checkRay(BlockPos pos0, BlockState state0, BlockPhysicsPars param0, boolean up) {
		BlockPos pos2 = pos0;
		BlockState state2 = state0;
		BlockState state1;
		BlockPos pos1;
		BlockPhysicsPars par1;
		BlockPhysicsPars par2 = param0;
		float force = param0.mass * G;
		boolean sucess = false;
		boolean ds = false;
		int lim = BPOConfig.MAIN.downCheckLimit;
		while (pos2.getY() < 255 && pos2.getY() > 0 && !sucess) {
			if (!up) {
				if (lim != -1 && lim <= task.counter) {
					return true;
				}
				task.counter++;
			}
			par1 = par2;
			pos1 = pos2;
			state1 = state2;
			pos2 = up ? pos2.up() : pos2.down();
			state2 = getBlockState(pos2);
			par2 = getParam(state2);
			force += (par2.mass * G);
			// System.out.println(state2 + " " + par2 + pos2);

			if ((!par2.fragile && !par2.falling) || (!par1.fragile && !par1.falling)) {
				return true;
			}

			ds = canSupport(state1, state2, pos1, pos2, par1, par2, force, false);

			// if (!ds && !checkPane(pos1, state1, par1, true)) {
			if (!ds && !checkSnap(pos1, state1, par1)) {
				if (!up) {
					fall(pos1);
					// System.out.println(state1 + " " + par1 + pos1);
					// System.out.println(getParam(Blocks.BEDROCK));
				}
				return false;
			} else if (!ds) {
				return true;
			}
		}
		return sucess;
	}

	private boolean checkArc(BlockPos pos0, BlockState state0, BlockPhysicsPars par0) {

		int[] holdDist = new int[4];

		for (Direction dir : Direction.Plane.HORIZONTAL) {
			int d = 0;
			float force = par0.mass * G;
			boolean success = false;
			BlockPos pos1 = pos0;
			BlockState state1 = state0;
			BlockPhysicsPars par1 = par0;
			w1: while (d++ <= par0.arc && !success) {
				BlockPos pos2 = pos1.offset(dir);
				BlockState state2 = getBlockState(pos2);
				BlockPhysicsPars par2 = getParam(state2);
				force += par2.mass * G;
				if (canSupport(state1, state2, pos1, pos2, par1, par2, force, true)) {
					if (supportDown(pos2, state2, par2, force)) {
						holdDist[dir.getOpposite().getHorizontalIndex()] = d;
						break w1;
					}
					if (par2.arc < d) {
						break w1;
					}
					pos1 = pos2;
					state1 = state2;
					par1 = par2;
				} else {
					break w1;
				}
			}
		}

		for (Direction dir : Direction.Plane.HORIZONTAL) {
			int hd = holdDist[dir.getHorizontalIndex()];
			if (hd == 0) {
				continue;
			}
			int d = hd - 2;
			float force = par0.mass * G;
			boolean success = false;
			boolean upped = false;
			BlockPos pos1 = pos0;
			BlockState state1 = state0;
			BlockPhysicsPars par1 = par0;
			w1: while (d++ <= par0.arc && !success) {
				BlockPos pos2 = pos1.offset(dir);
				BlockState state2 = getBlockState(pos2);
				BlockPhysicsPars par2 = getParam(state2);
				force += par2.mass * G;
				boolean el = true;
				if (canSupport(state1, state2, pos1, pos2, par1, par2, force, true)) {
					if (supportDown(pos2, state2, par2, force)) {
						return true;
					}
					if (par2.arc >= d) {
						upped = false;
						pos1 = pos2;
						state1 = state2;
						par1 = par2;
						el = false;
					}
				}
				if (el) {
					if (upped) {
						//System.out.println("a " + pos2);
						break w1;
					}
					pos2 = pos1.up();
					state2 = getBlockState(pos2);
					par2 = getParam(state2);
					if (par2.arc > 0 && par2.arc < d) {
						//System.out.println("b");
						break w1;
					}
					upped = true;
					if (canSupport(state1, state2, pos1, pos2, par1, par2, force, true)) {
						d = hd - 3;
						pos1 = pos2;
						state1 = state2;
						par1 = par2;
					} else {
						//System.out.println("c " + pos2 + " " + pos0);
						break w1;
					}
				}
			}
		}

		return false;
	}

	private boolean checkCeiling(BlockPos pos1, BlockState state1, BlockPhysicsPars par1) {

		//boolean mode = task.type != Type.DOWNRAY;
		boolean mode = true;

		Direction dir = Direction.EAST;

		BlockPos pos2 = pos1.offset(dir);
		BlockState state2 = getBlockState(pos2);
		BlockPhysicsPars par2 = getParam(state2);
		float force = par1.mass * G;
		if (canSupport(state1, state2, pos1, pos2, par1, par2, force, mode)) {
			BlockPos pos3 = pos1.offset(dir.getOpposite());
			BlockState state3 = getBlockState(pos3);
			BlockPhysicsPars par3 = getParam(state3);
			if (canSupport(state1, state3, pos1, pos3, par1, par3, force, mode)) {
				if (proofCeli(pos2, state2, par2, dir, mode, force)
						&& proofCeli(pos3, state3, par3, dir.getOpposite(), mode, force)) {
					return true;
				}
			}
		}
		dir = Direction.NORTH;
		pos2 = pos1.offset(dir);
		state2 = getBlockState(pos2);
		par2 = getParam(state2);
		if (canSupport(state1, state2, pos1, pos2, par1, par2, force, mode)) {
			BlockPos pos3 = pos1.offset(dir.getOpposite());
			BlockState state3 = getBlockState(pos3);
			BlockPhysicsPars par3 = getParam(state3);
			if (canSupport(state1, state3, pos1, pos3, par1, par3, force, mode)) {
				if (proofCeli(pos2, state2, par2, dir, mode, force)
						&& proofCeli(pos3, state3, par3, dir.getOpposite(), mode, force)) {
					return true;
				}
			}
		}

		return false;
	}

	private boolean proofCeli(BlockPos pos0, BlockState state0, BlockPhysicsPars param0, Direction dir, boolean mode,
			float force) {
		int maxDist = 32;
		int dist = 0;
		BlockPos pos1 = pos0;
		BlockState state1 = state0;
		BlockPhysicsPars par1 = param0;

		while (dist++ <= maxDist) {
			force += (par1.mass * G);
			BlockPos pos2 = pos1.offset(dir);
			BlockState state2 = getBlockState(pos2);
			BlockPhysicsPars par2 = getParam(state2);
			if (canSupport(state1, state2, pos1, pos2, par1, par2, force, mode)) {
				if (supportDown(pos2, state2, par2, force)) {
					return true;
				}
				pos1 = pos2;
				state1 = state2;
				par1 = par2;

			} else {
				return false;
			}
		}

		return false;
	}

	private boolean checkPane(BlockPos pos, BlockState state, BlockPhysicsPars param, boolean usp) {

		Set<BlockPos> posset = new HashSet<>();

		int dist = Math.max(param.linear, param.radial);
		if (usp) {
			//dist = Math.max(param.arc, dist);
		}
		if (dist < 1) {
			return false;
		}
		for (Direction dir : getRandomizedDirections(w.rand, false)) {
			int i = 0;
			float force = param.mass * G;
			BlockPos pos1 = pos;
			BlockState state1 = state;
			BlockPhysicsPars par1 = param;
			posset.add(pos);
			while (i < dist) {
				++i;

				BlockPos pos2 = pos.offset(dir, i);
				BlockState state2 = getBlockState(pos2);
				BlockPhysicsPars par2 = getParam(state2);
				posset.add(pos2);

				if (canSupport(state1, state2, pos1, pos2, par1, par2, force)) {
					if (ssa) {
						addDownrayTask(pos2.down());
						return true;
					}
					if (supportDown(pos2, state2, par2, force)) {
						// System.out.println(pos2);
						return true;
					} else {
						int remainDist = param.radial - i;
						if (remainDist > 0) {
							if (radialize(pos2, state2, par2, dir.rotateY(), force, posset, remainDist)
									|| radialize(pos2, state2, par2, dir.rotateYCCW(), force, posset, remainDist)) {
								return true;
							}

						}
					}
				} else {
					break;
				}
				force += (par2.mass * G);
				pos1 = pos2;
				state1 = state2;
				par1 = par2;
			}
		}

		return false;
	}

	private static boolean ssa = false;

	private boolean radialize(BlockPos pos0, BlockState state0, BlockPhysicsPars par0, Direction dir0, float force0,
			Set<BlockPos> set, int dist0) {

		Set<Quad> ths = new HashSet<>();
		Set<Quad> next = new HashSet<>();

		BlockPos pos1 = pos0.offset(dir0);
		if (set.contains(pos1)) {
			return false;
		}
		next.add(new Quad(pos0, state0, par0, dist0));

		while (!next.isEmpty()) {
			ths = next;
			next = new HashSet<>();
			for (Quad q : ths) {
				set.add(q.pos);
				for (Direction dir : getRandomizedDirections(w.rand, false)) {
					if (supportDown(q.pos, q.state, q.par, force0 + (q.par.mass * G))) {
						return true;
					}
					if (q.dist < 1) {
						break;
					}
					BlockPos pos2 = q.pos.offset(dir);
					if (set.contains(pos2) || onLine(pos2)) {
						continue;
					}
					// debug(q.pos);
					BlockState state2 = getBlockState(pos2);
					BlockPhysicsPars par2 = getParam(state2);
					if (canSupport(q.state, state2, q.pos, pos2, q.par, par2, force0 + (q.par.mass * G))) {
						next.add(new Quad(pos2, state2, par2, q.dist - 1));
					}
				}
			}
		}

		return false;
	}

	private boolean onLine(BlockPos pos2) {
		return pos.getX() == pos2.getX() || pos.getZ() == pos2.getZ();
	}

	private boolean supportDown(BlockPos pos0, BlockState state0, BlockPhysicsPars par0, float force) {
		BlockPos posd = pos0.down();
		BlockState stated = getBlockState(posd);
		BlockPhysicsPars pard = getParam(stated);
		// float force2 = force + (pard.mass * G);
		if (canSupport(state0, stated, pos0, posd, par0, pard, force)) {
			// System.out.println(posd);
			addDownrayTask(posd);
			return true;
		}
		// debug(pos0);
		// System.out.println(state0 + " " + stated);
		return false;
	}

	private boolean canFall(BlockState stated) {
		float force = param.mass * G;
		BlockPos posd = pos.down();
		boolean support = canSupport(state, stated, pos, posd, param, getParam(stated), force);
		return !support;
	}

	private boolean checkLogs(BlockState state1, BlockState state2, Direction dir) {
		FeatureContainer fc1 = BFUtils.getFeatures(state1.getBlock());
		FeatureContainer fc2 = BFUtils.getFeatures(state2.getBlock());
		if (checkLog(fc1, state1, dir) || checkLog(fc2, state2, dir)) {
			return true;
		}
		return false;
	}

	private boolean checkLog(FeatureContainer fc, BlockState state1, Direction dir) {
		boolean bl = false;
		if (fc != null && !fc.isEmpty()) {
			if (fc.contains(FeatureContainer.Simple.LOGS)) {
				bl = !state1.get(BlockStateProperties.AXIS).equals(dir.getAxis());
			}
		}
		return bl;
	}

	private boolean canSupport(BlockState state1, BlockState state2, BlockPos pos1, BlockPos pos2,
			BlockPhysicsPars par1, BlockPhysicsPars par2, float force, boolean... flags) {
		ssa = false;
		if (isAir(state2) || state2.getBlock() instanceof FlowingFluidBlock ) {
			return false;
		}
		Direction dir = dirFromVec(pos1, pos2);
		if (checkLogs(state1, state2, dir)) {
			return false;
		}
		boolean arc = false;
		if (flags.length > 0) {
			arc = flags[0];
		}

		// Direction dir = dirFromVec(pos1, pos2);
		VoxelShape voxelShape2 = state2.getCollisionShape(w, pos2);
		// VoxelShape voxelShape1 = state1.getCollisionShape(w, pos1);
		if (voxelShape2.isEmpty()) {
			return false;
		}
		// return VoxelShapes.doAdjacentCubeSidesFillSquare(voxelShape1, voxelShape2,
		// dir);

		if (par2.strength < force/* || par1.strength < force */) {
			return false;
		}

		if (!par2.falling && !par2.fragile) {
			return true;
		}

		boolean empty = par1.attachIgnore.isEmpty();
		boolean empty2 = par2.attachIgnore.isEmpty();

		boolean ssa2 = false;

		if (dir != Direction.DOWN && !arc && !par1.selfList.contains(state2.getBlock())) {
			if (par1.attach) {
				if (!empty && par1.attachIgnore.contains(state2.getBlock())) {
					return false;
				} else {
					// return true;
				}
			} else {
				if (!empty && par1.attachIgnore.contains(state2.getBlock())) {
					ssa2 = true;
					//return true;
				} else {
					return false;
				}
			}
			if (par2.attach) {
				// if (!empty2 && par2.attachIgnore.contains(state1.getBlock())) {
				// return false;
				// } else {
				// // return true;
				// }
			} else {
				if (!empty2 && par2.attachIgnore.contains(state1.getBlock())) {
					// return true;
				} else {
					if (ssa2 == true) {
						ssa = true;
						return true;
					}
					return false;
				}
			}
		}
		return true;
	}

	public static BlockPhysicsPars getParam(Block b, World w, BlockPos pos) {
		return BFUtils.getParam(b, pos, w);
	}

	private BlockPhysicsPars getParam(BlockState s) {
		return getParam(s.getBlock(), w, pos);
	}

	private void addDownrayTask(BlockPos p) {
		int lim = BPOConfig.MAIN.downCheckLimit;
		if (lim != -1 && lim <= task.counter) {
			return;
		}
		//BPO.LOGGER.info(pos + "  " + p);
		if (p.getY() >= pos.getY()) {
			p = new BlockPos(p.getX(), pos.getY() - 1, p.getZ());
		}
		BFTask newTask = new BFTask(castOwner, p, Type.DOWNRAY);
		newTask.counter = task.counter + 1;
		WWS.pushTask(newTask);
	}

	private static class Quad {
		public final BlockPos pos;
		public final BlockState state;
		public final BlockPhysicsPars par;
		public final int dist;

		public Quad(BlockPos pos, BlockState state, BlockPhysicsPars par, int dist) {
			this.pos = pos;
			this.state = state;
			this.par = par;
			this.dist = dist;
		}
	}

	private boolean occupyPos(BlockPos pos) {
		if (BFUtils.occupyPos(castOwner, pos)) {
			return true;
		}
		ocuFail = true;
		return false;
	}
}