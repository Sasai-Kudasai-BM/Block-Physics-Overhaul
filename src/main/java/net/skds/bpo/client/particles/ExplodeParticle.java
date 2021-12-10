package net.skds.bpo.client.particles;

import com.mojang.blaze3d.vertex.IVertexBuilder;

import net.minecraft.client.particle.IAnimatedSprite;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteTexturedParticle;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particles.BasicParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.skds.bpo.network.ExplosionPacket;
import net.skds.bpo.registry.ParticleTypesReg;
import net.skds.bpo.util.IndexedCord4B;

@OnlyIn(Dist.CLIENT)
public class ExplodeParticle extends SpriteTexturedParticle {

	protected ExplodeParticle(ClientWorld world, double x, double y, double z, double motionX, double motionY,
			double motionZ) {
		super(world, x, y, z);
		this.maxAge = 2;
		this.motionX = motionX;
		this.motionY = motionY;
		this.motionZ = motionZ;
		this.getScale(4f);

		//this.setSize(1.0F, 1.0F);
	}

	@Override
	public void move(double x, double y, double z) {
		this.posX += x;
		this.posY += y;
		this.posZ += z;
	}

	@Override
	public void tick() {
		this.prevPosX = this.posX;
		this.prevPosY = this.posY;
		this.prevPosZ = this.posZ;
		if (this.age++ >= this.maxAge) {
			this.setExpired();
		} else {
			move(this.motionX, this.motionY, this.motionZ);
		}
	}

	@Override
	public void renderParticle(IVertexBuilder buffer, ActiveRenderInfo renderInfo, float partialTicks) {
		super.renderParticle(buffer, renderInfo, partialTicks);
	}

	public static void spawnParticles(ClientWorld world, ExplosionPacket packet) {

		final float vc = 1f;

		int x0 = packet.x << 4;
		int y0 = packet.y << 4;
		int z0 = packet.z << 4;

		for (int i = 0; i < packet.steps; i++) {
			packet.readStep();

			int n = 0;
			int index = -1;
			while ((index = packet.cords.nextSetBit(index + 1)) != -1) {
				int x = IndexedCord4B.x(index) + x0;
				int y = IndexedCord4B.y(index) + y0;
				int z = IndexedCord4B.z(index) + z0;
				byte dat = packet.data[n];
				n++;

				int mask = 3;
				float vz = ((dat & mask) - 1) * vc;
				dat = (byte) (dat >> 2);
				float vy = ((dat & mask) - 1) * vc;
				dat = (byte) (dat >> 2);
				float vx = ((dat & mask) - 1) * vc;

				//System.out.printf("x:%s y:%s z:%s\n", vx, vy, vz);

				world.addParticle(ParticleTypesReg.EXPLODE.get(), true, x, y, z, vx, vy, vz);

			}
		}
	}

	@Override
	public IParticleRenderType getRenderType() {
		return IParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	public static class ExplodeParticleFactory implements IParticleFactory<BasicParticleType> {
		final IAnimatedSprite sprite;

		public ExplodeParticleFactory(IAnimatedSprite sprite) {
			this.sprite = sprite;
		}

		@Override
		public Particle makeParticle(BasicParticleType typeIn, ClientWorld worldIn, double x, double y, double z,
				double xSpeed, double ySpeed, double zSpeed) {
			ExplodeParticle particle = new ExplodeParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed);
			particle.selectSpriteRandomly(sprite);
			particle.setAlphaF(0.5F);
			return particle;
		}
	}
}
