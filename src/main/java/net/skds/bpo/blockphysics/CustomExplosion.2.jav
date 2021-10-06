package net.skds.bpo.blockphysics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
	private Long2ObjectArrayMap<FieldEntry> vectorFieldNew = new Long2ObjectArrayMap<>();
	private Long2ObjectArrayMap<FieldEntry> vectorFieldNew2 = new Long2ObjectArrayMap<>();

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
		//double pow = power * decayFuncAndCash(power, positionB) / 6;
		fillField(new FieldEntry(position, new Vector3d(positionB.getX(), positionB.getY(), positionB.getZ()), power));
		swapField();
		while (!vectorField.isEmpty()) {
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

	private void fillField(FieldEntry e) {
		long l = pack(e.position);
		if (e.pressure < 2E-2) {
			//for (PlayerEntity pl : world.getPlayers()) {
			//	PacketHandler.send(pl, new DebugPacket(BlockPos.fromLong(l)));
			//}
		}
		FieldEntry samp = vectorFieldNew.put(l, e);
		if (samp != null) {
			e.add(samp);
		}
	}

	private void fillFieldStrict(FieldEntry e) {
		long l = pack(e.position);
		vectorFieldNew.put(l, e);
	}

	private void interpolate() {
		vectorFieldNew.forEach(this::interpolation);
	}

	private void interpolation(long p, FieldEntry e) {
		int i = 0;
		double s = 0;
		for (FieldEntry en : e.getNeib().values()) {
			i++;
			s += en.pressure;
		}
		FieldEntry en2 = new FieldEntry(e.position, e.from, e.pressure);
		if (i > 0) {
			en2.pressure = s / i;
		}
		vectorFieldNew2.put(p, en2);
	}

	private void swapField() {
		interpolate();

		Long2ObjectArrayMap<FieldEntry> vf2 = new Long2ObjectArrayMap<>();
		vectorField.forEach((l, e) -> {
			e.incGen();
			if (e.generation <= 2) {
				vf2.put((long) l, e);
			}
		});
		vectorFieldNew2.forEach((l, e) -> {
			e.incGen();
			vf2.put((long) l, e);
		});
		vectorFieldNew.clear();
		vectorFieldNew2.clear();
		vectorField = vf2;
	}

	private void iterateField() {
		//List<FieldEntry> val = new ArrayList<>(vectorField.values());
		//Collections.shuffle(val);
		//for (FieldEntry e : val) {
		//	fieldIter(pack(e.position), e);
		//}

		vectorField.forEach(this::fieldIter);
		swapField();
	}

	private void fieldIter(long p, FieldEntry e) {
		if (e.generation > 1) {
			return;
		}
		Vector3d vec = e.getDirection();
		double pow = e.pressure;
		if (pow < 2E-2) {
			for (PlayerEntity pl : world.getPlayers()) {
				PacketHandler.send(pl, new DebugPacket(BlockPos.fromLong(p)));
			}
			e.pressure = 0;
			return;
		}
		BlockPos point = BlockPos.fromLong(p);
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

		int i = 0;
		Set<Vector3d> pre = new HashSet<>(8);

		Long2ObjectArrayMap<FieldEntry> neib = e.getNeib();

		List<Vector3d> dirL = new ArrayList<>();
		dirL.add(e.position.add(0, -1, 0));
		dirL.add(e.position.add(0, 1, 0));
		for (Direction dir : Direction.Plane.HORIZONTAL) {
			Direction dir2 = dir.rotateY();
			Vector3d director = new Vector3d(dir.getXOffset(), dir.getYOffset(), dir.getZOffset());
			Vector3d director2 = director.add(dir2.getXOffset(), dir2.getYOffset(), dir2.getZOffset()).add(0, 1, 0);
			Vector3d director3 = director2.add(0, -2, 0);
			
			Vector3d pos2 = e.position.add(director);

			Vector3d pos3u = e.position.add(director2.normalize());
			Vector3d pos3d = e.position.add(director3.normalize());
			

			dirL.add(pos2);
			dirL.add(pos3d);
			dirL.add(pos3u);
		}

		for (Vector3d vr : dirL) {
			if (hasOld(neib, e.generation, vr)) {
				continue;
			}
			i++;
			pre.add(vr);
		}

		double p2 = pow2 / i;

		for (Vector3d v : pre) {
			fillField(new FieldEntry(v, e.position, p2));
		}
	}

	private boolean hasOld(Long2ObjectArrayMap<FieldEntry> list, int gen, Vector3d position) {
		FieldEntry e = vectorField.get(pack(position));
		if (e != null && e.generation > gen) {
			//if (e.position.subtract(position).lengthSquared() < 1) {
				return true;
			//}
		}
		return false;
	}

	private double getPressure(Long2ObjectArrayMap<FieldEntry> list, Vector3d position) {
		FieldEntry e = vectorFieldNew.get(pack(position));
		if (e != null) {
			return e.pressure;
		}
		return 0;
	}

	private BlockState explodeConvert(BlockState state, double pressure) {
		ConversionPars conv = BFUtils.getConvParam(state.getBlock());
		if (conv.onExp() && pressure > conv.expP) {
			return conv.expState;
		}
		return state;
	}

	private class FieldEntry {
		public int generation = 0;
		public double pressure;
		public Vector3d position;
		public Vector3d from;

		FieldEntry(Vector3d position, Vector3d from, double pressure) {
			this.pressure = pressure;
			this.position = position;
			this.from = from;
		}

		public Long2ObjectArrayMap<FieldEntry> getNeib() {
			Long2ObjectArrayMap<FieldEntry> list = new Long2ObjectArrayMap<>();
			for (long l : getNeibPos(this)) {
				FieldEntry fe = vectorFieldNew.get(l);
				if (fe != null && fe != this) {
					list.put(l, fe);
				}
			}
			return list;
		}

		public Vector3d getDirection() {
			return position.subtract(from).normalize();
		}

		public void add(FieldEntry e) {
			generation = Math.max(generation, e.generation);
			pressure += e.pressure;
			position = position.add(e.position).scale(0.5);
			from = from.add(e.from).scale(0.5);
		}

		public void incGen() {
			generation++;
		}
	}

	private static long[] getNeibPos(FieldEntry fe) {
		long[] array = new long[0];
		int x0 = (int) Math.floor(fe.position.x) - 1;
		int y0 = (int) Math.floor(fe.position.y) - 1;
		int z0 = (int) Math.floor(fe.position.z) - 1;
		int xe = x0 + 2;
		int ye = y0 + 2;
		int ze = z0 + 2;
		for (int x = x0; x <= xe; x++) {
			for (int y = y0; y <= ye; y++) {
				for (int z = z0; z <= ze; z++) {
					int len0 = array.length;
					array = Arrays.copyOf(array, len0 + 1);
					array[len0] = BlockPos.pack(x, y, z);
				}
			}
		}
		return array;
	}

	private static long pack(Vector3d pos) {
		int x = (int) Math.floor(pos.x);
		int y = (int) Math.floor(pos.y);
		int z = (int) Math.floor(pos.z);
		return BlockPos.pack(x, y, z);
	}
}