package net.skds.bpo.blockphysics.explosion;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.skds.bpo.blockphysics.BlockPhysicsData;
import net.skds.bpo.blockphysics.FeatureContainer;
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
			destroyBlock(bp, bs);
		}
	}

	public void destroyBlock(BlockPos bp, BlockState bs) {

		//System.out.println(bs);

		BlockPhysicsData dat = BFUtils.getParam(bs.getBlock(), bp, world);
		FeatureContainer fc = BFUtils.getFeatures(bs.getBlock());
		if ((dat.fragile && (!dat.falling || dat.strength <= 25)) || fc.contains(FeatureContainer.Type.TNT)) {
			bs.onBlockExploded(world, bp, explosion);
		} else {
			TransformFeature trf = fc.get(FeatureContainer.Type.TRANSFORM);
			if (trf != TransformFeature.EMPTY) {
				bs = trf.expState;
			}

			world.setBlockState(bp, Blocks.AIR.getDefaultState(), 0);
			AdvancedFallingBlockEntity e = new AdvancedFallingBlockEntity(world, bp.getX() + 0.5, bp.getY(),
					bp.getZ() + 0.5, bs);
			e.fallTime = -5;
			Vector3d delta = e.getPositionVec().subtract(pos);
			float m = 2;
			Vector3d motion = delta.normalize()
					.scale((power / 4f) * m * 200f / (/*delta.lengthSquared() */ Math.max(e.pars.mass, 100)));
			if (motion.length() > m) {
				motion = motion.normalize().scale(m);
			}
			e.setMotion(motion);
			world.addEntity(e);
		}
	}
}
