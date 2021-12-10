package net.skds.bpo.blockphysics;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;

import it.unimi.dsi.fastutil.longs.Long2ObjectRBTreeMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.server.ServerWorld;
import net.skds.bpo.BPOConfig;
import net.skds.bpo.blockphysics.explosion.CustomExplosion;
import net.skds.bpo.blockphysics.explosion.EFChunk;
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

	public static final int EX_STEPS = 1;

	public static int E_COUNT = 0;

	private ConcurrentHashMap<BFTask, Integer> delayedTasks = new ConcurrentHashMap<>();
	private static ConcurrentSkipListSet<BFTask> TASKS = new ConcurrentSkipListSet<>(WWS::compare);
	private static int T_COUNT = 0;
	private static ConcurrentLinkedQueue<AdvancedFallingBlockEntity> AFBETasks = new ConcurrentLinkedQueue<>();

	public final Long2ObjectRBTreeMap<EFChunk> explosionMap = new Long2ObjectRBTreeMap<>();

	public final ConcurrentLinkedQueue<CustomExplosion> explosions = new ConcurrentLinkedQueue<>();

	public WWS(ServerWorld w, WWSGlobal owner) {
		world = (ServerWorld) w;
		String dimId = world.getDimensionKey().getLocation().toString();
		inBlacklist = BPOConfig.MAIN.dimensionBlacklist.contains(dimId);
		glob = owner;
		collisionMap = new CollisionMap(this);
	}

	public static int compare(BFTask k1, BFTask k2) {
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

	
	public boolean iterateExplosions() {
		explosionMap.forEach((i, c) -> {
			c.emptyLife++;
			c.tickA();
		});
		explosionMap.forEach((i, c) -> {
			c.tickB();
		});
		//if (false)
		explosionMap.long2ObjectEntrySet().removeIf((e) -> e.getValue().isEmpty());
		return !explosionMap.isEmpty();
	}

	public static ITaskRunnable nextTask(int i) {
		if (i > 15 || E_COUNT > BPOConfig.MAIN.maxFallingBlocks) {
			return null;
		}
		if (MTHooks.COUNTS.get() > 0 || Events.getRemainingTickTimeMilis() > MTHooks.TIME) {
			MTHooks.COUNTS.decrementAndGet();
			BFTask task;
			while ((task = TASKS.pollFirst()) != null) {
				// System.out.println(tested);
				task.worker = i;
				T_COUNT--;
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
			E_COUNT++;
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
		if (T_COUNT++ > BPOConfig.MAIN.maxQueueLen) {
			return;
		}
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

	public static void tickServerIn() {
		E_COUNT = 0;
		T_COUNT = TASKS.size();
	}

	// =========== Override ==========

	@Override
	public void tickIn() {
		collisionMap.tick();
		tickDT();		
		int i = EX_STEPS;
		do {
			i--;
			if (!iterateExplosions()) {
				return;
			}
		} while (i > 0);
		
		explosionMap.long2ObjectEntrySet().parallelStream().forEach(e -> {
			e.getValue().reset();
		});

	}

	@Override
	public void tickOut() {
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

	@Override
	public void tickPostMTH() {
	}

	@Override
	public void tickPreMTH() {
	}
}