package net.skds.bpo.blockphysics;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.skds.core.api.multithreading.ITaskRunnable;

public class BFTask implements ITaskRunnable {

	private static double uuid = 0;

	public int worker = -1;
	public int counter = 0;

	public final Type type;
	public final WWS owner;
	public final long pos;
	public final double priority;

	public BFTask(WWS owner, BlockPos pos, Type type) {
		this.type = type;
		this.owner = owner;
		this.pos = pos.toLong();
		uuid = uuid + 1.0E-6;
		if (uuid >=1) {
			uuid = 0;
		}
		this.priority = owner.glob.getSqDistToNBP(pos) + uuid;
		//System.out.println(pos + "  " + type);
	}

	@Override
	public boolean revoke(World wr) {
		World w = owner.world;
		if (w != wr) {
			return false;
		}
		return true;
	}

	@Override
	public double getPriority() {
		return priority;
	}

	@Override
	public int getSubPriority() {
		return 0;
	}

	@Override
	public void run() {
		
		BFExecutor executor = new BFExecutor(this);
		executor.run();
		
	}

	public static enum Type {
		UPDATE, RANDOM, NEIGHBOR, NEIGHBOR_UP, DOWNRAY, UPRAY, PLACED;
	}
}