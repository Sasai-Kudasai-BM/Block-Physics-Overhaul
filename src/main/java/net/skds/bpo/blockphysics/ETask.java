package net.skds.bpo.blockphysics;

import net.minecraft.world.World;
import net.skds.bpo.entity.AdvancedFallingBlockEntity;
import net.skds.core.api.multithreading.ITaskRunnable;

public class ETask implements ITaskRunnable {

	private static double PRIO = 0;
	private final double priority;
	private final AdvancedFallingBlockEntity afb;

	public ETask(AdvancedFallingBlockEntity afb) {
		PRIO++;
		priority = PRIO;
		this.afb = afb;
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
	public boolean revoke(World arg0) {
		return true;
	}

	@Override
	public void run() {
		afb.tick2();		
	}
}