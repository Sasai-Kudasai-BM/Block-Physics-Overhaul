package net.skds.bpo.blockphysics.explosion;

import java.util.Arrays;
import java.util.BitSet;
import java.util.function.IntConsumer;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.server.ServerChunkProvider;
import net.skds.bpo.blockphysics.BlockPhysicsData;
import net.skds.bpo.blockphysics.WWS;
import net.skds.bpo.entity.AdvancedFallingBlockEntity;
import net.skds.bpo.network.ExplosionPacket;
import net.skds.bpo.util.BFUtils;
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
	public float[] dp = new float[4096];

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
		if (!packet.isEmpty) {
			ServerChunkProvider scp = exp.world.getChunkProvider();
			scp.chunkManager.getTrackingPlayers(new ChunkPos(x, z), false).forEach(p -> PacketHandler.send(p, packet));
		}
		packet = new ExplosionPacket(WWS.EX_STEPS, x, y, z);
	}

	public void setInit(int dx, int dy, int dz, float pressure) {
		int index = index(dx, dy, dz);
		tickIndexes.set(index);
		p[index] = pressure;

		//EFChunk cx = getOrCreate(dx - 1, dy, dz);
		//int ix = index(dx - 1, dy, dz);
		//EFChunk cy = getOrCreate(dx, dy - 1, dz);
		//int iy = index(dx, dy - 1, dz);
		//EFChunk cz = getOrCreate(dx, dy, dz - 1);
		//int iz = index(dx, dy, dz - 1);
		float dp = pressure * -0.36f;
		vx[index] = dp;
		vy[index] = dp;
		vz[index] = dp;

		//cx.p[ix] = dp;
		//cy.p[iy] = dp;
		//cz.p[iz] = dp;
		//cx.addTick(ix);
		//cy.addTick(iy);
		//cz.addTick(iz);

		//p2vel(dx - 1, dy, dz);
		//p2vel(dx, dy - 1, dz);
		//p2vel(dx, dy, dz - 1);
		//p2vel(dx + 1, dy, dz);
		//p2vel(dx, dy + 1, dz);
		//p2vel(dx, dy, dz + 1);

		//for (int j = -1; j < 2; j++) {
		//	for (int k = -1; k < 2; k++) {
		//		for (int l = -1; l < 2; l++) {
		//			//getOrCreate(dx + j, dy + k, dz + l).addTick(index(dx + j, dy + k, dz + l));
		//			//getOrCreate(dx + j, dy + k, dz + l).p[index(dx + j, dy + k, dz + l)] = pressure * .12f;
		//			getOrCreate(dx + j, dy + k, dz + l).addTick(index(dx + j, dy + k, dz + l));
		//		}
		//	}
		//}
	}

	public boolean isEmpty() {
		return emptyLife > 3;
	}

	public static boolean isEmpty(float p, float v) {
		//return p < 0.2 || (p < 0.1 && v < 0.1);
		//return Math.abs(p) < 0.1 && v < 0.1;
		return (p < 0.5 && v < 0.5) || p < .001;
		//return Math.abs(p) + v < 0.4;
	}

	public void iterate(IntConsumer func) {
		int index = -1;
		while ((index = tickIndexes.nextSetBit(index + 1)) != -1) {
			func.accept(index);
		}
	}

	public void swap() {
		tickIndexes = nextTickIndexes;
		nextTickIndexes = new BitSet(4096);

		packet.cords = new BitSet(4096);
		packet.data = new byte[tickIndexes.length() * 3];
		datIndex = 0;
		iterate((index) -> {
			reactIter(index);
			pack(index, packet.cords);
		});
		dp = new float[4096];
		packet.data = Arrays.copyOf(packet.data, datIndex);
		if (datIndex > 0) {
			packet.isEmpty = false;
		}
		packet.writeStep();
	}

	public void pack(int i, BitSet set) {

		float l = lens(vx[i], vy[i], vz[i]);

		if (p[i] > .2 && p[i] + l > 2) {

			packet.data[datIndex] = (byte) ((byte) (vx[i] * 127 / l) & 255);
			packet.data[datIndex + 1] = (byte) ((byte) (vy[i] * 127 / l) & 255);
			packet.data[datIndex + 2] = (byte) ((byte) (vz[i] * 127 / l) & 255);

			set.set(i);
			datIndex += 3;
		}
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
		//swap();
		if (mp > 1.8f) {
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

		this.dp[i] += p[i];

		if (p[i] < 0) {
			p[i] = 0;
		}

		float sp = Math.abs(p[i]);
		float sv = lens(vx[i], vy[i], vz[i]);
		mp = Math.max(sp, mp);

		exp.maxPressure = Math.max(exp.maxPressure, p[i]);

		if (!isEmpty(sp, sv)) {
			addTick(i);
			cx.addTick(ix);
			cy.addTick(iy);
			cz.addTick(iz);
			getOrCreate(x + 1, y, z).addTick(index(x + 1, y, z));
			getOrCreate(x, y + 1, z).addTick(index(x, y + 1, z));
			getOrCreate(x, y, z + 1).addTick(index(x, y, z + 1));
		} else {
			p[i] *= 0.91f;
			vx[i] *= .91f;
			vy[i] *= .91f;
			vz[i] *= .91f;
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

		//float[] rx = react(x + 1, y, z, dx, 0, 0, cx.p[ix]);
		//float[] ry = react(x, y + 1, z, 0, dy, 0, cy.p[iy]);
		//float[] rz = react(x, y, z + 1, 0, 0, dz, cz.p[iz]);
		//float[] r = react(x, y, z, dx, dy, dz, lens(vx[i], vy[i], vz[i]));

		//if (rx[0] != 1f) {
		//	vx[i] *= rx[0];
		//}
		//if (ry[0] != 1f) {
		//	vy[i] *= ry[0];
		//}
		//if (rz[0] != 1f) {
		//	vz[i] *= rz[0];
		//}

		vx[i] += dx;
		vy[i] += dy;
		vz[i] += dz;

		dp[i] += lens(dx, dy, dz);

		//if (r[0] != 1f) {
		//	vx[i] *= r[0];
		//	vy[i] *= r[0];
		//	vz[i] *= r[0];
		//}
		//} else {
		//	if ((r = react(x + 1, y, z, -dx, dy, dz))[0] != 1f) {
		//		vx[i] *= r[0];
		//	}
		//	if ((r = react(x, y + 1, z, dx, -dy, dz))[0] != 1f) {
		//		vy[i] *= r[0];
		//	}
		//	if ((r = react(x, y, z + 1, dx, dy, -dz))[0] != 1f) {
		//		vz[i] *= r[0];
		//	}
		//}

	}

	public void reactIter(int index) {
		int x = x(index);
		int y = y(index);
		int z = z(index);

		BlockState bs = getBs(x, y, z);

		if (BFUtils.isAir(bs)) {
			return;
		}
		BlockPos pos = new BlockPos(x + (this.x << 4), y + (this.y << 4), z + (this.z << 4));
		BlockPhysicsData dat = BFUtils.getParam(bs.getBlock(), pos, exp.world);
		if (dat.falling || dat.fragile) {
			float vp = dp[index];
			float s = dat.strength / 5;
			if (vp > s) {
				exp.world.setBlockState(pos, Blocks.AIR.getDefaultState());

				AdvancedFallingBlockEntity afe = new AdvancedFallingBlockEntity(exp.world, x + (this.x << 4) + .5,
						y + (this.y << 4), z + (this.z << 4) + .5, bs);
				float k = 1f / 5;
				float max = 5;
				float xd = MathHelper.clamp(vx[index] * k, -max, max);
				float yd = MathHelper.clamp(vy[index] * k, -max, max);
				float zd = MathHelper.clamp(vz[index] * k, -max, max);
				afe.setVelocity(xd, yd, zd);
				//afe.fallTime = -4;
				exp.world.addEntity(afe);

				float rem = (vp - s) / vp;
				vx[index] *= rem;
				vy[index] *= rem;
				vz[index] *= rem;
			} else {
				vx[index] = 0;
				vy[index] = 0;
				vz[index] = 0;
			}
		} else {
			vx[index] = 0;
			vy[index] = 0;
			vz[index] = 0;
			//p[index] = 0;
		}

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

	private static float lens(float vx, float vy, float vz) {
		return (float) Math.sqrt((vx * vx) + (vy * vy) + (vz * vz));
	}

}
