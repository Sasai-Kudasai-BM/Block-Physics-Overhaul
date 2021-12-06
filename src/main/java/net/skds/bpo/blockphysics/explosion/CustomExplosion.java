package net.skds.bpo.blockphysics.explosion;


import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.skds.bpo.util.IndexedCord;
import net.skds.core.multithreading.TurboWorldReader;

public class CustomExplosion extends IndexedCord {

	static final byte BYTE = (byte) 0xff;

	final static double[] kernel = new double[9];

	public final TurboWorldReader reader;

	public float maxPressure;

	public final float power;
	public final Vector3d pos;
	public final World world;
	public final Explosion explosion;

	public final int x0, y0, z0;

	Int2ObjectRBTreeMap<EFChunk> map = new Int2ObjectRBTreeMap<>((a, b) -> a - b);

	public CustomExplosion(World world, Vector3d pos, float power, Explosion explosion) {
		this.reader = new TurboWorldReader(world);
		this.world = world;
		this.pos = pos;
		this.power = power;
		this.explosion = explosion;
		this.x0 = (int) pos.x;
		this.y0 = (int) pos.y;
		this.z0 = (int) pos.z;
	}

	public EFChunk put(EFChunk e) {
		return map.put(e.hashCode(), e);
	}

	public EFChunk getOrCreate(int index) {
		return map.computeIfAbsentPartial(index, (i) -> new EFChunk(i, this));
	}


	public void explode() {
		EFChunk c = new EFChunk(this);
		put(c);

		do {
			iterate();
		} while (maxPressure > 0.8);
	}

	public void iterate() {
		maxPressure = 0;
		map.forEach((i, c) -> {
			c.tickA();
		});
		map.forEach((i, c) -> {
			c.tickB();
		});
	}

	void debug(int x, int y, int z, float p, float v) {

		//if (e.isEmpty()) {
		//	continue;
		//}
		v = (float) Math.sqrt(v);

		//Block[] blocks = new Block[] { Blocks.SANDSTONE, Blocks.BROWN_WOOL, Blocks.RED_WOOL, Blocks.ORANGE_WOOL,
		//		Blocks.YELLOW_WOOL, Blocks.LIME_WOOL, Blocks.GREEN_WOOL, Blocks.CYAN_WOOL, Blocks.LIGHT_BLUE_WOOL,
		//		Blocks.BLUE_WOOL, Blocks.PURPLE_WOOL, Blocks.WHITE_WOOL };

		Block[] blocks2 = new Block[] { Blocks.AIR,  Blocks.AIR,  Blocks.AIR, Blocks.BROWN_STAINED_GLASS, Blocks.RED_STAINED_GLASS,
				Blocks.ORANGE_STAINED_GLASS, Blocks.YELLOW_STAINED_GLASS, Blocks.LIME_STAINED_GLASS,
				Blocks.GREEN_STAINED_GLASS, Blocks.CYAN_STAINED_GLASS, Blocks.LIGHT_BLUE_STAINED_GLASS,
				Blocks.BLUE_STAINED_GLASS, Blocks.PURPLE_STAINED_GLASS, Blocks.WHITE_STAINED_GLASS };

		int i = (int) (p * 12);
		int i2 = (int) (v * 5);

		if (i < 0) {
			i = 0;
		}
		if (i2 < 0) {
			i2 = 0;
		}
		if (i >= blocks2.length) {
			i = blocks2.length - 1;
		}
		if (i2 >= blocks2.length) {
			i2 = blocks2.length - 1;
		}

		BlockState bs = world.getBlockState(new BlockPos(pos.x + x, pos.y + y, pos.z + z));

		if (bs.isIn(BlockTags.IMPERMEABLE) || bs.isIn(Blocks.AIR)) {
			world.setBlockState(new BlockPos(pos.x + x, pos.y + y, pos.z + z), blocks2[i].getDefaultState(), 18);
		}


		//world.setBlockState(new BlockPos(pos.x + x, pos.y + y, pos.z + z), blocks2[i2].getDefaultState(), 18);

		//Minecraft.getInstance().world.addParticle(ParticleTypes.FLAME, true, pos.x + x, pos.y + y + 1, pos.z + z, 0, 0.2, 0);

		//if (pressure < 0.01) {
		//	world.setBlockState(new BlockPos(pos.x + x, pos.y + y, pos.z + z),
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
