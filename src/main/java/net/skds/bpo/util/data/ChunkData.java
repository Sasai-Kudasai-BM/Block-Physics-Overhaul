package net.skds.bpo.util.data;

import java.util.Arrays;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.LongArrayNBT;
import net.skds.core.api.IChunkSectionData;
import net.skds.core.util.SKDSUtils.Side;
import net.skds.core.util.data.ChunkSectionAdditionalData;

public class ChunkData implements IChunkSectionData {

	private static final long[] filled;

	public final ChunkSectionAdditionalData sectionData;
	private final Side side;
	private long[] naturalData;

	public ChunkData(ChunkSectionAdditionalData sectionData, Side side) {
		this.side = side;
		this.sectionData = sectionData;
		naturalData = filled.clone();
	}

	@Override
	public void deserialize(CompoundNBT nbt) {
		long[] array = nbt.getLongArray("NaturalGen");
		if (array.length == 64) {
			naturalData = array;
		}
	}

	@Override
	public void serialize(CompoundNBT nbt) {
		LongArrayNBT array = new LongArrayNBT(naturalData);
		nbt.put("NaturalGen", array);
	}

	//@Override
	//public void onBlockAdded(int x, int y, int z, BlockState newState, BlockState oldState) {
	//	if (sectionData.isFinished() && newState != oldState) {
	//		setNatural(x, y, z, false);
	//	}
	//}

	private int getIndex(int x, int y, int z) {
		int n = (x & 15) + ((y & 15) << 4) + ((z & 15) << 8);
		return n;
	}

	public boolean isNatural(int x, int y, int z) {
		int n = getIndex(x, y, z);
		long l = naturalData[n / 64];
		long a = 1L << (n & 63);
		boolean b = (l & a) != 0;		
		//System.out.println(y + " " + b);	
		return b;
	}

	public void setNatural(int x, int y, int z, boolean value) {
		int n = getIndex(x, y, z);
		long a = 1L << (n & 63);
		if (value) {
			naturalData[n / 64] |= a;
		} else {
			naturalData[n / 64] &= ~a;		
		}
	}

	static {
		filled = new long[64];		
		Arrays.fill(filled, -1);
	}

	@Override
	public Side getSide() {
		return side;
	}

	@Override
	public int getSize() {
		return 0;
	}
	
}
