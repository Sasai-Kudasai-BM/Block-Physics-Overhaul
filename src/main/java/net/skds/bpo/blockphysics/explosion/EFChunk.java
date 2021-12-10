package net.skds.bpo.blockphysics.explosion;

import java.util.BitSet;
import java.util.function.IntConsumer;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.server.ServerChunkProvider;
import net.skds.bpo.blockphysics.WWS;
import net.skds.bpo.network.ExplosionPacket;
import net.skds.bpo.util.IndexedCord4B;
import net.skds.core.network.PacketHandler;

public class EFChunk extends IndexedCord4B {

	public ExplosionPacket packet;
	int datIndex;

	public final int x, y, z;

	public final CustomExplosion exp;

	public float[] vx = new float[4096];
	public float[] vy = new float[4096];
	public float[] vz = new float[4096];
	public float[] p = new float[4096];

	public int emptyLife = 0;
	public float mp = 1488;

	BitSet tickIndexes = new BitSet(4096);
	BitSet nextTickIndexes = new BitSet(4096);

	public EFChunk(long index, CustomExplosion exp) {
		this.x = SectionPos.extractX(index);
		this.y = SectionPos.extractY(index);
		this.z = SectionPos.extractZ(index);
		this.exp = exp;

		this.packet = new ExplosionPacket(WWS.EX_STEPS, x, y, z);

		//if (x == -1 && y ==1 && z ==1) {
		//System.out.printf("x:%s y:%s z:%s\n", x, y, z);
		//}
	}

	public void reset() {
		ServerChunkProvider scp = exp.world.getChunkProvider();
		scp.chunkManager.getTrackingPlayers(new ChunkPos(x, z), false).forEach(p -> PacketHandler.send(p, packet));
		packet = new ExplosionPacket(WWS.EX_STEPS, x, y, z);
	}

	public void setPressure(int dx, int dy, int dz, float pressure) {
		int index = index(dx, dy, dz);
		tickIndexes.set(index);
		p[index] = pressure;
	}

	public boolean isEmpty() {
		//return tickIndexes.isEmpty() || mp < 0.8f;
		//System.out.println(mp);
		return emptyLife > 3;
		//return tickIndexes.isEmpty();
	}

	public void iterate(IntConsumer func) {
		int index = 0;
		if (tickIndexes.get(index)) {
			func.accept(index);
		}
		while ((index = tickIndexes.nextSetBit(index + 1)) != -1) {
			func.accept(index);
		}
	}

	public void swap() {
		tickIndexes = nextTickIndexes;
		nextTickIndexes = new BitSet(4096);
		packet.cords = new BitSet(4096);
		packet.data = new byte[tickIndexes.size()];
		datIndex = 0;
		iterate((index) -> {
			pack(index, packet.cords);
		});
		packet.writeStep();
	}

	public void pack(int i, BitSet set) {

		if (p[i] > 1) {
			final float k = 0.001f;
			byte bp = 0;
			byte bx = (byte) (vx[i] > k ? 2 : vx[i] < -k ? 0 : 1);
			byte by = (byte) (vy[i] > k ? 8 : vy[i] < -k ? 0 : 4);
			byte bz = (byte) (vz[i] > k ? 32 : vz[i] < -k ? 0 : 16);

			packet.data[datIndex] = (byte) (bp | bx | by | bz);
			set.set(i);
		}
		datIndex++;
	}

	public void addTick(int index) {
		nextTickIndexes.set(index);
	}

	public void tickA() {
		iterate((index) -> {
			p2vel(x(index), y(index), z(index));
		});
	}

	public void tickB() {
		mp = 0;
		iterate((index) -> {
			vel2p(x(index), y(index), z(index));
			//exp.debug(x(index) + (x << 4), y(index) + (y << 4), z(index) + (z << 4), p[index], len(vx[index], vy[index], vz[index]));
		});
		swap();
		if (mp > 0.6) {
			emptyLife = 0;
		}
	}

	private EFChunk getOrCreate(int x, int y, int z) {
		final int i16 = ~15;
		if ((x & i16) == 0 && (y & i16) == 0 && (z & i16) == 0) {
			return this;
		} else {
			return exp.getOrCreate(byBlockPos((this.x << 4) + x, (this.y << 4) + y, (this.z << 4) + z));
		}
	}

	public static boolean isEmpty(float p, float v) {
		//return p < 0.2 || (p < 0.1 && v < 0.1);
		return Math.abs(p) < 0.1 && v < 0.1;
	}

	public void vel2p(int x, int y, int z) {
		int i = index(x, y, z);

		int ix = index(x - 1, y, z);
		int iy = index(x, y - 1, z);
		int iz = index(x, y, z - 1);

		EFChunk cx = getOrCreate(x - 1, y, z);
		EFChunk cy = getOrCreate(x, y - 1, z);
		EFChunk cz = getOrCreate(x, y, z - 1);

		float dp = 0.0f;
		float dx = cx.vx[ix] - vx[i];
		float dy = cy.vy[iy] - vy[i];
		float dz = cz.vz[iz] - vz[i];

		dp += (dx *= 0.33F);
		dp += (dy *= 0.33F);
		dp += (dz *= 0.33F);

		//dp *= 0.33F;

		//float[] r = react(x, y, z, dx, dy, dz);
		//dp *= r[0];
		//dp *= 1f - r[1];

		p[i] *= 0.999F;
		p[i] += dp;

		mp = Math.max(Math.abs(p[i]), mp);

		//if (p[i] < 0) {
		//	p[i] = 0;
		//}

		exp.maxPressure = Math.max(exp.maxPressure, p[i]);

		if (!isEmpty(p[i], lensq(vx[i], vy[i], vz[i]))) {
			addTick(i);
			cx.addTick(ix);
			cy.addTick(iy);
			cz.addTick(iz);
			getOrCreate(x + 1, y, z).addTick(index(x + 1, y, z));
			getOrCreate(x, y + 1, z).addTick(index(x, y + 1, z));
			getOrCreate(x, y, z + 1).addTick(index(x, y, z + 1));
		} else {
			p[i]  *= 0.85f;
			vx[i] *= 0.85f;
			vy[i] *= 0.85f;
			vz[i] *= 0.85f;
		}
	}

	public void p2vel(int x, int y, int z) {
		int i = index(x, y, z);

		int ix = index(x + 1, y, z);
		int iy = index(x, y + 1, z);
		int iz = index(x, y, z + 1);

		EFChunk cx = getOrCreate(x + 1, y, z);
		EFChunk cy = getOrCreate(x, y + 1, z);
		EFChunk cz = getOrCreate(x, y, z + 1);

		float dx = p[i] - cx.p[ix];
		float dy = p[i] - cy.p[iy];
		float dz = p[i] - cz.p[iz];

		final float k = 0.33f;

		vx[i] *= 0.999F;
		vy[i] *= 0.999F;
		vz[i] *= 0.999F;

		dx *= k;
		dy *= k;
		dz *= k;

		vx[i] += dx;
		vy[i] += dy;
		vz[i] += dz;

		float[] r = react(x, y, z, dx, dy, dz);

		if (r[0] != 1f) {

			vx[i] *= r[0];
			vy[i] *= r[0];
			vz[i] *= r[0];
		} else {
			if ((r = react(x + 1, y, z, -dx, dy, dz))[0] != 1f) {
				vx[i] *= r[0];
			}
			if ((r = react(x, y + 1, z, dx, -dy, dz))[0] != 1f) {
				vy[i] *= r[0];
			}
			if ((r = react(x, y, z + 1, dx, dy, -dz))[0] != 1f) {
				vz[i] *= r[0];
			}
		}

	}

	// 0 non-reflect
	private float[] react(int x, int y, int z, float fwx, float fwy, float fwz) {
		BlockState bs = getBs(x, y, z);
		if (bs.isIn(Blocks.AIR) || bs.isIn(BlockTags.IMPERMEABLE)) {
			return new float[] { 1f };
		}
		return new float[] { 0.0f };
	}

	private BlockState getBs(int x, int y, int z) {
		BlockPos pos = new BlockPos(x + (this.x << 4), y + (this.y << 4), z + (this.z << 4));
		return exp.reader.getBlockState(pos);
	}

	public static long byBlockPos(int x, int y, int z) {
		return SectionPos.asLong(x >> 4, y >> 4, z >> 4);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (obj instanceof EFChunk) {
			EFChunk e2 = (EFChunk) obj;
			return x == e2.x && y == e2.y && z == e2.z;
		}
		return false;
	}

	private static float lensq(float vx, float vy, float vz) {
		return (vx * vy) + (vx * vy) + (vz * vz);
	}

}
