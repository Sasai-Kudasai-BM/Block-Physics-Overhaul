package net.skds.bpo.blockphysics;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.skds.bpo.entity.AdvancedFallingBlockEntity;
import net.skds.core.multithreading.TurboWorldReader;
public class CustomExplosionV3 {

	public final TurboWorldReader reader;

	public final float power;
	public final Vector3d pos;
	public final World world;
	public final Explosion explosion;
	
	public CustomExplosionV3(World world, Vector3d pos, float power, Explosion explosion) {
		this.reader = new TurboWorldReader(world);
		this.world = world;
		this.pos = pos;
		this.power = power;
		this.explosion = explosion;
	}

	public void explode() {
		for (BlockPos bp : explosion.getAffectedBlockPositions()) {
			BlockState bs = world.getBlockState(bp);
			if (bs.getMaterial() == Material.AIR || bs.getBlock() instanceof FlowingFluidBlock) {
				continue;
			}
			world.setBlockState(bp, Blocks.AIR.getDefaultState(), 0);
			AdvancedFallingBlockEntity e = new AdvancedFallingBlockEntity(world, bp.getX() + 0.5, bp.getY(), bp.getZ() + 0.5, bs);
			e.fallTime = -5;
			Vector3d delta = e.getPositionVec().subtract(pos);
			double m = 2;
			Vector3d motion = delta.normalize().scale((power / 4) * m / delta.lengthSquared());
			if (motion.length() > m) {
				motion = motion.normalize().scale(m / 4);
			}
			e.setMotion(motion);
			world.addEntity(e);
		}
	}
}
