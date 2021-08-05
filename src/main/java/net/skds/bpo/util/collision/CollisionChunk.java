package net.skds.bpo.util.collision;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.skds.core.util.other.Pair;

public class CollisionChunk {

	public CollisionChunkSection[] sections = new CollisionChunkSection[16];	
	public final CollisionMap owner;

	public CollisionChunk(CollisionMap map) {
		this.owner = map;
	}

	public Set<AxisAlignedBB> getBoxesExeptE(Entity entity, AxisAlignedBB area) {

		int y0 = ((int) Math.floor(area.minY - 2)) >> 4;
		int ye = ((int) Math.floor(area.maxY + 2)) >> 4;
		Set<AxisAlignedBB> set = new HashSet<>();
		for (int y = y0; y <= ye; y++) {
			CollisionChunkSection section = getSection(y);
			if (section == null) {				
				return set;
			}
			set.addAll(section.getBoxesEx(entity, area));
		}
		return set;
	}

	public Set<Pair<Entity, AxisAlignedBB>> getEntities(AxisAlignedBB area) {

		int y0 = ((int) Math.floor(area.minY - 2)) >> 4;
		int ye = ((int) Math.floor(area.maxY + 2)) >> 4;
		Set<Pair<Entity, AxisAlignedBB>> set = new HashSet<>();
		for (int y = y0; y <= ye; y++) {
			CollisionChunkSection section = getSection(y);
			if (section == null) {				
				return set;
			}
			set.addAll(section.getEntities(area));
		}
		return set;
	}

	public void tick() {
		for (CollisionChunkSection section : sections) {
			if (section != null)
				section.tick();
		}
	}

	public synchronized CollisionChunkSection getSection(int y) {
		if (y > 15 || y < 0) {
			return null;
		}
		if (sections[y] == null) {
			sections[y] = new CollisionChunkSection();
		}
		return sections[y];
	}

	public boolean clearAndCheck4Empty() {
		boolean ret = true;
		for (CollisionChunkSection ccs : sections) {
			if (ccs != null && !ccs.clearAndCheck4Empty()) {
				ret = false;
			}
		}
		return ret;
	}

	public class CollisionChunkSection {

		public ConcurrentHashMap<Long, Integer> poses = new ConcurrentHashMap<>();
		public Map<Entity, AxisAlignedBB> entities = new HashMap<>();
		public Map<Entity, AxisAlignedBB> boxes = new HashMap<>();
		
		public void tick() {
			poses.forEach((p, t) -> {
				t--;
				if (t > 0) {
					poses.put(p, t);
				} else {
					//BlockPos pos = BlockPos.fromLong(p);
					//WWS.pushTask(new BFTask(owner.owner, pos, BFTask.Type.NEIGHBOR));
					poses.remove(p);
				}
			});
		}

		public boolean clearAndCheck4Empty() {
			entities.clear();
			boxes.clear();
			return poses.isEmpty();
		}

		public boolean addBox(BlockPos pos, int time) {
			return poses.put(pos.toLong(), time) == null;
		}

		public boolean removeBox(BlockPos pos) {
			return poses.remove(pos.toLong()) != null;
		}

		public void addEntity(Entity e, AxisAlignedBB box) {
			entities.put(e, box);
		}

		public void addBox(Entity e, AxisAlignedBB box) {
			boxes.put(e, box);
		}

		public void removeBox(Entity e) {
			boxes.remove(e);
		}

		public Set<AxisAlignedBB> getBoxesEx(Entity en, AxisAlignedBB area) {
			Set<AxisAlignedBB> bx = new HashSet<>();
			boxes.forEach((e, b) -> {
				if (en != e && b.intersects(area)) {
					bx.add(b);
				}
			});
			poses.forEach((p, t) -> {
				BlockPos pos = BlockPos.fromLong(p);
				AxisAlignedBB aabb = new AxisAlignedBB(pos);
				if (aabb.intersects(area))
					bx.add(aabb);		
			});
			return bx;
		}

		public Set<Pair<Entity, AxisAlignedBB>> getEntities(AxisAlignedBB area) {
			Set<Pair<Entity, AxisAlignedBB>> bx = new HashSet<>();
			entities.forEach((e, b) -> {
				if (b.intersects(area)) {
					bx.add(new Pair<Entity,AxisAlignedBB>(e, b));
				}
			});
			return bx;
		}
	}
}