package net.skds.bpo.client.particles;

import java.util.BitSet;

import com.mojang.blaze3d.vertex.IVertexBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.IAnimatedSprite;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.particle.SpriteTexturedParticle;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.skds.bpo.network.ExplosionPacket;
import net.skds.bpo.util.IndexedCord4B;

@OnlyIn(Dist.CLIENT)
public class ExplodeParticle extends SpriteTexturedParticle {

	Data[] datArray = null;

	static final Minecraft mc = Minecraft.getInstance();

	protected ExplodeParticle(ClientWorld world, double x, double y, double z, double motionX, double motionY,
			double motionZ) {
		super(world, x, y, z);
		this.maxAge = 1;
		this.motionX = motionX;
		this.motionY = motionY;
		this.motionZ = motionZ;
		setBoundingBox(new AxisAlignedBB(x, y, z, x + 16, y + 16, z + 16));
	}

	@Override
	public float getScale(float scaleFactor) {
		return .6f;
	}

	@Override
	public void move(double x, double y, double z) {
	}

	@Override
	public void tick() {
		this.prevPosX = this.posX;
		this.prevPosY = this.posY;
		this.prevPosZ = this.posZ;
		if (this.age++ >= this.maxAge) {
			this.setExpired();
		} else {
			//move(this.motionX, this.motionY, this.motionZ);
		}
	}

	private void renderSingle(IVertexBuilder buffer, ActiveRenderInfo renderInfo, float x, float y, float z) {
		Vector3f[] avector3f = new Vector3f[] { new Vector3f(-1.0F, -1.0F, 0.0F), new Vector3f(-1.0F, 1.0F, 0.0F),
				new Vector3f(1.0F, 1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F) };
		float f4 = this.getScale(0);
		Quaternion quaternion = renderInfo.getRotation();
		for (int i = 0; i < 4; ++i) {
			Vector3f vector3f = avector3f[i];
			vector3f.transform(quaternion);
			vector3f.mul(f4);
			vector3f.add(x, y, z);
		}
		float f7 = this.getMinU();
		float f8 = this.getMaxU();
		float f5 = this.getMinV();
		float f6 = this.getMaxV();
		int j = 255;
		//int j = this.getBrightnessForRender(0);
		buffer.pos((double) avector3f[0].getX(), (double) avector3f[0].getY(), (double) avector3f[0].getZ())
				.tex(f8, f6)
				.color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(j)
				.endVertex();
		buffer.pos((double) avector3f[1].getX(), (double) avector3f[1].getY(), (double) avector3f[1].getZ())
				.tex(f8, f5)
				.color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(j)
				.endVertex();
		buffer.pos((double) avector3f[2].getX(), (double) avector3f[2].getY(), (double) avector3f[2].getZ())
				.tex(f7, f5)
				.color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(j)
				.endVertex();
		buffer.pos((double) avector3f[3].getX(), (double) avector3f[3].getY(), (double) avector3f[3].getZ())
				.tex(f7, f6)
				.color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(j)
				.endVertex();
	}

	@Override
	public boolean shouldCull() {
		return true;
	}

	@Override
	public void renderParticle(IVertexBuilder buffer, ActiveRenderInfo renderInfo, float partialTicks) {

		if (datArray != null) {

			Vector3d cam = renderInfo.getProjectedView();

			int partialInt = (int) (datArray.length * partialTicks);
			float partialSmall = (datArray.length * partialTicks) - partialInt;
			float vc = 0.45f * partialSmall;
			Data d = datArray[partialInt];

			int n = 0;
			int index = -1;
			while ((index = d.cords.nextSetBit(index + 1)) != -1) {
				float x = IndexedCord4B.x(index) + (float) (posX - cam.x);
				float y = IndexedCord4B.y(index) + (float) (posY - cam.y);
				float z = IndexedCord4B.z(index) + (float) (posZ - cam.z);

				float vx = vc * d.data[n + 0] / 127;
				float vy = vc * d.data[n + 1] / 127;
				float vz = vc * d.data[n + 2] / 127;
				n += 3;

				renderSingle(buffer, renderInfo, x + vx, y + vy, z + vz);
			}

		} else {
			Vector3d vector3d = renderInfo.getProjectedView();
			float x = (float) (MathHelper.lerp((double) partialTicks, this.prevPosX, this.posX) - vector3d.getX());
			float y = (float) (MathHelper.lerp((double) partialTicks, this.prevPosY, this.posY) - vector3d.getY());
			float z = (float) (MathHelper.lerp((double) partialTicks, this.prevPosZ, this.posZ) - vector3d.getZ());
			
			renderSingle(buffer, renderInfo, x, y, z);

		}
	}

	public static void spawnParticles(ClientWorld world, ExplosionPacket packet) {

		int x = packet.x << 4;
		int y = packet.y << 4;
		int z = packet.z << 4;

		ExplodeParticle p = ExplodeParticleFactory.instance.makeParticle(null, world, x, y, z, 0, 0, 0);

		p.datArray = new Data[packet.steps];

		for (int i = 0; i < packet.steps; i++) {
			packet.readStep();
			p.datArray[i] = new Data();
			p.datArray[i].cords = packet.cords;
			p.datArray[i].data = packet.data;
		}
		mc.particles.addEffect(p);
	}

	@Override
	public IParticleRenderType getRenderType() {
		return IParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	public static class ExplodeParticleFactory implements IParticleFactory<BasicParticleType> {
		public final IAnimatedSprite sprite;

		public static ExplodeParticleFactory instance;

		public ExplodeParticleFactory(IAnimatedSprite sprite) {
			this.sprite = sprite;
			instance = this;
		}

		@Override
		public ExplodeParticle makeParticle(BasicParticleType typeIn, ClientWorld worldIn, double x, double y, double z,
				double xSpeed, double ySpeed, double zSpeed) {
			ExplodeParticle particle = new ExplodeParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed);
			particle.selectSpriteRandomly(sprite);
			particle.setAlphaF(0.50F);
			return particle;
		}
	}

	private static class Data {	
		byte[] data;
		BitSet cords;
	}
}
