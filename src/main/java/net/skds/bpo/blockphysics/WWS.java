package net.skds.bpo.blockphysics;

import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.server.ServerWorld;
import net.skds.bpo.BPOConfig;
import net.skds.bpo.entity.AdvancedFallingBlockEntity;
import net.skds.bpo.util.collision.CollisionMap;
import net.skds.core.Events;
import net.skds.core.api.IWWS;
import net.skds.core.api.IWorldExtended;
import net.skds.core.api.multithreading.ITaskRunnable;
import net.skds.core.multithreading.MTHooks;
import net.skds.core.multithreading.ThreadProvider;
import net.skds.core.util.blockupdate.WWSGlobal;

public class WWS implements IWWS {

	public final WWSGlobal glob;
	public final ServerWorld world;

	public final boolean inBlacklist;

	public final CollisionMap collisionMap;

	private static final Comparator<BFTask> comp = new Comparator<BFTask>() {
		@Override
		public int compare(BFTask k1, BFTask k2) {
			if (k1.pos == k2.pos && k1.owner == k2.owner) {
				return 0;
			}
			double dcomp = (k1.getPriority() - k2.getPriority());
			int comp = (int) dcomp;
			if (comp == 0) {
				comp = dcomp > 0 ? 1 : -1;
			}
			return comp;
		}
	};

	private ConcurrentHashMap<BFTask, Integer> delayedTasks = new ConcurrentHashMap<>();
	private static ConcurrentSkipListSet<BFTask> TASKS = new ConcurrentSkipListSet<>(comp);
	private static ConcurrentLinkedQueue<AdvancedFallingBlockEntity> AFBETasks = new ConcurrentLinkedQueue<>();

	public WWS(ServerWorld w, WWSGlobal owner) {
		world = (ServerWorld) w;
		String dimId = world.getDimensionKey().getLocation().toString();
		inBlacklist = BPOConfig.MAIN.dimensionBlacklist.contains(dimId);
		glob = owner;
		collisionMap = new CollisionMap(this);
	}

	public static ITaskRunnable nextTask(int i) {
		if (i > 15) {
			return null;
		}
		if (MTHooks.COUNTS > 0 || Events.getRemainingTickTimeMilis() > MTHooks.TIME) {
			MTHooks.COUNTS--;
			BFTask task;
			while ((task = TASKS.pollFirst()) != null) {
				// System.out.println(tested);
				task.worker = i;
				return task;
			}
		}
		return null;
	}

	public static ITaskRunnable nextETask(int i) {
		if (i > 15) {
			return null;
		}

		AdvancedFallingBlockEntity e;
		if ((e = AFBETasks.poll()) != null) {			
			return new ETask(e);
		}

		return null;
	}

	public static void tickEntity(Entity e) {
		if (e instanceof LivingEntity) {
			WWS wws = (WWS) ((IWorldExtended) e.world).getWWS().getTyped(WWS.class);
			wws.collisionMap.addEntity(e);
		}
	}

	public static void afterEntityTick() {
		//AdvancedFallingBlockEntity e;
		//while ((e = AFBETasks.poll()) != null) {
		//	e.tick2();
		//}
		//if (true)
		//	return;
		ThreadProvider.doSyncFork(WWS::nextETask);
		try {
			ThreadProvider.waitForStop();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void pushAFBT(AdvancedFallingBlockEntity e) {
		AFBETasks.add(e);
	}

	public static void pushTask(BFTask task) {
		TASKS.add(task);
	}

	public static void pushDelaydedTask(BFTask task, int time) {
		if (time > 0) {
			task.owner.delayedTasks.put(task, time);
		} else {
			TASKS.add(task);
		}
	}

	private void tickDT() {
		delayedTasks.forEach((task, time) -> {
			time--;
			if (time > 0) {
				delayedTasks.put(task, time);
			} else {
				TASKS.add(task);
				delayedTasks.remove(task);
			}
		});
	}

	// =========== Override ==========

	@Override
	public void tickIn() {
		//BPOConfig.cash();
		collisionMap.tick();
		tickDT();
	}

	@Override
	public void tickOut() {		
		// System.out.println(TASKS.size());
		//System.out.println(AFBETasks.size());
		// System.out.println(delayedTasks.size());
		// System.out.println(collisionMap.chunks.size());
	}

	@Override
	public void close() {
		TASKS.forEach(t -> t.revoke(world));
		TASKS.clear();
	}

	@Override
	public WWSGlobal getG() {
		return glob;
	}
}