package net.skds.bpo.util.collision;

import java.util.HashSet;
import java.util.Set;

import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.skds.bpo.blockphysics.WWS;
import net.skds.core.util.other.Pair;

public class CollisionMap {

	public Long2ObjectArrayMap<CollisionChunk> chunks = new Long2ObjectArrayMap<>();
	public final WWS owner;

	public CollisionMap(WWS wws) {
		this.owner = wws;
	}

	public void clear() {
		// chunks = new Long2ObjectArrayMap<>();
		Long2ObjectArrayMap<CollisionChunk> chunks2 = new Long2ObjectArrayMap<>();
		chunks.forEach((p, c) -> {
			if (!c.clearAndCheck4Empty()) {
				chunks2.put((long) p, c);
			}
		});
		chunks = chunks2;
	}

	public void tick() {
		clear();
		chunks.forEach((p, c) -> c.tick());
	}

	public Set<AxisAlignedBB> getBoxesExeptE(Entity e, AxisAlignedBB area) {
		int x0 = ((int) Math.floor(area.minX - 2)) >> 4;
		int z0 = ((int) Math.floor(area.minZ - 2)) >> 4;

		int xe = ((int) Math.floor(area.maxX + 2)) >> 4;
		int ze = ((int) Math.floor(area.maxZ + 2)) >> 4;

		// Stream<Map.Entry<Entity, AxisAlignedBB>> stream = Stream.empty();
		Set<AxisAlignedBB> set = new HashSet<>();

		for (int x = x0; x <= xe; x++) {
			for (int z = z0; z <= ze; z++) {
				ChunkPos cpos = new ChunkPos(x, z);
				CollisionChunk chunk = getChunk(cpos);
				set.addAll(chunk.getBoxesExeptE(e, area));
			}
		}

		return set;
	}

	public Set<Pair<Entity, AxisAlignedBB>> getEntities(AxisAlignedBB area) {
		int x0 = ((int) Math.floor(area.minX - 2)) >> 4;
		int z0 = ((int) Math.floor(area.minZ - 2)) >> 4;

		int xe = ((int) Math.floor(area.maxX + 2)) >> 4;
		int ze = ((int) Math.floor(area.maxZ + 2)) >> 4;

		Set<Pair<Entity, AxisAlignedBB>> set = new HashSet<>();

		for (int x = x0; x <= xe; x++) {
			for (int z = z0; z <= ze; z++) {
				ChunkPos cpos = new ChunkPos(x, z);
				CollisionChunk chunk = getChunk(cpos);
				set.addAll(chunk.getEntities(area));
			}
		}

		return set;
	}

	public void addEntity(Entity e) {
		AxisAlignedBB box = e.getBoundingBox();
		BlockPos pos = e.getPosition();
		int y = pos.getY() >> 4;
		if (y > 15 || y < 0) {
			return;
		}
		CollisionChunk chunk = getChunk(pos);
		chunk.getSection(y).addEntity(e, box);
	}

	public void addBox(Entity e, AxisAlignedBB box) {
		BlockPos pos = e.getPosition();
		int y = pos.getY() >> 4;
		if (y > 15 || y < 0) {
			return;
		}
		CollisionChunk chunk = getChunk(pos);
		chunk.getSection(y).addBox(e, box);
	}

	public boolean addBox(BlockPos pos, int time) {
		int y = pos.getY() >> 4;
		if (y > 15 || y < 0) {
			return false;
		}
		CollisionChunk chunk = getChunk(pos);
		return chunk.getSection(y).addBox(pos, time);
	}

	public boolean removeBox(BlockPos pos) {
		int y = pos.getY() >> 4;
		if (y > 15 || y < 0) {
			return false;
		}
		CollisionChunk chunk = getChunk(pos);
		return chunk.getSection(y).removeBox(pos);
	}

	public void removeBox(Entity e) {
		BlockPos pos = e.getPosition();
		int y = pos.getY() >> 4;
		if (y > 15 || y < 0) {
			return;
		}
		CollisionChunk chunk = getChunk(pos);
		chunk.getSection(y).removeBox(e);
	}

	public CollisionChunk getChunk(BlockPos pos) {
		ChunkPos cp = new ChunkPos(pos);
		return getChunk(cp);
	}

	public CollisionChunk getChunk(ChunkPos cp) {
		long lp = cp.asLong();
		synchronized (chunks) {
			CollisionChunk chunk = chunks.get(lp);
			if (chunk == null) {
				chunk = new CollisionChunk(this);
				chunks.put(lp, chunk);
			}
			return chunk;
		}
	}
}