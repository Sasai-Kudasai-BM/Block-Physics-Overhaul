package net.skds.bpo.blockphysics.explosion;

import java.util.BitSet;
import java.util.function.IntConsumer;

import net.skds.bpo.util.IndexedCord;
import net.skds.bpo.util.IndexedCord4B;

public class EFChunk extends IndexedCord4B {

	public final byte x, y, z;

	public final CustomExplosion exp;

	public float[] vx = new float[4096];
	public float[] vy = new float[4096];
	public float[] vz = new float[4096];
	public float[] p = new float[4096];

	public boolean tick = false;

	BitSet tickIndexes = new BitSet(4096);
	BitSet nextTickIndexes = new BitSet(4096);

	//public long[] tickIndexes = new long[64];

	public EFChunk(CustomExplosion exp) {
		this.x = 0;
		this.y = 0;
		this.z = 0;
		this.exp = exp;
		tickIndexes.set(0);

		float p0 = 50.0f;

		p[0] = p0;

		EFChunk cx = getOrCreate(-1, 0, 0);
		EFChunk cy = getOrCreate(0, -1, 0);
		EFChunk cz = getOrCreate(0, 0, -1);
		
		int ix = index(-1, 0, 0);
		int iy = index(0, -1, 0);
		int iz = index(0, 0, -1);

		cx.addTick(ix);
		cy.addTick(iy);
		cz.addTick(iz);

		cx.vx[ix] = -0.33F * p0;
		cx.vy[iy] = -0.33F * p0;
		cx.vz[iz] = -0.33F * p0;
	}

	public EFChunk(int index, CustomExplosion exp) {
		this.x = IndexedCord.x(index);
		this.y = IndexedCord.y(index);
		this.z = IndexedCord.z(index);
		this.exp = exp;

		//System.out.printf("x:%s y:%s z:%s\n", x, y, z);
	}

	public void iterate(IntConsumer func) {
		tick = false;
		int index = 0;
		if (tickIndexes.get(index)) {
			func.accept(index);
			tick = true;
		}
		while ((index = tickIndexes.nextSetBit(index + 1)) != -1) {
			func.accept(index);
			tick = true;
		}
	}

	public void swap() {
		tickIndexes = nextTickIndexes;
		nextTickIndexes = new BitSet(4096);
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
		iterate((index) -> {			
			vel2p(x(index), y(index), z(index));
			exp.debug(x(index) + (x << 4), y(index) + (y << 4), z(index) + (z << 4), p[index], (float) Math.sqrt((vx[index]*vy[index]) + (vx[index]*vy[index]) + (vz[index]*vz[index])));
		});
		swap();
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
		//return false;
		//v = (float) Math.sqrt(v);

		//return Math.abs(p - 0.1) < 0.005 && v < 0.01;
		return p < 0.2 || (p < 0.1 && v < 0.001);
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
		dp += cx.vx[ix] - vx[i];
		dp += cy.vy[iy] - vy[i];
		dp += cz.vz[iz] - vz[i];

		dp *= 0.33F;

		p[i] *= 0.999F;
		p[i] += dp;

		if (p[i] < 0) {
			p[i] = 0;
		}

		exp.maxPressure = Math.max(exp.maxPressure, p[i]);
		
		//if (new BlockPos(x(ix), y(ix), z(ix)).distanceSq(new BlockPos(x, y, z)) > 1) {
		//	System.out.println("x");
		//}
		if (!isEmpty(p[i], (vx[i]*vy[i]) + (vx[i]*vy[i]) + (vz[i]*vz[i]))) {
			addTick(i);
			cx.addTick(ix);
			cy.addTick(iy);
			cz.addTick(iz);
			getOrCreate(x + 1, y, z).addTick(index(x + 1, y, z));
			getOrCreate(x, y + 1, z).addTick(index(x, y + 1, z));
			getOrCreate(x, y, z + 1).addTick(index(x, y, z + 1));
		} else {
			vx[i] *= 0.985;
			vy[i] *= 0.985;
			vz[i] *= 0.985;
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

		final float k = 0.33F;

		vx[i] *= 0.9999F;
		vy[i] *= 0.9999F;
		vz[i] *= 0.9999F;

		vx[i] += dx * k;
		vy[i] += dy * k;
		vz[i] += dz * k;

		//if (!isEmpty(p[i], (vx[i]*vy[i]) + (vx[i]*vy[i]) + (vz[i]*vz[i]))) {
		//	//System.out.println("index");
		//	addTick(i);
		//	cx.addTick(ix);
		//	cz.addTick(iz);
		//}
		//if (Math.abs(p[i] - 0.1) < 0.01) {
		//	vx[i] = 0;
		//	vz[i] = 0;
		//}
	}

	public void spread(int x, int y, int z) {

		/*
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
		*/
	}

	public static int byBlockPos(int x, int y, int z) {
		return IndexedCord.index(x >> 4, y >> 4, z >> 4);
	}

	@Override
	public int hashCode() {
		return IndexedCord.index(x, y, z);
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
}
