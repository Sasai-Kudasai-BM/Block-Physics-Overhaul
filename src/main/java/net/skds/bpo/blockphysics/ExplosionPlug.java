package net.skds.bpo.blockphysics;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.skds.bpo.blockphysics.features.TransformFeature;
import net.skds.bpo.entity.AdvancedFallingBlockEntity;
import net.skds.bpo.util.BFUtils;
public class ExplosionPlug {

	public final float power;
	public final Vector3d pos;
	public final World world;
	public final Explosion explosion;
	
	public ExplosionPlug(World world, Vector3d pos, float power, Explosion explosion) {
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
			
			TransformFeature tf = BFUtils.getFeatures(bs.getBlock()).get(FeatureContainer.Type.TRANSFORM);
			if (tf != null) {
				bs = tf.expState;
			}
			world.setBlockState(bp, Blocks.AIR.getDefaultState(), 0);
			AdvancedFallingBlockEntity e = new AdvancedFallingBlockEntity(world, bp.getX() + 0.5, bp.getY(), bp.getZ() + 0.5, bs);
			e.fallTime = -5;
			Vector3d delta = e.getPositionVec().subtract(pos);
			float m = 5;
			Vector3d motion = delta.normalize().scale((power / 4f) * m * 1f / (/*delta.lengthSquared() */ Math.sqrt(e.pars.mass)));
			if (motion.length() > m ) {
				motion = motion.normalize().scale(m);
			}
			e.setMotion(motion);
			world.addEntity(e);
		}
	}
}
