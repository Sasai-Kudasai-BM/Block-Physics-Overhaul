package net.skds.bpo.blockphysics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;

import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.block.TNTBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SExplosionPacket;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.skds.bpo.BPOConfig;
import net.skds.bpo.entity.AdvancedFallingBlockEntity;
import net.skds.bpo.network.DebugPacket;
import net.skds.bpo.util.BFUtils;
import net.skds.bpo.util.pars.BlockPhysicsPars;
import net.skds.bpo.util.pars.ConversionPars;
import net.skds.core.multithreading.TurboWorldReader;
import net.skds.core.network.PacketHandler;
import net.skds.core.util.other.Pair;
import net.skds.core.util.blockupdate.BasicExecutor;

public class CustomExplosion {
	final double k1 = .2E-2F;
	static final double sqr2 = 1.7320508;

	private final Explosion initiator;
	private final boolean fire;
	private final Random random = new Random();
	private final World world;
	@Nullable
	private final Entity exploder;
	private final double power;
	private final List<BlockPos> affectedBlockPositions = Lists.newArrayList();

	private final Long2ObjectArrayMap<Pair<BlockState, BlockPhysicsPars>> dataMap = new Long2ObjectArrayMap<>();
	// private Long2ObjectArrayMap<Vector3d> vectorField = new
	// Long2ObjectArrayMap<>();
	// private Long2ObjectArrayMap<Vector3d> vectorFieldOld = new
	// Long2ObjectArrayMap<>();
	private Long2ObjectArrayMap<FieldEntry> vectorField = new Long2ObjectArrayMap<>();
	private Long2ObjectArrayMap<FieldEntry> vectorFieldOld = new Long2ObjectArrayMap<>();
	private Long2ObjectArrayMap<FieldEntry> vectorFieldVeryOld = new Long2ObjectArrayMap<>();

	private final Vector3d position;
	private final BlockPos positionB;

	public CustomExplosion(World world, Vector3d pos, double power, @Nullable Entity entity, boolean fire,
			Explosion initiator) {
		this.world = world;
		this.position = pos;
		this.power = power;
		this.exploder = entity;
		this.fire = fire || BPOConfig.MAIN.explosionFire;
		this.initiator = initiator;
		this.positionB = new BlockPos(position);
	}

	public void doExplode() {
		if (!world.isRemote) {
			step0();
			for (PlayerEntity player : world.getPlayers()) {
				if (player.getDistanceSq(position.x, position.y, position.z) < 4096.0D) {
					((ServerPlayerEntity) player).connection.sendPacket(new SExplosionPacket(position.x, position.y,
							position.z, (float) power, initiator.getAffectedBlockPositions(),
							initiator.getPlayerKnockbackMap().get(player)));
				}
			}
		}
	}

	private void step0() {
		double pow = power * decayFuncAndCash(power, positionB) / 6;
		vectorFieldOld.put(positionB.toLong(), new FieldEntry(Direction.UP, 1));
		for (Direction dir : Direction.values()) {
			BlockPos pos2 = positionB.offset(dir);
			//Vector3d powVec = new Vector3d(pos2.getX() + 0.5, pos2.getY() + 0.5, pos2.getZ() + 0.5).subtract(position)
			//		.scale(pow);
			fillField(pos2, new FieldEntry(dir, pow));
		}
		swapField();
		while (!vectorFieldOld.isEmpty()) {
			iterateField();
		}
	}

	private double decayFuncAndCash(double power, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		BlockPhysicsPars par = BFUtils.getParam(state.getBlock(), pos, world);
		putInMap(pos, state, par);
		if (state.getBlock() instanceof FlowingFluidBlock) {
			return 1.0;
		}
		return decayFunc(par.strength, state.getExplosionResistance(world, pos, initiator), power);
	}

	private double getDura(double stren, double res) {
		double s;
		if (stren == 0) {
			s = res * 200;
		} else {
			s = stren;
		}
		return s * k1;
	}

	private double decayFunc(double stren, double res, double power) {
		if (res >= 3_000_000) {
			return 0;
		}
		double remain = power - getDura(stren, res);
		// final double k2 = 0.02F;
		// double remain = power - (res * k2);
		remain = remain > 0 ? remain : 0;
		return remain / power;
	}

	private void putInMap(BlockPos pos, BlockState state, BlockPhysicsPars par) {
		dataMap.put(pos.toLong(), new Pair<BlockState, BlockPhysicsPars>(state, par));
	}

	private double fillField(BlockPos pos, FieldEntry e) {
		if (e.pressure < 2E-2) {
			for (PlayerEntity pl : world.getPlayers()) {
				PacketHandler.send(pl, new DebugPacket(pos));
			}
			return 0.0;
		}
		long l = pos.toLong();
		FieldEntry samp = vectorField.put(l, e);
		if (samp != null) {
			return e.add(samp);
		}
		return 0.0;
	}

	private void fillFieldStrict(BlockPos pos, FieldEntry e) {
		long l = pos.toLong();
		//vectorField.put(l, e);		
		FieldEntry samp = vectorField.put(l, e);
		if (samp != null) {
			e.addStrict(e);
		}
	}

	private boolean hasOld(BlockPos pos, double pressure) {
		long l = pos.toLong();
		FieldEntry e = vectorFieldVeryOld.get(l);
		if (e != null && e.pressure > pressure / 3) {
			return true;
		} else {
			e = vectorFieldOld.get(l);
			if (e != null && e.pressure > pressure / 3) {
				return true;
			}
		}
		return false;
	}

	private void swapField() {
		vectorFieldVeryOld = vectorFieldOld;
		vectorFieldOld = vectorField;
		vectorField = new Long2ObjectArrayMap<>();
	}

	private void iterateField() {
		vectorFieldOld.forEach(this::fieldIter);
		swapField();
	}

	private void fieldIter(long p, FieldEntry e) {
		Vector3d vec = e.result;
		double pow = e.pressure;
		if (pow < 2E-2) {
			e.pressure = 0;
			return;
		}
		BlockPos point = BlockPos.fromLong(p);
		//for (PlayerEntity pl : world.getPlayers()) {
		//	PacketHandler.send(pl, new DebugPacket(point));
		//}
		double mp = decayFuncAndCash(pow, point);
		if (mp <= 0) {
			e.pressure = 0;
			return;
		}
		Pair<BlockState, BlockPhysicsPars> pair = dataMap.get(p);
		BlockState state = pair.a;
		BlockPhysicsPars par = pair.b;

		if (!TurboWorldReader.isAir(state) && !(state.getBlock() instanceof FlowingFluidBlock)) {
			double dur = getDura(par.strength, state.getExplosionResistance(world, point, initiator));
			boolean fire = (this.fire && pow > 0.1
					&& state.isFlammable(world, point, BasicExecutor.dirFromVec(point, positionB))
					&& random.nextFloat() > 0.6F);
			state = explodeConvert(state, pow / k1);
			if (state.getBlock() instanceof TNTBlock) {
				state.onBlockExploded(world, point, initiator);
				// world.setBlockState(point, Blocks.AIR.getDefaultState(), 3);
				// CustomExplosion ce = new CustomExplosion(world, position, 4, exploder, false,
				// initiator);
				// ce.doExplode();
			} else if (pow > 5 * dur && par.fragile) {
				if (pow > 15 * dur) {
					world.setBlockState(point, fire ? Blocks.FIRE.getDefaultState() : Blocks.AIR.getDefaultState(), 3);
				} else {
					world.destroyBlock(point, BPOConfig.MAIN.dropDestroyedBlocks);
				}
			} else {

				world.setBlockState(point, fire ? Blocks.FIRE.getDefaultState() : Blocks.AIR.getDefaultState(), 3);
				AdvancedFallingBlockEntity fbe = new AdvancedFallingBlockEntity(world, point.getX() + 0.5, point.getY(),
						point.getZ() + 0.5, state);
				Vector3d motion = vec.scale(3000D / fbe.pars.mass);
				if (motion.length() > 3) {
					motion = motion.normalize().scale(3);
				}
				if (fire) {
					fbe.setFire(100);
				}
				fbe.setMotion(motion);
				// fbe.setMotion(vec.scale(10D));
				fbe.fallTime = -4;
				// world.addEntity(fbe);
			}
		}

		double pow2 = pow * mp;

		double[] arr = new double[6];
		double si = 0;

		int i = 0;
		Map<BlockPos, Direction> pre = new HashMap<>(8);
		for (Direction dir : Direction.values()) {
			Vector3d director = new Vector3d(dir.getXOffset(), dir.getYOffset(), dir.getZOffset());
			double summ = director.dotProduct(vec.normalize());
			if (summ < -0.5 || e.dir.getOpposite() == dir) {
				//continue;
			}
			BlockPos posa = point.offset(dir);
			if (hasOld(posa, pow2)) {
				continue;
			}
			double s = summ + 1;
			//s *= s;
			arr[dir.getIndex()] = s;
			si += s;
			i++;
			pre.put(posa, dir);
		}

		double extra = 0.0;

		for (Map.Entry<BlockPos, Direction> en : pre.entrySet()) {
			BlockPos pos = en.getKey();
			Direction dir = en.getValue();
			double ref = fillField(pos, new FieldEntry(dir, pow2 * arr[dir.getIndex()] / si));
			extra += ref;
		}

		if (extra > 0) {
			Direction offset = Direction.getFacingFromVector(e.result.x, e.result.y, e.result.z);
			fillFieldStrict(point.offset(offset), new FieldEntry(e.dir, extra));
		}
	}

	private BlockState explodeConvert(BlockState state, double pressure) {
		ConversionPars conv = BFUtils.getConvParam(state.getBlock());
		if (conv.onExp() && pressure > conv.expP) {
			return conv.expState;
		}
		return state;
	}

	private static class FieldEntry {
		public double pressure;
		public Direction dir;
		public Vector3d result;

		FieldEntry(Direction dir, double pressure) {
			this.result = new Vector3d(dir.getXOffset() * pressure, dir.getYOffset() * pressure,
					dir.getZOffset() * pressure);
			this.pressure = pressure;
			this.dir = dir;
		}

		public double add(FieldEntry e) {
			if (e.pressure > pressure) {
				dir = e.dir;
			}
			pressure += e.pressure;
			double ret = pressure;

			//ret -= pressure /= 1.41421356;
			ret = 0;

			result = result.add(e.result).normalize().scale(pressure);
			return ret;
		}

		public void addStrict(FieldEntry e) {
			if (e.pressure > pressure) {
				dir = e.dir;
			}
			pressure += e.pressure;
			result = result.add(e.result).normalize().scale(pressure);
		}
	}
}