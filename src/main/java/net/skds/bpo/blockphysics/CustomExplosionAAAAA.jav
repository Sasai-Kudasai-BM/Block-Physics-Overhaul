package net.skds.bpo.blockphysics;

import java.util.ArrayList;

import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.skds.bpo.util.IndexedCord;
import net.skds.core.multithreading.TurboWorldReader;

public class CustomExplosion extends IndexedCord {

	static final byte BYTE = (byte) 0xff;

	final static double[] kernel = new double[9];

	public final TurboWorldReader reader;

	public final float power;
	public final Vector3d pos;
	public final World world;
	public final Explosion explosion;

	Int2ObjectRBTreeMap<Entry> map = new Int2ObjectRBTreeMap<>((a, b) -> a - b);

	public CustomExplosion(World world, Vector3d pos, float power, Explosion explosion) {
		this.reader = new TurboWorldReader(world);
		this.world = world;
		this.pos = pos;
		this.power = power;
		this.explosion = explosion;
	}

	public Entry put(Entry e) {
		return map.put(e.hashCode(), e);
	}

	public Entry getOrCreate(int index) {
		return map.computeIfAbsentPartial(index, Entry::new);
	}

	public Entry getOrCreate(int index, boolean empty) {
		if (empty) {
			Entry e = map.get(index);
			if (e == null) {
				e = new Entry(127, 127, 127);
			}
			return e;
		}
		return map.computeIfAbsentPartial(index, Entry::new);
	}

	public void explode() {
		Entry e = new Entry(0, 0, 0);
		put(e);

		for (int i = -1; i < 2; i++) {
			for (int j = -1; j < 2; j++) {
				if (i == 0 && j == 0) {
					continue;
				}
				put(new Entry(i, 0, j));
			}
		}
		e.pressure = 70.0;

		//do {
		//	iterate();
		//} while (!map.isEmpty());
	}

	public void iterate() {
		ArrayList<Entry> list = new ArrayList<>(map.size());
		map.forEach((i, e) -> {
			//e.age++;
			list.add(e);
		});
		for (Entry e : list) {
			e.p2vel();
		}
		for (Entry e : list) {
			e.vel2p();
		}
		for (Entry e : list) {
			//e.spread();
		}

		ArrayList<Entry> list2 = new ArrayList<>();
		map.forEach((i, e) -> {
			list2.add(e);
		});
		boolean empty = true;
		for (Entry e : list2) {

			//Minecraft.getInstance().world.addParticle(ParticleTypes.FLAME, pos.x + e.x, pos.y + e.y + 1, pos.z + e.z, e.vel.x * 1, 0.2, e.vel.z * 1);
			debug(e);
			if (!e.isEmpty() && !e.isStatic()) {
				empty = false;
			} else {

				//world.setBlockState(new BlockPos(pos.x + e.x, pos.y + e.y + 5, pos.z + e.z),
				//		Blocks.AIR.getDefaultState(), 18);
				//map.remove(e.hashCode());
			}

		}
		if (empty) {
			map.clear();
			((ServerWorld) world).getServer().getPlayerList().func_232641_a_(new StringTextComponent(empty + " " + list2.size()),
					ChatType.CHAT, null);
		}
		//((ServerWorld) world).getServer().sendMessage(new StringTextComponent(n + " " + n2), null);
		//list.clear();
		//map.forEach((i, e) -> {
		//	//if (!e.isEmpty())
		//	list.add(e);
		//});

		//System.out.println(list.size());
	}

	class Entry {
		//public final Entry[] neib = new Entry[6];
		public int age = 0;
		public final byte x;
		public final byte y;
		public final byte z;
		public Vector3d velOld = Vector3d.ZERO;
		public Vector3d vel = Vector3d.ZERO;
		public double pressureOld = 0;
		public double pressure = 0;

		public Entry(int index) {
			this(x(index), y(index), z(index));
		}

		public Entry(int dx, int dy, int dz) {
			this.x = (byte) dx;
			this.y = (byte) dy;
			this.z = (byte) dz;

			//world.setBlockState(new BlockPos(pos.x + x, pos.y + y + 5, pos.z + z), Blocks.AIR.getDefaultState(),
			//		18);
		}

		public void vel2p() {
			pressureOld = pressure;
			int ix = index(x - 1, y, z);
			int iz = index(x, y, z - 1);

			double dp = 0.0;
			Entry ex = getOrCreate(ix);
			Entry ez = getOrCreate(iz);
			dp += ex.vel.x - vel.x;
			dp += ez.vel.z - vel.z;

			dp *= 0.3;

			pressure += dp;
			if (pressure < -0.1) {
				pressure = -0.1;
			}
		}

		public void p2vel() {
			velOld = vel;
			//if (pressure < 1E-3) {
			//	vel = Vector3d.ZERO;
			//	return;
			//}

			int ix = index(x + 1, y, z);
			int iz = index(x, y, z + 1);

			Entry ex = getOrCreate(ix);
			Entry ez = getOrCreate(iz);
			double dx = pressure - ex.pressure;
			double dz = pressure - ez.pressure;

			final double k = 1.0;

			vel = vel.add(dx * k, 0, dz * k);
		}

		public void spread() {


			double dx = 0.0;
			double dz = 0.0;
			double dp = 0.0;
			for (int i = -1; i < 2; i++) {
				for (int j = -1; j < 2; j++) {
					int index = index(x + i, y, z + j);
					Entry e = getOrCreate(index);
					double ker = kernel[k(i, j)];
					dx += e.vel.x * ker;
					dz += e.vel.z * ker;
					dp += e.pressure * ker;
				}
			}

			if (dp < 0) {
				dp = 0;
			}

			pressure = dp;
			vel = new Vector3d(dx, 0, dz);
		}

		public boolean isStatic() {
			//return Math.abs(pressure - pressureOld) < 1E-4 && (vel.lengthSquared() - velOld.lengthSquared()) < 1e-8;
			return false;
		}

		public boolean isEmpty() {
			double ls = vel.lengthSquared();
			return pressure < 1E-9 && ls < 1E-18;
			//return pressure < 5E-3 || (pressure < 2E-1 && ls < 1E-2);
			//return (pressure - .1) < 1E-3 && vel.lengthSquared() < 1E-6;
			//return pressure < 1E-3;
			//return false;
		}

		@Override
		public int hashCode() {
			return index(x, y, z);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			} else if (obj instanceof Entry) {
				Entry e2 = (Entry) obj;
				return x == e2.x && y == e2.y && z == e2.z;
			}
			return false;
		}
	}

	void debug(Entry e) {

		//if (e.isEmpty()) {
		//	continue;
		//}

		Block[] blocks = new Block[] { Blocks.SANDSTONE, Blocks.BROWN_WOOL, Blocks.RED_WOOL, Blocks.ORANGE_WOOL,
				Blocks.YELLOW_WOOL, Blocks.LIME_WOOL, Blocks.GREEN_WOOL, Blocks.CYAN_WOOL, Blocks.LIGHT_BLUE_WOOL,
				Blocks.BLUE_WOOL, Blocks.PURPLE_WOOL, Blocks.WHITE_WOOL };

		Block[] blocks2 = new Block[] { Blocks.AIR, Blocks.BROWN_STAINED_GLASS, Blocks.RED_STAINED_GLASS,
				Blocks.ORANGE_STAINED_GLASS, Blocks.YELLOW_STAINED_GLASS, Blocks.LIME_STAINED_GLASS,
				Blocks.GREEN_STAINED_GLASS, Blocks.CYAN_STAINED_GLASS, Blocks.LIGHT_BLUE_STAINED_GLASS,
				Blocks.BLUE_STAINED_GLASS, Blocks.PURPLE_STAINED_GLASS, Blocks.WHITE_STAINED_GLASS };

		int i = (int) (e.pressure * 12);
		int i2 = (int) (e.vel.length() * 5);

		if (i < 0) {
			i = 0;
		}
		if (i2 < 0) {
			i2 = 0;
		}
		if (i >= blocks.length) {
			i = blocks.length - 1;
		}
		if (i2 >= blocks2.length) {
			i2 = blocks2.length - 1;
		}

		world.setBlockState(new BlockPos(pos.x + e.x, pos.y + e.y - 1, pos.z + e.z), blocks[i].getDefaultState(), 18);

		world.setBlockState(new BlockPos(pos.x + e.x, pos.y + e.y, pos.z + e.z), blocks2[i2].getDefaultState(), 18);

		//Minecraft.getInstance().world.addParticle(ParticleTypes.FLAME, pos.x + e.x, pos.y + e.y + 1, pos.z + e.z, e.vel.x * 1, 0.2, e.vel.z * 1);

		//if (e.pressure < 0.01) {
		//	world.setBlockState(new BlockPos(pos.x + e.x, pos.y + e.y, pos.z + e.z),
		//			Blocks.BEDROCK.getDefaultState());
		//}
	}

	static int k(int i, int j) {
		return (i + 1) + 3 * (j + 1);
	}

	static {
		int i, j;
		double s = 0.0;
		for (j = -1; j < 2; j++) {
			for (i = -1; i < 2; i++) {
				int k = k(i, j);
				kernel[k] = Math.exp(-2.0 * (i * i + j * j));
				s += kernel[k];
			}
		}
		s = 1.0 / s;
		for (j = -1; j < 2; j++) {
			for (i = -1; i < 2; i++) {
				kernel[k(i, j)] *= s;
			}
		}
	}
}
